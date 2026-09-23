package com.linweiyun.genshin.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class WorldTextColorConfig {

    public static ModConfigSpec.ConfigValue<String> PHYSICAL_COLOR;
    public static ModConfigSpec.ConfigValue<String> PYRO_COLOR;
    public static ModConfigSpec.ConfigValue<String> HYDRO_COLOR;
    public static ModConfigSpec.ConfigValue<String> DENDRO_COLOR;
    public static ModConfigSpec.ConfigValue<String> ELECTRO_COLOR;
    public static ModConfigSpec.ConfigValue<String> ANEMO_COLOR;
    public static ModConfigSpec.ConfigValue<String> CYRO_COLOR;
    public static ModConfigSpec.ConfigValue<String> FROZEN_COLOR;
    public static ModConfigSpec.ConfigValue<String> GEO_COLOR;
    public static ModConfigSpec.ConfigValue<String> MELT_COLOR;
    public static ModConfigSpec.ConfigValue<String> VAPORIZE_COLOR;
    public static ModConfigSpec.ConfigValue<String> ELECTRO_CHARGED_COLOR;
    public static ModConfigSpec.ConfigValue<String> SWIRL_COLOR;

    public static ModConfigSpec.ConfigValue<String> LUNAR_TOP_COLOR;
    public static ModConfigSpec.ConfigValue<String> STELLAR_BOTTOM_WIND_COLOR;
    public static ModConfigSpec.ConfigValue<String> STELLAR_BOTTOM_ICE_COLOR;

    static void register(ModConfigSpec.Builder builder) {
        builder.push("elemental-color");
        PHYSICAL_COLOR = builder
                .translation("minegenshin.configuration.elemental.physical_color")
                .define("physical_color", "#FFFFFF");
        PYRO_COLOR = builder
                .translation("minegenshin.configuration.elemental.pyro_color")
                .define("pyro_color", "#FB9B00");
        HYDRO_COLOR = builder
                .translation("minegenshin.configuration.elemental.hydro_color")
                .define("hydro_color", "#38CAFB");
        DENDRO_COLOR = builder
                .translation("minegenshin.configuration.elemental.dendro_color")
                .define("dendro_color", "#00E755");
        ELECTRO_COLOR = builder
                .translation("minegenshin.configuration.elemental.electro_color")
                .define("electro_color", "#DE9BFB");
        ANEMO_COLOR = builder
                .translation("minegenshin.configuration.elemental.anemo_color")
                .define("anemo_color", "#68FBCA");
        CYRO_COLOR = builder
                .translation("minegenshin.configuration.elemental.cyro_color")
                .define("cyro_color", "#99FBFB");
        GEO_COLOR = builder
                .translation("minegenshin.configuration.elemental.geo_color")
                .define("geo_color", "#FBCA68");
        builder.pop();

        builder.push("reaction-color");
        MELT_COLOR = builder
                .translation("minegenshin.configuration.reaction.melt")
                .define("melt_color", "#FFCC66");
        VAPORIZE_COLOR = builder
                .translation("minegenshin.configuration.reaction.vaporize")
                .define("vaporize_color", "#FFCC66");
        ELECTRO_CHARGED_COLOR = builder
                .translation("minegenshin.configuration.reaction.electro_charged")
                .define("electro_charged_color", "#DE9BFB");
        SWIRL_COLOR = builder
                .translation("minegenshin.configuration.reaction.swirl")
                .define("swirl_color", "#68FBCA");
        FROZEN_COLOR = builder
                .translation("minegenshin.configuration.elemental.frozen_color")
                .define("frozen_color", "#99FBFB");
        builder.pop();

        builder.push("lunar-stellar-color");
        LUNAR_TOP_COLOR = builder
                .translation("minegenshin.configuration.lunar_stellar.lunar_top")
                .define("lunar_top_color", "#DE9BFB");
        STELLAR_BOTTOM_WIND_COLOR = builder
                .translation("minegenshin.configuration.lunar_stellar.stellar_bottom_wind")
                .define("stellar_bottom_wind_color", "#68FBCA");
        STELLAR_BOTTOM_ICE_COLOR = builder
                .translation("minegenshin.configuration.lunar_stellar.stellar_bottom_ice")
                .define("stellar_bottom_ice_color", "#99FBFB");
        builder.pop();
    }
}