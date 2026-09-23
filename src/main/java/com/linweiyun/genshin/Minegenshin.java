package com.linweiyun.genshin;

import com.linweiyun.genshin.config.GenshinConfig;
import com.linweiyun.genshin.content.attribute.AttributeCapHandler;
import com.linweiyun.genshin.content.entities.ModEntities;
import com.linweiyun.genshin.content.entities.test.TestEntities;
import com.linweiyun.genshin.content.items.ModItems;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.Backpack;
import com.linweiyun.genshin.core.character.attachment.ModCharacterAttachmentTypes;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.system.about.block.BlockElementHelper;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.system.combat.action.ServerTickScheduler;
import com.linweiyun.genshin.core.system.combat.decay.DecayCounterService;
import com.linweiyun.genshin.core.system.registry.register.*;
import com.linweiyun.genshin.data.loot.modifier.LTModifiers;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Minegenshin.MOD_ID)
public class Minegenshin {
    public static final String MOD_ID = "minegenshin";
    public static final Logger LOGGER = LogUtils.getLogger();
    public Minegenshin(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, GenshinConfig.CHARACTER_SPEC, "minegenshin/character.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, GenshinConfig.WORLD_TEXT_COLOR_SPEC, "minegenshin/world-text-color.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, GenshinConfig.ENTITY_SPEC, "minegenshin/entity.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, GenshinConfig.WEAPON_SPEC, "minegenshin/weapon.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, GenshinConfig.ARTIFACT_SPEC, "minegenshin/artifact.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, GenshinConfig.REACTION_SPEC, "minegenshin/reaction.toml");


        ModElements.register(modEventBus);
        ModItems.register(modEventBus);
        ModItemGroups.register(modEventBus);
        ModEntities.register(modEventBus);
        //TEMP 测试实体（entity/test*/ 资源 + content/entities/test/）的注册入口
        TestEntities.register(modEventBus);
        ModDamageTypes.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModStatusDataComponents.register(modEventBus);
        ModElementalReactions.register(modEventBus);
        ArtifactSets.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModCharacterAttachmentTypes.register(modEventBus);

        ModAttributes.ATTRIBUTES.register(modEventBus);
        ModCharacters.CHARACTERS.register(modEventBus);
        AttachmentRegistration.register(modEventBus);
        ModCharacterEffects.register(modEventBus);
        ModMenus.register(modEventBus);

        ModStatusInstanceTypes.register(modEventBus);

        LTModifiers.register(modEventBus);

        PlayerUIMenuType.register(
                Identifier.fromNamespaceAndPath("minegenshin", "backpack"),
                player -> {
                    Backpack backpack = player.getData(AttachmentRegistration.BACKPACK_ATTACHMENT.get());
                    return backpack::createUI;
                }
        );



    }
    private void commonSetup(FMLCommonSetupEvent event) {
        ModElements.setupSubElements();
        NetworkManager.init();
        // 属性上限解放（AttributeFix 等价物）：要等所有属性都注册完，
        // 所以放 enqueueWork。放这里而不是 FMLLoadCompleteEvent —— 数据生成（runData）
        // 不会触发后者，那样这条路径就只在正式游戏里才被走到、没法验证。
        event.enqueueWork(AttributeCapHandler::applyCapRelief);
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
    public void onServerTick(ServerTickEvent.Post event) {
        ServerTickScheduler.tick();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            BlockElementHelper.onServerTick(level);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        DecayCounterService.shutdown();
        LOGGER.info("DecayCounter Worker stopped");
    }
}