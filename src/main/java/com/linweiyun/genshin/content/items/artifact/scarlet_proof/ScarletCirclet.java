package com.linweiyun.genshin.content.items.artifact.scarlet_proof;

import com.linweiyun.genshin.content.items.artifact.type.CircletArtifact;
import com.linweiyun.genshin.core.system.registry.register.ArtifactSets;

public class ScarletCirclet extends CircletArtifact {
    public ScarletCirclet(Properties properties) {
        super(properties);
        this.set = ArtifactSets.SCARLET_PROOF;
        this.star = 5;
    }

}
