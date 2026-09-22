package com.linweiyun.genshin.content.entities.test;

import com.linweiyun.genshin.Minegenshin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 测试实体的注册表。
 *
 * <p>刻意<b>不</b>塞进 {@code ModEntities}：测试实体是「技术验证用」的，
 * 与正式内容分开放，将来整包删掉时只需要删掉 {@code content/entities/test/} 一个目录，
 * 外加主类里的一行注册调用。
 *
 * <p>注册顺序无关 —— {@link DeferredRegister} 会在 mod 总线加载阶段统一挂上去。
 * 属性必须走 {@link EntityAttributeCreationEvent}，漏了会在实体首次生成时崩
 * （{@code Entity ... has no attributes}）。
 */
public class TestEntities {

    //TEMP
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Minegenshin.MOD_ID);

    /**
     * 1 号测试实体：敌对、地面、蠕动位移，2 格内大跳撞击。
     *
     * <p>{@code sized} 是碰撞箱（宽 × 高）。{@code eyeHeight} 影响视线判定、
     * {@code clientTrackingRange} 是客户端追踪距离（单位：区块）。
     */
    //TEMP
    public static final DeferredHolder<EntityType<?>, EntityType<Test1Entity>> TEST1 =
            ENTITIES.register(
                    "test1",
                    () -> EntityType.Builder.of(Test1Entity::new, MobCategory.MONSTER)
                            .sized(1.0F, 1.2F)
                            .eyeHeight(0.9F)
                            .clientTrackingRange(10)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Minegenshin.id(Test1Entity.ASSET_ID)))
            );

    /**
     * 2 号测试实体：远程技能，随机发动、抬手召唤冰块砸人。
     */
    //TEMP
    public static final DeferredHolder<EntityType<?>, EntityType<Test2Entity>> TEST2 =
            ENTITIES.register(
                    "test2",
                    () -> EntityType.Builder.of(Test2Entity::new, MobCategory.MONSTER)
                            .sized(1.0F, 1.2F)
                            .eyeHeight(0.9F)
                            .clientTrackingRange(10)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Minegenshin.id(Test2Entity.ASSET_ID)))
            );

    /**
     * 技能投射物：一块悬空旋转、砸向玩家的原版冰块。
     *
     * <p>用 {@link MobCategory#MISC}：它不是生物，不占刷怪名额，也不需要属性
     * （属性缺失的报错只针对 CREATURE / MONSTER 之类的生物类别）。
     * {@code updateInterval} 给小值让位置同步跟得上高速飞行。
     */
    //TEMP
    public static final DeferredHolder<EntityType<?>, EntityType<IceBlockProjectile>> ICE_BLOCK =
            ENTITIES.register(
                    "ice_block",
                    () -> EntityType.Builder.<IceBlockProjectile>of(IceBlockProjectile::new, MobCategory.MISC)
                            .sized(0.9F, 0.9F)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Minegenshin.id("ice_block")))
            );

    //TEMP
    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
        eventBus.addListener(TestEntities::registerEntityAttributes);
    }

    //TEMP
    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(TEST1.get(), Test1Entity.createAttributes().build());
        event.put(TEST2.get(), Test2Entity.createAttributes().build());
    }
}
