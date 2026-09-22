package com.linweiyun.genshin.content.entities.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * 「接近目标」—— 只负责走过去，不负责出手。
 *
 * <h2>为什么原版没有这个 Goal</h2>
 * 原版把「走过去」和「出手」缝在同一个 Goal 里（{@link net.minecraft.world.entity.ai.goal.MeleeAttackGoal}）：
 * 它一边寻路一边在贴脸时 {@code doHurtTarget}。这对「挥手打人」是对的，
 * 但一旦攻击表现<b>不是挥手</b>（例如本 MOD 的位移撞击），两件事就必须拆开：
 * 接近交给本 Goal，出招交给出招 Goal，两者用优先级交接。
 *
 * <p>拆开的好处：出招 Goal 完全不用关心寻路，专心演动作；接近 Goal 也不关心动作，
 * 一条逻辑对全部出招复用。
 *
 * <h2>交接点</h2>
 * 与出招 Goal 的距离阈值相等即可无缝衔接：本 Goal 在
 * {@code 距离 > stopDistance} 时运行，出招 Goal 在 {@code 距离 <= 触发距离} 时运行，
 * 两者刚好互补，中间没有「谁都不管」的空档。
 */
public class ApproachTargetGoal extends Goal {

    //TEMP
    private final PathfinderMob mob;

    //TEMP
    private final double speedModifier;

    /** 走到这个水平距离就收工，把位置让给出招 Goal。 */
    //TEMP
    private final double stopDistance;

    //TEMP
    private int repathCooldown;

    //TEMP
    public ApproachTargetGoal(PathfinderMob mob, double speedModifier, double stopDistance) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    //TEMP
    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && horizontalDistanceTo(target) > this.stopDistance;
    }

    //TEMP
    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    //TEMP
    @Override
    public boolean requiresUpdateEveryTick() {
        return false;
    }

    //TEMP
    @Override
    public void start() {
        this.repathCooldown = 0;
        this.mob.setAggressive(true);
    }

    //TEMP
    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // 不用每 tick 重算路径：目标挪动不大时照旧走，挪远了再重算
        if (--this.repathCooldown <= 0) {
            this.repathCooldown = this.adjustedTickDelay(6);
            this.mob.getNavigation().moveTo(target, this.speedModifier);
        }
    }

    //TEMP
    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    //TEMP
    private double horizontalDistanceTo(LivingEntity target) {
        double dx = target.getX() - this.mob.getX();
        double dz = target.getZ() - this.mob.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
