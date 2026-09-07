package com.linweiyun.genshin.content.items.artifact;

import com.linweiyun.genshin.config.ArtifactMainStatConfig;
import com.linweiyun.genshin.config.ArtifactSubStatConfig;
import com.linweiyun.genshin.content.attribute.AttributeType;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashMap;
import java.util.List;
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
        readStar(5, ArtifactMainStatConfig.MAIN_5_MAX_HP_FLAT, ArtifactMainStatConfig.MAIN_5_ATK_FLAT,
                ArtifactMainStatConfig.MAIN_5_MAX_HP_PERCENT, ArtifactMainStatConfig.MAIN_5_ATK_PERCENT,
                ArtifactMainStatConfig.MAIN_5_DEF_PERCENT, ArtifactMainStatConfig.MAIN_5_PHYSICAL_PERCENT,
                ArtifactMainStatConfig.MAIN_5_ER_PERCENT, ArtifactMainStatConfig.MAIN_5_EM_FLAT,
                ArtifactMainStatConfig.MAIN_5_CR_PERCENT, ArtifactMainStatConfig.MAIN_5_CDG_PERCENT,
                ArtifactMainStatConfig.MAIN_5_HB_PERCENT, ArtifactMainStatConfig.MAIN_5_PYRO_PERCENT,
                ArtifactMainStatConfig.MAIN_5_HYDRO_PERCENT, ArtifactMainStatConfig.MAIN_5_CYRO_PERCENT,
                ArtifactMainStatConfig.MAIN_5_ELECTRO_PERCENT, ArtifactMainStatConfig.MAIN_5_ANEMO_PERCENT,
                ArtifactMainStatConfig.MAIN_5_GEO_PERCENT, ArtifactMainStatConfig.MAIN_5_DENDRO_PERCENT);
        readStar(4, ArtifactMainStatConfig.MAIN_4_MAX_HP_FLAT, ArtifactMainStatConfig.MAIN_4_ATK_FLAT,
                ArtifactMainStatConfig.MAIN_4_MAX_HP_PERCENT, ArtifactMainStatConfig.MAIN_4_ATK_PERCENT,
                ArtifactMainStatConfig.MAIN_4_DEF_PERCENT, ArtifactMainStatConfig.MAIN_4_PHYSICAL_PERCENT,
                ArtifactMainStatConfig.MAIN_4_ER_PERCENT, ArtifactMainStatConfig.MAIN_4_EM_FLAT,
                ArtifactMainStatConfig.MAIN_4_CR_PERCENT, ArtifactMainStatConfig.MAIN_4_CDG_PERCENT,
                ArtifactMainStatConfig.MAIN_4_HB_PERCENT, ArtifactMainStatConfig.MAIN_4_PYRO_PERCENT,
                ArtifactMainStatConfig.MAIN_4_HYDRO_PERCENT, ArtifactMainStatConfig.MAIN_4_CYRO_PERCENT,
                ArtifactMainStatConfig.MAIN_4_ELECTRO_PERCENT, ArtifactMainStatConfig.MAIN_4_ANEMO_PERCENT,
                ArtifactMainStatConfig.MAIN_4_GEO_PERCENT, ArtifactMainStatConfig.MAIN_4_DENDRO_PERCENT);
        readStar(3, ArtifactMainStatConfig.MAIN_3_MAX_HP_FLAT, ArtifactMainStatConfig.MAIN_3_ATK_FLAT,
                ArtifactMainStatConfig.MAIN_3_MAX_HP_PERCENT, ArtifactMainStatConfig.MAIN_3_ATK_PERCENT,
                ArtifactMainStatConfig.MAIN_3_DEF_PERCENT, ArtifactMainStatConfig.MAIN_3_PHYSICAL_PERCENT,
                ArtifactMainStatConfig.MAIN_3_ER_PERCENT, ArtifactMainStatConfig.MAIN_3_EM_FLAT,
                ArtifactMainStatConfig.MAIN_3_CR_PERCENT, ArtifactMainStatConfig.MAIN_3_CDG_PERCENT,
                ArtifactMainStatConfig.MAIN_3_HB_PERCENT, ArtifactMainStatConfig.MAIN_3_PYRO_PERCENT,
                ArtifactMainStatConfig.MAIN_3_HYDRO_PERCENT, ArtifactMainStatConfig.MAIN_3_CYRO_PERCENT,
                ArtifactMainStatConfig.MAIN_3_ELECTRO_PERCENT, ArtifactMainStatConfig.MAIN_3_ANEMO_PERCENT,
                ArtifactMainStatConfig.MAIN_3_GEO_PERCENT, ArtifactMainStatConfig.MAIN_3_DENDRO_PERCENT);

        //TEMP 从 ArtifactSubStatConfig 读取五星副词条数据
        readSubStat5();
    }

    //TEMP 从 ArtifactSubStatConfig 读取五星副词条4档数值
    private static void readSubStat5() {
        SUB_STAT_TIERS_5.clear();
        tryReadSubStat("atk#FLAT", ArtifactSubStatConfig.SUB_5_ATK_FLAT);
        tryReadSubStat("max_hp#FLAT", ArtifactSubStatConfig.SUB_5_MAX_HP_FLAT);
        tryReadSubStat("def#FLAT", ArtifactSubStatConfig.SUB_5_DEF_FLAT);
        tryReadSubStat("atk#PERCENT", ArtifactSubStatConfig.SUB_5_ATK_PERCENT);
        tryReadSubStat("max_hp#PERCENT", ArtifactSubStatConfig.SUB_5_MAX_HP_PERCENT);
        tryReadSubStat("def#PERCENT", ArtifactSubStatConfig.SUB_5_DEF_PERCENT);
        tryReadSubStat("elemental_mastery#FLAT", ArtifactSubStatConfig.SUB_5_EM_FLAT);
        tryReadSubStat("energy_recharge#PERCENT", ArtifactSubStatConfig.SUB_5_ER_PERCENT);
        tryReadSubStat("crit_rate#PERCENT", ArtifactSubStatConfig.SUB_5_CR_PERCENT);
        tryReadSubStat("crit_dmg#PERCENT", ArtifactSubStatConfig.SUB_5_CDG_PERCENT);
    }

    private static void tryReadSubStat(String key, ModConfigSpec.ConfigValue<List<? extends Number>> config) {
        try {
            List<? extends Number> list = config.get();
            if (list != null && list.size() == 4) {
                SUB_STAT_TIERS_5.put(key, new double[]{
                        list.get(0).doubleValue(),
                        list.get(1).doubleValue(),
                        list.get(2).doubleValue(),
                        list.get(3).doubleValue()
                });
            }
        } catch (Exception ignored) {
        }
    }

    //TEMP 从 ConfigValue 读取 base+growth，key = "star#attrKey#kind"
    @SafeVarargs
    private static void readStar(int star, ModConfigSpec.ConfigValue<List<? extends Number>>... configs) {
        String[] attrKeys = {"max_hp#FLAT", "atk#FLAT", "max_hp#PERCENT", "atk#PERCENT",
                "def#PERCENT", "physical_bonus#PERCENT", "energy_recharge#PERCENT",
                "elemental_mastery#FLAT", "crit_rate#PERCENT", "crit_dmg#PERCENT",
                "healing_bonus#PERCENT", "pyro_bonus#PERCENT", "hydro_bonus#PERCENT",
                "cyro_bonus#PERCENT", "electro_bonus#PERCENT", "anemo_bonus#PERCENT",
                "geo_bonus#PERCENT", "dendro_bonus#PERCENT"};
        for (int i = 0; i < configs.length; i++) {
            try {
                List<? extends Number> list = configs[i].get();
                if (list.size() >= 2) {
                    String key = star + "#" + attrKeys[i];
                    MAIN_STAT.put(key, new double[]{list.get(0).doubleValue(), list.get(1).doubleValue()});
                }
            } catch (Exception ignored) {
            }
        }
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