package com.linweiyun.genshin.content.entities.teyvat.monster.slime;

import com.linweiyun.genshin.config.entity.MobBehaviorConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * 大型冰史莱姆所有技能 Goal 的公共骨架 —— 「冷却 + 随机 + 攻击欲望」只写一遍。
 *
 * <h2>三个条件</h2>
 * <ol>
 *   <li><b>冷却</b>：记一个「下次最早可用时刻」，和 {@code requiresUpdateEveryTick()} 无关，
 *       不用自己数 tick；</li>
 *   <li><b>随机</b>：{@code canUse()} 每隔一 tick 被问一次，所以不能每次都为真 ——
 *       先按 {@link #checkInterval} 抽样，再乘一次攻击欲望；</li>
 *   <li><b>欲望</b>：配置里的 {@code attack-desire} 是概率倍率，
 *       深渊环境把它调大就是「同一只怪更凶」。</li>
 * </ol>
 *
 * <p>子类只实现 {@link #extraCanUse}（额外前置条件）、{@link #chance()}（基础概率）
 * 和正常的 {@code start/tick/stop}。
 */
public abstract class CryoSlimeSkillGoal extends Goal {

    protected final LargeCryoSlime slime;

    /** 两次发动之间的最短间隔（刻）。 */
    private final int cooldownTicks;

    /** 平均每多少刻尝试一次。 */
    private final int checkInterval;

    private long nextAllowedTick;

    protected CryoSlimeSkillGoal(LargeCryoSlime slime, int cooldownTicks, int checkInterval) {
        this.slime = slime;
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.checkInterval = Math.max(1, checkInterval);
    }

    /** 基础发动概率（0~1），再乘攻击欲望。 */
    protected float chance() {
        return 1.0f;
    }

    /** 额外的发动条件（距离、目标状态之类）。 */
    protected boolean extraCanUse(LivingEntity target) {
        return true;
    }

    /** 技能结束的额外收尾（解锁动作、复位之类）。 */
    protected void onStopped() {
    }

    @Override
    public final boolean canUse() {
        if (this.slime.level().getGameTime() < this.nextAllowedTick) {
            return false;
        }
        if (!this.slime.onGround() || this.slime.isPassenger() || this.slime.isInWater()) {
            return false;
        }
        if (this.slime.getRandom().nextInt(this.checkInterval) != 0) {
            return false;
        }
        float desire = Math.max(0f, MobBehaviorConfig.attackDesire());
        if (this.slime.getRandom().nextFloat() >= this.chance() * desire) {
            return false;
        }
        LivingEntity target = this.slime.currentTarget();
        return target != null && extraCanUse(target);
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    /** 技能一旦起手就别被打断：抬手到一半断了会出现「跳一半停住」。 */
    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        this.nextAllowedTick = this.slime.level().getGameTime()
                + this.cooldownTicks + this.slime.getRandom().nextInt(this.cooldownTicks / 2 + 1);
        this.onStopped();
    }

    /** 水平距离。 */
    protected double horizontalDistanceTo(LivingEntity target) {
        double dx = target.getX() - this.slime.getX();
        double dz = target.getZ() - this.slime.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
