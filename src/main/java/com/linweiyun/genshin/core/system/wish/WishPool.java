package com.linweiyun.genshin.core.system.wish;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record WishPool(int rolls, List<WishEntry> entries) {

    public static final Codec<WishPool> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("rolls", 1).forGetter(WishPool::rolls),
                    Codec.list(WishEntry.CODEC).fieldOf("entries").forGetter(WishPool::entries)
            ).apply(instance, WishPool::new));
}