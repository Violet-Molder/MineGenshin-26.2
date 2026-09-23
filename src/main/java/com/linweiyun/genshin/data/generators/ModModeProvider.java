package com.linweiyun.genshin.data.generators;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.items.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Set;
import java.util.stream.Stream;

/**
 * 物品模型 / 物品定义（{@code assets/minegenshin/items/*.json} + {@code models/item/*.json}）的数据生成。
 *
 * <h2>为什么必须有这个 Provider</h2>
 * 原版 {@code ModelProvider} 在写完文件后会<b>校验</b>：
 * 本命名空间里注册过的每个物品都必须有一条「物品定义」，缺一个就
 * {@code IllegalStateException: Missing item model definitions for: [...]}，
 * 整个数据生成直接失败 —— 也就是说<b>不能有物品被漏掉</b>，
 * 哪怕它根本不打算用自己的贴图。
 *
 * <h2>三种物品，三种写法</h2>
 * <table border="1">
 *   <caption>按“模型从哪来”分类</caption>
 *   <tr><th>情况</th><th>怎么写</th><th>产出</th></tr>
 *   <tr>
 *     <td>有自己的贴图（普通平面物品）</td>
 *     <td>{@code itemModels.generateFlatItem(item, ModelTemplates.FLAT_ITEM)}</td>
 *     <td>{@code models/item/<名字>.json} + {@code items/<名字>.json}</td>
 *   </tr>
 *   <tr>
 *     <td><b>借原版模型</b>（漆黑碎片 = 下界之星）</td>
 *     <td>{@link #borrowModel}：只写物品定义，指向 {@code minecraft:item/nether_star}</td>
 *     <td>只有 {@code items/<名字>.json}，里面指向别人的模型</td>
 *   </tr>
 *   <tr>
 *     <td>模型是手写的（geo 物品 / 特殊渲染器）</td>
 *     <td>加进 {@link #HAND_WRITTEN_MODELS}，让校验跳过它</td>
 *     <td>数据生成不碰它，手写的 {@code items/*.json} 原样生效</td>
 *   </tr>
 * </table>
 *
 * <h2>为什么手写的要“跳过”而不是“生成一份”</h2>
 * geo 物品的物品定义长这样（{@code test_sword}）：
 * <pre>
 * { "model": { "type": "minecraft:special", "base": "...", "model": { "type": "geckolib:geckolib" } } }
 * </pre>
 * 数据生成只会写平面模型的定义，生成出来就是把这份特殊定义<b>盖掉</b>，
 * 物品立刻变成一个透明方块。所以这类物品只能排除在校验之外。
 */
public class ModModeProvider extends ModelProvider {

    /**
     * 模型是手写的物品 —— 数据生成既不生成、也不校验它们。
     *
     * <p>目前只有 geo 物品（{@code TestSword}）：
     * {@code assets/minegenshin/items/test_sword.json} 用的是 {@code geckolib:geckolib} 特殊模型。
     * <b>以后每加一个 geo 物品 / 自定义特殊渲染器的物品，都要往这里加一行</b>，
     * 否则数据生成会报 “Missing item model definitions”。
     */
    private static final Set<Item> HAND_WRITTEN_MODELS = Set.of(
            ModItems.BEYOND_THE_CHRYSALIS.get(),
            ModItems.WHIRLFLOW_HYMN.get());

    public ModModeProvider(PackOutput output) {
        super(output, Minegenshin.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // ---- 有自己的贴图：普通平面物品 ----
        itemModels.generateFlatItem(ModItems.PRIMOGEM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CRIMSON_FLOWER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CRIMSON_PLUME.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CRIMSON_SANDS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CRIMSON_GOBLET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CRIMSON_CIRCLET.get(), ModelTemplates.FLAT_ITEM);
        // 血红之证：贴图是从魔女套复制的一套，模型同样是普通平面物品
        itemModels.generateFlatItem(ModItems.SCARLET_FLOWER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SCARLET_PLUME.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SCARLET_SANDS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SCARLET_GOBLET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SCARLET_CIRCLET.get(), ModelTemplates.FLAT_ITEM);
        // 千岩牢固：同样是普通平面物品（贴图后补，先借用物品 id 自己的图）
        itemModels.generateFlatItem(ModItems.TENACITY_FLOWER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.TENACITY_PLUME.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.TENACITY_SANDS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.TENACITY_GOBLET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.TENACITY_CIRCLET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SWEET_MADAME.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.EVERLASTING_MOONGLOW.get(), ModelTemplates.FLAT_ITEM);

        // ---- 借原版模型：漆黑碎片照下界之星的样子显示，不单独出图 ----
        borrowModel(itemModels, ModItems.DARK_FRAGMENT.get(),
                Identifier.withDefaultNamespace("item/nether_star"),
                // 和手写时保持一致：换手不播交换动画
                new ClientItem.Properties(false, false, 1.0F));

    }

    /**
     * 物品没有自己的模型，直接引用别人的模型（如原版 {@code minecraft:item/nether_star}）。
     *
     * <p>只写物品定义 {@code assets/<命名空间>/items/<物品名>.json}：
     * <pre>
     * { "model": { "type": "minecraft:model", "model": "minecraft:item/nether_star" } }
     * </pre>
     * <b>不会</b>生成 {@code models/item/<物品名>.json} —— 那是被借的那个模型的事。
     *
     * @param model      要借的模型（可以是任何命名空间的）
     * @param properties 物品的客户端属性（换手动画 / GUI 里的放大等）
     */
    private static void borrowModel(ItemModelGenerators itemModels, Item item,
                                    Identifier model, ClientItem.Properties properties) {
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(model), properties);
    }

    /**
     * 校验时跳过手写模型的物品。
     *
     * <p>{@code getKnownItems()} 就是「这个 Provider 负责哪些物品」——
     * 校验、以及方块物品的自动兜底都用它。这里把它缩小到<b>真的由数据生成管</b>的那些。
     */
    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return super.getKnownItems().filter(holder -> !HAND_WRITTEN_MODELS.contains(holder.value()));
    }
}