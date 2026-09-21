package com.linweiyun.genshin.config.entity;

import net.neoforged.neoforge.common.ModConfigSpec;

public class EntityAttributeCapConfig {

    public static ModConfigSpec.ConfigValue<Boolean> ENABLE_CAP_RELIEF;
    public static ModConfigSpec.ConfigValue<Double> MAX_HEALTH_CAP;
    public static ModConfigSpec.ConfigValue<Double> ATTACK_DAMAGE_CAP;
    public static ModConfigSpec.ConfigValue<Double> ARMOR_CAP;
    public static ModConfigSpec.ConfigValue<Double> ARMOR_TOUGHNESS_CAP;
    public static ModConfigSpec.ConfigValue<Double> ATTACK_SPEED_CAP;
    public static ModConfigSpec.ConfigValue<Double> MOVEMENT_SPEED_CAP;
    public static ModConfigSpec.ConfigValue<Double> LUCK_CAP;

    static void register(ModConfigSpec.Builder builder) {
        builder.push("attribute-cap-relief");

        ENABLE_CAP_RELIEF = builder
                .comment("设为false可关闭属性上限解除（和AttributeFix等模组冲突时用）")
                .translation("minegenshin.configuration.entity.attribute-cap.enable")
                .define("enable-cap-relief", true);

        MAX_HEALTH_CAP = builder
                .comment("最大生命值上限（原版是1024，设成Double.MAX_VALUE等于无上限）")
                .translation("minegenshin.configuration.entity.attribute-cap.max-health")
                .defineInRange("max-health-cap", 1_000_000D, 0D, Double.MAX_VALUE);

        ATTACK_DAMAGE_CAP = builder
                .comment("攻击力上限（原版是2048）")
                .translation("minegenshin.configuration.entity.attribute-cap.attack-damage")
                .defineInRange("attack-damage-cap", 1_000_000D, 0D, Double.MAX_VALUE);

        ARMOR_CAP = builder
                .comment("护甲值上限（原版是30）")
                .translation("minegenshin.configuration.entity.attribute-cap.armor")
                .defineInRange("armor-cap", 1_000_000D, 0D, Double.MAX_VALUE);

        ARMOR_TOUGHNESS_CAP = builder
                .comment("护甲韧性上限（原版是20）")
                .translation("minegenshin.configuration.entity.attribute-cap.armor-toughness")
                .defineInRange("armor-toughness-cap", 1_000_000D, 0D, Double.MAX_VALUE);

        ATTACK_SPEED_CAP = builder
                .comment("攻击速度上限（原版是1024）")
                .translation("minegenshin.configuration.entity.attribute-cap.attack-speed")
                .defineInRange("attack-speed-cap", 1_000_000D, 0D, Double.MAX_VALUE);

        MOVEMENT_SPEED_CAP = builder
                .comment("移动速度上限（原版是20）")
                .translation("minegenshin.configuration.entity.attribute-cap.movement-speed")
                .defineInRange("movement-speed-cap", 1_000_000D, 0D, Double.MAX_VALUE);

        LUCK_CAP = builder
                .comment("幸运值上限（原版是1024）")
                .translation("minegenshin.configuration.entity.attribute-cap.luck")
                .defineInRange("luck-cap", 1_000_000D, 0D, Double.MAX_VALUE);

        builder.pop();
    }

    public static boolean isEnabled() {
        try { return ENABLE_CAP_RELIEF.get(); }
        catch (Exception e) { return true; }
    }

    public static double getMaxHealthCap() {
        try { return MAX_HEALTH_CAP.get(); }
        catch (Exception e) { return 1_000_000D; }
    }

    public static double getAttackDamageCap() {
        try { return ATTACK_DAMAGE_CAP.get(); }
        catch (Exception e) { return 1_000_000D; }
    }

    public static double getArmorCap() {
        try { return ARMOR_CAP.get(); }
        catch (Exception e) { return 1_000_000D; }
    }

    public static double getArmorToughnessCap() {
        try { return ARMOR_TOUGHNESS_CAP.get(); }
        catch (Exception e) { return 1_000_000D; }
    }

    public static double getAttackSpeedCap() {
        try { return ATTACK_SPEED_CAP.get(); }
        catch (Exception e) { return 1_000_000D; }
    }

    public static double getMovementSpeedCap() {
        try { return MOVEMENT_SPEED_CAP.get(); }
        catch (Exception e) { return 1_000_000D; }
    }

    public static double getLuckCap() {
        try { return LUCK_CAP.get(); }
        catch (Exception e) { return 1_000_000D; }
    }
}