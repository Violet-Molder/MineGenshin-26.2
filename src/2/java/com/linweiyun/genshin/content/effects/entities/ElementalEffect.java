package com.linweiyun.genshin.content.effects.entities;

import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ElementalEffect extends MobEffect {
  private final ElementalsGIM elemental;

  protected ElementalEffect(MobEffectCategory category, int color, ElementalsGIM elemental) {
    super(category, color);
    this.elemental = elemental;
  }

  public ElementalsGIM getElemental() {
    return elemental;
  }
}
