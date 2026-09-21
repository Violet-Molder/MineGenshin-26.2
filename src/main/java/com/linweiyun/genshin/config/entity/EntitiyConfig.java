package com.linweiyun.genshin.config.entity;

import net.neoforged.neoforge.common.ModConfigSpec;

public class EntitiyConfig {

    public static ModConfigSpec.ConfigValue<String> CALCULATION_MODE;
    public static ModConfigSpec.ConfigValue<String> SPAWN_RADIUS;

    public static void register(ModConfigSpec.Builder builder) {

        builder.push("spawn-level");
        CALCULATION_MODE = builder
                .translation("minegenshin.configuration.entity.spawn.level.calculation-mode")
                .define("calculation-mode", "NEAREST");
        SPAWN_RADIUS = builder
                .translation("minegenshin.configuration.entity.spawn.level.calculation-radius")
                .define("calculation-radius", "160.0");
        builder.pop();

        EntityHealthConfig.register(builder);
        EntityAttackConfig.register(builder);
        EntityAttributeCapConfig.register(builder);

    }

    public enum CalculationMode { NEAREST, HIGHEST, LOWEST, COMPREHENSIVE }

    public static CalculationMode getCalculationMode() {
        try { return CalculationMode.valueOf(CALCULATION_MODE.get().toUpperCase()); }
        catch (Exception e) { return CalculationMode.NEAREST; }
    }

    public static double getSpawnRadius() {
        try { return Double.parseDouble(SPAWN_RADIUS.get()); }
        catch (Exception e) { return 160.0; }
    }
}