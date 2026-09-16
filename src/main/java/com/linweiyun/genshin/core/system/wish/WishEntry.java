package com.linweiyun.genshin.core.system.wish;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record WishEntry(String type, String id, int weight, int count) {

    public static final Codec<WishEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("type").forGetter(WishEntry::type),
                    Codec.STRING.fieldOf("id").forGetter(WishEntry::id),
                    Codec.INT.fieldOf("weight").forGetter(WishEntry::weight),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(WishEntry::count)
            ).apply(instance, WishEntry::new));
}