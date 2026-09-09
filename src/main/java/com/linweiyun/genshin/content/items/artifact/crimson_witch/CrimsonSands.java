package com.linweiyun.genshin.content.items.artifact.crimson_witch;

import com.linweiyun.genshin.content.items.artifact.type.SandsArtifact;
import com.linweiyun.genshin.core.system.registry.register.ArtifactSets;

public class CrimsonSands extends SandsArtifact {
    public CrimsonSands(Properties properties) {
        super(properties);
        this.set = ArtifactSets.CRIMSON_WITCH;
        this.star = 5;
    }
}
