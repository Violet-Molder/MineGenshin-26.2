package com.linweiyun.genshin.content.entities.teyvat;

import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TeyvatEntityStats(
        int level,
        int combatTicks,
        boolean targeting,
        float physicalResistance,
        float pyroResistance,
        float hydroResistance,
        float anemoResistance,
        float electroResistance,
        float dendroResistance,
        float cyroResistance,
        float geoResistance
) {
    private static final float DEFAULT_RES = 0.10f;

    public static final TeyvatEntityStats DEFAULT = new TeyvatEntityStats(
            0, 0, false, DEFAULT_RES, DEFAULT_RES, DEFAULT_RES, DEFAULT_RES,
            DEFAULT_RES, DEFAULT_RES, DEFAULT_RES, DEFAULT_RES
    );

    public static final Codec<TeyvatEntityStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(TeyvatEntityStats::level),
            Codec.INT.fieldOf("combat_ticks").forGetter(TeyvatEntityStats::combatTicks),
            Codec.BOOL.fieldOf("targeting").forGetter(TeyvatEntityStats::targeting),
            Codec.FLOAT.fieldOf("physical_resistance").forGetter(TeyvatEntityStats::physicalResistance),
            Codec.FLOAT.fieldOf("pyro_resistance").forGetter(TeyvatEntityStats::pyroResistance),
            Codec.FLOAT.fieldOf("hydro_resistance").forGetter(TeyvatEntityStats::hydroResistance),
            Codec.FLOAT.fieldOf("anemo_resistance").forGetter(TeyvatEntityStats::anemoResistance),
            Codec.FLOAT.fieldOf("electro_resistance").forGetter(TeyvatEntityStats::electroResistance),
            Codec.FLOAT.fieldOf("dendro_resistance").forGetter(TeyvatEntityStats::dendroResistance),
            Codec.FLOAT.fieldOf("cyro_resistance").forGetter(TeyvatEntityStats::cyroResistance),
            Codec.FLOAT.fieldOf("geo_resistance").forGetter(TeyvatEntityStats::geoResistance)
    ).apply(instance, TeyvatEntityStats::new));

    public static final StreamCodec<FriendlyByteBuf, TeyvatEntityStats> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, TeyvatEntityStats::level,
                    ByteBufCodecs.INT, TeyvatEntityStats::combatTicks,
                    ByteBufCodecs.BOOL, TeyvatEntityStats::targeting,
                    ByteBufCodecs.FLOAT, TeyvatEntityStats::physicalResistance,
                    ByteBufCodecs.FLOAT, TeyvatEntityStats::pyroResistance,
                    ByteBufCodecs.FLOAT, TeyvatEntityStats::hydroResistance,
                    ByteBufCodecs.FLOAT, TeyvatEntityStats::anemoResistance,
                    ByteBufCodecs.FLOAT, TeyvatEntityStats::electroResistance,
                    ByteBufCodecs.FLOAT, TeyvatEntityStats::dendroResistance,
                    ByteBufCodecs.FLOAT, TeyvatEntityStats::cyroResistance,
                    ByteBufCodecs.FLOAT, TeyvatEntityStats::geoResistance,
                    TeyvatEntityStats::new
            );

    public float getElementResistance(GenshinElement element) {
        if (element == ModElements.PYRO.get()) return pyroResistance;
        if (element == ModElements.HYDRO.get()) return hydroResistance;
        if (element == ModElements.ANEMO.get()) return anemoResistance;
        if (element == ModElements.ELECTRO.get()) return electroResistance;
        if (element == ModElements.DENDRO.get()) return dendroResistance;
        if (element == ModElements.CYRO.get()) return cyroResistance;
        if (element == ModElements.GEO.get()) return geoResistance;
        return DEFAULT_RES;
    }

    public TeyvatEntityStats withLevel(int newLevel) {
        return new TeyvatEntityStats(newLevel, combatTicks, targeting, physicalResistance,
                pyroResistance, hydroResistance, anemoResistance,
                electroResistance, dendroResistance, cyroResistance, geoResistance);
    }

    public TeyvatEntityStats withCombatTicks(int ticks) {
        return new TeyvatEntityStats(level, ticks, targeting, physicalResistance,
                pyroResistance, hydroResistance, anemoResistance,
                electroResistance, dendroResistance, cyroResistance, geoResistance);
    }

    public TeyvatEntityStats withTargeting(boolean targeting) {
        return new TeyvatEntityStats(level, combatTicks, targeting, physicalResistance,
                pyroResistance, hydroResistance, anemoResistance,
                electroResistance, dendroResistance, cyroResistance, geoResistance);
    }

    public TeyvatEntityStats withElementResistance(GenshinElement element, float resistance) {
        if (element == ModElements.PYRO.get()) return new TeyvatEntityStats(level, combatTicks, targeting, physicalResistance,
                resistance, hydroResistance, anemoResistance, electroResistance,
                dendroResistance, cyroResistance, geoResistance);
        if (element == ModElements.HYDRO.get()) return new TeyvatEntityStats(level, combatTicks, targeting, physicalResistance,
                pyroResistance, resistance, anemoResistance, electroResistance,
                dendroResistance, cyroResistance, geoResistance);
        if (element == ModElements.ANEMO.get()) return new TeyvatEntityStats(level, combatTicks, targeting, physicalResistance,
                pyroResistance, hydroResistance, resistance, electroResistance,
                dendroResistance, cyroResistance, geoResistance);
        if (element == ModElements.ELECTRO.get()) return new TeyvatEntityStats(level, combatTicks, targeting, physicalResistance,
                pyroResistance, hydroResistance, anemoResistance, resistance,
                dendroResistance, cyroResistance, geoResistance);
        if (element == ModElements.DENDRO.get()) return new TeyvatEntityStats(level, combatTicks, targeting, physicalResistance,
                pyroResistance, hydroResistance, anemoResistance, electroResistance,
                resistance, cyroResistance, geoResistance);
        if (element == ModElements.CYRO.get()) return new TeyvatEntityStats(level, combatTicks, targeting, physicalResistance,
                pyroResistance, hydroResistance, anemoResistance, electroResistance,
                dendroResistance, resistance, geoResistance);
        if (element == ModElements.GEO.get()) return new TeyvatEntityStats(level, combatTicks, targeting, physicalResistance,
                pyroResistance, hydroResistance, anemoResistance, electroResistance,
                dendroResistance, cyroResistance, resistance);
        return this;
    }

    public TeyvatEntityStats withPhysicalResistance(float resistance) {
        return new TeyvatEntityStats(level, combatTicks, targeting, resistance,
                pyroResistance, hydroResistance, anemoResistance,
                electroResistance, dendroResistance, cyroResistance, geoResistance);
    }

    public int getDefense() {
        return level * 5 + 500;
    }
}