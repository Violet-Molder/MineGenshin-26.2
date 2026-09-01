package com.linweiyun.genshin.content.items.components;

import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record DataComponentCharacter(PlayerCharacterData characterData) {

  public static final Codec<DataComponentCharacter> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      PlayerCharacterData.getCodec()
                          .fieldOf("character_data")
                          .forGetter(DataComponentCharacter::characterData))
                  .apply(instance, DataComponentCharacter::new));

  public static final StreamCodec<FriendlyByteBuf, DataComponentCharacter> STREAM_CODEC =
      StreamCodec.composite(
          PlayerCharacterData.getStreamCodec(),
          DataComponentCharacter::characterData,
          DataComponentCharacter::new);

  public static final StreamCodec<FriendlyByteBuf, DataComponentCharacter> UNIT_STREAM_CODEC =
      StreamCodec.unit(new DataComponentCharacter(new PlayerCharacterData()));
}
