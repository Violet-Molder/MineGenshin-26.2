package com.linweiyun.genshin.config.weapon;

import net.neoforged.neoforge.common.ModConfigSpec;

public class WeaponConfig {

    public static ModConfigSpec.IntValue ASCENSION_5STAR;
    public static ModConfigSpec.IntValue ASCENSION_4STAR;
    public static ModConfigSpec.IntValue ASCENSION_3STAR;
    public static ModConfigSpec.IntValue ASCENSION_2STAR;
    public static ModConfigSpec.IntValue ASCENSION_1STAR;

    public static void register(ModConfigSpec.Builder builder) {

        WeaponXpConfig.register(builder);

        builder.push("ascension");
        ASCENSION_5STAR = builder
                .translation("minegenshin.configuration.5star")
                .defineInRange("bonus_5star", 31, 0, 1000);
        ASCENSION_4STAR = builder
                .translation("minegenshin.configuration.4star")
                .defineInRange("bonus_4star", 26, 0, 1000);
        ASCENSION_3STAR = builder
                .translation("minegenshin.configuration.3star")
                .defineInRange("bonus_3star", 19, 0, 1000);
        ASCENSION_2STAR = builder
                .translation("minegenshin.configuration.2star")
                .defineInRange("bonus_2star", 16, 0, 1000);
        ASCENSION_1STAR = builder
                .translation("minegenshin.configuration.1star")
                .defineInRange("bonus_1star", 11, 0, 1000);
        builder.pop();

        WeaponSubStatConfig.register(builder);
        WeaponMainStatConfig.register(builder);
    }

    public static int getAscensionBonus(int star) {
        return switch (star) {
            case 5 -> ASCENSION_5STAR.get();
            case 4 -> ASCENSION_4STAR.get();
            case 3 -> ASCENSION_3STAR.get();
            case 2 -> ASCENSION_2STAR.get();
            case 1 -> ASCENSION_1STAR.get();
            default -> 0;
        };
    }
}