package com.linweiyun.genshin.content.items.character.data;

import com.linweiyun.genshin.Config;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.content.items.artifacts.*;
import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.linweiyun.genshin.content.items.components.DataComponentCharacter;
import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.core.attributes.MGAttribute;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerCharacterData implements IPersistedSerializable {

  @Persisted(key = "character_level")
  private int characterLevel;

  @Persisted(key = "max_exp")
  private int maxExp;

  @Persisted(key = "current_exp")
  protected int currentExp;

  @Persisted(key = "max_hp")
  protected MGAttribute maxHP;

  @Persisted(key = "atk")
  protected MGAttribute ATK;

  @Persisted(key = "def")
  protected MGAttribute DEF;

  @Persisted(key = "current_hp")
  protected double currentHP;

  //突破等级
  @Persisted(key = "ascension_phase")
  protected int ascensionPhase;

  @Persisted(key = "normal_attack_level")
  protected int normalAttackLevel;

  @Persisted(key = "charged_attack_level")
  protected int chargedAttackLevel;

  @Persisted(key = "plunging_attack_level")
  protected int plungingAttackLevel;

  @Persisted(key = "elemental_skill_level")
  protected int elementalSkillLevel;

  @Persisted(key = "elemental_burst_level")
  protected int elementalBurstLevel;

  @Persisted(key = "current_obtaining_energy")
  protected float currentObtainingEnergy;

  @Persisted(key = "elemental_skill_cooldown_tick")
  protected float elementalSkillCooldownTick;

  @Persisted(key = "elemental_burst_cooldown_tick")
  protected float elementalBurstCooldownTick;

  private FlowerArtifact flower;

  private PlumArtifact plume;
  private SandsArtifact sands;
  private GobletArtifact goblet;
  private CircletArtifact circlet;
  private Map<ArtifactSlot, Artifact> equipment;

  public PlayerCharacterData() {
    this.characterLevel = 1;
    this.currentExp = 0;
    this.maxExp = 1000;
    this.maxHP = new MGAttribute();
    this.ATK = new MGAttribute();
    this.DEF = new MGAttribute();
    this.currentHP = this.maxHP.getTotalValue(true);
    this.ascensionPhase = 0;
    this.normalAttackLevel = 1;
    this.chargedAttackLevel = 1;
    this.plungingAttackLevel = 1;
    this.elementalSkillLevel = 1;
    this.elementalBurstLevel = 1;
    this.currentObtainingEnergy = 0;
    this.elementalSkillCooldownTick = 0;
    this.elementalBurstCooldownTick = 0;
  }

  public PlayerCharacterData(PlayerCharacterData original) {
    this.characterLevel = original.characterLevel;
    this.currentExp = original.currentExp;
    this.maxExp = original.maxExp;
    this.maxHP = original.maxHP;
    this.ATK = original.ATK;
    this.DEF = original.DEF;
    this.currentHP = original.currentHP;
    this.ascensionPhase = original.ascensionPhase;
    this.normalAttackLevel = original.normalAttackLevel;
    this.chargedAttackLevel = original.chargedAttackLevel;
    this.plungingAttackLevel = original.plungingAttackLevel;
    this.elementalSkillLevel = original.elementalSkillLevel;
    this.elementalBurstLevel = original.elementalBurstLevel;
    this.currentObtainingEnergy = original.currentObtainingEnergy;
    this.elementalSkillCooldownTick = original.elementalSkillCooldownTick;
    this.elementalBurstCooldownTick = original.elementalBurstCooldownTick;
  }

  public PlayerCharacterData(double maxHP, double ATK, double DEF) {
    this.characterLevel = 1;
    this.currentExp = 0;
    this.maxExp = 1000;
    this.maxHP = new MGAttribute(maxHP);
    this.ATK = new MGAttribute(ATK);
    this.DEF = new MGAttribute(DEF);
    this.currentHP = this.maxHP.getTotalValue(true);
    this.ascensionPhase = 0;
    this.normalAttackLevel = 1;
    this.chargedAttackLevel = 1;
    this.plungingAttackLevel = 1;
    this.elementalSkillLevel = 1;
    this.elementalBurstLevel = 1;
    this.currentObtainingEnergy = 0;
    this.elementalSkillCooldownTick = 0;
    this.elementalBurstCooldownTick = 0;
  }

  public void changeData(ItemStack stack, Consumer<PlayerCharacterData> modifier, Player player) {
    modifier.accept(this);
    if (stack == null || !(stack.getItem() instanceof PlayerCharacter)) {
      return;
    }
    stack.set(
        DataComponentRegistryCharacter.CHARACTER_DATA.get(),
        new DataComponentCharacter(this.copy()));
    CompoundTag tag = this.serializeNBT(player.registryAccess());
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    int slot = characterParty.getSlotByCharacter(stack);
    if (player instanceof ServerPlayer) {
      NetworkManager.setCharacterDataToPlayer((ServerPlayer) player, tag, slot);
    } else {
      NetworkManager.setCharacterDataToServer(tag, slot);
    }
  }

  public int getLevel() {
    return characterLevel;
  }
  public int getCurrentExp() {
    return currentExp;
  }
  public int getMaxExp() {
    return maxExp;
  }

  public MGAttribute getMaxHP() {
    return maxHP;
  }

  public MGAttribute getATK() {
    return ATK;
  }

  public MGAttribute getDEF() {
    return DEF;
  }

  public double getCurrentHP() {
    return currentHP;
  }

  public int getAscensionPhase() {
    return ascensionPhase;
  }

  public int getNormalAttackLevel() {
    return normalAttackLevel;
  }

  public int getChargedAttackLevel() {
    return chargedAttackLevel;
  }

  public int getPlungingAttackLevel() {
    return plungingAttackLevel;
  }

  public int getElementalSkillLevel() {
    return elementalSkillLevel;
  }

  public int getElementalBurstLevel() {
    return elementalBurstLevel;
  }

  public float getCurrentObtainingEnergy() {
    return currentObtainingEnergy;
  }

  public float getElementalSkillCooldownTick() {
    return elementalSkillCooldownTick;
  }

  public float getElementalBurstCooldownTick() {
    return elementalBurstCooldownTick;
  }

  public void setCurrentHP(double currentHP) {
    System.out.println(currentHP);
    this.currentHP = currentHP;
  }

  public void setLevel(int level) {
    this.characterLevel = Math.max(1, Math.min(level, 90));
  }
  public void setMaxExp(int maxExp) {
    this.maxExp = maxExp;
  }

  public void setCurrentExp(int currentExp) {
    this.currentExp = currentExp;
  }

  public void setAscensionPhase(int ascensionPhase) {
    this.ascensionPhase = ascensionPhase;
  }

  public void setNormalAttackLevel(int normalAttackLevel) {
    this.normalAttackLevel = normalAttackLevel;
  }

  public void setChargedAttackLevel(int chargedAttackLevel) {
    this.chargedAttackLevel = chargedAttackLevel;
  }

  public void setPlungingAttackLevel(int plungingAttackLevel) {
    this.plungingAttackLevel = plungingAttackLevel;
  }

  public void setElementalSkillLevel(int elementalSkillLevel) {
    this.elementalSkillLevel = elementalSkillLevel;
  }

  public void setElementalBurstLevel(int elementalBurstLevel) {
    this.elementalBurstLevel = elementalBurstLevel;
  }

  public void setCurrentObtainingEnergy(float currentObtainingEnergy) {
    this.currentObtainingEnergy = currentObtainingEnergy;
  }

  public void setElementalSkillCooldownTick(float elementalSkillCooldownTick) {
    this.elementalSkillCooldownTick = elementalSkillCooldownTick;
  }

  public void setElementalBurstCooldownTick(float elementalBurstCooldownTick) {
    this.elementalBurstCooldownTick = elementalBurstCooldownTick;
  }

  public void healHP(double amount) {
    this.currentHP = Math.min(this.currentHP + amount, this.maxHP.getTotalValue(true));
  }

  public void hurtHP(float amount) {
    this.currentHP = (int) Math.max(0, this.currentHP - amount);
  }

  public void addLevel(int levelsToAdd) {
    setLevel(this.characterLevel + levelsToAdd);
  }

  public static final Codec<PlayerCharacterData> CODEC =
      PersistedParser.createCodec(PlayerCharacterData::new);

  public static Codec<PlayerCharacterData> getCodec() {
    return CODEC;
  }

  public static final StreamCodec<ByteBuf, PlayerCharacterData> STREAM_CODEC =
      PersistedParser.createStreamCodec(PlayerCharacterData::new);

  public static StreamCodec<ByteBuf, PlayerCharacterData> getStreamCodec() {
    return STREAM_CODEC;
  }

  // 复制方法 - 用于 customMark
  public PlayerCharacterData copy() {
    PlayerCharacterData copy = new PlayerCharacterData();
    copy.characterLevel = this.characterLevel;
    copy.currentExp = this.currentExp;
    copy.maxExp = this.maxExp;
    copy.maxHP = this.maxHP.copy();
    copy.ATK = this.ATK.copy();
    copy.DEF = this.DEF.copy();
    copy.currentHP = this.currentHP;
    copy.ascensionPhase = this.ascensionPhase;
    copy.normalAttackLevel = this.normalAttackLevel;
    copy.chargedAttackLevel = this.chargedAttackLevel;
    copy.plungingAttackLevel = this.plungingAttackLevel;
    copy.elementalSkillLevel = this.elementalSkillLevel;
    copy.elementalBurstLevel = this.elementalBurstLevel;
    copy.currentObtainingEnergy = this.currentObtainingEnergy;
    copy.elementalSkillCooldownTick = this.elementalSkillCooldownTick;
    copy.elementalBurstCooldownTick = this.elementalBurstCooldownTick;
    return copy;
  }

  public boolean equals(PlayerCharacterData a, PlayerCharacterData b) {
    return a.characterLevel == b.characterLevel && a.maxHP.equals(a.maxHP, b.maxHP);
  }
}
