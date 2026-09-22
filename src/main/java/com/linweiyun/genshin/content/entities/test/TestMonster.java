package com.linweiyun.genshin.content.entities.test;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.linweiyun.genshin.content.entities.teyvat.monster.TeyvatMonster;
import com.linweiyun.genshin.core.asset.AssetCategory;
import com.linweiyun.genshin.core.asset.AssetSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 测试实体的公共骨架 —— GeckoLib 接入 + 「动作状态」同步。
 *
 * <h2>为什么要有动作状态同步</h2>
 * 动画在客户端算，而「有没有目标」「是不是在扑击」这些判断在服务端。
 * 原版 {@code Mob#getTarget()} <b>不</b>同步给客户端，所以客户端直接读它永远是 null。
 * 这里把动作压成一个 int 写进 {@link SynchedEntityData}，两端读到的就是同一个值。
 *
 * <h2>锁定机制</h2>
 * 默认动作（待机 / 蠕动 / 小跳）由每 tick 的移动状态推导，但扑击这种<b>有始有终</b>的动作
 * 不能被推导覆盖，所以扑击期间用 {@link #lockAction(TestAction)} 锁住，结束再解锁。
 *
 * <p>资源全部按 {@code entity/<assetId>/} 取，见 {@link #assetSet()}。
 */
public abstract class TestMonster extends TeyvatMonster implements GeoEntity {

    /** 动作状态；存的是 {@link TestAction} 的顺序号。 */
    //TEMP
    private static final EntityDataAccessor<Integer> DATA_ACTION =
            SynchedEntityData.defineId(TestMonster.class, EntityDataSerializers.INT);

    //TEMP
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** 非 null 时禁止默认动作推导覆盖它。 */
    //TEMP
    @Nullable
    private TestAction lockedAction;

    //TEMP
    protected TestMonster(EntityType<? extends TeyvatMonster> type, Level level) {
        super(type, level);
    }

    // ==================== 资源 ====================

    /** 资源 id，等于 {@code assets/minegenshin/entity/<id>/} 的目录名。 */
    //TEMP
    public abstract String assetId();

    /** 这个实体的三件套：{@code entity/<assetId>/<assetId>.{json,animation.json,png}}。 */
    //TEMP
    public AssetSet assetSet() {
        return AssetSet.of(AssetCategory.ENTITY, assetId());
    }

    /** 全部测试实体共用的属性底色；子类在 {@code createAttributes()} 里继续 {@code add}。 */
    //TEMP
    public static AttributeSupplier.Builder createTestAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    // ==================== 动作状态 ====================

    //TEMP
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ACTION, TestAction.IDLE.ordinal());
    }

    //TEMP
    public TestAction getAction() {
        return TestAction.byOrdinal(this.entityData.get(DATA_ACTION));
    }

    //TEMP
    public void setAction(TestAction action) {
        this.entityData.set(DATA_ACTION, action.ordinal());
    }

    /** 锁住动作：这段时间里 {@link #defaultAction()} 不会被写进去。 */
    //TEMP
    public void lockAction(TestAction action) {
        this.lockedAction = action;
        this.setAction(action);
    }

    //TEMP
    public void unlockAction() {
        this.lockedAction = null;
    }

    //TEMP
    public boolean isActionLocked() {
        return this.lockedAction != null;
    }

    //TEMP
    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (!isActionLocked()) {
            setAction(defaultAction());
        }
    }

    /**
     * 没有锁定时的动作 —— 子类按自己的走法覆写。
     *
     * <p>默认「不动就待机、在动就小跳」，子类一般会区分「有目标 / 无目标」。
     */
    //TEMP
    protected TestAction defaultAction() {
        return isMovingHorizontally() ? TestAction.HOP : TestAction.IDLE;
    }

    /** 水平方向是不是在动。 */
    //TEMP
    protected boolean isMovingHorizontally() {
        if (this.walkAnimation.speed() > 0.02F) {
            return true;
        }
        var delta = this.getDeltaMovement();
        return delta.x * delta.x + delta.z * delta.z > 1.0E-4D;
    }

    // ==================== GeckoLib ====================

    //TEMP
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    /**
     * 一个控制器管全部动作。
     *
     * <p>不拆多个控制器：动作之间是互斥的（在扑击就不可能同时在蠕动），
     * 拆开会变成多个控制器同时播放、骨骼互相覆盖。
     */
    //TEMP
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("action", 3, this::actionController));
    }

    //TEMP
    private PlayState actionController(AnimationTest<GeoAnimatable> test) {
        return test.setAndContinue(getAction().animation());
    }
}
