package com.linweiyun.genshin.core.character.catalyst;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class CatalystCharacter extends PGCharacter {

    public CatalystCharacter() {
        super();
    }


    public CatalystCharacter(
            int characterUUID, int starRating, Component name,
            String elementalId, CharacterAscendAttribute ascendAttribute,
            int skillMaxCooldownTick, int burstMaxCooldownTick,
            float maxObtainingEnergy, String textureId,
            Map<Identifier, Supplier<List<? extends Integer>>> statGrowthMap) {
        super(characterUUID, starRating, name,
                elementalId, ascendAttribute,
                skillMaxCooldownTick, burstMaxCooldownTick,
                maxObtainingEnergy, textureId, statGrowthMap);
    }

    public CatalystCharacter(
            int characterUUID, int starRating, Component name,
            String elementalId, CharacterAscendAttribute ascendAttribute,
            int skillShortMaxCooldownTick, int skillLongMaxCooldownTick, int burstMaxCooldownTick,
            float maxObtainingEnergy, String textureId,
            Map<Identifier, Supplier<List<? extends Integer>>> statGrowthMap) {
        super(characterUUID, starRating, name,
                elementalId, ascendAttribute,
                skillShortMaxCooldownTick, skillLongMaxCooldownTick, burstMaxCooldownTick,
                maxObtainingEnergy, textureId, statGrowthMap);
    }
}