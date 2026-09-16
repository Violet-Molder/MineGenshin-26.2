package com.linweiyun.genshin.core.system.registry.register;

import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.content.effect.character.artifact.CrimsonWitch2;
import com.linweiyun.genshin.content.effect.character.artifact.CrimsonWitch4;
import com.linweiyun.genshin.content.effect.character.impl.DamageBonusEffect;
import com.linweiyun.genshin.content.effect.character.shenhe.IcyQuillEffect;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCharacterEffects {
    public static final DeferredRegister<ICharacterEffect> CHARACTER_EFFECTS = ModRegistries.CHARACTER_EFFECTS;
    public static final DeferredHolder<ICharacterEffect, IcyQuillEffect> ICY_QUILL_EFFECT = CHARACTER_EFFECTS.register(
            "icy_quill",
            IcyQuillEffect::new
    );

    // ======== 通用效果 ========
    public static final DeferredHolder<ICharacterEffect, DamageBonusEffect> DAMAGE_BONUS_EFFECT = CHARACTER_EFFECTS.register(
            "damage_bonus",
            () -> new DamageBonusEffect()
    );

    // ======= 圣遗物套装效果 =======
    public static final DeferredHolder<ICharacterEffect, CrimsonWitch2> CRIMSON_WITCH2_EFFECT = CHARACTER_EFFECTS.register(
            "crimson_witch2",
            CrimsonWitch2::new
    );
    public static final DeferredHolder<ICharacterEffect, CrimsonWitch4> CRIMSON_WITCH4_EFFECT = CHARACTER_EFFECTS.register(
            "crimson_witch4",
            CrimsonWitch4::new
    );

    public static void register(IEventBus eventBus) {
        CHARACTER_EFFECTS.register(eventBus);
    }
}