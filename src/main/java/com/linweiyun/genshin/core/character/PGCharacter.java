package com.linweiyun.genshin.core.character;

import com.linweiyun.genshin.config.character.CharacterXpConfig;
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
import com.linweiyun.genshin.content.items.component.WeaponStatsComponent;
import com.linweiyun.genshin.content.items.weapon.WeaponItem;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.character.talent.TalentBase;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.combat.action.ActionManager;
import com.linweiyun.genshin.core.system.combat.action.ActionSet;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.system.registry.register.ModDataComponents;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.core.sync.ISyncCharacter;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.mojang.logging.LogUtils;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Holder;
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

public class PGCharacter implements IPersistedSerializable, ISyncCharacter {

    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Override public IManagedStorage getSyncStorage() { return syncStorage; }
    @Override public PGCharacter getSelfCharacter() { return this; }
    @Override public void notifyPersistence() { }

    @Getter
    @Setter
    @Persisted(key = "character_uuid")
    protected int characterUUID;
    @Getter
    @Setter
    @Persisted(key = "star_rating")
    protected int starRating;
    @Getter
    @Setter
    @Persisted(key = "name")
    protected Component name;
    @Persisted(key = "elemental")
    protected String elementalId;
    private transient GenshinElement elemental;
    @Getter
    @Setter
    @Persisted(key = "ascend_attribute")
    protected CharacterAscendAttribute ascendAttribute;
    @Getter
    @Setter
    @Persisted(key = "texture_id")
    protected String textureId;
    @Getter
    @Setter
    @Persisted(key = "data")
    protected PGCharacterData data;

    private static final String SOURCE_WEAPON = "weapon";

    protected transient TalentBase talent;

    private static final Logger LOGGER = LogUtils.getLogger();

    // ============ 动作集缓存 ============
    private final Map<String, ActionSet> actionSetCache = new HashMap<>();

    public PGCharacter() {
        this.data = new PGCharacterData();
        this.data.setParentCharacter(this);
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
        this.data = new PGCharacterData();
        this.data.setParentCharacter(this);
        this.data.setSkillShortMaxCooldownTick(skillMaxCooldownTick);
        this.data.setSkillLongMaxCooldownTick(skillMaxCooldownTick);
        this.data.setBurstMaxCooldownTick(burstMaxCooldownTick);
        this.data.setMaxObtainingEnergy(maxObtainingEnergy);
        this.textureId = textureId;
    }

    public PGCharacter(
            int characterUUID, int starRating, Component name,
            String elementalId, CharacterAscendAttribute ascendAttribute,
            int skillShortMaxCooldownTick, int skillLongMaxCooldownTick, int burstMaxCooldownTick,
            float maxObtainingEnergy, String textureId,
            Map<Identifier, Supplier<List<? extends Integer>>> statGrowthMap) {
        this.characterUUID = characterUUID;
        this.starRating = starRating;
        this.name = name;
        this.elementalId = elementalId;
        this.ascendAttribute = ascendAttribute;
        this.data = new PGCharacterData();
        this.data.setParentCharacter(this);
        this.data.setSkillShortMaxCooldownTick(skillShortMaxCooldownTick);
        this.data.setSkillLongMaxCooldownTick(skillLongMaxCooldownTick);
        this.data.setBurstMaxCooldownTick(burstMaxCooldownTick);
        this.data.setMaxObtainingEnergy(maxObtainingEnergy);
        this.textureId = textureId;
    }

    private AttributeType resolveType(Identifier id) {
        return ModAttributes.ATTRIBUTES.getRegistry().get().getValue(id);
    }

    public Class<? extends WeaponItem> getAllowedWeaponClass() {
        return WeaponItem.class;
    }

    // ============ 动作系统扩展点 ============

    public String getActionStateKey(Player player) {
        return "default";
    }

    public final ActionSet getActionSet(Player player) {
        String key = getActionStateKey(player);
        return actionSetCache.computeIfAbsent(key, k -> {
            ActionSet built = (talent != null) ? talent.buildActionSet(this, k) : null;
            return built != null ? built : buildFallbackActionSet();
        });
    }

    protected ActionSet buildFallbackActionSet() {
        return ActionSet.builder().build();
    }

    protected void invalidateActionSetCache() {
        actionSetCache.clear();
    }

    // ============ 动作逻辑（由 ActionManager 触发） ============

    public void performElementalSkill(Player player, int skillTime) {
        if (data.getElementalSkillCooldownTick() == 0) {
            if (talent != null) talent.elementalSkill(player, this, skillTime);
            data.setElementalSkillStacks(data.getElementalSkillStacks() - 1);
            if (player.level().isClientSide()) return;
            if (skillTime < 1000) {
                data.setElementalSkillCooldownTick(data.getSkillShortMaxCooldownTick());
            } else {
                data.setElementalSkillCooldownTick(data.getSkillLongMaxCooldownTick());
            }
            syncRealtimeState();
        } else {
            player.sendSystemMessage(Component.translatable("message.minegenshin.skill_cooldown", data.getElementalSkillCooldownTick()));
        }
    }

    public void performElementalBurst(Player player) {
        if (data.getElementalBurstCooldownTick() > 0) {
            player.sendSystemMessage(Component.translatable("message.minegenshin.skill_cooldown"));
            return;
        }
        if (data.getCurrentObtainingEnergy() < data.getMaxObtainingEnergy()) {
            player.sendSystemMessage(Component.translatable("message.minegenshin.not_enough_energy"));
            return;
        }
        if (talent != null) talent.elementalBurst(player, this);
        if (player.level().isClientSide()) return;
        data.setCurrentObtainingEnergy(0);
        data.setElementalBurstCooldownTick(data.getBurstMaxCooldownTick());
        syncRealtimeState();
    }

    public void performNormalAttack(Player player, int comboStage) {
        if (talent != null) talent.attack(player, this, comboStage);
    }

    public void performChargedAttack(Player player) {
        if (talent != null) talent.chargeAttack(player, this);
    }

    public int getMaxComboCount() {
        if (talent != null) return talent.getMaxCombo();
        return 1;
    }

    public int getNormalAttackPrecastTicks(int stage) {
        if (talent != null) return talent.getPrecastTicks(stage);
        return 0;
    }

    public int getNormalAttackPostcastTicks(int stage) {
        if (talent != null) return talent.getPostcastTicks(stage);
        return 0;
    }

    public int getChargedAttackChargeTicks() {
        if (talent != null) return talent.getChargeTicks();
        return 20;
    }

    public int getChargedAttackPrecastTicks() {
        if (talent != null) return talent.getChargedPrecastTicks();
        return 5;
    }

    public int getChargedAttackPostcastTicks() {
        if (talent != null) return talent.getChargedPostcastTicks();
        return 15;
    }

    public int getSkillPrecastTicks() {
        if (talent != null) return talent.getSkillPrecastTicks();
        return 5;
    }

    public int getSkillPostcastTicks() {
        if (talent != null) return talent.getSkillPostcastTicks();
        return 10;
    }

    public int getBurstPrecastTicks() {
        if (talent != null) return talent.getBurstPrecastTicks();
        return 10;
    }

    public int getBurstPostcastTicks() {
        if (talent != null) return talent.getBurstPostcastTicks();
        return 20;
    }

    public void frontTick(Player player) {}

    public void backTick(Player player) {}

    public void tick(Player player) {
        data.tick();
        recalculateDirtyArtifactSlots();
        frontTick(player);
        backTick(player);
        if (!player.level().isClientSide()) {
            ActionManager.get(player).tick(player, this);
            syncRealtimeState();
        }
    }

    // ============ 圣遗物 / 武器 ============

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
        if (slotIndex == ArtifactInventory.SLOT_WEAPON) {
            recalculateWeaponSlot();
            return;
        }
        ArtifactType type = ArtifactInventory.slotToType(slotIndex);
        String source = null;
        if (type != null) {
            source = type.name().toLowerCase();
        }

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
    }

    public void recalculateWeaponSlot() {
        for (AttributeType attrType : ModRegistries.ATTRIBUTE_TYPE_REGISTRY) {
            data.removeAttributeModifier(attrType, SOURCE_WEAPON);
        }

        data.removeAttributeBaseValue(ModAttributes.ATK.get(), SOURCE_WEAPON);
        data.setWeaponBaseATK(0);

        ItemStack stack = data.getArtifactInventory().getItem(ArtifactInventory.SLOT_WEAPON);
        if (stack.isEmpty() || !(stack.getItem() instanceof WeaponItem weapon)) return;

        WeaponStatsComponent stats = stack.getOrDefault(
                ModDataComponents.WEAPON_STATS.get(), WeaponStatsComponent.DEFAULT);

        if (stats.mainStat != null && stats.mainStat.isInitialized()) {
            double mainValue = stats.mainStat.getValue();
            data.setWeaponBaseATK(mainValue);
            data.setAttributeBaseValue(ModAttributes.ATK.get(), SOURCE_WEAPON, mainValue);
        }

        if (stats.subStat != null && stats.subStat.isInitialized()) {
            AttributeType attr = stats.subStat.getAttribute();
            double value = stats.subStat.getValue();
            if (isBaseAttribute(attr)) {
                data.setAttributePercentModifier(attr, SOURCE_WEAPON, value);
            } else {
                data.setAttributeFlatModifier(attr, SOURCE_WEAPON, value);
            }
        }
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
                .map(CharacterEffectInstance::getEffect)
                .filter(effect -> effect instanceof ArtifactSetEffect)
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
            if (count >= 2) {
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

    public void hurt(float amount) {
        double before = data.getCurrentHP();
        data.hurtHP(amount);
        syncRealtimeState();
        if (data.getCurrentHP() <= 0 && before > 0) {
            incapacitate();
        }
    }

    public void incapacitate() {
        Player player = data.getOwnerPlayer();
        if (player == null) return;
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        int currentIndex = attachment.getCurrentCharacterIndex();
        for (int offset = 1; offset <= 4; offset++) {
            int nextIndex = (currentIndex + offset) % 4;
            PGCharacter nextChar = attachment.getPartyCharacter(nextIndex);
            if (nextChar != null && nextChar.getData().getCurrentHP() > 0) {
                attachment.setCurrentCharacterIndex(nextIndex);
                if (player instanceof ServerPlayer sp) {
                    NetworkManager.setCharacterSelectionToPlayer(sp, nextIndex);
                }
                return;
            }
        }

        player.setData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT, false);
        if (player instanceof ServerPlayer sp) {
            NetworkManager.setGenshinModeToPlayer(sp, false);
        }
    }

    public void revive(int hp) {
        float maxHp = (float) data.getAttributeTotalValue(ModAttributes.MAX_HP.value());
        data.setCurrentHP(Math.min(hp, maxHp));
        enableDeployIfNeeded();
        syncRealtimeState();
    }

    public void revive(float percent) {
        float maxHp = (float) data.getAttributeTotalValue(ModAttributes.MAX_HP.value());
        data.setCurrentHP(Math.min(maxHp * percent, maxHp));
        enableDeployIfNeeded();
        syncRealtimeState();
    }

    public void revive(float percent, int extraHp) {
        float maxHp = (float) data.getAttributeTotalValue(ModAttributes.MAX_HP.value());
        data.setCurrentHP(Math.min(maxHp * percent + extraHp, maxHp));
        enableDeployIfNeeded();
        syncRealtimeState();
    }

    private void enableDeployIfNeeded() {
        if (data.getCurrentHP() <= 0) return;
        Player player = data.getOwnerPlayer();
        if (player == null) return;
        Boolean genshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
        if (!genshinMode) {
            player.setData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT, true);
            if (player instanceof ServerPlayer sp) {
                NetworkManager.setGenshinModeToPlayer(sp, true);
            }
        }
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
        return list.getFirst();
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
        var expList = CharacterXpConfig.getAllXp();
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
        var expList = CharacterXpConfig.getAllXp();
        int totalExpConsumed = 0;
        int levelsToGain = 0;
        int oldLevel = data.getLevel();
        int currentAscensionPhase = data.getAscensionPhase();
        while (oldLevel < 90) {
            int expNeeded = expList.get(oldLevel - 1);
            if (data.getCurrentExp() - totalExpConsumed < expNeeded) break;
            int maxLevelForPhase = currentAscensionPhase == 0
                    ? 20
                    : Math.min((currentAscensionPhase + 3) * 10, 90);
            if (oldLevel >= maxLevelForPhase) break;
            totalExpConsumed += expNeeded;
            levelsToGain++;
            oldLevel++;
        }
        if (levelsToGain == 0) return;
        data.setCurrentExp(data.getCurrentExp() - totalExpConsumed);
        data.addLevel(levelsToGain);
        int statIndex = data.getLevel() - 1 + data.getAscensionPhase();
        updateBaseStatsFromConfig(statIndex);
        data.setCurrentHP(data.getAttributeTotalValue(ModAttributes.MAX_HP.value()));

        if (data.getLevel() < 90) {
            data.setMaxExp(expList.get(data.getLevel() - 1));
        }
    }

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

        if (CharacterAscendAttribute.PERCENT_STATS.contains(ascendAttr)) {
            data.removeAttributeModifier(targetAttrType, "ascension_bonus");
        } else {
            data.removeAttributeBaseValue(targetAttrType, "character_ascend");
        }

        int bonusCount = getAscensionBonusCount(newPhase);
        if (bonusCount > 0) {
            applyAscendBonus(ascendAttr, targetAttrType, starRating, bonusCount);
        }

        tryLevelUp();
    }

    private void applyAscendBonus(CharacterAscendAttribute attr, AttributeType type,
                                  int starRating, int bonusCount) {
        int factor = starRating + 1;
        String baseKey = "character_ascend";
        switch (attr) {
            case ATK, HP:
                data.addAttributePercentModifier(type, "ascension_bonus", 0.012f * factor * bonusCount);
                break;
            case DEF:
                data.addAttributePercentModifier(type, "ascension_bonus", 0.015f * factor * bonusCount);
                break;
            case CR:
                data.setAttributeBaseValue(type, baseKey, 0.008 * factor * bonusCount);
                break;
            case CDG:
                data.setAttributeBaseValue(type, baseKey, 0.016 * factor * bonusCount);
                break;
            case HB:
                data.setAttributeBaseValue(type, baseKey, (starRating == 5 ? 0.056 : 0.047) * bonusCount);
                break;
            case ELEMENTAL_BONUS:
                data.setAttributeBaseValue(type, baseKey, 0.012 * factor * bonusCount);
                break;
            case EM:
                data.setAttributeBaseValue(type, baseKey, (double) (starRating == 5 ? 29 : 24) * bonusCount);
                break;
            case ER:
                data.setAttributeBaseValue(type, baseKey, (0.013333 * factor) * bonusCount);
                break;
        }
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
            case CR -> ModAttributes.CR.value();
            case CDG -> ModAttributes.CDG.value();
            case HB -> ModAttributes.HB.value();
            case ELEMENTAL_BONUS -> getElementalDamageBonusType();
            case EM -> ModAttributes.ELEMENTAL_MASTERY.value();
            case ER -> ModAttributes.ER.value();
        };
    }

    private AttributeType getElementalDamageBonusType() {
        GenshinElement element = getElemental();
        if (element == ModElements.PYRO.get()) return ModAttributes.PYRO_BONUS.value();
        if (element == ModElements.HYDRO.get()) return ModAttributes.HYDRO_BONUS.value();
        if (element == ModElements.DENDRO.get()) return ModAttributes.DENDRO_BONUS.value();
        if (element == ModElements.ELECTRO.get()) return ModAttributes.ELECTRO_BONUS.value();
        if (element == ModElements.ANEMO.get()) return ModAttributes.ANEMO_BONUS.value();
        if (element == ModElements.CYRO.get()) return ModAttributes.CYRO_BONUS.value();
        if (element == ModElements.GEO.get()) return ModAttributes.GEO_BONUS.value();
        if (element == ModElements.FYSIKOS.get()) return ModAttributes.PHYSICAL_BONUS.value();
        return ModAttributes.PHYSICAL_BONUS.value();
    }

    public void upgradeNormalAttack() { data.upgradeNormalAttack(); }
    public void upgradeElementalSkill() { data.upgradeElementalSkill(); }
    public void upgradeElementalBurst() { data.upgradeElementalBurst(); }

    public GenshinElement getElemental() {
        if (elemental != null) return elemental;
        if (elementalId != null && !elementalId.isEmpty()) {
            String[] parts = elementalId.split(":", 2);
            Identifier id = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
            elemental = ModRegistries.ELEMENT_REGISTRY.get(id).map(Holder.Reference::value).orElse(null);
            return elemental;
        }
        return ModElements.FYSIKOS.get();
    }

    public int getSkillShortMaxCooldownTick() { return data.getSkillShortMaxCooldownTick(); }
    public int getSkillLongMaxCooldownTick() { return data.getSkillLongMaxCooldownTick(); }
    public int getBurstMaxCooldownTick() { return data.getBurstMaxCooldownTick(); }
    public float getMaxObtainingEnergy() { return data.getMaxObtainingEnergy(); }

    // ==================== HUD 显示 CD（角色可覆写） ====================

    /** HUD 显示用的战技剩余 CD。默认返回内部真实值。 */
    public float getSkillDisplayCooldown() {
        return data.getElementalSkillCooldownTick();
    }

    /** HUD 显示用的战技 CD 上限，用于进度条比例。 */
    public int getSkillDisplayMaxCooldown() {
        return data.getSkillShortMaxCooldownTick();
    }

    /** HUD 显示用的元素爆发剩余 CD。 */
    public float getBurstDisplayCooldown() {
        return data.getElementalBurstCooldownTick();
    }

    /** HUD 显示用的元素爆发 CD 上限。 */
    public int getBurstDisplayMaxCooldown() {
        return data.getBurstMaxCooldownTick();
    }

    private void updateBaseStatsFromConfig(int statIndex) {
        for (AttributeType type : this.getStatGrowthTypes()) {
            int value = this.getStatAtLevel(type, statIndex);
            data.setAttributeBaseValue(type, value);
        }
    }

    public void syncRealtimeState() {
        data.syncToClient();
        syncToClient();
    }
}