package com.linweiyun.genshin.content.entities.test;

import com.geckolib.renderer.GeoEntityRenderer;
import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.client.render.geo.AssetGeoCache;
import com.linweiyun.genshin.client.render.geo.CategoryGeoModel;
import com.linweiyun.genshin.core.asset.AssetCategory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 测试实体的客户端接入 —— 渲染器 + 资源索引预热。
 *
 * <p>单独开一个 {@link EventBusSubscriber}，而不是往 {@code MinegenshinClient} 里加代码：
 * 同一个事件允许多个订阅者，测试实体这块可以整包删掉。
 *
 * <p>模型用 {@link CategoryGeoModel}（「类别 + id」驱动），
 * 所以这里的路径字符串只有 {@link Test1Entity#ASSET_ID} 一个，
 * 新增测试实体就是复制这一行、换个 id。
 */
@EventBusSubscriber(modid = Minegenshin.MOD_ID, value = Dist.CLIENT)
public final class TestEntityRenderers {

    //TEMP
    private TestEntityRenderers() {
    }

    //TEMP
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TestEntities.TEST1.get(), context ->
                new GeoEntityRenderer<>(context,
                        new CategoryGeoModel<Test1Entity>(AssetCategory.ENTITY, Test1Entity.ASSET_ID)));
        // test2 自己不写贴图/模型时先借用 test1 的那套，资源到位后自动切换
        event.registerEntityRenderer(TestEntities.TEST2.get(), context ->
                new GeoEntityRenderer<>(context,
                        new CategoryGeoModel<Test2Entity>(AssetCategory.ENTITY, Test2Entity.ASSET_ID)
                                .withFallback(AssetCategory.ENTITY, Test1Entity.ASSET_ID)));
        // 冰块投射物：原版方块模型，不需要 GeckoLib
        event.registerEntityRenderer(TestEntities.ICE_BLOCK.get(), IceBlockProjectileRenderer::new);
    }

    /**
     * 统一布局的 {@code <id>.json} 模型解析缓存（{@link AssetGeoCache}）。
     *
     * <p><b>刻意不注册成全局资源重载监听器。</b>
     *
     * <p>把它挂到 {@code AddClientReloadListenersEvent} 上曾经导致过一次事故：
     * 那个监听器一旦抛异常，<b>整个客户端资源重载都会失败</b>，
     * 于是所有资源驱动的东西（角色动作、动画、贴图）一起失效 ——
     * 表现是「某些角色左键没反应、动画也不播」，查起来完全指不到这里。
     *
     * <p>{@link AssetGeoCache} 自带「首次查询时同步扫一次」的兜底
     * （见 {@code AssetGeoCache#ensureLoaded}），所以不注册也照样能用，
     * 而且它的影响范围被限制在「读统一布局资源」这一件事上。
     */
    //TEMP
    private static void registerAssetCache() {
        AssetGeoCache.warmUp();
    }

    /** 客户端 setup 时预热一次资源索引（拿不到资源管理器就留到首次查询再扫）。 */
    //TEMP
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(TestEntityRenderers::registerAssetCache);
    }
}
