package com.linweiyun.genshin.core.registry;

import static com.linweiyun.genshin.content.blocks.BlocksGIM.BLOCKS;
import static com.linweiyun.genshin.content.items.ItemsGIM.ITEMS;

import com.linweiyun.genshin.content.ItemGroupRegistry;
import com.linweiyun.genshin.content.items.ItemsGIM;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class RegistryHelperGIM {

  public static <T extends Item> Supplier<T> registerItem(
      String name, Function<Item.Properties, ? extends T> func) {
    return ITEMS.registerItem(name, func);
  }

  public static <T extends Item> Supplier<T> registerOrdinaryItem(
      String name, Function<Item.Properties, ? extends T> func) {
    Supplier<T> item = registerItem(name, func);
    ItemGroupRegistry.ordinaryItemList.add(item);
    return item;
  }

  public static <T extends Item> Supplier<T> registerFoodItem(
      String name, Supplier<? extends T> sup) {
    Supplier<T> item = ITEMS.register(name, sup);
    ItemGroupRegistry.foodList.add(item);
    return item;
  }

  public static <T extends Block> Supplier<T> registerBlock(
      String name, Function<BlockBehaviour.Properties, ? extends T> func) {
    Supplier<T> block = BLOCKS.registerBlock(name, func);
    registerBlockItem(name, block);
    ItemGroupRegistry.blockList.add(block);
    return block;
  }

  public static <T extends Block> Supplier<BlockItem> registerBlockItem(
      String name, Supplier<T> block) {
    return ItemsGIM.ITEMS.registerSimpleBlockItem(name, block);
  }
}
