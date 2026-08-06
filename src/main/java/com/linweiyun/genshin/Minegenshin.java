package com.linweiyun.genshin;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.registry.register.CharacterRegister;
import com.linweiyun.genshin.registry.register.ItemGroupRegister;
import com.linweiyun.genshin.registry.register.ItemsRegister;
import com.linweiyun.genshin.registry.register.SkillExecutorRegister;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Minegenshin.MOD_ID)
public class Minegenshin {
    public static final String MOD_ID = "minegenshin";
    public static final Logger LOGGER = LogUtils.getLogger();
    public Minegenshin(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);

        ItemsRegister.register(modEventBus);
        ItemGroupRegister.register(modEventBus);

        ModAttributes.ATTRIBUTES.register(modEventBus);
        CharacterRegister.CHARACTERS.register(modEventBus);
        AttachmentRegistration.register(modEventBus);
        SkillExecutorRegister.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.CHARACTER_EXP_SPEC, "minegenshin/exp.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.CHARACTER_ATTRIBUTE_SPEC, "minegenshin/attribute.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}
