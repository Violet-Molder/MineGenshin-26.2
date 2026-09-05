package com.linweiyun.genshin.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ArtifactSubStatConfig {

    //TEMP 由 ArtifactConfig 主入口在共享 builder 上调用，预留副词条配置节点
    public static void register(ModConfigSpec.Builder builder) {
        builder.push("subStat");

        //TEMP 副词条配置后续补充，目前为空

        builder.pop(); // subStat pop
    }
}