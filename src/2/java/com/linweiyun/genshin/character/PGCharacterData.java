package com.linweiyun.genshin.character;

import com.linweiyun.genshin.content.effect.character.CharacterEffectContainer;
import com.linweiyun.genshin.core.attribute.AttributeContainer;
import com.linweiyun.genshin.core.attribute.AttributeType;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.registry.register.CharacterRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.nbt.ListTag;

public class PGCharacterData implements IPersistedSerializable {
    @Persisted(key = "character_uuid")
    private int characterUUID;

    @Persisted(key = "character_level")
    private int characterLevel;

    @Persisted(key = "max_exp")
    private int maxExp;

    @Persisted(key = "current_exp")
    private int currentExp;

    @Persisted(key = "current_hp")
    private double currentHP;

    @Persisted(key = "ascension_phase")
    private int ascensionPhase;

    @Persisted(key = "attributes")
    private AttributeContainer attributes = new AttributeContainer();

    @Persisted(key = "normal_attack_level")
    private int normalAttackLevel;

    @Persisted(key = "charged_attack_level")
    private int chargedAttackLevel;

    @Persisted(key = "plunging_attack_level")
    private int plungingAttackLevel;

    @Persisted(key = "elemental_skill_level")
    private int elementalSkillLevel;

    @Persisted(key = "elemental_burst_level")
    private int elementalBurstLevel;

    @Persisted(key = "current_obtaining_energy")
    private float currentObtainingEnergy;

    @Persisted(key = "elemental_skill_cooldown_tick")
    private float elementalSkillCooldownTick;

    @Persisted(key = "elemental_burst_cooldown_tick")
    private float elementalBurstCooldownTick;

    @Persisted(key = "effect_data")
    private ListTag effectDataList = new ListTag();
    // 效果容器的缓存引用，延迟加载，避免每次操作都重新反序列化
    private CharacterEffectContainer effectContainer;

    public PGCharacterData() {}

    public PGCharacterData(int characterUUID, double baseHP, double baseATK, double baseDEF) {
        this.characterUUID = characterUUID;
        this.characterLevel = 1;
        this.currentExp = 0;
        this.maxExp = 1000;
        this.ascensionPhase = 0;
        this.normalAttackLevel = 1;
        this.chargedAttackLevel = 1;
        this.plungingAttackLevel = 1;
        this.elementalSkillLevel = 1;
        this.elementalBurstLevel = 1;
        this.currentObtainingEnergy = 0;
        this.elementalSkillCooldownTick = 0;
        this.elementalBurstCooldownTick = 0;

        this.attributes.setBaseValue(ModAttributes.MAX_HP.value(), baseHP);
        this.attributes.setBaseValue(ModAttributes.ATK.value(), baseATK);
        this.attributes.setBaseValue(ModAttributes.DEF.value(), baseDEF);
        this.currentHP = this.attributes.getValue(ModAttributes.MAX_HP.value());
    }

    public AttributeContainer getAttributes() {
        return attributes;
    }
    public double getValue(AttributeType type) {
        return attributes.getValue(type);
    }

    public int getCharacterUUID() { return characterUUID; }
    public int getLevel() { return characterLevel; }
    public int getCurrentExp() { return currentExp; }
    public int getMaxExp() { return maxExp; }
    public double getMaxHP() {
        return attributes.getValue(ModAttributes.MAX_HP.value());
    }
    public double getATK() {
        return attributes.getValue(ModAttributes.ATK.value());
    }
    public double getDEF() {
        return attributes.getValue(ModAttributes.DEF.value());
    }
    public double getCurrentHP() { return currentHP; }
    public int getAscensionPhase() { return ascensionPhase; }
    public float getCurrentObtainingEnergy() { return currentObtainingEnergy; }
    public float getElementalSkillCooldownTick() { return elementalSkillCooldownTick; }
    public float getElementalBurstCooldownTick() { return elementalBurstCooldownTick; }

    public void setCurrentExp(int currentExp) { this.currentExp = currentExp; }
    public void setMaxExp(int maxExp) { this.maxExp = maxExp; }
    public void setCurrentHP(double currentHP) { this.currentHP = currentHP; }
    public void setAscensionPhase(int ascensionPhase) { this.ascensionPhase = ascensionPhase; }
    public void setCurrentObtainingEnergy(float energy) { this.currentObtainingEnergy = energy; }
    public void setElementalSkillCooldownTick(float tick) { this.elementalSkillCooldownTick = tick; }
    public void setElementalBurstCooldownTick(float tick) { this.elementalBurstCooldownTick = tick; }

    public void addLevel(int levels) {
        this.characterLevel = Math.max(1, Math.min(this.characterLevel + levels, 90));
    }

    public void healHP(double amount) {
        this.currentHP = Math.min(this.currentHP + amount, getMaxHP());
    }

    public void hurtHP(float amount) {
        this.currentHP = Math.max(0, this.currentHP - amount);
    }
    public PGCharacter getDefinition() {
        return CharacterRegister.getByUUID(characterUUID);
    }

    /**
     * 获取角色的效果容器
     * 采用延迟加载模式：首次调用时从NBT标签反序列化，后续直接返回缓存
     * @return 效果容器
     */
    public CharacterEffectContainer getEffectContainer() {
        if (effectContainer == null) {                                     // 首次调用时加载
            effectContainer = CharacterEffectContainer.fromListTag(effectDataList); // 从NBT反序列化
        }
        return effectContainer;                                            // 返回缓存
    }

    /**
     * 将效果容器的当前状态同步写回NBT标签
     * 在修改效果数据后必须调用此方法，确保下次持久化时数据正确
     */
    public void syncEffectsToTag() {
        if (effectContainer != null) {                                     // 容器已加载时才同步
            effectDataList = effectContainer.toListTag();                  // 序列化回NBT标签
        }
    }


    public PGCharacterData copy() {
        PGCharacterData copy = new PGCharacterData();
        copy.characterUUID = this.characterUUID;
        copy.characterLevel = this.characterLevel;
        copy.currentExp = this.currentExp;
        copy.maxExp = this.maxExp;
        copy.currentHP = this.currentHP;
        copy.ascensionPhase = this.ascensionPhase;
        copy.attributes = this.attributes.copy();
        copy.normalAttackLevel = this.normalAttackLevel;
        copy.chargedAttackLevel = this.chargedAttackLevel;
        copy.plungingAttackLevel = this.plungingAttackLevel;
        copy.elementalSkillLevel = this.elementalSkillLevel;
        copy.elementalBurstLevel = this.elementalBurstLevel;
        copy.currentObtainingEnergy = this.currentObtainingEnergy;
        copy.elementalSkillCooldownTick = this.elementalSkillCooldownTick;
        copy.elementalBurstCooldownTick = this.elementalBurstCooldownTick;
        copy.effectDataList = this.effectDataList.copy();                  // 深拷贝效果NBT数据
        if (this.effectContainer != null) {                                // 如果容器已加载
            copy.effectContainer = this.effectContainer.copy();            // 也深拷贝容器缓存
        }
        return copy;
    }
}
