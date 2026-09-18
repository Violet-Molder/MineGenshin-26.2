package com.linweiyun.genshin.config.artifact;

import com.linweiyun.genshin.config.util.StringDoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ArtifactSubStatConfig {

    // ====================== 五星 副词条 ======================

    // 暴击率 4档
    public static StringDoubleValue SUB_5_CRIT_RATE_TIER1, SUB_5_CRIT_RATE_TIER2, SUB_5_CRIT_RATE_TIER3, SUB_5_CRIT_RATE_TIER4;
    // 暴击伤害 4档
    public static StringDoubleValue SUB_5_CRIT_DMG_TIER1, SUB_5_CRIT_DMG_TIER2, SUB_5_CRIT_DMG_TIER3, SUB_5_CRIT_DMG_TIER4;
    // 生命：tier1-4=percent, tier5-8=flat
    public static StringDoubleValue SUB_5_HP_PERCENT_TIER1, SUB_5_HP_PERCENT_TIER2, SUB_5_HP_PERCENT_TIER3, SUB_5_HP_PERCENT_TIER4;
    public static ModConfigSpec.IntValue SUB_5_HP_FLAT_TIER1, SUB_5_HP_FLAT_TIER2, SUB_5_HP_FLAT_TIER3, SUB_5_HP_FLAT_TIER4;
    // 攻击：tier1-4=percent, tier5-8=flat
    public static StringDoubleValue SUB_5_ATK_PERCENT_TIER1, SUB_5_ATK_PERCENT_TIER2, SUB_5_ATK_PERCENT_TIER3, SUB_5_ATK_PERCENT_TIER4;
    public static ModConfigSpec.IntValue SUB_5_ATK_FLAT_TIER1, SUB_5_ATK_FLAT_TIER2, SUB_5_ATK_FLAT_TIER3, SUB_5_ATK_FLAT_TIER4;
    // 防御：tier1-4=percent, tier5-8=flat
    public static StringDoubleValue SUB_5_DEF_PERCENT_TIER1, SUB_5_DEF_PERCENT_TIER2, SUB_5_DEF_PERCENT_TIER3, SUB_5_DEF_PERCENT_TIER4;
    public static ModConfigSpec.IntValue SUB_5_DEF_FLAT_TIER1, SUB_5_DEF_FLAT_TIER2, SUB_5_DEF_FLAT_TIER3, SUB_5_DEF_FLAT_TIER4;
    // 元素精通 4档
    public static ModConfigSpec.IntValue SUB_5_EM_TIER1, SUB_5_EM_TIER2, SUB_5_EM_TIER3, SUB_5_EM_TIER4;
    // 元素充能效率 4档
    public static StringDoubleValue SUB_5_ER_TIER1, SUB_5_ER_TIER2, SUB_5_ER_TIER3, SUB_5_ER_TIER4;

    // ====================== 四星 副词条 ======================

    // 暴击率 4档
    public static StringDoubleValue SUB_4_CRIT_RATE_TIER1, SUB_4_CRIT_RATE_TIER2, SUB_4_CRIT_RATE_TIER3, SUB_4_CRIT_RATE_TIER4;
    // 暴击伤害 4档
    public static StringDoubleValue SUB_4_CRIT_DMG_TIER1, SUB_4_CRIT_DMG_TIER2, SUB_4_CRIT_DMG_TIER3, SUB_4_CRIT_DMG_TIER4;
    // 生命
    public static StringDoubleValue SUB_4_HP_PERCENT_TIER1, SUB_4_HP_PERCENT_TIER2, SUB_4_HP_PERCENT_TIER3, SUB_4_HP_PERCENT_TIER4;
    public static ModConfigSpec.IntValue SUB_4_HP_FLAT_TIER1, SUB_4_HP_FLAT_TIER2, SUB_4_HP_FLAT_TIER3, SUB_4_HP_FLAT_TIER4;
    // 攻击
    public static StringDoubleValue SUB_4_ATK_PERCENT_TIER1, SUB_4_ATK_PERCENT_TIER2, SUB_4_ATK_PERCENT_TIER3, SUB_4_ATK_PERCENT_TIER4;
    public static ModConfigSpec.IntValue SUB_4_ATK_FLAT_TIER1, SUB_4_ATK_FLAT_TIER2, SUB_4_ATK_FLAT_TIER3, SUB_4_ATK_FLAT_TIER4;
    // 防御
    public static StringDoubleValue SUB_4_DEF_PERCENT_TIER1, SUB_4_DEF_PERCENT_TIER2, SUB_4_DEF_PERCENT_TIER3, SUB_4_DEF_PERCENT_TIER4;
    public static ModConfigSpec.IntValue SUB_4_DEF_FLAT_TIER1, SUB_4_DEF_FLAT_TIER2, SUB_4_DEF_FLAT_TIER3, SUB_4_DEF_FLAT_TIER4;
    // 元素精通 4档
    public static ModConfigSpec.IntValue SUB_4_EM_TIER1, SUB_4_EM_TIER2, SUB_4_EM_TIER3, SUB_4_EM_TIER4;
    // 元素充能效率 4档
    public static StringDoubleValue SUB_4_ER_TIER1, SUB_4_ER_TIER2, SUB_4_ER_TIER3, SUB_4_ER_TIER4;


    static void register(ModConfigSpec.Builder builder) {
        builder.push("subStat");

        builder.push("5star");

        builder.push("crit-rate");
        SUB_5_CRIT_RATE_TIER1 = StringDoubleValue.defineInRange(builder, "tier1", 0.027, 0.0, 1.0);
        SUB_5_CRIT_RATE_TIER2 = StringDoubleValue.defineInRange(builder, "tier2", 0.031, 0.0, 1.0);
        SUB_5_CRIT_RATE_TIER3 = StringDoubleValue.defineInRange(builder, "tier3", 0.035, 0.0, 1.0);
        SUB_5_CRIT_RATE_TIER4 = StringDoubleValue.defineInRange(builder, "tier4", 0.039, 0.0, 1.0);
        builder.pop();

        builder.push("crit-dmg");
        SUB_5_CRIT_DMG_TIER1 = StringDoubleValue.defineInRange(builder, "tier1", 0.054, 0.0, 1.0);
        SUB_5_CRIT_DMG_TIER2 = StringDoubleValue.defineInRange(builder, "tier2", 0.062, 0.0, 1.0);
        SUB_5_CRIT_DMG_TIER3 = StringDoubleValue.defineInRange(builder, "tier3", 0.070, 0.0, 1.0);
        SUB_5_CRIT_DMG_TIER4 = StringDoubleValue.defineInRange(builder, "tier4", 0.078, 0.0, 1.0);
        builder.pop();

        builder.push("hp");
        SUB_5_HP_PERCENT_TIER1 = StringDoubleValue.defineInRange(builder, "tier1-percent", 0.041, 0.0, 1.0);
        SUB_5_HP_PERCENT_TIER2 = StringDoubleValue.defineInRange(builder, "tier2-percent", 0.047, 0.0, 1.0);
        SUB_5_HP_PERCENT_TIER3 = StringDoubleValue.defineInRange(builder, "tier3-percent", 0.053, 0.0, 1.0);
        SUB_5_HP_PERCENT_TIER4 = StringDoubleValue.defineInRange(builder, "tier4-percent", 0.058, 0.0, 1.0);
        SUB_5_HP_FLAT_TIER1 = builder.defineInRange("tier1", 209, 0, 1000);
        SUB_5_HP_FLAT_TIER2 = builder.defineInRange("tier2", 239, 0, 1000);
        SUB_5_HP_FLAT_TIER3 = builder.defineInRange("tier3", 269, 0, 1000);
        SUB_5_HP_FLAT_TIER4 = builder.defineInRange("tier4", 299, 0, 1000);
        builder.pop();

        builder.push("atk");
        SUB_5_ATK_PERCENT_TIER1 = StringDoubleValue.defineInRange(builder, "tier1-percent", 0.041, 0.0, 1.0);
        SUB_5_ATK_PERCENT_TIER2 = StringDoubleValue.defineInRange(builder, "tier2-percent", 0.047, 0.0, 1.0);
        SUB_5_ATK_PERCENT_TIER3 = StringDoubleValue.defineInRange(builder, "tier3-percent", 0.053, 0.0, 1.0);
        SUB_5_ATK_PERCENT_TIER4 = StringDoubleValue.defineInRange(builder, "tier4-percent", 0.058, 0.0, 1.0);
        SUB_5_ATK_FLAT_TIER1 = builder.defineInRange("tier1", 14, 0, 1000);
        SUB_5_ATK_FLAT_TIER2 = builder.defineInRange("tier2", 16, 0, 1000);
        SUB_5_ATK_FLAT_TIER3 = builder.defineInRange("tier3", 18, 0, 1000);
        SUB_5_ATK_FLAT_TIER4 = builder.defineInRange("tier4", 19, 0, 1000);
        builder.pop();

        builder.push("def");
        SUB_5_DEF_PERCENT_TIER1 = StringDoubleValue.defineInRange(builder, "tier1", 0.051, 0.0, 1.0);
        SUB_5_DEF_PERCENT_TIER2 = StringDoubleValue.defineInRange(builder, "tier2", 0.058, 0.0, 1.0);
        SUB_5_DEF_PERCENT_TIER3 = StringDoubleValue.defineInRange(builder, "tier3", 0.066, 0.0, 1.0);
        SUB_5_DEF_PERCENT_TIER4 = StringDoubleValue.defineInRange(builder, "tier4", 0.073, 0.0, 1.0);
        SUB_5_DEF_FLAT_TIER1 = builder.defineInRange("tier5", 16, 0, 1000);
        SUB_5_DEF_FLAT_TIER2 = builder.defineInRange("tier6", 19, 0, 1000);
        SUB_5_DEF_FLAT_TIER3 = builder.defineInRange("tier7", 21, 0, 1000);
        SUB_5_DEF_FLAT_TIER4 = builder.defineInRange("tier8", 23, 0, 1000);
        builder.pop();

        builder.push("elemental-mastery");
        SUB_5_EM_TIER1 = builder.defineInRange("tier1", 16, 0, 100);
        SUB_5_EM_TIER2 = builder.defineInRange("tier2", 19, 0, 100);
        SUB_5_EM_TIER3 = builder.defineInRange("tier3", 21, 0, 100);
        SUB_5_EM_TIER4 = builder.defineInRange("tier4", 23, 0, 100);
        builder.pop();

        builder.push("energy-recharge");
        SUB_5_ER_TIER1 = StringDoubleValue.defineInRange(builder, "tier1", 0.045, 0.0, 1.0);
        SUB_5_ER_TIER2 = StringDoubleValue.defineInRange(builder, "tier2", 0.052, 0.0, 1.0);
        SUB_5_ER_TIER3 = StringDoubleValue.defineInRange(builder, "tier3", 0.058, 0.0, 1.0);
        SUB_5_ER_TIER4 = StringDoubleValue.defineInRange(builder, "tier4", 0.065, 0.0, 1.0);
        builder.pop();

        builder.pop(); // 5star

        builder.push("4star");

        builder.push("crit-rate");
        SUB_4_CRIT_RATE_TIER1 = StringDoubleValue.defineInRange(builder, "tier1", 0.027, 0.0, 1.0);
        SUB_4_CRIT_RATE_TIER2 = StringDoubleValue.defineInRange(builder, "tier2", 0.031, 0.0, 1.0);
        SUB_4_CRIT_RATE_TIER3 = StringDoubleValue.defineInRange(builder, "tier3", 0.035, 0.0, 1.0);
        SUB_4_CRIT_RATE_TIER4 = StringDoubleValue.defineInRange(builder, "tier4", 0.039, 0.0, 1.0);
        builder.pop();

        builder.push("crit-dmg");
        SUB_4_CRIT_DMG_TIER1 = StringDoubleValue.defineInRange(builder, "tier1", 0.054, 0.0, 1.0);
        SUB_4_CRIT_DMG_TIER2 = StringDoubleValue.defineInRange(builder, "tier2", 0.062, 0.0, 1.0);
        SUB_4_CRIT_DMG_TIER3 = StringDoubleValue.defineInRange(builder, "tier3", 0.070, 0.0, 1.0);
        SUB_4_CRIT_DMG_TIER4 = StringDoubleValue.defineInRange(builder, "tier4", 0.078, 0.0, 1.0);
        builder.pop();

        builder.push("hp");
        SUB_4_HP_PERCENT_TIER1 = StringDoubleValue.defineInRange(builder, "tier1-percent", 0.041, 0.0, 1.0);
        SUB_4_HP_PERCENT_TIER2 = StringDoubleValue.defineInRange(builder, "tier2-percent", 0.047, 0.0, 1.0);
        SUB_4_HP_PERCENT_TIER3 = StringDoubleValue.defineInRange(builder, "tier3-percent", 0.053, 0.0, 1.0);
        SUB_4_HP_PERCENT_TIER4 = StringDoubleValue.defineInRange(builder, "tier4-percent", 0.058, 0.0, 1.0);
        SUB_4_HP_FLAT_TIER1 = builder.defineInRange("tier1", 209, 0, 1000);
        SUB_4_HP_FLAT_TIER2 = builder.defineInRange("tier2", 239, 0, 1000);
        SUB_4_HP_FLAT_TIER3 = builder.defineInRange("tier3", 269, 0, 1000);
        SUB_4_HP_FLAT_TIER4 = builder.defineInRange("tier4", 299, 0, 1000);
        builder.pop();

        builder.push("atk");
        SUB_4_ATK_PERCENT_TIER1 = StringDoubleValue.defineInRange(builder, "tier1-percent", 0.041, 0.0, 1.0);
        SUB_4_ATK_PERCENT_TIER2 = StringDoubleValue.defineInRange(builder, "tier2-percent", 0.047, 0.0, 1.0);
        SUB_4_ATK_PERCENT_TIER3 = StringDoubleValue.defineInRange(builder, "tier3-percent", 0.053, 0.0, 1.0);
        SUB_4_ATK_PERCENT_TIER4 = StringDoubleValue.defineInRange(builder, "tier4-percent", 0.058, 0.0, 1.0);
        SUB_4_ATK_FLAT_TIER1 = builder.defineInRange("tier1", 14, 0, 1000);
        SUB_4_ATK_FLAT_TIER2 = builder.defineInRange("tier2", 16, 0, 1000);
        SUB_4_ATK_FLAT_TIER3 = builder.defineInRange("tier3", 18, 0, 1000);
        SUB_4_ATK_FLAT_TIER4 = builder.defineInRange("tier4", 19, 0, 1000);
        builder.pop();

        builder.push("def");
        SUB_4_DEF_PERCENT_TIER1 = StringDoubleValue.defineInRange(builder, "tier1", 0.051, 0.0, 1.0);
        SUB_4_DEF_PERCENT_TIER2 = StringDoubleValue.defineInRange(builder, "tier2", 0.058, 0.0, 1.0);
        SUB_4_DEF_PERCENT_TIER3 = StringDoubleValue.defineInRange(builder, "tier3", 0.066, 0.0, 1.0);
        SUB_4_DEF_PERCENT_TIER4 = StringDoubleValue.defineInRange(builder, "tier4", 0.073, 0.0, 1.0);
        SUB_4_DEF_FLAT_TIER1 = builder.defineInRange("tier5", 16, 0, 1000);
        SUB_4_DEF_FLAT_TIER2 = builder.defineInRange("tier6", 19, 0, 1000);
        SUB_4_DEF_FLAT_TIER3 = builder.defineInRange("tier7", 21, 0, 1000);
        SUB_4_DEF_FLAT_TIER4 = builder.defineInRange("tier8", 23, 0, 1000);
        builder.pop();

        builder.push("elemental-mastery");
        SUB_4_EM_TIER1 = builder.defineInRange("tier1", 16, 0, 100);
        SUB_4_EM_TIER2 = builder.defineInRange("tier2", 19, 0, 100);
        SUB_4_EM_TIER3 = builder.defineInRange("tier3", 21, 0, 100);
        SUB_4_EM_TIER4 = builder.defineInRange("tier4", 23, 0, 100);
        builder.pop();

        builder.push("energy-recharge");
        SUB_4_ER_TIER1 = StringDoubleValue.defineInRange(builder, "tier1", 0.045, 0.0, 1.0);
        SUB_4_ER_TIER2 = StringDoubleValue.defineInRange(builder, "tier2", 0.052, 0.0, 1.0);
        SUB_4_ER_TIER3 = StringDoubleValue.defineInRange(builder, "tier3", 0.058, 0.0, 1.0);
        SUB_4_ER_TIER4 = StringDoubleValue.defineInRange(builder, "tier4", 0.065, 0.0, 1.0);
        builder.pop();

        builder.pop(); // 4star
        builder.pop(); // subStat
    }
}