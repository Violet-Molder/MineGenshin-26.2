package com.linweiyun.genshin.content.entities.entity.summon.field.player;

import com.linweiyun.genshin.content.entities.entity.summon.field.FieldShapeType;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.linweiyun.genshin.enums.DamageTypeEnum;
import com.linweiyun.genshin.enums.ElementalsGIM;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FieldTalismanSpirit extends PlayerField {
  private final List<Vec3> particlePositions = new ArrayList<>();
  private boolean particlesCalculated = false;
  private int damageTickCounter = 0;
  private int secondDamageTickCounter = 0;

  public FieldTalismanSpirit(EntityType<?> entityType, Level level) {
    super(entityType, level);
    SHAPE_TYPE = FieldShapeType.CYLINDER;
    HORIZONTAL_RADIUS = 6;
    VERTICAL_RADIUS = 2f;
    FIELD_UUID = 612001;
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(DURATION, 12 * 20);
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag compoundTag) {
    super.readAdditionalSaveData(compoundTag);
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag compoundTag) {
    super.addAdditionalSaveData(compoundTag);
  }

  @Override
  public void tick() {
    if (damageTickCounter % 20 == 0) {
      createCylinderRegion();
      secondDamageTickCounter = 10;
    }
    damageTickCounter++;
    if (secondDamageTickCounter >= -1) {
      secondDamageTickCounter--;
    }
    if (secondDamageTickCounter == 0) {
      createCylinderRegion();
    }

    if (!particlesCalculated) {
      calculateParticlePositions();
      particlesCalculated = true;
    }

    spawnCachedParticles();

    super.tick();
  }

  private void calculateParticlePositions() {
    double centerX = this.getX();
    double centerY = this.getY();
    double centerZ = this.getZ();
    float radius = getHorizontalRadius();
    float halfHeight = getVerticalRadius();

    int circleParticles = 32;
    for (int i = 0; i < circleParticles; i++) {
      double angle = 2 * Math.PI * i / circleParticles;
      double x = centerX + radius * Math.cos(angle);
      double z = centerZ + radius * Math.sin(angle);
      particlePositions.add(new Vec3(x, centerY + halfHeight, z));
      particlePositions.add(new Vec3(x, centerY - halfHeight, z));
    }

    int lineCount = 24;
    int steps = 7;
    for (int i = 0; i < lineCount; i++) {
      double angle = 2 * Math.PI * i / lineCount;
      double x = centerX + radius * Math.cos(angle);
      double z = centerZ + radius * Math.sin(angle);

      for (int step = 0; step <= steps; step++) {
        double y = centerY - halfHeight + (2 * halfHeight * step / steps);
        particlePositions.add(new Vec3(x, y, z));
      }
    }
  }

  private void spawnCachedParticles() {
    Level level = this.level();
    for (Vec3 pos : particlePositions) {
      level.addParticle(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 0, 0, 0);
    }
  }

  private void createCylinderRegion() {
    double centerX = this.getX();
    double centerY = this.getY();
    double centerZ = this.getZ();
    float radius = getHorizontalRadius();
    float halfHeight = getVerticalRadius();
    AABB cylinderBounds =
        new AABB(
            centerX - radius,
            centerY - halfHeight,
            centerZ - radius,
            centerX + radius,
            centerY + halfHeight,
            centerZ + radius);
    List<LivingEntity> entitiesInCylinder =
        this.level()
            .getEntitiesOfClass(LivingEntity.class, cylinderBounds, this::isEntityInCylinder);
    for (LivingEntity entity : entitiesInCylinder) {
      handleEntityInField(entity);
    }
  }

  private boolean isEntityInCylinder(LivingEntity entity) {
    double centerX = this.getX();
    double centerY = this.getY();
    double centerZ = this.getZ();
    float radius = getHorizontalRadius();
    float halfHeight = getVerticalRadius();
    Vec3 entityPos = entity.position();
    double horizontalDistanceSq =
        (entityPos.x - centerX) * (entityPos.x - centerX)
            + (entityPos.z - centerZ) * (entityPos.z - centerZ);
    double verticalDistance = Math.abs(entityPos.y - centerY);
    return horizontalDistanceSq <= radius * radius && verticalDistance <= halfHeight;
  }

  private void handleEntityInField(LivingEntity entity) {
    Player owner = this.getOwner();
    if (entity.equals(owner)) {
      return;
    }
    CombatHelper.hurt(
        owner, getCaser(), entity, DamageTypeEnum.ELEMENTAL_BURST, ElementalsGIM.CYRO, 0.7f, 10);
  }
}