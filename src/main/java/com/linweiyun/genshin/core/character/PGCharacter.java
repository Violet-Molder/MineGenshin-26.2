package com.linweiyun.genshin.core.character;

import com.linweiyun.genshin.Config;
import com.linweiyun.genshin.core.attribute.AttributeType;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PGCharacter implements IPersistedSerializable {
    @Persisted(key = "character_uuid")
    protected int characterUUID;
    @Persisted(key = "star_rating")
    protected int starRating;
    @Persisted(key = "name")
    protected Component name;
    @Persisted(key = "elemental")
    protected ElementalsGIM elemental;
    @Persisted(key = "ascend_attribute")
    protected CharacterAscendAttribute ascendAttribute;
    @Persisted(key = "skill_short_cooldown")
    protected int skillShortMaxCooldownTick;
    @Persisted(key = "skill_long_cooldown")
    protected int skillLongMaxCooldownTick;
    @Persisted(key = "burst_cooldown")
    protected int burstMaxCooldownTick;
    @Persisted(key = "max_obtaining_energy")
    protected float maxObtainingEnergy;
    @Persisted(key = "texture_id")
    protected String textureId;
    @Persisted(key = "data")
    protected PGCharacterData data;


    private static final Logger LOGGER = LogUtils.getLogger();
    public PGCharacter() {
        this.data = new PGCharacterData();
    }

    public PGCharacter(
            int characterUUID, int starRating, Component name,
            ElementalsGIM elemental, CharacterAscendAttribute ascendAttribute,
            int skillMaxCooldownTick, int burstMaxCooldownTick,
            float maxObtainingEnergy, String textureId,
            Map<Identifier, Supplier<List<? extends Integer>>> statGrowthMap) {
        this.characterUUID = characterUUID;
        this.starRating = starRating;
        this.name = name;
        this.elemental = elemental;
        this.ascendAttribute = ascendAttribute;
        this.skillShortMaxCooldownTick = skillMaxCooldownTick;
        this.skillLongMaxCooldownTick = skillMaxCooldownTick;
        this.burstMaxCooldownTick = burstMaxCooldownTick;
        this.maxObtainingEnergy = maxObtainingEnergy;
        this.textureId = textureId;
        this.data = new PGCharacterData();
    }
    public PGCharacter(
            int characterUUID, int starRating, Component name,
            ElementalsGIM elemental, CharacterAscendAttribute ascendAttribute,
            int skillShortMaxCooldownTick,int skillLongMaxCooldownTick, int burstMaxCooldownTick,
            float maxObtainingEnergy, String textureId,
            Map<Identifier, Supplier<List<? extends Integer>>> statGrowthMap) {
        this.characterUUID = characterUUID;
        this.starRating = starRating;
        this.name = name;
        this.elemental = elemental;
        this.ascendAttribute = ascendAttribute;
        this.skillShortMaxCooldownTick = skillShortMaxCooldownTick;
        this.skillLongMaxCooldownTick = skillLongMaxCooldownTick;
        this.burstMaxCooldownTick = burstMaxCooldownTick;
        this.maxObtainingEnergy = maxObtainingEnergy;
        this.textureId = textureId;
        this.data = new PGCharacterData();
    }
    private AttributeType resolveType(Identifier id) {
        return ModAttributes.ATTRIBUTES.getRegistry().get().getValue(id);
    }
    public void performElementalSkill(Player player, int skillTime) {
        LOGGER.info(player + String.valueOf(data.getElementalSkillCooldownTick()));
        if (data.getElementalSkillCooldownTick() == 0) {
            triggerElementalSkill(player, skillTime);
            data.setElementalSkillStacks(data.getElementalSkillStacks() - 1);
            if (player.level().isClientSide()) return;
            if (skillTime < 1000) {
                data.setElementalSkillCooldownTick(skillShortMaxCooldownTick);
            } else {
                data.setElementalSkillCooldownTick(skillLongMaxCooldownTick);
            }
        } else {
            player.sendSystemMessage(Component.literal("技能冷却中，当前CD" + data.getElementalSkillCooldownTick()));
               }

    };
    public void performElementalBurst(Player player) {
        LOGGER.info(String.valueOf(data.getElementalBurstCooldownTick()));
        if (data.getElementalBurstCooldownTick() == 0) {
            triggerElementalBurst(player);
            if (player.level().isClientSide()) return;
            data.setElementalBurstCooldownTick(burstMaxCooldownTick);

        } else {
            player.sendSystemMessage(Component.literal("技能冷却中"));
        }
    };
    protected void triggerElementalSkill(Player player, int skillTime){};
    protected void triggerElementalBurst(Player player){
        System.out.println(player + "bbbbbbbbb)");
    };

    public void frontTick(Player player) {

    }

    public void backTick(Player player) {

    }

    public void tick(Player player) {
        data.tick();
        frontTick(player);
        backTick(player);
    }

    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of();
    }
    public int getStatAtLevel(AttributeType type, int levelIndex) {
        Supplier<List<? extends Integer>> supplier = getStatGrowthMap().get(type.id());
        if (supplier == null) return 0;
        List<? extends Integer> list = supplier.get();
        if (levelIndex < 0 || levelIndex >= list.size()) return 0;
        return list.get(levelIndex);
    }

    public double getBaseStat(AttributeType type) {
        Supplier<List<? extends Integer>> supplier = getStatGrowthMap().get(type.id());
        if (supplier == null) return type.defaultValue();
        List<? extends Integer> list = supplier.get();
        if (list.isEmpty()) return type.defaultValue();
        return list.get(0);
    }

    public Set<AttributeType> getStatGrowthTypes() {
        return getStatGrowthMap().keySet().stream()
                .map(this::resolveType)
                .collect(Collectors.toSet());
    }
    public void addExp(int amount) {
        data.setCurrentExp(data.getCurrentExp() + amount);
        tryLevelUp();
    }
    public void tryLevelUp() {
        var expList = Config.CHARACTER_UP_EXP.get();
        // ===== 计算阶段：遍历计算可升级级数，不修改状态 =====
        int totalExpConsumed = 0; //将要消耗的经验值的总数
        int levelsToGain = 0;
        int oldLevel = data.getLevel();
        int currentAscensionPhase = data.getAscensionPhase();
        while (oldLevel < 90) {
            int expNeeded = expList.get(oldLevel - 1); //获取升至下一级所需经验值
            if (data.getCurrentExp() - totalExpConsumed < expNeeded) break; //在循环模拟计算中若当前经验值减去将要消耗的经验值总数后小于升级需要的经验值则跳出循环，根据模拟数据执行真正的升级
            int maxLevelForPhase = currentAscensionPhase == 0
                    ? 20
                    : Math.min((currentAscensionPhase + 3) * 10, 90);//计算当前突破等级最高可升至几级
            if (oldLevel >= maxLevelForPhase) break;
            totalExpConsumed += expNeeded;
            levelsToGain++;
            oldLevel++;
        }
        if (levelsToGain == 0) return;
        data.setCurrentExp(data.getCurrentExp() - totalExpConsumed);
        data.addLevel(levelsToGain);
        // ========== 通过 AttributeType 设置基础值 ==========
        int statIndex = data.getLevel() - 1 + data.getAscensionPhase();
        System.out.println(statIndex);
        updateBaseStatsFromConfig(statIndex);
        // 升级回满血
        data.setCurrentHP(data.getAttributeTotalValue(ModAttributes.MAX_HP.value()));

        if (data.getLevel() < 90) {
            data.setMaxExp(expList.get(data.getLevel() - 1));
        }
        // TODO: 发布角色升级事件

    }
    // ========== 突破逻辑 ==========
    public void ascend() {
        int maxLevelForPhase = data.getAscensionPhase() == 0
                ? 20
                : Math.min((data.getAscensionPhase() + 3) * 10, 90);

        if (data.getLevel() != maxLevelForPhase) return;

        int newPhase = data.getAscensionPhase() + 1;
        data.setAscensionPhase(newPhase);
        int statIndex = maxLevelForPhase - 1 + newPhase;
        updateBaseStatsFromConfig(statIndex);
        // 替代原来的 character.getATK().addExtraPercent(0.012f * (start + 1))
        int starRating = this.getStarRating();
        CharacterAscendAttribute ascendAttr = this.getAscendAttribute();

        // 先移除旧的突破加成，再添加新的
        data.removeAttributeModifier(getAscendAttributeType(ascendAttr), "ascension_bonus");

        AttributeType targetAttrType = getAscendAttributeType(ascendAttr);
        switch (ascendAttr) {
            case ATK, HP:
                data.addAttributePercentModifier(targetAttrType, "ascension_bonus", 0.012f * (starRating + 1));
                break;
            case DEF:
                data.addAttributePercentModifier(targetAttrType, "ascension_bonus", 0.015f * (starRating + 1));
                break;
        }

        // 突破后尝试继续升级
        tryLevelUp();
    }

    private AttributeType getAscendAttributeType(CharacterAscendAttribute ascendAttr) {
        return switch (ascendAttr) {
            case ATK -> ModAttributes.ATK.value();
            case HP -> ModAttributes.MAX_HP.value();
            case DEF -> ModAttributes.DEF.value();
        };
    }
    public int getCharacterUUID() { return characterUUID; }
    public int getStarRating() { return starRating; }
    public Component getName() { return name; }
    public ElementalsGIM getElemental() { return elemental; }
    public CharacterAscendAttribute getAscendAttribute() { return ascendAttribute; }
    public int getSkillShortMaxCooldownTick() { return skillShortMaxCooldownTick; }
    public int getSkillLongMaxCooldownTick() { return skillLongMaxCooldownTick; }
    public int getBurstMaxCooldownTick() { return burstMaxCooldownTick; }
    public float getMaxObtainingEnergy() { return maxObtainingEnergy; }
    public String getTextureId() { return textureId; }

    public PGCharacterData getData() {
        return data;
    }

    public void setData(PGCharacterData data) {
        this.data = data;
    }

    // 从 Config 列表中读取指定等级的属性值，设置到 AttributeContainer
    private void updateBaseStatsFromConfig(int statIndex) {
        for (AttributeType type : this.getStatGrowthTypes()) {
            int value = this.getStatAtLevel(type, statIndex);
            System.out.println(value);
            data.setAttributeBaseValue(type, value);
        }
    }


}