package com.linweiyun.genshin.content.items.weapon.sword;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.google.common.base.Suppliers;
import com.linweiyun.genshin.client.render.geo.GenshinItemGeoModel;
import com.linweiyun.genshin.core.asset.GenshinAssets;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 测试用剑（GeckoLib geo 物品）。
 *
 * <p>资源全在 {@code assets/minegenshin/item/test_sword/} 下，模型/贴图路径由
 * {@link GenshinItemGeoModel} 统一推出。不用 GeckoLib 的 {@code DefaultedItemGeoModel}：
 * 它会按注册名把路径猜成 {@code geckolib/models/item/test_sword.geo.json} 这种旧布局，
 * 而且猜不出贴图与本 MOD 的统一目录。
 *
 * <p>不注册动画控制器 —— 这把剑是静态的，注册了反而会让 GeckoLib 去找不存在的动画文件并刷错误日志。
 */
public class TestSword extends Sword implements GeoItem {

    /** 物品名（注册名去掉命名空间），也是 {@code item/<名字>/} 这个目录名。 */
    public static final String NAME = "test_sword";

    public TestSword(Properties properties) {
        super(properties);
    }

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private final Supplier<GeoItemRenderer<TestSword>> renderer =
                    Suppliers.memoize(() -> new GeoItemRenderer<>(new TestSwordGeoModel()));

            @Override
            public @Nullable GeoItemRenderer<TestSword> getGeoItemRenderer() {
                return this.renderer.get();
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 静态武器：不需要动画
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** 这把剑的模型：{@code item/test_sword/test_sword.geo.json}。 */
    public static final class TestSwordGeoModel extends GenshinItemGeoModel<TestSword> {
        public TestSwordGeoModel() {
            super(NAME);
        }
    }

    /** GUI 图标路径：{@code minegenshin:icon/item/test_sword.png}。 */
    public static Identifier iconId() {
        return GenshinAssets.itemIcon(NAME);
    }
}
