package com.linweiyun.genshin.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class MonsterHealthConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Double> REFERENCE_HEALTH;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> HEALTH_BASE;

    public static final ModConfigSpec MONSTER_HEALTH_SPEC;

    private static final List<Double> DEFAULT_HEALTH_BASE = List.of(
            73.0, 93.0, 114.0, 138.0, 164.0, 192.0, 222.0, 238.0, 262.0, 287.0,
            327.0, 369.0, 412.0, 461.0, 511.0, 562.0, 625.0, 680.0, 736.0, 885.0,
            932.0, 979.0, 1024.0, 1084.0, 1145.0, 1208.0, 1272.0, 1338.0, 1405.0, 1473.0,
            1555.0, 1639.0, 1708.0, 2020.0, 2089.0, 2158.0, 2253.0, 2358.0, 2464.0, 2695.0,
            2825.0, 2958.0, 3084.0, 3212.0, 3593.0, 3719.0, 3935.0, 4155.0, 4380.0, 4996.0,
            5224.0, 5455.0, 5688.0, 5924.0, 6147.0, 6333.0, 6572.0, 6835.0, 7101.0, 8381.0,
            8657.0, 8961.0, 9268.0, 9668.0, 9983.0, 10242.0, 11265.0, 11620.0, 11950.0, 13052.0,
            13411.0, 13806.0, 14092.0, 14504.0, 14921.0, 15265.0, 15664.0, 16067.0, 16440.0, 18566.0,
            18948.0, 19333.0, 19573.0, 20179.0, 20424.0, 20823.0, 21248.0, 21718.0, 22197.0, 24354.0,
            24932.0, 25571.0, 26218.0, 27123.0, 27749.0, 27923.0, 28116.0, 30658.0, 30870.0, 36765.0
    );

    static {
        BUILDER.push("monster-health");

        REFERENCE_HEALTH = BUILDER
                .comment("Benchmark health (vanilla zombie max health), used to calculate entity multiplier.\n"
                        + "Entity multiplier = entity vanilla max health / this benchmark")
                .defineInRange("reference_health", 20.0, 1.0, Double.MAX_VALUE);

        HEALTH_BASE = BUILDER
                .comment("Base health values for each level (1~100)")
                .defineList("health_base", DEFAULT_HEALTH_BASE, () -> 20.0, obj -> obj instanceof Double);

        BUILDER.pop();
        MONSTER_HEALTH_SPEC = BUILDER.build();
    }

    public static double getReferenceHealth() {
        return REFERENCE_HEALTH.get();
    }

    public static double getHealthBase(int level) {
        List<? extends Double> list = HEALTH_BASE.get();
        int index = Math.clamp(level - 1, 0, list.size() - 1);
        return list.get(index);
    }
}