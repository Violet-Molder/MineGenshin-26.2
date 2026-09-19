package com.linweiyun.genshin.core.system.combat.action;

import com.linweiyun.genshin.core.character.PGCharacter;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActionManager {

    private static final Map<UUID, ActionManager> MANAGERS = new ConcurrentHashMap<>();

    @Getter
    private ActionState current;
    private ActionDefinition buffered;

    private String cachedStateKey;
    private int lastComboIndex = -1;
    private long lastComboEndTick = Long.MIN_VALUE;

    public static ActionManager get(Player player) {
        return MANAGERS.computeIfAbsent(player.getUUID(), k -> new ActionManager());
    }

    public static void remove(Player player) {
        MANAGERS.remove(player.getUUID());
    }

    public boolean requestNormalAttack(Player player, PGCharacter character) {
        refreshStateKey(player, character);
        ActionSet set = character.getActionSet(player);
        if (set == null || set.getNormalComboSize() == 0) return false;
        int idx = resolveNextComboIndex(player, set);
        return request(player, character, set.getNormalAttack(idx));
    }

    public boolean requestChargedAttack(Player player, PGCharacter character) {
        refreshStateKey(player, character);
        ActionSet set = character.getActionSet(player);
        return set != null && request(player, character, set.getChargedAttack());
    }

    public boolean requestElementalSkill(Player player, PGCharacter character, boolean longPress) {
        refreshStateKey(player, character);
        ActionSet set = character.getActionSet(player);
        if (set == null) return false;
        ActionDefinition def = longPress ? set.getElementalSkillHold() : set.getElementalSkillTap();
        return request(player, character, def);
    }

    public boolean requestElementalBurst(Player player, PGCharacter character) {
        refreshStateKey(player, character);
        ActionSet set = character.getActionSet(player);
        return set != null && request(player, character, set.getElementalBurst());
    }

    private boolean request(Player player, PGCharacter character, ActionDefinition def) {
        if (def == null) return false;
        if (current != null && !current.isFinished()) {
            if (def.isCombo()) { buffered = def; return true; }
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

    private int resolveNextComboIndex(Player player, ActionSet set) {
        if (current != null && !current.isFinished() && current.getDefinition().isCombo()) {
            return current.getDefinition().comboIndex + 1;
        }
        if (lastComboIndex < 0) return 0;
        ActionDefinition lastDef = set.getNormalAttack(lastComboIndex);
        if (lastDef == null) return 0;
        long elapsed = player.level().getGameTime() - lastComboEndTick;
        if (elapsed > lastDef.postcastTicks) return 0;
        return lastComboIndex + 1;
    }

    public void resetCombo() {
        lastComboIndex = -1;
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