package com.linweiyun.genshin.content.items.weapon;

import com.linweiyun.genshin.config.weapon.WeaponConfig;
import com.linweiyun.genshin.config.weapon.WeaponSubStatConfig;
import com.linweiyun.genshin.content.attribute.AttributeType;
import static com.linweiyun.genshin.config.weapon.WeaponMainStatConfig.*;

public class WeaponStatData {

    public static int getBaseAtk(int star, int tier) {
        if (star >= 5) {
            return switch (tier) {
                case 2 -> BASE_ATK_5_TIER2_LV1.get();
                case 3 -> BASE_ATK_5_TIER3_LV1.get();
                case 4 -> BASE_ATK_5_TIER4_LV1.get();
                default -> BASE_ATK_5_TIER1_LV1.get();
            };
        } else if (star == 4) {
            return switch (tier) {
                case 2 -> BASE_ATK_4_TIER2_LV1.get();
                case 3 -> BASE_ATK_4_TIER3_LV1.get();
                case 4 -> BASE_ATK_4_TIER4_LV1.get();
                default -> BASE_ATK_4_TIER1_LV1.get();
            };
        } else if (star == 3) {
            return switch (tier) {
                case 2 -> BASE_ATK_3_TIER2_LV1.get();
                case 3 -> BASE_ATK_3_TIER3_LV1.get();
                default -> BASE_ATK_3_TIER1_LV1.get();
            };
        } else if (star == 2) {
            return BASE_ATK_2_TIER1_LV1.get();
        }
        return BASE_ATK_1_TIER1_LV1.get();
    }

    public static double getMainStatGrowth(int star, int tier, int ascensionStage) {
        if (star >= 5) return get5StarGrowth(tier, ascensionStage);
        if (star == 4) return get4StarGrowth(tier, ascensionStage);
        if (star == 3) return get3StarGrowth(tier, ascensionStage);
        if (star == 2) return get2StarGrowth(ascensionStage);
        return get1StarGrowth(ascensionStage);
    }

    private static double get5StarGrowth(int tier, int stage) {
        return switch (tier) {
            case 2 -> switchG2_5(stage);
            case 3 -> switchG3_5(stage);
            case 4 -> switchG4_5(stage);
            default -> switchG1_5(stage);
        };
    }

    private static double switchG1_5(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_5_TIER1_01.get(); case 2 -> GROWTH_5_TIER1_02.get();
            case 3 -> GROWTH_5_TIER1_03.get(); case 4 -> GROWTH_5_TIER1_04.get();
            case 5 -> GROWTH_5_TIER1_05.get(); case 6 -> GROWTH_5_TIER1_06.get();
            case 7 -> GROWTH_5_TIER1_07.get(); default -> 0.0;
        };
    }

    private static double switchG2_5(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_5_TIER2_01.get(); case 2 -> GROWTH_5_TIER2_02.get();
            case 3 -> GROWTH_5_TIER2_03.get(); case 4 -> GROWTH_5_TIER2_04.get();
            case 5 -> GROWTH_5_TIER2_05.get(); case 6 -> GROWTH_5_TIER2_06.get();
            case 7 -> GROWTH_5_TIER2_07.get(); default -> 0.0;
        };
    }

    private static double switchG3_5(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_5_TIER3_01.get(); case 2 -> GROWTH_5_TIER3_02.get();
            case 3 -> GROWTH_5_TIER3_03.get(); case 4 -> GROWTH_5_TIER3_04.get();
            case 5 -> GROWTH_5_TIER3_05.get(); case 6 -> GROWTH_5_TIER3_06.get();
            case 7 -> GROWTH_5_TIER3_07.get(); default -> 0.0;
        };
    }

    private static double switchG4_5(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_5_TIER4_01.get(); case 2 -> GROWTH_5_TIER4_02.get();
            case 3 -> GROWTH_5_TIER4_03.get(); case 4 -> GROWTH_5_TIER4_04.get();
            case 5 -> GROWTH_5_TIER4_05.get(); case 6 -> GROWTH_5_TIER4_06.get();
            case 7 -> GROWTH_5_TIER4_07.get(); default -> 0.0;
        };
    }

    private static double get4StarGrowth(int tier, int stage) {
        return switch (tier) {
            case 2 -> switchG2_4(stage);
            case 3 -> switchG3_4(stage);
            case 4 -> switchG4_4(stage);
            default -> switchG1_4(stage);
        };
    }

    private static double switchG1_4(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_4_TIER1_01.get(); case 2 -> GROWTH_4_TIER1_02.get();
            case 3 -> GROWTH_4_TIER1_03.get(); case 4 -> GROWTH_4_TIER1_04.get();
            case 5 -> GROWTH_4_TIER1_05.get(); case 6 -> GROWTH_4_TIER1_06.get();
            case 7 -> GROWTH_4_TIER1_07.get(); default -> 0.0;
        };
    }

    private static double switchG2_4(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_4_TIER2_01.get(); case 2 -> GROWTH_4_TIER2_02.get();
            case 3 -> GROWTH_4_TIER2_03.get(); case 4 -> GROWTH_4_TIER2_04.get();
            case 5 -> GROWTH_4_TIER2_05.get(); case 6 -> GROWTH_4_TIER2_06.get();
            case 7 -> GROWTH_4_TIER2_07.get(); default -> 0.0;
        };
    }

    private static double switchG3_4(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_4_TIER3_01.get(); case 2 -> GROWTH_4_TIER3_02.get();
            case 3 -> GROWTH_4_TIER3_03.get(); case 4 -> GROWTH_4_TIER3_04.get();
            case 5 -> GROWTH_4_TIER3_05.get(); case 6 -> GROWTH_4_TIER3_06.get();
            case 7 -> GROWTH_4_TIER3_07.get(); default -> 0.0;
        };
    }

    private static double switchG4_4(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_4_TIER4_01.get(); case 2 -> GROWTH_4_TIER4_02.get();
            case 3 -> GROWTH_4_TIER4_03.get(); case 4 -> GROWTH_4_TIER4_04.get();
            case 5 -> GROWTH_4_TIER4_05.get(); case 6 -> GROWTH_4_TIER4_06.get();
            case 7 -> GROWTH_4_TIER4_07.get(); default -> 0.0;
        };
    }

    private static double get3StarGrowth(int tier, int stage) {
        return switch (tier) {
            case 2 -> switchG2_3(stage);
            case 3 -> switchG3_3(stage);
            default -> switchG1_3(stage);
        };
    }

    private static double switchG1_3(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_3_TIER1_01.get(); case 2 -> GROWTH_3_TIER1_02.get();
            case 3 -> GROWTH_3_TIER1_03.get(); case 4 -> GROWTH_3_TIER1_04.get();
            case 5 -> GROWTH_3_TIER1_05.get(); case 6 -> GROWTH_3_TIER1_06.get();
            case 7 -> GROWTH_3_TIER1_07.get(); default -> 0.0;
        };
    }

    private static double switchG2_3(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_3_TIER2_01.get(); case 2 -> GROWTH_3_TIER2_02.get();
            case 3 -> GROWTH_3_TIER2_03.get(); case 4 -> GROWTH_3_TIER2_04.get();
            case 5 -> GROWTH_3_TIER2_05.get(); case 6 -> GROWTH_3_TIER2_06.get();
            case 7 -> GROWTH_3_TIER2_07.get(); default -> 0.0;
        };
    }

    private static double switchG3_3(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_3_TIER3_01.get(); case 2 -> GROWTH_3_TIER3_02.get();
            case 3 -> GROWTH_3_TIER3_03.get(); case 4 -> GROWTH_3_TIER3_04.get();
            case 5 -> GROWTH_3_TIER3_05.get(); case 6 -> GROWTH_3_TIER3_06.get();
            case 7 -> GROWTH_3_TIER3_07.get(); default -> 0.0;
        };
    }

    private static double get2StarGrowth(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_2_TIER1_01.get(); case 2 -> GROWTH_2_TIER1_02.get();
            case 3 -> GROWTH_2_TIER1_03.get(); case 4 -> GROWTH_2_TIER1_04.get();
            case 5 -> GROWTH_2_TIER1_05.get(); case 6 -> GROWTH_2_TIER1_06.get();
            case 7 -> GROWTH_2_TIER1_07.get(); default -> 0.0;
        };
    }

    private static double get1StarGrowth(int stage) {
        return switch (stage) {
            case 1 -> GROWTH_1_TIER1_01.get(); case 2 -> GROWTH_1_TIER1_02.get();
            case 3 -> GROWTH_1_TIER1_03.get(); case 4 -> GROWTH_1_TIER1_04.get();
            case 5 -> GROWTH_1_TIER1_05.get(); case 6 -> GROWTH_1_TIER1_06.get();
            case 7 -> GROWTH_1_TIER1_07.get(); default -> 0.0;
        };
    }

    public static int getAscensionStage(int level) {
        if (level <= 20) return 1;
        if (level <= 40) return 2;
        if (level <= 50) return 3;
        if (level <= 60) return 4;
        if (level <= 70) return 5;
        if (level <= 80) return 6;
        return 7;
    }

    public static int calculateMainStatValue(int star, int tier, int level, int ascended) {
        double total = getBaseAtk(star, tier);
        for (int lv = 2; lv <= level; lv++) {
            int stage = getAscensionStage(lv - 1);
            total += getMainStatGrowth(star, tier, stage);
        }
        total += ascended * WeaponConfig.getAscensionBonus(star);
        return (int) Math.floor(total);
    }

    public static double getSubStatBase(int star, int tier, AttributeType subStatType) {
        String sub = subStatToKey(subStatType);
        if (sub.isEmpty()) return 0.0;
        if (star >= 5) return getSub5(tier, sub);
        if (star == 4) return getSub4(tier, sub);
        if (star == 3) return getSub3(tier, sub);
        return 0.0;
    }

    private static String subStatToKey(AttributeType attr) {
        return switch (attr.id().getPath()) {
            case "max_hp" -> "hp";
            case "crit_rate" -> "crit-rate";
            case "crit_dmg" -> "crit-dmg";
            case "energy_recharge" -> "energy-recharge";
            case "atk" -> "atk";
            case "physical_bonus" -> "physical-bonus";
            case "elemental_mastery" -> "elemental-mastery";
            default -> "";
        };
    }

    private static double getSub5(int tier, String sub) {
        return switch (tier) {
            case 2 -> sub5_46(sub);
            case 3 -> sub5_48(sub);
            case 4 -> sub5_49(sub);
            default -> sub5_44(sub);
        };
    }

    private static double getSub4(int tier, String sub) {
        return switch (tier) {
            case 2 -> sub4_42(sub);
            case 3 -> sub4_44(sub);
            case 4 -> sub4_45(sub);
            default -> sub4_41(sub);
        };
    }

    private static double getSub3(int tier, String sub) {
        return switch (tier) {
            case 2 -> sub3_39(sub);
            case 3 -> sub3_40(sub);
            default -> sub3_38(sub);
        };
    }

    private static double sub5_44(String sub) { return switch (sub) { case "crit-rate" -> WeaponSubStatConfig.SUB_5_TIER44_CRIT_RATE.get(); case "crit-dmg" -> WeaponSubStatConfig.SUB_5_TIER44_CRIT_DMG.get(); case "energy-recharge" -> WeaponSubStatConfig.SUB_5_TIER44_ENERGY_RECHARGE.get(); case "atk" -> WeaponSubStatConfig.SUB_5_TIER44_ATK_PERCENT.get(); case "physical-bonus" -> WeaponSubStatConfig.SUB_5_TIER44_PHYSICAL_PERCENT.get(); case "hp" -> WeaponSubStatConfig.SUB_5_TIER44_HP_PERCENT.get(); case "elemental-mastery" -> WeaponSubStatConfig.SUB_5_TIER44_ELEMENTAL_MASTERY.get(); default -> 0.0; }; }
    private static double sub5_46(String sub) { return switch (sub) { case "crit-rate" -> WeaponSubStatConfig.SUB_5_TIER46_CRIT_RATE.get(); case "crit-dmg" -> WeaponSubStatConfig.SUB_5_TIER46_CRIT_DMG.get(); case "energy-recharge" -> WeaponSubStatConfig.SUB_5_TIER46_ENERGY_RECHARGE.get(); case "atk" -> WeaponSubStatConfig.SUB_5_TIER46_ATK_PERCENT.get(); case "physical-bonus" -> WeaponSubStatConfig.SUB_5_TIER46_PHYSICAL_PERCENT.get(); case "hp" -> WeaponSubStatConfig.SUB_5_TIER46_HP_PERCENT.get(); case "elemental-mastery" -> WeaponSubStatConfig.SUB_5_TIER46_ELEMENTAL_MASTERY.get(); default -> 0.0; }; }
    private static double sub5_48(String sub) { return switch (sub) { case "crit-rate" -> WeaponSubStatConfig.SUB_5_TIER48_CRIT_RATE.get(); case "crit-dmg" -> WeaponSubStatConfig.SUB_5_TIER48_CRIT_DMG.get(); case "energy-recharge" -> WeaponSubStatConfig.SUB_5_TIER48_ENERGY_RECHARGE.get(); case "atk" -> WeaponSubStatConfig.SUB_5_TIER48_ATK_PERCENT.get(); case "physical-bonus" -> WeaponSubStatConfig.SUB_5_TIER48_PHYSICAL_PERCENT.get(); case "hp" -> WeaponSubStatConfig.SUB_5_TIER48_HP_PERCENT.get(); case "elemental-mastery" -> WeaponSubStatConfig.SUB_5_TIER48_ELEMENTAL_MASTERY.get(); default -> 0.0; }; }
    private static double sub5_49(String sub) { return switch (sub) { case "crit-rate" -> WeaponSubStatConfig.SUB_5_TIER49_CRIT_RATE.get(); case "crit-dmg" -> WeaponSubStatConfig.SUB_5_TIER49_CRIT_DMG.get(); case "energy-recharge" -> WeaponSubStatConfig.SUB_5_TIER49_ENERGY_RECHARGE.get(); case "atk" -> WeaponSubStatConfig.SUB_5_TIER49_ATK_PERCENT.get(); case "physical-bonus" -> WeaponSubStatConfig.SUB_5_TIER49_PHYSICAL_PERCENT.get(); case "elemental-mastery" -> WeaponSubStatConfig.SUB_5_TIER49_ELEMENTAL_MASTERY.get(); default -> 0.0; }; }

    private static double sub4_41(String sub) { return switch (sub) { case "crit-rate" -> WeaponSubStatConfig.SUB_4_TIER41_CRIT_RATE.get(); case "crit-dmg" -> WeaponSubStatConfig.SUB_4_TIER41_CRIT_DMG.get(); case "energy-recharge" -> WeaponSubStatConfig.SUB_4_TIER41_ENERGY_RECHARGE.get(); case "atk" -> WeaponSubStatConfig.SUB_4_TIER41_ATK_PERCENT.get(); case "physical-bonus" -> WeaponSubStatConfig.SUB_4_TIER41_PHYSICAL_PERCENT.get(); case "elemental-mastery" -> WeaponSubStatConfig.SUB_4_TIER41_ELEMENTAL_MASTERY.get(); default -> 0.0; }; }
    private static double sub4_42(String sub) { return switch (sub) { case "crit-rate" -> WeaponSubStatConfig.SUB_4_TIER42_CRIT_RATE.get(); case "crit-dmg" -> WeaponSubStatConfig.SUB_4_TIER42_CRIT_DMG.get(); case "energy-recharge" -> WeaponSubStatConfig.SUB_4_TIER42_ENERGY_RECHARGE.get(); case "atk" -> WeaponSubStatConfig.SUB_4_TIER42_ATK_PERCENT.get(); case "physical-bonus" -> WeaponSubStatConfig.SUB_4_TIER42_PHYSICAL_PERCENT.get(); case "elemental-mastery" -> WeaponSubStatConfig.SUB_4_TIER42_ELEMENTAL_MASTERY.get(); default -> 0.0; }; }
    private static double sub4_44(String sub) { return switch (sub) { case "crit-rate" -> WeaponSubStatConfig.SUB_4_TIER44_CRIT_RATE.get(); case "crit-dmg" -> WeaponSubStatConfig.SUB_4_TIER44_CRIT_DMG.get(); case "energy-recharge" -> WeaponSubStatConfig.SUB_4_TIER44_ENERGY_RECHARGE.get(); case "atk" -> WeaponSubStatConfig.SUB_4_TIER44_ATK_PERCENT.get(); case "physical-bonus" -> WeaponSubStatConfig.SUB_4_TIER44_PHYSICAL_PERCENT.get(); case "elemental-mastery" -> WeaponSubStatConfig.SUB_4_TIER44_ELEMENTAL_MASTERY.get(); default -> 0.0; }; }
    private static double sub4_45(String sub) { return switch (sub) { case "crit-rate" -> WeaponSubStatConfig.SUB_4_TIER45_CRIT_RATE.get(); case "crit-dmg" -> WeaponSubStatConfig.SUB_4_TIER45_CRIT_DMG.get(); case "energy-recharge" -> WeaponSubStatConfig.SUB_4_TIER45_ENERGY_RECHARGE.get(); case "atk" -> WeaponSubStatConfig.SUB_4_TIER45_ATK_PERCENT.get(); case "physical-bonus" -> WeaponSubStatConfig.SUB_4_TIER45_PHYSICAL_PERCENT.get(); case "elemental-mastery" -> WeaponSubStatConfig.SUB_4_TIER45_ELEMENTAL_MASTERY.get(); default -> 0.0; }; }

    private static double sub3_38(String sub) { return switch (sub) { case "crit-rate" -> WeaponSubStatConfig.SUB_3_TIER38_CRIT_RATE.get(); case "crit-dmg" -> WeaponSubStatConfig.SUB_3_TIER38_CRIT_DMG.get(); case "energy-recharge" -> WeaponSubStatConfig.SUB_3_TIER38_ENERGY_RECHARGE.get(); case "atk" -> WeaponSubStatConfig.SUB_3_TIER38_ATK_PERCENT.get(); case "physical-bonus" -> WeaponSubStatConfig.SUB_3_TIER38_PHYSICAL_PERCENT.get(); case "elemental-mastery" -> WeaponSubStatConfig.SUB_3_TIER38_ELEMENTAL_MASTERY.get(); default -> 0.0; }; }
    private static double sub3_39(String sub) { return switch (sub) { case "crit-rate" -> WeaponSubStatConfig.SUB_3_TIER39_CRIT_RATE.get(); case "crit-dmg" -> WeaponSubStatConfig.SUB_3_TIER39_CRIT_DMG.get(); case "energy-recharge" -> WeaponSubStatConfig.SUB_3_TIER39_ENERGY_RECHARGE.get(); case "atk" -> WeaponSubStatConfig.SUB_3_TIER39_ATK_PERCENT.get(); case "physical-bonus" -> WeaponSubStatConfig.SUB_3_TIER39_PHYSICAL_PERCENT.get(); case "elemental-mastery" -> WeaponSubStatConfig.SUB_3_TIER39_ELEMENTAL_MASTERY.get(); default -> 0.0; }; }
    private static double sub3_40(String sub) { return switch (sub) { case "crit-rate" -> WeaponSubStatConfig.SUB_3_TIER40_CRIT_RATE.get(); case "crit-dmg" -> WeaponSubStatConfig.SUB_3_TIER40_CRIT_DMG.get(); case "energy-recharge" -> WeaponSubStatConfig.SUB_3_TIER40_ENERGY_RECHARGE.get(); case "atk" -> WeaponSubStatConfig.SUB_3_TIER40_ATK_PERCENT.get(); case "physical-bonus" -> WeaponSubStatConfig.SUB_3_TIER40_PHYSICAL_PERCENT.get(); case "elemental-mastery" -> WeaponSubStatConfig.SUB_3_TIER40_ELEMENTAL_MASTERY.get(); default -> 0.0; }; }

    public static double getSubStatValue(int star, int tier, AttributeType subStatType, int level) {
        double base = getSubStatBase(star, tier, subStatType);
        if (base == 0.0) return 0.0;
        double mult = WeaponSubStatConfig.getMultiplier(level);
        return base * mult;
    }
}