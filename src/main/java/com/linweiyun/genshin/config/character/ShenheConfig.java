package com.linweiyun.genshin.config.character;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ShenheConfig {
    public static void register(ModConfigSpec.Builder builder) {
        builder.push("shenhe");
        ShenheAttributeConfig.register(builder);
        ShenheTalentConfig.register(builder);
        builder.pop();
    }
}