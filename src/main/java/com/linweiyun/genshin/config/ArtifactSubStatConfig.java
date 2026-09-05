package com.linweiyun.genshin.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ArtifactSubStatConfig {

    //TEMP 五星副词条4档数值，每个配置项是 List.of(1档, 2档, 3档, 4档)
    //TEMP 每次提升副词条时，增加的值 = 当前档位值

    public static ModConfigSpec.ConfigValue<List<? extends Number>> SUB_5_ATK_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> SUB_5_MAX_HP_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> SUB_5_DEF_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> SUB_5_ATK_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> SUB_5_MAX_HP_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> SUB_5_DEF_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> SUB_5_EM_FLAT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> SUB_5_ER_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> SUB_5_CR_PERCENT;
    public static ModConfigSpec.ConfigValue<List<? extends Number>> SUB_5_CDG_PERCENT;

    //TEMP 由 ArtifactConfig 主入口在共享 builder 上调用
    public static void register(ModConfigSpec.Builder builder) {
        builder.push("subStat");

        builder.push("5star").translation("config.genshin.artifact.subStat.5star");

        SUB_5_ATK_FLAT = builder.defineList(
                List.of("atk", "flat"),
                () -> List.of(14, 16, 18, 19),
                null, obj -> obj instanceof Integer);
        SUB_5_MAX_HP_FLAT = builder.defineList(
                List.of("max_hp", "flat"),
                () -> List.of(209, 239, 269, 299),
                null, obj -> obj instanceof Integer);
        SUB_5_DEF_FLAT = builder.defineList(
                List.of("def", "flat"),
                () -> List.of(16, 19, 21, 23),
                null, obj -> obj instanceof Integer);
        SUB_5_ATK_PERCENT = builder.defineList(
                List.of("atk", "percent"),
                () -> List.of(4.1, 4.7, 5.3, 5.8),
                null, obj -> obj instanceof Number);
        SUB_5_MAX_HP_PERCENT = builder.defineList(
                List.of("max_hp", "percent"),
                () -> List.of(4.1, 4.7, 5.3, 5.8),
                null, obj -> obj instanceof Number);
        SUB_5_DEF_PERCENT = builder.defineList(
                List.of("def", "percent"),
                () -> List.of(5.1, 5.8, 6.6, 7.3),
                null, obj -> obj instanceof Number);
        SUB_5_EM_FLAT = builder.defineList(
                List.of("elemental_mastery", "flat"),
                () -> List.of(16, 19, 21, 23),
                null, obj -> obj instanceof Integer);
        SUB_5_ER_PERCENT = builder.defineList(
                List.of("energy_recharge", "percent"),
                () -> List.of(4.5, 5.2, 5.8, 6.5),
                null, obj -> obj instanceof Number);
        SUB_5_CR_PERCENT = builder.defineList(
                List.of("crit_rate", "percent"),
                () -> List.of(2.7, 3.1, 3.5, 3.9),
                null, obj -> obj instanceof Number);
        SUB_5_CDG_PERCENT = builder.defineList(
                List.of("crit_dmg", "percent"),
                () -> List.of(5.4, 6.2, 7.0, 7.8),
                null, obj -> obj instanceof Number);

        builder.pop(); // 5star pop
        builder.pop(); // subStat pop
    }
}