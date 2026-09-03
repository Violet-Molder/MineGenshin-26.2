package com.linweiyun.genshin.core.character;

import com.linweiyun.genshin.content.effect.character.CharacterEffectContainer;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.attribute.AttributeContainer;
import com.linweiyun.genshin.content.attribute.AttributeInstance;
import com.linweiyun.genshin.content.attribute.AttributeType;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class PGCharacterData implements IPersistedSerializable {
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
    @Persisted(key = "constellation")
    private int constellation;
    @Persisted(key = "attributes")
    private AttributeContainer attributes = new AttributeContainer();
    @Persisted(key = "normal_attack_level")
    private int normalAttackLevel;
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
    @Persisted(key = "elemental_skill_max_stacks")
    private int elementalSkillMaxStacks;
    @Persisted(key = "elemental_skill_stacks")
    private int elementalSkillStacks;
    @Persisted(key = "skill_list")
    protected List<Integer> skillCoolList;
    @Persisted(key = "status_container")
    private StatusContainer statusContainer = new StatusContainer();
    // 效果容器的缓存引用，延迟加载，避免每次操作都重新反序列化
    private CharacterEffectContainer effectContainer;

    private boolean dirty = false;

    public PGCharacterData() {
        this.characterLevel = 1;
        this.currentExp = 0;
        this.maxExp = 1000;
        this.ascensionPhase = 0;
        this.constellation = 0;
        this.normalAttackLevel = 1;
        this.elementalSkillLevel = 1;
        this.elementalBurstLevel = 1;
        this.currentObtainingEnergy = 0;
        this.elementalSkillCooldownTick = 0;
        this.elementalBurstCooldownTick = 0;
        this.elementalSkillMaxStacks = 1;
        this.elementalSkillStacks = 1;
        this.attributes = new AttributeContainer();
        this.effectDataList = new ListTag();
        this.skillCoolList = new ArrayList<>();
    }
    public void initBaseStats(double baseHP, double baseATK, double baseDEF) {
        this.attributes.setBaseValue(ModAttributes.MAX_HP.value(), baseHP);
        this.attributes.setBaseValue(ModAttributes.ATK.value(), baseATK);
        this.attributes.setBaseValue(ModAttributes.DEF.value(), baseDEF);
        this.currentHP = this.attributes.getTotalValue(ModAttributes.MAX_HP.value());
    }



    // ==== 属性数据获取 ====
    public AttributeInstance getAttribute(AttributeType type) {return attributes.get(type);}
    public double getAttributeBaseValue(AttributeType type) {return attributes.getBaseValue(type);}
    public double getAttributeTotalValue(AttributeType type) {return attributes.getTotalValue(type);}
    public double getAttributeFlatModifier(AttributeType type) {return attributes.getFlatModifier(type);}
    public double getAttributeTempFlatModifier(AttributeType type) {return attributes.getTempFlatModifier(type);}
    public double getAttributePercentModifier(AttributeType type) {return attributes.getPercentModifier(type);}
    public double getAttributeTempPercentModifier(AttributeType type) {return attributes.getTempPercentModifier(type);}

    // ==== 属性数据修改 ====
    public void setAttributeBaseValue(AttributeType type, double value) { attributes.setBaseValue(type, value);markDirty();}
    public void addAttributeFlatModifier(AttributeType type, String source, double value) { attributes.addFlatModifier(type, source, value);markDirty();}
    public void addAttributePercentModifier(AttributeType type, String source, double value) { attributes.addPercentModifier(type, source, value);markDirty();}
    public void addAttributeTempFlatModifier(AttributeType type, String source, double value) { attributes.addTempFlatModifier(type, source, value);markDirty();}
    public void addAttributeTempPercentModifier(AttributeType type, String source, double value) { attributes.addTempPercentModifier(type, source, value);markDirty();}
    public void removeAttributeModifier(AttributeType type, String source) { attributes.removeModifier(type, source);markDirty();}

    public int getLevel() { return characterLevel; }
    public int getCurrentExp() { return currentExp; }
    public int getMaxExp() { return maxExp; }
    public double getCurrentHP() { return currentHP; }
    public int getAscensionPhase() { return ascensionPhase; }
    public float getCurrentObtainingEnergy() { return currentObtainingEnergy; }
    public float getElementalSkillCooldownTick() { return elementalSkillCooldownTick; }
    public float getElementalBurstCooldownTick() { return elementalBurstCooldownTick; }
    public int getElementalSkillMaxStacks() { return elementalSkillMaxStacks; }
    public int getElementalSkillStacks() { return elementalSkillStacks; }
    public StatusContainer getStatusContainer() { return statusContainer; }
    public int getNormalAttackLevel() { return normalAttackLevel; }
    public int getElementalSkillLevel() { return elementalSkillLevel; }
    public int getElementalBurstLevel() { return elementalBurstLevel; }



    public List<Integer> getSkillCoolList() {
        return skillCoolList;
    }


    public void setCurrentExp(int currentExp) { this.currentExp = currentExp;markDirty();}
    public void setMaxExp(int maxExp) { this.maxExp = maxExp;markDirty();}
    public void setCurrentHP(double currentHP) { this.currentHP = currentHP;markDirty();}
    public void setAscensionPhase(int ascensionPhase) { this.ascensionPhase = ascensionPhase;markDirty();}
    public void setCurrentObtainingEnergy(float energy) { this.currentObtainingEnergy = energy;markDirty();}
    public void setElementalSkillCooldownTick(float tick) { this.elementalSkillCooldownTick = tick;markDirty();}
    public void setElementalBurstCooldownTick(float tick) { this.elementalBurstCooldownTick = tick;markDirty();}
    public void setElementalSkillMaxStacks(int stacks) { this.elementalSkillMaxStacks = stacks;markDirty();}
    public void setElementalSkillStacks(int stacks) { this.elementalSkillStacks = stacks;markDirty();}
    public void addLevel(int levels) {this.characterLevel = Math.max(1, Math.min(this.characterLevel + levels, 90));markDirty();}
    public void healHP(double amount) {this.currentHP = Math.min(this.currentHP + amount, getAttributeTotalValue(ModAttributes.MAX_HP.value()));markDirty();}
    public void hurtHP(float amount) {this.currentHP = Math.max(0, this.currentHP - amount);markDirty();}

    /**
     * 获取角色的效果容器
     * 采用延迟加载模式：首次调用时从NBT标签反序列化，后续直接返回缓存
     * @return 效果容器
     */
    public void tick() {
        if (elementalSkillCooldownTick > 0) {
            elementalSkillCooldownTick--;
            markDirty();
            if (elementalSkillCooldownTick == 0) {
                if (skillCoolList != null && !skillCoolList.isEmpty()) {  // ← 增加空检查
                    skillCoolList.removeFirst();
                    if (!skillCoolList.isEmpty()) {
                        elementalSkillCooldownTick = skillCoolList.getFirst();
                        markDirty();

                    }
                }
                if (elementalSkillStacks < elementalSkillMaxStacks) {
                    elementalSkillStacks++;
                    markDirty();
                }
            }
        }
        if (elementalBurstCooldownTick > 0) {
            elementalBurstCooldownTick--;
            markDirty();
        }
        statusContainer.tick();
    }
    public CharacterEffectContainer getEffectContainer() {
        if (effectContainer == null) {                                     // 首次调用时加载
            effectContainer = CharacterEffectContainer.fromListTag(effectDataList); // 从NBT反序列化
        }
        return effectContainer;                                            // 返回缓存
    }
    public ListTag getEffectDataList() { return effectDataList; }
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
        copy.characterLevel = this.characterLevel;
        copy.currentExp = this.currentExp;
        copy.maxExp = this.maxExp;
        copy.currentHP = this.currentHP;
        copy.ascensionPhase = this.ascensionPhase;
        copy.constellation = this.constellation;
        copy.attributes = this.attributes.copy();
        copy.normalAttackLevel = this.normalAttackLevel;
        copy.elementalSkillLevel = this.elementalSkillLevel;
        copy.elementalBurstLevel = this.elementalBurstLevel;
        copy.currentObtainingEnergy = this.currentObtainingEnergy;
        copy.elementalSkillCooldownTick = this.elementalSkillCooldownTick;
        copy.elementalBurstCooldownTick = this.elementalBurstCooldownTick;
        copy.elementalSkillMaxStacks = this.elementalSkillMaxStacks;
        copy.elementalSkillStacks = this.elementalSkillStacks;
        copy.skillCoolList = this.skillCoolList;
        copy.effectDataList = this.effectDataList.copy();                  // 深拷贝效果NBT数据
        if (this.effectContainer != null) {                                // 如果容器已加载
            copy.effectContainer = this.effectContainer.copy();            // 也深拷贝容器缓存
        }
        return copy;
    }

    private void markDirty() {
        this.dirty = true;
    }
    public boolean isDirty() { return dirty; }
    public void clearDirty() { dirty = false; }
}