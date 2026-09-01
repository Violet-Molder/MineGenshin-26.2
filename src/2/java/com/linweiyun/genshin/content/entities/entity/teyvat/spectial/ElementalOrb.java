package com.linweiyun.genshin.content.entities.entity.teyvat.spectial;

import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.core.events.custom.EleOrbTargetingEvent;
import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

public class ElementalOrb extends Entity {
  private Player followingPlayer;
  public static final EntityDataAccessor<Float> DATA_VALUE =
      SynchedEntityData.defineId(ElementalOrb.class, EntityDataSerializers.FLOAT);
  public static final EntityDataAccessor<String> DATA_ELEMENTAL =
      SynchedEntityData.defineId(ElementalOrb.class, EntityDataSerializers.STRING);

  public ElementalOrb(EntityType<? extends Entity> entityEntityType, Level level) {
    super(entityEntityType, level);
    this.getEntityData().set(DATA_VALUE, 1.2f);
    this.getEntityData().set(DATA_ELEMENTAL, ElementalsGIM.CYRO.getId());
  }

  protected Entity.@NotNull MovementEmission getMovementEmission() {
    return MovementEmission.NONE;
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(DATA_VALUE, 1.2f);
    builder.define(DATA_ELEMENTAL, ElementalsGIM.CYRO.getId());
  }

  @Override
  protected double getDefaultGravity() {
    return 0.03;
  }

  @Override
  public void tick() {
    super.tick();
    this.xo = this.getX();
    this.yo = this.getY();
    this.zo = this.getZ();
    if (this.isEyeInFluid(FluidTags.WATER)) {
      this.setUnderwaterMovement();
    } else {
      this.applyGravity();
    }

    if (this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA)) {
      this.setDeltaMovement(
          (this.random.nextFloat() - this.random.nextFloat()) * 0.2F,
          0.2F,
          (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
    }

    if (!this.level().noCollision(this.getBoundingBox())) {
      this.moveTowardsClosestSpace(
          this.getX(),
          (this.getBoundingBox().minY + this.getBoundingBox().maxY) / (double) 2.0F,
          this.getZ());
    }

    if (this.tickCount % 20 == 1) {
      this.scanForEntities();
    }

    if (this.followingPlayer != null
        && (this.followingPlayer.isSpectator() || this.followingPlayer.isDeadOrDying())) {
      this.followingPlayer = null;
    }

    if (this.followingPlayer != null) {
      Vec3 vec3 =
          new Vec3(
              this.followingPlayer.getX() - this.getX(),
              this.followingPlayer.getY()
                  + (double) this.followingPlayer.getEyeHeight() / (double) 2.0F
                  - this.getY(),
              this.followingPlayer.getZ() - this.getZ());
      double d0 = vec3.lengthSqr();
      if (d0 < (double) 64.0F) {
        double d1 = (double) 1.0F - Math.sqrt(d0) / (double) 8.0F;
        this.setDeltaMovement(this.getDeltaMovement().add(vec3.normalize().scale(d1 * d1 * 0.1)));
      }
    }

    this.move(MoverType.SELF, this.getDeltaMovement());
    float f = 0.98F;
    if (this.onGround()) {
      BlockPos pos = this.getBlockPosBelowThatAffectsMyMovement();
      f = this.level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98F;
    }

    this.setDeltaMovement(this.getDeltaMovement().multiply(f, 0.98, f));
    if (this.onGround()) {
      this.setDeltaMovement(this.getDeltaMovement().multiply(1.0F, -0.9, 1.0F));
    }
  }

  public @NotNull BlockPos getBlockPosBelowThatAffectsMyMovement() {
    return this.getOnPos(0.999999F);
  }

  private void scanForEntities() {
    if (this.followingPlayer == null || this.followingPlayer.distanceToSqr(this) > (double) 64.0F) {
      this.followingPlayer =
          NeoForge.EVENT_BUS.post(new EleOrbTargetingEvent(this, 8.0F)).getFollowingPlayer();
    }
  }

  private void setUnderwaterMovement() {
    Vec3 vec3 = this.getDeltaMovement();
    this.setDeltaMovement(
        vec3.x * (double) 0.99F,
        Math.min(vec3.y + (double) 5.0E-4F, 0.06F),
        vec3.z * (double) 0.99F);
  }

  public float getValue() {
    return this.getEntityData().get(DATA_VALUE);
  }

  public ElementalsGIM getElemental() {
    String elementalId = this.getEntityData().get(DATA_ELEMENTAL);
    return ElementalsGIM.getElementById(elementalId);
  }

  public void setValue(float value) {
    this.getEntityData().set(DATA_VALUE, value);
  }

  public void setElemental(ElementalsGIM elemental) {
    this.getEntityData().set(DATA_ELEMENTAL, elemental.getId());
  }

  @Override
  public void playerTouch(Player player) {
    if (!player.level().isClientSide) {
      if (player.takeXpDelay == 0) {

        player.takeXpDelay = 2;
        player.take(this, 1);
        addElementalEnergy(player);
        this.discard();
      }
    }
  }

  @Override
  protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {}

  @Override
  protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {}

  private void addElementalEnergy(Player player) {
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    ItemStack stack = characterParty.getCurrentCharacter();
    PlayerCharacterData data = CombatHelper.getCharacterData(stack);
    if (data != null && stack.getItem() instanceof PlayerCharacter playerCharacter) {
      data.changeData(
          stack,
          characterData ->
              characterData.setCurrentObtainingEnergy(
                  Math.min(
                      data.getCurrentObtainingEnergy() + this.getValue(),
                      playerCharacter.getMaxObtainingEnergy())),
          player);
    }
  }
}
