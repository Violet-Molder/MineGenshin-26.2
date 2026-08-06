package com.linweiyun.genshin.registry.register;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.items.custom.ItemPrimogem;
import com.linweiyun.genshin.registry.RegistryHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ItemsRegister {
  // 创建一个延迟注册器，用于注册物品
  public static final DeferredRegister.Items ITEMS =
      DeferredRegister.createItems(Minegenshin.MOD_ID);

  public static final Supplier<ItemPrimogem> PRIMOGEM =
      RegistryHelper.registerOrdinaryItem("primogem", ItemPrimogem::new);

  public static void register(IEventBus modEventBus) {
    ITEMS.register(modEventBus);
  }
}
