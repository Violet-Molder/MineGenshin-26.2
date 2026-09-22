package com.linweiyun.genshin.content.entities.test;

import com.linweiyun.genshin.content.entities.ai.goal.LeapSmashGoal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

/**
 * test1 专用的扑击 —— 把通用 {@link LeapSmashGoal} 的四个钩子接到「动画状态 + 撞击伤害」上。
 *
 * <pre>
 * 起跳   → attack.leap   锁定
 * 撞上   → attack.smash  锁定，走 {@link Test1Entity#smashAttack} 的重击伤害
 * 掉头   → attack.land   锁定
 * 落地   → 解锁，交回给移动状态推导（待机 / 蠕动 / 小跳）
 * </pre>
 *
 * <p>锁定是必须的：解锁之后 {@link TestMonster#defaultAction()} 会立刻按移动状态改动作，
 * 中途解锁会出现「跳到一半变成蠕动」的跳帧。
 */
public class Test1LeapSmashGoal extends LeapSmashGoal<Test1Entity> {

    //TEMP
    public Test1LeapSmashGoal(Test1Entity mob) {
        super(mob, Test1Entity.SMASH_DISTANCE);
    }

    //TEMP
    @Override
    protected void onLeapStarted() {
        this.mob.lockAction(TestAction.LEAP);
    }

    /** 撞击：走重击伤害（原版伤害源 × 倍数），不用通用的 {@code doHurtTarget}。 */
    //TEMP
    @Override
    protected void smash(LivingEntity target) {
        this.mob.swing(InteractionHand.MAIN_HAND);
        this.mob.smashAttack(target);
        this.mob.lockAction(TestAction.SMASH);
    }

    //TEMP
    @Override
    protected void beginReturn() {
        super.beginReturn();
        this.mob.lockAction(TestAction.LAND);
    }

    //TEMP
    @Override
    protected void onLeapFinished() {
        this.mob.unlockAction();
    }
}
