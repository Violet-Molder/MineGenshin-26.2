package com.linweiyun.genshin.content.effects.itemstacks.instance;

import com.linweiyun.genshin.content.effects.itemstacks.IItemStackEffect;
import com.linweiyun.genshin.content.effects.itemstacks.registries.ItemStackEffectRegistries;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record ItemStackEffectInstance(
    IItemStackEffect effect,
    @Persisted(key = "duration") int duration,
    @Persisted(key = "amplifier") int amplifier,
    @Persisted(key = "hidden") boolean hidden,
    @Persisted(key = "data") CompoundTag data)
    implements IPersistedSerializable {
  // 无限持续时间
  public static final int INFINITE = -1;
  public static final CompoundTag EMPTY_DATA = new CompoundTag();

  public ResourceLocation effectId() {
    return effect != null
        ? ItemStackEffectRegistries.ITEM_STACK_EFFECT_REGISTRY.getKey(effect)
        : ResourceLocation.tryParse("unknown");
  }

  public static final Codec<ItemStackEffectInstance> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      ItemStackEffectRegistries.ITEM_STACK_EFFECT_REGISTRY
                          .byNameCodec()
                          .fieldOf("id")
                          .forGetter(ItemStackEffectInstance::effect),
                      Codec.INT.fieldOf("duration").forGetter(ItemStackEffectInstance::duration),
                      Codec.INT.fieldOf("amplifier").forGetter(ItemStackEffectInstance::amplifier),
                      CompoundTag.CODEC
                          .optionalFieldOf("data", EMPTY_DATA)
                          .forGetter(ItemStackEffectInstance::data))
                  .apply(instance, ItemStackEffectInstance::new));

  public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackEffectInstance> STREAM_CODEC =
      StreamCodec.composite(
          ResourceLocation.STREAM_CODEC,
          ItemStackEffectInstance::effectId,
          ByteBufCodecs.VAR_INT,
          ItemStackEffectInstance::duration,
          ByteBufCodecs.VAR_INT,
          ItemStackEffectInstance::amplifier,
          ByteBufCodecs.BOOL,
          ItemStackEffectInstance::hidden,
          ByteBufCodecs.COMPOUND_TAG,
          ItemStackEffectInstance::data,
          ItemStackEffectInstance::new);

  ItemStackEffectInstance(
      ResourceLocation effectId, int duration, int amplifier, boolean hidden, CompoundTag data) {
    this(
        ItemStackEffectRegistries.ITEM_STACK_EFFECT_REGISTRY.get(effectId),
        duration,
        amplifier,
        hidden,
        data);
  }

  ItemStackEffectInstance(ResourceLocation effectId, int duration, int amplifier, boolean hidden) {
    this(
        ItemStackEffectRegistries.ITEM_STACK_EFFECT_REGISTRY.get(effectId),
        duration,
        amplifier,
        hidden,
        EMPTY_DATA);
  }

  public ItemStackEffectInstance(
      IItemStackEffect effect, int duration, int amplifier, boolean hidden) {
    this(effect, duration, amplifier, hidden, EMPTY_DATA);
  }

  public ItemStackEffectInstance(
      IItemStackEffect effect, int duration, int amplifier, CompoundTag data) {
    this(effect, duration, amplifier, false, data);
  }

  public ItemStackEffectInstance withData(CompoundTag newData) {
    return new ItemStackEffectInstance(effect, duration, amplifier, hidden, newData);
  }

  public CompoundTag getData() {
    return data;
  }

  public int getIntData(String key) {
    return data.getInt(key);
  }

  public String getStringData(String key) {
    return data.getString(key);
  }

  public void setIntData(String key, int value) {
    this.data.putInt(key, value);
  }

  public void setStringData(String key, String value) {
    this.data.putString(key, value);
  }

  public IItemStackEffect getEffect() {
    return effect;
  }
}
