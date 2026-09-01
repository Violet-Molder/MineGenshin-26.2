package com.linweiyun.genshin.content.entities.damagesource;

import com.linweiyun.genshin.Minegenshin;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public class ElementalDamageTypeGIM {
  public static final ResourceKey<DamageType> NORMAL_ATTACK =
      ResourceKey.create(
          Registries.DAMAGE_TYPE,
          ResourceLocation.fromNamespaceAndPath(Minegenshin.MOD_ID, "normal_attack"));
  public static final ResourceKey<DamageType> CHARGED_ATTACK =
      ResourceKey.create(
          Registries.DAMAGE_TYPE,
          ResourceLocation.fromNamespaceAndPath(Minegenshin.MOD_ID, "charged_attack"));
  public static final ResourceKey<DamageType> PLUNGING_ATTACK =
      ResourceKey.create(
          Registries.DAMAGE_TYPE,
          ResourceLocation.fromNamespaceAndPath(Minegenshin.MOD_ID, "plunging_attack"));

  public static final ResourceKey<DamageType> ELEMENTAL_SKILL_ATTACK =
      ResourceKey.create(
          Registries.DAMAGE_TYPE,
          ResourceLocation.fromNamespaceAndPath(Minegenshin.MOD_ID, "elemental_skill_attack"));
  public static final ResourceKey<DamageType> ELEMENTAL_BURST_ATTACK =
      ResourceKey.create(
          Registries.DAMAGE_TYPE,
          ResourceLocation.fromNamespaceAndPath(Minegenshin.MOD_ID, "elemental_burst_attack"));
  public static final ResourceKey<DamageType> MONSTER_ATTACK =
      ResourceKey.create(
          Registries.DAMAGE_TYPE,
          ResourceLocation.fromNamespaceAndPath(Minegenshin.MOD_ID, "monster_attack"));
}
