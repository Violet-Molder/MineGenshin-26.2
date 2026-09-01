package com.linweiyun.genshin.core.events.custom;

import java.util.Optional;
import javax.annotation.Nullable;

import com.linweiyun.genshin.content.entities.entity.teyvat.spectial.ElementalOrb;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class EleOrbTargetingEvent extends Event {
  private final ElementalOrb eleOrb;
  private final double scanDistance;
  private Optional<Player> followingPlayer = Optional.empty();

  public EleOrbTargetingEvent(ElementalOrb eleOrb, double scanDistance) {
    this.eleOrb = eleOrb;
    this.scanDistance = scanDistance;
  }

  public @Nullable Player getFollowingPlayer() {
    return this.followingPlayer.isPresent()
        ? this.followingPlayer.orElse(null)
        : this.eleOrb.level().getNearestPlayer(this.eleOrb, this.scanDistance);
  }

  public void setFollowingPlayer(@Nullable Player newFollowingPlayer) {
    this.followingPlayer = Optional.ofNullable(newFollowingPlayer);
  }

  public ElementalOrb getEleOrb() {
    return this.eleOrb;
  }

  public double getScanDistance() {
    return this.scanDistance;
  }
}
