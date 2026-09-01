package com.linweiyun.genshin.core.system.combat;

import com.linweiyun.genshin.content.effects.entities.EffectRegistry;
import com.linweiyun.genshin.content.effects.entities.elemental_infusion_effect.ElementalInfusionEffect;
import com.linweiyun.genshin.content.effects.entities.instance.MobEffectInstanceAboutElemental;
import com.linweiyun.genshin.content.effects.itemstacks.ItemStackEffectHelper;
import com.linweiyun.genshin.content.effects.itemstacks.character.skill.shenhe.IcyQuillEffect;
import com.linweiyun.genshin.content.effects.itemstacks.instance.ItemStackEffectInstance;
import com.linweiyun.genshin.content.entities.TeyvatLivingEntity;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.content.entities.attachments.attachment.PlayerGenshinModeAttachment;
import com.linweiyun.genshin.content.entities.damagesource.ElementalDamageSourceGIM;
import com.linweiyun.genshin.content.entities.damagesource.ElementalDamageSourcesGIM;
import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.linweiyun.genshin.content.items.character.player_character.catalyst.CatalystCharacter;
import com.linweiyun.genshin.content.items.components.DataComponentCharacter;
import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.enums.AttachmentType;
import com.linweiyun.genshin.enums.DamageTypeEnum;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.linweiyun.genshin.enums.ElementalsGIM;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CombatHelper {
  public static void hurt(
      LivingEntity attackEntity,
      ItemStack attackCharacter,
      LivingEntity target,
      DamageTypeEnum damageType,
      float attackAmp) {
    ElementalsGIM elemental = ElementalsGIM.FYSIKOS;
    if (!attackEntity.level().isClientSide) {
      if (attackEntity instanceof Player attacker) {
        DeferredHolder<MobEffect, ElementalInfusionEffect> elementalEffect =
            whichElementalAttackEffect(attacker);
        if (elementalEffect != null) {
          elemental = elementalEffect.value().getElemental();
        }
      }
    }
    hurt(attackEntity, attackCharacter, target, damageType, elemental, attackAmp, 0);
  }

  public static void hurt(
      LivingEntity attackEntity,
      ItemStack attackCharacter,
      LivingEntity target,
      DamageTypeEnum damageType,
      float attackAmp,
      float attackDamage) {
    ElementalsGIM elemental = ElementalsGIM.FYSIKOS;
    if (!attackEntity.level().isClientSide) {
      if (attackEntity instanceof Player attacker) {
        DeferredHolder<MobEffect, ElementalInfusionEffect> elementalEffect =
            whichElementalAttackEffect(attacker);
        if (elementalEffect != null) {
          elemental = elementalEffect.value().getElemental();
        }
      }
    }
    hurt(attackEntity, attackCharacter, target, damageType, elemental, attackAmp, attackDamage);
  }

  public static void hurt(
      LivingEntity attackEntity,
      ItemStack attackCharacter,
      LivingEntity target,
      DamageTypeEnum damageType,
      ElementalsGIM elemental,
      float attackAmp,
      float attackDamage) {
    ElementalDamageSourceGIM damageSource = calculateDamageSource(attackEntity, damageType);
    if (damageSource != null) {

      damageSource.setElemental(elemental);
      float endDamage =
          calculateDamage(
              attackEntity,
              attackCharacter,
              target,
              damageType,
              elemental,
              attackAmp,
              attackDamage);
      target.hurt(damageSource, endDamage);
    }
  }

  public static float calculateDamage(
      Entity attackEntity,
      ItemStack attackCharacter,
      LivingEntity target,
      DamageTypeEnum damageType,
      ElementalsGIM elemental,
      float attackAmp,
      float attackDamage) {
    double characterATK = 0;
    float reactAmp = 1.0f;
    float baseAmp;
    boolean attackIsGenshinMode = entityIsGenshinMode(attackEntity);
    boolean targetIsGenshinMode = entityIsGenshinMode(target);
    if (attackEntity instanceof Player attacker && attackIsGenshinMode) {
      if (attackCharacter.getItem() instanceof CatalystCharacter catalystCharacter
          && damageType == DamageTypeEnum.NORMAL_ATTACK) {
        elemental = catalystCharacter.getElement();
      }
      addElementalAboutToTarget(target, true, elemental, AttachmentType.WEAK);
      if (!(attacker.getMainHandItem().getItem() instanceof TieredItem)
          && damageType == DamageTypeEnum.NORMAL_ATTACK) {
        return (float) attacker.getAttributes().getValue(Attributes.ATTACK_DAMAGE);
      } else {
        PlayerCharacterData currentData = CombatHelper.getCharacterData(attackCharacter);
        if (currentData != null) {
          characterATK = currentData.getATK().getTotalValue(true);
        }

        ElementalReactionType reactType = getReaction(target, elemental);
        if (reactType == ElementalReactionType.FROZEN) {
          reactAmp = 1.5f;
        }
        baseAmp = baseAmpCalculate(currentData, elemental, attackCharacter, attacker);
        attackDamage =
            (float)
                (((characterATK * attackAmp + baseAmp) * reactAmp)
                    + attacker.getAttributes().getValue(Attributes.ATTACK_DAMAGE));
      }
    }

    if (attackIsGenshinMode && !targetIsGenshinMode) {
      attackDamage = mapGenshinDamageToMC(attackDamage);
      if (attackEntity instanceof LivingEntity) {
        attackDamage =
            (float)
                (attackDamage
                    + ((LivingEntity) attackEntity)
                        .getAttributes()
                        .getValue(Attributes.ATTACK_DAMAGE));
      }
    } else if (!attackIsGenshinMode && targetIsGenshinMode) {
      attackDamage = reverseMapMCToGenshin(attackDamage);
    }
    if (target instanceof Player targetPlayer && targetIsGenshinMode) {
      CharacterParty targetSelectionAttachment =
          targetPlayer.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
      ItemStack targetStack = targetSelectionAttachment.getCurrentCharacter();
      PlayerCharacterData targetData = CombatHelper.getCharacterData(targetStack);
      if (targetData != null) {
        float oldHP = (float) targetData.getCurrentHP();
        float finalAttackDamage = attackDamage;
        if (finalAttackDamage > oldHP) {
          targetData.changeData(targetStack, data -> data.setCurrentHP(0), targetPlayer);
          CharacterParty party =
              targetPlayer.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
          int nextIndex = findNextAliveCharacterIndex(party) + 1;
          if (nextIndex != party.getCurrentCharacterSlot()) {
            party.setCurrentCharacter(nextIndex, (ServerPlayer) targetPlayer);
            attackDamage = 0;
          } else {
            attackDamage = finalAttackDamage - oldHP;
          }
        } else {
          targetData.changeData(targetStack, data -> data.hurtHP(finalAttackDamage), targetPlayer);
          attackDamage = finalAttackDamage - oldHP;
        }
      }
    }
    return attackDamage;
  }

  public static int findNextAliveCharacterIndex(CharacterParty party) {
    ItemStack[] items = party.getItems();
    int currentSlot = party.getCurrentCharacterSlot() - 1;
    for (int i = 1; i < items.length; i++) {
      int checkSlot = (currentSlot + i) % items.length;
      ItemStack stack = items[checkSlot];
      if (stack.isEmpty()) continue;
      if (stack == party.getCurrentCharacter()) continue;
      PlayerCharacterData data = getCharacterData(stack);
      if (data != null && data.getCurrentHP() > 0) {
        return checkSlot;
      }
    }
    return party.getCurrentCharacterSlot();
  }

  public static float calculateDamage(
      Entity attackEntity,
      ItemStack attackCharacter,
      LivingEntity target,
      DamageTypeEnum damageType,
      float attackAmp) {
    ElementalsGIM elemental = ElementalsGIM.FYSIKOS;
    if (!attackEntity.level().isClientSide) {
      if (attackEntity instanceof Player attacker) {
        DeferredHolder<MobEffect, ElementalInfusionEffect> elementalEffect =
            whichElementalAttackEffect(attacker);
        if (elementalEffect != null) {
          elemental = elementalEffect.value().getElemental();
        }
      }
    }
    return calculateDamage(
        attackEntity, attackCharacter, target, damageType, elemental, attackAmp, 0);
  }

  public static float calculateDamage(
      Entity attackEntity,
      ItemStack attackCharacter,
      LivingEntity target,
      DamageTypeEnum damageType,
      float attackAmp,
      float attackDamage) {
    ElementalsGIM elemental = ElementalsGIM.FYSIKOS;
    if (attackEntity instanceof Player attacker) {
      DeferredHolder<MobEffect, ElementalInfusionEffect> elementalEffect =
          whichElementalAttackEffect(attacker);
      if (elementalEffect != null) {
        elemental = elementalEffect.value().getElemental();
      }
    }
    return calculateDamage(
        attackEntity, attackCharacter, target, damageType, elemental, attackAmp, attackDamage);
  }

  public static boolean entityIsGenshinMode(Entity entity) {
    if (entity != null) {
      if (entity instanceof Player player) {
        PlayerGenshinModeAttachment genshinModeAttachment =
            player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
        return genshinModeAttachment.isGenshinMode();
      } else return entity instanceof TeyvatLivingEntity teyvatEntity;
    }

    return false;
  }

  private static float baseAmpCalculate(
      PlayerCharacterData currentData,
      ElementalsGIM elementalsGIM,
      ItemStack stack,
      Player player) {
    if (currentData != null) {
      float baseDamage = 0;
      List<ItemStackEffectInstance> effectList = ItemStackEffectHelper.getEffects(stack).effects();
      switch (elementalsGIM) {
        case CYRO:
          for (ItemStackEffectInstance instance : effectList) {
            if (instance.getEffect() instanceof IcyQuillEffect icyQuillEffect) {
              baseDamage = instance.getIntData(IcyQuillEffect.icyQuillDamageKey);
              int count = instance.getIntData(IcyQuillEffect.icyQuillCountKey);
              icyQuillEffect.reduceQuills(stack, 1, player);
            }
          }
          break;
        case DENDRO:
        default:
          return baseDamage;
      }
      return baseDamage;
    }
    return 0;
  }

  public static PlayerCharacterData getCharacterData(ItemStack stack) {
    DataComponentCharacter component =
        stack.get(DataComponentRegistryCharacter.CHARACTER_DATA.get());
    if (component != null) {
      return component.characterData();
    }
    return null;
  }

  public static ElementalDamageSourceGIM calculateDamageSource(
      Entity attackerEntity, DamageTypeEnum damageType) {
    switch (damageType) {
      case NORMAL_ATTACK -> {
        return ElementalDamageSourcesGIM.NORMAL_ATTACK_DAMAGE_SOURCE(attackerEntity);
      }
      case CHARGED_ATTACK -> {
        return ElementalDamageSourcesGIM.CHARGED_ATTACK_DAMAGE_SOURCE(attackerEntity);
      }
      case PLUNGING_ATTACK -> {
        return ElementalDamageSourcesGIM.PLUNGING_ATTACK_DAMAGE_SOURCE(attackerEntity);
      }
      case ELEMENTAL_SKILL -> {
        return ElementalDamageSourcesGIM.ELEMENTAL_SKILL_ATTACK_DAMAGE_SOURCE(attackerEntity);
      }
      case ELEMENTAL_BURST -> {
        return ElementalDamageSourcesGIM.ELEMENTAL_BURST_DAMAGE_SOURCE(attackerEntity);
      }
      case MONSTER_ATTACK -> {
        return ElementalDamageSourcesGIM.MONSTER_DAMAGE_SOURCE(attackerEntity);
      }
    }
    return null;
  }

  private static void addElementalAboutToTarget(
      LivingEntity target,
      boolean isIndependent,
      ElementalsGIM elemental,
      AttachmentType attachmentType) {
    MobEffectInstance instance = null;
    float attachmentAmount = attachmentType.getInitialAmount();
    instance =
        switch (elemental) {
          case PYRO -> new MobEffectInstanceAboutElemental(
              EffectRegistry.ABOUT_PYRO, attachmentType, attachmentAmount, true, true);
          case HYDRO -> new MobEffectInstanceAboutElemental(
              EffectRegistry.ABOUT_HYDRO, attachmentType, attachmentAmount, true, true);
          case ANEMO -> new MobEffectInstanceAboutElemental(
              EffectRegistry.ABOUT_ANEMO, attachmentType, attachmentAmount, true, true);
          case ELECTRO -> new MobEffectInstanceAboutElemental(
              EffectRegistry.ABOUT_ELECTRO, attachmentType, attachmentAmount, true, true);
          case DENDRO -> new MobEffectInstanceAboutElemental(
              EffectRegistry.ABOUT_DENDRO, attachmentType, attachmentAmount, true, true);
          case CYRO -> new MobEffectInstanceAboutElemental(
              EffectRegistry.ABOUT_CYRO, attachmentType, attachmentAmount, true, true);
          case GEO -> new MobEffectInstanceAboutElemental(
              EffectRegistry.ABOUT_GEO, attachmentType, attachmentAmount, true, true);
          default -> null;
        };
    if (instance != null) {
      target.addEffect(instance);
    }
  }

  public static DeferredHolder<MobEffect, ElementalInfusionEffect> whichElementalAttackEffect(
      LivingEntity attacker) {
    DeferredHolder<MobEffect, ElementalInfusionEffect> whichEffect = null;
    for (DeferredHolder<MobEffect, ElementalInfusionEffect> effect :
        EffectRegistry.ELEMENT_INFUSION_EFFECTS) {
      if (attacker.hasEffect(effect)) {
        whichEffect = effect;
        break;
      }
    }
    return whichEffect;
  }

  public static DeferredHolder<MobEffect, ElementalInfusionEffect> whichElementalAttackEffect(
      LivingEntity attacker, ElementalsGIM elementalsGIM) {
    DeferredHolder<MobEffect, ElementalInfusionEffect> whichEffect = null;
    for (DeferredHolder<MobEffect, ElementalInfusionEffect> effect :
        EffectRegistry.ELEMENT_INFUSION_EFFECTS) {
      if (attacker.hasEffect(effect) && effect.value().getElemental() != elementalsGIM) {
        whichEffect = effect;
        break;
      }
    }
    return whichEffect;
  }

  public static ElementalReactionType getReaction(
      LivingEntity livingEntity, ElementalsGIM elemental) {
    boolean hasFrozen = livingEntity.hasEffect(EffectRegistry.FROZEN);
    boolean hasQuicken = false;
    boolean hasPyro = livingEntity.hasEffect(EffectRegistry.ABOUT_PYRO);
    boolean hasHydro = livingEntity.hasEffect(EffectRegistry.ABOUT_HYDRO);
    boolean hasAnemo = livingEntity.hasEffect(EffectRegistry.ABOUT_ANEMO);
    boolean hasElectro = livingEntity.hasEffect(EffectRegistry.ABOUT_ELECTRO);
    boolean hasDendro = livingEntity.hasEffect(EffectRegistry.ABOUT_DENDRO);
    boolean hasCyro = livingEntity.hasEffect(EffectRegistry.ABOUT_CYRO);
    boolean hasGeo = livingEntity.hasEffect(EffectRegistry.ABOUT_GEO);
    switch (elemental) {
      case ANEMO:
        if (hasPyro || hasHydro || hasCyro || hasElectro) return ElementalReactionType.SWIRL;
        break;
      case CYRO:
        if (hasAnemo) return ElementalReactionType.SWIRL;
        if (hasElectro) return ElementalReactionType.SUPERCONDUCT;
        if (hasHydro) return ElementalReactionType.FROZEN;
        if (hasPyro) return ElementalReactionType.MELT;
        if (hasGeo) return ElementalReactionType.CRYSTALLIZE;
        break;
      case ELECTRO:
        if (hasAnemo) return ElementalReactionType.SWIRL;
        if (hasCyro) return ElementalReactionType.SUPERCONDUCT;
        if (hasHydro && !hasFrozen) return ElementalReactionType.ELECTRO_CHARGED;
        if (hasPyro) return ElementalReactionType.OVERLOAD;
        if (hasDendro) return ElementalReactionType.QUICKEN;
        if (hasGeo) return ElementalReactionType.CRYSTALLIZE;
        break;
      case HYDRO:
        if (hasAnemo) return ElementalReactionType.SWIRL;
        if (hasCyro) return ElementalReactionType.FROZEN;
        if (hasElectro && !hasFrozen) return ElementalReactionType.ELECTRO_CHARGED;
        if (hasPyro && !hasFrozen) return ElementalReactionType.VAPORIZE;
        if (hasDendro) return ElementalReactionType.BLOOM;
        if (hasGeo) return ElementalReactionType.CRYSTALLIZE;
        break;
      case PYRO:
        if (hasAnemo) return ElementalReactionType.SWIRL;
        if (hasCyro) return ElementalReactionType.MELT;
        if (hasElectro) return ElementalReactionType.OVERLOAD;
        if (hasHydro && !hasFrozen) return ElementalReactionType.VAPORIZE;
        if (hasDendro) return ElementalReactionType.BURNING;
        if (hasGeo) return ElementalReactionType.CRYSTALLIZE;
        break;
      case DENDRO:
        if (hasElectro) return ElementalReactionType.QUICKEN;
        if (hasHydro) return ElementalReactionType.BLOOM;
        if (hasPyro) return ElementalReactionType.BURNING;
        if (hasGeo) return ElementalReactionType.CRYSTALLIZE;
        break;
      case GEO:
        if (hasPyro || hasHydro || hasCyro || hasElectro) return ElementalReactionType.CRYSTALLIZE;
        break;
      default:
        return null;
    }
    return null;
  }

  public static float mapGenshinDamageToMC(float originalDmg) {
    if (originalDmg <= 0) {
      return 0f;
    }
    final float LOW_DMG_THRESHOLD = 5f;
    final float LOW_DMG_MULTIPLIER = 0.98f;
    final float LOG_FACTOR = 2.17f;
    final float BASE_OFFSET = -4.0f;
    float mapped;
    if (originalDmg <= LOW_DMG_THRESHOLD) {
      mapped = originalDmg * LOW_DMG_MULTIPLIER;
    } else {
      mapped = (float) (LOG_FACTOR * Math.log(originalDmg)) + BASE_OFFSET;
    }
    return Math.round(mapped * 10) / 10f;
  }

  public static float reverseMapMCToGenshin(float mcDamage) {
    if (mcDamage <= 0) {
      return 0f;
    }

    final float LOW_DMG_THRESHOLD = 10.0f;
    final float LOW_DMG_MULTIPLIER = 10.0f;
    final float HIGH_DMG_LOG_FACTOR = 120.0f;
    final float HIGH_DMG_BASE = 20.0f;
    float finalDamage;
    if (mcDamage <= LOW_DMG_THRESHOLD) {
      finalDamage = mcDamage * LOW_DMG_MULTIPLIER;
    } else {
      finalDamage = (float) (HIGH_DMG_LOG_FACTOR * Math.log(mcDamage - 9)) + HIGH_DMG_BASE;
    }
    return Math.max(0, Math.round(finalDamage * 10) / 10);
  }
}
