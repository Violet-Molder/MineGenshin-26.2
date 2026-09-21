package com.linweiyun.genshin.content.effect.character.impl;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.reaction.StellarGlimmerBranch;
import net.minecraft.world.entity.player.Player;

/**
 * 蝶变 · 叛弃之风：装备者造成的<b>星扩散</b>反应伤害 +36%，持续 10 秒。
 *
 * <p>走「反应加成区里那份星烁加成」（和圣遗物四件套那 40% 同一个位置）——
 * 只给星扩散分支，不给星超导。
 */
public class DiebianForsakenWindEffect implements ICharacterEffect {

    public static final int DURATION_TICKS = 10 * 20;
    public static final float STELLAR_SWIRL_BONUS = 0.36f;

    @Override
    public float getStellarGlimmerBonus(StellarGlimmerBranch branch) {
        return branch == StellarGlimmerBranch.SWIRL ? STELLAR_SWIRL_BONUS : 0f;
    }

    @Override
    public void onEffectOverride(Player holder, PGCharacter character,
                                 CharacterEffectInstance existingInstance,
                                 CharacterEffectInstance newInstance) {
        existingInstance.setDuration(newInstance.getDuration());
    }
}
