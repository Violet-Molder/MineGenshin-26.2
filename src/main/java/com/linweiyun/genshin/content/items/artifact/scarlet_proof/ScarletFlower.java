package com.linweiyun.genshin.content.items.artifact.scarlet_proof;

import com.linweiyun.genshin.content.items.artifact.type.FlowerArtifact;
import com.linweiyun.genshin.core.system.registry.register.ArtifactSets;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class ScarletFlower extends FlowerArtifact {
    private static final Logger LOGGER = LogUtils.getLogger();
    public ScarletFlower(Properties properties) {
        super(properties);
        this.set = ArtifactSets.SCARLET_PROOF;
        this.star = 5;
    }
}
