package com.linweiyun.genshin;

import com.linweiyun.genshin.config.ArtifactConfig;
import com.linweiyun.genshin.config.Config;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.system.combat.decay.DecayCounterService;
import com.linweiyun.genshin.core.system.registry.register.*;
import com.linweiyun.genshin.config.ElementalReactionConfig;
import com.linweiyun.genshin.config.MonsterLevelConfig;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
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
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.CHARACTER_EXP_SPEC, "minegenshin/exp.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.CHARACTER_ATTRIBUTE_SPEC, "minegenshin/attribute.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ElementalReactionConfig.REACTION_SPEC, "minegenshin/genshin_reaction.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, MonsterLevelConfig.MONSTER_LEVEL_SPEC, "minegenshin/monster_level.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ArtifactConfig.ARTIFACT_SPEC, "minegenshin/artifact.toml");

        ModItems.register(modEventBus);
        ModItemGroups.register(modEventBus);
        ModEntities.register(modEventBus);
        ModDamageTypes.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModStatusDataComponents.register(modEventBus);
        ModElementalReactions.register(modEventBus);
        ArtifactSets.register(modEventBus);
        ModDataComponents.register(modEventBus);

        ModAttributes.ATTRIBUTES.register(modEventBus);
        ModCharacters.CHARACTERS.register(modEventBus);
        AttachmentRegistration.register(modEventBus);
        ModCharacterEffects.register(modEventBus);
        ModMenus.register(modEventBus);


    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        DecayCounterService.initOnServer(event.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD));
        LOGGER.info("DecayCounter Worker started");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        DecayCounterService.shutdown();
        LOGGER.info("DecayCounter Worker stopped");
    }
}