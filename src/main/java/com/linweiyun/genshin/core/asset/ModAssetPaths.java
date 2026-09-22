package com.linweiyun.genshin.core.asset;

import com.linweiyun.genshin.Minegenshin;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 统一资源布局的路径拼装 —— 唯一知道「文件放哪」的地方。
 *
 * <h2>完整布局</h2>
 * <pre>
 * assets/minegenshin/
 * ├── item/&lt;id&gt;/
 * │      &lt;id&gt;.json               GeckoLib 模型（也接受 &lt;id&gt;.geo.json）
 * │      &lt;id&gt;.animation.json     动画
 * │      &lt;id&gt;.png                贴图
 * ├── block/&lt;id&gt;/
 * │      blockstate.json         方块状态定义（等价于原版 blockstates/&lt;id&gt;.json）
 * │      &lt;id&gt;.json               方块模型
 * │      &lt;id&gt;.animation.json
 * │      &lt;id&gt;.png
 * │      blockitem/&lt;名字&gt;.json   该方块对应物品的资源，名字默认与方块 id 相同
 * │      blockitem/&lt;名字&gt;.animation.json
 * │      blockitem/&lt;名字&gt;.png
 * ├── entity/&lt;id&gt;/
 * │      &lt;id&gt;.json
 * │      &lt;id&gt;.animation.json
 * │      &lt;id&gt;.png
 * └── character/&lt;id&gt;/
 *        （维持 {@code GenshinAssets} 的既有约定：<id>.animation.json + 可选 <id>.geo.json / <id>.png）
 * </pre>
 *
 * <h2>两种「标识符」不要混淆</h2>
 * <ul>
 *   <li><b>文件位置</b>：带扩展名，给 {@code ResourceManager} 用，例如
 *       {@code minegenshin:entity/test1/test1.json}。</li>
 *   <li><b>资源键</b>：去掉扩展名，给 GeckoLib 的缓存用，例如
 *       {@code minegenshin:entity/test1/test1}。GeckoLib 的
 *       {@code GeckoLibResources.SUFFIX_STRIPPER} 会把 {@code .geo.json} 与 {@code .json}
 *       都剥成同一个键，所以 {@code test1.json} 与 {@code test1.geo.json} 是同一个键 ——
 *       这正是两种写法可以并存的原因。</li>
 * </ul>
 */
public final class ModAssetPaths {

    //TEMP
    public static final String JSON_SUFFIX = ".json";
    //TEMP
    public static final String GEO_SUFFIX = ".geo.json";
    //TEMP
    public static final String ANIMATION_SUFFIX = ".animation.json";
    //TEMP
    public static final String ANIMATIONS_SUFFIX = ".animations.json";
    //TEMP
    public static final String PNG_SUFFIX = ".png";

    /** 方块状态定义的文件名：{@code block/&lt;id&gt;/blockstate.json}。 */
    //TEMP
    public static final String BLOCKSTATE_FILE = "blockstate.json";

    /** 方块物品资源所在子目录名：{@code block/&lt;id&gt;/blockitem/}。 */
    //TEMP
    public static final String BLOCK_ITEM_DIR = "blockitem";

    //TEMP
    private ModAssetPaths() {
    }

    // ==================== 目录 ====================

    /** 某个类别下某个 id 的目录：{@code <类别>/<id>}。 */
    //TEMP
    public static String dir(AssetCategory category, String id) {
        return category.folder() + "/" + id;
    }

    /** 某个方块对应的物品资源目录：{@code block/<方块id>/blockitem}。 */
    //TEMP
    public static String blockItemDir(String blockId) {
        return dir(AssetCategory.BLOCK, blockId) + "/" + BLOCK_ITEM_DIR;
    }

    // ==================== 文件位置 ====================

    /** 类别目录下的任意文件：{@code <类别>/<id>/<relative>}。 */
    //TEMP
    public static Identifier file(AssetCategory category, String id, String relative) {
        return Minegenshin.id(dir(category, id) + "/" + relative);
    }

    /** 按目录 + 基名拼文件：{@code <目录>/<基名><扩展名>}。 */
    //TEMP
    public static Identifier inDir(String directory, String baseName, String suffix) {
        return Minegenshin.id(directory + "/" + baseName + suffix);
    }

    /** 模型文件：{@code <类别>/<id>/<id>.json}。 */
    //TEMP
    public static Identifier geoModel(AssetCategory category, String id) {
        return file(category, id, id + JSON_SUFFIX);
    }

    /** 模型文件（GeckoLib 原生后缀写法）：{@code <类别>/<id>/<id>.geo.json}。 */
    //TEMP
    public static Identifier geoModelWithSuffix(AssetCategory category, String id) {
        return file(category, id, id + GEO_SUFFIX);
    }

    /** 动画文件：{@code <类别>/<id>/<id>.animation.json}。 */
    //TEMP
    public static Identifier animation(AssetCategory category, String id) {
        return file(category, id, id + ANIMATION_SUFFIX);
    }

    /** 贴图文件：{@code <类别>/<id>/<id>.png}。 */
    //TEMP
    public static Identifier texture(AssetCategory category, String id) {
        return file(category, id, id + PNG_SUFFIX);
    }

    /** 方块状态定义：{@code block/<id>/blockstate.json}。 */
    //TEMP
    public static Identifier blockState(String blockId) {
        return file(AssetCategory.BLOCK, blockId, BLOCKSTATE_FILE);
    }

    /** 方块物品的模型：{@code block/<方块id>/blockitem/<物品名>.json}。 */
    //TEMP
    public static Identifier blockItemGeoModel(String blockId, String itemName) {
        return inDir(blockItemDir(blockId), itemName, JSON_SUFFIX);
    }

    /** 方块物品的模型（GeckoLib 原生后缀写法）。 */
    //TEMP
    public static Identifier blockItemGeoModelWithSuffix(String blockId, String itemName) {
        return inDir(blockItemDir(blockId), itemName, GEO_SUFFIX);
    }

    /** 方块物品的动画：{@code block/<方块id>/blockitem/<物品名>.animation.json}。 */
    //TEMP
    public static Identifier blockItemAnimation(String blockId, String itemName) {
        return inDir(blockItemDir(blockId), itemName, ANIMATION_SUFFIX);
    }

    /** 方块物品的贴图：{@code block/<方块id>/blockitem/<物品名>.png}。 */
    //TEMP
    public static Identifier blockItemTexture(String blockId, String itemName) {
        return inDir(blockItemDir(blockId), itemName, PNG_SUFFIX);
    }

    // ==================== 资源键（喂给 GeckoLib） ====================

    /**
     * 模型 / 动画的缓存键：{@code <类别>/<id>/<id>}（无扩展名）。
     *
     * <p>GeckoLib 对模型和动画剥掉的是同一个后缀集合（{@code .geo.json} / {@code .animation.json} /
     * {@code .json}），所以两者共用同一个键。
     */
    //TEMP
    public static Identifier assetKey(AssetCategory category, String id) {
        return Minegenshin.id(dir(category, id) + "/" + id);
    }

    /** 方块物品的缓存键：{@code block/<方块id>/blockitem/<物品名>}。 */
    //TEMP
    public static Identifier blockItemKey(String blockId, String itemName) {
        return Minegenshin.id(blockItemDir(blockId) + "/" + itemName);
    }

    // ==================== 后缀判定 ====================

    /**
     * 模型文件判定：{@code .json} 结尾，但不是动画、也不是方块状态定义。
     *
     * <p>{@code .geo.json} 同样算模型 —— 两种写法都收。
     */
    //TEMP
    public static boolean isGeoModelFile(Identifier raw) {
        if (raw == null) {
            return false;
        }
        String path = raw.getPath();
        if (!path.endsWith(JSON_SUFFIX) || path.endsWith(ANIMATION_SUFFIX) || path.endsWith(ANIMATIONS_SUFFIX)) {
            return false;
        }
        return !path.endsWith("/" + BLOCKSTATE_FILE);
    }

    /** 动画文件判定：{@code .animation.json} 或 {@code .animations.json} 结尾。 */
    //TEMP
    public static boolean isAnimationFile(Identifier raw) {
        if (raw == null) {
            return false;
        }
        String path = raw.getPath();
        return path.endsWith(ANIMATION_SUFFIX) || path.endsWith(ANIMATIONS_SUFFIX);
    }

    /** 从文件位置算出模型缓存键（剥 {@code .geo.json}，再剥 {@code .json}）。 */
    //TEMP
    public static Identifier modelKeyOf(Identifier raw) {
        return strip(raw, GEO_SUFFIX, JSON_SUFFIX);
    }

    /** 从文件位置算出动画缓存键（剥 {@code .animation.json} / {@code .animations.json} / {@code .json}）。 */
    //TEMP
    public static Identifier animationKeyOf(Identifier raw) {
        return strip(raw, ANIMATION_SUFFIX, ANIMATIONS_SUFFIX, JSON_SUFFIX);
    }

    //TEMP
    private static Identifier strip(@Nullable Identifier raw, String... suffixes) {
        if (raw == null) {
            return null;
        }
        String path = raw.getPath();
        for (String suffix : suffixes) {
            if (path.endsWith(suffix)) {
                path = path.substring(0, path.length() - suffix.length());
                break;
            }
        }
        return Identifier.fromNamespaceAndPath(raw.getNamespace(), path);
    }

    // ==================== 反查 ====================

    /** 这个资源位置属于哪个类别；不在四种统一布局里就返回 null。 */
    //TEMP
    @Nullable
    public static AssetCategory categoryOf(@Nullable Identifier raw) {
        if (raw == null || !Minegenshin.MOD_ID.equals(raw.getNamespace())) {
            return null;
        }
        String path = raw.getPath();
        for (AssetCategory category : AssetCategory.values()) {
            if (category.matchesPath(path)) {
                return category;
            }
        }
        return null;
    }

    /**
     * 从资源位置里取出 id：类别文件夹之后的<b>第一段</b>。
     *
     * <pre>
     * entity/test1/test1.json            → test1
     * block/first_block/blockitem/a.json → first_block
     * item/test_sword/test_sword.png     → test_sword
     * </pre>
     */
    //TEMP
    @Nullable
    public static String idOf(@Nullable Identifier raw) {
        AssetCategory category = categoryOf(raw);
        if (category == null) {
            return null;
        }
        String remainder = raw.getPath().substring(category.folder().length() + 1);
        int slash = remainder.indexOf('/');
        String id = slash < 0 ? remainder : remainder.substring(0, slash);
        return id.isEmpty() ? null : id;
    }

    /**
     * 从资源位置里取出「作用」：类别 + id 之后剩下的相对路径，也就是文件名。
     *
     * <pre>
     * entity/test1/test1.json            → test1.json
     * block/first_block/blockstate.json  → blockstate.json
     * block/first_block/blockitem/a.png  → blockitem/a.png
     * </pre>
     */
    //TEMP
    @Nullable
    public static String roleOf(@Nullable Identifier raw) {
        AssetCategory category = categoryOf(raw);
        if (category == null) {
            return null;
        }
        String remainder = raw.getPath().substring(category.folder().length() + 1);
        int slash = remainder.indexOf('/');
        if (slash < 0) {
            return null;
        }
        String role = remainder.substring(slash + 1);
        return role.isEmpty() ? null : role;
    }

    /** 这个资源位置是不是方块状态定义。 */
    //TEMP
    public static boolean isBlockState(@Nullable Identifier raw) {
        return raw != null
                && AssetCategory.BLOCK.matchesPath(raw.getPath())
                && raw.getPath().endsWith("/" + BLOCKSTATE_FILE);
    }

    /** 这个资源位置是不是方块物品资源（{@code block/<id>/blockitem/…}）。 */
    //TEMP
    public static boolean isBlockItem(@Nullable Identifier raw) {
        return raw != null
                && AssetCategory.BLOCK.matchesPath(raw.getPath())
                && raw.getPath().contains("/" + BLOCK_ITEM_DIR + "/");
    }

    /**
     * 资源位置所在目录：去掉最后一段。
     *
     * <pre>
     * entity/test1/test1.json             → entity/test1
     * block/first_block/blockitem/a.png   → block/first_block/blockitem
     * </pre>
     */
    //TEMP
    @Nullable
    public static String dirOf(@Nullable Identifier raw) {
        if (raw == null) {
            return null;
        }
        String path = raw.getPath();
        int slash = path.lastIndexOf('/');
        return slash <= 0 ? null : path.substring(0, slash);
    }

    /**
     * 资源位置的基名：最后一段去掉已知扩展名。
     *
     * <pre>
     * entity/test1/test1.animation.json   → test1
     * entity/test1/test1.geo.json         → test1
     * entity/test1/test1.png              → test1
     * block/first_block/blockstate.json   → blockstate
     * </pre>
     */
    //TEMP
    @Nullable
    public static String baseNameOf(@Nullable Identifier raw) {
        if (raw == null) {
            return null;
        }
        String path = raw.getPath();
        int slash = path.lastIndexOf('/');
        String file = slash < 0 ? path : path.substring(slash + 1);
        for (String suffix : new String[]{ANIMATION_SUFFIX, ANIMATIONS_SUFFIX, GEO_SUFFIX, JSON_SUFFIX, PNG_SUFFIX}) {
            if (file.endsWith(suffix)) {
                return file.substring(0, file.length() - suffix.length());
            }
        }
        return file;
    }

    /** 目录的最后一段：{@code entity/test1} → {@code test1}。 */
    //TEMP
    @Nullable
    public static String dirNameOf(@Nullable String dir) {
        if (dir == null) {
            return null;
        }
        int slash = dir.lastIndexOf('/');
        return slash < 0 ? dir : dir.substring(slash + 1);
    }
}
