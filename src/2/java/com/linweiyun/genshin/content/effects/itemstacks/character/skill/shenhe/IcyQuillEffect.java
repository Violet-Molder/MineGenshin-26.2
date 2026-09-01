package com.linweiyun.genshin.content.effects.itemstacks.character.skill.shenhe;

import com.linweiyun.genshin.content.effects.itemstacks.IItemStackEffect;
import com.linweiyun.genshin.content.effects.itemstacks.ItemStackEffectHelper;
import com.linweiyun.genshin.content.effects.itemstacks.registries.ItemStackEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class IcyQuillEffect implements IItemStackEffect {

  public static final String icyQuillCountKey = "ice_quill_count";
  public static final String icyQuillDamageKey = "damage_amount";

  @Override
  public void onEffectAdded(ItemStack stack, Player holder) {
    var effects = ItemStackEffectHelper.getEffects(stack);
    var currentInstance =
        effects.effects().stream()
            .filter(e -> e.effect().equals(ItemStackEffects.ICE_QUILL_EFFECT.get()))
            .findFirst()
            .orElse(null);

    if (currentInstance != null) {
      int count = currentInstance.getIntData(icyQuillCountKey);
      int damage = currentInstance.getIntData(icyQuillDamageKey);
    }
  }

  public void reduceQuills(ItemStack stack, int amount, Player player) {
    var instance = ItemStackEffectHelper.getEffectInstance(stack, this);

    if (instance == null) return;
    int currentCount = instance.getIntData(icyQuillCountKey);
    int newCount = Math.max(0, currentCount - amount);
    instance.setIntData(icyQuillCountKey, newCount);
    if (newCount == 0) {
      ItemStackEffectHelper.removeEffect(stack, player, ItemStackEffects.ICE_QUILL_EFFECT.get());
    }
  }
}
