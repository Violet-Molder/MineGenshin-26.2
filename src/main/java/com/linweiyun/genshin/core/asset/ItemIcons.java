package com.linweiyun.genshin.core.asset;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 物品图标解析 —— GUI 里该画哪张图。
 *
 * <h2>为什么不能直接画物品贴图</h2>
 * 原版「平面物品」的 {@code textures/item/<名字>.png} 本身就是图标，直接画没问题；
 * 但 GeckoLib 的 <b>geo 物品</b>（比如 {@code test_sword}）那张 png 是
 * <b>3D 模型的 UV 展开图集</b>（32×32 起），直接当 2D 精灵画出来是一坨错位色块，
 * 尺寸也对不上。所以这类物品必须另配一张图标。
 *
 * <h2>解析顺序</h2>
 * <ol>
 *   <li>{@code minegenshin:icon/item/<物品名>.png} —— 本 MOD 的图标目录；
 *       物品来自别的命名空间时也先找本 MOD 的，方便给原版/其它 MOD 的物品补图标。</li>
 *   <li>{@code <该物品自己的命名空间>:icon/item/<物品名>.png} —— 让其它 MOD 也能自带图标。</li>
 *   <li>都找不到就退回 {@code <命名空间>:textures/item/<物品名>.png}（旧的画法，原版平面物品仍然正确）。</li>
 * </ol>
 *
 * <h2>目录约定</h2>
 * <pre>
 * assets/minegenshin/icon/item/test_sword.png
 * assets/minegenshin/icon/artifact/crimson_flower.png
 * assets/minegenshin/icon/weapon/everlasting_moonglow.png
 * </pre>
 */
public final class ItemIcons {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 找不到任何图时用的兜底。 */
    public static final String EMPTY = GenshinAssets.MOD_ID + ":textures/empty.png";

    /** 分类：物品图标。 */
    public static final String CATEGORY_ITEM = "item";

    /** 解析结果缓存：注册名 → 实际要画的路径字符串（可能是 null 表示用兜底）。 */
    private static final Map<Identifier, String> CACHE = new ConcurrentHashMap<>();

    private ItemIcons() {
    }

    /**
     * 这件物品在 GUI 里该画哪张图。
     *
     * @return 可以直接喂给 {@code SpriteTexture.of(String)} 的字符串
     */
    public static String pathOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return EMPTY;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return EMPTY;
        }
        String resolved = CACHE.computeIfAbsent(itemId, ItemIcons::resolve);
        return resolved == null ? EMPTY : resolved;
    }

    /** 这件物品有没有专属图标（没有的话 GUI 画的是它的物品贴图）。 */
    public static boolean hasIcon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && iconFor(itemId) != null;
    }

    /** 清掉缓存（资源重载后图标可能新增）。 */
    public static void invalidate() {
        CACHE.clear();
    }

    // ==================== 内部 ====================

    @Nullable
    private static String resolve(Identifier itemId) {
        Identifier icon = iconFor(itemId);
        if (icon != null) {
            return icon.toString();
        }

        // 退回物品自己的贴图：原版平面物品的贴图就是图标，画出来是对的。
        // geo 物品的贴图是 UV 图集，画出来会是一坨错位色块 —— 所以这类物品请补一张图标。
        Identifier flat = flatTexture(itemId);
        if (exists(flat)) {
            return flat.toString();
        }

        // 再退一步：统一布局里 item/<名字>/<名字>.png（geo 物品的贴图通常在这）
        Identifier unified = Identifier.fromNamespaceAndPath(
                GenshinAssets.MOD_ID, GenshinAssets.ITEM_ROOT + "/" + itemId.getPath() + "/" + itemId.getPath() + ".png");
        if (exists(unified)) {
            return unified.toString();
        }

        return null;
    }

    /** 按「本 MOD 优先 → 物品自己的命名空间」找一个真实存在的图标。 */
    @Nullable
    private static Identifier iconFor(Identifier itemId) {
        Identifier own = GenshinAssets.icon(CATEGORY_ITEM, itemId.getPath());
        if (exists(own)) {
            return own;
        }
        Identifier their = Identifier.fromNamespaceAndPath(
                itemId.getNamespace(), GenshinAssets.ICON_ROOT + "/" + CATEGORY_ITEM + "/" + itemId.getPath() + ".png");
        if (exists(their)) {
            return their;
        }
        return null;
    }

    private static Identifier flatTexture(Identifier itemId) {
        return Identifier.fromNamespaceAndPath(
                itemId.getNamespace(), "textures/item/" + itemId.getPath() + ".png");
    }

    /** 这个资源在不在资源包里。只在解析时查一次，结果进缓存。 */
    private static boolean exists(Identifier id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return false;
        }
        try {
            Optional<Resource> resource = minecraft.getResourceManager().getResource(id);
            return resource.isPresent();
        } catch (Exception e) {
            LOGGER.debug("[ItemIcons] 查询图标失败：{}", id, e);
            return false;
        }
    }
}
