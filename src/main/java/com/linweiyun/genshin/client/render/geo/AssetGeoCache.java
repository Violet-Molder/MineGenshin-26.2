package com.linweiyun.genshin.client.render.geo;

import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.loading.loader.GeckoLibLoader;
import com.geckolib.loading.math.MathParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.linweiyun.genshin.core.asset.AssetCategory;
import com.linweiyun.genshin.core.asset.ModAssetPaths;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.PreparableReloadListener.SharedState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 统一布局资源索引缓存：扫描 {@code item/}、{@code block/}、{@code entity/} 三个根，
 * 把与目录同名的模型 / 动画 / 贴图预烘培成 GeckoLib 的对象，供 {@link CategoryGeoModel}
 * 按「目录」一次性取用（{@link #files(String)}）。
 *
 * <p><b>刻意不注册成全局资源重载监听器。</b>它实现 {@link PreparableReloadListener}
 * 只是为了「万一要注册」时可用；实际靠 {@link #warmUp()} 和首次查询时的
 * {@link #ensureLoaded()} 同步兜底。原因见 {@code TestEntityRenderers} 的注释：
 * 它一旦作为重载监听器抛异常，<b>整个客户端资源重载都会失败</b>，
 * 表现是「所有资源驱动的东西一起失效」，排查时完全指不到这里。
 *
 * <p>两类防御是那次事故的直接产物：
 * <ul>
 *   <li>{@link #scan} 用 try/catch(Throwable) 把失败限制在「本次跳过统一布局资源」；</li>
 *   <li>所有 Map 复制都走 {@link #copyNonNull} —— GeckoLib 的 loader 解析失败时
 *       会往 Map 里塞 null，直接 {@code Map.copyOf} 会 NPE 并炸掉整个重载。</li>
 * </ul>
 */
public final class AssetGeoCache implements PreparableReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final GeckoLibLoader<JsonObject> LOADER = new GeckoLibGsonLoader();
    private static final String[] ROOTS = new String[]{AssetCategory.ITEM.folder(), AssetCategory.BLOCK.folder(), AssetCategory.ENTITY.folder()};
    private static volatile Map<Identifier, BakedGeoModel> models = Map.of();
    private static volatile Map<Identifier, BakedAnimations> animations = Map.of();
    private static volatile Map<String, AssetGeoCache.DirFiles> index = Map.of();
    private static volatile boolean reloaded = false;

    @Nullable
    public static BakedGeoModel model(@Nullable Identifier location) {
        if (location == null) {
            return null;
        }

        ensureLoaded();
        return models.get(location);
    }

    @Nullable
    public static Animation animation(@Nullable Identifier animationKey, @Nullable String name) {
        if (animationKey != null && name != null) {
            ensureLoaded();
            BakedAnimations baked = animations.get(animationKey);
            if (baked == null) {
                return null;
            }

            Animation exact = baked.getAnimation(name);
            if (exact != null) {
                return exact;
            }

            String matched = fuzzyAnimationName(baked, name);
            if (matched == null) {
                return null;
            }

            LOGGER.info("[AssetGeoCache] 动画名宽松匹配：{} 里没有 '{}'，改用 '{}'", new Object[]{animationKey, name, matched});
            return baked.getAnimation(matched);
        } else {
            return null;
        }
    }

    public static Set<String> animationNames(@Nullable Identifier animationKey) {
        if (animationKey == null) {
            return Set.of();
        }

        ensureLoaded();
        BakedAnimations baked = animations.get(animationKey);
        return baked == null ? Set.of() : baked.animations().keySet();
    }

    @Nullable
    private static String fuzzyAnimationName(BakedAnimations baked, String wanted) {
        String suffix = "." + wanted;
        String candidate = null;

        for (String available : baked.animations().keySet()) {
            if (available.equals(wanted) || available.endsWith(suffix)) {
                if (candidate != null) {
                    return null;
                }

                candidate = available;
            }
        }

        return candidate;
    }

    public static AssetGeoCache.DirFiles files(@Nullable String dir) {
        if (dir == null) {
            return AssetGeoCache.DirFiles.EMPTY;
        }

        ensureLoaded();
        return index.getOrDefault(dir, AssetGeoCache.DirFiles.EMPTY);
    }

    public static int dirCount() {
        return index.size();
    }

    public static void warmUp() {
        ensureLoaded();
    }

    public static int modelCount() {
        return models.size();
    }

    public static Set<String> knownDirs() {
        ensureLoaded();
        return index.keySet();
    }

    private static void ensureLoaded() {
        if (!reloaded) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                ResourceManager resourceManager = minecraft.getResourceManager();
                if (resourceManager != null) {
                    synchronized (AssetGeoCache.class) {
                        if (!reloaded) {
                            LOGGER.warn("[AssetGeoCache] 重载监听器未生效，改为首次查询时同步扫描");
                            AssetGeoCache instance = new AssetGeoCache();
                            instance.apply(instance.scan(resourceManager));
                        }
                    }
                }
            }
        }
    }

    public CompletableFuture<Void> reload(SharedState sharedState, Executor prepExecutor, PreparationBarrier barrier, Executor applyExecutor) {
        ResourceManager resourceManager = sharedState.resourceManager();
        return CompletableFuture.<AssetGeoCache.Scanned>supplyAsync(() -> this.scan(resourceManager), prepExecutor)
            .<AssetGeoCache.Scanned>thenCompose(barrier::wait)
            .thenAcceptAsync(this::apply, applyExecutor);
    }

    private AssetGeoCache.Scanned scan(ResourceManager resourceManager) {
        try {
            return this.scanUnsafe(resourceManager);
        } catch (Throwable t) {
            LOGGER.error("[AssetGeoCache] 扫描失败，本次跳过统一布局资源（不影响其它资源重载）", t);
            return new AssetGeoCache.Scanned(Map.of(), Map.of(), Map.of());
        }
    }

    private AssetGeoCache.Scanned scanUnsafe(ResourceManager resourceManager) {
        Map<Identifier, BakedGeoModel> foundModels = new HashMap<>(models);
        Map<Identifier, BakedAnimations> foundAnimations = new HashMap<>(animations);
        Map<String, AssetGeoCache.DirFiles> foundIndex = new HashMap<>(index);
        MathParser mathParser = MathParser.createWithDeduplication();

        for (String root : ROOTS) {
            Map<Identifier, Resource> resources;
            try {
                resources = resourceManager.listResources(root, id -> id.getNamespace().equals("minegenshin"));
            } catch (Exception e) {
                LOGGER.warn("[AssetGeoCache] 扫描根 '{}' 失败：{}", root, e.toString());
                continue;
            }

            int before = foundModels.size() + foundAnimations.size();

            for (Entry<Identifier, Resource> entry : resources.entrySet()) {
                Identifier raw = entry.getKey();
                String path = raw.getPath();
                if (path.startsWith(root + "/")) {
                    String dir = ModAssetPaths.dirOf(raw);
                    if (dir != null) {
                        if (path.endsWith(".png")) {
                            foundIndex.merge(dir, new AssetGeoCache.DirFiles(null, null, raw), AssetGeoCache::preferTexture);
                        } else if (path.endsWith(".json") && !ModAssetPaths.isBlockState(raw)) {
                            AssetGeoCache.ContentKind kind = classify(entry.getValue());
                            if (kind == AssetGeoCache.ContentKind.UNKNOWN) {
                                kind = ModAssetPaths.isAnimationFile(raw) ? AssetGeoCache.ContentKind.ANIMATION : AssetGeoCache.ContentKind.MODEL;
                                LOGGER.info("[AssetGeoCache] {} 的内容判不出类型，按文件名当作 {}", raw, kind);
                            }

                            try {
                                if (kind == AssetGeoCache.ContentKind.ANIMATION) {
                                    JsonObject json = (JsonObject)LOADER.deserializeGeckoLibAnimationFile(raw, entry.getValue());
                                    BakedAnimations baked = LOADER.bakeGeckoLibAnimationsFile(raw, json, mathParser);
                                    if (baked != null) {
                                        foundAnimations.put(ModAssetPaths.animationKeyOf(raw), baked);
                                        foundIndex.merge(dir, new AssetGeoCache.DirFiles(null, raw, null), AssetGeoCache::preferAnimation);
                                    } else {
                                        LOGGER.warn("[AssetGeoCache] 动画烘培返回 null，跳过：{}", raw);
                                    }
                                } else {
                                    JsonObject json = (JsonObject)LOADER.deserializeGeckoLibModelFile(raw, entry.getValue());
                                    BakedGeoModel baked = LOADER.bakeGeckoLibModelFile(raw, json);
                                    if (baked != null) {
                                        foundModels.put(ModAssetPaths.modelKeyOf(raw), baked);
                                        foundIndex.merge(dir, new AssetGeoCache.DirFiles(raw, null, null), AssetGeoCache::preferModel);
                                    } else {
                                        LOGGER.warn("[AssetGeoCache] 模型烘培返回 null，跳过：{}", raw);
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.error("[AssetGeoCache] 烘培失败（文件读到了，但 GeckoLib 解析不了；这种情况下运行时会报「Unable to find animation file」）：{}", raw, e);
                            }
                        }
                    }
                }
            }

            LOGGER.info(
                "[AssetGeoCache] 扫描根 '{}'：命中文件 {} 个，新烘培 {} 个", new Object[]{root, resources.size(), foundModels.size() + foundAnimations.size() - before}
            );
        }

        return new AssetGeoCache.Scanned(copyNonNull(foundModels), copyNonNull(foundAnimations), copyNonNull(foundIndex));
    }

    private static <K, V> Map<K, V> copyNonNull(Map<K, V> source) {
        Map<K, V> clean = new HashMap<>(source.size());
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                clean.put((K)key, (V)value);
            }
        });
        return Map.copyOf(clean);
    }

    private static AssetGeoCache.ContentKind classify(Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root != null && root.isJsonObject()) {
                JsonObject json = root.getAsJsonObject();
                if (json.has("animations")) {
                    return AssetGeoCache.ContentKind.ANIMATION;
                } else {
                    return !json.has("minecraft:geometry") && !json.has("geometry") ? AssetGeoCache.ContentKind.UNKNOWN : AssetGeoCache.ContentKind.MODEL;
                }
            } else {
                return AssetGeoCache.ContentKind.UNKNOWN;
            }
        } catch (Exception e) {
            LOGGER.warn("[AssetGeoCache] 读取 {} 失败：{}", resource.sourcePackId(), e.toString());
            return AssetGeoCache.ContentKind.UNKNOWN;
        }
    }

    private static AssetGeoCache.DirFiles preferTexture(AssetGeoCache.DirFiles existing, AssetGeoCache.DirFiles incoming) {
        if (existing.texture() == null) {
            return new AssetGeoCache.DirFiles(existing.model(), existing.animation(), incoming.texture());
        } else {
            return matchesDirName(incoming.texture()) && !matchesDirName(existing.texture())
                ? new AssetGeoCache.DirFiles(existing.model(), existing.animation(), incoming.texture())
                : existing;
        }
    }

    private static AssetGeoCache.DirFiles preferAnimation(AssetGeoCache.DirFiles existing, AssetGeoCache.DirFiles incoming) {
        if (existing.animation() == null) {
            return new AssetGeoCache.DirFiles(existing.model(), incoming.animation(), existing.texture());
        } else {
            return matchesDirName(incoming.animation()) && !matchesDirName(existing.animation())
                ? new AssetGeoCache.DirFiles(existing.model(), incoming.animation(), existing.texture())
                : existing;
        }
    }

    private static AssetGeoCache.DirFiles preferModel(AssetGeoCache.DirFiles existing, AssetGeoCache.DirFiles incoming) {
        if (existing.model() == null) {
            return new AssetGeoCache.DirFiles(incoming.model(), existing.animation(), existing.texture());
        }

        boolean incomingNamed = matchesDirName(incoming.model());
        boolean existingNamed = matchesDirName(existing.model());
        if (incomingNamed != existingNamed) {
            return new AssetGeoCache.DirFiles(incomingNamed ? incoming.model() : existing.model(), existing.animation(), existing.texture());
        }

        boolean incomingPlain = !incoming.model().getPath().endsWith(".geo.json");
        boolean existingPlain = !existing.model().getPath().endsWith(".geo.json");
        return incomingPlain != existingPlain
            ? new AssetGeoCache.DirFiles(incomingPlain ? incoming.model() : existing.model(), existing.animation(), existing.texture())
            : existing;
    }

    private static boolean matchesDirName(@Nullable Identifier file) {
        String dir = ModAssetPaths.dirOf(file);
        String base = ModAssetPaths.baseNameOf(file);
        return dir != null && base != null && base.equals(ModAssetPaths.dirNameOf(dir));
    }

    private void apply(AssetGeoCache.Scanned scanned) {
        models = scanned.models();
        animations = scanned.animations();
        index = scanned.index();
        reloaded = true;
        LOGGER.info("[AssetGeoCache] 已索引 {} 个目录 / {} 个模型 / {} 个动画文件", new Object[]{index.size(), models.size(), animations.size()});
        if (LOGGER.isInfoEnabled()) {
            for (Entry<String, AssetGeoCache.DirFiles> entry : new TreeMap<>(index).entrySet()) {
                AssetGeoCache.DirFiles files = entry.getValue();
                LOGGER.info(
                    "[AssetGeoCache]   {}  model={}  animation={}  texture={}",
                    new Object[]{entry.getKey(), describe(files.model()), describe(files.animation()), describe(files.texture())}
                );
            }

            for (Entry<Identifier, BakedAnimations> entry : new TreeMap<>(animations).entrySet()) {
                LOGGER.info("[AssetGeoCache]   动画 {} 里可用名字：{}", entry.getKey(), entry.getValue().animations().keySet());
            }
        }
    }

    private static String describe(@Nullable Identifier location) {
        return location == null ? "MISSING" : location.toString();
    }

    private enum ContentKind {
        MODEL,
        ANIMATION,
        UNKNOWN;
    }

    public record DirFiles(@Nullable Identifier model, @Nullable Identifier animation, @Nullable Identifier texture) {
        public static final AssetGeoCache.DirFiles EMPTY = new AssetGeoCache.DirFiles(null, null, null);
    }

    private record Scanned(Map<Identifier, BakedGeoModel> models, Map<Identifier, BakedAnimations> animations, Map<String, AssetGeoCache.DirFiles> index) {
    }
}
