package com.linweiyun.genshin.content.effects.entities.elemental_reaction;

import com.linweiyun.genshin.mixin_interfaces.IEntityNoAIAccessor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class FrozenEffect extends MobEffect {
  public FrozenEffect(MobEffectCategory category, int color) {
    super(category, color);
  }

  @Override
  public boolean applyEffectTick(@NotNull LivingEntity livingEntity, int amplifier) {

    return true;
  }

  @Override
  public void onEffectAdded(@NotNull LivingEntity livingEntity, int amplifier) {

    if (livingEntity instanceof Player player) {
      player.getAbilities().mayfly = false;
      player.onUpdateAbilities();
    } else {
      if (livingEntity instanceof IEntityNoAIAccessor accessor) {
        accessor.setNoAi(true);
      }
    }

    super.onEffectAdded(livingEntity, amplifier);
  }

  @Override
  public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
    return true;
  }
}
