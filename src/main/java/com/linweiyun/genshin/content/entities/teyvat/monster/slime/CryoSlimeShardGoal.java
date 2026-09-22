package com.linweiyun.genshin.content.entities.teyvat.monster.slime;

import com.linweiyun.genshin.config.entity.MobBehaviorConfig;
import com.linweiyun.genshin.content.entities.test.IceBlockProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * <b>技能一：冰刺三连</b> —— 朝目标发射若干枚缩小版冰块。
 *
 * <p>就是把 {@code IceBlockProjectile} 的渲染缩放调到约 1/3、蓄力时间设为 0
 * （生成即飞），一次发 {@code shard-count} 枚、每 {@link #FIRE_INTERVAL} 刻一枚 ——
 * 三枚之间错开几刻，看起来才像「连发」而不是「一团」。
 *
 * <p>命中伤害由投射物自己结算（原版伤害源 → 按攻击力换算），倍率 1.0。
 */
public class CryoSlimeShardGoal extends CryoSlimeSkillGoal {

    /** 冰刺的渲染缩放（约为整块冰的 1/3）。 */
    //TEMP
    private static final float SHARD_SCALE = 1.0F / 3.0F;

    /** 抬手刻数（抬手完才开始出冰刺）。 */
    //TEMP
    private static final int WINDUP_TICKS = 12;

    /** 两枚之间的间隔刻数：够前一枚升起来，能看清「左中右」三根。 */
    //TEMP
    private static final int FIRE_INTERVAL = 10;

    /** 三根冰刺的横向间距（格）。 */
    //TEMP
    private static final double SIDE_SPACING = 0.9D;

    /** 发射距离范围。 */
    //TEMP
    private static final double MIN_RANGE = 3.0D;
    //TEMP
    private static final double MAX_RANGE = 24.0D;

    //TEMP
    private int elapsed;

    //TEMP
    private int fired;

    //TEMP
    private int total;

    //TEMP
    public CryoSlimeShardGoal(LargeCryoSlime slime) {
        super(slime, MobBehaviorConfig.shardCooldownTicks(), 14);
    }

    //TEMP
    @Override
    protected float chance() {
        return 0.6f;
    }

    //TEMP
    @Override
    protected boolean extraCanUse(LivingEntity target) {
        double distance = horizontalDistanceTo(target);
        return distance >= MIN_RANGE && distance <= MAX_RANGE;
    }

    //TEMP
    @Override
    public boolean canContinueToUse() {
        return this.elapsed <= WINDUP_TICKS + this.total * FIRE_INTERVAL + 10;
    }

    //TEMP
    @Override
    public void start() {
        this.elapsed = 0;
        this.fired = 0;
        this.total = MobBehaviorConfig.shardCount();
        this.slime.getNavigation().stop();
        this.slime.setAggressive(true);
    }

    //TEMP
    @Override
    public void tick() {
        this.elapsed++;
        LivingEntity target = this.slime.currentTarget();
        if (target != null) {
            this.slime.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (this.elapsed < WINDUP_TICKS) {
            return;
        }
        // 抬手结束后开始连发
        int expected = (this.elapsed - WINDUP_TICKS) / FIRE_INTERVAL;
        while (this.fired < Math.min(expected, this.total)) {
            this.fired++;
            if (target != null && target.isAlive()) {
                fire(target, this.fired);
            }
        }
    }

    //TEMP
    private void fire(LivingEntity target, int index) {
        if (!(this.slime.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // 生成 → 从身体内部升起 → 转两圈 → 再飞出去（chargeTicks 给足，别一出来就射）
        IceBlockProjectile shard = IceBlockProjectile.create(
                serverLevel, this.slime, target, SHARD_SCALE, IceBlockProjectile.CHARGE_TICKS);
        if (shard == null) {
            return;
        }
        shard.withChargeOffset(shardOffset(index));
        serverLevel.addFreshEntity(shard);
    }

    /** 以施法者为原点，沿身体右方向错开成「左 / 中 / 右」三根。 */
    //TEMP
    private Vec3 shardOffset(int index) {
        double side = (index - (this.total + 1) / 2.0D) * SIDE_SPACING;
        double rightX = Math.cos(Math.toRadians(this.slime.getYRot()));
        double rightZ = Math.sin(Math.toRadians(this.slime.getYRot()));
        return new Vec3(rightX * side, 0.0D, rightZ * side);
    }

    //TEMP
    @Override
    protected void onStopped() {
        this.slime.setAggressive(false);
        this.slime.getNavigation().stop();
    }
}
