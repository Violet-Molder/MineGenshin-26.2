package com.linweiyun.genshin.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ArtifactConfig {

    //TEMP 共享的顶层 builder，三个子配置类都在这个 builder 下 push 自己的子节点
    private static final ModConfigSpec.Builder ARTIFACT_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec ARTIFACT_SPEC;

    static {
        //TEMP 依次让三个子配置类在共享 builder 上注册各自的节点
        ArtifactLevelConfig.register(ARTIFACT_BUILDER);
        ArtifactMainStatConfig.register(ARTIFACT_BUILDER);
        ArtifactSubStatConfig.register(ARTIFACT_BUILDER);

        ARTIFACT_SPEC = ARTIFACT_BUILDER.build();
    }
}