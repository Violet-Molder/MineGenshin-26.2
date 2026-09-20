package com.linweiyun.genshin.core.system.combat.action.data;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.Map;

/**
 * 角色渲染定义：模型/动画/贴图路径 + 常态动画映射。
 * 双端安全（只存储字符串，通过 toIdentifier 生成 GeckoLib 所需路径）。
 * <p>
 * 每个角色在自己的包里定义渲染数据，通过 {@link CharacterRenderRepository} 注册。
 */
public final class CharacterRenderData {

    private final String id;
    private final String modelPath;
    public final String texturePath;
    public final String animationPath;
    private final Map<String, String> animMapping;
    private final float bodyScale;

    public CharacterRenderData(String id, String modelPath, String texturePath,
                               String animationPath, Map<String, String> animMapping,
                               float bodyScale) {
        this.id = id;
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.animationPath = animationPath;
        this.animMapping = animMapping == null ? Collections.emptyMap() : animMapping;
        this.bodyScale = bodyScale;
    }

    public String id() { return id; }
    public String modelPath() { return modelPath; }

    /**
     * 生成 GeckoLib 用的模型 Identifier（自动剥离 .geo.json 后缀）。
     * 例：modelPath="default/default.geo.json"
     * → minegenshin:character/default/default
     */
    public Identifier modelIdentifier(String modId) {
        String path = stripSuffix(modelPath, ".geo.json");
        return Identifier.fromNamespaceAndPath(modId, "character/" + path);
    }

    public Identifier textureIdentifier(String modId) {
        return Identifier.fromNamespaceAndPath(modId, "textures/character/" + texturePath);
    }

    /**
     * 生成动画 Identifier（自动剥离 .animation.json 后缀）。
     * 例：animationPath="vesna/vesna.animation.json"
     * → minegenshin:character/vesna/vesna.animation
     */
    public Identifier animationIdentifier(String modId) {
        String path = stripSuffix(animationPath, ".json");
        return Identifier.fromNamespaceAndPath(modId, "character/" + path);
    }

    private static String stripSuffix(String path, String suffix) {
        if (path != null && path.endsWith(suffix)) {
            return path.substring(0, path.length() - suffix.length());
        }
        return path;
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