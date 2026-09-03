package com.linweiyun.genshin.mixin;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ElementalReactionConfig {
    private static final ModConfigSpec.Builder REACTION_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> REACTION_LEVEL_COEFFICIENT;

    public static final ModConfigSpec.ConfigValue<Double> REACTION_MULTIPLIER_OVERLOAD;
    public static final ModConfigSpec.ConfigValue<Double> REACTION_MULTIPLIER_SUPERCONDUCT;
    public static final ModConfigSpec.ConfigValue<Double> REACTION_MULTIPLIER_ELECTRO_CHARGED;
    public static final ModConfigSpec.ConfigValue<Double> REACTION_MULTIPLIER_SWIRL;
    public static final ModConfigSpec.ConfigValue<Double> REACTION_MULTIPLIER_BLOOM;
    public static final ModConfigSpec.ConfigValue<Double> REACTION_MULTIPLIER_BURNING;

    public static final ModConfigSpec REACTION_SPEC;

    static {
        REACTION_BUILDER
                .push("fusionReactionCoefficient")
                .translation("config.genshin.fusionReaction");

        REACTION_BUILDER
                .push("tameReactionLevelCoefficient")
                .translation("config.genshin.fusionReaction.levelCoefficient");

        REACTION_LEVEL_COEFFICIENT = REACTION_BUILDER
                .translation("config.genshin.fusionReaction.levelCoefficient.list")
                .defineList(
                        List.of("reaction_level_coefficient"),
                        () -> List.of(
                                17.17, 39.90, 54.27, 68.64, 83.01, 97.38, 111.75, 126.12, 140.49, 154.86,
                                169.29, 183.72, 198.15, 212.58, 227.01, 241.44, 255.87, 270.30, 284.73, 299.16,
                                313.59, 328.02, 342.45, 356.88, 371.31, 385.74, 400.17, 414.60, 429.03, 443.46,
                                457.89, 472.32, 486.75, 501.18, 515.61, 530.04, 544.47, 558.90, 573.33, 587.76,
                                602.19, 616.62, 631.05, 645.48, 659.91, 674.34, 715.68, 735.80, 755.92, 776.04,
                                796.16, 816.28, 836.40, 856.52, 876.64, 896.76, 916.88, 937.00, 957.12, 501.25,
                                540.17, 579.09, 618.01, 656.93, 695.85, 734.77, 773.69, 812.61, 851.53, 765.64,
                                787.65, 809.66, 831.67, 853.68, 875.69, 897.70, 919.71, 941.72, 963.73, 1077.44,
                                1107.01, 1136.58, 1166.15, 1195.72, 1251.91, 1282.90, 1313.89, 1344.88, 1375.87, 1446.85
                        ),
                        null,
                        obj -> obj instanceof Double,
                        ModConfigSpec.Range.of(90, 90)
                );

        REACTION_BUILDER.pop();

        REACTION_BUILDER
                .push("tameReactionBaseMultiplier")
                .translation("config.genshin.fusionReaction.baseMultiplier");

        REACTION_MULTIPLIER_OVERLOAD = REACTION_BUILDER
                .translation("config.genshin.fusionReaction.baseMultiplier.overload")
                .defineInRange("overload", 2.0, 0.0, 100.0);

        REACTION_MULTIPLIER_SUPERCONDUCT = REACTION_BUILDER
                .translation("config.genshin.fusionReaction.baseMultiplier.superconduct")
                .defineInRange("superconduct", 1.5, 0.0, 100.0);

        REACTION_MULTIPLIER_ELECTRO_CHARGED = REACTION_BUILDER
                .translation("config.genshin.fusionReaction.baseMultiplier.electroCharged")
                .defineInRange("electroCharged", 1.2, 0.0, 100.0);

        REACTION_MULTIPLIER_SWIRL = REACTION_BUILDER
                .translation("config.genshin.fusionReaction.baseMultiplier.swirl")
                .defineInRange("swirl", 1.0, 0.0, 100.0);

        REACTION_MULTIPLIER_BLOOM = REACTION_BUILDER
                .translation("config.genshin.fusionReaction.baseMultiplier.bloom")
                .defineInRange("bloom", 1.0, 0.0, 100.0);

        REACTION_MULTIPLIER_BURNING = REACTION_BUILDER
                .translation("config.genshin.fusionReaction.baseMultiplier.burning")
                .defineInRange("burning", 0.5, 0.0, 100.0);

        REACTION_SPEC = REACTION_BUILDER.build();
    }
}
