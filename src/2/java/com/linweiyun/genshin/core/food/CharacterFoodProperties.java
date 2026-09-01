package com.linweiyun.genshin.core.food;

import com.linweiyun.genshin.content.effects.itemstacks.instance.ItemStackEffectInstance;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CharacterFoodProperties(
    float fixedValue, float percentValue, float eatSeconds, List<ItemStackEffectInstance> effects) {
  public static final float DEFAULT_EAT_SECONDS = 1.6F;
  public static final CharacterFoodProperties EMPTY =
      new CharacterFoodProperties(0, 0, DEFAULT_EAT_SECONDS, List.of());

  public int eatDurationTicks() {
    return (int) (this.eatSeconds * 20.0F);
  }

  // ====================== 序列化 ======================
  public static final Codec<CharacterFoodProperties> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.FLOAT
                          .optionalFieldOf("fixed_heal", 0f)
                          .forGetter(CharacterFoodProperties::fixedValue),
                      Codec.FLOAT
                          .optionalFieldOf("percent_heal", 0f)
                          .forGetter(CharacterFoodProperties::percentValue),
                      Codec.FLOAT
                          .optionalFieldOf("eat_seconds", DEFAULT_EAT_SECONDS)
                          .forGetter(CharacterFoodProperties::eatSeconds),
                      ItemStackEffectInstance.CODEC
                          .listOf()
                          .optionalFieldOf("effects", List.of())
                          .forGetter(CharacterFoodProperties::effects))
                  .apply(instance, CharacterFoodProperties::new));

  // ====================== 网络同步 ======================
  public static final StreamCodec<RegistryFriendlyByteBuf, CharacterFoodProperties> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.FLOAT,
          CharacterFoodProperties::fixedValue,
          ByteBufCodecs.FLOAT,
          CharacterFoodProperties::percentValue,
          ByteBufCodecs.FLOAT,
          CharacterFoodProperties::eatSeconds,
          ItemStackEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()),
          CharacterFoodProperties::effects,
          CharacterFoodProperties::new);

  // ====================== 构建器 ======================
  public static class Builder {
    private float fixedValue = 0;
    private float percentValue = 0;
    private float eatSeconds = DEFAULT_EAT_SECONDS;
    private final List<ItemStackEffectInstance> effects = new java.util.ArrayList<>();

    // 设置固定恢复值
    public Builder fixedHeal(float amount) {
      this.fixedValue = amount;
      return this;
    }

    // 设置百分比恢复（50 = 50%）
    public Builder percentHeal(float percent) {
      this.percentValue = percent;
      return this;
    }

    // 设置食用时间
    public Builder eatSeconds(float seconds) {
      this.eatSeconds = seconds;
      return this;
    }

    // 快速食用
    public Builder fast() {
      this.eatSeconds = 0.8F;
      return this;
    }

    // 添加效果
    public Builder addEffect(ItemStackEffectInstance effect) {
      this.effects.add(effect);
      return this;
    }

    public CharacterFoodProperties build() {
      return new CharacterFoodProperties(
          fixedValue, percentValue, eatSeconds, List.copyOf(effects));
    }
  }
}
