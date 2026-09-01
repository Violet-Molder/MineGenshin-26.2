package com.linweiyun.genshin.core.events;

import com.linweiyun.genshin.content.effects.itemstacks.ItemStackEffectHelper;
import com.linweiyun.genshin.content.effects.itemstacks.instance.ItemStackEffectInstance;
import com.linweiyun.genshin.content.effects.itemstacks.registries.ItemStackEffectRegistries;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class GlobalCharacterHandler {

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {

    Player player = event.getEntity();
    if (player.level().isClientSide) return;

    CharacterParty party = player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT.get());
    for (ItemStack stack : party.getItems()) {
      if (stack.isEmpty()) continue;
      PlayerCharacterData data = CombatHelper.getCharacterData(stack);
      assert data != null;
      if (data.getElementalSkillCooldownTick() > 0) {
        data.changeData(
            stack,
            characterData ->
                characterData.setElementalSkillCooldownTick(
                    characterData.getElementalSkillCooldownTick() - 1),
            player);
      }
      if (data.getElementalBurstCooldownTick() > 0) {
        data.changeData(
            stack,
            characterData ->
                characterData.setElementalBurstCooldownTick(
                    characterData.getElementalBurstCooldownTick() - 1),
            player);
      }
      var effects = ItemStackEffectHelper.getEffects(stack);
      if (effects.effects().isEmpty()) continue;
      for (var instance : effects.effects()) {
        var effect = ItemStackEffectRegistries.ITEM_STACK_EFFECT_REGISTRY.get(instance.effectId());
        if (effect == null) continue;
        boolean keep;
        effect.onEffectTick(stack, player);
        if (stack == party.getCurrentCharacter()) {
          keep = effect.onEffectFrontTick(stack, player);
        } else {
          keep = effect.onEffectBackTick(stack, player);
        }
        if (instance.duration() != ItemStackEffectInstance.INFINITE) {
          int newDuration = instance.duration() - 1;
          if (newDuration <= 0) keep = false;
          var updated =
              new ItemStackEffectInstance(
                  instance.getEffect(), newDuration, instance.amplifier(), instance.hidden());
          ItemStackEffectHelper.addEffect(stack, player, updated);
        }

        if (!keep) {
          effect.onEffectRemoved(stack, player);
          ItemStackEffectHelper.removeEffect(stack, player, instance.effect());
        }
      }
    }
  }
}
