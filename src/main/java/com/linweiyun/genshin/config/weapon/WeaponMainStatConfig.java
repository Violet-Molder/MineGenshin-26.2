package com.linweiyun.genshin.config.weapon;

import com.linweiyun.genshin.config.util.StringDoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec;

public class WeaponMainStatConfig {
    // ====================== Lv1初始基础攻击 ======================
    public static ModConfigSpec.IntValue BASE_ATK_5_TIER1_LV1;
    public static ModConfigSpec.IntValue BASE_ATK_5_TIER2_LV1;
    public static ModConfigSpec.IntValue BASE_ATK_5_TIER3_LV1;
    public static ModConfigSpec.IntValue BASE_ATK_5_TIER4_LV1;

    public static ModConfigSpec.IntValue BASE_ATK_4_TIER1_LV1;
    public static ModConfigSpec.IntValue BASE_ATK_4_TIER2_LV1;
    public static ModConfigSpec.IntValue BASE_ATK_4_TIER3_LV1;
    public static ModConfigSpec.IntValue BASE_ATK_4_TIER4_LV1;

    public static ModConfigSpec.IntValue BASE_ATK_3_TIER1_LV1;
    public static ModConfigSpec.IntValue BASE_ATK_3_TIER2_LV1;
    public static ModConfigSpec.IntValue BASE_ATK_3_TIER3_LV1;

    public static ModConfigSpec.IntValue BASE_ATK_2_TIER1_LV1;

    public static ModConfigSpec.IntValue BASE_ATK_1_TIER1_LV1;

    // ====================== 分段成长值 ascend1~ascend7 ======================
    public static StringDoubleValue GROWTH_5_TIER1_01, GROWTH_5_TIER1_02, GROWTH_5_TIER1_03, GROWTH_5_TIER1_04, GROWTH_5_TIER1_05, GROWTH_5_TIER1_06, GROWTH_5_TIER1_07;
    public static StringDoubleValue GROWTH_5_TIER2_01, GROWTH_5_TIER2_02, GROWTH_5_TIER2_03, GROWTH_5_TIER2_04, GROWTH_5_TIER2_05, GROWTH_5_TIER2_06, GROWTH_5_TIER2_07;
    public static StringDoubleValue GROWTH_5_TIER3_01, GROWTH_5_TIER3_02, GROWTH_5_TIER3_03, GROWTH_5_TIER3_04, GROWTH_5_TIER3_05, GROWTH_5_TIER3_06, GROWTH_5_TIER3_07;
    public static StringDoubleValue GROWTH_5_TIER4_01, GROWTH_5_TIER4_02, GROWTH_5_TIER4_03, GROWTH_5_TIER4_04, GROWTH_5_TIER4_05, GROWTH_5_TIER4_06, GROWTH_5_TIER4_07;

    public static StringDoubleValue GROWTH_4_TIER1_01, GROWTH_4_TIER1_02, GROWTH_4_TIER1_03, GROWTH_4_TIER1_04, GROWTH_4_TIER1_05, GROWTH_4_TIER1_06, GROWTH_4_TIER1_07;
    public static StringDoubleValue GROWTH_4_TIER2_01, GROWTH_4_TIER2_02, GROWTH_4_TIER2_03, GROWTH_4_TIER2_04, GROWTH_4_TIER2_05, GROWTH_4_TIER2_06, GROWTH_4_TIER2_07;
    public static StringDoubleValue GROWTH_4_TIER3_01, GROWTH_4_TIER3_02, GROWTH_4_TIER3_03, GROWTH_4_TIER3_04, GROWTH_4_TIER3_05, GROWTH_4_TIER3_06, GROWTH_4_TIER3_07;
    public static StringDoubleValue GROWTH_4_TIER4_01, GROWTH_4_TIER4_02, GROWTH_4_TIER4_03, GROWTH_4_TIER4_04, GROWTH_4_TIER4_05, GROWTH_4_TIER4_06, GROWTH_4_TIER4_07;

    public static StringDoubleValue GROWTH_3_TIER1_01, GROWTH_3_TIER1_02, GROWTH_3_TIER1_03, GROWTH_3_TIER1_04, GROWTH_3_TIER1_05, GROWTH_3_TIER1_06, GROWTH_3_TIER1_07;
    public static StringDoubleValue GROWTH_3_TIER2_01, GROWTH_3_TIER2_02, GROWTH_3_TIER2_03, GROWTH_3_TIER2_04, GROWTH_3_TIER2_05, GROWTH_3_TIER2_06, GROWTH_3_TIER2_07;
    public static StringDoubleValue GROWTH_3_TIER3_01, GROWTH_3_TIER3_02, GROWTH_3_TIER3_03, GROWTH_3_TIER3_04, GROWTH_3_TIER3_05, GROWTH_3_TIER3_06, GROWTH_3_TIER3_07;

    public static StringDoubleValue GROWTH_2_TIER1_01, GROWTH_2_TIER1_02, GROWTH_2_TIER1_03, GROWTH_2_TIER1_04, GROWTH_2_TIER1_05, GROWTH_2_TIER1_06, GROWTH_2_TIER1_07;

    public static StringDoubleValue GROWTH_1_TIER1_01, GROWTH_1_TIER1_02, GROWTH_1_TIER1_03, GROWTH_1_TIER1_04, GROWTH_1_TIER1_05, GROWTH_1_TIER1_06, GROWTH_1_TIER1_07;


    static void register(ModConfigSpec.Builder builder) {
        builder.push("mainStat");

        builder.push("weapon-base");
        builder.push("5star");
        BASE_ATK_5_TIER1_LV1 = builder.defineInRange("tier1", 44, 0, 1000);
        BASE_ATK_5_TIER2_LV1 = builder.defineInRange("tier2", 46, 0, 1000);
        BASE_ATK_5_TIER3_LV1 = builder.defineInRange("tier3", 48, 0, 1000);
        BASE_ATK_5_TIER4_LV1 = builder.defineInRange("tier4", 49, 0, 1000);
        builder.pop();

        builder.push("4star");
        BASE_ATK_4_TIER1_LV1 = builder.defineInRange("tier1", 41, 0, 1000);
        BASE_ATK_4_TIER2_LV1 = builder.defineInRange("tier2", 42, 0, 1000);
        BASE_ATK_4_TIER3_LV1 = builder.defineInRange("tier3", 44, 0, 1000);
        BASE_ATK_4_TIER4_LV1 = builder.defineInRange("tier4", 45, 0, 1000);
        builder.pop();

        builder.push("3star");
        BASE_ATK_3_TIER1_LV1 = builder.defineInRange("tier1", 38, 0, 1000);
        BASE_ATK_3_TIER2_LV1 = builder.defineInRange("tier2", 39, 0, 1000);
        BASE_ATK_3_TIER3_LV1 = builder.defineInRange("tier3", 40, 0, 1000);
        builder.pop();

        builder.push("2star");
        BASE_ATK_2_TIER1_LV1 = builder.defineInRange("tier1", 33, 0, 1000);
        builder.pop();

        builder.push("1star");
        BASE_ATK_1_TIER1_LV1 = builder.defineInRange("tier1", 23, 0, 1000);
        builder.pop();
        builder.pop();

        builder.push("weapon-growth-per-level");
        builder.push("5star");
        builder.push("tier1");
        GROWTH_5_TIER1_01 = StringDoubleValue.defineInRange(builder, "ascend1", 3.47, 0.0, 100.0);
        GROWTH_5_TIER1_02 = StringDoubleValue.defineInRange(builder, "ascend2", 3.45, 0.0, 100.0);
        GROWTH_5_TIER1_03 = StringDoubleValue.defineInRange(builder, "ascend3", 3.40, 0.0, 100.0);
        GROWTH_5_TIER1_04 = StringDoubleValue.defineInRange(builder, "ascend4", 3.50, 0.0, 100.0);
        GROWTH_5_TIER1_05 = StringDoubleValue.defineInRange(builder, "ascend5", 3.60, 0.0, 100.0);
        GROWTH_5_TIER1_06 = StringDoubleValue.defineInRange(builder, "ascend6", 3.60, 0.0, 100.0);
        GROWTH_5_TIER1_07 = StringDoubleValue.defineInRange(builder, "ascend7", 3.60, 0.0, 100.0);
        builder.pop();

        builder.push("tier2");
        GROWTH_5_TIER2_01 = StringDoubleValue.defineInRange(builder, "ascend1", 4.00, 0.0, 100.0);
        GROWTH_5_TIER2_02 = StringDoubleValue.defineInRange(builder, "ascend2", 4.10, 0.0, 100.0);
        GROWTH_5_TIER2_03 = StringDoubleValue.defineInRange(builder, "ascend3", 4.20, 0.0, 100.0);
        GROWTH_5_TIER2_04 = StringDoubleValue.defineInRange(builder, "ascend4", 4.30, 0.0, 100.0);
        GROWTH_5_TIER2_05 = StringDoubleValue.defineInRange(builder, "ascend5", 4.40, 0.0, 100.0);
        GROWTH_5_TIER2_06 = StringDoubleValue.defineInRange(builder, "ascend6", 4.40, 0.0, 100.0);
        GROWTH_5_TIER2_07 = StringDoubleValue.defineInRange(builder, "ascend7", 4.50, 0.0, 100.0);
        builder.pop();

        builder.push("tier3");
        GROWTH_5_TIER3_01 = StringDoubleValue.defineInRange(builder, "ascend1", 4.47, 0.0, 100.0);
        GROWTH_5_TIER3_02 = StringDoubleValue.defineInRange(builder, "ascend2", 4.85, 0.0, 100.0);
        GROWTH_5_TIER3_03 = StringDoubleValue.defineInRange(builder, "ascend3", 4.90, 0.0, 100.0);
        GROWTH_5_TIER3_04 = StringDoubleValue.defineInRange(builder, "ascend4", 5.10, 0.0, 100.0);
        GROWTH_5_TIER3_05 = StringDoubleValue.defineInRange(builder, "ascend5", 5.20, 0.0, 100.0);
        GROWTH_5_TIER3_06 = StringDoubleValue.defineInRange(builder, "ascend6", 5.30, 0.0, 100.0);
        GROWTH_5_TIER3_07 = StringDoubleValue.defineInRange(builder, "ascend7", 5.30, 0.0, 100.0);
        builder.pop();

        builder.push("tier4");
        GROWTH_5_TIER4_01 = StringDoubleValue.defineInRange(builder, "ascend1", 5.04, 0.0, 100.0);
        GROWTH_5_TIER4_02 = StringDoubleValue.defineInRange(builder, "ascend2", 5.50, 0.0, 100.0);
        GROWTH_5_TIER4_03 = StringDoubleValue.defineInRange(builder, "ascend3", 5.75, 0.0, 100.0);
        GROWTH_5_TIER4_04 = StringDoubleValue.defineInRange(builder, "ascend4", 5.90, 0.0, 100.0);
        GROWTH_5_TIER4_05 = StringDoubleValue.defineInRange(builder, "ascend5", 6.00, 0.0, 100.0);
        GROWTH_5_TIER4_06 = StringDoubleValue.defineInRange(builder, "ascend6", 6.11, 0.0, 100.0);
        GROWTH_5_TIER4_07 = StringDoubleValue.defineInRange(builder, "ascend7", 6.21, 0.0, 100.0);
        builder.pop();
        builder.pop();

        builder.push("4star");
        builder.push("tier1");
        GROWTH_4_TIER1_01 = StringDoubleValue.defineInRange(builder, "ascend1", 3.05, 0.0, 100.0);
        GROWTH_4_TIER1_02 = StringDoubleValue.defineInRange(builder, "ascend2", 2.95, 0.0, 100.0);
        GROWTH_4_TIER1_03 = StringDoubleValue.defineInRange(builder, "ascend3", 2.80, 0.0, 100.0);
        GROWTH_4_TIER1_04 = StringDoubleValue.defineInRange(builder, "ascend4", 2.90, 0.0, 100.0);
        GROWTH_4_TIER1_05 = StringDoubleValue.defineInRange(builder, "ascend5", 2.80, 0.0, 100.0);
        GROWTH_4_TIER1_06 = StringDoubleValue.defineInRange(builder, "ascend6", 2.80, 0.0, 100.0);
        GROWTH_4_TIER1_07 = StringDoubleValue.defineInRange(builder, "ascend7", 2.70, 0.0, 100.0);
        builder.pop();

        builder.push("tier2");
        GROWTH_4_TIER2_01 = StringDoubleValue.defineInRange(builder, "ascend1", 3.53, 0.0, 100.0);
        GROWTH_4_TIER2_02 = StringDoubleValue.defineInRange(builder, "ascend2", 3.50, 0.0, 100.0);
        GROWTH_4_TIER2_03 = StringDoubleValue.defineInRange(builder, "ascend3", 3.50, 0.0, 100.0);
        GROWTH_4_TIER2_04 = StringDoubleValue.defineInRange(builder, "ascend4", 3.50, 0.0, 100.0);
        GROWTH_4_TIER2_05 = StringDoubleValue.defineInRange(builder, "ascend5", 3.50, 0.0, 100.0);
        GROWTH_4_TIER2_06 = StringDoubleValue.defineInRange(builder, "ascend6", 3.50, 0.0, 100.0);
        GROWTH_4_TIER2_07 = StringDoubleValue.defineInRange(builder, "ascend7", 3.50, 0.0, 100.0);
        builder.pop();

        builder.push("tier3");
        GROWTH_4_TIER3_01 = StringDoubleValue.defineInRange(builder, "ascend1", 3.95, 0.0, 100.0);
        GROWTH_4_TIER3_02 = StringDoubleValue.defineInRange(builder, "ascend2", 4.05, 0.0, 100.0);
        GROWTH_4_TIER3_03 = StringDoubleValue.defineInRange(builder, "ascend3", 4.10, 0.0, 100.0);
        GROWTH_4_TIER3_04 = StringDoubleValue.defineInRange(builder, "ascend4", 4.20, 0.0, 100.0);
        GROWTH_4_TIER3_05 = StringDoubleValue.defineInRange(builder, "ascend5", 4.20, 0.0, 100.0);
        GROWTH_4_TIER3_06 = StringDoubleValue.defineInRange(builder, "ascend6", 4.20, 0.0, 100.0);
        GROWTH_4_TIER3_07 = StringDoubleValue.defineInRange(builder, "ascend7", 4.20, 0.0, 100.0);
        builder.pop();

        builder.push("tier4");
        GROWTH_4_TIER4_01 = StringDoubleValue.defineInRange(builder, "ascend1", 4.58, 0.0, 100.0);
        GROWTH_4_TIER4_02 = StringDoubleValue.defineInRange(builder, "ascend2", 4.65, 0.0, 100.0);
        GROWTH_4_TIER4_03 = StringDoubleValue.defineInRange(builder, "ascend3", 4.70, 0.0, 100.0);
        GROWTH_4_TIER4_04 = StringDoubleValue.defineInRange(builder, "ascend4", 4.80, 0.0, 100.0);
        GROWTH_4_TIER4_05 = StringDoubleValue.defineInRange(builder, "ascend5", 4.80, 0.0, 100.0);
        GROWTH_4_TIER4_06 = StringDoubleValue.defineInRange(builder, "ascend6", 4.80, 0.0, 100.0);
        GROWTH_4_TIER4_07 = StringDoubleValue.defineInRange(builder, "ascend7", 4.80, 0.0, 100.0);
        builder.pop();
        builder.pop();

        builder.push("3star");
        builder.push("tier1");
        GROWTH_3_TIER1_01 = StringDoubleValue.defineInRange(builder, "ascend1", 2.63, 0.0, 100.0);
        GROWTH_3_TIER1_02 = StringDoubleValue.defineInRange(builder, "ascend2", 2.50, 0.0, 100.0);
        GROWTH_3_TIER1_03 = StringDoubleValue.defineInRange(builder, "ascend3", 2.40, 0.0, 100.0);
        GROWTH_3_TIER1_04 = StringDoubleValue.defineInRange(builder, "ascend4", 2.40, 0.0, 100.0);
        GROWTH_3_TIER1_05 = StringDoubleValue.defineInRange(builder, "ascend5", 2.40, 0.0, 100.0);
        GROWTH_3_TIER1_06 = StringDoubleValue.defineInRange(builder, "ascend6", 2.40, 0.0, 100.0);
        GROWTH_3_TIER1_07 = StringDoubleValue.defineInRange(builder, "ascend7", 2.40, 0.0, 100.0);
        builder.pop();

        builder.push("tier2");
        GROWTH_3_TIER2_01 = StringDoubleValue.defineInRange(builder, "ascend1", 2.95, 0.0, 100.0);
        GROWTH_3_TIER2_02 = StringDoubleValue.defineInRange(builder, "ascend2", 2.80, 0.0, 100.0);
        GROWTH_3_TIER2_03 = StringDoubleValue.defineInRange(builder, "ascend3", 2.80, 0.0, 100.0);
        GROWTH_3_TIER2_04 = StringDoubleValue.defineInRange(builder, "ascend4", 2.80, 0.0, 100.0);
        GROWTH_3_TIER2_05 = StringDoubleValue.defineInRange(builder, "ascend5", 2.80, 0.0, 100.0);
        GROWTH_3_TIER2_06 = StringDoubleValue.defineInRange(builder, "ascend6", 2.80, 0.0, 100.0);
        GROWTH_3_TIER2_07 = StringDoubleValue.defineInRange(builder, "ascend7", 2.80, 0.0, 100.0);
        builder.pop();

        builder.push("tier3");
        GROWTH_3_TIER3_01 = StringDoubleValue.defineInRange(builder, "ascend1", 3.26, 0.0, 100.0);
        GROWTH_3_TIER3_02 = StringDoubleValue.defineInRange(builder, "ascend2", 3.20, 0.0, 100.0);
        GROWTH_3_TIER3_03 = StringDoubleValue.defineInRange(builder, "ascend3", 3.20, 0.0, 100.0);
        GROWTH_3_TIER3_04 = StringDoubleValue.defineInRange(builder, "ascend4", 3.20, 0.0, 100.0);
        GROWTH_3_TIER3_05 = StringDoubleValue.defineInRange(builder, "ascend5", 3.20, 0.0, 100.0);
        GROWTH_3_TIER3_06 = StringDoubleValue.defineInRange(builder, "ascend6", 3.20, 0.0, 100.0);
        GROWTH_3_TIER3_07 = StringDoubleValue.defineInRange(builder, "ascend7", 3.20, 0.0, 100.0);
        builder.pop();
        builder.pop();

        builder.push("2star");
        builder.push("tier1");
        GROWTH_2_TIER1_01 = StringDoubleValue.defineInRange(builder, "ascend1", 2.63, 0.0, 100.0);
        GROWTH_2_TIER1_02 = StringDoubleValue.defineInRange(builder, "ascend2", 2.50, 0.0, 100.0);
        GROWTH_2_TIER1_03 = StringDoubleValue.defineInRange(builder, "ascend3", 2.40, 0.0, 100.0);
        GROWTH_2_TIER1_04 = StringDoubleValue.defineInRange(builder, "ascend4", 2.40, 0.0, 100.0);
        GROWTH_2_TIER1_05 = StringDoubleValue.defineInRange(builder, "ascend5", 2.40, 0.0, 100.0);
        GROWTH_2_TIER1_06 = StringDoubleValue.defineInRange(builder, "ascend6", 2.40, 0.0, 100.0);
        GROWTH_2_TIER1_07 = StringDoubleValue.defineInRange(builder, "ascend7", 2.40, 0.0, 100.0);
        builder.pop();
        builder.pop();

        builder.push("1star");
        builder.push("tier1");
        GROWTH_1_TIER1_01 = StringDoubleValue.defineInRange(builder, "ascend1", 2.00, 0.0, 100.0);
        GROWTH_1_TIER1_02 = StringDoubleValue.defineInRange(builder, "ascend2", 1.90, 0.0, 100.0);
        GROWTH_1_TIER1_03 = StringDoubleValue.defineInRange(builder, "ascend3", 1.80, 0.0, 100.0);
        GROWTH_1_TIER1_04 = StringDoubleValue.defineInRange(builder, "ascend4", 1.80, 0.0, 100.0);
        GROWTH_1_TIER1_05 = StringDoubleValue.defineInRange(builder, "ascend5", 1.80, 0.0, 100.0);
        GROWTH_1_TIER1_06 = StringDoubleValue.defineInRange(builder, "ascend6", 1.80, 0.0, 100.0);
        GROWTH_1_TIER1_07 = StringDoubleValue.defineInRange(builder, "ascend7", 1.80, 0.0, 100.0);
        builder.pop();
        builder.pop();

        builder.pop();
    }
}