package com.linweiyun.genshin.content.items.component;

import com.linweiyun.genshin.content.items.artifact.ArtifactLevelData;
import com.linweiyun.genshin.content.items.artifact.ArtifactStatData;
import com.linweiyun.genshin.content.items.artifact.ArtifactType;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArtifactStatsComponent implements IPersistedSerializable {
    public static final Codec<ArtifactStatsComponent> CODEC = PersistedParser.createCodec(ArtifactStatsComponent::new);
    public static final StreamCodec<ByteBuf, ArtifactStatsComponent> STREAM_CODEC = PersistedParser.createStreamCodec(ArtifactStatsComponent::new);

    public static final ArtifactStatsComponent DEFAULT = new ArtifactStatsComponent(0, 0, null, List.of());

    @Persisted(key = "level")
    public int level;

    @Persisted(key = "exp")
    public int exp;

    @Persisted(key = "main_stat")
    public TeyvatItemStat mainStat;

    @Persisted(key = "sub_stats")
    public List<TeyvatItemStat> subStats;

    public ArtifactStatsComponent() {}

    public ArtifactStatsComponent(int level, int exp, TeyvatItemStat mainStat, List<TeyvatItemStat> subStats) {
        this.level = level;
        this.exp = exp;
        this.mainStat = mainStat;
        this.subStats = subStats != null ? subStats : new ArrayList<>();
    }

    public ArtifactStatsComponent copy() {
        TeyvatItemStat mainCopy = mainStat != null ? mainStat.copy() : null;
        List<TeyvatItemStat> subsCopy = new ArrayList<>();
        for (TeyvatItemStat s : subStats) subsCopy.add(s.copy());
        return new ArtifactStatsComponent(level, exp, mainCopy, subsCopy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactStatsComponent that)) return false;
        return level == that.level
                && exp == that.exp
                && Objects.equals(mainStat, that.mainStat)
                && Objects.equals(subStats, that.subStats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, exp, mainStat, subStats);
    }

    public int getMaxLevel(int star) {
        return ArtifactLevelData.getMaxLevel(star);
    }

    public long getExpToNextLevel(int star) {
        return ArtifactLevelData.getExpToNextLevel(star, level);
    }

    public long addExp(int amount, int star, ArtifactType type) {
        if (level >= getMaxLevel(star)) return 0;
        long added = Math.min(amount, getExpToNextLevel(star) - exp);
        exp += added;
        while (level < getMaxLevel(star) && exp >= getExpToNextLevel(star)) {
            exp -= getExpToNextLevel(star);
            level++;
            updateMainStatValue(star);
            checkUnlockSubStats(star);
        }
        return added;
    }

    private void updateMainStatValue(int star) {
        if (mainStat == null) return;
        double base = ArtifactStatData.getMainStatBase(mainStat.getAttribute(), mainStat.getKind(), star);
        double growth = ArtifactStatData.getMainStatGrowth(mainStat.getAttribute(), mainStat.getKind(), star);
        mainStat.setValue(base + growth * (level - 1));
    }

    private void checkUnlockSubStats(int star) {
        int initialUnlock = switch (star) {
            case 1, 2 -> 1;
            case 3 -> 2;
            case 4 -> 3;
            case 5 -> 3;
            default -> 1;
        };
        int unlockedCountByLevel = level / 4;
        int totalUnlocked = Math.min(initialUnlock + unlockedCountByLevel, subStats.size());
        for (int i = 0; i < subStats.size(); i++) {
            subStats.get(i).setUnlocked(i < totalUnlocked);
        }
    }
}