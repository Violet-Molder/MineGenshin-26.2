package com.linweiyun.genshin.content.effects.entities.about_elemental_effect;

import com.linweiyun.genshin.content.effects.entities.EffectRegistry;
import com.linweiyun.genshin.content.effects.entities.ElementalEffect;
import com.linweiyun.genshin.content.effects.entities.elemental_reaction.ElementalReactionHandler;
import com.linweiyun.genshin.content.effects.entities.instance.MobEffectInstanceAboutElemental;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

public class AboutElementalEffect extends ElementalEffect {
  public AboutElementalEffect(MobEffectCategory category, int color, ElementalsGIM elemental) {
    super(category, color, elemental);
  }

  @Override
  public void onEffectAdded(@NotNull LivingEntity livingEntity, int amplifier) {
    DeferredHolder<MobEffect, AboutElementalEffect> effectHolder =
        getEffectHolderByElemental(getElemental());
    MobEffectInstance instance;
    if (effectHolder != null) {
      instance = livingEntity.getEffect(effectHolder);
      if (instance instanceof MobEffectInstanceAboutElemental elementalInstance) {
        ElementalReactionType reaction = CombatHelper.getReaction(livingEntity, getElemental());
        if (reaction == null) {
          elementalInstance.setAttachmentAmount(elementalInstance.getAttachmentAmount() * 0.8f);
        } else {
          if (reaction == ElementalReactionType.FROZEN) {
            ElementalReactionHandler.executeElementalReaction(
                livingEntity, reaction, getElemental());
          }
        }
      }
    }

    super.onEffectAdded(livingEntity, amplifier);
  }

  @Override
  public boolean applyEffectTick(@NotNull LivingEntity livingEntity, int amplifier) {
    DeferredHolder<MobEffect, AboutElementalEffect> effectHolder =
        getEffectHolderByElemental(getElemental());
    MobEffectInstance instance;
    if (effectHolder != null) {

      instance = livingEntity.getEffect(effectHolder);
      if (instance instanceof MobEffectInstanceAboutElemental elementalInstance) {
        if (elementalInstance.getAttachmentAmount() <= 0) {
          livingEntity.removeEffect(effectHolder);
        }
        int ticker = elementalInstance.getTickCounter();
        if (ticker < 20) {
          elementalInstance.addTick();
        } else {
          elementalInstance.decayOnce();
          livingEntity.sendSystemMessage(
              Component.literal("元素量衰减至" + elementalInstance.getAttachmentAmount()));
          elementalInstance.resetTickCounter();
          if (elementalInstance.getAttachmentAmount() <= 0) {
            livingEntity.removeEffect(effectHolder);
            livingEntity.sendSystemMessage(Component.literal("元素量衰减结束" + getElemental()));
          }
        }
      }
    }

    super.applyEffectTick(livingEntity, amplifier);
    return true;
  }

  @Override
  public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
    return true;
  }

  private DeferredHolder<MobEffect, AboutElementalEffect> getEffectHolderByElemental(
      ElementalsGIM elemental) {
    return switch (elemental) {
      case PYRO -> EffectRegistry.ABOUT_PYRO;
      case HYDRO -> EffectRegistry.ABOUT_HYDRO;
      case ANEMO -> EffectRegistry.ABOUT_ANEMO;
      case ELECTRO -> EffectRegistry.ABOUT_ELECTRO;
      case DENDRO -> EffectRegistry.ABOUT_DENDRO;
      case CYRO -> EffectRegistry.ABOUT_CYRO;
      case GEO -> EffectRegistry.ABOUT_GEO;
      default -> null;
    };
  }
}
