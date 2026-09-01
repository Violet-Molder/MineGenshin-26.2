package com.linweiyun.genshin;

import com.linweiyun.genshin.content.ItemGroupRegistry;
import com.linweiyun.genshin.content.blocks.BlocksGIM;
import com.linweiyun.genshin.content.effects.entities.EffectRegistry;
import com.linweiyun.genshin.content.effects.itemstacks.registries.ItemStackEffects;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.entity.EntityRegister;
import com.linweiyun.genshin.content.items.ItemsGIM;
import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.data.loot.ModLootTypes;
import com.linweiyun.genshin.data.loot.modifier.LTModifiers;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Minegenshin.MOD_ID)
public class Minegenshin {
  public static final String MOD_ID = "minegenshin";
  public static final Logger LOGGER = LogUtils.getLogger();

  public Minegenshin(IEventBus modEventBus, ModContainer modContainer) {
    NeoForge.EVENT_BUS.register(this);

    modEventBus.addListener(this::addCreative);
    //        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
    ItemStackEffects.register(modEventBus);
    DataComponentRegistryCharacter.register(modEventBus);
    ItemsGIM.register(modEventBus);
    BlocksGIM.register(modEventBus);
    EntityRegister.register(modEventBus);
    AttachmentRegistration.register(modEventBus);
    ItemGroupRegistry.register(modEventBus);
    EffectRegistry.register(modEventBus);

    LTModifiers.register(modEventBus);
    ModLootTypes.register(modEventBus);

    modContainer.registerConfig(ModConfig.Type.COMMON, Config.CHARACTER_EXP_SPEC, "minegenshin/exp.toml");
    modContainer.registerConfig(ModConfig.Type.COMMON, Config.CHARACTER_ATTRIBUTE_SPEC, "minegenshin/attribute.toml");
  }

  private void addCreative(BuildCreativeModeTabContentsEvent event) {
    if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {}
  }

  public static ResourceLocation id(String id) {
    return ResourceLocation.fromNamespaceAndPath(MOD_ID, id);
  }

  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {}
}
