package com.linweiyun.genshin.core.system.registry.register;

import com.linweiyun.genshin.content.attribute.AttributeType;
import com.linweiyun.genshin.content.items.artifact.ArtifactSet;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ArtifactSets {
    public static final DeferredRegister<ArtifactSet> ARTIFACT_SET = ModRegistries.ARTIFACT_SETS;

    public static final DeferredHolder<ArtifactSet, ArtifactSet> CRIMSON_WITCH =
            ARTIFACT_SET.register("crimson_witch", () -> new ArtifactSet(1,ModCharacterEffects.CRIMSON_WITCH2_EFFECT, ModCharacterEffects.CRIMSON_WITCH4_EFFECT,true ,true));


    public static void register(IEventBus eventBus) {
        ARTIFACT_SET.register(eventBus);
    }
}
