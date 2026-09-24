package com.linweiyun.genshin;

import com.geckolib.renderer.GeoEntityRenderer;
import com.linweiyun.genshin.client.render.character.FirstPersonCharacterRenderer;
import com.linweiyun.genshin.client.render.geo.GenshinGeoCache;
import com.linweiyun.genshin.client.render.geo.AssetGeoCache;
import com.linweiyun.genshin.client.render.geo.CategoryGeoModel;
import com.linweiyun.genshin.client.damage.DamageIndicatorClientBridge;
import com.linweiyun.genshin.core.asset.AssetCategory;
import com.linweiyun.genshin.core.asset.GenshinAssets;
import com.linweiyun.genshin.content.entities.teyvat.monster.slime.LargeCryoSlime;
import com.linweiyun.genshin.client.combat.action.CharacterAnimationRegistry;
import com.linweiyun.genshin.content.entities.teyvat.skill.vesna.VesnaAttackProjectileRenderer;
import com.linweiyun.genshin.content.entities.teyvat.skill.vesna.VesnaSpiritSwordRenderer;
import com.linweiyun.genshin.render.entity.ElementalOrbRenderer;
import com.linweiyun.genshin.render.entity.FieldTalismanSpiritRender;
import com.linweiyun.genshin.render.entity.IceBlockProjectileRenderer;
import com.linweiyun.genshin.render.entity.StellarVortexRenderer;
import com.linweiyun.genshin.render.entity.ThunderCloudRenderer;
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

        // 伤害飘字：客户端安装表现实现（公共侧只依赖 api 契约）
        DamageIndicatorClientBridge.install();

        // 资产路径规则：默认把本 MOD 的 character 资源解析到 character/<角色id>/ 下
        GenshinAssets.installDefaults();

        // 角色动作系统：加角色在 CharacterAnimationRegistry 里加一行
        CharacterAnimationRegistry.registerAll();

        // 第一人称接管原版手臂。
        // RenderHandEvent 是游戏总线事件（NeoForge.EVENT_BUS），不是 mod 总线事件，
        // 所以这里显式注册，而不是靠 @EventBusSubscriber 去猜总线。
        NeoForge.EVENT_BUS.addListener(FirstPersonCharacterRenderer::onRenderHand);

        // 统一布局模型解析缓存（AssetGeoCache）的预热。
        // 它同时也作为客户端资源重载监听器注册（见 addReloadListeners），这里只是保证
        // 「第一次查询之前就已经扫好」。客户端初始化阶段资源管理器可能还是空的，
        // 那一轮空扫描不会落锚，会在首次查询或下一次资源重载时补扫（见 AssetGeoCache#apply）。
        event.enqueueWork(AssetGeoCache::warmUp);
    }

    /**
     * 本 MOD 自己的 GeckoLib 资源缓存（两套）。
     *
     * <p>GeckoLib 只扫 {@code assets/<ns>/geckolib/{models,animations}}，而我们要把角色资源
     * 集中在 {@code assets/minegenshin/character/<角色id>/}，所以自己扫、自己烘培；
     * 取值走 {@code GenshinGeoModel} 覆盖的两个方法，<b>不动 GeckoLib 任何全局行为</b>。
     *
     * <p>{@code AssetGeoCache} 是统一布局（{@code item/}、{@code block/}、{@code entity/}）
     * 的索引，同样在这里注册；它的 {@code reload} 已经把异常兜住，不会把整个资源重载带崩。
     */
    @SubscribeEvent
    public static void addReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Minegenshin.id("genshin_geo_cache"), new GenshinGeoCache());
        event.addListener(Minegenshin.id("asset_geo_cache"), new AssetGeoCache());
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
                ModEntities.LARGE_CRYO_SLIME.get(), context -> new GeoEntityRenderer<>(context,
                        new CategoryGeoModel<LargeCryoSlime>(AssetCategory.ENTITY, "large_cryo_slime"))
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
        // 冰块投射物：原版方块模型，不需要 GeckoLib
        event.registerEntityRenderer(ModEntities.ICE_BLOCK.get(), IceBlockProjectileRenderer::new);
    }



}
