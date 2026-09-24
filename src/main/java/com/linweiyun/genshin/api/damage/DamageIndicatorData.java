package com.linweiyun.genshin.api.damage;

/**
 * 一条伤害飘字的数据契约：公共侧只负责填这个对象，客户端决定怎么画。
 *
 * @param originX 起点（攻击方）坐标
 * @param targetX 落点（受击方）坐标
 */
public record DamageIndicatorData(
        double originX, double originY, double originZ,
        double targetX, double targetY, double targetZ,
        String text,
        int topColor,
        int bottomColor,
        byte style,
        boolean italic,
        float baseScale,
        float startScale,
        int durationMs
) {}
