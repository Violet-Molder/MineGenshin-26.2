package com.linweiyun.genshin.data.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

public class CharacterLootEntry extends LootPoolSingletonContainer {
  public static final MapCodec<CharacterLootEntry> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      BuiltInRegistries.ITEM
                          .holderByNameCodec()
                          .fieldOf("character")
                          .forGetter(entry -> entry.item))
                  .and(singletonFields(instance))
                  .apply(instance, CharacterLootEntry::new));

  private final Holder<Item> item;

  private CharacterLootEntry(
      Holder<Item> item,
      int weight,
      int quality,
      List<LootItemCondition> conditions,
      List<LootItemFunction> functions) {
    super(weight, quality, conditions, functions);
    this.item = item;
  }

  @Override
  public @NotNull LootPoolEntryType getType() {
    return ModLootTypes.CHARACTER_ENTRY.get();
  }

  @Override
  public void createItemStack(Consumer<ItemStack> stackConsumer, @NotNull LootContext lootContext) {
    stackConsumer.accept(new ItemStack(this.item));
  }

  public static LootPoolSingletonContainer.Builder<?> characterEntry(ItemLike item) {
    return simpleBuilder(
        (weight, quality, conditions, functions) ->
            new CharacterLootEntry(
                item.asItem().builtInRegistryHolder(), weight, quality, conditions, functions));
  }
}
