package com.linweiyun.genshin.mixin_interfaces;

import net.minecraft.world.damagesource.DamageSource;

public interface IDamageContainerAccessor {
  void setModifiedSource(DamageSource source);
}
