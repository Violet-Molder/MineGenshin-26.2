package com.linweiyun.genshin.content.effects.itemstacks;

import com.linweiyun.genshin.content.effects.itemstacks.instance.ItemStackEffectInstance;
import com.linweiyun.genshin.content.effects.itemstacks.registries.ItemStackEffectRegistries;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemStackEffectHelper {
  public static void addEffect(ItemStack stack, Player holder, ItemStackEffectInstance instance) {
    var oldList = getEffects(stack);
    boolean alreadyHas =
        oldList.effects().stream().anyMatch(e -> e.effect().equals(instance.effect()));

    if (!alreadyHas) {
      IItemStackEffect effect =
          ItemStackEffectRegistries.ITEM_STACK_EFFECT_REGISTRY.get(instance.effectId());
      if (effect != null) {
        effect.onEffectAdded(stack, holder);
      }
    }
    stack.update(
        DataComponentRegistryCharacter.ITEM_STACK_EFFECTS.get(),
        ItemStackEffectList.EMPTY,
        list -> list.withEffect(instance));
    DataResult<Tag> result =
        ItemStackEffectList.CODEC.encodeStart(NbtOps.INSTANCE, getEffects(stack));
    Tag encoded = result.result().orElse(null);
    if (encoded == null) return;
    ListTag effectsData = (ListTag) encoded;
    CharacterParty party = holder.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    int slot = party.getSlotByCharacter(stack);
    if (slot < 0) return;

    if (holder.level().isClientSide) {
      NetworkManager.setItemStackEffectsToServer(effectsData, slot);
    } else if (holder instanceof ServerPlayer serverPlayer) {
      NetworkManager.setItemStackEffectsToPlayer(serverPlayer, effectsData, slot);
    }
  }

  public static void removeEffect(ItemStack stack, Player holder, IItemStackEffect effect) {
    var oldList = getEffects(stack);
    boolean hadEffect = oldList.effects().stream().anyMatch(e -> e.effect().equals(effect));

    if (hadEffect) {
      effect.onEffectRemoved(stack, holder);
      oldList.removeEffect(effect);
    }

    stack.update(
        DataComponentRegistryCharacter.ITEM_STACK_EFFECTS.get(),
        ItemStackEffectList.EMPTY,
        list -> list.removeEffect(effect));
    DataResult<Tag> result =
        ItemStackEffectList.CODEC.encodeStart(NbtOps.INSTANCE, getEffects(stack));
    Tag encoded = result.result().orElse(null);
    if (encoded == null) return;
    ListTag effectsData = (ListTag) encoded;
    CharacterParty party = holder.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    int slot = party.getSlotByCharacter(stack);
    if (slot < 0) return;

    if (holder.level().isClientSide) {
      NetworkManager.setItemStackEffectsToServer(effectsData, slot);
    } else if (holder instanceof ServerPlayer serverPlayer) {
      NetworkManager.setItemStackEffectsToPlayer(serverPlayer, effectsData, slot);
    }
  }

  public static ItemStackEffectList getEffects(ItemStack stack) {
    return stack.getOrDefault(
        DataComponentRegistryCharacter.ITEM_STACK_EFFECTS.get(), ItemStackEffectList.EMPTY);
  }

  public static void clearEffects(ItemStack stack, Player holder) {
    var list = getEffects(stack);
    for (var inst : list.effects()) {
      IItemStackEffect effect =
          ItemStackEffectRegistries.ITEM_STACK_EFFECT_REGISTRY.get(inst.effectId());
      if (effect != null) {
        effect.onEffectRemoved(stack, holder);
      }
    }
    stack.set(DataComponentRegistryCharacter.ITEM_STACK_EFFECTS.get(), ItemStackEffectList.EMPTY);
    DataResult<Tag> result =
        ItemStackEffectList.CODEC.encodeStart(NbtOps.INSTANCE, getEffects(stack));
    Tag encoded = result.result().orElse(null);
    if (encoded == null) return;
    ListTag effectsData = (ListTag) encoded;
    CharacterParty party = holder.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    int slot = party.getSlotByCharacter(stack);
    if (slot < 0) return;

    if (holder.level().isClientSide) {
      NetworkManager.setItemStackEffectsToServer(effectsData, slot);
    } else if (holder instanceof ServerPlayer serverPlayer) {
      NetworkManager.setItemStackEffectsToPlayer(serverPlayer, effectsData, slot);
    }
  }

  public static ItemStackEffectInstance getEffectInstance(
      ItemStack stack, IItemStackEffect effect) {
    var effects = getEffects(stack);
    return effects.effects().stream()
        .filter(e -> e.effect().equals(effect))
        .findFirst()
        .orElse(null);
  }
}
