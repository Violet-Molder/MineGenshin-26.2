package com.linweiyun.genshin.content.skill_node;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 突进系统。
 * <p>
 * 客户端和服务端 <b>状态独立</b>（两个 Map）。
 * 单机时两端共享 JVM，static 会被两端同时 tick，导致距离减半 —— 必须分开。
 * <p>
 * 客户端：每 tick {@code player.move(MoverType.SELF, movePerTick)} 推位置，不走摩擦。
 * 服务端：不推位置，只做线段扫掠命中检测。
 */
public final class DashSystem {

    private DashSystem() {}

    /** 客户端专用（视觉位移） */
    private static final Map<UUID, DashState> CLIENT_STATES = new ConcurrentHashMap<>();
    /** 服务端专用（伤害扫掠） */
    private static final Map<UUID, DashState> SERVER_STATES = new ConcurrentHashMap<>();

    private static final Object LOCK = new Object();
    private static boolean tickerRegistered = false;

    private static final int DEFAULT_DASH_TICKS = 3;
    private static final double DEFAULT_HIT_RADIUS = 0.6;

    private static final class DashState {
        final Vec3 movePerTick;
        int remainingTicks;
        final Consumer<LivingEntity> onHit;
        final double hitRadius;
        final Set<UUID> hitTargets = new HashSet<>();

        DashState(Vec3 movePerTick, int remainingTicks,
                  Consumer<LivingEntity> onHit, double hitRadius) {
            this.movePerTick = movePerTick;
            this.remainingTicks = remainingTicks;
            this.onHit = onHit;
            this.hitRadius = hitRadius;
        }

        boolean hasDamage() { return onHit != null; }
    }

    private static Map<UUID, DashState> statesFor(Player player) {
        return player.level().isClientSide() ? CLIENT_STATES : SERVER_STATES;
    }

    public static Vec3 startDash(Player player, Vec3 delta) {
        return startDash(player, delta, DEFAULT_DASH_TICKS);
    }

    public static Vec3 startDash(Player player, Vec3 delta, int ticks) {
        return start(player, delta, ticks, null, DEFAULT_HIT_RADIUS);
    }

    public static Vec3 startDamageDash(Player player, Vec3 delta, int ticks,
                                       Consumer<LivingEntity> onHit) {
        return start(player, delta, ticks, onHit, DEFAULT_HIT_RADIUS);
    }

    public static Vec3 startDamageDash(Player player, Vec3 delta, int ticks,
                                       Consumer<LivingEntity> onHit, double hitRadius) {
        return start(player, delta, ticks, onHit, hitRadius);
    }

    private static Vec3 start(Player player, Vec3 delta, int ticks,
                              Consumer<LivingEntity> onHit, double hitRadius) {
        int safeTicks = Math.max(1, ticks);
        DashState state = new DashState(
                delta.scale(1.0 / safeTicks), safeTicks, onHit, hitRadius);
        statesFor(player).put(player.getUUID(), state);
        ensureTickerRegistered();
        return delta;
    }

    private static void ensureTickerRegistered() {
        synchronized (LOCK) {
            if (tickerRegistered) return;
            NeoForge.EVENT_BUS.register(DashTicker.class);
            tickerRegistered = true;
        }
    }

    private static void releaseTickerIfIdle() {
        synchronized (LOCK) {
            if (!tickerRegistered) return;
            if (!CLIENT_STATES.isEmpty()) return;
            if (!SERVER_STATES.isEmpty()) return;
            NeoForge.EVENT_BUS.unregister(DashTicker.class);
            tickerRegistered = false;
        }
    }

    static void onPlayerTick(Player player) {
        Map<UUID, DashState> states = statesFor(player);
        DashState state = states.get(player.getUUID());
        if (state == null) return;

        state.remainingTicks--;

        if (state.remainingTicks < 0) {
            if (player.level().isClientSide()) {
                player.setDeltaMovement(Vec3.ZERO);
            }
            states.remove(player.getUUID());
            releaseTickerIfIdle();
            return;
        }

        if (player.level().isClientSide()) {
            // 客户端：直接推位置
            player.move(MoverType.SELF, state.movePerTick);
            player.setDeltaMovement(Vec3.ZERO);
        } else {
            // 服务端：只做伤害扫掠
            if (state.hasDamage()) {
                Vec3 from = player.position();
                Vec3 to = from.add(state.movePerTick);
                sweepDetect(player, from, to, state);
            }
        }
    }

    private static void sweepDetect(Player player, Vec3 from, Vec3 to, DashState state) {
        AABB sweep = new AABB(from, to).inflate(state.hitRadius + 0.5);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class, sweep,
                e -> e != player && e.isAlive() && !state.hitTargets.contains(e.getUUID())
        );
        for (LivingEntity target : candidates) {
            if (segmentHits(from, to, target, state.hitRadius)) {
                state.hitTargets.add(target.getUUID());
                try {
                    state.onHit.accept(target);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static boolean segmentHits(Vec3 from, Vec3 to, LivingEntity target, double radius) {
        AABB box = target.getBoundingBox().inflate(radius);
        if (box.contains(from) || box.contains(to)) return true;
        return box.clip(from, to).isPresent();
    }

    public static boolean isDashing(Player player) {
        return statesFor(player).containsKey(player.getUUID());
    }

    public static void cancelDash(Player player) {
        if (statesFor(player).remove(player.getUUID()) != null) {
            releaseTickerIfIdle();
        }
    }

    public static final class DashTicker {
        private DashTicker() {}

        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            DashSystem.onPlayerTick(event.getEntity());
        }
    }
}