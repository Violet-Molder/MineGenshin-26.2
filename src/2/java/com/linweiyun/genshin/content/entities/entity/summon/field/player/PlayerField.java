package com.linweiyun.genshin.content.entities.entity.summon.field.player;

import com.linweiyun.genshin.content.entities.entity.summon.PlayerSummon;
import com.linweiyun.genshin.content.entities.entity.summon.field.MGField;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public abstract class PlayerField extends MGField implements PlayerSummon {
  private static final EntityDataAccessor<ItemStack> caster =
      SynchedEntityData.defineId(PlayerField.class, EntityDataSerializers.ITEM_STACK);
  private static final EntityDataAccessor<String> ownerUUID =
      SynchedEntityData.defineId(PlayerField.class, EntityDataSerializers.STRING);

  public PlayerField(EntityType<?> entityType, Level level) {
    super(entityType, level);
  }

  @Override
  public ItemStack getCaser() {
    return this.entityData.get(caster);
  }

  @Override
  public Player getOwner() {
    return this.level().getPlayerByUUID(UUID.fromString(this.entityData.get(ownerUUID)));
  }

  @Override
  public void setCaser(ItemStack caser) {
    this.entityData.set(caster, caser);
  }

  @Override
  public void setOwner(Player owner) {
    this.entityData.set(ownerUUID, owner.getStringUUID());
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(caster, ItemStack.EMPTY);
    builder.define(ownerUUID, "");
  }

  @Override
  protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {}

  @Override
  protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {}
}
