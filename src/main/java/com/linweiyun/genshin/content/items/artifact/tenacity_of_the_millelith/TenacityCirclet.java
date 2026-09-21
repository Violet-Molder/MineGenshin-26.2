package com.linweiyun.genshin.content.items.artifact.tenacity_of_the_millelith;

import com.linweiyun.genshin.content.items.artifact.type.CircletArtifact;
import com.linweiyun.genshin.core.system.registry.register.ArtifactSets;

/** 千岩牢固 · 将帅兜鍪（General's Ancient Helm）。 */
public class TenacityCirclet extends CircletArtifact {

    public TenacityCirclet(Properties properties) {
        super(properties);
        this.set = ArtifactSets.TENACITY_OF_THE_MILLELITH;
        this.star = 5;
    }
}
