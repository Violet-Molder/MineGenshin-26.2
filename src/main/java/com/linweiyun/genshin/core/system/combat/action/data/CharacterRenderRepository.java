package com.linweiyun.genshin.core.system.combat.action.data;


import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 角色渲染定义注册表。
 * <p>
 * 各角色在自己的包里定义 {@link CharacterRenderData}，通过 {@link #register(CharacterRenderData)} 注册。
 * 注册时机：角色类的 static 初始化块或构造函数中。
 * <p>
 * 查询时按角色 ID（与 PGCharacter.textureId 一致）匹配。
 */
public final class CharacterRenderRepository {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, CharacterRenderData> REGISTRY = new LinkedHashMap<>();

    private CharacterRenderRepository() {}

    /**
     * 注册一个角色的渲染定义。通常在角色构造函数中调用。
     * 重复注册同一 ID 会被忽略（先到先得）。
     */
    public static void register(CharacterRenderData data) {
        if (data == null || data.id() == null || data.id().isEmpty()) return;
        if (REGISTRY.containsKey(data.id())) return;
        REGISTRY.put(data.id(), data);
        LOGGER.debug("[CharacterRender] 注册角色渲染: {}", data.id());
    }

    public static CharacterRenderData get(String id) {
        if (id == null || id.isEmpty()) return null;
        return REGISTRY.get(id);
    }

    public static Map<String, CharacterRenderData> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    public static int size() { return REGISTRY.size(); }
}