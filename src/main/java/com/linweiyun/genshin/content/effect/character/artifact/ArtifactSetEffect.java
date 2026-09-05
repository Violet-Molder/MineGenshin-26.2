package com.linweiyun.genshin.content.effect.character.artifact;

import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import net.minecraft.network.chat.Component;

public abstract class ArtifactSetEffect implements ICharacterEffect {

    public Component getDescription() {
        return Component.translatable("effect." + getClass().getSimpleName().toLowerCase() + ".desc");
    }
}
