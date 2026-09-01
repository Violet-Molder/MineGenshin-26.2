package com.linweiyun.genshin.content.entities.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;

public class ElementalDamageSourcesGIM {
  public static ElementalDamageSourceGIM NORMAL_ATTACK_DAMAGE_SOURCE(Entity causer) {
    return new ElementalDamageSourceGIM(
        causer
            .level()
            .registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(ElementalDamageTypeGIM.NORMAL_ATTACK),
        causer);
  }

  public static ElementalDamageSourceGIM PLUNGING_ATTACK_DAMAGE_SOURCE(Entity causer) {
    return new ElementalDamageSourceGIM(
        causer
            .level()
            .registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(ElementalDamageTypeGIM.PLUNGING_ATTACK),
        causer);
  }

  public static ElementalDamageSourceGIM CHARGED_ATTACK_DAMAGE_SOURCE(Entity causer) {
    return new ElementalDamageSourceGIM(
        causer
            .level()
            .registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(ElementalDamageTypeGIM.CHARGED_ATTACK),
        causer);
  }

  public static ElementalDamageSourceGIM ELEMENTAL_SKILL_ATTACK_DAMAGE_SOURCE(Entity causer) {
    return new ElementalDamageSourceGIM(
        causer
            .level()
            .registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(ElementalDamageTypeGIM.ELEMENTAL_SKILL_ATTACK),
        causer);
  }

  public static ElementalDamageSourceGIM ELEMENTAL_BURST_DAMAGE_SOURCE(Entity causer) {
    return new ElementalDamageSourceGIM(
        causer
            .level()
            .registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(ElementalDamageTypeGIM.ELEMENTAL_BURST_ATTACK),
        causer);
  }

  public static ElementalDamageSourceGIM MONSTER_DAMAGE_SOURCE(Entity causer) {
    return new ElementalDamageSourceGIM(
        causer
            .level()
            .registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(ElementalDamageTypeGIM.MONSTER_ATTACK),
        causer);
  }
}
