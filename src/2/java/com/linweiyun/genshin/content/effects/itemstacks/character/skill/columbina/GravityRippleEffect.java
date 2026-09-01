package com.linweiyun.genshin.content.effects.itemstacks.character.skill.columbina;

import com.linweiyun.genshin.content.effects.itemstacks.IItemStackEffect;
import com.linweiyun.genshin.content.effects.itemstacks.ItemStackEffectHelper;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.linweiyun.genshin.enums.DamageTypeEnum;
import com.linweiyun.genshin.enums.ElementalsGIM;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GravityRippleEffect implements IItemStackEffect {
  private static final String LAST_TRIGGER_TICK_KEY = "last_trigger_tick";

  @Override
  public boolean onEffectTick(ItemStack stack, Player holder) {
    int currentTick = holder.tickCount;
    int lastTriggerTick = holder.getPersistentData().getInt(LAST_TRIGGER_TICK_KEY);
    var instance = ItemStackEffectHelper.getEffectInstance(stack, this);
    if (instance != null) {
      System.out.println("[GravityRipple] 剩余持续时间: " + instance.duration());
    }
    if (currentTick - lastTriggerTick >= 40) {
      List<LivingEntity> targets =
          holder
              .level()
              .getEntitiesOfClass(
                  LivingEntity.class,
                  holder.getBoundingBox().inflate(5.0),
                  entity -> entity != holder && entity.isAlive());

      if (!targets.isEmpty()) {
        for (LivingEntity target : targets) {
          CombatHelper.hurt(
              holder,
              stack,
              target,
              DamageTypeEnum.ELEMENTAL_SKILL,
              ElementalsGIM.HYDRO,
              1.0f,
              20.0f);
        }
        holder.getPersistentData().putInt(LAST_TRIGGER_TICK_KEY, currentTick);
      }
    }
    return true;
  }
}
