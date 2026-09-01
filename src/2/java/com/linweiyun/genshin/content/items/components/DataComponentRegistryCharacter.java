package com.linweiyun.genshin.content.items.components;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.effects.itemstacks.ItemStackEffectList;
import com.linweiyun.genshin.core.food.CharacterFoodProperties;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DataComponentRegistryCharacter {
  public static final DeferredRegister.DataComponents REGISTRAR =
      DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Minegenshin.MOD_ID);

  public static final DeferredHolder<
          DataComponentType<?>, DataComponentType<DataComponentCharacter>>
      CHARACTER_DATA =
          REGISTRAR.registerComponentType(
              "character_data",
              builder ->
                  builder
                      .persistent(DataComponentCharacter.CODEC)
                      .networkSynchronized(DataComponentCharacter.STREAM_CODEC));
  public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemStackEffectList>>
      ITEM_STACK_EFFECTS =
          REGISTRAR.registerComponentType(
              "item_stack_effects",
              builder ->
                  builder
                      .persistent(ItemStackEffectList.CODEC)
                      .networkSynchronized(ItemStackEffectList.STREAM_CODEC)
                      .cacheEncoding());
  public static final DeferredHolder<
          DataComponentType<?>, DataComponentType<CharacterFoodProperties>>
      CHARACTER_FOOD =
          REGISTRAR.registerComponentType(
              "character_food",
              builder ->
                  builder
                      .persistent(CharacterFoodProperties.CODEC)
                      .networkSynchronized(CharacterFoodProperties.STREAM_CODEC)
                      .cacheEncoding());

  public static void register(IEventBus modEventBus) {
    REGISTRAR.register(modEventBus);
  }
}
