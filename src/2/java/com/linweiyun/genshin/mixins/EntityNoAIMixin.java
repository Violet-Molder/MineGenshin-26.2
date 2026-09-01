package com.linweiyun.genshin.mixins;

import com.linweiyun.genshin.mixin_interfaces.IEntityNoAIAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityNoAIMixin implements IEntityNoAIAccessor {

  private boolean noAi;

  @Override
  public boolean getNoAi() {
    return noAi;
  }

  @Override
  public void setNoAi(boolean noAi) {
    this.noAi = noAi;
  }

  @Inject(method = "saveWithoutId", at = @At("TAIL"))
  private void addNoAISave(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
    compound.putBoolean("NoAI", this.noAi);
  }

  @Inject(method = "load", at = @At("TAIL"))
  private void addNoAILoad(CompoundTag compound, CallbackInfo ci) {
    if (compound.contains("NoAI")) {
      this.noAi = compound.getBoolean("NoAI");
    }
  }
}
