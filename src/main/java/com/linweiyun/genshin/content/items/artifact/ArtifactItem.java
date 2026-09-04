package com.linweiyun.genshin.content.items.artifact;

import com.linweiyun.genshin.content.items.TeyvatItem;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ArtifactItem extends TeyvatItem {
    protected ArtifactType type;
    protected DeferredHolder<ArtifactSet, ArtifactSet> set;
    public ArtifactItem(Properties properties) {
        super(properties);
    }

    public ArtifactType getType() {return type;}

    public DeferredHolder<ArtifactSet, ArtifactSet> getSet() {return set;}
}
