package com.linweiyun.genshin.content.items.artifact.stat;

import com.linweiyun.genshin.config.artifact.ArtifactMainStatConfig;
import com.linweiyun.genshin.config.artifact.ArtifactSubStatConfig;
import com.linweiyun.genshin.content.attribute.AttributeType;
import com.linweiyun.genshin.content.items.artifact.ArtifactLevelData;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;

import java.util.HashMap;
import java.util.Map;

public class ArtifactStatData {

    //TEMP 主词条数据：key = star#attrKey#kind，value = double[2] {base, growth}
    //TEMP 公式：value = base + level × growth
    private static final Map<String, double[]> MAIN_STAT = new HashMap<>();
    private static volatile boolean loaded = false;

    //TEMP 从配置文件读取，支持热重载
    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        reloadFromConfig();
    }

    //TEMP 从 ArtifactMainStatConfig 读取所有主词条数据
    public static void reloadFromConfig() {
        MAIN_STAT.clear();

        putMainStat("5#max_hp#FLAT",              ArtifactMainStatConfig.HP_5_FLAT_BASE.get(), ArtifactMainStatConfig.HP_5_FLAT_PER_LEVEL.get());
        putMainStat("5#atk#FLAT",                ArtifactMainStatConfig.ATK_5_FLAT_BASE.get(), ArtifactMainStatConfig.ATK_5_FLAT_PER_LEVEL.get());
        putMainStat("5#max_hp#PERCENT",           ArtifactMainStatConfig.HP_5_PERCENT_BASE.get(), ArtifactMainStatConfig.HP_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#atk#PERCENT",              ArtifactMainStatConfig.ATK_5_PERCENT_BASE.get(), ArtifactMainStatConfig.ATK_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#def#PERCENT",              ArtifactMainStatConfig.DEF_5_PERCENT_BASE.get(), ArtifactMainStatConfig.DEF_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#physical_bonus#PERCENT",   ArtifactMainStatConfig.PHYSICAL_5_PERCENT_BASE.get(), ArtifactMainStatConfig.PHYSICAL_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#energy_recharge#PERCENT",  ArtifactMainStatConfig.ER_5_PERCENT_BASE.get(), ArtifactMainStatConfig.ER_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#elemental_mastery#FLAT",   ArtifactMainStatConfig.EM_5_FLAT_BASE.get(), ArtifactMainStatConfig.EM_5_FLAT_PER_LEVEL.get());
        putMainStat("5#crit_rate#PERCENT",        ArtifactMainStatConfig.CR_5_PERCENT_BASE.get(), ArtifactMainStatConfig.CR_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#crit_dmg#PERCENT",         ArtifactMainStatConfig.CD_5_PERCENT_BASE.get(), ArtifactMainStatConfig.CD_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#healing_bonus#PERCENT",    ArtifactMainStatConfig.HEAL_5_PERCENT_BASE.get(), ArtifactMainStatConfig.HEAL_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#pyro_bonus#PERCENT",       ArtifactMainStatConfig.PYRO_5_PERCENT_BASE.get(), ArtifactMainStatConfig.PYRO_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#hydro_bonus#PERCENT",      ArtifactMainStatConfig.HYDRO_5_PERCENT_BASE.get(), ArtifactMainStatConfig.HYDRO_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#cyro_bonus#PERCENT",       ArtifactMainStatConfig.CYRO_5_PERCENT_BASE.get(), ArtifactMainStatConfig.CYRO_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#electro_bonus#PERCENT",    ArtifactMainStatConfig.ELECTRO_5_PERCENT_BASE.get(), ArtifactMainStatConfig.ELECTRO_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#anemo_bonus#PERCENT",      ArtifactMainStatConfig.ANEMO_5_PERCENT_BASE.get(), ArtifactMainStatConfig.ANEMO_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#geo_bonus#PERCENT",        ArtifactMainStatConfig.GEO_5_PERCENT_BASE.get(), ArtifactMainStatConfig.GEO_5_PERCENT_PER_LEVEL.get());
        putMainStat("5#dendro_bonus#PERCENT",     ArtifactMainStatConfig.DENDRO_5_PERCENT_BASE.get(), ArtifactMainStatConfig.DENDRO_5_PERCENT_PER_LEVEL.get());

        putMainStat("4#max_hp#FLAT",              ArtifactMainStatConfig.HP_4_FLAT_BASE.get(), ArtifactMainStatConfig.HP_4_FLAT_PER_LEVEL.get());
        putMainStat("4#atk#FLAT",                ArtifactMainStatConfig.ATK_4_FLAT_BASE.get(), ArtifactMainStatConfig.ATK_4_FLAT_PER_LEVEL.get());
        putMainStat("4#max_hp#PERCENT",           ArtifactMainStatConfig.HP_4_PERCENT_BASE.get(), ArtifactMainStatConfig.HP_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#atk#PERCENT",              ArtifactMainStatConfig.ATK_4_PERCENT_BASE.get(), ArtifactMainStatConfig.ATK_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#def#PERCENT",              ArtifactMainStatConfig.DEF_4_PERCENT_BASE.get(), ArtifactMainStatConfig.DEF_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#physical_bonus#PERCENT",   ArtifactMainStatConfig.PHYSICAL_4_PERCENT_BASE.get(), ArtifactMainStatConfig.PHYSICAL_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#energy_recharge#PERCENT",  ArtifactMainStatConfig.ER_4_PERCENT_BASE.get(), ArtifactMainStatConfig.ER_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#elemental_mastery#FLAT",   ArtifactMainStatConfig.EM_4_FLAT_BASE.get(), ArtifactMainStatConfig.EM_4_FLAT_PER_LEVEL.get());
        putMainStat("4#crit_rate#PERCENT",        ArtifactMainStatConfig.CR_4_PERCENT_BASE.get(), ArtifactMainStatConfig.CR_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#crit_dmg#PERCENT",         ArtifactMainStatConfig.CD_4_PERCENT_BASE.get(), ArtifactMainStatConfig.CD_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#healing_bonus#PERCENT",    ArtifactMainStatConfig.HEAL_4_PERCENT_BASE.get(), ArtifactMainStatConfig.HEAL_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#pyro_bonus#PERCENT",       ArtifactMainStatConfig.PYRO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.PYRO_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#hydro_bonus#PERCENT",      ArtifactMainStatConfig.HYDRO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.HYDRO_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#cyro_bonus#PERCENT",       ArtifactMainStatConfig.CYRO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.CYRO_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#electro_bonus#PERCENT",    ArtifactMainStatConfig.ELECTRO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.ELECTRO_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#anemo_bonus#PERCENT",      ArtifactMainStatConfig.ANEMO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.ANEMO_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#geo_bonus#PERCENT",        ArtifactMainStatConfig.GEO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.GEO_4_PERCENT_PER_LEVEL.get());
        putMainStat("4#dendro_bonus#PERCENT",     ArtifactMainStatConfig.DENDRO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.DENDRO_4_PERCENT_PER_LEVEL.get());

        double scale3 = 0.8;
        putMainStat("3#max_hp#FLAT",              ArtifactMainStatConfig.HP_4_FLAT_BASE.get() * scale3, ArtifactMainStatConfig.HP_4_FLAT_PER_LEVEL.get() * scale3);
        putMainStat("3#atk#FLAT",                ArtifactMainStatConfig.ATK_4_FLAT_BASE.get() * scale3, ArtifactMainStatConfig.ATK_4_FLAT_PER_LEVEL.get() * scale3);
        putMainStat("3#max_hp#PERCENT",           ArtifactMainStatConfig.HP_4_PERCENT_BASE.get(), ArtifactMainStatConfig.HP_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#atk#PERCENT",              ArtifactMainStatConfig.ATK_4_PERCENT_BASE.get(), ArtifactMainStatConfig.ATK_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#def#PERCENT",              ArtifactMainStatConfig.DEF_4_PERCENT_BASE.get(), ArtifactMainStatConfig.DEF_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#physical_bonus#PERCENT",   ArtifactMainStatConfig.PHYSICAL_4_PERCENT_BASE.get(), ArtifactMainStatConfig.PHYSICAL_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#energy_recharge#PERCENT",  ArtifactMainStatConfig.ER_4_PERCENT_BASE.get(), ArtifactMainStatConfig.ER_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#elemental_mastery#FLAT",   ArtifactMainStatConfig.EM_4_FLAT_BASE.get(), ArtifactMainStatConfig.EM_4_FLAT_PER_LEVEL.get());
        putMainStat("3#crit_rate#PERCENT",        ArtifactMainStatConfig.CR_4_PERCENT_BASE.get(), ArtifactMainStatConfig.CR_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#crit_dmg#PERCENT",         ArtifactMainStatConfig.CD_4_PERCENT_BASE.get(), ArtifactMainStatConfig.CD_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#healing_bonus#PERCENT",    ArtifactMainStatConfig.HEAL_4_PERCENT_BASE.get(), ArtifactMainStatConfig.HEAL_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#pyro_bonus#PERCENT",       ArtifactMainStatConfig.PYRO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.PYRO_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#hydro_bonus#PERCENT",      ArtifactMainStatConfig.HYDRO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.HYDRO_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#cyro_bonus#PERCENT",       ArtifactMainStatConfig.CYRO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.CYRO_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#electro_bonus#PERCENT",    ArtifactMainStatConfig.ELECTRO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.ELECTRO_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#anemo_bonus#PERCENT",      ArtifactMainStatConfig.ANEMO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.ANEMO_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#geo_bonus#PERCENT",        ArtifactMainStatConfig.GEO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.GEO_4_PERCENT_PER_LEVEL.get());
        putMainStat("3#dendro_bonus#PERCENT",     ArtifactMainStatConfig.DENDRO_4_PERCENT_BASE.get(), ArtifactMainStatConfig.DENDRO_4_PERCENT_PER_LEVEL.get());

        readSubStat5();
    }

    private static void putMainStat(String key, double base, double growth) {
        MAIN_STAT.put(key, new double[]{base, growth});
    }

    private static void readSubStat5() {
        SUB_STAT_TIERS_5.clear();

        putSubStatTier("atk#FLAT",   ArtifactSubStatConfig.SUB_5_ATK_FLAT_TIER1.get(),
                ArtifactSubStatConfig.SUB_5_ATK_FLAT_TIER2.get(),
                ArtifactSubStatConfig.SUB_5_ATK_FLAT_TIER3.get(),
                ArtifactSubStatConfig.SUB_5_ATK_FLAT_TIER4.get());
        putSubStatTier("max_hp#FLAT", ArtifactSubStatConfig.SUB_5_HP_FLAT_TIER1.get(),
                ArtifactSubStatConfig.SUB_5_HP_FLAT_TIER2.get(),
                ArtifactSubStatConfig.SUB_5_HP_FLAT_TIER3.get(),
                ArtifactSubStatConfig.SUB_5_HP_FLAT_TIER4.get());
        putSubStatTier("def#FLAT",   ArtifactSubStatConfig.SUB_5_DEF_FLAT_TIER1.get(),
                ArtifactSubStatConfig.SUB_5_DEF_FLAT_TIER2.get(),
                ArtifactSubStatConfig.SUB_5_DEF_FLAT_TIER3.get(),
                ArtifactSubStatConfig.SUB_5_DEF_FLAT_TIER4.get());
        putSubStatTier("atk#PERCENT", ArtifactSubStatConfig.SUB_5_ATK_PERCENT_TIER1.get(),
                ArtifactSubStatConfig.SUB_5_ATK_PERCENT_TIER2.get(),
                ArtifactSubStatConfig.SUB_5_ATK_PERCENT_TIER3.get(),
                ArtifactSubStatConfig.SUB_5_ATK_PERCENT_TIER4.get());
        putSubStatTier("max_hp#PERCENT", ArtifactSubStatConfig.SUB_5_HP_PERCENT_TIER1.get(),
                ArtifactSubStatConfig.SUB_5_HP_PERCENT_TIER2.get(),
                ArtifactSubStatConfig.SUB_5_HP_PERCENT_TIER3.get(),
                ArtifactSubStatConfig.SUB_5_HP_PERCENT_TIER4.get());
        putSubStatTier("def#PERCENT", ArtifactSubStatConfig.SUB_5_DEF_PERCENT_TIER1.get(),
                ArtifactSubStatConfig.SUB_5_DEF_PERCENT_TIER2.get(),
                ArtifactSubStatConfig.SUB_5_DEF_PERCENT_TIER3.get(),
                ArtifactSubStatConfig.SUB_5_DEF_PERCENT_TIER4.get());
        putSubStatTier("elemental_mastery#FLAT", ArtifactSubStatConfig.SUB_5_EM_TIER1.get(),
                ArtifactSubStatConfig.SUB_5_EM_TIER2.get(),
                ArtifactSubStatConfig.SUB_5_EM_TIER3.get(),
                ArtifactSubStatConfig.SUB_5_EM_TIER4.get());
        putSubStatTier("energy_recharge#PERCENT", ArtifactSubStatConfig.SUB_5_ER_TIER1.get(),
                ArtifactSubStatConfig.SUB_5_ER_TIER2.get(),
                ArtifactSubStatConfig.SUB_5_ER_TIER3.get(),
                ArtifactSubStatConfig.SUB_5_ER_TIER4.get());
        putSubStatTier("crit_rate#PERCENT", ArtifactSubStatConfig.SUB_5_CRIT_RATE_TIER1.get(),
                ArtifactSubStatConfig.SUB_5_CRIT_RATE_TIER2.get(),
                ArtifactSubStatConfig.SUB_5_CRIT_RATE_TIER3.get(),
                ArtifactSubStatConfig.SUB_5_CRIT_RATE_TIER4.get());
        putSubStatTier("crit_dmg#PERCENT", ArtifactSubStatConfig.SUB_5_CRIT_DMG_TIER1.get(),
                ArtifactSubStatConfig.SUB_5_CRIT_DMG_TIER2.get(),
                ArtifactSubStatConfig.SUB_5_CRIT_DMG_TIER3.get(),
                ArtifactSubStatConfig.SUB_5_CRIT_DMG_TIER4.get());
    }

    private static void putSubStatTier(String key, double t1, double t2, double t3, double t4) {
        SUB_STAT_TIERS_5.put(key, new double[]{t1, t2, t3, t4});
    }

    private static void putSubStatTier(String key, int t1, int t2, int t3, int t4) {
        SUB_STAT_TIERS_5.put(key, new double[]{(double)t1, (double)t2, (double)t3, (double)t4});
    }

    private static String keyOf(int star, AttributeType attr, TeyvatItemStat.StatKind kind) {
        return star + "#" + attr.id().getPath() + "#" + kind.name();
    }

    private static int getStarBucket(int star) {
        if (star >= 5) return 5;
        if (star == 4) return 4;
        return 3;
    }

    //TEMP 新公式：value = base + level × growth
    public static double getMainStatValue(AttributeType attr, TeyvatItemStat.StatKind kind, int star, int level) {
        ensureLoaded();
        int bucket = getStarBucket(star);
        double[] pair = MAIN_STAT.get(keyOf(bucket, attr, kind));
        if (pair == null) return 0;
        double base = pair[0];
        double growth = pair[1];
        return base + level * growth;
    }

    //TEMP 1级时的值
    public static double getMainStatInitialValue(AttributeType attr, TeyvatItemStat.StatKind kind, int star) {
        return getMainStatValue(attr, kind, star, 1);
    }

    // 保留旧方法做兼容
    @Deprecated
    public static double getMainStatFullValue(AttributeType attr, TeyvatItemStat.StatKind kind, int star) {
        return getMainStatValue(attr, kind, star, ArtifactLevelData.getMaxLevel(star));
    }

    @Deprecated
    public static double getMainStatBase(AttributeType attr, int star) {
        return 0;
    }

    //TEMP 返回初始值（base）
    public static double getMainStatBase(AttributeType attr, TeyvatItemStat.StatKind kind, int star) {
        ensureLoaded();
        int bucket = getStarBucket(star);
        double[] pair = MAIN_STAT.get(keyOf(bucket, attr, kind));
        return pair != null ? pair[0] : 0;
    }

    //TEMP 返回每级系数（growth）
    public static double getMainStatGrowth(AttributeType attr, TeyvatItemStat.StatKind kind, int star) {
        ensureLoaded();
        int bucket = getStarBucket(star);
        double[] pair = MAIN_STAT.get(keyOf(bucket, attr, kind));
        return pair != null ? pair[1] : 0;
    }

    // ====== 副词条数据暂时保留不动 ======
    private static final Map<String, double[]> SUB_STAT_TIERS_5 = new HashMap<>();
    private static final Map<String, double[]> SUB_STAT_TIERS_4 = new HashMap<>();

    static {
        putSubStat_5("atk",               TeyvatItemStat.StatKind.FLAT,    new double[]{14, 16, 18, 19});
        putSubStat_5("max_hp",            TeyvatItemStat.StatKind.FLAT,    new double[]{209, 239, 269, 299});
        putSubStat_5("def",               TeyvatItemStat.StatKind.FLAT,    new double[]{16, 19, 21, 23});
        putSubStat_5("elemental_mastery", TeyvatItemStat.StatKind.FLAT,    new double[]{16, 19, 21, 23});
        putSubStat_5("atk",               TeyvatItemStat.StatKind.PERCENT, new double[]{0.041, 0.047, 0.053, 0.058});
        putSubStat_5("max_hp",            TeyvatItemStat.StatKind.PERCENT, new double[]{0.041, 0.047, 0.053, 0.058});
        putSubStat_5("def",               TeyvatItemStat.StatKind.PERCENT, new double[]{0.051, 0.058, 0.066, 0.073});
        putSubStat_5("energy_recharge",   TeyvatItemStat.StatKind.PERCENT, new double[]{0.045, 0.052, 0.058, 0.065});
        putSubStat_5("crit_rate",         TeyvatItemStat.StatKind.PERCENT, new double[]{0.027, 0.031, 0.035, 0.039});
        putSubStat_5("crit_dmg",          TeyvatItemStat.StatKind.PERCENT, new double[]{0.054, 0.062, 0.07, 0.078});

        putSubStat_4("max_hp",            TeyvatItemStat.StatKind.FLAT,    new double[]{167, 191, 215, 239});
        putSubStat_4("atk",               TeyvatItemStat.StatKind.FLAT,    new double[]{11, 12, 14, 16});
        putSubStat_4("def",               TeyvatItemStat.StatKind.FLAT,    new double[]{13, 15, 17, 19});
        putSubStat_4("elemental_mastery", TeyvatItemStat.StatKind.FLAT,    new double[]{13, 15, 17, 19});
        putSubStat_4("energy_recharge",   TeyvatItemStat.StatKind.PERCENT, new double[]{0.036, 0.041, 0.047, 0.052});
        putSubStat_4("def",               TeyvatItemStat.StatKind.PERCENT, new double[]{0.041, 0.047, 0.053, 0.058});
        putSubStat_4("max_hp",            TeyvatItemStat.StatKind.PERCENT, new double[]{0.033, 0.037, 0.042, 0.047});
        putSubStat_4("atk",               TeyvatItemStat.StatKind.PERCENT, new double[]{0.033, 0.037, 0.042, 0.047});
        putSubStat_4("crit_rate",         TeyvatItemStat.StatKind.PERCENT, new double[]{0.022, 0.025, 0.028, 0.031});
        putSubStat_4("crit_dmg",          TeyvatItemStat.StatKind.PERCENT, new double[]{0.044, 0.05, 0.056, 0.062});
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

    public static double[] getSubStatRange(AttributeType attr, TeyvatItemStat.StatKind kind, int star) {
        String key = keyOf(attr, kind);
        double[] tiers;
        if (star >= 5) {
            tiers = SUB_STAT_TIERS_5.getOrDefault(key, new double[]{0, 0, 0, 0});
        } else if (star == 4) {
            tiers = SUB_STAT_TIERS_4.getOrDefault(key, new double[]{0, 0, 0, 0});
        } else {
            double[] base5 = SUB_STAT_TIERS_5.getOrDefault(key, new double[]{0, 0, 0, 0});
            double ratio = 0.48 / 1.00;
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
            double ratio = 0.48 / 1.00;
            tiers = new double[base5.length];
            for (int i = 0; i < base5.length; i++) tiers[i] = base5[i] * ratio;
        }
        int idx = Math.max(0, Math.min(tier - 1, tiers.length - 1));
        return tiers[idx];
    }
}