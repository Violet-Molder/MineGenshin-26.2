package com.linweiyun.genshin.content.effects.entities.instance;

import com.linweiyun.genshin.content.effects.entities.about_elemental_effect.AboutElementalEffect;
import com.linweiyun.genshin.enums.AttachmentType;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

public class MobEffectInstanceAboutElemental extends MobEffectInstance {
  public ElementalsGIM getElemental() {
    return elemental;
  }

  private float attachmentAmount;
  private final AttachmentType attachmentType;
  private int tickCounter;
  private final ElementalsGIM elemental;

  @Override
  public boolean tick(@NotNull LivingEntity entity, @NotNull Runnable onExpirationRunnable) {
    return super.tick(entity, onExpirationRunnable);
  }

  public MobEffectInstanceAboutElemental(
      DeferredHolder<MobEffect, AboutElementalEffect> effect, AttachmentType type) {
    this(effect, type, type.getInitialAmount());
  }

  public MobEffectInstanceAboutElemental(
      DeferredHolder<MobEffect, AboutElementalEffect> effect,
      AttachmentType type,
      float attachmentAmount) {
    this(effect, type, attachmentAmount, true, true, true);
  }

  public MobEffectInstanceAboutElemental(
      DeferredHolder<MobEffect, AboutElementalEffect> effect,
      AttachmentType type,
      float attachmentAmount,
      boolean ambient) {
    this(effect, type, attachmentAmount, ambient, true, true);
  }

  public MobEffectInstanceAboutElemental(
      DeferredHolder<MobEffect, AboutElementalEffect> effect,
      AttachmentType type,
      float attachmentAmount,
      boolean ambient,
      boolean visible) {
    this(effect, type, attachmentAmount, ambient, visible, true);
  }

  public MobEffectInstanceAboutElemental(
      DeferredHolder<MobEffect, AboutElementalEffect> effect,
      AttachmentType type,
      float attachmentAmount,
      boolean ambient,
      boolean visible,
      boolean showIcon) {
    super(effect, MobEffectInstance.INFINITE_DURATION, 0, ambient, visible, showIcon);
    this.attachmentType = type;
    this.attachmentAmount = attachmentAmount;
    this.tickCounter = 0;
    this.elemental = effect.get().getElemental();
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  public float getAttachmentAmount() {
    return attachmentAmount;
  }

  public AttachmentType getAttachmentType() {
    return attachmentType;
  }

  public int getTickCounter() {
    return tickCounter;
  }

  public void setAttachmentAmount(float amount) {
    this.attachmentAmount = amount;
  }

  public void addTick() {
    this.tickCounter++;
  }

  public void decayOnce() {
    attachmentAmount -= attachmentType.getDecayPer20Ticks();
  }

  public void resetTickCounter() {
    this.tickCounter = 0;
  }
}
