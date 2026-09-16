package com.linweiyun.genshin.core.system.registry.register;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.items.component.ArtifactStatsComponent;
import com.linweiyun.genshin.content.items.component.WeaponStatsComponent;
import com.linweiyun.genshin.content.items.food.CharacterFoodProperties;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTRAR =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Minegenshin.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ArtifactStatsComponent>> ARTIFACT_STATS =
            REGISTRAR.registerComponentType(
                    "artifact_stats",
                    builder -> builder
                            .persistent(ArtifactStatsComponent.CODEC)
                            .networkSynchronized(ArtifactStatsComponent.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponStatsComponent>> WEAPON_STATS =
            REGISTRAR.registerComponentType(
                    "weapon_stats",
                    builder -> builder
                            .persistent(WeaponStatsComponent.CODEC)
                            .networkSynchronized(WeaponStatsComponent.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CharacterFoodProperties>> CHARACTER_FOOD =
            REGISTRAR.registerComponentType(
                    "character_food",
                    builder -> builder
                            .persistent(CharacterFoodProperties.CODEC)
                            .networkSynchronized(CharacterFoodProperties.STREAM_CODEC)
            );

    public static void register(IEventBus bus) {
        REGISTRAR.register(bus);
    }
}