package com.linweiyun.genshin.content.effect.character.artifact;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import net.minecraft.world.entity.player.Player;

/**
 * 千岩牢固 · 二件套：生命值上限提升 20%。
 *
 * <p>生命值上限是百分比特效（{@code +20%}），所以走<b>百分比</b>修饰符
 * （和血红之证二件套的攻击力 +18% 同一写法）。
 */
public class TenacityOfTheMillelith2 extends ArtifactSetEffect {

    private static final String SOURCE = "tenacity_of_the_millelith2";
    private static final double HP_PERCENT = 0.20;

    @Override
    public void onEffectAdded(Player holder, PGCharacter character, CharacterEffectInstance instance) {
        character.getData().addAttributePercentModifier(ModAttributes.MAX_HP.value(), SOURCE, HP_PERCENT);
    }

    @Override
    public void onEffectRemoved(Player holder, PGCharacter character, CharacterEffectInstance instance) {
        character.getData().removeAttributeModifier(ModAttributes.MAX_HP.value(), SOURCE);
    }
}
