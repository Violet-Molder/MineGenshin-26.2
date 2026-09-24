package net.luoshu.imaginarybranch.combat;

import net.luoshu.imaginarybranch.animation.state.ActionStateMachine;
import net.luoshu.imaginarybranch.combat.targeting.CombatTargeting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 近战贴上去 + 转向 + 吸附 —— 「锁到目标之后怎么打到人」。
 *
 * <h2>三件事</h2>
 * <ol>
 *   <li><b>转向</b>：衰减式追角，快但不瞬移，而且<b>只写 {@code yBodyRot}</b>（模型朝向），
 *       绝不碰 {@code yRot} —— 那是本机玩家的镜头，碰一下玩家就觉得画面被拽走了。</li>
 *   <li><b>突进</b>：目标在攻击距离外时冲过去，期间动画冻在起手帧，到位再解冻重新计时。
 *       方向是<b>全向</b>的（含 Y）—— 目标是飞的/在脚下的都能贴过去。</li>
 *   <li><b>吸附</b>：<b>出手那一瞬间</b>朝目标推一小步（一次一小段）。不是持续跟随 ——
 *       持续跟随就成磁铁了。</li>
 * </ol>
 *
 * <h2>到位判定留了宽容度</h2>
 * 冲刺时目标也在动，要求精确贴住会出现「人已经贴脸了却永远没到位」→ 贴着人鬼畜。
 * 所以进到「停靠距离 + {@link #ARRIVE_SLACK}」就算到位。
 */
public final class AttackApproach {

    // ==================== 转向 ====================

    /** 每刻消掉剩余夹角的比例；越大越急。 */
    public static final float TURN_GAIN = 0.45f;
    /** 最小角速度（度/刻）—— 小角度也保持一点惯性。 */
    public static final float TURN_MIN_SPEED = 6.0f;
    /** 最大角速度（度/刻，45 ≈ 900°/秒）：大角度也不会一帧到位。 */
    public static final float TURN_MAX_SPEED = 45.0f;
    /** 夹角小于这个度数就算转到了（度）。 */
    public static final float TURN_TOLERANCE = 1.5f;
    /** 一次动作最多持续转向多少刻（突进期间不消耗这份预算）。 */
    public static final int TURN_BUDGET_TICKS = 16;

    // ==================== 突进 ====================

    /** 突进终点：贴到目标身前多近就停（格）。 */
    public static final double STOP_DISTANCE = 1.4;
    /** 到位宽容度（格）—— 「差不多就行」，别死磕精确距离。 */
    public static final double ARRIVE_SLACK = 0.6;
    /** 突进速度（格/刻），全向。 */
    public static final double DASH_SPEED = 1.0;
    /** 兜底：最多突进多少刻，超时也要继续打。 */
    public static final int MAX_APPROACH_TICKS = 20;
    /**
     * 「已经够得着」连续多少刻就强制开打。
     *
     * <p>目标在到位线附近来回动时，光靠精确距离判定会让人一直跟着跑；
     * 够得着（进到这一招的攻击距离）连续这么多刻就直接出手 —— 反正已经打得到了。
     */
    public static final int ARRIVE_PATIENCE_TICKS = 3;

    // ==================== 吸附 ====================

    /** 吸附带（格）：目标在「攻击距离 + 这个」以内才在出手时推一小步。 */
    public static final double ADHESION_BAND = 1.5;
    /** 出手那一下的推力（冲量，≈ 半个格位移）。 */
    public static final double ADHESION_STEP_SPEED = 0.22;
    /** 冲量 → 位移的换算（地面摩擦 ≈0.4/刻），用来按缺口收力。 */
    public static final double ADHESION_TRAVEL_FACTOR = 2.5;
    /** 比这个还近就不再推（推了会穿模/穿过去丢锁）。 */
    public static final double ADHESION_MIN_DISTANCE = 0.9;

    // ==================== 招式形态 ====================

    /**
     * 一次出手的形态 —— <b>逐招式</b>决定近战/远程、要不要突进、索敌多远。
     *
     * <p>本工程没有 {@code ActionStep} 那套数据驱动（时序硬编码在 {@code Miyabi*Client} 里），
     * 所以这里给两个预设，直接用/按需改。
     */
    public record ApproachParams(boolean ranged, boolean dash,
                                 double acquireRange, double keepRange,
                                 double acquireAngle, double keepAngle,
                                 double stopDistance, double dashSpeed,
                                 int maxApproachTicks, float turnSpeed) {

        /** 近战默认：索敌 6 / 保持 9 / 攻击距离外突进贴脸。 */
        public static final ApproachParams MELEE =
                new ApproachParams(false, true, -1, -1, -1, -1, -1, -1, 0, -1f);

        /** 远程：不突进、索敌 = 攻击距离、只转向。 */
        public static final ApproachParams RANGED =
                new ApproachParams(true, false, -1, -1, -1, -1, -1, -1, 0, -1f);

        /** 这一招实际会不会突进。 */
        public boolean wantsDash() {
            return !ranged && dash;
        }

        // ==================== 自定义：链式修改 ====================
        //
        // 用法（只写关心的，其余保持预设）：
        //     ApproachParams.MELEE.withRange(8, 11).withDashProfile(3.0, 0.9, 24)
        // 也可以直接 new 一个常量放在角色自己的类里。

        public ApproachParams withDash(boolean enabled) {
            return new ApproachParams(ranged, enabled, acquireRange, keepRange, acquireAngle,
                    keepAngle, stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        /** 索敌与保持距离（格）。传 -1 表示这一项用默认。 */
        public ApproachParams withRange(double acquire, double keep) {
            return new ApproachParams(ranged, dash, acquire, keep, acquireAngle,
                    keepAngle, stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        /** 索敌与保持的视角半角（度）。传 -1 表示这一项用默认。 */
        public ApproachParams withAngles(double acquireAngle, double keepAngle) {
            return new ApproachParams(ranged, dash, acquireRange, keepRange, acquireAngle,
                    keepAngle, stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        /** 突进手感三件套：停几格 / 冲多快 / 最多几刻。 */
        public ApproachParams withDashProfile(double stopDistance, double dashSpeed,
                                              int maxApproachTicks) {
            return new ApproachParams(ranged, dash, acquireRange, keepRange, acquireAngle,
                    keepAngle, stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        /** 转向最大角速度（度/刻）：想让人转得更急/更缓就调它。 */
        public ApproachParams withTurnSpeed(float degreesPerTick) {
            return new ApproachParams(ranged, dash, acquireRange, keepRange, acquireAngle,
                    keepAngle, stopDistance, dashSpeed, maxApproachTicks, degreesPerTick);
        }

        /** 换算成索敌参数：<b>远程招式的索敌距离 = 攻击距离</b>（够得着才锁）。 */
        public CombatTargeting.Params targeting(double attackRange) {
            CombatTargeting.Params base = ranged
                    ? CombatTargeting.Params.forRange(acquireRange > 0 ? acquireRange : attackRange)
                    : CombatTargeting.Params.DEFAULT;

            double acquire = acquireRange > 0 ? acquireRange : base.acquireRange();
            double keep = keepRange > 0 ? keepRange : base.keepRange();
            double acquireAngle = this.acquireAngle > 0 ? this.acquireAngle : base.acquireAngle();
            double keepAngle = this.keepAngle > 0 ? this.keepAngle : base.keepAngle();

            return new CombatTargeting.Params(acquire, acquireAngle, keep, keepAngle, base.policy());
        }
    }

    // ==================== 状态 ====================

    private static Runnable onArrive;
    private static int targetId;

    // 转向（独立于突进：到位之后还要把最后几度转完）
    private static boolean facing;
    private static int facingTicksLeft;
    private static float facingTurnSpeed = TURN_MAX_SPEED;

    // 突进
    private static boolean dashing;
    private static int dashTicksLeft;
    private static double dashStopDistance = STOP_DISTANCE;
    private static double dashSpeed = DASH_SPEED;
    private static double dashAttackRange = 3.0;
    /** 连续多少刻「已经够得着」（用于 {@link #ARRIVE_PATIENCE_TICKS} 的兜底开打）。 */
    private static int nearTicks;

    private AttackApproach() {
    }

    /** 正在突进。 */
    public static boolean isActive() {
        return dashing;
    }

    /** 这次攻击需不需要突进（目标在攻击距离外）。 */
    public static boolean needsDash(Player player, @Nullable LivingEntity target, double attackRange) {
        return target != null && player.distanceTo(target) > attackRange;
    }

    // ==================== 开始 ====================

    /**
     * 开始贴脸流程：转向 + 突进，到位后回调。
     *
     * <p>调用方负责先用 {@link ApproachParams#wantsDash()} 判断要不要走这条路；
     * 只要走这条，就一定会突进。
     *
     * @param attackRange 这一招的生效攻击距离（决定到位线）
     * @param onArrive    到位后干什么（解冻动画 + 切状态 + 发服务端请求）
     */
    public static void begin(LocalPlayer player, LivingEntity target, ApproachParams params,
                             double attackRange, Runnable onArrive) {
        startFacing(player, target, params);

        AttackApproach.targetId = target.getId();
        AttackApproach.onArrive = onArrive;
        AttackApproach.dashing = true;
        AttackApproach.nearTicks = 0;
        AttackApproach.dashTicksLeft = params != null && params.maxApproachTicks() > 0
                ? params.maxApproachTicks()
                : MAX_APPROACH_TICKS;
        AttackApproach.dashStopDistance = params != null && params.stopDistance() > 0
                ? params.stopDistance()
                : STOP_DISTANCE;
        AttackApproach.dashSpeed = params != null && params.dashSpeed() > 0
                ? params.dashSpeed()
                : DASH_SPEED;
        AttackApproach.dashAttackRange = attackRange > 0 ? attackRange : 3.0;

        // 冻结动画：状态机把当前动作停在这一帧
        ActionStateMachine.setApproachFrozen(true);
    }

    /**
     * 只转向不位移 —— 目标已经在攻击距离内、或者这一招是远程招式时用这个。
     *
     * @return 有没有开始转向（没目标就返回 false）
     */
    public static boolean faceTarget(LocalPlayer player, @Nullable LivingEntity target,
                                     @Nullable ApproachParams params) {
        if (target == null) {
            return false;
        }
        startFacing(player, target, params);
        return true;
    }

    private static void startFacing(LocalPlayer player, LivingEntity target, @Nullable ApproachParams params) {
        targetId = target.getId();
        facing = true;
        facingTicksLeft = TURN_BUDGET_TICKS;
        facingTurnSpeed = params != null && params.turnSpeed() > 0 ? params.turnSpeed() : TURN_MAX_SPEED;
    }

    /**
     * 吸附：<b>出手那一瞬间</b>朝目标推一小段，一次一小步。
     *
     * <p>要的手感是「每一刀各自带一小步」：目标挪开半步 → 这一刀先不动，
     * 下一刀再贴回去。因为每次只走一小段、连招间隔又短，看起来就是攻击自带吸附，
     * 而不是像磁铁一样被持续拖着走。
     */
    public static void stepToward(LocalPlayer player, LivingEntity target, double attackRange) {
        double distance = player.distanceTo(target);
        if (distance > attackRange + ADHESION_BAND || distance <= ADHESION_MIN_DISTANCE) {
            return;
        }

        Vec3 delta = target.position().subtract(player.position());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal < 1.0E-6) {
            return;
        }

        // 按缺口收力：贴脸时不推、离得远时推满（也就半个格）
        double gap = Math.min(distance - ADHESION_MIN_DISTANCE, ADHESION_BAND);
        double speed = Math.min(ADHESION_STEP_SPEED, gap / ADHESION_TRAVEL_FACTOR);

        Vec3 nudge = new Vec3(delta.x, 0, delta.z).scale(speed / horizontal);
        player.setDeltaMovement(player.getDeltaMovement().add(nudge));
        player.hurtMarked = true;
    }

    /** 取消当前突进与转向（换动作 / 回常态时调用）。 */
    public static void cancel() {
        clearDash();
        facing = false;
        facingTicksLeft = 0;
        ActionStateMachine.setApproachFrozen(false);
    }

    // ==================== 每刻 ====================

    /** 每客户端 tick 调一次（在状态机 tick 之前）。 */
    public static void tick(LocalPlayer player) {
        if (!facing && !dashing) {
            return;
        }

        LivingEntity target = resolveTarget(player);

        // 目标没了 → 突进立刻收摊（保留原速度别把人定在半路），转向也停
        if (target == null) {
            if (dashing) {
                finish(player, false);
            }
            facing = false;
            facingTicksLeft = 0;
            return;
        }

        tickFacing(player, target);

        if (!dashing) {
            return;
        }

        // 超时 → 照样继续打，只是不位移了
        if (--dashTicksLeft <= 0) {
            finish(player, false);
            return;
        }

        double distance = player.distanceTo(target);

        // 到位判定：进到「停靠距离 + 宽容度」就出手（别死磕精确贴住）
        if (distance <= arriveDistance()) {
            finish(player, true);
            return;
        }

        // 兜底：已经「够得着」（进到这一招的攻击距离）却还没进到位线 → 连续几刻就强制开打。
        // 目标在到位线附近来回动时，距离会一直在线上弹，
        // 只靠精确判定会出现「人在旁边跟着跑，动画却一直冻着不出手」。够得着就已经能打中了。
        if (distance <= dashAttackRange) {
            if (++nearTicks >= ARRIVE_PATIENCE_TICKS) {
                finish(player, true);
                return;
            }
        } else {
            nearTicks = 0;
        }

        // 全向突进：含 Y。目标是飞的 / 在脚下的，只推水平会永远差一段高度
        Vec3 delta = target.position().subtract(player.position());
        double remaining = delta.length();
        if (remaining < 1.0E-6) {
            finish(player, true);
            return;
        }

        // 不按剩余距离收力：靠近到位线时收力，正好和目标往外走的速度（怪约 0.2 格/刻）抵消，
        // 距离会永远卡在到位线上一点点 → 人跟着目标跑直到 20 刻超时，动画全程冻着。
        // 全程满速，靠「最多一步到目标身上」防冲过头：下一帧距离就够近，直接判定到位。
        double step = Math.min(dashSpeed, remaining);
        Vec3 velocity = delta.scale(step / remaining);
        player.setDeltaMovement(velocity.x, velocity.y, velocity.z);
        player.hurtMarked = true;
    }

    /** 到位线 = min(攻击距离, 停靠距离 + 宽容度)。 */
    private static double arriveDistance() {
        double slack = Math.max(0.0, dashStopDistance + ARRIVE_SLACK);
        return Math.min(dashAttackRange, slack);
    }

    /**
     * 转向一步。
     *
     * <p><b>两个必须遵守的约束</b>（都踩过）：
     * <ol>
     *   <li><b>绝不过冲</b>：转动量夹在剩余夹角以内（用 {@code approachDegrees}）。
     *       早期版本直接相加，剩余 5° 时一步跨到对面、下一帧再跨回来 → 镜头左右乱晃（±5° 极限环）。</li>
     *   <li><b>只转身体</b>：写 {@code yBodyRot}，不写 {@code yRot} / {@code yHeadRot}。
     *       本机玩家的 {@code yRot} 就是镜头，碰一下就有「被拽」的观感。</li>
     * </ol>
     */
    private static void tickFacing(LocalPlayer player, LivingEntity target) {
        if (!facing) {
            return;
        }

        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        if (dx * dx + dz * dz < 1.0E-6) {
            facing = false;
            return;
        }

        float targetYaw = yawTo(dx, dz);
        float delta = Mth.wrapDegrees(targetYaw - player.yBodyRot);

        if (Math.abs(delta) <= TURN_TOLERANCE) {
            setBodyYaw(player, targetYaw);
            facing = false;
            return;
        }

        if (!dashing && --facingTicksLeft <= 0) {
            facing = false;
            return;
        }

        float minSpeed = Math.min(TURN_MIN_SPEED, facingTurnSpeed);
        float speed = Math.max(minSpeed, Math.min(Math.abs(delta) * TURN_GAIN, facingTurnSpeed));
        setBodyYaw(player, Mth.approachDegrees(player.yBodyRot, targetYaw, speed));
    }

    /**
     * 到位：急停 + 解冻动画并回调（回调里切状态、发请求）。
     *
     * @param hardStop true = 贴到目标的正常到位（水平速度清零做急停）；
     *                 false = 目标没了/超时（保留原速度，别把人定在半路）
     */
    private static void finish(LocalPlayer player, boolean hardStop) {
        Runnable callback = onArrive;
        clearDash();

        if (hardStop) {
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            player.hurtMarked = true;
        }

        ActionStateMachine.setApproachFrozen(false);

        if (callback != null) {
            callback.run();
        }
    }

    private static void clearDash() {
        dashing = false;
        dashTicksLeft = 0;
        onArrive = null;
    }

    // ==================== 工具 ====================

    private static float yawTo(double dx, double dz) {
        return (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
    }

    private static void setBodyYaw(Player player, float yaw) {
        // 只写身体朝向：模型转过去对准目标，玩家的镜头一动不动
        player.setYBodyRot(yaw);
    }

    @Nullable
    private static LivingEntity resolveTarget(LocalPlayer player) {
        if (targetId == 0 || player.level() == null) {
            return null;
        }
        return player.level().getEntity(targetId) instanceof LivingEntity living && living.isAlive()
                ? living
                : null;
    }
}
