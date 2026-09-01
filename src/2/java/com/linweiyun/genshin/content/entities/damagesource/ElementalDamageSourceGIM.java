package com.linweiyun.genshin.content.entities.damagesource;

import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ElementalDamageSourceGIM extends DamageSource {

  private final Holder<DamageType> type;
  @Nullable private final Entity causingEntity;
  @Nullable private final Entity directEntity;
  @Nullable private final Vec3 damageSourcePosition;
  private ElementalsGIM elemental;

  public ElementalDamageSourceGIM(
      Holder<DamageType> type,
      @Nullable Entity directEntity,
      @Nullable Entity causingEntity,
      @Nullable Vec3 damageSourcePosition) {
    super(type, directEntity, causingEntity, damageSourcePosition);
    this.type = type;
    this.causingEntity = causingEntity;
    this.directEntity = directEntity;
    this.damageSourcePosition = damageSourcePosition;
  }

  public ElementalDamageSourceGIM(
      Holder<DamageType> type, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
    this(type, directEntity, causingEntity, null);
  }

  public ElementalDamageSourceGIM(Holder<DamageType> type, Vec3 damageSourcePosition) {
    this(type, (Entity) null, null, damageSourcePosition);
  }

  public ElementalDamageSourceGIM(Holder<DamageType> type, @Nullable Entity entity) {
    this(type, entity, entity);
  }

  public ElementalDamageSourceGIM(Holder<DamageType> type) {
    this(type, (Entity) null, (Entity) null, (Vec3) null);
  }

  public void setElemental(ElementalsGIM elemental) {
    this.elemental = elemental;
  }

  public ElementalsGIM getElement() {
    return elemental;
  }

  public @Nullable Vec3 getDamageSourcePosition() {
    return damageSourcePosition;
  }

  @Override
  public @Nullable Entity getDirectEntity() {
    return directEntity;
  }

  public @Nullable Entity getCausingEntity() {
    return causingEntity;
  }

  public Holder<DamageType> getType() {
    return type;
  }
}
