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

    /** 血红之证（Scarlet Proof）—— 套装编号 002，2+4 件套都有。 */
    public static final DeferredHolder<ArtifactSet, ArtifactSet> SCARLET_PROOF =
            ARTIFACT_SET.register("scarlet_proof", () -> new ArtifactSet(2, ModCharacterEffects.SCARLET_PROOF2_EFFECT, ModCharacterEffects.SCARLET_PROOF4_EFFECT, true, true));

    /** 千岩牢固（Tenacity of the Millelith）—— 套装编号 003，2+4 件套都有（UID 324003）。 */
    public static final DeferredHolder<ArtifactSet, ArtifactSet> TENACITY_OF_THE_MILLELITH =
            ARTIFACT_SET.register("tenacity_of_the_millelith", () -> new ArtifactSet(3,
                    ModCharacterEffects.TENACITY_OF_THE_MILLELITH2_EFFECT,
                    ModCharacterEffects.TENACITY_OF_THE_MILLELITH4_EFFECT, true, true));


    public static void register(IEventBus eventBus) {
        ARTIFACT_SET.register(eventBus);
    }
}
