package com.linweiyun.genshin.content.entities.teyvat.monster.slime;

import com.linweiyun.genshin.config.entity.MobBehaviorConfig;
import com.linweiyun.genshin.content.skill_node.IceMistField;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * <b>技能二：冰雾</b> —— 朝身前喷出一片持续存在的冰雾。
 *
 * <p>形状 3×3×<b>1</b>（只有最底下一层），持续 {@code mist-duration} 秒，
 * 每 0.5 秒结算一次攻击力 × 0.3 的冰元素伤害，并给玩家的<b>场上角色</b>挂弱冰附着。
 *
 * <p>真正的表现与结算都在技能节点 {@link IceMistField} 里，这个 Goal 只负责
 * 「什么时候放、放在哪」。
 */
public class CryoSlimeMistGoal extends CryoSlimeSkillGoal {

    /** 抬手刻数。 */
    private static final int WINDUP_TICKS = 14;

    /** 冰雾铺在身前几格。 */
    private static final double FORWARD_OFFSET = 3.0D;

    private int elapsed;

    private boolean spawned;

    public CryoSlimeMistGoal(LargeCryoSlime slime) {
        super(slime, MobBehaviorConfig.mistCooldownTicks(), 16);
    }

    @Override
    protected boolean extraCanUse(LivingEntity target) {
        // 近中距离才喷：太远喷了也白喷
        double distance = horizontalDistanceTo(target);
        return distance >= 2.0D && distance <= 14.0D;
    }

    @Override
    protected float chance() {
        return 0.55f;
    }

    @Override
    public boolean canContinueToUse() {
        return this.elapsed <= WINDUP_TICKS + 20;
    }

    @Override
    public void start() {
        this.elapsed = 0;
        this.spawned = false;
        this.slime.getNavigation().stop();
        this.slime.setAggressive(true);
    }

    @Override
    public void tick() {
        this.elapsed++;
        LivingEntity target = this.slime.currentTarget();
        if (target != null) {
            this.slime.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (!this.spawned && this.elapsed >= WINDUP_TICKS) {
            this.spawned = true;
            spray();
        }
    }

    private void spray() {
        if (!(this.slime.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 forward = this.slime.getLookAngle();
        Vec3 center = new Vec3(
                this.slime.getX() + forward.x * FORWARD_OFFSET,
                this.slime.getY(),
                this.slime.getZ() + forward.z * FORWARD_OFFSET);
        IceMistField.spawn(serverLevel, center, MobBehaviorConfig.mistDurationTicks(),
                this.slime, this.slime.attackDamageValue());
    }

    @Override
    protected void onStopped() {
        this.slime.setAggressive(false);
        this.slime.getNavigation().stop();
    }
}
