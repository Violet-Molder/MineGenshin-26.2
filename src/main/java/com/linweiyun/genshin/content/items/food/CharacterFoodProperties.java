package com.linweiyun.genshin.content.items.food;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CharacterFoodProperties(
        float fixedHeal,
        float percentHeal,
        float eatSeconds,
        boolean reviveFallen
) {
    public static final float DEFAULT_EAT_SECONDS = 1.6F;
    public static final CharacterFoodProperties EMPTY =
            new CharacterFoodProperties(0, 0, DEFAULT_EAT_SECONDS, false);

    public int eatDurationTicks() {
        return (int) (this.eatSeconds * 20.0F);
    }

    public boolean hasHeal() {
        return fixedHeal > 0 || percentHeal > 0;
    }

    public float calculateHeal(float maxHP) {
        float result = fixedHeal;
        if (percentHeal > 0) {
            result += maxHP * (percentHeal / 100f);
        }
        return result;
    }

    public static final Codec<CharacterFoodProperties> CODEC =
            RecordCodecBuilder.create(
                    instance -> instance
                            .group(
                                    Codec.FLOAT.optionalFieldOf("fixed_heal", 0f)
                                            .forGetter(CharacterFoodProperties::fixedHeal),
                                    Codec.FLOAT.optionalFieldOf("percent_heal", 0f)
                                            .forGetter(CharacterFoodProperties::percentHeal),
                                    Codec.FLOAT.optionalFieldOf("eat_seconds", DEFAULT_EAT_SECONDS)
                                            .forGetter(CharacterFoodProperties::eatSeconds),
                                    Codec.BOOL.optionalFieldOf("revive_fallen", false)
                                            .forGetter(CharacterFoodProperties::reviveFallen)
                            )
                            .apply(instance, CharacterFoodProperties::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterFoodProperties> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    CharacterFoodProperties::fixedHeal,
                    ByteBufCodecs.FLOAT,
                    CharacterFoodProperties::percentHeal,
                    ByteBufCodecs.FLOAT,
                    CharacterFoodProperties::eatSeconds,
                    ByteBufCodecs.BOOL,
                    CharacterFoodProperties::reviveFallen,
                    CharacterFoodProperties::new);

    public static class Builder {
        private float fixedHeal = 0;
        private float percentHeal = 0;
        private float eatSeconds = DEFAULT_EAT_SECONDS;
        private boolean reviveFallen = false;

        public Builder fixedHeal(float amount) {
            this.fixedHeal = amount;
            return this;
        }

        public Builder percentHeal(float percent) {
            this.percentHeal = percent;
            return this;
        }

        public Builder eatSeconds(float seconds) {
            this.eatSeconds = seconds;
            return this;
        }

        public Builder fast() {
            this.eatSeconds = 0.8F;
            return this;
        }

        public Builder reviveFallen() {
            this.reviveFallen = true;
            return this;
        }

        public CharacterFoodProperties build() {
            return new CharacterFoodProperties(fixedHeal, percentHeal, eatSeconds, reviveFallen);
        }
    }
}