package com.linweiyun.genshin.content.effects.itemstacks.character.skill;

import com.linweiyun.genshin.content.effects.itemstacks.IItemStackEffect;
import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RegenerationEffect implements IItemStackEffect {
  public static final RegenerationEffect INSTANCE = new RegenerationEffect();

  @Override
  public void onEffectAdded(ItemStack stack, Player holder) {}

  @Override
  public boolean onEffectFrontTick(ItemStack stack, Player holder) {

    if (holder.tickCount % 40 == 0) {
      PlayerCharacterData data = CombatHelper.getCharacterData(stack);
      if (data != null) {
        data.changeData(
            stack,
            playerData -> playerData.healHP(playerData.getMaxHP().getTotalValue(true) * 0.05),
            (Player) holder);
      }
    }
    return true;
  }

  @Override
  public boolean onEffectBackTick(ItemStack stack, Player holder) {
    if (holder.tickCount % 40 == 0) {
      PlayerCharacterData data = CombatHelper.getCharacterData(stack);
      if (data != null) {
        data.changeData(
            stack,
            playerData -> playerData.healHP(playerData.getMaxHP().getTotalValue(true) * 0.05),
            (Player) holder);
      }
    }
    return true;
  }

  @Override
  public void onEffectRemoved(ItemStack stack, Player holder) {}
}
