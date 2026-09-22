package com.linweiyun.genshin.content.entities;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.entities.area.StellarVortexEntity;
import com.linweiyun.genshin.content.entities.area.TalismanSpiritArea;
import com.linweiyun.genshin.content.entities.area.ThunderCloudEntity;
import com.linweiyun.genshin.content.entities.misc.ElementalOrb;
import com.linweiyun.genshin.content.entities.teyvat.monster.slime.LargeCryoSlime;
import com.linweiyun.genshin.content.entities.teyvat.skill.vesna.VesnaAttackProjectile;
import com.linweiyun.genshin.content.entities.teyvat.skill.vesna.VesnaSpiritSwordEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Minegenshin.MOD_ID);
    // 修复：使用 ResourceKey 替代 String
    // MobCategory 用 MISC：它是「领域」不是生物。用 CREATURE 会让 NeoForge 在加载期
    // 报 `Entity minegenshin:talisman_spirit has no attributes`（那句只是噪音，
    // 但领域本来就不该占生物的类别/刷怪名额）。
    public static final Supplier<EntityType<TalismanSpiritArea>> FIELD_TALISMAN_SPIRIT =
            ENTITIES.register(
                    "talisman_spirit",
                    () -> EntityType.Builder.of(TalismanSpiritArea::new, MobCategory.MISC)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Minegenshin.id("talisman_spirit"))
                            )
            );
    public static final Supplier<EntityType<ElementalOrb>> ELEMENTAL_ORB =
            ENTITIES.register(
                    "elemental_orb",
                    () -> EntityType.Builder.<ElementalOrb>of(ElementalOrb::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .eyeHeight(0.125F)
                            .clientTrackingRange(6)
                            .updateInterval(20)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Minegenshin.id("elemental_orb"))
                            )
            );
    /**
     * 大型冰史莱姆。
     *
     * <p>注册 id 就是 {@code large_cryo_slime}，资源文件名也跟着它
     * （{@code geckolib/models/entity/large_cryo_slime.geo.json} 等，
     * GeckoLib 的默认路径按实体 id 推导）。
     */
    //TEMP
    public static final Supplier<EntityType<LargeCryoSlime>> LARGE_CRYO_SLIME =
            ENTITIES.register(
                    "large_cryo_slime",
                    () ->
                            EntityType.Builder.of(LargeCryoSlime::new, MobCategory.MONSTER)
                                    .sized(1.3964844F, 1.6F)
                                    .eyeHeight(1.52F)
                                    .passengerAttachments(1.31875F)
                                    .clientTrackingRange(10)
                                    .build(ResourceKey.create(
                                            Registries.ENTITY_TYPE,
                                            Minegenshin.id("large_cryo_slime")
                                    )));

    public static final Supplier<EntityType<ThunderCloudEntity>> THUNDER_CLOUD =
            ENTITIES.register(
                    "thunder_cloud",
                    () -> EntityType.Builder.<ThunderCloudEntity>of(ThunderCloudEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Minegenshin.id("thunder_cloud"))
                            ));

    public static final Supplier<EntityType<StellarVortexEntity>> STELLAR_VORTEX =
            ENTITIES.register(
                    "stellar_vortex",
                    () -> EntityType.Builder.<StellarVortexEntity>of(StellarVortexEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Minegenshin.id("stellar_vortex"))
                            ));

    public static final Supplier<EntityType<VesnaAttackProjectile>> VESNA_ATTACK_PROJECTILE =
            ENTITIES.register(
                    "vesna_attack_projectile",
                    () -> EntityType.Builder.<VesnaAttackProjectile>of(VesnaAttackProjectile::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Minegenshin.id("vesna_attack_projectile"))
                            ));

    public static final Supplier<EntityType<VesnaSpiritSwordEntity>> VESNA_SPIRIT_SWORD =
            ENTITIES.register(
                    "vesna_spirit_sword",
                    () -> EntityType.Builder.<VesnaSpiritSwordEntity>of(VesnaSpiritSwordEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Minegenshin.id("vesna_spirit_sword"))
                            ));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
        eventBus.addListener(ModEntities::registerEntityAttributes);
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(LARGE_CRYO_SLIME.get(), LargeCryoSlime.createAttributes().build());
    }
}