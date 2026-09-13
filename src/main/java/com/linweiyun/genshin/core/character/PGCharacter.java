package com.linweiyun.genshin.core.character;

import com.linweiyun.genshin.config.Config;
import com.linweiyun.genshin.content.attribute.AttributeType;
import com.linweiyun.genshin.content.effect.character.CharacterEffectContainer;
import com.linweiyun.genshin.content.effect.character.CharacterEffectHelper;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.content.effect.character.artifact.ArtifactSetEffect;
import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.content.items.artifact.ArtifactSet;
import com.linweiyun.genshin.content.items.artifact.inventory.ArtifactInventory;
import com.linweiyun.genshin.content.items.artifact.type.ArtifactType;
import com.linweiyun.genshin.content.items.component.ArtifactStatsComponent;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.system.registry.register.ModDataComponents;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.HashMap;
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
    protected String elementalId;
    private transient GenshinElement elemental;
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
            String elementalId, CharacterAscendAttribute ascendAttribute,
            int skillMaxCooldownTick, int burstMaxCooldownTick,
            float maxObtainingEnergy, String textureId,
            Map<Identifier, Supplier<List<? extends Integer>>> statGrowthMap) {
        this.characterUUID = characterUUID;
        this.starRating = starRating;
        this.name = name;
        this.elementalId = elementalId;
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
            String elementalId, CharacterAscendAttribute ascendAttribute,
            int skillShortMaxCooldownTick,int skillLongMaxCooldownTick, int burstMaxCooldownTick,
            float maxObtainingEnergy, String textureId,
            Map<Identifier, Supplier<List<? extends Integer>>> statGrowthMap) {
        this.characterUUID = characterUUID;
        this.starRating = starRating;
        this.name = name;
        this.elementalId = elementalId;
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
        if (data.getElementalBurstCooldownTick() == 0) {
            triggerElementalBurst(player);
            if (player.level().isClientSide()) return;
            data.setElementalBurstCooldownTick(burstMaxCooldownTick);

        } else {
            player.sendSystemMessage(Component.literal("技能冷却中"));
        }
    };
    protected void triggerElementalSkill(Player player, int skillTime){};
    protected void triggerElementalBurst(Player player){};

    public void frontTick(Player player) {

    }

    public void backTick(Player player) {

    }

    public void tick(Player player) {
        data.tick();
        recalculateDirtyArtifactSlots();
        frontTick(player);
        backTick(player);
    }

    public void equipArtifact(ArtifactType type, ItemStack artifactStack) {
        if (artifactStack.getItem() instanceof ArtifactItem artifactItem) {
            if (artifactItem.getType() != type) return;
            int slot = ArtifactInventory.typeToSlot(type);
            data.getArtifactInventory().setItem(slot, artifactStack.copy());
        }
    }
    public void unequipArtifact(ArtifactType type) {
        int slot = ArtifactInventory.typeToSlot(type);
        data.getArtifactInventory().setItem(slot, ItemStack.EMPTY);
    }

    public void recalculateDirtyArtifactSlots() {
        ArtifactInventory inv = data.getArtifactInventory();
        if (!inv.hasDirtySlots()) return;
        for (int i = 0; i < ArtifactInventory.SLOT_COUNT; i++) {
            if (inv.isDirty(i)) {
                recalculateArtifactSlot(i);
                inv.clearDirty(i);
            }
        }
        refreshArtifactSetEffects();
    }

    private void recalculateArtifactSlot(int slotIndex) {
        ArtifactType type = ArtifactInventory.slotToType(slotIndex);
        String source = type.name().toLowerCase();

        double oldMaxHP = data.getAttributeTotalValue(ModAttributes.MAX_HP.get());
        double oldCurrentHP = data.getCurrentHP();
        double hpRatio = oldMaxHP > 0 ? oldCurrentHP / oldMaxHP : 1.0;

        for (AttributeType attrType : ModRegistries.ATTRIBUTE_TYPE_REGISTRY) {
            data.removeAttributeModifier(attrType, source);
        }

        ItemStack stack = data.getArtifactInventory().getItem(slotIndex);
        if (!stack.isEmpty() && stack.getItem() instanceof ArtifactItem) {
            ArtifactStatsComponent stats = stack.getOrDefault(
                    ModDataComponents.ARTIFACT_STATS.get(),
                    ArtifactStatsComponent.DEFAULT
            );
            applyStatWithSet(stats.mainStat, source);
            for (TeyvatItemStat subStat : stats.subStats) {
                if (subStat.isUnlocked()) {
                    applyStatWithSet(subStat, source);
                }
            }
        }

        double newMaxHP = data.getAttributeTotalValue(ModAttributes.MAX_HP.get());
        double newCurrentHP = newMaxHP * hpRatio;
        data.setCurrentHP(newCurrentHP);
    }

    private void applyStatWithSet(TeyvatItemStat stat, String source) {
        if (!stat.isInitialized()) return;
        AttributeType attr = stat.getAttribute();
        double value = stat.getValue();
        boolean isBaseAttr = isBaseAttribute(attr);
        if (isBaseAttr) {
            if (stat.getKind() == TeyvatItemStat.StatKind.FLAT) {
                data.setAttributeFlatModifier(attr, source, value);
            } else {
                data.setAttributePercentModifier(attr, source, value);
            }
        } else {
            data.setAttributeFlatModifier(attr, source, value);
        }
    }

    private static boolean isBaseAttribute(AttributeType attr) {
        if (attr == null || attr.id() == null) return false;
        AttributeType registryAttr = ModRegistries.ATTRIBUTE_TYPE_REGISTRY.getValue(attr.id());
        return registryAttr == ModAttributes.MAX_HP.get()
                || registryAttr == ModAttributes.ATK.get()
                || registryAttr == ModAttributes.DEF.get();
    }

    private void refreshArtifactSetEffects() {
        CharacterEffectContainer container = data.getEffectContainer();
        List<ICharacterEffect> toRemove = container.getEffects().stream()
                .filter(inst -> inst.getEffect() instanceof ArtifactSetEffect)
                .map(CharacterEffectInstance::getEffect)
                .toList();
        for (ICharacterEffect effect : toRemove) {
            CharacterEffectHelper.removeEffect(this.data.getOwnerPlayer(), this, effect);
        }
        Map<ArtifactSet, Integer> setCountMap = new HashMap<>();
        for (ItemStack stack : data.getAllArtifactsAsList()) {
            if (!(stack.getItem() instanceof ArtifactItem art)) continue;
            ArtifactSet set = art.getSet().get();
            setCountMap.merge(set, 1, Integer::sum);
        }
        setCountMap.forEach((set, count) -> {
            if (count >= 2){
                CharacterEffectInstance inst = new CharacterEffectInstance(
                        set.twoPcEffect().get(),
                        CharacterEffectInstance.INFINITE, 1, true
                );
                CharacterEffectHelper.addEffect(this.data.getOwnerPlayer(), this, inst);
            }
            if (count >= 4 && set.hasFourPcEffect()) {
                CharacterEffectInstance inst = new CharacterEffectInstance(
                        set.fourPcEffect().get(),
                        CharacterEffectInstance.INFINITE, 1, true
                );
                CharacterEffectHelper.addEffect(this.data.getOwnerPlayer(), this, inst);
            }
            data.syncEffectsToTag();
        });

    }


    public boolean hurt(float amount) {
        double before = data.getCurrentHP();
        data.hurtHP(amount);
        //AI 扣血后若血量归0且之前活着，则直接处理倒下逻辑
        if (data.getCurrentHP() <= 0 && before > 0) {
            incapacitate();
            return true;
        }
        return false;
    }

    //AI 改为无参，从 data 里获取所属 Player；不操作血量（hurt() 已将 currentHP 置 0），只处理切换和全队阵亡时关闭原神模式
    public void incapacitate() {
        Player player = data.getOwnerPlayer();
        if (player == null) return;
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        //AI 找 party 中下一个 currentHP > 0 的角色
        int currentIndex = attachment.getCurrentCharacterIndex();
        for (int offset = 1; offset <= 4; offset++) {
            int nextIndex = (currentIndex + offset) % 4;
            PGCharacter nextChar = attachment.getPartyCharacter(nextIndex);
            if (nextChar != null && nextChar.getData().getCurrentHP() > 0) {
                attachment.setCurrentCharacterIndex(nextIndex);
                NetworkManager.setCharacterSelectionToPlayer(
                        (ServerPlayer) player, nextIndex);
                return;
            }
        }

        //AI 全部阵亡则关闭原神模式并同步客户端
        player.setData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT, false);
        NetworkManager.setGenshinModeToPlayer(
                (ServerPlayer) player, false);
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
        if (data.getLevel() >= 90) {
            return;
        }
        var expList = Config.CHARACTER_UP_EXP.get();
        long totalMaxExp = 0;
        for (int i = 0; i < 89; i++) {
            totalMaxExp += expList.get(i);
        }
        long currentSpent = 0;
        for (int i = 0; i < data.getLevel() - 1; i++) {
            currentSpent += expList.get(i);
        }
        long remaining = totalMaxExp - currentSpent - data.getCurrentExp();
        if (amount > remaining) {
            data.setCurrentExp(data.getCurrentExp() + (int) remaining);
        } else {
            data.setCurrentExp(data.getCurrentExp() + amount);
        }
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

        int starRating = this.getStarRating();
        CharacterAscendAttribute ascendAttr = this.getAscendAttribute();
        AttributeType targetAttrType = getAscendAttributeType(ascendAttr);

        data.removeAttributeModifier(targetAttrType, "ascension_bonus");

        int bonusCount = getAscensionBonusCount(newPhase);
        if (bonusCount > 0) {
            switch (ascendAttr) {
                case ATK, HP:
                    data.addAttributePercentModifier(targetAttrType, "ascension_bonus", 0.012f * (starRating + 1) * bonusCount);
                    break;
                case DEF:
                    data.addAttributePercentModifier(targetAttrType, "ascension_bonus", 0.015f * (starRating + 1) * bonusCount);
                    break;
            }
        }

        // 突破后尝试继续升级
        tryLevelUp();
    }

    private static int getAscensionBonusCount(int phase) {
        return switch (phase) {
            case 0, 1 -> 0;
            case 2 -> 1;
            case 3, 4 -> 2;
            case 5 -> 3;
            case 6 -> 4;
            default -> 0;
        };
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
    public GenshinElement getElemental() {
        if (elemental != null) return elemental;
        if (elementalId != null && !elementalId.isEmpty()) {
            String[] parts = elementalId.split(":", 2);
            Identifier id = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
            elemental = ModRegistries.ELEMENT_REGISTRY.get(id).map(r -> r.value()).orElse(null);
            return elemental;
        }
        return ModElements.FYSIKOS.get();
    }
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
            data.setAttributeBaseValue(type, value);
        }
    }


}