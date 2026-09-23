package com.linweiyun.genshin.content.entities.test;

import com.linweiyun.genshin.content.entities.ai.goal.ApproachTargetGoal;
import com.linweiyun.genshin.content.entities.ai.control.WriggleMoveControl;
import com.linweiyun.genshin.content.entities.teyvat.monster.TeyvatMonster;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * 2 号测试实体 {@code minegenshin:test2} —— 和 test1 同一副身体，技能换成「召唤冰块砸人」。
 *
 * <h2>与 test1 的差别</h2>
 * <table border="1">
 *   <caption>两只对比</caption>
 *   <tr><th></th><th>test1</th><th>test2</th></tr>
 *   <tr><td>移动</td><td colspan="2">一样（{@link WriggleMoveControl} 蠕动 / 小跳）</td></tr>
 *   <tr><td>接近</td><td colspan="2">一样（{@link ApproachTargetGoal}）</td></tr>
 *   <tr><td>出招</td><td>贴到 2 格：大跳撞击</td><td><b>远程</b>：随机发动，抬手召唤冰块后砸过去</td></tr>
 * </table>
 *
 * <p>「随机发动 + CD」在 {@link SummonIceBlockGoal} 里，做成独立 Goal 的好处是
 * 它和「接近」天然分工：远程技能不占位移，怪可以一边走一边放。
 */
public class Test2Entity extends TestMonster {

    /** 资源 id。 */
    //TEMP
    public static final String ASSET_ID = "test2";

    //TEMP
    public Test2Entity(EntityType<? extends TeyvatMonster> type, Level level) {
        super(type, level);
        this.moveControl = new WriggleMoveControl<Test2Entity>(this);

        // 和 test1 一样：不飞、不游
        this.navigation.setCanFloat(false);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    //TEMP
    @Override
    public String assetId() {
        return ASSET_ID;
    }

    //TEMP
    public static AttributeSupplier.Builder createAttributes() {
        return createTestAttributes()
                // 比 test1 脆一点、慢一点，换来的是一手远程
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    // ==================== AI ====================

    /**
     * 优先级 1 是远程技能，优先级 2 才是接近 —— 因为技能不占 MOVE 标志位，
     * 两者可以同时跑：边走边放。
     */
    //TEMP
    @Override
    protected void registerGoals() {
        // 1：随机发动，抬手召唤冰块
        this.goalSelector.addGoal(1, new SummonIceBlockGoal(this));
        // 2：只走不打，负责把距离拉进技能射程
        this.goalSelector.addGoal(2, new ApproachTargetGoal(this, 0.6D, SummonIceBlockGoal.CAST_RANGE * 0.5D));
        // 4：没目标时漫游
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    //TEMP
    @Override
    protected TestAction defaultAction() {
        boolean moving = isMovingHorizontally();
        if (this.getTarget() != null) {
            return moving ? TestAction.HOP : TestAction.IDLE;
        }
        return moving ? TestAction.WRIGGLE : TestAction.IDLE;
    }

    // ==================== 技能入口 ====================

    /**
     * 真正把冰块召出来。
     *
     * <p>由 {@link SummonIceBlockGoal} 在抬手动作结束时调用 —— 抬手和「冰块出现」
     * 分成两步，是为了让动画上的「召唤」有明确的时刻。
     */
    //TEMP
    public void summonIceBlock(LivingEntity target) {
        IceBlockProjectile ice = IceBlockProjectile.create(this.level(), this, target);
        if (ice != null) {
            this.level().addFreshEntity(ice);
        }
    }

    @Override
    public boolean onAttachElement(GenshinElement element, AttachmentSource source, AttachmentProfile profile) {
        return false;
    }
}
