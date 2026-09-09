package com.linweiyun.genshin.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ArtifactMainStatConfig {

    //TEMP 公式：value = base + level × growth，每个配置项是 List.of(base, growth)

    // ====== 5星主词条 ======
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_MAX_HP_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_MAX_HP_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_ATK_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_ATK_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_DEF_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_PHYSICAL_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_ER_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_EM_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_CR_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_CDG_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_HB_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_PYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_HYDRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_CYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_ELECTRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_ANEMO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_GEO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_5_DENDRO_PERCENT;

    // ====== 4星主词条 ======
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_MAX_HP_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_ATK_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_MAX_HP_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_ATK_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_DEF_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_PHYSICAL_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_ER_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_EM_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_CR_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_CDG_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_HB_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_PYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_HYDRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_CYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_ELECTRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_ANEMO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_GEO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_4_DENDRO_PERCENT;

    // ====== 3星主词条（TEMP 暂时和4星一样）======
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_MAX_HP_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_ATK_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_MAX_HP_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_ATK_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_DEF_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_PHYSICAL_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_ER_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_EM_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_CR_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_CDG_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_HB_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_PYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_HYDRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_CYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_ELECTRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_ANEMO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_GEO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAIN_3_DENDRO_PERCENT;

    // ====== 主词条抽取权重 (时之沙/空之杯/理之冠) ======
    public static ModConfigSpec.ConfigValue<String> WEIGHT_SANDS_HP_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_SANDS_ATK_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_SANDS_DEF_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_SANDS_EM_FLAT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_SANDS_ER_PERCENT;

    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_HP_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_ATK_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_DEF_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_PYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_HYDRO_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_CYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_ELECTRO_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_ANEMO_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_GEO_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_DENDRO_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_PHYSICAL_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_GOBLET_EM_FLAT;

    public static ModConfigSpec.ConfigValue<String> WEIGHT_CIRCLET_HP_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_CIRCLET_ATK_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_CIRCLET_DEF_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_CIRCLET_CR_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_CIRCLET_CDG_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_CIRCLET_HB_PERCENT;
    public static ModConfigSpec.ConfigValue<String> WEIGHT_CIRCLET_EM_FLAT;

    //TEMP 由 ArtifactConfig 主入口在共享 builder 上调用
    public static void register(ModConfigSpec.Builder builder) {
        builder.push("mainStat");

        // --- 5星 ---
        builder.push("5star");

        MAIN_5_MAX_HP_FLAT = builder.defineList(
                List.of("max-hp", "flat"),
                () -> List.of("717.0", "203.15"),
                null, obj -> obj instanceof String);
        MAIN_5_MAX_HP_PERCENT = builder.defineList(
                List.of("max-hp", "percent"),
                () -> List.of("0.07", "0.0198"),
                null, obj -> obj instanceof String);
        MAIN_5_ATK_FLAT = builder.defineList(
                List.of("atk", "flat"),
                () -> List.of("47.0", "13.2"),
                null, obj -> obj instanceof String);
        MAIN_5_ATK_PERCENT = builder.defineList(
                List.of("atk", "percent"),
                () -> List.of("0.07", "0.0198"),
                null, obj -> obj instanceof String);
        MAIN_5_DEF_PERCENT = builder.defineList(
                List.of("def", "percent"),
                () -> List.of("0.087", "0.0248"),
                null, obj -> obj instanceof String);
        MAIN_5_EM_FLAT = builder.defineList(
                List.of("elemental-mastery", "flat"),
                () -> List.of("28.0", "7.925"),
                null, obj -> obj instanceof String);
        MAIN_5_CR_PERCENT = builder.defineList(
                List.of("crit-rate", "percent"),
                () -> List.of("0.047", "0.013"),
                null, obj -> obj instanceof String);
        MAIN_5_CDG_PERCENT = builder.defineList(
                List.of("crit-dmg", "percent"),
                () -> List.of("0.093", "0.02645"),
                null, obj -> obj instanceof String);
        MAIN_5_HB_PERCENT = builder.defineList(
                List.of("healing-bonus", "percent"),
                () -> List.of("0.054", "0.01525"),
                null, obj -> obj instanceof String);
        MAIN_5_ER_PERCENT = builder.defineList(
                List.of("energy-recharge", "percent"),
                () -> List.of("0.078", "0.022"),
                null, obj -> obj instanceof String);
        MAIN_5_PYRO_PERCENT = builder.defineList(
                List.of("pyro-bonus", "percent"),
                () -> List.of("0.07", "0.0198"),
                null, obj -> obj instanceof String);
        MAIN_5_HYDRO_PERCENT = builder.defineList(
                List.of("hydro-bonus", "percent"),
                () -> List.of("0.07", "0.0198"),
                null, obj -> obj instanceof String);
        MAIN_5_DENDRO_PERCENT = builder.defineList(
                List.of("dendro-bonus", "percent"),
                () -> List.of("0.07", "0.0198"),
                null, obj -> obj instanceof String);
        MAIN_5_ELECTRO_PERCENT = builder.defineList(
                List.of("electro-bonus", "percent"),
                () -> List.of("0.07", "0.0198"),
                null, obj -> obj instanceof String);
        MAIN_5_ANEMO_PERCENT = builder.defineList(
                List.of("anemo-bonus", "percent"),
                () -> List.of("0.07", "0.0198"),
                null, obj -> obj instanceof String);
        MAIN_5_CYRO_PERCENT = builder.defineList(
                List.of("cyro-bonus", "percent"),
                () -> List.of("0.07", "0.0198"),
                null, obj -> obj instanceof String);
        MAIN_5_GEO_PERCENT = builder.defineList(
                List.of("geo-bonus", "percent"),
                () -> List.of("0.07", "0.0198"),
                null, obj -> obj instanceof String);
        MAIN_5_PHYSICAL_PERCENT = builder.defineList(
                List.of("physical-bonus", "percent"),
                () -> List.of("0.087", "0.0248"),
                null, obj -> obj instanceof String);
        builder.pop();

        // --- 4星 ---
        builder.push("4star");

        MAIN_4_MAX_HP_FLAT = builder.defineList(
                List.of("max-hp", "flat"),
                () -> List.of("645.0", "182.875"),
                null, obj -> obj instanceof String);
        MAIN_4_ATK_FLAT = builder.defineList(
                List.of("atk", "flat"),
                () -> List.of("42.0", "11.875"),
                null, obj -> obj instanceof String);
        MAIN_4_MAX_HP_PERCENT = builder.defineList(
                List.of("max-hp", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_4_ATK_PERCENT = builder.defineList(
                List.of("atk", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_4_DEF_PERCENT = builder.defineList(
                List.of("def", "percent"),
                () -> List.of("0.079", "0.02225"),
                null, obj -> obj instanceof String);
        MAIN_4_PHYSICAL_PERCENT = builder.defineList(
                List.of("physical-bonus", "percent"),
                () -> List.of("0.079", "0.02225"),
                null, obj -> obj instanceof String);
        MAIN_4_ER_PERCENT = builder.defineList(
                List.of("energy-recharge", "percent"),
                () -> List.of("0.07", "0.0198125"),
                null, obj -> obj instanceof String);
        MAIN_4_EM_FLAT = builder.defineList(
                List.of("elemental-mastery", "flat"),
                () -> List.of("25.2", "7.13125"),
                null, obj -> obj instanceof String);
        MAIN_4_CR_PERCENT = builder.defineList(
                List.of("crit-rate", "percent"),
                () -> List.of("0.042", "0.011875"),
                null, obj -> obj instanceof String);
        MAIN_4_CDG_PERCENT = builder.defineList(
                List.of("crit-dmg", "percent"),
                () -> List.of("0.084", "0.02375"),
                null, obj -> obj instanceof String);
        MAIN_4_HB_PERCENT = builder.defineList(
                List.of("healing-bonus", "percent"),
                () -> List.of("0.048", "0.01375"),
                null, obj -> obj instanceof String);
        MAIN_4_PYRO_PERCENT = builder.defineList(
                List.of("pyro-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_4_HYDRO_PERCENT = builder.defineList(
                List.of("hydro-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_4_CYRO_PERCENT = builder.defineList(
                List.of("cyro-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_4_ELECTRO_PERCENT = builder.defineList(
                List.of("electro-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_4_ANEMO_PERCENT = builder.defineList(
                List.of("anemo-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_4_GEO_PERCENT = builder.defineList(
                List.of("geo-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_4_DENDRO_PERCENT = builder.defineList(
                List.of("dendro-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        builder.pop();

        // --- 3星（TEMP 暂时和4星一样）---
        builder.push("3star");

        MAIN_3_MAX_HP_FLAT = builder.defineList(
                List.of("max-hp", "flat"),
                () -> List.of("645.0", "182.875"),
                null, obj -> obj instanceof String);
        MAIN_3_ATK_FLAT = builder.defineList(
                List.of("atk", "flat"),
                () -> List.of("42.0", "11.875"),
                null, obj -> obj instanceof String);
        MAIN_3_MAX_HP_PERCENT = builder.defineList(
                List.of("max-hp", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_3_ATK_PERCENT = builder.defineList(
                List.of("atk", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_3_DEF_PERCENT = builder.defineList(
                List.of("def", "percent"),
                () -> List.of("0.079", "0.02225"),
                null, obj -> obj instanceof String);
        MAIN_3_PHYSICAL_PERCENT = builder.defineList(
                List.of("physical-bonus", "percent"),
                () -> List.of("0.079", "0.02225"),
                null, obj -> obj instanceof String);
        MAIN_3_ER_PERCENT = builder.defineList(
                List.of("energy-recharge", "percent"),
                () -> List.of("0.07", "0.0198125"),
                null, obj -> obj instanceof String);
        MAIN_3_EM_FLAT = builder.defineList(
                List.of("elemental-mastery", "flat"),
                () -> List.of("25.2", "7.13125"),
                null, obj -> obj instanceof String);
        MAIN_3_CR_PERCENT = builder.defineList(
                List.of("crit-rate", "percent"),
                () -> List.of("0.042", "0.011875"),
                null, obj -> obj instanceof String);
        MAIN_3_CDG_PERCENT = builder.defineList(
                List.of("crit-dmg", "percent"),
                () -> List.of("0.084", "0.02375"),
                null, obj -> obj instanceof String);
        MAIN_3_HB_PERCENT = builder.defineList(
                List.of("healing-bonus", "percent"),
                () -> List.of("0.048", "0.01375"),
                null, obj -> obj instanceof String);
        MAIN_3_PYRO_PERCENT = builder.defineList(
                List.of("pyro-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_3_HYDRO_PERCENT = builder.defineList(
                List.of("hydro-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_3_CYRO_PERCENT = builder.defineList(
                List.of("cyro-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_3_ELECTRO_PERCENT = builder.defineList(
                List.of("electro-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_3_ANEMO_PERCENT = builder.defineList(
                List.of("anemo-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_3_GEO_PERCENT = builder.defineList(
                List.of("geo-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        MAIN_3_DENDRO_PERCENT = builder.defineList(
                List.of("dendro-bonus", "percent"),
                () -> List.of("0.063", "0.0178125"),
                null, obj -> obj instanceof String);
        builder.pop();

        // ====== 主词条抽取权重 (时之沙/空之杯/理之冠) ======
        builder.push("mainStatWeight");

        // --- 时之沙 ---
        builder.push("weightSands");
        WEIGHT_SANDS_HP_PERCENT = builder.define("max-hp", "26.68");
        WEIGHT_SANDS_ATK_PERCENT = builder.define("atk", "26.66");
        WEIGHT_SANDS_DEF_PERCENT = builder.define("def", "26.66");
        WEIGHT_SANDS_EM_FLAT = builder.define("elemental-mastery", "10.0");
        WEIGHT_SANDS_ER_PERCENT = builder.define("energy-recharge", "10.0");
        builder.pop();

        // --- 空之杯 ---
        builder.push("weightGoblet");
        WEIGHT_GOBLET_HP_PERCENT = builder.define("max-hp", "21.25");
        WEIGHT_GOBLET_ATK_PERCENT = builder.define("atk", "21.25");
        WEIGHT_GOBLET_DEF_PERCENT = builder.define("def", "20.0");
        WEIGHT_GOBLET_PYRO_PERCENT = builder.define("pyro-bonus", "5.0");
        WEIGHT_GOBLET_HYDRO_PERCENT = builder.define("hydro-bonus", "5.0");
        WEIGHT_GOBLET_CYRO_PERCENT = builder.define("cyro-bonus", "5.0");
        WEIGHT_GOBLET_ELECTRO_PERCENT = builder.define("electro-bonus", "5.0");
        WEIGHT_GOBLET_ANEMO_PERCENT = builder.define("anemo-bonus", "5.0");
        WEIGHT_GOBLET_GEO_PERCENT = builder.define("geo-bonus", "5.0");
        WEIGHT_GOBLET_DENDRO_PERCENT = builder.define("dendro-bonus", "5.0");
        WEIGHT_GOBLET_PHYSICAL_PERCENT = builder.define("physical-bonus", "5.0");
        WEIGHT_GOBLET_EM_FLAT = builder.define("elemental-mastery", "2.5");
        builder.pop();

        // --- 理之冠 ---
        builder.push("weightCirclet");
        WEIGHT_CIRCLET_HP_PERCENT = builder.define("max-hp", "22.0");
        WEIGHT_CIRCLET_ATK_PERCENT = builder.define("atk", "22.0");
        WEIGHT_CIRCLET_DEF_PERCENT = builder.define("def", "22.0");
        WEIGHT_CIRCLET_CR_PERCENT = builder.define("crit-rate", "10.0");
        WEIGHT_CIRCLET_CDG_PERCENT = builder.define("crit-dmg", "10.0");
        WEIGHT_CIRCLET_HB_PERCENT = builder.define("healing-bonus", "10.0");
        WEIGHT_CIRCLET_EM_FLAT = builder.define("elemental-mastery", "4.0");
        builder.pop();

        builder.pop(); // mainStatWeight pop

        builder.pop(); // mainStat pop
    }
}