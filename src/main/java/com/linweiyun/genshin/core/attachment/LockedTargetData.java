package com.linweiyun.genshin.core.attachment;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public class LockedTargetData implements IPersistedSerializable {

    @Persisted(key = "target_id_most")
    private long targetIdMost;

    @Persisted(key = "target_id_least")
    private long targetIdLeast;

    @Persisted(key = "expire_tick")
    private long expireTick;

    public LockedTargetData() {
        this(0, 0, 0);
    }

    public LockedTargetData(UUID targetId, long expireTick) {
        this.targetIdMost = targetId.getMostSignificantBits();
        this.targetIdLeast = targetId.getLeastSignificantBits();
        this.expireTick = expireTick;
    }

    public LockedTargetData(long targetIdMost, long targetIdLeast, long expireTick) {
        this.targetIdMost = targetIdMost;
        this.targetIdLeast = targetIdLeast;
        this.expireTick = expireTick;
    }

    public static final LockedTargetData EMPTY = new LockedTargetData(0, 0, 0);
    public static final Codec<LockedTargetData> CODEC = PersistedParser.createCodec(LockedTargetData::new);
    public static final StreamCodec<ByteBuf, LockedTargetData> STREAM_CODEC = PersistedParser.createStreamCodec(LockedTargetData::new);

    public UUID targetId() {
        return new UUID(targetIdMost, targetIdLeast);
    }

    public long expireTick() {
        return expireTick;
    }

    public boolean isValid(long currentTick) {
        return (targetIdMost != 0 || targetIdLeast != 0) && currentTick < expireTick;
    }
}