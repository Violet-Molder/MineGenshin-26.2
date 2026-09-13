package com.linweiyun.genshin.render.gui.hud;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HealthBarTrail {

    private static final Map<Integer, Float> TRAIL_RATIO = new HashMap<>();

    public static float get(int entityId, float currentRatio) {
        return TRAIL_RATIO.computeIfAbsent(entityId, id -> 1.0f);
    }

    public static void update(int entityId, float currentRatio, float speed) {
        float trail = TRAIL_RATIO.getOrDefault(entityId, currentRatio);
        if (trail > currentRatio) {
            trail = Math.max(currentRatio, trail - speed);
        } else {
            trail = currentRatio;
        }
        TRAIL_RATIO.put(entityId, trail);
    }

    public static void remove(int entityId) {
        TRAIL_RATIO.remove(entityId);
    }

    public static void cleanup(Set<Integer> activeIds) {
        TRAIL_RATIO.keySet().removeIf(id -> !activeIds.contains(id));
    }
}