package com.linweiyun.genshin.core.system.combat.action;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActionManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * key = "C:UUID" / "S:UUID"
     * 单机时客户端和服务端在同一个 JVM，UUID 相同，必须用 side 区分。
     */
    private static final Map<String, ActionManager> MANAGERS = new ConcurrentHashMap<>();

    private ActionState current;
    private ActionDefinition buffered;

    /**
     * 当前正在驱动状态机的角色实例。
     * <p>
     * 一个 ActionManager 是 per-player 的，但一个玩家有多个 party 角色。
     * 服务端的 CharacterTickEvent 会 tick 所有 party 成员——如果每个成员的 tick
     * 都无条件推进状态机，会导致 party 之间的动作互相打断。
     * <p>
     * activeCharacter 记录"当前动作属于哪个角色"，只有它的 tick / request 才会
     * 驱动状态机；其他角色的 tick 直接忽略。
     * <p>
     * 玩家切换角色通过显式的 {@link #interrupt(InterruptReason)} 打断；request 时
     * 如果传入的 character 与 activeCharacter 不同，也会自动打断（兜底）。
     */
    private PGCharacter activeCharacter;

    private int lastComboIndex = 0;
    private long lastComboEndTick = Long.MIN_VALUE;

    public static ActionManager get(Player player) {
        String key = (player.level().isClientSide() ? "C:" : "S:") + player.getUUID();
        return MANAGERS.computeIfAbsent(key, k -> new ActionManager());
    }

    public static void remove(Player player) {
        String key = (player.level().isClientSide() ? "C:" : "S:") + player.getUUID();
        MANAGERS.remove(key);
    }

    public boolean requestNormalAttack(Player player, PGCharacter character) {
        String side = player.level().isClientSide() ? "CLIENT" : "SERVER";
        ActionSet set = character.getActionSet(player);
        if (set == null) {
            LOGGER.warn("[ActionManager] [{}] requestNormalAttack: actionSet=null (talent={})",
                    side, character.getTalentDebugInfo());
            return false;
        }
        if (set.getNormalComboSize() == 0) {
            LOGGER.warn("[ActionManager] [{}] requestNormalAttack: comboSize=0", side);
            return false;
        }
        int idx = resolveNextComboIndex(player, set);
        return request(player, character, set.getNormalAttack(idx));
    }

    public boolean requestChargedAttack(Player player, PGCharacter character) {
        String side = player.level().isClientSide() ? "CLIENT" : "SERVER";
        ActionSet set = character.getActionSet(player);
        if (set == null) {
            LOGGER.warn("[ActionManager] [{}] requestChargedAttack: actionSet=null", side);
            return false;
        }
        return request(player, character, set.getChargedAttack());
    }

    public boolean requestElementalSkill(Player player, PGCharacter character, int skillTime) {
        String side = player.level().isClientSide() ? "CLIENT" : "SERVER";
        LOGGER.info("[ActionManager] [{}] requestElementalSkill skillTime={}", side, skillTime);

        ActionSet set = character.getActionSet(player);
        if (set == null) {
            LOGGER.warn("[ActionManager] [{}] actionSet=null (talent={})",
                    side, character.getTalentDebugInfo());
            return false;
        }
        boolean longPress = skillTime >= 1000;
        ActionDefinition def = longPress ? set.getElementalSkillHold() : set.getElementalSkillTap();
        if (def == null) {
            LOGGER.warn("[ActionManager] [{}] elementalSkill{} def=null (comboSize={})",
                    side, longPress ? "Hold" : "Tap", set.getNormalComboSize());
            return false;
        }

        // 1. CD / 能量检查
        if (!character.canUseElementalSkill(player, skillTime)) {
            LOGGER.info("[ActionManager] [{}] canUse=false, rejected", side);
            character.sendSkillCooldownMessage(player);
            return false;
        }

        // 2. 启动动作
        if (!request(player, character, def)) {
            LOGGER.info("[ActionManager] [{}] request() rejected (busy?)", side);
            return false;
        }

        // 3. 立即设 CD
        character.applyElementalSkillCooldown(player, skillTime);
        LOGGER.info("[ActionManager] [{}] elementalSkill STARTED", side);
        return true;
    }

    public boolean requestElementalBurst(Player player, PGCharacter character) {
        String side = player.level().isClientSide() ? "CLIENT" : "SERVER";
        LOGGER.info("[ActionManager] [{}] requestElementalBurst", side);

        ActionSet set = character.getActionSet(player);
        if (set == null) {
            LOGGER.warn("[ActionManager] [{}] actionSet=null", side);
            return false;
        }
        ActionDefinition def = set.getElementalBurst();
        if (def == null) {
            LOGGER.warn("[ActionManager] [{}] burst def=null", side);
            return false;
        }

        if (!character.canUseElementalBurst(player)) {
            LOGGER.info("[ActionManager] [{}] burst canUse=false", side);
            character.sendBurstCooldownMessage(player);
            return false;
        }

        if (!request(player, character, def)) {
            LOGGER.info("[ActionManager] [{}] burst request() rejected", side);
            return false;
        }

        character.applyElementalBurstCooldown(player);
        LOGGER.info("[ActionManager] [{}] burst STARTED", side);
        return true;
    }

    private boolean request(Player player, PGCharacter character, ActionDefinition def) {
        if (def == null) return false;

        if (activeCharacter != null && activeCharacter != character) {
            if (current != null && !current.isFinished()) {
                current.interrupt(InterruptReason.SWITCH_CHARACTER);
            }
            buffered = null;
            resetCombo();
        }
        activeCharacter = character;

        if (current != null && !current.isFinished()) {
            boolean isSkill = def.kind == ActionKind.ELEMENTAL_SKILL_TAP
                    || def.kind == ActionKind.ELEMENTAL_SKILL_HOLD
                    || def.kind == ActionKind.ELEMENTAL_BURST;

            if (current.isProtected()) {
                // 保护期内：仅技能可打断
                if (isSkill) {
                    current.interrupt(InterruptReason.MANUAL);
                    buffered = null;
                } else {
                    return false;
                }
            } else {
                // 非保护期（comboWindow 或收尾）：直接替换
                current.interrupt(InterruptReason.MANUAL);
                if (!def.isCombo()) resetCombo();
            }
        }

        start(player, character, def);
        return true;
    }

    private void start(Player player, PGCharacter character, ActionDefinition def) {
        ActionContext ctx = new ActionContext(player, character, def);
        current = new ActionState(def, ctx);
    }

    public void tick(Player player, PGCharacter character) {
        // ⭐ 只有当前活跃角色的 tick 才驱动状态机；
        //    party 里其他成员的 tick 直接忽略，避免互相打断。
        if (activeCharacter != null && activeCharacter != character) {
            return;
        }

        if (current == null || current.isFinished()) {
            if (buffered != null) {
                ActionDefinition next = buffered;
                buffered = null;
                start(player, character, next);
            }
            return;
        }

        current.tick();

        if (current.isFinished()) {
            ActionDefinition def = current.getDefinition();
            if (def.isCombo()) {
                lastComboIndex = def.comboIndex;
                lastComboEndTick = player.level().getGameTime();
            }
            if (buffered != null) {
                ActionDefinition next = buffered;
                buffered = null;
                start(player, character, next);
            }
        }
    }

    public void interrupt(InterruptReason reason) {
        if (current == null || current.isFinished()) return;
        boolean forced = reason == InterruptReason.SWITCH_CHARACTER
                || reason == InterruptReason.DEATH
                || reason == InterruptReason.JUMP;
        if (forced) {
            current.interrupt(reason);
            buffered = null;
            resetCombo();
            if (reason == InterruptReason.SWITCH_CHARACTER || reason == InterruptReason.DEATH) {
                activeCharacter = null;
            }
            return;
        }
        if (!current.isProtected()) {
            current.interrupt(reason);
            buffered = null;
        }
    }

    public boolean isBusy() { return current != null && !current.isFinished(); }

    public boolean isMovementBlocked() {
        return current != null && !current.isFinished() && current.isProtected();
    }

    public boolean isAttackBlocked() {
        return current != null && !current.isFinished() && current.isProtected();
    }

    public ActionState getCurrent() { return current; }

    private int resolveNextComboIndex(Player player, ActionSet set) {
        if (current != null && !current.isFinished() && current.getDefinition().isCombo()) {
            return current.getDefinition().comboIndex + 1;
        }
        if (lastComboIndex <= 0) return 1;
        ActionDefinition lastDef = set.getNormalAttack(lastComboIndex);
        if (lastDef == null) return 1;
        long elapsed = player.level().getGameTime() - lastComboEndTick;
        if (elapsed > lastDef.comboWindow()) return 1;
        return lastComboIndex + 1;
    }

    public void resetCombo() {
        lastComboIndex = 0;
        lastComboEndTick = Long.MIN_VALUE;
        buffered = null;
    }
}