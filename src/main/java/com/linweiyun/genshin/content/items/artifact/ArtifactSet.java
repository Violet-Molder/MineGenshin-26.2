package com.linweiyun.genshin.content.items.artifact;

import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import net.neoforged.neoforge.registries.DeferredHolder;

public record ArtifactSet(DeferredHolder<ICharacterEffect, ?> twoPcEffect,
                          DeferredHolder<ICharacterEffect, ?> fourPcEffect, boolean hasFourPcEffect) {
}
