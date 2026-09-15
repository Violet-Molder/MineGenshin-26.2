package com.linweiyun.genshin.config.artifact;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ArtifactConfig {
    public static void register(ModConfigSpec.Builder builder) {
        ArtifactLevelConfig.register(builder);
        ArtifactMainStatConfig.register(builder);
        ArtifactSubStatConfig.register(builder);
    }
}