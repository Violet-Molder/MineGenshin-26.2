package com.linweiyun.genshin.client.render.character;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 从一件物品反解出它的 GeckoLib geo 模型与贴图。
 *
 * <p>骨骼替换要在「源模型里挑一根骨骼」时，先得知道源模型是哪个文件。
 * 物品 → geo 模型这条反解走的是 GeckoLib 自己的约定：
 * 物品是 {@code GeoItem} 时，{@code GeoRenderProvider} 会给出它的 {@link GeoItemRenderer}，
 * 渲染器里挂着它的 {@link GeoModel}，模型按渲染状态报出自己的模型/贴图路径。
 *
 * <p>反解失败（不是 geo 物品、渲染器还没建、资源还没加载）时返回 {@code null}，
 * 调用方退回「整个物品模型」的画法，不会崩也不会空着。
 */
public final class GeoItemModelResolver {

    private GeoItemModelResolver() {
    }

    /** 一件物品的 geo 模型与贴图。 */
    public record Resolved(Identifier modelId, Identifier textureId) {
    }

    /**
     * @return 模型/贴图路径；不是 GeckoLib geo 物品或无法反解时返回 {@code null}
     */
    @Nullable
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Resolved resolve(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        try {
            if (!(stack.getItem() instanceof GeoItem)) {
                return null;
            }

            GeoItemRenderer renderer = GeoRenderProvider.of(stack).getGeoItemRenderer();
            if (renderer == null) {
                return null;
            }

            GeoModel model = renderer.getGeoModel();
            if (model == null) {
                return null;
            }

            GeoItemRenderer.RenderData renderData = new GeoItemRenderer.RenderData(
                    stack,
                    new ItemStackRenderState(),
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    Minecraft.getInstance().level,
                    player);

            GeoRenderState renderState = (GeoRenderState)
                    ((GeoRenderer) renderer).createRenderState((GeoAnimatable) stack.getItem(), renderData);

            return new Resolved(
                    model.getModelResource(renderState),
                    model.getTextureResource(renderState));
        } catch (Exception e) {
            return null;
        }
    }

    /** 模型 id → 烘培好的模型；还没加载出来时返回 {@code null}。 */
    @Nullable
    public static BakedGeoModel bakedModel(Identifier modelId) {
        if (modelId == null) {
            return null;
        }
        BakedGeoModel baked = GeckoLibResources.getBakedModels().getModel(modelId);
        return (baked == null || baked.isMissingno()) ? null : baked;
    }
}
