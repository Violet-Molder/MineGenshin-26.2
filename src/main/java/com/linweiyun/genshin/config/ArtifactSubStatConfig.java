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

        builder.push("5star");

        SUB_5_ATK_FLAT = builder.defineList(
                List.of("atk", "flat"),
                () -> List.of(14, 16, 18, 19),
                null, obj -> obj instanceof Integer);
        SUB_5_ATK_PERCENT = builder.defineList(
                List.of("atk", "percent"),
                () -> List.of(0.041, 0.047, 0.053, 0.058),
                null, obj -> obj instanceof Number);
        SUB_5_MAX_HP_FLAT = builder.defineList(
                List.of("max-hp", "flat"),
                () -> List.of(209, 239, 269, 299),
                null, obj -> obj instanceof Integer);
        SUB_5_DEF_FLAT = builder.defineList(
                List.of("def", "flat"),
                () -> List.of(16, 19, 21, 23),
                null, obj -> obj instanceof Integer);
        SUB_5_MAX_HP_PERCENT = builder.defineList(
                List.of("max-hp", "percent"),
                () -> List.of(0.041, 0.047, 0.053, 0.058),
                null, obj -> obj instanceof Number);
        SUB_5_DEF_PERCENT = builder.defineList(
                List.of("def", "percent"),
                () -> List.of(0.051, 0.058, 0.066, 0.073),
                null, obj -> obj instanceof Number);
        SUB_5_EM_FLAT = builder.defineList(
                List.of("elemental-mastery", "flat"),
                () -> List.of(16, 19, 21, 23),
                null, obj -> obj instanceof Integer);
        SUB_5_ER_PERCENT = builder.defineList(
                List.of("energy-recharge", "percent"),
                () -> List.of(0.045, 0.052, 0.058, 0.065),
                null, obj -> obj instanceof Number);
        SUB_5_CR_PERCENT = builder.defineList(
                List.of("crit-rate", "percent"),
                () -> List.of(0.027, 0.031, 0.035, 0.039),
                null, obj -> obj instanceof Number);
        SUB_5_CDG_PERCENT = builder.defineList(
                List.of("crit-dmg", "percent"),
                () -> List.of(0.054, 0.062, 0.07, 0.078),
                null, obj -> obj instanceof Number);

        builder.pop(); // 5star pop
        builder.pop(); // subStat pop
    }
}