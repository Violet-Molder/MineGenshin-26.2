package com.linweiyun.genshin.content.items.weapon;

import com.linweiyun.genshin.config.weapon.WeaponXpConfig;
import com.linweiyun.genshin.config.weapon.WeaponConfig;
import com.linweiyun.genshin.config.weapon.WeaponSubStatConfig;
import com.linweiyun.genshin.config.util.StringDoubleValue;

public class WeaponLevelData {

    public static final int MAX_LEVEL = 90;
    public static final int MAX_ASCENSION = 6;

    public static final int[] ASCENSION_LEVELS = {20, 40, 50, 60, 70, 80};

    public static long getExpToNextLevel(int star, int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return 0;
        int nextLevel = currentLevel + 1;
        try {
            return switch (Math.min(star, 5)) {
                case 5 -> WeaponXpConfig.get5Star(nextLevel);
                case 4 -> WeaponXpConfig.get4Star(nextLevel);
                case 3 -> WeaponXpConfig.get3Star(nextLevel);
                default -> 0;
            };
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getMaxLevel(int ascended) {
        if (ascended == 0) return 20;
        return Math.min(40 + (ascended - 1) * 10, MAX_LEVEL);
    }

    public static int getAscensionBonus(int star) {
        return WeaponConfig.getAscensionBonus(star);
    }

    /**
     * 副词条的成长倍率。
     *
     * <p>⚠️ 档位是**每 5 级一档**（lv1 / lv5 / lv10 / … / lv90 共 19 档，见
     * {@link WeaponSubStatConfig#MULT_01} 那一串），所以必须交给
     * {@link WeaponSubStatConfig#getMultiplier(int)} 去查档 ——
     * 以前这里按 {@code level - 1} 直接索引那张表，等于「升一级跳一档」，
     * 副词条几下就顶到接近满级（1 级 1.0 → 2 级 1.162 → 5 级 1.565 …）。
     */
    public static double getSubStatMultiplier(int level) {
        return WeaponSubStatConfig.getMultiplier(level);
    }
}