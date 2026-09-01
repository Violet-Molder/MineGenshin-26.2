package com.linweiyun.genshin.mixins;

import com.linweiyun.genshin.mixin_interfaces.IDamageContainerAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageContainer.class)
public class DamageContainerMixin implements IDamageContainerAccessor {
  @Final @Shadow private DamageSource source;

  private DamageSource modifiedSource;

  public void setModifiedSource(DamageSource newSource) {
    this.modifiedSource = newSource;
  }

  @Inject(method = "getSource", at = @At("HEAD"), cancellable = true)
  public void getSource(CallbackInfoReturnable<DamageSource> cir) {
    cir.setReturnValue(this.modifiedSource != null ? this.modifiedSource : this.source);
  }
}
