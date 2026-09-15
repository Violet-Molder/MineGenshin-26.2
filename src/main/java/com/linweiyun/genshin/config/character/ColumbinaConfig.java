package com.linweiyun.genshin.config.character;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ColumbinaConfig {
    public static void register(ModConfigSpec.Builder builder) {
        builder.push("columbina");
        ColumbinaAttributeConfig.register(builder);
        ColumbinaTalentConfig.register(builder);
        builder.pop();
    }
}