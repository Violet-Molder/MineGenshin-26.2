package com.linweiyun.genshin.content.stat;

import com.linweiyun.genshin.content.attribute.AttributeType;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public class TeyvatItemStat implements IPersistedSerializable {

    public enum StatKind { FLAT, PERCENT }

    @Persisted(key = "stat_attribute")
    private AttributeType attribute = new AttributeType();
    @Persisted(key = "stat_value")
    private double value;
    @Persisted(key = "stat_kind")
    private StatKind kind = StatKind.FLAT;
    @Persisted(key = "stat_unlocked")
    private boolean unlocked = true;
    @Persisted(key = "stat_initialized")
    private boolean initialized = false;
    @Persisted(key = "stat_tier")
    private int tier = 1;
    @Persisted(key = "stat_upgrade_count")
    private int upgradeCount = 0;

    public TeyvatItemStat() {}

    public TeyvatItemStat(AttributeType attribute, double value, StatKind kind) {
        this.attribute = attribute;
        this.value = value;
        this.kind = kind;
        this.initialized = true;
    }

    public TeyvatItemStat(AttributeType attribute, double value, StatKind kind, boolean unlocked) {
        this.attribute = attribute;
        this.value = value;
        this.kind = kind;
        this.unlocked = unlocked;
        this.initialized = true;
    }

    public TeyvatItemStat(AttributeType attribute, double value, StatKind kind, boolean unlocked, int tier) {
        this.attribute = attribute;
        this.value = value;
        this.kind = kind;
        this.unlocked = unlocked;
        this.tier = tier;
        this.initialized = true;
    }

    public AttributeType getAttribute() { return attribute; }
    public double getValue() { return value; }
    public StatKind getKind() { return kind; }
    public boolean isUnlocked() { return unlocked; }
    public boolean isInitialized() { return initialized; }
    public int getTier() { return tier; }
    public int getUpgradeCount() { return upgradeCount; }

    public void setValue(double value) { this.value = value; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    public void setKind(StatKind kind) { this.kind = kind; }
    public void setAttribute(AttributeType attribute) { this.attribute = attribute; }
    public void setTier(int tier) { this.tier = tier; }
    public void setUpgradeCount(int upgradeCount) { this.upgradeCount = upgradeCount; }

    public TeyvatItemStat copy() {
        TeyvatItemStat copy = new TeyvatItemStat(attribute, value, kind, unlocked, tier);
        copy.upgradeCount = this.upgradeCount;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeyvatItemStat that)) return false;
        return Double.compare(that.value, value) == 0
                && unlocked == that.unlocked
                && tier == that.tier
                && upgradeCount == that.upgradeCount
                && Objects.equals(attribute, that.attribute)
                && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, value, kind, unlocked, tier, upgradeCount);
    }

    public static class SubStatOption {
        public final AttributeType attribute;
        public final StatKind kind;
        public SubStatOption(AttributeType attribute, StatKind kind) {
            this.attribute = attribute;
            this.kind = kind;
        }
    }

    public static final Codec<TeyvatItemStat> CODEC = PersistedParser.createCodec(TeyvatItemStat::new);
    public static final StreamCodec<ByteBuf, TeyvatItemStat> STREAM_CODEC = PersistedParser.createStreamCodec(TeyvatItemStat::new);
}