package com.linweiyun.genshin.core.system.registry.register;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.items.DarkFragment;
import com.linweiyun.genshin.content.items.artifact.crimson_witch.*;
import com.linweiyun.genshin.content.items.food.CharacterFoods;
import com.linweiyun.genshin.content.items.food.FoodItem;
import com.linweiyun.genshin.content.items.preicous.ItemPrimogem;
import com.linweiyun.genshin.content.items.weapon.catalyst.EverlastingMoonglow;
import com.linweiyun.genshin.core.system.registry.RegistryHelper;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
  public static final DeferredRegister.Items ITEMS =
          DeferredRegister.createItems(Minegenshin.MOD_ID);

  public static final Supplier<ItemPrimogem> PRIMOGEM =
          RegistryHelper.registerOrdinaryItem("primogem", ItemPrimogem::new);

  public static final Supplier<CrimsonFlower> CRIMSON_FLOWER =
          RegistryHelper.registerOrdinaryItem("crimson_flower", CrimsonFlower::new);
  public static final Supplier<CrimsonPlume> CRIMSON_PLUME =
          RegistryHelper.registerOrdinaryItem("crimson_plume", CrimsonPlume::new);
  public static final Supplier<CrimsonSands> CRIMSON_SANDS =
          RegistryHelper.registerOrdinaryItem("crimson_sands", CrimsonSands::new);
  public static final Supplier<CrimsonGoblet> CRIMSON_GOBLET =
          RegistryHelper.registerOrdinaryItem("crimson_goblet", CrimsonGoblet::new);
  public static final Supplier<CrimsonCirclet> CRIMSON_CIRCLET =
          RegistryHelper.registerOrdinaryItem("crimson_circlet", CrimsonCirclet::new);

  public static final Supplier<DarkFragment> DARK_FRAGMENT =
          RegistryHelper.registerOrdinaryItem("dark_fragment", DarkFragment::new);

  public static final Supplier<EverlastingMoonglow> EVERLASTING_MOONGLOW =
          RegistryHelper.registerOrdinaryItem("everlasting_moonglow", EverlastingMoonglow::new);

  public static final Supplier<FoodItem> SWEET_MADAME =
          RegistryHelper.registerFoodItem("sweet_madame", props -> new FoodItem(
                  CharacterFoods.SWEET_MADAME, props));

  public static void register(IEventBus modEventBus) {
    ITEMS.register(modEventBus);
  }
}