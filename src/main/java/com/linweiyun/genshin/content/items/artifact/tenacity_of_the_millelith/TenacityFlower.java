package com.linweiyun.genshin.content.items.artifact.tenacity_of_the_millelith;

import com.linweiyun.genshin.content.items.artifact.type.FlowerArtifact;
import com.linweiyun.genshin.core.system.registry.register.ArtifactSets;

/** 千岩牢固 · 勋绩之花（Flower of Accolades）。 */
public class TenacityFlower extends FlowerArtifact {

    public TenacityFlower(Properties properties) {
        super(properties);
        this.set = ArtifactSets.TENACITY_OF_THE_MILLELITH;
        this.star = 5;
    }
}
