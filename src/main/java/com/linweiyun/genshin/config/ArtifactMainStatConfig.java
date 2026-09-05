package com.linweiyun.genshin.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ArtifactMainStatConfig {

    //TEMP 公式：value = base + level × growth，每个配置项是 List.of(base, growth)

    // ====== 5星主词条 ======
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_MAX_HP_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_ATK_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_MAX_HP_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_ATK_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_DEF_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_PHYSICAL_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_ER_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_EM_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_CR_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_CDG_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_HB_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_PYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_HYDRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_CYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_ELECTRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_ANEMO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_GEO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_5_DENDRO_PERCENT;

    // ====== 4星主词条 ======
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_MAX_HP_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_ATK_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_MAX_HP_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_ATK_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_DEF_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_PHYSICAL_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_ER_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_EM_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_CR_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_CDG_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_HB_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_PYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_HYDRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_CYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_ELECTRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_ANEMO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_GEO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_4_DENDRO_PERCENT;

    // ====== 3星主词条（TEMP 暂时和4星一样）======
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_MAX_HP_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_ATK_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_MAX_HP_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_ATK_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_DEF_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_PHYSICAL_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_ER_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_EM_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_CR_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_CDG_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_HB_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_PYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_HYDRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_CYRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_ELECTRO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_ANEMO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_GEO_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> MAIN_3_DENDRO_PERCENT;

    //TEMP 由 ArtifactConfig 主入口在共享 builder 上调用
    public static void register(ModConfigSpec.Builder builder) {
        builder
                .push("mainStat")
                .translation("config.genshin.artifact.mainStat");

        // --- 5星 ---
        builder.push("5star").translation("config.genshin.artifact.mainStat.5star");

        MAIN_5_MAX_HP_FLAT = builder.defineList(
                List.of("max_hp", "flat"),
                () -> List.of(717.0, 203.15),
                null, obj -> obj instanceof Number);
        MAIN_5_ATK_FLAT = builder.defineList(
                List.of("atk", "flat"),
                () -> List.of(47.0, 13.2),
                null, obj -> obj instanceof Number);
        MAIN_5_MAX_HP_PERCENT = builder.defineList(
                List.of("max_hp", "percent"),
                () -> List.of(7.0, 1.98),
                null, obj -> obj instanceof Number);
        MAIN_5_ATK_PERCENT = builder.defineList(
                List.of("atk", "percent"),
                () -> List.of(7.0, 1.98),
                null, obj -> obj instanceof Number);
        MAIN_5_DEF_PERCENT = builder.defineList(
                List.of("def", "percent"),
                () -> List.of(8.7, 2.48),
                null, obj -> obj instanceof Number);
        MAIN_5_PHYSICAL_PERCENT = builder.defineList(
                List.of("physical_bonus", "percent"),
                () -> List.of(8.7, 2.48),
                null, obj -> obj instanceof Number);
        MAIN_5_ER_PERCENT = builder.defineList(
                List.of("energy_recharge", "percent"),
                () -> List.of(7.8, 2.2),
                null, obj -> obj instanceof Number);
        MAIN_5_EM_FLAT = builder.defineList(
                List.of("elemental_mastery", "flat"),
                () -> List.of(28.0, 7.925),
                null, obj -> obj instanceof Number);
        MAIN_5_CR_PERCENT = builder.defineList(
                List.of("crit_rate", "percent"),
                () -> List.of(4.7, 1.32),
                null, obj -> obj instanceof Number);
        MAIN_5_CDG_PERCENT = builder.defineList(
                List.of("crit_dmg", "percent"),
                () -> List.of(9.3, 2.645),
                null, obj -> obj instanceof Number);
        MAIN_5_HB_PERCENT = builder.defineList(
                List.of("healing_bonus", "percent"),
                () -> List.of(5.4, 1.525),
                null, obj -> obj instanceof Number);
        MAIN_5_PYRO_PERCENT = builder.defineList(
                List.of("pyro_bonus", "percent"),
                () -> List.of(7.0, 1.98),
                null, obj -> obj instanceof Number);
        MAIN_5_HYDRO_PERCENT = builder.defineList(
                List.of("hydro_bonus", "percent"),
                () -> List.of(7.0, 1.98),
                null, obj -> obj instanceof Number);
        MAIN_5_CYRO_PERCENT = builder.defineList(
                List.of("cyro_bonus", "percent"),
                () -> List.of(7.0, 1.98),
                null, obj -> obj instanceof Number);
        MAIN_5_ELECTRO_PERCENT = builder.defineList(
                List.of("electro_bonus", "percent"),
                () -> List.of(7.0, 1.98),
                null, obj -> obj instanceof Number);
        MAIN_5_ANEMO_PERCENT = builder.defineList(
                List.of("anemo_bonus", "percent"),
                () -> List.of(7.0, 1.98),
                null, obj -> obj instanceof Number);
        MAIN_5_GEO_PERCENT = builder.defineList(
                List.of("geo_bonus", "percent"),
                () -> List.of(7.0, 1.98),
                null, obj -> obj instanceof Number);
        MAIN_5_DENDRO_PERCENT = builder.defineList(
                List.of("dendro_bonus", "percent"),
                () -> List.of(7.0, 1.98),
                null, obj -> obj instanceof Number);
        builder.pop();

        // --- 4星 ---
        builder.push("4star").translation("config.genshin.artifact.mainStat.4star");

        MAIN_4_MAX_HP_FLAT = builder.defineList(
                List.of("max_hp", "flat"),
                () -> List.of(645.0, 182.875),
                null, obj -> obj instanceof Number);
        MAIN_4_ATK_FLAT = builder.defineList(
                List.of("atk", "flat"),
                () -> List.of(42.0, 11.875),
                null, obj -> obj instanceof Number);
        MAIN_4_MAX_HP_PERCENT = builder.defineList(
                List.of("max_hp", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_4_ATK_PERCENT = builder.defineList(
                List.of("atk", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_4_DEF_PERCENT = builder.defineList(
                List.of("def", "percent"),
                () -> List.of(7.9, 2.225),
                null, obj -> obj instanceof Number);
        MAIN_4_PHYSICAL_PERCENT = builder.defineList(
                List.of("physical_bonus", "percent"),
                () -> List.of(7.9, 2.225),
                null, obj -> obj instanceof Number);
        MAIN_4_ER_PERCENT = builder.defineList(
                List.of("energy_recharge", "percent"),
                () -> List.of(7.0, 1.98125),
                null, obj -> obj instanceof Number);
        MAIN_4_EM_FLAT = builder.defineList(
                List.of("elemental_mastery", "flat"),
                () -> List.of(25.2, 7.13125),
                null, obj -> obj instanceof Number);
        MAIN_4_CR_PERCENT = builder.defineList(
                List.of("crit_rate", "percent"),
                () -> List.of(4.2, 1.1875),
                null, obj -> obj instanceof Number);
        MAIN_4_CDG_PERCENT = builder.defineList(
                List.of("crit_dmg", "percent"),
                () -> List.of(8.4, 2.375),
                null, obj -> obj instanceof Number);
        MAIN_4_HB_PERCENT = builder.defineList(
                List.of("healing_bonus", "percent"),
                () -> List.of(4.8, 1.375),
                null, obj -> obj instanceof Number);
        MAIN_4_PYRO_PERCENT = builder.defineList(
                List.of("pyro_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_4_HYDRO_PERCENT = builder.defineList(
                List.of("hydro_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_4_CYRO_PERCENT = builder.defineList(
                List.of("cyro_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_4_ELECTRO_PERCENT = builder.defineList(
                List.of("electro_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_4_ANEMO_PERCENT = builder.defineList(
                List.of("anemo_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_4_GEO_PERCENT = builder.defineList(
                List.of("geo_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_4_DENDRO_PERCENT = builder.defineList(
                List.of("dendro_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        builder.pop();

        // --- 3星（TEMP 暂时和4星一样）---
        builder.push("3star").translation("config.genshin.artifact.mainStat.3star");

        MAIN_3_MAX_HP_FLAT = builder.defineList(
                List.of("max_hp", "flat"),
                () -> List.of(645.0, 182.875),
                null, obj -> obj instanceof Number);
        MAIN_3_ATK_FLAT = builder.defineList(
                List.of("atk", "flat"),
                () -> List.of(42.0, 11.875),
                null, obj -> obj instanceof Number);
        MAIN_3_MAX_HP_PERCENT = builder.defineList(
                List.of("max_hp", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_3_ATK_PERCENT = builder.defineList(
                List.of("atk", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_3_DEF_PERCENT = builder.defineList(
                List.of("def", "percent"),
                () -> List.of(7.9, 2.225),
                null, obj -> obj instanceof Number);
        MAIN_3_PHYSICAL_PERCENT = builder.defineList(
                List.of("physical_bonus", "percent"),
                () -> List.of(7.9, 2.225),
                null, obj -> obj instanceof Number);
        MAIN_3_ER_PERCENT = builder.defineList(
                List.of("energy_recharge", "percent"),
                () -> List.of(7.0, 1.98125),
                null, obj -> obj instanceof Number);
        MAIN_3_EM_FLAT = builder.defineList(
                List.of("elemental_mastery", "flat"),
                () -> List.of(25.2, 7.13125),
                null, obj -> obj instanceof Number);
        MAIN_3_CR_PERCENT = builder.defineList(
                List.of("crit_rate", "percent"),
                () -> List.of(4.2, 1.1875),
                null, obj -> obj instanceof Number);
        MAIN_3_CDG_PERCENT = builder.defineList(
                List.of("crit_dmg", "percent"),
                () -> List.of(8.4, 2.375),
                null, obj -> obj instanceof Number);
        MAIN_3_HB_PERCENT = builder.defineList(
                List.of("healing_bonus", "percent"),
                () -> List.of(4.8, 1.375),
                null, obj -> obj instanceof Number);
        MAIN_3_PYRO_PERCENT = builder.defineList(
                List.of("pyro_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_3_HYDRO_PERCENT = builder.defineList(
                List.of("hydro_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_3_CYRO_PERCENT = builder.defineList(
                List.of("cyro_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_3_ELECTRO_PERCENT = builder.defineList(
                List.of("electro_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_3_ANEMO_PERCENT = builder.defineList(
                List.of("anemo_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_3_GEO_PERCENT = builder.defineList(
                List.of("geo_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        MAIN_3_DENDRO_PERCENT = builder.defineList(
                List.of("dendro_bonus", "percent"),
                () -> List.of(6.3, 1.78125),
                null, obj -> obj instanceof Number);
        builder.pop();

        builder.pop(); // mainStat pop
    }
}