//package com.linweiyun.genshin.data.loot;
//
//import com.linweiyun.genshin.Minegenshin;
//import net.minecraft.core.registries.Registries;
//import net.neoforged.bus.api.IEventBus;
//import net.neoforged.neoforge.registries.DeferredHolder;
//import net.neoforged.neoforge.registries.DeferredRegister;
//
//public class ModLootTypes {
//  public static final DeferredRegister<LootPoolEntryType> LOOT_ENTRY_TYPES =
//      DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE, Minegenshin.MOD_ID);
//
//  public static final DeferredHolder<LootPoolEntryType, LootPoolEntryType> CHARACTER_ENTRY =
//      LOOT_ENTRY_TYPES.register("character", () -> new LootPoolEntryType(CharacterLootEntry.CODEC));
//
//  public static void register(IEventBus bus) {
//    LOOT_ENTRY_TYPES.register(bus);
//  }
//}
