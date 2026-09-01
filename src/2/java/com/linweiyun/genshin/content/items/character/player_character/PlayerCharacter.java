package com.linweiyun.genshin.content.items.character.player_character;

import com.linweiyun.genshin.Config;
import com.linweiyun.genshin.content.effects.itemstacks.ItemStackEffectHelper;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.linweiyun.genshin.core.food.CharacterFoodProperties;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;

import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

public abstract class PlayerCharacter extends Item implements IPersistedSerializable {

  protected int starRating;
  protected int characterUUID;
  protected Component name;
  protected float SKILL_MAX_COOLDOWN_TICK;
  protected float BURST_MAX_COOLDOWN_TICK;
  protected float maxObtainingEnergy;
  protected ElementalsGIM ELEMENTAL;
  protected CharacterAscendAttribute ASCEND_ATTRIBUTE;
  //角色每级基础属性列表
  protected ModConfigSpec.ConfigValue<List<? extends Integer>> CHARACTER_HP;
  protected ModConfigSpec.ConfigValue<List<? extends Integer>> CHARACTER_DEF;
  protected ModConfigSpec.ConfigValue<List<? extends Integer>> CHARACTER_ATK;

  public PlayerCharacter(Properties properties) {

    super(properties.stacksTo(1));
  }

  //角色获得经验的方法
  public void addExperience(ItemStack stack, Player player, int amount) {
    PlayerCharacterData data = CombatHelper.getCharacterData(stack);
    if (data == null) return;
    data.changeData(stack, d -> {
      d.setCurrentExp(d.getCurrentExp() + amount);
    }, player);
    tryLevelUp(stack, player);
  }
  //角色升级的方法
  public void tryLevelUp(ItemStack stack, Player player) {
    PlayerCharacterData data = CombatHelper.getCharacterData(stack);
    if (data == null) return;
    var expList = Config.CHARACTER_UP_EXP.get();
    data.changeData(stack, d -> {
      while (d.getLevel() < 90) {
        int expNeeded = expList.get(d.getLevel() - 1);
        if (d.getCurrentExp() < expNeeded) break;
        int maxLevelForPhase = d.getAscensionPhase() == 0
                ? 20
                : Math.min((d.getAscensionPhase() + 3) * 10, 90);
        if (d.getLevel() >= maxLevelForPhase) break;
        int newExp = d.getCurrentExp() - expNeeded;
        player.sendSystemMessage(Component.literal("当前经验值：" + d.getCurrentExp() + " 升级需要的经验值：" + expNeeded + " 升级后等级：" + (d.getLevel() + 1) + " 升级后的经验值：" + newExp));
        d.setCurrentExp(d.getCurrentExp() - expNeeded);
        d.addLevel(1);
        d.getATK().setBaseValue(CHARACTER_ATK.get().get(d.getLevel() - 1 + d.getAscensionPhase()));
        d.getMaxHP().setBaseValue(CHARACTER_HP.get().get(d.getLevel() - 1 + d.getAscensionPhase()));
        d.setCurrentHP(d.getMaxHP().getTotalValue(true));
        d.getDEF().setBaseValue(CHARACTER_DEF.get().get(d.getLevel() - 1 + d.getAscensionPhase()));
        if (d.getLevel() < 90) {
          d.setMaxExp(expList.get(d.getLevel() - 1));
        }
      }
    }, player);
  }
  //角色突破的方法
  public void ascend(ItemStack stack, Player player) {
    PlayerCharacterData data = CombatHelper.getCharacterData(stack);
    if (data == null) return;
    data.changeData(stack, d -> {
      int maxLevelForPhase = d.getAscensionPhase() == 0
              ? 20
              : Math.min((d.getAscensionPhase() + 3) * 10, 90);
      System.out.println(maxLevelForPhase);
      if (d.getLevel() == maxLevelForPhase) {
        int newAscensionPhase = d.getAscensionPhase() + 1;
        d.setAscensionPhase(newAscensionPhase);
        d.getATK().setBaseValue(CHARACTER_ATK.get().get(maxLevelForPhase - 1 + newAscensionPhase));
        d.getMaxHP().setBaseValue(CHARACTER_HP.get().get(maxLevelForPhase - 1 + newAscensionPhase));
        d.getDEF().setBaseValue(CHARACTER_DEF.get().get(maxLevelForPhase - 1 + newAscensionPhase));
        if (newAscensionPhase == 2 || newAscensionPhase == 3 || newAscensionPhase == 5 || newAscensionPhase == 6) {
          int start = starRating;
          switch (ASCEND_ATTRIBUTE) {
            case ATK:
              d.getATK().addExtraPercent(0.012f * (start + 1));
              break;
            case HP:
              d.getMaxHP().addExtraPercent(0.012f * (start + 1));
              break;
            case DEF:
              d.getDEF().addExtraPercent(0.015f * (start + 1));
              break;
          }
        }
        tryLevelUp(stack, player);
      }

    }, player);
  }
  public void performElementalSkill(Player player) {

    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    ItemStack character = characterParty.getStackByCharacter(this);
    PlayerCharacterData currentData = CombatHelper.getCharacterData(character);
    if (currentData != null && currentData.getElementalSkillCooldownTick() <= 0) {
      elementalSkill(player);
      currentData.changeData(
          character, (data) -> data.setElementalSkillCooldownTick(SKILL_MAX_COOLDOWN_TICK), player);
    } else {
      if (currentData != null) {
        player.sendSystemMessage(
            Component.literal("当前技能CD：" + currentData.getElementalSkillCooldownTick()));
      }
    }
  }

  public void performElementalBurst(Player player) {
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    ItemStack character = characterParty.getStackByCharacter(this);
    PlayerCharacterData currentData = CombatHelper.getCharacterData(character);
    if (currentData != null) {
      if (currentData.getElementalBurstCooldownTick() <= 0) {
        if (currentData.getCurrentObtainingEnergy() == this.maxObtainingEnergy) {
          elementalBurst(player);
          currentData.changeData(character, (data) -> data.setCurrentObtainingEnergy(0), player);
          currentData.changeData(
              character,
              (data) -> data.setElementalBurstCooldownTick(BURST_MAX_COOLDOWN_TICK),
              player);
        } else {
          player.sendSystemMessage(Component.literal("§e当前元素能量不足"));
        }
      } else {
        player.sendSystemMessage(
            Component.literal("§e当前技能CD：" + currentData.getElementalBurstCooldownTick()));
      }
    }
  }

  public abstract void elementalSkill(Player player);

  public abstract void elementalBurst(Player player);

  public int getStarRating() {
    return starRating;
  }

  public int getCharacterUUID() {
    return characterUUID;
  }

  public Component getCharacterName() {
    return name;
  }

  public float getMaxObtainingEnergy() {
    return maxObtainingEnergy;
  }

  public ElementalsGIM getElement() {
    return ELEMENTAL;
  }

  public CharacterAscendAttribute getAscendAttribute() {
    return ASCEND_ATTRIBUTE;
  }

  public int getLevel(ItemStack stack) {
    PlayerCharacterData characterData = CombatHelper.getCharacterData(stack);
    if (characterData != null) {
      return characterData.getLevel();
    }
    return 1;
  }

  public float getSkillMaxCooldown() {
    return SKILL_MAX_COOLDOWN_TICK;
  }

  public float getBurstMaxCooldown() {
    return BURST_MAX_COOLDOWN_TICK;
  }

  public final void eat(Player player, ItemStack foodStack) {
    ItemStack characterStack =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT).getStackByCharacter(this);
    PlayerCharacterData data = CombatHelper.getCharacterData(characterStack);
    if (data == null) return;

    CharacterFoodProperties food =
        foodStack.get(DataComponentRegistryCharacter.CHARACTER_FOOD.get());
    if (food == null) return;

    data.changeData(
        characterStack,
        d -> {
          if (food.fixedValue() > 0) {
            d.healHP(food.fixedValue());
          }
          if (food.percentValue() > 0) {
            double heal = d.getMaxHP().getTotalValue(true) * (food.percentValue() / 100.0);
            d.healHP(heal);
          }
        },
        player);
    food.effects()
        .forEach(
            effect -> {
              ItemStackEffectHelper.addEffect(characterStack, player, effect);
            });
  }

  @Override
  public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
    return PersistedParser.serializeNBT(this, provider);
  }

  @Override
  public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
    PersistedParser.deserializeNBT(tag, this, provider);
  }
}
