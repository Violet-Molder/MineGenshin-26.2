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

    /**
     * 圣遗物是否已激活（是否已抽取词条）。
     *
     * 只有激活后的圣遗物才能放入角色的圣遗物栏，才能获取经验和升级。
     * 未激活的圣遗物（创造模式物品栏、刚拿到的原始实例）为 false；
     * 玩家右键使用后调用 initializeArtifactStackIfNeeded，抽取一次词条并置为 true。
     */
    @Persisted(key = "activated")
    public boolean activated = false;

    private Runnable onStatsChanged = () -> {};

    public ArtifactStatsComponent() {
        this.mainStat = new TeyvatItemStat();
        this.subStats = new ArrayList<>();
    }

    public ArtifactStatsComponent(int level, int exp, TeyvatItemStat mainStat, List<TeyvatItemStat> subStats) {
        this(level, exp, mainStat, subStats, false);
    }

    public ArtifactStatsComponent(int level, int exp, TeyvatItemStat mainStat, List<TeyvatItemStat> subStats, boolean activated) {
        this.level = level;
        this.exp = exp;
        //TEMP 防御：也确保非 null，避免调用方误传 null
        this.mainStat = mainStat != null ? mainStat : new TeyvatItemStat();
        this.subStats = subStats != null ? subStats : new ArrayList<>();
        this.activated = activated;
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
        return new ArtifactStatsComponent(level, exp, mainCopy, subsCopy, activated);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactStatsComponent that)) return false;
        return level == that.level
                && exp == that.exp
                && activated == that.activated
                && Objects.equals(mainStat, that.mainStat)
                && Objects.equals(subStats, that.subStats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, exp, activated, mainStat, subStats);
    }

    public int getMaxLevel(int star) {
        return ArtifactLevelData.getMaxLevel(star);
    }

    public long getExpToNextLevel(int star) {
        return ArtifactLevelData.getExpToNextLevel(star, level);
    }

    /**
     * 向圣遗物添加经验。
     *
     * 规则：
     *   - 未激活的圣遗物不能获得经验，直接返回 0。
     *   - 已满级的圣遗物不能获得经验，直接返回 0。
     *   - 经验会持续累积并在满足条件时自动升级；
     *     如果一次性给出的经验足够连续升多级，会一直升级到
     *     没有更多经验或满级为止。
     *   - 升级后剩余的经验（不足以再升一级的部分）会保留在 exp 字段中，
     *     不会被丢弃。
     *   - 满级后剩余经验会被清零，不再保留。
     *
     * @param amount 本次添加的经验值
     * @param star   圣遗物星级（用于计算各等级所需经验）
     * @param type   圣遗物部位（暂未使用，保留接口以兼容后续扩展）
     * @return 实际被消耗的经验值（未激活、已满级等情况返回 0）
     */
    public long addExp(int amount, int star, ArtifactType type) {
        // 未激活的圣遗物不能获得经验
        if (!activated) return 0;
        int maxLevel = getMaxLevel(star);
        if (level >= maxLevel) return 0;
        if (amount <= 0) return 0;

        long remaining = amount;
        long consumed = 0;

        while (remaining > 0 && level < maxLevel) {
            long needed = getExpToNextLevel(star) - exp;
            if (needed <= 0) {
                // 防御：当前经验已经达到或超过升级所需，直接升级
                exp -= (int) getExpToNextLevel(star);
                if (exp < 0) exp = 0;
                level++;
                updateMainStatValue(star);
                upgradeSubStats(star);
                continue;
            }
            long gained = Math.min(remaining, needed);
            exp += gained;
            remaining -= gained;
            consumed += gained;

            // 检查是否可以升级（可能连续升多级）
            while (level < maxLevel && exp >= getExpToNextLevel(star)) {
                exp -= (int) getExpToNextLevel(star);
                level++;
                updateMainStatValue(star);
                upgradeSubStats(star);
            }
        }

        // 满级后经验清零
        if (level >= maxLevel) {
            exp = 0;
        }

        if (consumed > 0) {
            onStatsChanged.run();
        }
        return consumed;
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