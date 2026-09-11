package com.linweiyun.genshin.client.damage;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class DamageIndicatorManager {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final List<DamageIndicator> ACTIVE = new ArrayList<>();
    private static final int MAX_INDICATORS = 256;

    private DamageIndicatorManager() {}

    public static void add(DamageIndicator indicator) {
        ACTIVE.add(indicator);
        if (ACTIVE.size() > MAX_INDICATORS) {
            ACTIVE.remove(0);
            LOGGER.warn("[DI-Manager] exceeded max, evicting oldest");
        }
        LOGGER.info("[DI-Manager] add -> active size = {}", ACTIVE.size());
    }

    /** 由客户端 Tick 事件驱动，移除过期飘字 */
    public static void tick() {
        if (ACTIVE.isEmpty()) return;
        int before = ACTIVE.size();
        ACTIVE.removeIf(DamageIndicator::isExpired);
        int removed = before - ACTIVE.size();
        if (removed > 0) {
            LOGGER.info("[DI-Manager] tick removed {} expired, active size = {}", removed, ACTIVE.size());
        }
    }

    public static List<DamageIndicator> getActive() {
        return ACTIVE;
    }

    public static void clear() {
        LOGGER.info("[DI-Manager] clear, was size = {}", ACTIVE.size());
        ACTIVE.clear();
    }
}