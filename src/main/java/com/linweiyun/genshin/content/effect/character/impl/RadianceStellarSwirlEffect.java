package com.linweiyun.genshin.content.effect.character.impl;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class RadianceStellarSwirlEffect implements ICharacterEffect {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final int DURATION_TICKS = 160; // 8s = 160 ticks

    @Override
    public void onEffectOverride(Player holder, PGCharacter character,
                                 CharacterEffectInstance existingInstance,
                                 CharacterEffectInstance newInstance) {
        existingInstance.setDuration(newInstance.getDuration());
        LOGGER.debug("[辉映-星扩散] 刷新持续时间 to={} | char={}",
                newInstance.getDuration(), character.getName());
    }
}