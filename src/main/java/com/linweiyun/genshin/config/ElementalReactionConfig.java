package com.linweiyun.genshin.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ElementalReactionConfig {
    private static final ModConfigSpec.Builder REACTION_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> REACTION_LEVEL_COEFFICIENT;

    public static final ModConfigSpec.ConfigValue<String> VAPORIZE_COEFFICIENT_POSITIVE;
    public static final ModConfigSpec.ConfigValue<String> VAPORIZE_COEFFICIENT_NEGATIVE;
    public static final ModConfigSpec.ConfigValue<String> MELT_COEFFICIENT_POSITIVE;
    public static final ModConfigSpec.ConfigValue<String> MELT_COEFFICIENT_NEGATIVE;

    public static final ModConfigSpec.ConfigValue<String> SUPERCONDUCT_COEFFICIENT;
    public static final ModConfigSpec.ConfigValue<String> SWIRL_COEFFICIENT;
    public static final ModConfigSpec.ConfigValue<String> ELECTOR_CHARGED_COEFFICIENT;
    public static final ModConfigSpec.ConfigValue<String> ICE_BREAKING_COEFFICIENT;
    public static final ModConfigSpec.ConfigValue<String> OVERLOADED_COEFFICIENT;
    public static final ModConfigSpec.ConfigValue<String> BURNING_COEFFICIENT;
    public static final ModConfigSpec.ConfigValue<String> BLOOM_COEFFICIENT;
    public static final ModConfigSpec.ConfigValue<String> BURGEON_COEFFICIENT;
    public static final ModConfigSpec.ConfigValue<String> HYPERBLOOM_COEFFICIENT;


    public static final ModConfigSpec REACTION_SPEC;

    static {
        REACTION_BUILDER.push("reaction-amplifying");

        VAPORIZE_COEFFICIENT_POSITIVE = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.vaporize.positive")
                .define("vaporize-positive", "2.0");

        VAPORIZE_COEFFICIENT_NEGATIVE = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.vaporize.negative")
                .define("vaporize-negative", "1.5");

        MELT_COEFFICIENT_POSITIVE = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.melt.positive")
                .define("melt-positive", "2.0");

        MELT_COEFFICIENT_NEGATIVE = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.melt.negative")
                .define("melt-negative", "1.5");

        REACTION_BUILDER.pop();

        REACTION_BUILDER.push("reaction-fusion");
        REACTION_LEVEL_COEFFICIENT = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.level")
                .defineList(
                        List.of("reaction_level_coefficient"),
                        () -> List.of(
                                "17.17", "39.90", "54.27", "68.64", "83.01", "97.38", "111.75", "126.12", "140.49", "154.86",
                                "169.29", "183.72", "198.15", "212.58", "227.01", "241.44", "255.87", "270.30", "284.73", "299.16",
                                "313.59", "328.02", "342.45", "356.88", "371.31", "385.74", "400.17", "414.60", "429.03", "443.46",
                                "457.89", "472.32", "486.75", "501.18", "515.61", "530.04", "544.47", "558.90", "573.33", "587.76",
                                "602.19", "616.62", "631.05", "645.48", "659.91", "674.34", "715.68", "735.80", "755.92", "776.04",
                                "796.16", "816.28", "836.40", "856.52", "876.64", "896.76", "916.88", "937.00", "957.12", "501.25",
                                "540.17", "579.09", "618.01", "656.93", "695.85", "734.77", "773.69", "812.61", "851.53", "765.64",
                                "787.65", "809.66", "831.67", "853.68", "875.69", "897.70", "919.71", "941.72", "963.73", "1077.44",
                                "1107.01", "1136.58", "1166.15", "1195.72", "1251.91", "1282.90", "1313.89", "1344.88", "1375.87", "1446.85"
                        ),
                        null,
                        obj -> obj instanceof String
                );

        SUPERCONDUCT_COEFFICIENT = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.superconduct")
                .define("superconduct", "1.5");

        SWIRL_COEFFICIENT = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.swirl")
                .define("swirl", "0.6");

        ELECTOR_CHARGED_COEFFICIENT = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.elector-charged")
                .define("elector-charged", "2.0");

        ICE_BREAKING_COEFFICIENT = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.ice-breaking")
                .define("ice-breaking", "3.0");

        OVERLOADED_COEFFICIENT = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.overloaded")
                .define("overloaded", "2.75");

        BURNING_COEFFICIENT = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.burning")
                .define("burning", "0.25");

        BLOOM_COEFFICIENT = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.bloom")
                .define("bloom", "1.0");

        BURGEON_COEFFICIENT = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.burgeon")
                .define("burgeon", "3.0");

        HYPERBLOOM_COEFFICIENT = REACTION_BUILDER
                .translation("minegenshin.config.reaction.coefficient.hyperbloom")
                .define("hyperbloom", "3.0");

        REACTION_BUILDER.pop();
        REACTION_SPEC = REACTION_BUILDER.build();
    }
}