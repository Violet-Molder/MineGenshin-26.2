package com.linweiyun.genshin.content.skill_node;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DashSystem {

    private static final Map<UUID, DashState> STATES = new ConcurrentHashMap<>();
    private static final int DEFAULT_DASH_TICKS = 3;

    private static class DashState {
        final Vec3 movePerTick;
        int remainingTicks;

        DashState(Vec3 movePerTick, int remainingTicks) {
            this.movePerTick = movePerTick;
            this.remainingTicks = remainingTicks;
        }
    }

    public static Vec3 startDash(Player player, Vec3 delta, int ticks) {
        DashState state = new DashState(delta.scale(1.0 / ticks), ticks);
        STATES.put(player.getUUID(), state);
        return delta;
    }

    public static Vec3 startDash(Player player, Vec3 delta) {
        return startDash(player, delta, DEFAULT_DASH_TICKS);
    }

    public static void tickDash(Player player) {
        UUID uuid = player.getUUID();
        DashState state = STATES.get(uuid);
        if (state == null) return;

        state.remainingTicks--;
        if (state.remainingTicks < 0) {
            player.setDeltaMovement(Vec3.ZERO);
            STATES.remove(uuid);
            return;
        }
        player.setDeltaMovement(state.movePerTick);
    }

    public static boolean isDashing(Player player) {
        return STATES.containsKey(player.getUUID());
    }
}