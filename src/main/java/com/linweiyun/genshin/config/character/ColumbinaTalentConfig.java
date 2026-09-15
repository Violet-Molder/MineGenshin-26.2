package com.linweiyun.genshin.config.character;

import com.linweiyun.genshin.config.util.StringDoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ColumbinaTalentConfig {

    public static StringDoubleValue NA_BASE_1, NA_BASE_2, NA_BASE_3;
    public static StringDoubleValue NA_PER_LEVEL_1, NA_PER_LEVEL_2, NA_PER_LEVEL_3;

    static void register(ModConfigSpec.Builder builder) {
        builder.push("talent");

        builder.push("normal-attack");
        NA_BASE_1 = StringDoubleValue.defineInRange(builder, "nab1", 0.468, 0.0, 100.0);
        NA_BASE_2 = StringDoubleValue.defineInRange(builder, "nab2", 0.366, 0.0, 100.0);
        NA_BASE_3 = StringDoubleValue.defineInRange(builder, "nab3", 0.585, 0.0, 100.0);
        NA_PER_LEVEL_1 = StringDoubleValue.defineInRange(builder, "nap1", 0.0421, 0.0, 100.0);
        NA_PER_LEVEL_2 = StringDoubleValue.defineInRange(builder, "nap2", 0.033, 0.0, 100.0);
        NA_PER_LEVEL_3 = StringDoubleValue.defineInRange(builder, "nap3", 0.0526, 0.0, 100.0);
        builder.pop();

        builder.pop();
    }

    public static double getNABase(int segment) {
        return switch (segment) {
            case 1 -> NA_BASE_1.get();
            case 2 -> NA_BASE_2.get();
            case 3 -> NA_BASE_3.get();
            default -> 0.0;
        };
    }

    public static double getNAPerLevel(int segment) {
        return switch (segment) {
            case 1 -> NA_PER_LEVEL_1.get();
            case 2 -> NA_PER_LEVEL_2.get();
            case 3 -> NA_PER_LEVEL_3.get();
            default -> 0.0;
        };
    }
}