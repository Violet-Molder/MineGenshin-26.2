package com.linweiyun.genshin.content.items.component;

import com.linweiyun.genshin.content.attribute.AttributeType;
import com.linweiyun.genshin.content.items.weapon.WeaponLevelData;
import com.linweiyun.genshin.content.items.weapon.WeaponStatData;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public class WeaponStatsComponent implements IPersistedSerializable {
    public static final Codec<WeaponStatsComponent> CODEC = PersistedParser.createCodec(WeaponStatsComponent::new);
    public static final StreamCodec<ByteBuf, WeaponStatsComponent> STREAM_CODEC = PersistedParser.createStreamCodec(WeaponStatsComponent::new);

    public static final WeaponStatsComponent DEFAULT = new WeaponStatsComponent();

    @Persisted(key = "level")
    public int level;

    @Persisted(key = "exp")
    public int exp;

    @Persisted(key = "ascended")
    public int ascended;

    @Persisted(key = "tier")
    public int tier;

    @Persisted(key = "main_stat")
    public TeyvatItemStat mainStat = new TeyvatItemStat();

    @Persisted(key = "sub_stat")
    public TeyvatItemStat subStat = new TeyvatItemStat();

    @Persisted(key = "sub_stat_type")
    public AttributeType subStatType = new AttributeType();

    @Persisted(key = "uid")
    public long uid;

    @Persisted(key = "stored_exp")
    public int storedExp = 0;

    @Persisted(key = "refinement")
    public int refinementRank = 1;

    private Runnable onStatsChanged = () -> {};

    public WeaponStatsComponent() {
        this.level = 1;
        this.exp = 0;
        this.ascended = 0;
        this.tier = 1;
        this.mainStat = new TeyvatItemStat();
        this.subStat = new TeyvatItemStat();
        this.subStatType = new AttributeType();
        this.uid = 0;
        this.refinementRank = 1;
    }

    public WeaponStatsComponent(int level, int exp, int ascended, int tier,
                                TeyvatItemStat mainStat, TeyvatItemStat subStat, AttributeType subStatType) {
        this.level = level;
        this.exp = exp;
        this.ascended = ascended;
        this.tier = tier;
        this.mainStat = mainStat != null ? mainStat : new TeyvatItemStat();
        this.subStat = subStat != null ? subStat : new TeyvatItemStat();
        this.subStatType = subStatType != null ? subStatType : new AttributeType();
        this.refinementRank = 1;
    }

    public WeaponStatsComponent(int level, int exp, int ascended, int tier,
                                TeyvatItemStat mainStat, TeyvatItemStat subStat,
                                AttributeType subStatType, long uid) {
        this(level, exp, ascended, tier, mainStat, subStat, subStatType);
        this.uid = uid;
    }

    public WeaponStatsComponent(int level, int exp, int ascended, int tier,
                                TeyvatItemStat mainStat, TeyvatItemStat subStat,
                                AttributeType subStatType, long uid, int refinementRank) {
        this(level, exp, ascended, tier, mainStat, subStat, subStatType, uid);
        this.refinementRank = refinementRank;
    }

    public void setOnStatsChanged(Runnable onStatsChanged) {
        this.onStatsChanged = onStatsChanged != null ? onStatsChanged : () -> {};
    }

    public WeaponStatsComponent copy() {
        TeyvatItemStat mainCopy = mainStat != null ? mainStat.copy() : new TeyvatItemStat();
        TeyvatItemStat subCopy = subStat != null ? subStat.copy() : new TeyvatItemStat();
        WeaponStatsComponent copy = new WeaponStatsComponent(level, exp, ascended, tier, mainCopy, subCopy, subStatType, uid, refinementRank);
        copy.storedExp = this.storedExp;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WeaponStatsComponent that)) return false;
        return level == that.level
                && exp == that.exp
                && ascended == that.ascended
                && tier == that.tier
                && uid == that.uid
                && refinementRank == that.refinementRank
                && Objects.equals(mainStat, that.mainStat)
                && Objects.equals(subStat, that.subStat)
                && Objects.equals(subStatType, that.subStatType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, exp, ascended, tier, uid, refinementRank, mainStat, subStat, subStatType);
    }

    public int getMaxLevel() {
        return WeaponLevelData.getMaxLevel(ascended);
    }

    public long getExpToNextLevel(int star) {
        return WeaponLevelData.getExpToNextLevel(star, level);
    }

    public boolean canAscend() {
        if (ascended >= WeaponLevelData.MAX_ASCENSION) return false;
        int requiredLevel = WeaponLevelData.ASCENSION_LEVELS[ascended];
        return level >= requiredLevel && level >= getMaxLevel();
    }

    public boolean ascend(int star) {
        if (!canAscend()) return false;
        if (ascended >= WeaponLevelData.MAX_ASCENSION) return false;
        ascended++;
        updateMainStatValue(star);
        updateSubStatValue(star);
        if (storedExp > 0) {
            addExp(storedExp, star);
            storedExp = 0;
        }
        onStatsChanged.run();
        return true;
    }

    public long addExp(int amount, int star) {
        if (level >= WeaponLevelData.MAX_LEVEL) return 0;
        if (amount <= 0) return 0;

        int currentMaxLevel = getMaxLevel();
        long remaining = amount;
        long consumed = 0;

        if (level >= currentMaxLevel) {
            long maxStorable = getTotalExpToMaxLevel(star);
            long storable = maxStorable - storedExp;
            if (storable > 0) {
                long toStore = Math.min(remaining, storable);
                storedExp += (int) toStore;
                remaining -= toStore;
                consumed += toStore;
            }
            if (consumed > 0) onStatsChanged.run();
            return consumed;
        }

        while (remaining > 0 && level < currentMaxLevel) {
            long needed = getExpToNextLevel(star) - exp;
            if (needed <= 0) {
                exp -= (int) getExpToNextLevel(star);
                if (exp < 0) exp = 0;
                level++;
                updateMainStatValue(star);
                updateSubStatValue(star);
                continue;
            }
            long gained = Math.min(remaining, needed);
            exp += (int) gained;
            remaining -= gained;
            consumed += gained;

            while (level < currentMaxLevel && exp >= getExpToNextLevel(star)) {
                exp -= (int) getExpToNextLevel(star);
                level++;
                updateMainStatValue(star);
                updateSubStatValue(star);
            }
        }

        if (level >= currentMaxLevel) {
            exp = 0;
        }

        if (remaining > 0 && level < WeaponLevelData.MAX_LEVEL) {
            long maxStorable = getTotalExpToMaxLevel(star);
            long storable = maxStorable - storedExp;
            if (storable > 0) {
                long toStore = Math.min(remaining, storable);
                storedExp += (int) toStore;
                consumed += toStore;
            }
        }

        if (consumed > 0) {
            onStatsChanged.run();
        }
        return consumed;
    }

    public long getTotalExpToMaxLevel(int star) {
        long total = 0;
        for (int lv = level; lv < WeaponLevelData.MAX_LEVEL; lv++) {
            long needed = WeaponLevelData.getExpToNextLevel(star, lv);
            if (needed <= 0) break;
            total += needed;
        }
        return total;
    }

    public void updateMainStatValue(int star) {
        int value = WeaponStatData.calculateMainStatValue(star, tier, level, ascended);
        mainStat = new TeyvatItemStat(ModAttributes.ATK.get(), value, TeyvatItemStat.StatKind.FLAT);
    }

    public void updateSubStatValue(int star) {
        if (subStatType == null || subStatType.id() == null) return;
        double value = WeaponStatData.getSubStatValue(star, tier, subStatType, level);
        subStat = new TeyvatItemStat(subStatType, value, TeyvatItemStat.StatKind.PERCENT);
    }

    public void initStats(int star) {
        updateMainStatValue(star);
        updateSubStatValue(star);
    }

    public static int getMaxRefinement(int star) {
        if (star >= 4) return 5;
        return 1;
    }

    public boolean canIncreaseRefinement(int star) {
        return refinementRank < getMaxRefinement(star);
    }

    public boolean increaseRefinement(int star) {
        if (!canIncreaseRefinement(star)) return false;
        refinementRank++;
        onStatsChanged.run();
        return true;
    }
}