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

    public static double getSubStatMultiplier(int level) {
        int idx = Math.max(0, Math.min(level - 1, WeaponSubStatConfig.MULT_01 == null ? 0 : 18));
        StringDoubleValue[] mults = {
            WeaponSubStatConfig.MULT_01, WeaponSubStatConfig.MULT_02, WeaponSubStatConfig.MULT_03,
            WeaponSubStatConfig.MULT_04, WeaponSubStatConfig.MULT_05, WeaponSubStatConfig.MULT_06,
            WeaponSubStatConfig.MULT_07, WeaponSubStatConfig.MULT_08, WeaponSubStatConfig.MULT_09,
            WeaponSubStatConfig.MULT_10, WeaponSubStatConfig.MULT_11, WeaponSubStatConfig.MULT_12,
            WeaponSubStatConfig.MULT_13, WeaponSubStatConfig.MULT_14, WeaponSubStatConfig.MULT_15,
            WeaponSubStatConfig.MULT_16, WeaponSubStatConfig.MULT_17, WeaponSubStatConfig.MULT_18,
            WeaponSubStatConfig.MULT_19
        };
        return mults[idx].get();
    }
}