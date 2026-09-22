package com.linweiyun.genshin.content.entities.test;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * test2 的远程技能：「随机发动 + CD」地召唤一块冰砸向目标。
 *
 * <h2>为什么可以只占 LOOK 不占 MOVE</h2>
 * 远程技能不需要位移，所以这个 Goal <b>不声明 MOVE</b> —— 于是它可以和
 * {@link com.linweiyun.genshin.content.entities.ai.goal.ApproachTargetGoal}
 * 同时运行：一边走位一边施法。这正是「标志位只声明真正要用的」带来的好处；
 * 如果习惯性写上 MOVE，两者就会互相抢占，表现成「走两步停一下」。
 *
 * <h2>随机与冷却怎么分开</h2>
 * <ul>
 *   <li><b>冷却</b>：用 {@code level().getGameTime()} 记一个「下次最早可用的时刻」，
 *       打完就推到 {@code now + cooldown}，与 {@code requiresUpdateEveryTick()} 无关，
 *       不用自己数 tick；</li>
 *   <li><b>随机</b>：{@code canUse()} 会被 {@code GoalSelector} 每隔一 tick 问一次，
 *       所以不能每次都为真（否则 CD 一到就立刻放），这里用
 *       {@code random.nextInt(checkInterval) == 0} 把它摊成「平均每 checkInterval 刻试一次」。</li>
 * </ul>
 *
 * <h2>抬手 → 出冰</h2>
 * {@code start()} 只锁动画，真正生成投射物在 {@code windupTicks} 之后 ——
 * 这样动画上的「抬手」和冰块出现是同一个时刻，而不是同时发生。
 */
public class SummonIceBlockGoal extends Goal {

    /** 技能射程：目标在这个距离内才会起手。 */
    //TEMP
    public static final double CAST_RANGE = 14.0D;

    /** 抬手刻数（动画 attack.cast 的长度，大致对齐即可）。 */
    //TEMP
    private static final int WINDUP_TICKS = 10;

    /** 冷却区间（刻）。 */
    //TEMP
    private static final int COOLDOWN_MIN = 60;
    //TEMP
    private static final int COOLDOWN_MAX = 100;

    /** 平均每多少刻尝试发动一次。 */
    //TEMP
    private static final int CHECK_INTERVAL = 30;

    //TEMP
    private final Test2Entity mob;

    //TEMP
    private int elapsed;

    //TEMP
    private boolean summoned;

    //TEMP
    private long nextAllowedTick;

    //TEMP
    public SummonIceBlockGoal(Test2Entity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    //TEMP
    @Override
    public boolean canUse() {
        if (this.mob.level().getGameTime() < this.nextAllowedTick) {
            return false;
        }
        if (!this.mob.onGround() || this.mob.isPassenger() || this.mob.isInWater()) {
            return false;
        }
        if (this.mob.getRandom().nextInt(CHECK_INTERVAL) != 0) {
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = horizontalDistanceTo(target);
        return distance <= CAST_RANGE;
    }

    //TEMP
    @Override
    public boolean canContinueToUse() {
        return this.elapsed <= WINDUP_TICKS + 5;
    }

    /** 抬手期间不要被打断：手抬一半被打断，冰块就不出来了。 */
    //TEMP
    @Override
    public boolean isInterruptable() {
        return false;
    }

    //TEMP
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    //TEMP
    @Override
    public void start() {
        this.elapsed = 0;
        this.summoned = false;
        this.mob.lockAction(TestAction.CAST);
        this.mob.getNavigation().stop();
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    //TEMP
    @Override
    public void tick() {
        this.elapsed++;
        LivingEntity target = this.mob.getTarget();
        if (target != null && target.isAlive()) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        if (!this.summoned && this.elapsed >= WINDUP_TICKS) {
            this.summoned = true;
            if (target != null && target.isAlive()) {
                this.mob.summonIceBlock(target);
            }
        }
    }

    //TEMP
    @Override
    public void stop() {
        this.mob.unlockAction();
        this.nextAllowedTick = this.mob.level().getGameTime()
                + COOLDOWN_MIN + this.mob.getRandom().nextInt(COOLDOWN_MAX - COOLDOWN_MIN + 1);
    }

    //TEMP
    private double horizontalDistanceTo(LivingEntity target) {
        double dx = target.getX() - this.mob.getX();
        double dz = target.getZ() - this.mob.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
