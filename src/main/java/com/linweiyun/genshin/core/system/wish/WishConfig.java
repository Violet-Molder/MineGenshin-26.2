package com.linweiyun.genshin.core.system.wish;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record WishConfig(List<WishPool> pools) {

    public static final Codec<WishConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.list(WishPool.CODEC).fieldOf("pools").forGetter(WishConfig::pools)
            ).apply(instance, WishConfig::new));
}