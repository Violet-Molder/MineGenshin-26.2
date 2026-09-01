package com.linweiyun.genshin.content.effects.itemstacks.registries;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.effects.itemstacks.IItemStackEffect;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@EventBusSubscriber(modid = Minegenshin.MOD_ID)
public class ItemStackEffectRegistries {

  public static final ResourceKey<Registry<IItemStackEffect>> ITEM_STACK_EFFECT_REGISTRY_KEY =
      ResourceKey.createRegistryKey(Minegenshin.id("item_stack_effects"));

  public static final Registry<IItemStackEffect> ITEM_STACK_EFFECT_REGISTRY =
      new RegistryBuilder<>(ITEM_STACK_EFFECT_REGISTRY_KEY)
          .sync(true)
          .defaultKey(Minegenshin.id("empty"))
          .maxId(256)
          .create();

  @SubscribeEvent
  public static void registerRegistries(NewRegistryEvent event) {
    event.register(ITEM_STACK_EFFECT_REGISTRY);
  }
}
