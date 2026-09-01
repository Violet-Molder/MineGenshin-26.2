package com.linweiyun.genshin.data.loot.modifier;

import com.linweiyun.genshin.Minegenshin;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class LTModifiers {
  public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>>
      GLOBAL_LOOT_MODIFIER_SERIALIZERS =
          DeferredRegister.create(
              NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Minegenshin.MOD_ID);

  public static final Supplier<MapCodec<PrimogemDropModifier>> PRIMOGEM_DROP_MODIFIER =
      GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("primogem_drop", () -> PrimogemDropModifier.CODEC);

  public static void register(IEventBus eventBus) {
    GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(eventBus);
  }
}
