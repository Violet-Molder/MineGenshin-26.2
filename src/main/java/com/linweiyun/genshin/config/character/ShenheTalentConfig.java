package com.linweiyun.genshin.config.character;

import com.linweiyun.genshin.config.util.StringDoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ShenheTalentConfig {

    public static StringDoubleValue NA_BASE_1, NA_BASE_2, NA_BASE_3, NA_BASE_4, NA_BASE_5;
    public static StringDoubleValue NA_PER_LEVEL_1, NA_PER_LEVEL_2, NA_PER_LEVEL_3, NA_PER_LEVEL_4, NA_PER_LEVEL_5;

    public static StringDoubleValue SKILL_PRESS_BASE;
    public static StringDoubleValue SKILL_PRESS_PER_LEVEL;
    public static StringDoubleValue SKILL_HOLD_BASE;
    public static StringDoubleValue SKILL_HOLD_PER_LEVEL;
    public static StringDoubleValue ICY_QUILL_BASE;
    public static StringDoubleValue ICY_QUILL_PER_LEVEL;

    public static StringDoubleValue BURST_CAST_BASE;
    public static StringDoubleValue BURST_CAST_PER_LEVEL;
    public static StringDoubleValue BURST_RES_SHRED_BASE;
    public static StringDoubleValue BURST_RES_SHRED_PER_LEVEL;
    public static StringDoubleValue BURST_DOT_BASE;
    public static StringDoubleValue BURST_DOT_PER_LEVEL;

    static void register(ModConfigSpec.Builder builder) {
        builder.push("talent");

        builder.push("normal-attack");
        NA_BASE_1 = StringDoubleValue.defineInRange(builder, "nab1", 0.433, 0.0, 100.0);
        NA_BASE_2 = StringDoubleValue.defineInRange(builder, "nab2", 0.402, 0.0, 100.0);
        NA_BASE_3 = StringDoubleValue.defineInRange(builder, "nab3", 0.533, 0.0, 100.0);
        NA_BASE_4 = StringDoubleValue.defineInRange(builder, "nab4", 0.263, 0.0, 100.0);
        NA_BASE_5 = StringDoubleValue.defineInRange(builder, "nab5", 0.656, 0.0, 100.0);
        NA_PER_LEVEL_1 = StringDoubleValue.defineInRange(builder, "nap1", 0.0482, 0.0, 100.0);
        NA_PER_LEVEL_2 = StringDoubleValue.defineInRange(builder, "nap2", 0.0450, 0.0, 100.0);
        NA_PER_LEVEL_3 = StringDoubleValue.defineInRange(builder, "nap3", 0.0595, 0.0, 100.0);
        NA_PER_LEVEL_4 = StringDoubleValue.defineInRange(builder, "nap4", 0.0294, 0.0, 100.0);
        NA_PER_LEVEL_5 = StringDoubleValue.defineInRange(builder, "nap5", 0.0733, 0.0, 100.0);
        builder.pop();

        builder.push("elemental-skill");
        SKILL_PRESS_BASE = StringDoubleValue.defineInRange(builder, "shehe-press-damage", 1.39, 0.0, 100.0);
        SKILL_PRESS_PER_LEVEL = StringDoubleValue.defineInRange(builder, "shehe-press-per-level", 0.1308, 0.0, 100.0);
        SKILL_HOLD_BASE = StringDoubleValue.defineInRange(builder, "shehe-hold-damage", 1.888, 0.0, 100.0);
        SKILL_HOLD_PER_LEVEL = StringDoubleValue.defineInRange(builder, "shehe-hold-per-level", 0.177, 0.0, 100.0);
        ICY_QUILL_BASE = StringDoubleValue.defineInRange(builder, "shehe-dmg-bonus", 0.457, 0.0, 100.0);
        ICY_QUILL_PER_LEVEL = StringDoubleValue.defineInRange(builder, "shehe-dmg-bonus-per-level", 0.04275, 0.0, 100.0);
        builder.pop();

        builder.push("elemental-burst");
        BURST_CAST_BASE = StringDoubleValue.defineInRange(builder, "shehe-burst-skill-damage", 1.01, 0.0, 100.0);
        BURST_CAST_PER_LEVEL = StringDoubleValue.defineInRange(builder, "shehe-burst-skill-damage-per-level", 0.0942, 0.0, 100.0);
        BURST_RES_SHRED_BASE = StringDoubleValue.defineInRange(builder, "shehe-burst-res-decrease", 0.06, 0.0, 100.0);
        BURST_RES_SHRED_PER_LEVEL = StringDoubleValue.defineInRange(builder, "shehe-burst-res-decrease-per-level", 0.0075, 0.0, 100.0);
        BURST_DOT_BASE = StringDoubleValue.defineInRange(builder, "shehe-burst-dot", 0.331, 0.0, 100.0);
        BURST_DOT_PER_LEVEL = StringDoubleValue.defineInRange(builder, "shehe-burst-dot-per-level", 0.0311, 0.0, 100.0);
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
            default -> 0.0;
        };
    }
}