package com.linweiyun.genshin.content.items.artifact;

import com.linweiyun.genshin.content.attribute.AttributeType;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;

import java.util.HashMap;
import java.util.Map;

public class ArtifactStatData {

    private static final double[] RARITY_RATIO = {0.10, 0.24, 0.48, 0.80, 1.00};

    private static final Map<String, Double> MAIN_STAT_FULL_5 = new HashMap<>();
    private static final Map<String, double[]> SUB_STAT_TIERS_5 = new HashMap<>();
    private static final Map<String, double[]> SUB_STAT_TIERS_4 = new HashMap<>();

    static {
        putMainStat("max_hp",            TeyvatItemStat.StatKind.FLAT,    4780.0);
        putMainStat("atk",               TeyvatItemStat.StatKind.FLAT,    311.0);
        putMainStat("def",               TeyvatItemStat.StatKind.PERCENT, 58.3);
        putMainStat("max_hp",            TeyvatItemStat.StatKind.PERCENT, 46.6);
        putMainStat("atk",               TeyvatItemStat.StatKind.PERCENT, 46.6);
        putMainStat("pyro_bonus",        TeyvatItemStat.StatKind.PERCENT, 46.6);
        putMainStat("hydro_bonus",       TeyvatItemStat.StatKind.PERCENT, 46.6);
        putMainStat("cyro_bonus",        TeyvatItemStat.StatKind.PERCENT, 46.6);
        putMainStat("electro_bonus",     TeyvatItemStat.StatKind.PERCENT, 46.6);
        putMainStat("anemo_bonus",       TeyvatItemStat.StatKind.PERCENT, 46.6);
        putMainStat("geo_bonus",         TeyvatItemStat.StatKind.PERCENT, 46.6);
        putMainStat("dendro_bonus",      TeyvatItemStat.StatKind.PERCENT, 46.6);
        putMainStat("physical_bonus",    TeyvatItemStat.StatKind.PERCENT, 58.3);
        putMainStat("energy_recharge",   TeyvatItemStat.StatKind.PERCENT, 51.8);
        putMainStat("elemental_mastery", TeyvatItemStat.StatKind.FLAT,    187.0);
        putMainStat("crit_rate",         TeyvatItemStat.StatKind.PERCENT, 31.1);
        putMainStat("crit_dmg",          TeyvatItemStat.StatKind.PERCENT, 62.2);
        putMainStat("healing_bonus",     TeyvatItemStat.StatKind.PERCENT, 35.9);

        putSubStat_5("atk",               TeyvatItemStat.StatKind.FLAT,    new double[]{14, 16, 18, 19});
        putSubStat_5("max_hp",            TeyvatItemStat.StatKind.FLAT,    new double[]{209, 239, 269, 299});
        putSubStat_5("def",               TeyvatItemStat.StatKind.FLAT,    new double[]{16, 19, 21, 23});
        putSubStat_5("elemental_mastery", TeyvatItemStat.StatKind.FLAT,    new double[]{16, 19, 21, 23});
        putSubStat_5("atk",               TeyvatItemStat.StatKind.PERCENT, new double[]{4.1, 4.7, 5.3, 5.8});
        putSubStat_5("max_hp",            TeyvatItemStat.StatKind.PERCENT, new double[]{4.1, 4.7, 5.3, 5.8});
        putSubStat_5("def",               TeyvatItemStat.StatKind.PERCENT, new double[]{5.1, 5.8, 6.6, 7.3});
        putSubStat_5("energy_recharge",   TeyvatItemStat.StatKind.PERCENT, new double[]{4.5, 5.2, 5.8, 6.5});
        putSubStat_5("crit_rate",         TeyvatItemStat.StatKind.PERCENT, new double[]{2.7, 3.1, 3.5, 3.9});
        putSubStat_5("crit_dmg",          TeyvatItemStat.StatKind.PERCENT, new double[]{5.4, 6.2, 7.0, 7.8});

        putSubStat_4("max_hp",            TeyvatItemStat.StatKind.FLAT,    new double[]{167, 191, 215, 239});
        putSubStat_4("atk",               TeyvatItemStat.StatKind.FLAT,    new double[]{11, 12, 14, 16});
        putSubStat_4("def",               TeyvatItemStat.StatKind.FLAT,    new double[]{13, 15, 17, 19});
        putSubStat_4("elemental_mastery", TeyvatItemStat.StatKind.FLAT,    new double[]{13, 15, 17, 19});
        putSubStat_4("energy_recharge",   TeyvatItemStat.StatKind.PERCENT, new double[]{3.6, 4.1, 4.7, 5.2});
        putSubStat_4("def",               TeyvatItemStat.StatKind.PERCENT, new double[]{4.1, 4.7, 5.3, 5.8});
        putSubStat_4("max_hp",            TeyvatItemStat.StatKind.PERCENT, new double[]{3.3, 3.7, 4.2, 4.7});
        putSubStat_4("atk",               TeyvatItemStat.StatKind.PERCENT, new double[]{3.3, 3.7, 4.2, 4.7});
        putSubStat_4("crit_rate",         TeyvatItemStat.StatKind.PERCENT, new double[]{2.2, 2.5, 2.8, 3.1});
        putSubStat_4("crit_dmg",          TeyvatItemStat.StatKind.PERCENT, new double[]{4.4, 5.0, 5.6, 6.2});
    }

    private static void putMainStat(String attrKey, TeyvatItemStat.StatKind kind, double value) {
        MAIN_STAT_FULL_5.put(attrKey + "#" + kind.name(), value);
    }

    private static void putSubStat_5(String attrKey, TeyvatItemStat.StatKind kind, double[] tiers) {
        SUB_STAT_TIERS_5.put(attrKey + "#" + kind.name(), tiers);
    }

    private static void putSubStat_4(String attrKey, TeyvatItemStat.StatKind kind, double[] tiers) {
        SUB_STAT_TIERS_4.put(attrKey + "#" + kind.name(), tiers);
    }

    private static String keyOf(AttributeType attr, TeyvatItemStat.StatKind kind) {
        return attr.id().getPath() + "#" + kind.name();
    }

    private static double getRatio(int star) {
        if (star < 1 || star > 5) return 1.0;
        return RARITY_RATIO[star - 1];
    }

    private static int getMaxLevel(int star) {
        return star <= 2 ? 4 : 4 * star;
    }

    public static double getMainStatFullValue(AttributeType attr, TeyvatItemStat.StatKind kind, int star) {
        Double full5 = MAIN_STAT_FULL_5.get(keyOf(attr, kind));
        if (full5 == null) return 0;
        return full5 * getRatio(star);
    }

    @Deprecated
    public static double getMainStatBase(AttributeType attr, int star) {
        return 0;
    }

    public static double getMainStatBase(AttributeType attr, TeyvatItemStat.StatKind kind, int star) {
        double full = getMainStatFullValue(attr, kind, star);
        int maxLevel = getMaxLevel(star);
        return full / maxLevel;
    }

    public static double getMainStatGrowth(AttributeType attr, TeyvatItemStat.StatKind kind, int star) {
        double full = getMainStatFullValue(attr, kind, star);
        double base = getMainStatBase(attr, kind, star);
        int maxLevel = getMaxLevel(star);
        if (maxLevel <= 1) return 0;
        return (full - base) / (maxLevel - 1);
    }

    public static double[] getSubStatRange(AttributeType attr, TeyvatItemStat.StatKind kind, int star) {
        String key = keyOf(attr, kind);
        double[] tiers;
        if (star >= 5) {
            tiers = SUB_STAT_TIERS_5.getOrDefault(key, new double[]{0, 0, 0, 0});
        } else if (star == 4) {
            tiers = SUB_STAT_TIERS_4.getOrDefault(key, new double[]{0, 0, 0, 0});
        } else {
            double[] base5 = SUB_STAT_TIERS_5.getOrDefault(key, new double[]{0, 0, 0, 0});
            double ratio = getRatio(star) / getRatio(5);
            tiers = new double[base5.length];
            for (int i = 0; i < base5.length; i++) tiers[i] = base5[i] * ratio;
        }
        return new double[]{tiers[0], tiers[tiers.length - 1]};
    }

    public static double getSubStatTierValue(AttributeType attr, TeyvatItemStat.StatKind kind, int star, int tier) {
        String key = keyOf(attr, kind);
        double[] tiers;
        if (star >= 5) {
            tiers = SUB_STAT_TIERS_5.getOrDefault(key, new double[]{0, 0, 0, 0});
        } else if (star == 4) {
            tiers = SUB_STAT_TIERS_4.getOrDefault(key, new double[]{0, 0, 0, 0});
        } else {
            double[] base5 = SUB_STAT_TIERS_5.getOrDefault(key, new double[]{0, 0, 0, 0});
            double ratio = getRatio(star) / getRatio(5);
            tiers = new double[base5.length];
            for (int i = 0; i < base5.length; i++) tiers[i] = base5[i] * ratio;
        }
        int idx = Math.max(0, Math.min(tier - 1, tiers.length - 1));
        return tiers[idx];
    }
}