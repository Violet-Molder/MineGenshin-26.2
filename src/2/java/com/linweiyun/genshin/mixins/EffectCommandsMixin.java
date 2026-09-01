package com.linweiyun.genshin.mixins;

import com.linweiyun.genshin.content.effects.entities.ElementalEffectHelper;
import com.linweiyun.genshin.content.effects.entities.about_elemental_effect.AboutElementalEffect;
import com.linweiyun.genshin.content.effects.entities.instance.MobEffectInstanceAboutElemental;
import com.linweiyun.genshin.enums.AttachmentType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.EffectCommands;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EffectCommands.class)
public class EffectCommandsMixin {

  @Inject(method = "giveEffect", at = @At("HEAD"), cancellable = true)
  private static void giveEffect(
      CommandSourceStack source,
      Collection<? extends Entity> targets,
      Holder<MobEffect> effect,
      Integer seconds,
      int amplifier,
      boolean showParticles,
      CallbackInfoReturnable<Integer> cir) {
    MobEffect mobEffect = effect.value();
    if (mobEffect instanceof AboutElementalEffect aboutElementalEffect) {
      cir.cancel();
      for (Entity entity : targets) {
        if (entity instanceof LivingEntity livingEntity) {
          DeferredHolder<MobEffect, AboutElementalEffect> effectHolder =
              ElementalEffectHelper.getEffectHolderByElemental(aboutElementalEffect.getElemental());
          MobEffectInstanceAboutElemental instance = null;
          if (effectHolder != null) {
            instance = new MobEffectInstanceAboutElemental(effectHolder, AttachmentType.WEAK);
          }
          if (instance != null) {
            livingEntity.addEffect(instance);
            livingEntity.sendSystemMessage(Component.literal(instance.toString()));
          }
        }
      }
    }
  }
}
