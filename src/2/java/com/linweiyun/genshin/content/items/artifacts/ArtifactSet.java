package com.linweiyun.genshin.content.items.artifacts;

import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public abstract class ArtifactSet {
  Player player;
  private Component setName;
  private Component twoPieceEffectText;
  private Component fourPieceEffectText;
  private boolean twoPieceActive;
  private boolean fourPieceActive;
  protected Map<ArtifactSlot, Artifact> equippedArtifacts;

  public void twoPieceActiveEffect() {}

  public void fourPieceActiveEffect() {}

  public Component getSetName() {
    return setName;
  }

  public Component getTwoPieceEffectText() {
    return twoPieceEffectText;
  }

  public Component getFourPieceEffectText() {
    return fourPieceEffectText;
  }

  public void setTwoPieceActive(boolean twoPieceActive) {
    this.twoPieceActive = twoPieceActive;
  }

  public void setFourPieceActive(boolean fourPieceActive) {
    this.fourPieceActive = fourPieceActive;
  }

  public boolean isTwoPieceActive() {
    return twoPieceActive;
  }

  public boolean isFourPieceActive() {
    return fourPieceActive;
  }
}
