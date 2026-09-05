package com.linweiyun.genshin.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ArtifactLevelConfig {

    //TEMP 等级经验配置值（3/4/5星分别每级所需经验）
    public static ModConfigSpec.ConfigValue<List<? extends Integer>> EXP_3_STAR;
    public static ModConfigSpec.ConfigValue<List<? extends Integer>> EXP_4_STAR;
    public static ModConfigSpec.ConfigValue<List<? extends Integer>> EXP_5_STAR;

    //TEMP 由 ArtifactConfig 主入口在共享 builder 上调用，不自己 build spec
    public static void register(ModConfigSpec.Builder builder) {
        builder
                .push("levelUpExp")
                .translation("config.genshin.artifact.levelUpExp");

        EXP_3_STAR = builder
                .translation("config.genshin.artifact.levelUpExp.3star")
                .defineList(
                        List.of("3_star_exp_per_level"),
                        () -> List.of(
                                1800, 2225, 2650, 3100, 3550, 4000, 4500, 5000, 5525, 6075, 6625, 7225
                        ),
                        null,
                        obj -> obj instanceof Integer
                );

        EXP_4_STAR = builder
                .translation("config.genshin.artifact.levelUpExp.4star")
                .defineList(
                        List.of("4_star_exp_per_level"),
                        () -> List.of(
                                2400, 2975, 3550, 4125, 4725, 5350, 6000, 6675, 7375, 8100,
                                8850, 9625, 10425, 12125, 14075, 16300
                        ),
                        null,
                        obj -> obj instanceof Integer
                );

        EXP_5_STAR = builder
                .translation("config.genshin.artifact.levelUpExp.5star")
                .defineList(
                        List.of("5_star_exp_per_level"),
                        () -> List.of(
                                3000, 3725, 4425, 5150, 5900, 6675, 7500, 8350, 9225, 10125,
                                11050, 12025, 13025, 15150, 17600, 20375, 23500, 27050, 31050, 35575
                        ),
                        null,
                        obj -> obj instanceof Integer
                );

        builder.pop(); // levelUpExp pop
    }
}