package com.linweiyun.genshin.config.character;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ArlecchinoConfig {
    public static void register(ModConfigSpec.Builder builder) {
        builder.push("arlecchino");
        ArlecchinoAttributeConfig.register(builder);
        ArlecchinoTalentConfig.register(builder);
        builder.pop();
    }
}