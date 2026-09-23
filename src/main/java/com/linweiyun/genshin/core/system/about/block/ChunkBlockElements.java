package com.linweiyun.genshin.core.system.about.block;

import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * Chunk 级方块元素容器。
 * 直接 @Persisted Map<Long, ElementalAttachmentInstance> —— LDLib2 支持 Map 序列化。
 */
public class ChunkBlockElements implements IPersistedSerializable {

    public static final Codec<ChunkBlockElements> CODEC =
            PersistedParser.createCodec(ChunkBlockElements::new);
    public static final StreamCodec<ByteBuf, ChunkBlockElements> STREAM_CODEC =
            PersistedParser.createStreamCodec(ChunkBlockElements::new);

    @Persisted(key = "elements")
    private Map<Long, ElementalAttachmentInstance> elements = new HashMap<>();

    public ChunkBlockElements() {}

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public Map<Long, ElementalAttachmentInstance> getElements() {
        return elements;
    }

    public ElementalAttachmentInstance get(BlockPos pos) {
        return elements.get(pos.asLong());
    }

    public void put(BlockPos pos, ElementalAttachmentInstance inst) {
        elements.put(pos.asLong(), inst);
    }

    public void remove(BlockPos pos) {
        elements.remove(pos.asLong());
    }
}