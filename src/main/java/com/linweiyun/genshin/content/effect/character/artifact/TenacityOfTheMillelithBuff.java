package com.linweiyun.genshin.content.effect.character.artifact;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import net.minecraft.world.entity.player.Player;

/**
 * 千岩牢固 · 四件套的<b>触发 buff</b>：元素战技命中敌人后 3 秒内攻击力 +20%。
 *
 * <p><b>关于「护盾强效提升 30%」</b>：本 MOD 目前没有护盾及其相关机制，
 * 所以这一份暂时<b>未实现</b>；将来做护盾系统时在这里补上（加一个护盾强效属性并在这里一起给）。
 *
 * <p>再触发一次只刷新时长（不叠层），和 {@code ScarletProofBuffEffect} 同一口径。
 */
public class TenacityOfTheMillelithBuff implements ICharacterEffect {

    /** 攻击力 +20%（ATK 存的是小数，0.20 = 20%）。 */
    public static final double ATK_PERCENT = 0.20;

    private static final String ATK_SOURCE = "tenacity_of_the_millelith4_atk";

    @Override
    public void onEffectAdded(Player holder, PGCharacter character, CharacterEffectInstance instance) {
        character.getData().addAttributePercentModifier(ModAttributes.ATK.value(), ATK_SOURCE, ATK_PERCENT);
    }

    @Override
    public void onEffectRemoved(Player holder, PGCharacter character, CharacterEffectInstance instance) {
        character.getData().removeAttributeModifier(ModAttributes.ATK.value(), ATK_SOURCE);
    }

    /** 再触发只刷新时间（不叠加）。 */
    @Override
    public void onEffectOverride(Player holder, PGCharacter character,
                                 CharacterEffectInstance existingInstance,
                                 CharacterEffectInstance newInstance) {
        newInstance.setDuration(TenacityOfTheMillelith4.BUFF_DURATION_TICKS);
    }
}
