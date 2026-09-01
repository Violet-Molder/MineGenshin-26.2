package com.linweiyun.genshin.content.entities.entity.summon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface PlayerSummon {
  Player getOwner();

  ItemStack getCaser();

  void setOwner(Player owner);

  void setCaser(ItemStack caser);
}
