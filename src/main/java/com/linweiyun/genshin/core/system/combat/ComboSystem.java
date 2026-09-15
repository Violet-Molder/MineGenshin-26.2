package com.linweiyun.genshin.core.system.combat;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ComboSystem {
    private static final Map<UUID, ComboState> STATES = new HashMap<>();

    public static class ComboState {
        int comboStage = 0;
        int pendingStage = -1;
        long precastEndTick = 0;
        long postcastEndTick = 0;
        long windowEndTick = 0;

        boolean chargedPending = false;
        long chargedPrecastEndTick = 0;
        long chargedPostcastEndTick = 0;
    }

    private static ComboState getState(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), k -> new ComboState());
    }

    public static int getComboStage(Player player) {
        ComboState state = getState(player);
        long tick = player.level().getGameTime();
        if (state.windowEndTick > 0 && tick > state.windowEndTick) {
            state.comboStage = 0;
            state.pendingStage = -1;
            state.precastEndTick = 0;
            state.postcastEndTick = 0;
            state.windowEndTick = 0;
        }
        return state.comboStage;
    }

    public static void advanceCombo(Player player, int precastTicks, int postcastTicks,
                                     int windowTicks, int maxCombo) {
        ComboState state = getState(player);
        long tick = player.level().getGameTime();
        state.comboStage = (state.comboStage + 1) % maxCombo;
        state.pendingStage = state.comboStage;
        state.precastEndTick = tick + precastTicks;
        state.postcastEndTick = tick + precastTicks + postcastTicks;
        state.windowEndTick = tick + precastTicks + postcastTicks + windowTicks;
    }

    public static void resetCombo(Player player) {
        ComboState state = STATES.get(player.getUUID());
        if (state != null) {
            state.comboStage = 0;
            state.pendingStage = -1;
            state.precastEndTick = 0;
            state.postcastEndTick = 0;
            state.windowEndTick = 0;
            state.chargedPending = false;
            state.chargedPrecastEndTick = 0;
            state.chargedPostcastEndTick = 0;
        }
    }

    /**
     * 移动封锁：仅在前摇+后摇期间为 true，窗口期不封锁移动。
     */
    public static boolean isMovementBlocked(Player player) {
        ComboState state = STATES.get(player.getUUID());
        if (state == null) return false;
        long tick = player.level().getGameTime();
        return tick <= state.postcastEndTick;
    }

    /**
     * 攻击封锁：仅在前摇+后摇期间为 true。
     */
    public static boolean isAttackBlocked(Player player) {
        ComboState state = STATES.get(player.getUUID());
        if (state == null) return false;
        long tick = player.level().getGameTime();
        return tick <= state.postcastEndTick;
    }

    public static int consumePendingAttack(Player player) {
        ComboState state = STATES.get(player.getUUID());
        if (state == null || state.pendingStage < 0) return -1;
        long tick = player.level().getGameTime();
        if (tick > state.precastEndTick) {
            int stage = state.pendingStage;
            state.pendingStage = -1;
            return stage;
        }
        return -1;
    }

    public static void startChargedAttack(Player player, int precastTicks, int postcastTicks) {
        ComboState state = getState(player);
        long tick = player.level().getGameTime();
        state.comboStage = 0;
        state.pendingStage = -1;
        state.precastEndTick = 0;
        state.postcastEndTick = 0;
        state.windowEndTick = 0;
        state.chargedPending = true;
        state.chargedPrecastEndTick = tick + precastTicks;
        state.chargedPostcastEndTick = tick + precastTicks + postcastTicks;
    }

    public static boolean consumePendingChargedAttack(Player player) {
        ComboState state = STATES.get(player.getUUID());
        if (state == null || !state.chargedPending) return false;
        long tick = player.level().getGameTime();
        if (tick > state.chargedPrecastEndTick) {
            state.chargedPending = false;
            state.chargedPrecastEndTick = 0;
            return true;
        }
        return false;
    }

    public static boolean isChargedAttackBlocking(Player player) {
        ComboState state = STATES.get(player.getUUID());
        if (state == null || !state.chargedPending) return false;
        long tick = player.level().getGameTime();
        return tick <= state.chargedPostcastEndTick;
    }
}