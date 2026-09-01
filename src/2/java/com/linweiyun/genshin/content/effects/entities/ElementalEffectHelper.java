package com.linweiyun.genshin.content.effects.entities;

import com.linweiyun.genshin.content.effects.entities.about_elemental_effect.AboutElementalEffect;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ElementalEffectHelper {
  public static DeferredHolder<MobEffect, AboutElementalEffect> getEffectHolderByElemental(
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

  public static DeferredHolder<MobEffect, AboutElementalEffect> getEffectHolderByReactionElemental(
      ElementalReactionType reactionType, ElementalsGIM elemental) {
    switch (reactionType) {
      case FROZEN:
        if (elemental == ElementalsGIM.CYRO) return EffectRegistry.ABOUT_HYDRO;
        if (elemental == ElementalsGIM.HYDRO) return EffectRegistry.ABOUT_CYRO;
        break;
      default:
        return null;
    }
    return null;
  }
}
