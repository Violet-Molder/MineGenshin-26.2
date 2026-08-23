package com.linweiyun.genshin.registry.register;

import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.content.effect.character.shenhe.IcyQuillEffect;
import com.linweiyun.genshin.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CharacterEffectRegister {
    public static final DeferredRegister<ICharacterEffect> CHARACTER_EFFECTS = ModRegistries.CHARACTER_EFFECTS;
    public static final DeferredHolder<ICharacterEffect, IcyQuillEffect> ICY_QUILL_EFFECT = CHARACTER_EFFECTS.register(
            "icy_quill",
            IcyQuillEffect::new
    );
    public static void register(IEventBus eventBus) {
        CHARACTER_EFFECTS.register(eventBus);
    }
}
