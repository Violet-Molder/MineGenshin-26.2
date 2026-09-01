package com.linweiyun.genshin.content.effects.itemstacks;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IItemStackEffect extends IPersistedSerializable {

  default void onEffectAdded(ItemStack stack, Player holder) {}

  default void onEffectRemoved(ItemStack stack, Player holder) {}

  default boolean onEffectFrontTick(ItemStack stack, Player holder) {
    return true;
  }

  default boolean onEffectBackTick(ItemStack stack, Player holder) {
    return true;
  }

  default boolean onEffectTick(ItemStack stack, Player holder) {
    return true;
  }

  default void onAttacked(ItemStack stack, Player holder, Player target) {}
  ;

  default boolean isInstantaneous() {
    return false;
  }
}
