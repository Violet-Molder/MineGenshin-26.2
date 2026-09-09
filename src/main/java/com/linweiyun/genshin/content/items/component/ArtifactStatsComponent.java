package com.linweiyun.genshin.content.items.component;

import com.linweiyun.genshin.content.items.artifact.ArtifactLevelData;
import com.linweiyun.genshin.content.items.artifact.stat.ArtifactStatData;
import com.linweiyun.genshin.content.items.artifact.type.ArtifactType;
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
import java.util.Random;

public class ArtifactStatsComponent implements IPersistedSerializable {
    public static final Codec<ArtifactStatsComponent> CODEC = PersistedParser.createCodec(ArtifactStatsComponent::new);
    public static final StreamCodec<ByteBuf, ArtifactStatsComponent> STREAM_CODEC = PersistedParser.createStreamCodec(ArtifactStatsComponent::new);

    public static final ArtifactStatsComponent DEFAULT = new ArtifactStatsComponent(0, 0, new TeyvatItemStat(), new ArrayList<>());

    @Persisted(key = "level")
    public int level;

    @Persisted(key = "exp")
    public int exp;

    @Persisted(key = "main_stat")
    public TeyvatItemStat mainStat = new TeyvatItemStat();

    @Persisted(key = "sub_stats")
    public List<TeyvatItemStat> subStats = new ArrayList<>();

    private Runnable onStatsChanged = () -> {};

    public ArtifactStatsComponent() {
        this.mainStat = new TeyvatItemStat();
        this.subStats = new ArrayList<>();
    }

    public ArtifactStatsComponent(int level, int exp, TeyvatItemStat mainStat, List<TeyvatItemStat> subStats) {
        this.level = level;
        this.exp = exp;
        //TEMP 防御：也确保非 null，避免调用方误传 null
        this.mainStat = mainStat != null ? mainStat : new TeyvatItemStat();
        this.subStats = subStats != null ? subStats : new ArrayList<>();
    }

    public void setOnStatsChanged(Runnable onStatsChanged) {
        this.onStatsChanged = onStatsChanged != null ? onStatsChanged : () -> {};
    }


    public ArtifactStatsComponent copy() {
        TeyvatItemStat mainCopy = mainStat != null ? mainStat.copy() : null;
        List<TeyvatItemStat> subsCopy = new ArrayList<>();
        if (subStats != null) {
            for (TeyvatItemStat s : subStats) subsCopy.add(s.copy());
        }
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
        if (added <= 0) return 0;
        exp += added;
        while (level < getMaxLevel(star) && exp >= getExpToNextLevel(star)) {
            exp -= getExpToNextLevel(star);
            level++;
            updateMainStatValue(star);
            upgradeSubStats(star);
        }
        onStatsChanged.run();
        return added;
    }

    private void updateMainStatValue(int star) {
        if (mainStat == null) return;
        double value = ArtifactStatData.getMainStatValue(mainStat.getAttribute(), mainStat.getKind(), star, level);
        mainStat.setValue(value);
    }

    private static final Random RANDOM = new Random();

    private void upgradeSubStats(int star) {
        if (level <= 0 || level % 4 != 0 || subStats.isEmpty()) return;

        List<TeyvatItemStat> locked = new ArrayList<>();
        List<TeyvatItemStat> unlocked = new ArrayList<>();
        for (TeyvatItemStat s : subStats) {
            if (s.isUnlocked()) unlocked.add(s);
            else locked.add(s);
        }

        if (!locked.isEmpty()) {
            locked.get(0).setUnlocked(true);
        } else {
            TeyvatItemStat target = unlocked.get(RANDOM.nextInt(unlocked.size()));
            target.setUpgradeCount(target.getUpgradeCount() + 1);
            double baseValue = ArtifactStatData.getSubStatTierValue(
                    target.getAttribute(), target.getKind(), star, target.getTier());
            target.setValue(baseValue * (target.getUpgradeCount() + 1));
        }
    }
}