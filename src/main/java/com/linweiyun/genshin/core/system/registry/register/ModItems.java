package com.linweiyun.genshin.core.system.registry.register;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.items.artifact.crimson_witch.*;
import com.linweiyun.genshin.content.items.custom.ItemPrimogem;
import com.linweiyun.genshin.core.system.registry.RegistryHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
  // 创建一个延迟注册器，用于注册物品
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

  public static void register(IEventBus modEventBus) {
    ITEMS.register(modEventBus);
  }
}
