package com.linweiyun.genshin.config.character;

import com.linweiyun.genshin.config.util.StringDoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ArlecchinoTalentConfig {

    public static StringDoubleValue NA_BASE_1, NA_BASE_2, NA_BASE_3, NA_BASE_4, NA_BASE_5, NA_BASE_6, NA_BASE_7;
    public static StringDoubleValue NA_PER_LEVEL_1, NA_PER_LEVEL_2, NA_PER_LEVEL_3, NA_PER_LEVEL_4, NA_PER_LEVEL_5, NA_PER_LEVEL_6, NA_PER_LEVEL_7;

    static void register(ModConfigSpec.Builder builder) {
        builder.push("talent");

        builder.push("normal-attack");
        NA_BASE_1 = StringDoubleValue.defineInRange(builder, "nab1", 1.204, 0.0, 100.0);
        NA_BASE_2 = StringDoubleValue.defineInRange(builder, "nab2", 0.475, 0.0, 100.0);
        NA_BASE_3 = StringDoubleValue.defineInRange(builder, "nab3", 0.521, 0.0, 100.0);
        NA_BASE_4 = StringDoubleValue.defineInRange(builder, "nab4", 0.654, 0.0, 100.0);
        NA_BASE_5 = StringDoubleValue.defineInRange(builder, "nab5", 0.371, 0.0, 100.0);
        NA_BASE_6 = StringDoubleValue.defineInRange(builder, "nab6", 0.7, 0.0, 100.0);
        NA_BASE_7 = StringDoubleValue.defineInRange(builder, "nab7", 0.854, 0.0, 100.0);
        NA_PER_LEVEL_1 = StringDoubleValue.defineInRange(builder, "nap1", 0.142, 0.0, 100.0);
        NA_PER_LEVEL_2 = StringDoubleValue.defineInRange(builder, "nap2", 0.056, 0.0, 100.0);
        NA_PER_LEVEL_3 = StringDoubleValue.defineInRange(builder, "nap3", 0.062, 0.0, 100.0);
        NA_PER_LEVEL_4 = StringDoubleValue.defineInRange(builder, "nap4", 0.077, 0.0, 100.0);
        NA_PER_LEVEL_5 = StringDoubleValue.defineInRange(builder, "nap5", 0.082, 0.0, 100.0);
        NA_PER_LEVEL_6 = StringDoubleValue.defineInRange(builder, "nap6", 0.092, 0.0, 100.0);
        NA_PER_LEVEL_7 = StringDoubleValue.defineInRange(builder, "nap7", 0.102, 0.0, 100.0);
        builder.pop();

        builder.pop();
    }

    public static double getNABase(int segment) {
        return switch (segment) {
            case 1 -> NA_BASE_1.get();
            case 2 -> NA_BASE_2.get();
            case 3 -> NA_BASE_3.get();
            case 4 -> NA_BASE_4.get();
            case 5 -> NA_BASE_5.get();
            case 6 -> NA_BASE_6.get();
            case 7 -> NA_BASE_7.get();
            default -> 0.0;
        };
    }

    public static double getNAPerLevel(int segment) {
        return switch (segment) {
            case 1 -> NA_PER_LEVEL_1.get();
            case 2 -> NA_PER_LEVEL_2.get();
            case 3 -> NA_PER_LEVEL_3.get();
            case 4 -> NA_PER_LEVEL_4.get();
            case 5 -> NA_PER_LEVEL_5.get();
            case 6 -> NA_PER_LEVEL_6.get();
            case 7 -> NA_PER_LEVEL_7.get();
            default -> 0.0;
        };
    }
}