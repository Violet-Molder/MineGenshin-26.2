package com.linweiyun.genshin.content.items.artifacts;

import net.minecraft.world.item.Item;

public class Artifact extends Item {
  private final ArtifactSet artifactSet;

  public Artifact(Properties properties, ArtifactSet artifactSet) {
    super(properties);
    this.artifactSet = artifactSet;
  }

  public ArtifactSet getArtifactSet() {
    return artifactSet;
  }
}
