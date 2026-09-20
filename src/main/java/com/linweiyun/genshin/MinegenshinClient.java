package com.linweiyun.genshin;

import com.geckolib.renderer.GeoEntityRenderer;
import com.linweiyun.genshin.client.render.character.FirstPersonCharacterRenderer;
import com.linweiyun.genshin.client.render.geo.GenshinGeoCache;
import com.linweiyun.genshin.core.asset.GenshinAssets;
import com.linweiyun.genshin.core.system.combat.animation.CharacterAnimationRegistry;
import com.linweiyun.genshin.content.entities.teyvat.skill.vesna.VesnaAttackProjectileRenderer;
import com.linweiyun.genshin.content.entities.teyvat.skill.vesna.VesnaSpiritSwordRenderer;
import com.linweiyun.genshin.render.render.entity.ElementalOrbRenderer;
import com.linweiyun.genshin.render.render.entity.FieldTalismanSpiritRender;
import com.linweiyun.genshin.render.render.entity.StellarVortexRenderer;
import com.linweiyun.genshin.render.render.entity.ThunderCloudRenderer;
import com.linweiyun.genshin.render.gui.screens.ScreenArtifaceInfo;
import com.linweiyun.genshin.core.attachment.ClientAttachmentSync;
import com.linweiyun.genshin.content.entities.ModEntities;
import com.linweiyun.genshin.core.system.registry.register.ModMenus;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;


@Mod(value = Minegenshin.MOD_ID, dist = Dist.CLIENT)

@EventBusSubscriber(modid = Minegenshin.MOD_ID, value = Dist.CLIENT)
public class MinegenshinClient {
    public MinegenshinClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> new ConfigurationScreen(container, parent));
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ClientAttachmentSync.init(() -> Minecraft.getInstance().player);

        // 资产路径规则：默认把本 MOD 的 character 资源解析到 character/<角色id>/ 下
        GenshinAssets.installDefaults();

        // 角色动作系统：加角色在 CharacterAnimationRegistry 里加一行
        CharacterAnimationRegistry.registerAll();

        // 第一人称接管原版手臂。
        // RenderHandEvent 是游戏总线事件（NeoForge.EVENT_BUS），不是 mod 总线事件，
        // 所以这里显式注册，而不是靠 @EventBusSubscriber 去猜总线。
        NeoForge.EVENT_BUS.addListener(FirstPersonCharacterRenderer::onRenderHand);
    }

    /**
     * 本 MOD 自己的 GeckoLib 资源缓存。
     *
     * <p>GeckoLib 只扫 {@code assets/<ns>/geckolib/{models,animations}}，而我们要把角色资源
     * 集中在 {@code assets/minegenshin/character/<角色id>/}，所以自己扫、自己烘培；
     * 取值走 {@code GenshinGeoModel} 覆盖的两个方法，<b>不动 GeckoLib 任何全局行为</b>。
     */
    @SubscribeEvent
    public static void addReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Minegenshin.id("genshin_geo_cache"), new GenshinGeoCache());
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CHARACTER_INFO_MENU.get(), ScreenArtifaceInfo::new);
    }

    @SubscribeEvent
    public static void registerDebugEntries(RegisterDebugEntriesEvent event) {}

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntities.FIELD_TALISMAN_SPIRIT.get(), FieldTalismanSpiritRender::new);
        event.registerEntityRenderer(
                ModEntities.SLIME_CYRO.get(), context -> new GeoEntityRenderer<>(context, ModEntities.SLIME_CYRO.get())
        );
        event.registerEntityRenderer(
                ModEntities.ELEMENTAL_ORB.get(), ElementalOrbRenderer::new);
        event.registerEntityRenderer(
                ModEntities.THUNDER_CLOUD.get(), ThunderCloudRenderer::new);
        event.registerEntityRenderer(
                ModEntities.STELLAR_VORTEX.get(), StellarVortexRenderer::new);
        event.registerEntityRenderer(
                ModEntities.VESNA_ATTACK_PROJECTILE.get(), VesnaAttackProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.VESNA_SPIRIT_SWORD.get(), VesnaSpiritSwordRenderer::new);
    }



}