package com.linweiyun.genshin.content.effects.entities;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.effects.entities.about_elemental_effect.AboutElementalEffect;
import com.linweiyun.genshin.content.effects.entities.elemental_infusion_effect.ElementalInfusionEffect;
import com.linweiyun.genshin.content.effects.entities.elemental_reaction.FrozenEffect;
import com.linweiyun.genshin.enums.ElementalsGIM;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EffectRegistry {
  public static final DeferredRegister<MobEffect> MOB_EFFECTS =
      DeferredRegister.create(Registries.MOB_EFFECT, Minegenshin.MOD_ID);

  // 存储所有元素附着效果的列表
  public static final List<DeferredHolder<MobEffect, ElementalInfusionEffect>>
      ELEMENT_INFUSION_EFFECTS = new ArrayList<>();
  public static final List<DeferredHolder<MobEffect, AboutElementalEffect>>
      ABOUT_ELEMENT_INFUSION_EFFECTS = new ArrayList<>();

  // 元素附魔效果
  public static final DeferredHolder<MobEffect, ElementalInfusionEffect> PYRO_INFUSION =
      registerElementAttachEffect(
          "pyro_infusion",
          () ->
              new ElementalInfusionEffect(
                  MobEffectCategory.BENEFICIAL, 0xFF4500, ElementalsGIM.PYRO),
          0);
  public static final DeferredHolder<MobEffect, ElementalInfusionEffect> HYDRO_INFUSION =
      registerElementAttachEffect(
          "hydro_infusion",
          () ->
              new ElementalInfusionEffect(
                  MobEffectCategory.BENEFICIAL, 0x1E90FF, ElementalsGIM.HYDRO),
          0);
  public static final DeferredHolder<MobEffect, ElementalInfusionEffect> ANEMO_INFUSION =
      registerElementAttachEffect(
          "anemo_infusion",
          () ->
              new ElementalInfusionEffect(
                  MobEffectCategory.BENEFICIAL, 0x20B2AA, ElementalsGIM.ANEMO),
          0);
  public static final DeferredHolder<MobEffect, ElementalInfusionEffect> ELECTRO_INFUSION =
      registerElementAttachEffect(
          "electro_infusion",
          () ->
              new ElementalInfusionEffect(
                  MobEffectCategory.BENEFICIAL, 0x9370DB, ElementalsGIM.ELECTRO),
          0);
  public static final DeferredHolder<MobEffect, ElementalInfusionEffect> DENDRO_INFUSION =
      registerElementAttachEffect(
          "dendro_infusion",
          () ->
              new ElementalInfusionEffect(
                  MobEffectCategory.BENEFICIAL, 0x32CD32, ElementalsGIM.DENDRO),
          0);
  public static final DeferredHolder<MobEffect, ElementalInfusionEffect> CYRO_INFUSION =
      registerElementAttachEffect(
          "cyro_infusion",
          () ->
              new ElementalInfusionEffect(
                  MobEffectCategory.BENEFICIAL, 0x87CEFA, ElementalsGIM.CYRO),
          0);
  public static final DeferredHolder<MobEffect, ElementalInfusionEffect> GEO_INFUSION =
      registerElementAttachEffect(
          "geo_infusion",
          () ->
              new ElementalInfusionEffect(
                  MobEffectCategory.BENEFICIAL, 0xDAA520, ElementalsGIM.GEO),
          0);

  // 元素附着效果
  public static final DeferredHolder<MobEffect, AboutElementalEffect> ABOUT_PYRO =
      registerElementAttachEffect(
          "pyro_about",
          () -> new AboutElementalEffect(MobEffectCategory.NEUTRAL, 0xFF0000, ElementalsGIM.PYRO),
          1);

  public static final DeferredHolder<MobEffect, AboutElementalEffect> ABOUT_HYDRO =
      registerElementAttachEffect(
          "hydro_about",
          () -> new AboutElementalEffect(MobEffectCategory.NEUTRAL, 0x0000FF, ElementalsGIM.HYDRO),
          1);

  public static final DeferredHolder<MobEffect, AboutElementalEffect> ABOUT_ANEMO =
      registerElementAttachEffect(
          "anemo_about",
          () -> new AboutElementalEffect(MobEffectCategory.NEUTRAL, 0x008080, ElementalsGIM.ANEMO),
          1);

  public static final DeferredHolder<MobEffect, AboutElementalEffect> ABOUT_ELECTRO =
      registerElementAttachEffect(
          "electro_about",
          () ->
              new AboutElementalEffect(MobEffectCategory.NEUTRAL, 0x4B0082, ElementalsGIM.ELECTRO),
          1);

  public static final DeferredHolder<MobEffect, AboutElementalEffect> ABOUT_DENDRO =
      registerElementAttachEffect(
          "dendro_about",
          () -> new AboutElementalEffect(MobEffectCategory.NEUTRAL, 0x006400, ElementalsGIM.DENDRO),
          1);

  public static final DeferredHolder<MobEffect, AboutElementalEffect> ABOUT_CYRO =
      registerElementAttachEffect(
          "cyro_about",
          () -> new AboutElementalEffect(MobEffectCategory.NEUTRAL, 0x0000FF, ElementalsGIM.CYRO),
          1);

  public static final DeferredHolder<MobEffect, AboutElementalEffect> ABOUT_GEO =
      registerElementAttachEffect(
          "geo_about",
          () -> new AboutElementalEffect(MobEffectCategory.NEUTRAL, 0x8B4513, ElementalsGIM.GEO),
          1);

  // 元素反应效果
  public static final DeferredHolder<MobEffect, FrozenEffect> FROZEN =
      registerElementAttachEffect(
          "frozen",
          () ->
              (FrozenEffect)
                  new FrozenEffect(MobEffectCategory.HARMFUL, 0x00FFFF)
                      .addAttributeModifier(
                          Attributes.MOVEMENT_SPEED,
                          ResourceLocation.fromNamespaceAndPath(
                              Minegenshin.MOD_ID, "effect.frozen.move_speed"),
                          -1,
                          AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                      .addAttributeModifier(
                          Attributes.JUMP_STRENGTH,
                          ResourceLocation.fromNamespaceAndPath(
                              Minegenshin.MOD_ID, "effect.frozen.jump_strength"),
                          -1F,
                          AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                      .addAttributeModifier(
                          Attributes.WATER_MOVEMENT_EFFICIENCY,
                          ResourceLocation.fromNamespaceAndPath(
                              Minegenshin.MOD_ID, "effect.frozen.swimmng_speed"),
                          10,
                          AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                      .addAttributeModifier(
                          Attributes.FLYING_SPEED,
                          ResourceLocation.fromNamespaceAndPath(
                              Minegenshin.MOD_ID, "effect.frozen.flying_speed"),
                          -1,
                          AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
          2);

  public static <T extends MobEffect> DeferredHolder<MobEffect, T> registerElementAttachEffect(
      String name, Supplier<T> effectSupplier, int flag) {
    DeferredHolder<MobEffect, T> effect = MOB_EFFECTS.register(name, effectSupplier);

    switch (flag) {
      case 0:
        ELEMENT_INFUSION_EFFECTS.add((DeferredHolder<MobEffect, ElementalInfusionEffect>) effect);

        break;
      case 1:
        ABOUT_ELEMENT_INFUSION_EFFECTS.add(
            (DeferredHolder<MobEffect, AboutElementalEffect>) effect);
        break;
      case 2:
      default:
        break;
    }
    return effect;
  }

  public static void register(IEventBus bus) {
    MOB_EFFECTS.register(bus);
  }
}
