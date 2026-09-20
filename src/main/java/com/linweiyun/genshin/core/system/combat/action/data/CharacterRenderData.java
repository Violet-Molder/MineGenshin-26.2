package com.linweiyun.genshin.core.system.combat.action.data;

import com.linweiyun.genshin.core.asset.GenshinAssets;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 角色渲染定义：模型/动画/贴图<b>相对路径</b> + 常态动画映射 + 骨骼挂点。
 *
 * <h2>目录约定</h2>
 * 三个字段都是相对 {@code assets/minegenshin/} 的路径。默认组合是
 * <b>模型贴图共用、动画独立</b>：
 * <pre>
 * character/default/default.geo.json      ← 所有角色共用
 * character/default/default.png           ← 所有角色共用
 * character/vesna/vesna.animation.json    ← 每个角色自己的
 * </pre>
 * 用 {@link #character(String, Map, float, CharacterBoneMount...)} 就是这套默认值；
 * 某个角色要做专属模型时，用显式路径构造。
 *
 * <p>想换目录不要改这里 —— 用 {@link com.linweiyun.genshin.core.asset.GeoPathOverrides} 注册规则。
 *
 * <p>双端安全（只存字符串）。每个角色在自己的包里定义，通过 {@link CharacterRenderRepository} 注册。
 */
public final class CharacterRenderData {

    private final String id;
    private final String modelPath;
    private final String texturePath;
    private final String animationPath;
    /** 主动画文件之外的额外动画文件（第一人称动画、动作包……）。 */
    private final List<String> extraAnimationPaths;
    private final Map<String, String> animMapping;
    private final float bodyScale;
    private final List<CharacterBoneMount> boneMounts;

    /**
     * 最省事的写法：<b>共用模型 + 共用贴图 + 本角色独立动画</b>。
     *
     * <pre>
     * character/default/default.geo.json
     * character/default/default.png
     * character/&lt;角色id&gt;/&lt;角色id&gt;.animation.json
     * </pre>
     */
    public static CharacterRenderData character(String id, Map<String, String> animMapping,
                                                float bodyScale, CharacterBoneMount... boneMounts) {
        return new CharacterRenderData(id,
                GenshinAssets.defaultModelPath(),
                GenshinAssets.defaultTexturePath(),
                GenshinAssets.characterAnimationPath(id),
                animMapping, bodyScale, boneMounts);
    }

    /**
     * 这个角色有专属模型：三个文件都在 {@code character/<角色id>/} 下，同名。
     */
    public static CharacterRenderData characterWithOwnModel(String id, Map<String, String> animMapping,
                                                            float bodyScale, CharacterBoneMount... boneMounts) {
        return new CharacterRenderData(id,
                GenshinAssets.characterModelPath(id),
                GenshinAssets.characterTexturePath(id),
                GenshinAssets.characterAnimationPath(id),
                animMapping, bodyScale, boneMounts);
    }

    public CharacterRenderData(String id, String modelPath, String texturePath,
                               String animationPath, Map<String, String> animMapping,
                               float bodyScale) {
        this(id, modelPath, texturePath, animationPath, animMapping, bodyScale, List.of());
    }

    /**
     * @param boneMounts 骨骼挂点；空表示这个角色不做任何骨骼替换。
     *                   一个角色可以挂多根骨骼（剑身、剑鞘、背后的弓……各自独立取内容）。
     */
    public CharacterRenderData(String id, String modelPath, String texturePath,
                               String animationPath, Map<String, String> animMapping,
                               float bodyScale, List<CharacterBoneMount> boneMounts) {
        this(id, modelPath, texturePath, animationPath, animMapping, bodyScale,
                boneMounts == null ? new CharacterBoneMount[0] : boneMounts.toArray(new CharacterBoneMount[0]));
    }

    /** 便捷写法：直接列挂点，不用自己包一层 List。 */
    public CharacterRenderData(String id, String modelPath, String texturePath,
                               String animationPath, Map<String, String> animMapping,
                               float bodyScale, CharacterBoneMount... boneMounts) {
        this(id, modelPath, texturePath, animationPath, List.of(), animMapping, bodyScale,
                boneMounts == null ? new CharacterBoneMount[0] : boneMounts);
    }

    /** 完整构造：带额外动画文件。 */
    public CharacterRenderData(String id, String modelPath, String texturePath,
                               String animationPath, List<String> extraAnimationPaths,
                               Map<String, String> animMapping,
                               float bodyScale, CharacterBoneMount... boneMounts) {
        this.id = id;
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.animationPath = animationPath;
        this.extraAnimationPaths = extraAnimationPaths == null ? List.of() : List.copyOf(extraAnimationPaths);
        this.animMapping = animMapping == null ? Collections.emptyMap() : animMapping;
        this.bodyScale = bodyScale;
        this.boneMounts = boneMounts == null
                ? List.of()
                : java.util.Arrays.stream(boneMounts).filter(m -> m != null && m.isValid()).toList();
    }

    public String id() { return id; }
    public String modelPath() { return modelPath; }
    public String texturePath() { return texturePath; }
    public String animationPath() { return animationPath; }

    /** 主文件之外的额外动画文件；空表示动画都在主文件里。 */
    public List<String> extraAnimationPaths() { return extraAnimationPaths; }

    /** 追加一个额外动画文件（第一人称、动作包……）。返回 this 方便链式写。 */
    public CharacterRenderData withAnimationFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return this;
        }
        List<String> merged = new java.util.ArrayList<>(extraAnimationPaths);
        merged.add(relativePath);
        return new CharacterRenderData(id, modelPath, texturePath, animationPath, merged,
                animMapping, bodyScale,
                boneMounts.toArray(new CharacterBoneMount[0]));
    }

    /** 全部动画文件的相对路径（主 + 额外）。 */
    public List<String> allAnimationPaths() {
        List<String> all = new java.util.ArrayList<>(extraAnimationPaths.size() + 1);
        all.add(animationPath);
        all.addAll(extraAnimationPaths);
        return all;
    }

    /** 骨骼挂点列表；空表示不做骨骼替换。 */
    public List<CharacterBoneMount> boneMounts() { return boneMounts; }

    /** 模型 id：剥掉 {@code .geo.json} 后缀。 */
    public Identifier modelIdentifier() {
        return GenshinAssets.fromModelPath(modelPath);
    }

    /** 贴图位置（保留扩展名）。 */
    public Identifier textureIdentifier() {
        return GenshinAssets.fromTexturePath(texturePath);
    }

    /** 动画 id：剥掉 {@code .animation.json} 后缀。 */
    public Identifier animationIdentifier() {
        return GenshinAssets.fromAnimationPath(animationPath);
    }

    public Map<String, String> animMapping() { return animMapping; }
    public float bodyScale() { return bodyScale; }

    public boolean isValid() {
        return id != null && !id.isEmpty()
                && modelPath != null && texturePath != null && animationPath != null;
    }

    // ==================== 默认动画映射 ====================

    public static Map<String, String> defaultAnimMapping() {
        return Map.ofEntries(
                Map.entry("idle", "idle"),
                Map.entry("walk", "walk"),
                Map.entry("run", "run"),
                Map.entry("walk_back", "walk_back"),
                Map.entry("crouch", "crouch"),
                Map.entry("crouch_walk", "crouch_walk"),
                Map.entry("jump", "jump"),
                Map.entry("jump_down", "jump_down"),
                Map.entry("air_idle", "idle"),
                Map.entry("air_move", "walk"),
                Map.entry("air_sprint", "run"),
                Map.entry("swim", "swim"),
                Map.entry("climb", "climb"),
                Map.entry("sleep", "sleep")
        );
    }
}