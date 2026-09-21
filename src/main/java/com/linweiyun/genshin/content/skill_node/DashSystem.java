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
 * 突进系统 —— 技能自己的位移（申鹤的 E / 重击都在用）。
 *
 * <h2>按「距离」走，不按「速度」走</h2>
 * 每一刻直接 {@code player.move(MoverType.SELF, 方向 × 本刻步长)}：
 * {@code move} 只处理碰撞，<b>不掺摩擦</b>，走多少就是多少；而且这一步
 * <b>永远不越过总距离</b>（最后一步只走剩下的零头）。
 * 一旦走满总距离（{@code delta.length()}，例如申鹤 E 的 10 格），或者撞墙走不动了，
 * 就<b>立即刹车</b>：{@code setDeltaMovement(ZERO)} 把残留动量清掉，绝不再靠摩擦滑一段。
 *
 * <p>反过来用速度驱动（{@code setDeltaMovement} + {@code hurtMarked}）是<b>不行</b>的：
 * 那样实际位移由摩擦、玩家输入、客户端预测共同决定，走多远不可控，也刹不住 ——
 * 表现就是「冲不到 10 格」或者「到了还在飘」。
 *
 * <h2>两端各一份状态</h2>
 * <ul>
 *   <li><b>客户端</b>：真正推位置（这一段位移的表现层）。</li>
 *   <li><b>服务端</b>：不推位置（服务端玩家的位置本来就跟着客户端走），
 *       只按「实际走过的距离」记账 + 做线段扫掠命中。</li>
 * </ul>
 * 单机时两端共享 JVM，所以两张表必须分开：合成一张会被同时推两次，距离直接错。
 *
 * <p>谁负责在客户端调用它：角色天赋现在只在服务端跑，所以客户端那一次是由
 * {@code ResourceDrivenActionHandler} 的「本地招式钩子」触发的
 * （见 {@code PGCharacter#runsTalentOnClient()}）。
 */
public final class DashSystem {

    private DashSystem() {}

    /** 客户端专用（视觉位移） */
    private static final Map<UUID, DashState> CLIENT_STATES = new ConcurrentHashMap<>();
    /** 服务端专用（伤害扫掠 + 距离记账） */
    private static final Map<UUID, DashState> SERVER_STATES = new ConcurrentHashMap<>();

    private static final Object LOCK = new Object();
    private static boolean tickerRegistered = false;

    private static final int DEFAULT_DASH_TICKS = 3;
    private static final double DEFAULT_HIT_RADIUS = 0.6;

    /** 距离容差（格）：剩余小于它就当成「到位了」。 */
    private static final double DISTANCE_EPSILON = 0.05;

    /** 单刻最大步长（格）—— 刻数填得很小时防止一帧穿墙。 */
    private static final double MAX_STEP_PER_TICK = 1.5;

    /** 撞墙判定：本刻实际位移不到期望的一半，就认为被挡住了。 */
    private static final double BLOCKED_RATIO = 0.5;

    /** 兜底：超过「刻数 + 这么多刻」还没走完就强制收尾，别留下永远不结束的状态。 */
    private static final int GRACE_TICKS = 10;

    // ============================================================
    // 状态
    // ============================================================

    private static final class DashState {
        /** 单位方向。 */
        final Vec3 direction;
        /** 这一趟总共要走多少格。 */
        final double totalDistance;
        /** 每刻走多少格。 */
        final double stepPerTick;
        /** 已经走了多少格（按实际位移累计）。 */
        double traveled;
        /** 兜底剩余刻数。 */
        int ticksLeft;
        final Consumer<LivingEntity> onHit;
        final double hitRadius;
        final Set<UUID> hitTargets = new HashSet<>();

        DashState(Vec3 direction, double totalDistance, double stepPerTick, int ticksLeft,
                  Consumer<LivingEntity> onHit, double hitRadius) {
            this.direction = direction;
            this.totalDistance = totalDistance;
            this.stepPerTick = stepPerTick;
            this.ticksLeft = ticksLeft;
            this.onHit = onHit;
            this.hitRadius = hitRadius;
        }

        boolean hasDamage() { return onHit != null; }

        double remaining() { return totalDistance - traveled; }
    }

    private static Map<UUID, DashState> statesFor(Player player) {
        return player.level().isClientSide() ? CLIENT_STATES : SERVER_STATES;
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

    /**
     * @param delta 这一趟的位移矢量 —— <b>长度就是总距离</b>（方向取它的单位向量）
     * @param ticks 打算用几刻走完（决定每刻步长，不影响总距离）
     */
    private static Vec3 start(Player player, Vec3 delta, int ticks,
                              Consumer<LivingEntity> onHit, double hitRadius) {
        double totalDistance = delta.length();
        if (totalDistance < DISTANCE_EPSILON) {
            return delta;                       // 没有位移需求
        }

        int safeTicks = Math.max(1, ticks);
        Vec3 direction = delta.scale(1.0 / totalDistance);
        double stepPerTick = Math.min(totalDistance / safeTicks, MAX_STEP_PER_TICK);

        DashState state = new DashState(direction, totalDistance, stepPerTick,
                safeTicks + GRACE_TICKS, onHit, hitRadius);
        statesFor(player).put(player.getUUID(), state);
        ensureTickerRegistered();
        return delta;
    }

    // ============================================================
    // 动态注册 / 注销（没有突进时零开销）
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
            if (!CLIENT_STATES.isEmpty()) return;
            if (!SERVER_STATES.isEmpty()) return;
            NeoForge.EVENT_BUS.unregister(DashTicker.class);
            tickerRegistered = false;
        }
    }

    // ============================================================
    // 每刻推进（由 DashTicker 调用）
    // ============================================================

    static void onPlayerTick(Player player) {
        Map<UUID, DashState> states = statesFor(player);
        DashState state = states.get(player.getUUID());
        if (state == null) return;

        if (--state.ticksLeft <= 0 || state.remaining() <= DISTANCE_EPSILON) {
            brake(player, states);
            return;
        }

        double step = Math.min(state.stepPerTick, state.remaining());
        Vec3 before = player.position();

        if (player.level().isClientSide()) {
            // 客户端：按格推位置 —— player.move 不掺摩擦，这一步走多少就是多少
            player.move(MoverType.SELF, state.direction.scale(step));
            Vec3 actual = player.position().subtract(before);
            state.traveled += actual.length();

            // 撞墙（实际位移远小于期望）也立刻刹车，不要贴墙磨完剩下的刻数
            if (state.remaining() <= DISTANCE_EPSILON
                    || actual.length() < step * BLOCKED_RATIO) {
                brake(player, states);
            }
        } else {
            // 服务端：不推位置，只扫伤害 + 按实际位移记账（位置由客户端同步上来）
            if (state.hasDamage()) {
                sweepDetect(player, before, before.add(state.direction.scale(step)), state);
            }
            Vec3 actual = player.position().subtract(before);
            state.traveled += actual.length();
            if (state.remaining() <= DISTANCE_EPSILON) {
                brake(player, states);
            }
        }
    }

    /**
     * 立即刹车：把残留动量清零（不然摩擦/惯性还会再滑一小段，看着像「刹不住」），
     * 然后结束这次突进。
     */
    private static void brake(Player player, Map<UUID, DashState> states) {
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;               // 两端都把「速度归零」推出去
        states.remove(player.getUUID());
        releaseTickerIfIdle();
    }

    // ============================================================
    // 伤害扫掠
    // ============================================================

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

    // ============================================================
    // 状态查询 / 取消
    // ============================================================

    public static boolean isDashing(Player player) {
        return statesFor(player).containsKey(player.getUUID());
    }

    /** 还剩多少格没走完（没在突进时返回 0）。 */
    public static double remainingDistance(Player player) {
        DashState state = statesFor(player).get(player.getUUID());
        return state == null ? 0.0 : Math.max(0.0, state.remaining());
    }

    public static void cancelDash(Player player) {
        player.setDeltaMovement(Vec3.ZERO);
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
