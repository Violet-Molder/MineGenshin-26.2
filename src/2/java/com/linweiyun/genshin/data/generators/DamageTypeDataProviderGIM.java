package com.linweiyun.genshin.data.generators;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.entities.damagesource.ElementalDamageTypeGIM;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DeathMessageType;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

public class DamageTypeDataProviderGIM extends DatapackBuiltinEntriesProvider {
  public DamageTypeDataProviderGIM(
      PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
    super(
        output,
        registries,
        new RegistrySetBuilder()
            .add(
                Registries.DAMAGE_TYPE,
                bootstrap -> {
                  bootstrap.register(
                      ElementalDamageTypeGIM.NORMAL_ATTACK,
                      new DamageType(
                          ElementalDamageTypeGIM.NORMAL_ATTACK.location().getPath(),
                          DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                          0.1f,
                          DamageEffects.HURT,
                          DeathMessageType.DEFAULT));
                  bootstrap.register(
                      ElementalDamageTypeGIM.CHARGED_ATTACK,
                      new DamageType(
                          ElementalDamageTypeGIM.CHARGED_ATTACK.location().getPath(),
                          DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                          0.1f,
                          DamageEffects.HURT,
                          DeathMessageType.DEFAULT));
                  bootstrap.register(
                      ElementalDamageTypeGIM.PLUNGING_ATTACK,
                      new DamageType(
                          ElementalDamageTypeGIM.PLUNGING_ATTACK.location().getPath(),
                          DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                          0.1f,
                          DamageEffects.HURT,
                          DeathMessageType.DEFAULT));
                  bootstrap.register(
                      ElementalDamageTypeGIM.ELEMENTAL_SKILL_ATTACK,
                      new DamageType(
                          ElementalDamageTypeGIM.ELEMENTAL_SKILL_ATTACK.location().getPath(),
                          DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                          0.1f,
                          DamageEffects.HURT,
                          DeathMessageType.DEFAULT));
                  bootstrap.register(
                      ElementalDamageTypeGIM.ELEMENTAL_BURST_ATTACK,
                      new DamageType(
                          ElementalDamageTypeGIM.ELEMENTAL_BURST_ATTACK.location().getPath(),
                          DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                          0.1f,
                          DamageEffects.HURT,
                          DeathMessageType.DEFAULT));
                  bootstrap.register(
                      ElementalDamageTypeGIM.MONSTER_ATTACK,
                      new DamageType(
                          ElementalDamageTypeGIM.MONSTER_ATTACK.location().getPath(),
                          DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                          0.1f,
                          DamageEffects.HURT,
                          DeathMessageType.DEFAULT));
                }),
        Set.of(Minegenshin.MOD_ID));
  }
}
