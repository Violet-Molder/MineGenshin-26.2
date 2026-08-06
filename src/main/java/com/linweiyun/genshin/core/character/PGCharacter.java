package com.linweiyun.genshin.core.character;

import com.linweiyun.genshin.core.attribute.AttributeType;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PGCharacter {
    private final int characterUUID;
    private final int starRating;
    private final Component name;
    private final ElementalsGIM elemental;
    private final CharacterAscendAttribute ascendAttribute;
    private final float skillMaxCooldownTick;
    private final float burstMaxCooldownTick;
    private final float maxObtainingEnergy;
    private final String textureId;

    private final Map<Identifier, Supplier<List<? extends Integer>>> statGrowthMap;


    public PGCharacter(
            int characterUUID, int starRating, Component name,
            ElementalsGIM elemental, CharacterAscendAttribute ascendAttribute,
            float skillMaxCooldownTick, float burstMaxCooldownTick,
            float maxObtainingEnergy, String textureId,
            Map<Identifier, Supplier<List<? extends Integer>>> statGrowthMap) {
        this.characterUUID = characterUUID;
        this.starRating = starRating;
        this.name = name;
        this.elemental = elemental;
        this.ascendAttribute = ascendAttribute;
        this.skillMaxCooldownTick = skillMaxCooldownTick;
        this.burstMaxCooldownTick = burstMaxCooldownTick;
        this.maxObtainingEnergy = maxObtainingEnergy;
        this.textureId = textureId;
        this.statGrowthMap = statGrowthMap;
    }
    private AttributeType resolveType(Identifier id) {
        return ModAttributes.ATTRIBUTES.getRegistry().get().getValue(id);
    }

    public int getStatAtLevel(AttributeType type, int levelIndex) {
        Supplier<List<? extends Integer>> supplier = statGrowthMap.get(type.getId());
        if (supplier == null) return 0;
        List<? extends Integer> list = supplier.get();
        if (levelIndex < 0 || levelIndex >= list.size()) return 0;
        return list.get(levelIndex);
    }

    public double getBaseStat(AttributeType type) {
        Supplier<List<? extends Integer>> supplier = statGrowthMap.get(type.getId());
        if (supplier == null) return type.getDefaultValue();
        List<? extends Integer> list = supplier.get();
        if (list.isEmpty()) return type.getDefaultValue();
        return list.get(0);
    }

    public Set<AttributeType> getStatGrowthTypes() {
        return statGrowthMap.keySet().stream()
                .map(this::resolveType)
                .collect(Collectors.toSet());
    }
    public int getCharacterUUID() { return characterUUID; }
    public int getStarRating() { return starRating; }
    public Component getName() { return name; }
    public ElementalsGIM getElemental() { return elemental; }
    public CharacterAscendAttribute getAscendAttribute() { return ascendAttribute; }
    public float getSkillMaxCooldownTick() { return skillMaxCooldownTick; }
    public float getBurstMaxCooldownTick() { return burstMaxCooldownTick; }
    public float getMaxObtainingEnergy() { return maxObtainingEnergy; }
    public String getTextureId() { return textureId; }
}
