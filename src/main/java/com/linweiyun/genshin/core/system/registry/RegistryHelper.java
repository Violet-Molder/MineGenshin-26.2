package com.linweiyun.genshin.core.system.registry;

import com.linweiyun.genshin.core.system.registry.register.ModItemGroups;
import com.linweiyun.genshin.core.system.registry.register.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;


import java.util.function.Function;
import java.util.function.Supplier;

import static com.linweiyun.genshin.core.system.registry.register.ModBlocks.BLOCKS;
import static com.linweiyun.genshin.core.system.registry.register.ModItems.ITEMS;

public class RegistryHelper {

  public static <T extends Item> Supplier<T> registerItem(
      String name, Function<Item.Properties, ? extends T> func) {
    return ITEMS.registerItem(name, func);
  }

  public static <T extends Item> Supplier<T> registerOrdinaryItem(
      String name, Function<Item.Properties, ? extends T> func) {
    Supplier<T> item = registerItem(name, func);
    ModItemGroups.ordinaryItemList.add(item);
    return item;
  }

  public static <T extends Item> Supplier<T> registerFoodItem(
      String name, Function<Item.Properties, ? extends T> func) {
    Supplier<T> item = ITEMS.registerItem(name, func);
    ModItemGroups.foodList.add(item);
    return item;
  }

  public static <T extends Block> Supplier<T> registerBlock(
      String name, Function<BlockBehaviour.Properties, ? extends T> func) {
    Supplier<T> block = BLOCKS.registerBlock(name, func);
    registerBlockItem(name, block);
    ModItemGroups.blockList.add(block);
    return block;
  }

  public static <T extends Block> Supplier<BlockItem> registerBlockItem(
      String name, Supplier<T> block) {
    return ModItems.ITEMS.registerSimpleBlockItem(name, block);
  }
}