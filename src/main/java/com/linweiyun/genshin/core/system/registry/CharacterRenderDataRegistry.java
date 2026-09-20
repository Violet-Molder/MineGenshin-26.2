package com.linweiyun.genshin.core.system.registry;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

public final class CharacterRenderDataRegistry {

    private static final Map<String, CharacterRenderData> REGISTRY = new ConcurrentHashMap<>();

    private CharacterRenderDataRegistry() {}

    public static void register(String id, CharacterRenderData data) {
        if (id == null || data == null) return;
        Minegenshin.LOGGER.debug("CharacterRenderDataRegistry: registered '{}'", id);
        REGISTRY.put(id, data);
    }

    @Nullable
    public static CharacterRenderData get(String id) {
        return REGISTRY.get(id);
    }

    public static boolean contains(String id) {
        return REGISTRY.containsKey(id);
    }
}
