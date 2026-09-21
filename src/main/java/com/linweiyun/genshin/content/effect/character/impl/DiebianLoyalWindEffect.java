package com.linweiyun.genshin.content.effect.character.impl;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import net.minecraft.world.entity.player.Player;

/**
 * 蝶变 · 忠忱之风：暴击伤害 +56%，持续 10 秒。
 *
 * <p>由武器被动轮换获得（见 {@code Diebian}），退场时会被清掉。
 */
public class DiebianLoyalWindEffect implements ICharacterEffect {

    public static final int DURATION_TICKS = 10 * 20;
    /** 暴击伤害 +56%（属性里存小数，0.56 = 56%）。 */
    public static final double CRIT_DMG_BONUS = 0.56;

    private static final String SOURCE = "diebian_loyal_wind";

    @Override
    public void onEffectAdded(Player holder, PGCharacter character, CharacterEffectInstance instance) {
        character.getData().addAttributeTempFlatModifier(ModAttributes.CDG.value(), SOURCE, CRIT_DMG_BONUS);
    }

    @Override
    public void onEffectRemoved(Player holder, PGCharacter character, CharacterEffectInstance instance) {
        character.getData().removeAttributeModifier(ModAttributes.CDG.value(), SOURCE);
    }

    /** 再次获得只刷新时间，不叠层。 */
    @Override
    public void onEffectOverride(Player holder, PGCharacter character,
                                 CharacterEffectInstance existingInstance,
                                 CharacterEffectInstance newInstance) {
        existingInstance.setDuration(newInstance.getDuration());
    }
}
