package com.linweiyun.genshin.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public class MonsterAttackConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.ConfigValue<Double> REFERENCE_ATTACK;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> ATTACK_BASE;
    public static final ModConfigSpec MONSTER_ATTACK_SPEC;

    static {
        BUILDER.push("monster-attack");
        REFERENCE_ATTACK = BUILDER.comment("Benchmark attack for entity multiplier (vanilla zombie base attack)")
                .defineInRange("reference_attack", 3.0, 0.0, Double.MAX_VALUE);
        ATTACK_BASE = BUILDER.comment("Base attack values for level 1~100")
                .defineList("attack_base", java.util.List.of(
                        25.0, 29.0, 34.0, 38.0, 43.0, 48.0, 53.0, 58.0, 63.0, 68.0,
                        75.0, 83.0, 90.0, 99.0, 108.0, 116.0, 129.0, 141.0, 155.0, 168.0,
                        179.0, 191.0, 204.0, 216.0, 229.0, 238.0, 246.0, 255.0, 264.0, 274.0,
                        284.0, 295.0, 306.0, 319.0, 329.0, 340.0, 355.0, 373.0, 390.0, 409.0,
                        428.0, 449.0, 468.0, 488.0, 509.0, 524.0, 548.0, 571.0, 596.0, 630.0,
                        653.0, 676.0, 700.0, 724.0, 746.0, 766.0, 794.0, 824.0, 854.0, 888.0,
                        915.0, 946.0, 978.0, 1015.0, 1048.0, 1071.0, 1123.0, 1158.0, 1190.0, 1224.0,
                        1258.0, 1294.0, 1321.0, 1359.0, 1396.0, 1424.0, 1460.0, 1496.0, 1531.0, 1575.0,
                        1609.0, 1641.0, 1665.0, 1689.0, 1714.0, 1741.0, 1779.0, 1819.0, 1859.0, 1904.0,
                        1953.0, 2005.0, 2060.0, 2130.0, 2183.0, 2200.0, 2220.0, 2384.0, 2405.0, 2463.0
                ), () -> 3.0, obj -> obj instanceof Double);
        BUILDER.pop();
        MONSTER_ATTACK_SPEC = BUILDER.build();
    }

    public static double getReferenceAttack() { return REFERENCE_ATTACK.get(); }
    public static double getAttackBase(int level) {
        List<? extends Double> list = ATTACK_BASE.get();
        return list.get(Math.clamp(level - 1, 0, list.size() - 1));
    }
}