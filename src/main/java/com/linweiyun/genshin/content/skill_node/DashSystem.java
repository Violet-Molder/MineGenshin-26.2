package com.linweiyun.genshin.content.skill_node;

import net.minecraft.world.entity.LivingEntity;
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
 * 突进系统 —— 生命周期与"是否有突进进行中"绑定。
 * <p>
 * 核心行为：
 * <ul>
 *     <li>没有任何突进时：<b>不在事件总线上注册</b>，玩家 tick 派发完全不触达本类。</li>
 *     <li>首次调用 startDash / startDamageDash：动态注册 {@link DashTicker}。</li>
 *     <li>最后一个突进结束时：动态注销 {@link DashTicker}，回到零开销状态。</li>
 * </ul>
 * <p>
 * 移动模型：
 * <ul>
 *     <li>客户端：{@link #startDash}/{@link #startDamageDash} 在客户端调用时，仅做视觉移动。</li>
 *     <li>服务端：调用时既做移动，也做线段扫掠命中检测。</li>
 *     <li>由动作系统触发时，通常只在服务端调用 —— 客户端靠位置同步自动跟随。</li>
 * </ul>
 */
public final class DashSystem {

    private DashSystem() {}

    private static final Map<UUID, DashState> STATES = new ConcurrentHashMap<>();

    private static final Object LOCK = new Object();
    private static boolean tickerRegistered = false;

    private static final int DEFAULT_DASH_TICKS = 3;
    private static final double DEFAULT_HIT_RADIUS = 0.6;

    // ============================================================
    // 状态
    // ============================================================

    private static final class DashState {
        final Vec3 movePerTick;
        int remainingTicks;
        final Consumer<LivingEntity> onHit;   // null = 纯位移
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

    // ============================================================
    // 启动入口
    // ============================================================

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
        STATES.put(player.getUUID(), state);
        ensureTickerRegistered();
        return delta;
    }

    // ============================================================
    // 动态注册 / 注销
    // ============================================================

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
            if (!STATES.isEmpty()) return;
            NeoForge.EVENT_BUS.unregister(DashTicker.class);
            tickerRegistered = false;
        }
    }

    // ============================================================
    // 每 tick 推进（由 DashTicker 调用）
    // ============================================================

    static void onPlayerTick(Player player) {
        DashState state = STATES.get(player.getUUID());
        if (state == null) return;

        state.remainingTicks--;
        if (state.remainingTicks < 0) {
            player.setDeltaMovement(Vec3.ZERO);
            STATES.remove(player.getUUID());
            releaseTickerIfIdle();
            return;
        }

        // 服务端：先做线段扫掠命中（用当前位置 → 下一 tick 位置）
        if (!player.level().isClientSide() && state.hasDamage()) {
            Vec3 from = player.position();
            Vec3 to = from.add(state.movePerTick);
            sweepDetect(player, from, to, state);
        }

        // 两端都设置速度：客户端负责视觉，服务端负责权威位置（会自动同步给客户端）
        player.setDeltaMovement(state.movePerTick);
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
                    // TODO: log
                }
            }
        }
    }

    private static boolean segmentHits(Vec3 from, Vec3 to, LivingEntity target, double radius) {
        AABB box = target.getBoundingBox().inflate(radius);
        if (box.contains(from) || box.contains(to)) return true;
        return box.clip(from, to).isPresent();
    }

    // ============================================================
    // 状态查询 / 取消
    // ============================================================

    public static boolean isDashing(Player player) {
        return STATES.containsKey(player.getUUID());
    }

    public static void cancelDash(Player player) {
        if (STATES.remove(player.getUUID()) != null) {
            releaseTickerIfIdle();
        }
    }

    // ============================================================
    // 动态监听器 —— 只有存在突进时才会被注册到事件总线
    // ============================================================

    public static final class DashTicker {
        private DashTicker() {}

        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            DashSystem.onPlayerTick(event.getEntity());
        }
    }
}