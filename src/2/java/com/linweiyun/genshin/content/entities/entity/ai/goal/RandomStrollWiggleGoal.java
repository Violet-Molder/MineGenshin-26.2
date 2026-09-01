package com.linweiyun.genshin.content.entities.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

public class RandomStrollWiggleGoal extends Goal {
  public static final int DEFAULT_INTERVAL = 120;
  public static final int WAIT_TICKS = 10;

  protected final PathfinderMob mob;
  protected double wantedX;
  protected double wantedY;
  protected double wantedZ;
  protected final double speedModifier;
  protected int interval;
  protected boolean forceTrigger;
  private final boolean checkNoActionTime;
  private WiggleMoveState moveState = WiggleMoveState.IDLE;
  private int waitTickCounter;

  public RandomStrollWiggleGoal(PathfinderMob mob, double speedModifier) {
    this(mob, speedModifier, 120);
  }

  public RandomStrollWiggleGoal(PathfinderMob mob, double speedModifier, int interval) {
    this(mob, speedModifier, interval, true);
  }

  public RandomStrollWiggleGoal(
      PathfinderMob mob, double speedModifier, int interval, boolean checkNoActionTime) {
    this.mob = mob;
    this.speedModifier = speedModifier;
    this.interval = interval;
    this.checkNoActionTime = checkNoActionTime;
    this.setFlags(EnumSet.of(Flag.MOVE));
  }

  @Override
  public boolean canUse() {
    if (this.mob.hasControllingPassenger()) {
      return false;
    } else {
      if (!this.forceTrigger) {
        if (this.checkNoActionTime && this.mob.getNoActionTime() >= 100) {
          return false;
        }
        if (this.mob.getRandom().nextInt(this.reducedTickDelay(this.interval)) != 0) {
          return false;
        }
      }

      Vec3 vec3 = this.getPosition();
      if (vec3 == null) {
        return false;
      } else {
        this.wantedX = vec3.x;
        this.wantedY = vec3.y;
        this.wantedZ = vec3.z;
        this.forceTrigger = false;
        return true;
      }
    }
  }

  @Override
  public boolean canContinueToUse() {
    if (this.mob.hasControllingPassenger()) {
      return false;
    }

    PathNavigation navigation = this.mob.getNavigation();
    return switch (this.moveState) {
      case MOVING -> !navigation.isDone();
      case WAITING -> this.waitTickCounter < WAIT_TICKS;
      default -> false;
    };
  }

  @Override
  public void start() {
    super.start();
    this.moveState = WiggleMoveState.MOVING;
    this.waitTickCounter = 0;
    this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
  }

  @Override
  public void tick() {
    super.tick();
    PathNavigation navigation = this.mob.getNavigation();
    if (this.moveState == WiggleMoveState.MOVING && navigation.isDone()) {
      this.moveState = WiggleMoveState.WAITING;
      this.waitTickCounter = 0;
      navigation.stop();
    }
    if (this.moveState == WiggleMoveState.WAITING) {
      this.waitTickCounter++;
    }
  }

  @Override
  public void stop() {
    super.stop();
    this.mob.getNavigation().stop();
    this.moveState = WiggleMoveState.IDLE;
    this.waitTickCounter = 0;
  }

  @Nullable
  protected Vec3 getPosition() {
    return net.minecraft.world.entity.ai.util.DefaultRandomPos.getPos(this.mob, 10, 7);
  }

  protected static int reducedTickDelay(int reduction) {
    return Mth.positiveCeilDiv(reduction, 2);
  }

  public void trigger() {
    this.forceTrigger = true;
  }

  public void setInterval(int newchance) {
    this.interval = newchance;
  }

  private enum WiggleMoveState {
    IDLE,
    MOVING,
    WAITING
  }
}
