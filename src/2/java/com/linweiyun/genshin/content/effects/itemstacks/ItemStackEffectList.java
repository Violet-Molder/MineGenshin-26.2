package com.linweiyun.genshin.content.effects.itemstacks;

import com.linweiyun.genshin.content.effects.itemstacks.instance.ItemStackEffectInstance;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ItemStackEffectList(List<ItemStackEffectInstance> effects) {
  public static final ItemStackEffectList EMPTY = new ItemStackEffectList(List.of());

  // 序列化
  public static final Codec<ItemStackEffectList> CODEC =
      ItemStackEffectInstance.CODEC
          .listOf()
          .xmap(ItemStackEffectList::new, ItemStackEffectList::effects);

  // 网络同步
  public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackEffectList> STREAM_CODEC =
      ItemStackEffectInstance.STREAM_CODEC
          .apply(ByteBufCodecs.list())
          .map(ItemStackEffectList::new, ItemStackEffectList::effects);

  public ItemStackEffectList withEffect(ItemStackEffectInstance instance) {
    var newList = new java.util.ArrayList<>(effects);
    // 替换同ID效果
    newList.removeIf(e -> e.effectId().equals(instance.effectId()));
    newList.add(instance);
    return new ItemStackEffectList(List.copyOf(newList));
  }

  public ItemStackEffectList removeEffect(IItemStackEffect effect) {
    var newList = effects.stream().filter(e -> !e.effect().equals(effect)).toList();
    return new ItemStackEffectList(newList);
  }
}
