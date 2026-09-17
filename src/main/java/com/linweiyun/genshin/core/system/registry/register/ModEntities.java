package com.linweiyun.genshin.core.system.registry.register;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.entities.area.StellarVortexEntity;
import com.linweiyun.genshin.content.entities.area.TalismanSpiritArea;
import com.linweiyun.genshin.content.entities.area.ThunderCloudEntity;
import com.linweiyun.genshin.content.entities.misc.ElementalOrb;
import com.linweiyun.genshin.content.entities.teyvat.monster.slime.SlimeCyro;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Minegenshin.MOD_ID);
    // 修复：使用 ResourceKey 替代 String
    public static final Supplier<EntityType<TalismanSpiritArea>> FIELD_TALISMAN_SPIRIT =
            ENTITIES.register(
                    "talisman_spirit",
                    () -> EntityType.Builder.of(TalismanSpiritArea::new, MobCategory.CREATURE)
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
    public static final Supplier<EntityType<SlimeCyro>> SLIME_CYRO =
            ENTITIES.register(
                    "slime_cyro",
                    () ->
                            EntityType.Builder.of(SlimeCyro::new, MobCategory.MONSTER)
                                    .sized(1.3964844F, 1.6F)
                                    .eyeHeight(1.52F)
                                    .passengerAttachments(1.31875F)
                                    .clientTrackingRange(10)
                                    .build(ResourceKey.create(
                                            Registries.ENTITY_TYPE,
                                            Minegenshin.id("slime_cyro")
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

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
        eventBus.addListener(ModEntities::registerEntityAttributes);
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(SLIME_CYRO.get(), SlimeCyro.createAttributes().build());
    }
}