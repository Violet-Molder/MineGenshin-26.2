package com.linweiyun.genshin.registry.register;

import com.linweiyun.genshin.Minegenshin;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ItemGroupRegister {
  public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Minegenshin.MOD_ID);

  public static final List<Supplier<? extends Item>> ordinaryItemList = new ArrayList<>();
  public static final List<Supplier<? extends Item>> foodList = new ArrayList<>();
  public static final List<Supplier<? extends Block>> blockList = new ArrayList<>();

  public static final Supplier<CreativeModeTab> ITEM_GROUP =
      CREATIVE_MODE_TABS.register(
          "item_group",
          () ->
              CreativeModeTab.builder()
                  .title(
                      Component.translatable(
                          "itemGroup." + Minegenshin.MOD_ID + ".regularItemGroup"))
                  .icon(() -> new ItemStack(ItemsRegister.PRIMOGEM.get()))
                  .displayItems(
                      (params, output) ->
                          ordinaryItemList.forEach(item -> output.accept(item.get())))
                  .build());

  //  public static final Supplier<CreativeModeTab> BLOCK_GROUP =
  //      CREATIVE_MODE_TABS.register(
  //          "block_group",
  //          () ->
  //              CreativeModeTab.builder()
  //                  .title(Component.translatable("itemGroup." + Minegenshin.MOD_ID +
  // ".block_group"))
  //                  .icon(() -> new ItemStack(ItemsGIM.PRIMOGEM.get()))
  //                  .displayItems(
  //                      (params, output) -> blockList.forEach(item -> output.accept(item.get())))
  //                  .build());
  public static final Supplier<CreativeModeTab> FOOD_GROUP =
      CREATIVE_MODE_TABS.register(
          "food_group",
          () ->
              CreativeModeTab.builder()
                  .title(Component.translatable("itemGroup." + Minegenshin.MOD_ID + ".foodGroup"))
                  .icon(() -> new ItemStack(ItemsRegister.PRIMOGEM.get()))
                  .displayItems(
                      (params, output) -> foodList.forEach(item -> output.accept(item.get())))
                  .build());

  public static void register(IEventBus eventBus) {
    CREATIVE_MODE_TABS.register(eventBus);
  }
}
