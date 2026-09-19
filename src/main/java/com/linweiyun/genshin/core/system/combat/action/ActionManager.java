package com.linweiyun.genshin.core.system.combat.action;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
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

    private String cachedStateKey;
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
        refreshStateKey(player, character);
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
        refreshStateKey(player, character);
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

        refreshStateKey(player, character);
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

        refreshStateKey(player, character);
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

        if (current != null && !current.isFinished()) {
            ActionPhase phase = current.getPhase();
            if (phase == ActionPhase.POSTCAST && def.isCombo()) {
                buffered = def;
                return true;
            }
            return false;
        }

        start(player, character, def);
        return true;
    }

    private void start(Player player, PGCharacter character, ActionDefinition def) {
        ActionContext ctx = new ActionContext(player, character, def);
        current = new ActionState(def, ctx);
    }

    public void tick(Player player, PGCharacter character) {
        refreshStateKey(player, character);

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
        if (reason == InterruptReason.SWITCH_CHARACTER || reason == InterruptReason.DEATH) {
            current.interrupt(reason);
            buffered = null;
            resetCombo();
            return;
        }
        ActionPhase phase = current.getPhase();
        if (phase == ActionPhase.PRECAST || phase == ActionPhase.POSTCAST) {
            current.interrupt(reason);
            buffered = null;
        }
    }

    public boolean isBusy() { return current != null && !current.isFinished(); }

    public ActionPhase getPhase() {
        if (current == null || current.isFinished()) return ActionPhase.IDLE;
        return current.getPhase();
    }

    public boolean isMovementBlocked() {
        ActionPhase p = getPhase();
        return p == ActionPhase.PRECAST || p == ActionPhase.ACTIVE;
    }

    public boolean isAttackBlocked() { return getPhase() == ActionPhase.ACTIVE; }

    public ActionState getCurrent() { return current; }

    private int resolveNextComboIndex(Player player, ActionSet set) {
        if (current != null && !current.isFinished() && current.getDefinition().isCombo()) {
            return current.getDefinition().comboIndex + 1;
        }
        if (lastComboIndex <= 0) return 1;
        ActionDefinition lastDef = set.getNormalAttack(lastComboIndex);
        if (lastDef == null) return 1;
        long elapsed = player.level().getGameTime() - lastComboEndTick;
        if (elapsed > lastDef.postcastTicks) return 1;
        return lastComboIndex + 1;
    }

    public void resetCombo() {
        lastComboIndex = 0;
        lastComboEndTick = Long.MIN_VALUE;
        buffered = null;
    }

    private void refreshStateKey(Player player, PGCharacter character) {
        String key = character.getActionStateKey(player);
        if (!key.equals(cachedStateKey)) {
            cachedStateKey = key;
            if (current != null && !current.isFinished()) {
                current.interrupt(InterruptReason.MANUAL);
            }
            buffered = null;
            resetCombo();
        }
    }
}