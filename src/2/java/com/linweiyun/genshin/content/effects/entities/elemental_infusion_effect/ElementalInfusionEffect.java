package com.linweiyun.genshin.content.effects.entities.elemental_infusion_effect;

import com.linweiyun.genshin.content.effects.entities.ElementalEffect;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

public class ElementalInfusionEffect extends ElementalEffect {
  final ElementalsGIM elemental;

  public ElementalInfusionEffect(MobEffectCategory category, int color, ElementalsGIM elemental) {
    super(category, color, elemental);
    this.elemental = elemental;
  }

  @Override
  public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
    return true;
  }

  @Override
  public boolean applyEffectTick(@NotNull LivingEntity livingEntity, int amplifier) {

    return super.applyEffectTick(livingEntity, amplifier);
  }

  @Override
  public void onEffectAdded(@NotNull LivingEntity livingEntity, int amplifier) {

    DeferredHolder<MobEffect, ElementalInfusionEffect> effect =
        CombatHelper.whichElementalAttackEffect(livingEntity, this.elemental);
    if (effect != null && effect.value() != this) {
      livingEntity.removeEffect(effect);
    }

    super.onEffectAdded(livingEntity, amplifier);
  }
}
