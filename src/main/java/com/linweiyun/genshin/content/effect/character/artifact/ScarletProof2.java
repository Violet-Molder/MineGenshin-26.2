package com.linweiyun.genshin.content.effect.character.artifact;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import net.minecraft.world.entity.player.Player;

/**
 * 血红之证 · 二件套：攻击力提升 18%。
 *
 * <p>攻击力是百分比特效（{@code +18%}），所以走 <b>百分比</b>修饰符
 * （元素伤害加成那种「本身就是个百分数」的属性才用 flat，见魔女套）。
 */
public class ScarletProof2 extends ArtifactSetEffect {

    private static final String SOURCE = "scarlet_proof2";
    private static final double ATK_PERCENT = 0.18;

    @Override
    public void onEffectAdded(Player holder, PGCharacter character, CharacterEffectInstance instance) {
        character.getData().addAttributePercentModifier(ModAttributes.ATK.value(), SOURCE, ATK_PERCENT);
    }

    @Override
    public void onEffectRemoved(Player holder, PGCharacter character, CharacterEffectInstance instance) {
        character.getData().removeAttributeModifier(ModAttributes.ATK.value(), SOURCE);
    }
}
