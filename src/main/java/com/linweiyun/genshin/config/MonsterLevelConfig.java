package com.linweiyun.genshin.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MonsterLevelConfig {

    public enum CalculationMode {
        NEAREST,
        HIGHEST,
        LOWEST,
        COMPREHENSIVE
    }

    private static final ModConfigSpec.Builder MONSTER_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> CALCULATION_MODE;
    public static final ModConfigSpec.ConfigValue<String> SPAWN_RADIUS;

    public static final ModConfigSpec MONSTER_LEVEL_SPEC;

    static {
        MONSTER_BUILDER
                .push("monster-level");

        CALCULATION_MODE = MONSTER_BUILDER
                .comment("生成等级计算方式：\n"
                        + "  NEAREST — 取生成点距离最近的玩家冒险等阶\n"
                        + "  HIGHEST — 取范围内最高冒险等阶的玩家\n"
                        + "  LOWEST  — 取范围内最低冒险等阶的玩家\n"
                        + "  COMPREHENSIVE — 排除最高AR，取第二高AR，低于第二高25级以上排除，剩余取平均AR再转世界等级")
                .define("calculation-mode", "NEAREST");

        SPAWN_RADIUS = MONSTER_BUILDER
                .comment("搜索半径（方块），仅在该半径内的玩家会被纳入等级计算。\n"
                        + "默认 160.0（对应 MC spawn-distance=10 区块），可根据服务器实际配置调整")
                .define("calculation-radius", "160.0");

        MONSTER_BUILDER.pop();
        MONSTER_LEVEL_SPEC = MONSTER_BUILDER.build();
    }

    public static CalculationMode getCalculationMode() {
        try {
            return CalculationMode.valueOf(CALCULATION_MODE.get().toUpperCase());
        } catch (Exception e) {
            return CalculationMode.NEAREST;
        }
    }

    public static double getSpawnRadius() {
        try {
            return Double.parseDouble(SPAWN_RADIUS.get());
        } catch (Exception e) {
            return 160.0;
        }
    }
}