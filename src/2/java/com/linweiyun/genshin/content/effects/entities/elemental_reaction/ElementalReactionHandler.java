package com.linweiyun.genshin.content.effects.entities.elemental_reaction;

import com.linweiyun.genshin.content.effects.entities.EffectRegistry;
import com.linweiyun.genshin.content.effects.entities.ElementalEffectHelper;
import com.linweiyun.genshin.content.effects.entities.about_elemental_effect.AboutElementalEffect;
import com.linweiyun.genshin.content.effects.entities.instance.MobEffectInstanceAboutElemental;
import com.linweiyun.genshin.enums.AttachmentType;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.linweiyun.genshin.enums.ElementalsGIM;
import java.util.Objects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ElementalReactionHandler {
  ElementalReactionType elementalReactionType;

  public static void executeElementalReaction(
      LivingEntity target,
      ElementalReactionType elementalReactionType,
      ElementalsGIM topElemental) {
    switch (elementalReactionType) {
      case FROZEN -> executeFrozenReaction(target, topElemental);
      default -> executeDefaultReaction(target, topElemental);
    }
  }

  public static void executeElementalReaction(
      LivingEntity target,
      LivingEntity source,
      ElementalReactionType elementalReactionType,
      ElementalsGIM topElemental) {}

  private static void executeFrozenReaction(LivingEntity target, ElementalsGIM topElemental) {
    int durationTick;
    DeferredHolder<MobEffect, AboutElementalEffect> topEffectHolder =
        ElementalEffectHelper.getEffectHolderByElemental(topElemental);
    DeferredHolder<MobEffect, AboutElementalEffect> bottomEffectHolder =
        ElementalEffectHelper.getEffectHolderByReactionElemental(
            ElementalReactionType.FROZEN, topElemental);
    if (topEffectHolder != null && bottomEffectHolder != null) {
      MobEffectInstanceAboutElemental topElementalInstance =
          (MobEffectInstanceAboutElemental) target.getEffect(topEffectHolder);
      MobEffectInstanceAboutElemental bottomElementalInstance =
          (MobEffectInstanceAboutElemental) target.getEffect(bottomEffectHolder);
      if (Objects.requireNonNull(topElementalInstance).getAttachmentType() == AttachmentType.WEAK
          || Objects.requireNonNull(bottomElementalInstance).getAttachmentType()
              == AttachmentType.WEAK) {
        durationTick = 60;
      } else {
        durationTick = 120;
      }
      target.addEffect(new MobEffectInstance(EffectRegistry.FROZEN, durationTick, 0, false, true));
      float newBottomElementalAmount =
          Math.max(
              0,
              Objects.requireNonNull(bottomElementalInstance).getAttachmentAmount()
                  - topElementalInstance.getAttachmentAmount());
      bottomElementalInstance.setAttachmentAmount(newBottomElementalAmount);
      topElementalInstance.setAttachmentAmount(0);
    }
  }

  private static void executeDefaultReaction(LivingEntity target, ElementalsGIM topElemental) {}
}
