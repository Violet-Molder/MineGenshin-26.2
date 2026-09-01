package com.linweiyun.genshin.content.entities.entity.summon.field;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class MGField extends Entity {
  protected FieldShapeType SHAPE_TYPE;
  protected float HORIZONTAL_RADIUS = 0;
  protected float VERTICAL_RADIUS = 0;
  protected int FIELD_UUID = 0;
  protected static final EntityDataAccessor<Integer> DURATION =
      SynchedEntityData.defineId(MGField.class, EntityDataSerializers.INT);

  public MGField(EntityType<?> entityType, Level level) {
    super(entityType, level);
  }

  @Override
  public void tick() {
    super.tick();
    if (this.entityData.get(DURATION) > 0) {
      this.entityData.set(DURATION, this.entityData.get(DURATION) - 1);
      if (this.entityData.get(DURATION) <= 0) {
        this.discard();
      }
    }
  }

  public FieldShapeType getShapeType() {
    return SHAPE_TYPE;
  }

  public float getHorizontalRadius() {
    return HORIZONTAL_RADIUS;
  }

  public float getVerticalRadius() {
    return VERTICAL_RADIUS;
  }

  public int getFieldUUID() {
    return FIELD_UUID;
  }
}