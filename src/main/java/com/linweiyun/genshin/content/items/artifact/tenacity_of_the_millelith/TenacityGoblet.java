package com.linweiyun.genshin.content.items.artifact.tenacity_of_the_millelith;

import com.linweiyun.genshin.content.items.artifact.type.GobletArtifact;
import com.linweiyun.genshin.core.system.registry.register.ArtifactSets;

/** 千岩牢固 · 盟誓金爵（Noble's Pledging Vessel）。 */
public class TenacityGoblet extends GobletArtifact {

    public TenacityGoblet(Properties properties) {
        super(properties);
        this.set = ArtifactSets.TENACITY_OF_THE_MILLELITH;
        this.star = 5;
    }
}
