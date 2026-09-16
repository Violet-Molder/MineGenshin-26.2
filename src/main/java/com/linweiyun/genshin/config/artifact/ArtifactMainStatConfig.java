package com.linweiyun.genshin.config.artifact;

import com.linweiyun.genshin.config.util.StringDoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ArtifactMainStatConfig {

    // 五星
    public static StringDoubleValue HP_5_FLAT_BASE, HP_5_FLAT_PER_LEVEL;
    public static StringDoubleValue HP_5_PERCENT_BASE, HP_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue ATK_5_FLAT_BASE, ATK_5_FLAT_PER_LEVEL;
    public static StringDoubleValue ATK_5_PERCENT_BASE, ATK_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue DEF_5_PERCENT_BASE, DEF_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue EM_5_FLAT_BASE, EM_5_FLAT_PER_LEVEL;
    public static StringDoubleValue CR_5_PERCENT_BASE, CR_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue CD_5_PERCENT_BASE, CD_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue HEAL_5_PERCENT_BASE, HEAL_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue ER_5_PERCENT_BASE, ER_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue PYRO_5_PERCENT_BASE, PYRO_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue HYDRO_5_PERCENT_BASE, HYDRO_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue DENDRO_5_PERCENT_BASE, DENDRO_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue ELECTRO_5_PERCENT_BASE, ELECTRO_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue ANEMO_5_PERCENT_BASE, ANEMO_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue CYRO_5_PERCENT_BASE, CYRO_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue GEO_5_PERCENT_BASE, GEO_5_PERCENT_PER_LEVEL;
    public static StringDoubleValue PHYSICAL_5_PERCENT_BASE, PHYSICAL_5_PERCENT_PER_LEVEL;

    public static ModConfigSpec.ConfigValue<String> WEIGHT_SANDS_HP_PERCENT, WEIGHT_SANDS_ATK_PERCENT, WEIGHT_SANDS_DEF_PERCENT, WEIGHT_SANDS_EM_FLAT, WEIGHT_SANDS_ER_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_PYRO_PERCENT, WEIGHT_GOBLET_HYDRO_PERCENT, WEIGHT_GOBLET_CYRO_PERCENT, WEIGHT_GOBLET_ELECTRO_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_ANEMO_PERCENT, WEIGHT_GOBLET_GEO_PERCENT, WEIGHT_GOBLET_DENDRO_PERCENT, WEIGHT_GOBLET_PHYSICAL_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_ATK_PERCENT, WEIGHT_GOBLET_HP_PERCENT, WEIGHT_GOBLET_DEF_PERCENT, WEIGHT_GOBLET_EM_FLAT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_CIRCLET_CR_PERCENT, WEIGHT_CIRCLET_CDG_PERCENT, WEIGHT_CIRCLET_HB_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_CIRCLET_HP_PERCENT, WEIGHT_CIRCLET_ATK_PERCENT, WEIGHT_CIRCLET_DEF_PERCENT, WEIGHT_CIRCLET_EM_FLAT;

    // 四星
    public static StringDoubleValue HP_4_FLAT_BASE, HP_4_FLAT_PER_LEVEL;
    public static StringDoubleValue HP_4_PERCENT_BASE, HP_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue ATK_4_FLAT_BASE, ATK_4_FLAT_PER_LEVEL;
    public static StringDoubleValue ATK_4_PERCENT_BASE, ATK_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue DEF_4_PERCENT_BASE, DEF_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue PHYSICAL_4_PERCENT_BASE, PHYSICAL_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue ER_4_PERCENT_BASE, ER_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue EM_4_FLAT_BASE, EM_4_FLAT_PER_LEVEL;
    public static StringDoubleValue CR_4_PERCENT_BASE, CR_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue CD_4_PERCENT_BASE, CD_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue HEAL_4_PERCENT_BASE, HEAL_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue PYRO_4_PERCENT_BASE, PYRO_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue HYDRO_4_PERCENT_BASE, HYDRO_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue CYRO_4_PERCENT_BASE, CYRO_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue ELECTRO_4_PERCENT_BASE, ELECTRO_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue ANEMO_4_PERCENT_BASE, ANEMO_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue GEO_4_PERCENT_BASE, GEO_4_PERCENT_PER_LEVEL;
    public static StringDoubleValue DENDRO_4_PERCENT_BASE, DENDRO_4_PERCENT_PER_LEVEL;

    static void register(ModConfigSpec.Builder builder) {
        builder.push("mainStat");

        builder.push("5star");

        builder.push("hp");
        HP_5_FLAT_BASE = StringDoubleValue.defineInRange(builder, "flat_base", 717.0, 0.0, 100000.0);
        HP_5_FLAT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "flat_per_level", 203.15, 0.0, 100000.0);
        HP_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.07, 0.0, 10.0);
        HP_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0198, 0.0, 10.0);
        builder.pop();

        builder.push("atk");
        ATK_5_FLAT_BASE = StringDoubleValue.defineInRange(builder, "flat_base", 47.0, 0.0, 100000.0);
        ATK_5_FLAT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "flat_per_level", 13.2, 0.0, 100000.0);
        ATK_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.07, 0.0, 10.0);
        ATK_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0198, 0.0, 10.0);
        builder.pop();

        builder.push("def");
        DEF_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.087, 0.0, 10.0);
        DEF_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0248, 0.0, 10.0);
        builder.pop();

        builder.push("elemental-mastery");
        EM_5_FLAT_BASE = StringDoubleValue.defineInRange(builder, "flat_base", 28.0, 0.0, 100000.0);
        EM_5_FLAT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "flat_per_level", 7.925, 0.0, 100000.0);
        builder.pop();

        builder.push("crit-rate");
        CR_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.047, 0.0, 10.0);
        CR_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0132, 0.0, 10.0);
        builder.pop();

        builder.push("crit-dmg");
        CD_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.093, 0.0, 10.0);
        CD_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.02645, 0.0, 10.0);
        builder.pop();

        builder.push("healing-bonus");
        HEAL_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.054, 0.0, 10.0);
        HEAL_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.01525, 0.0, 10.0);
        builder.pop();

        builder.push("energy-recharge");
        ER_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.078, 0.0, 10.0);
        ER_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.022, 0.0, 10.0);
        builder.pop();

        builder.push("pyro-bonus");
        PYRO_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.07, 0.0, 10.0);
        PYRO_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0198, 0.0, 10.0);
        builder.pop();

        builder.push("hydro-bonus");
        HYDRO_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.07, 0.0, 10.0);
        HYDRO_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0198, 0.0, 10.0);
        builder.pop();

        builder.push("dendro-bonus");
        DENDRO_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.07, 0.0, 10.0);
        DENDRO_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0198, 0.0, 10.0);
        builder.pop();

        builder.push("electro-bonus");
        ELECTRO_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.07, 0.0, 10.0);
        ELECTRO_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0198, 0.0, 10.0);
        builder.pop();

        builder.push("anemo-bonus");
        ANEMO_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.07, 0.0, 10.0);
        ANEMO_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0198, 0.0, 10.0);
        builder.pop();

        builder.push("cyro-bonus");
        CYRO_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.07, 0.0, 10.0);
        CYRO_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0198, 0.0, 10.0);
        builder.pop();

        builder.push("geo-bonus");
        GEO_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.07, 0.0, 10.0);
        GEO_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0198, 0.0, 10.0);
        builder.pop();

        builder.push("physical-bonus");
        PHYSICAL_5_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.087, 0.0, 10.0);
        PHYSICAL_5_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0248, 0.0, 10.0);
        builder.pop();

        builder.pop();

        builder.push("4star");

        builder.push("hp");
        HP_4_FLAT_BASE = StringDoubleValue.defineInRange(builder, "flat_base", 645.0, 0.0, 100000.0);
        HP_4_FLAT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "flat_per_level", 182.875, 0.0, 100000.0);
        HP_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.063, 0.0, 10.0);
        HP_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0178125, 0.0, 10.0);
        builder.pop();

        builder.push("atk");
        ATK_4_FLAT_BASE = StringDoubleValue.defineInRange(builder, "flat_base", 42.0, 0.0, 100000.0);
        ATK_4_FLAT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "flat_per_level", 11.875, 0.0, 100000.0);
        ATK_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.063, 0.0, 10.0);
        ATK_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0178125, 0.0, 10.0);
        builder.pop();

        builder.push("def");
        DEF_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.079, 0.0, 10.0);
        DEF_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.02225, 0.0, 10.0);
        builder.pop();

        builder.push("physical-bonus");
        PHYSICAL_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.079, 0.0, 10.0);
        PHYSICAL_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.02225, 0.0, 10.0);
        builder.pop();

        builder.push("energy-recharge");
        ER_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.07, 0.0, 10.0);
        ER_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0198125, 0.0, 10.0);
        builder.pop();

        builder.push("elemental-mastery");
        EM_4_FLAT_BASE = StringDoubleValue.defineInRange(builder, "flat_base", 25.2, 0.0, 100000.0);
        EM_4_FLAT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "flat_per_level", 7.13125, 0.0, 100000.0);
        builder.pop();

        builder.push("crit-rate");
        CR_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.042, 0.0, 10.0);
        CR_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.011875, 0.0, 10.0);
        builder.pop();

        builder.push("crit-dmg");
        CD_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.084, 0.0, 10.0);
        CD_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.02375, 0.0, 10.0);
        builder.pop();

        builder.push("healing-bonus");
        HEAL_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.048, 0.0, 10.0);
        HEAL_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.01375, 0.0, 10.0);
        builder.pop();

        builder.push("pyro-bonus");
        PYRO_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.063, 0.0, 10.0);
        PYRO_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0178125, 0.0, 10.0);
        builder.pop();

        builder.push("hydro-bonus");
        HYDRO_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.063, 0.0, 10.0);
        HYDRO_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0178125, 0.0, 10.0);
        builder.pop();

        builder.push("cyro-bonus");
        CYRO_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.063, 0.0, 10.0);
        CYRO_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0178125, 0.0, 10.0);
        builder.pop();

        builder.push("electro-bonus");
        ELECTRO_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.063, 0.0, 10.0);
        ELECTRO_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0178125, 0.0, 10.0);
        builder.pop();

        builder.push("anemo-bonus");
        ANEMO_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.063, 0.0, 10.0);
        ANEMO_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0178125, 0.0, 10.0);
        builder.pop();

        builder.push("geo-bonus");
        GEO_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.063, 0.0, 10.0);
        GEO_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0178125, 0.0, 10.0);
        builder.pop();

        builder.push("dendro-bonus");
        DENDRO_4_PERCENT_BASE = StringDoubleValue.defineInRange(builder, "percent_base", 0.063, 0.0, 10.0);
        DENDRO_4_PERCENT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "percent_per_level", 0.0178125, 0.0, 10.0);
        builder.pop();

        builder.pop();

        builder.push("artifact-stat-weight");
        builder.push("artifact-sands");
        WEIGHT_SANDS_HP_PERCENT = builder.define("hp", "0.25");
        WEIGHT_SANDS_ATK_PERCENT = builder.define("atk", "0.25");
        WEIGHT_SANDS_DEF_PERCENT = builder.define("def", "0.25");
        WEIGHT_SANDS_EM_FLAT = builder.define("elemental-mastery", "0.15");
        WEIGHT_SANDS_ER_PERCENT = builder.define("energy-recharge", "0.10");
        builder.pop();
        builder.push("artifact-goblet");
        WEIGHT_GOBLET_PYRO_PERCENT = builder.define("pyro-bonus", "0.07");
        WEIGHT_GOBLET_HYDRO_PERCENT = builder.define("hydro-bonus", "0.07");
        WEIGHT_GOBLET_CYRO_PERCENT = builder.define("cyro-bonus", "0.07");
        WEIGHT_GOBLET_ELECTRO_PERCENT = builder.define("electro-bonus", "0.07");
        WEIGHT_GOBLET_ANEMO_PERCENT = builder.define("anemo-bonus", "0.07");
        WEIGHT_GOBLET_GEO_PERCENT = builder.define("geo-bonus", "0.07");
        WEIGHT_GOBLET_DENDRO_PERCENT = builder.define("dendro-bonus", "0.07");
        WEIGHT_GOBLET_PHYSICAL_PERCENT = builder.define("physical-bonus", "0.07");
        WEIGHT_GOBLET_ATK_PERCENT = builder.define("atk", "0.10");
        WEIGHT_GOBLET_HP_PERCENT = builder.define("hp", "0.10");
        WEIGHT_GOBLET_DEF_PERCENT = builder.define("def", "0.10");
        WEIGHT_GOBLET_EM_FLAT = builder.define("elemental-mastery", "0.12");
        builder.pop();
        builder.push("artifact-circlet");
        WEIGHT_CIRCLET_CR_PERCENT = builder.define("crit-rate", "0.15");
        WEIGHT_CIRCLET_CDG_PERCENT = builder.define("crit-dmg", "0.15");
        WEIGHT_CIRCLET_HB_PERCENT = builder.define("healing-bonus", "0.10");
        WEIGHT_CIRCLET_HP_PERCENT = builder.define("hp", "0.15");
        WEIGHT_CIRCLET_ATK_PERCENT = builder.define("atk", "0.15");
        WEIGHT_CIRCLET_DEF_PERCENT = builder.define("def", "0.15");
        WEIGHT_CIRCLET_EM_FLAT = builder.define("elemental-mastery", "0.15");
        builder.pop();
        builder.pop();

        builder.pop();
    }
}