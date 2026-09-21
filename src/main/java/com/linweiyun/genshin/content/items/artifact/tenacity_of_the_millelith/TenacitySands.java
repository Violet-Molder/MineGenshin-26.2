package com.linweiyun.genshin.content.items.artifact.tenacity_of_the_millelith;

import com.linweiyun.genshin.content.items.artifact.type.SandsArtifact;
import com.linweiyun.genshin.core.system.registry.register.ArtifactSets;

/** 千岩牢固 · 金铜时晷（Orichalceous Time-Dial）。 */
public class TenacitySands extends SandsArtifact {

    public TenacitySands(Properties properties) {
        super(properties);
        this.set = ArtifactSets.TENACITY_OF_THE_MILLELITH;
        this.star = 5;
    }
}
