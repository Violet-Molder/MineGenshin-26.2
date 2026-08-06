package com.linweiyun.genshin.data.generators;

import com.linweiyun.genshin.Minegenshin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;


@EventBusSubscriber(modid = Minegenshin.MOD_ID)
public class DataGenerators {
  @SubscribeEvent
  public static void gatherData(GatherDataEvent.Client event) {
    event.createProvider(ModModeProvider::new);
    event.createProvider(DamageTypeDataProviderGIM::new);
  }
}
