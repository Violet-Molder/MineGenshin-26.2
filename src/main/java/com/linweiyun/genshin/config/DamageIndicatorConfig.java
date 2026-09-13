package com.linweiyun.genshin.config;

import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

public class DamageIndicatorConfig {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ========== 元素伤害颜色 ==========
    public static final ModConfigSpec.ConfigValue<String> COLOR_PYRO;
    public static final ModConfigSpec.ConfigValue<String> COLOR_CYRO;
    public static final ModConfigSpec.ConfigValue<String> COLOR_HYDRO;
    public static final ModConfigSpec.ConfigValue<String> COLOR_ANEMO;
    public static final ModConfigSpec.ConfigValue<String> COLOR_ELECTRO;
    public static final ModConfigSpec.ConfigValue<String> COLOR_DENDRO;
    public static final ModConfigSpec.ConfigValue<String> COLOR_GEO;
    public static final ModConfigSpec.ConfigValue<String> COLOR_PHYSICAL;

    // ========== 元素反应颜色 ==========
    // 增幅反应
    public static final ModConfigSpec.ConfigValue<String> COLOR_MELT;
    public static final ModConfigSpec.ConfigValue<String> COLOR_VAPORIZE;
    // 聚变反应
    public static final ModConfigSpec.ConfigValue<String> COLOR_SHATTERED;
    public static final ModConfigSpec.ConfigValue<String> COLOR_SUPERCONDUCT;
    public static final ModConfigSpec.ConfigValue<String> COLOR_SWIRL;
    public static final ModConfigSpec.ConfigValue<String> COLOR_ELECTRO_CHARGED;
    public static final ModConfigSpec.ConfigValue<String> COLOR_OVERLOAD;
    public static final ModConfigSpec.ConfigValue<String> COLOR_BURNING;
    // 绽放系
    public static final ModConfigSpec.ConfigValue<String> COLOR_BLOOM;
    public static final ModConfigSpec.ConfigValue<String> COLOR_HYPERBLOOM;
    public static final ModConfigSpec.ConfigValue<String> COLOR_BURGEON;
    // 激化系
    public static final ModConfigSpec.ConfigValue<String> COLOR_QUICKEN;
    public static final ModConfigSpec.ConfigValue<String> COLOR_AGGRAVATE;
    public static final ModConfigSpec.ConfigValue<String> COLOR_SPREAD;
    // 特殊反应
    public static final ModConfigSpec.ConfigValue<String> COLOR_FROZEN;
    public static final ModConfigSpec.ConfigValue<String> COLOR_CRYSTALLIZE;

    public static final ModConfigSpec DAMAGE_INDICATOR_SPEC;

    static {
        LOGGER.info("[DI-Config] static init begin");

        // ========== 元素伤害颜色 ==========
        BUILDER.push("damage_indicator_element_colors");

        COLOR_PYRO     = BUILDER.comment("火元素伤害颜色 (十六进制)").define("pyro", "#FB9B00");
        COLOR_CYRO     = BUILDER.comment("冰元素伤害颜色 (十六进制)").define("cyro", "#99FBFB");
        COLOR_HYDRO    = BUILDER.comment("水元素伤害颜色 (十六进制)").define("hydro", "#38CAFB");
        COLOR_ANEMO    = BUILDER.comment("风元素伤害颜色 (十六进制)").define("anemo", "#68FBCA");
        COLOR_ELECTRO  = BUILDER.comment("雷元素伤害颜色 (十六进制)").define("electro", "#DE9BFB");
        COLOR_DENDRO   = BUILDER.comment("草元素伤害颜色 (十六进制)").define("dendro", "#00E755");
        COLOR_GEO      = BUILDER.comment("岩元素伤害颜色 (十六进制)").define("geo", "#FBCA68");
        COLOR_PHYSICAL = BUILDER.comment("物理/真实伤害颜色 (十六进制)").define("physical", "#FFFFFF");

        BUILDER.pop();

        // ========== 元素反应颜色 ==========
        BUILDER.push("damage_indicator_reaction_colors");

        // 增幅反应
        COLOR_MELT    = BUILDER.comment("融化反应颜色 (十六进制)").define("melt", "#FFCC66");
        COLOR_VAPORIZE = BUILDER.comment("蒸发反应颜色 (十六进制)").define("vaporize", "#FFCC66");

        // 聚变反应
        COLOR_SHATTERED       = BUILDER.comment("碎冰反应颜色 (十六进制)").define("shattered", "#BBEFFF");
        COLOR_SUPERCONDUCT    = BUILDER.comment("超导反应颜色 (十六进制)").define("superconduct", "#B39BFF");
        COLOR_SWIRL           = BUILDER.comment("扩散反应颜色 (十六进制)").define("swirl", "#7FFFD4");
        COLOR_ELECTRO_CHARGED = BUILDER.comment("感电反应颜色 (十六进制)").define("electro_charged", "#D48BFF");
        COLOR_OVERLOAD        = BUILDER.comment("超载反应颜色 (十六进制)").define("overload", "#FF809B");
        COLOR_BURNING         = BUILDER.comment("燃烧反应颜色 (十六进制)").define("burning", "#FF6633");

        // 绽放系
        COLOR_BLOOM      = BUILDER.comment("绽放反应颜色 (十六进制)").define("bloom", "#8FE060");
        COLOR_HYPERBLOOM = BUILDER.comment("超绽放反应颜色 (十六进制)").define("hyperbloom", "#C77BFF");
        COLOR_BURGEON    = BUILDER.comment("烈绽放反应颜色 (十六进制)").define("burgeon", "#FFB03A");

        // 激化系
        COLOR_QUICKEN  = BUILDER.comment("原激化反应颜色 (十六进制)").define("quicken", "#D4E85B");
        COLOR_AGGRAVATE = BUILDER.comment("超激化反应颜色 (十六进制)").define("aggravate", "#B7E85B");
        COLOR_SPREAD   = BUILDER.comment("蔓激化反应颜色 (十六进制)").define("spread", "#62C93F");

        // 特殊反应
        COLOR_FROZEN      = BUILDER.comment("冻结反应颜色 (十六进制)").define("frozen", "#AEE0FF");
        COLOR_CRYSTALLIZE = BUILDER.comment("结晶反应颜色 (十六进制)").define("crystallize", "#FFD966");

        BUILDER.pop();

        DAMAGE_INDICATOR_SPEC = BUILDER.build();
        LOGGER.info("[DI-Config] static init done");
    }

    // =====================================================================
    //  颜色解析
    // =====================================================================

    /** 将 "#RRGGBB" 解析为 ARGB 整数颜色值 */
    public static int parseColor(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            return 0xFF000000 | Integer.parseUnsignedInt(h, 16);
        } catch (Exception e) {
            LOGGER.error("[DI-Config] parseColor failed for hex='{}'", hex, e);
            return 0xFFFFFFFF;
        }
    }

    /** 按元素获取颜色（类元素自动回退到主元素） */
    public static int getColorForElement(GenshinElement element) {
        if (element == null) {
            LOGGER.warn("[DI-Config] getColorForElement: element is null, using physical color");
            return safeGet(COLOR_PHYSICAL, 0xFFFFFFFF);
        }

        GenshinElement main = element.getMainElement();
        try {
            int color;
            if (main == ModElements.PYRO.get()) {
                color = parseColor(COLOR_PYRO.get());
            } else if (main == ModElements.CYRO.get()) {
                color = parseColor(COLOR_CYRO.get());
            } else if (main == ModElements.HYDRO.get()) {
                color = parseColor(COLOR_HYDRO.get());
            } else if (main == ModElements.ANEMO.get()) {
                color = parseColor(COLOR_ANEMO.get());
            } else if (main == ModElements.ELECTRO.get()) {
                color = parseColor(COLOR_ELECTRO.get());
            } else if (main == ModElements.DENDRO.get()) {
                color = parseColor(COLOR_DENDRO.get());
            } else if (main == ModElements.GEO.get()) {
                color = parseColor(COLOR_GEO.get());
            } else {
                color = parseColor(COLOR_PHYSICAL.get());
            }
            LOGGER.info("[DI-Config] getColorForElement: element={} main={} color=0x{}",
                    element.getId(), main.getId(), Integer.toHexString(color));
            return color;
        } catch (Exception e) {
            LOGGER.error("[DI-Config] getColorForElement failed for element={}", element.getId(), e);
            return 0xFFFFFFFF;
        }
    }

    /** 按反应类型获取颜色 */
    public static int getColorForReaction(ElementalReactionType type) {
        if (type == null) {
            LOGGER.warn("[DI-Config] getColorForReaction: type is null, using physical color");
            return safeGet(COLOR_PHYSICAL, 0xFFFFFFFF);
        }

        try {
            int color = switch (type) {
                // 增幅
                case MELT            -> parseColor(COLOR_MELT.get());
                case VAPORIZE        -> parseColor(COLOR_VAPORIZE.get());
                // 聚变
                case SHATTERED       -> parseColor(COLOR_SHATTERED.get());
                case SUPERCONDUCT    -> parseColor(COLOR_SUPERCONDUCT.get());
                case SWIRL           -> parseColor(COLOR_SWIRL.get());
                case ELECTRO_CHARGED -> parseColor(COLOR_ELECTRO_CHARGED.get());
                case OVERLOAD        -> parseColor(COLOR_OVERLOAD.get());
                case BURNING         -> parseColor(COLOR_BURNING.get());
                // 绽放
                case BLOOM           -> parseColor(COLOR_BLOOM.get());
                case HYPERBLOOM      -> parseColor(COLOR_HYPERBLOOM.get());
                case BURGEON         -> parseColor(COLOR_BURGEON.get());
                // 激化
                case QUICKEN         -> parseColor(COLOR_QUICKEN.get());
                case AGGRAVATE       -> parseColor(COLOR_AGGRAVATE.get());
                case SPREAD          -> parseColor(COLOR_SPREAD.get());
                // 特殊
                case FROZEN          -> parseColor(COLOR_FROZEN.get());
                case CRYSTALLIZE     -> parseColor(COLOR_CRYSTALLIZE.get());
            };
            LOGGER.info("[DI-Config] getColorForReaction: type={} color=0x{}",
                    type, Integer.toHexString(color));
            return color;
        } catch (Exception e) {
            LOGGER.error("[DI-Config] getColorForReaction failed for type={}", type, e);
            return 0xFFFFFFFF;
        }
    }

    private static int safeGet(ModConfigSpec.ConfigValue<String> v, int fallback) {
        try { return parseColor(v.get()); } catch (Exception e) { return fallback; }
    }
}