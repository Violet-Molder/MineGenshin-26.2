package com.linweiyun.genshin.core.character;

import com.linweiyun.genshin.content.effect.character.CharacterEffectContainer;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.core.attribute.AttributeContainer;
import com.linweiyun.genshin.core.attribute.AttributeType;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.registry.register.CharacterRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;

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
    @Persisted(key = "elemental_skill_stacks")
    private int elementalSkillStacks = 1;
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
        this.elementalSkillStacks = 1;

        this.attributes.setBaseValue(ModAttributes.MAX_HP.value(), baseHP);
        this.attributes.setBaseValue(ModAttributes.ATK.value(), baseATK);
        this.attributes.setBaseValue(ModAttributes.DEF.value(), baseDEF);
        this.currentHP = this.attributes.getValue(ModAttributes.MAX_HP.value());
    }

    public void tick(Player player) {
        // 1. 处理CD
        tickCooldowns();
        // 2. 处理效果
        tickEffects(player);
        // 3. 角色自身逻辑（子类重写）
        onTick(player);
    }
    public void backTick(Player player) {
    }
    public void frontTick(Player player) {
    }
    protected void onTick(Player player) {
    }
    protected void tickCooldowns() {
        // 元素战技CD - 每秒减20tick
        if (elementalSkillCooldownTick > 0) {
            elementalSkillCooldownTick = Math.max(0, elementalSkillCooldownTick - 20f);
            // 如果有多层技能，CD归零时层数+1
            if (elementalSkillCooldownTick == 0 && elementalSkillStacks < getMaxElementalSkillStacks()) {
                elementalSkillStacks++;
                // 还有剩余层数需要充能，重置CD
                if (elementalSkillStacks < getMaxElementalSkillStacks()) {
                    elementalSkillCooldownTick = getSkillChargeCooldown(elementalSkillStacks - 1);
                }
            }
        }
        // 元素爆发CD
        if (elementalBurstCooldownTick > 0) {
            elementalBurstCooldownTick = Math.max(0, elementalBurstCooldownTick - 20f);
        }
    }
    public int getMaxElementalSkillStacks() {
        return 1;
    }
    public float getSkillChargeCooldown(int stackIndex) {
        PGCharacterDefine def = getDefinition();
        return def != null ? def.getSkillMaxCooldownTick() : 200f;
    }
    protected void tickEffects(Player player) {
        CharacterEffectContainer container = getEffectContainer();
        boolean hasChanges = false;

        for (CharacterEffectInstance instance : container.getEffects()) {
            ICharacterEffect effect = instance.getEffect();
            if (effect == null) continue;
            // 持续时间-1秒
            if (instance.getDuration() != CharacterEffectInstance.INFINITE) {
                instance.setDuration(instance.getDuration() - 20);
            }
            // 调用效果tick方法
            boolean remain = effect.onEffectTick(player, this, instance);
            // 持续时间<=0或效果返回false则移除
            if (instance.getDuration() <= 0 || !remain) {
                effect.onEffectRemoved(player, this, instance);
                container.removeEffect(instance.getEffectIdString());
                hasChanges = true;
            }
        }

        if (hasChanges) {
            syncEffectsToTag();
        }
    }
//    public boolean canUseElementalSkill(boolean isLongPress) {
//        if (elementalSkillStacks <= 0) return false;
//        PGCharacterDefine def = getDefinition();
//        if (def == null) return false;
////        float energyCost = getSkillEnergyCost(isLongPress);
//        return currentObtainingEnergy >= energyCost;
//    }

    public boolean canUseElementalBurst() {
        PGCharacterDefine def = getDefinition();
        if (def == null) return false;
        if (elementalBurstCooldownTick > 0) return false;
        return currentObtainingEnergy >= def.getMaxObtainingEnergy();
    }
//    public boolean triggerElementalSkill(boolean isLongPress) {
//        if (!canUseElementalSkill(isLongPress)) return false;
//
//        // 消耗一层
//        elementalSkillStacks--;
//
//        // 还有层数：设置下一层充能CD
//        if (elementalSkillStacks < getMaxElementalSkillStacks()) {
//            int nextIndex = getMaxElementalSkillStacks() - elementalSkillStacks - 1;
//            elementalSkillCooldownTick = getSkillChargeCooldown(nextIndex);
//        } else {
//            // 所有层数用完：设置普通CD
//            PGCharacterDefine def = getDefinition();
//            if (def != null) {
//                elementalSkillCooldownTick = def.getSkillMaxCooldownTick();
//            }
//        }
//
//        return true;
//    }

    public boolean triggerElementalBurst() {
        if (!canUseElementalBurst()) return false;

        currentObtainingEnergy = 0;
        PGCharacterDefine def = getDefinition();
        if (def != null) {
            elementalBurstCooldownTick = def.getBurstMaxCooldownTick();
        }
        return true;
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
    public PGCharacterDefine getDefinition() {
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
        copy.elementalSkillStacks = this.elementalSkillStacks;
        copy.effectDataList = this.effectDataList.copy();                  // 深拷贝效果NBT数据
        if (this.effectContainer != null) {                                // 如果容器已加载
            copy.effectContainer = this.effectContainer.copy();            // 也深拷贝容器缓存
        }
        return copy;
    }
}
