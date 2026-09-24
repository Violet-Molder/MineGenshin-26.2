package com.linweiyun.genshin.core.system.about.block;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.element.ModElements;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 冻结方块的集中式推进 + 加载自愈。
 *
 * <p><b>集中式推进</b>：不靠原版方块 tick 链 —— 那格一旦被排期/邻居变化/区块边界搅断，
 * 就再也没人推它，表现是「一片冰里边缘逐个化、中间一直冻着」。这里每 tick 统一推动，
 * 与排期无关，且天然带上限（每 tick 最多若干格）。
 *
 * <p><b>加载自愈</b>：推进表是内存态（不落存档），重启后老冰不会有人推。所以区块加载时
 * 扫一遍元素容器，把「是浮冰、且这格有元素数据」的位置补进表。
 * 判据里<b>不能</b>要求「有活着的冻元素」—— 老冰的冻元素可能早就耗尽了（当时没人推，
 * 方块没被换回水），要求它就会永远认不出来、永远冻着。
 */
@EventBusSubscriber
public final class BlockElementTicker {

    private BlockElementTicker() {
    }

    /** 每 tick 统一推进登记在案的冻结方块（内部有每格每 tick 只推进一次的去重）。 */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BlockElementHelper.trackedTick(level);
        }
    }

    /** 区块加载时把老的、还冻着的方块补进推进表（自愈）。 */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)
                || !chunk.hasData(AttachmentRegistration.CHUNK_ELEMENTS)) {
            return;
        }
        var data = chunk.getData(AttachmentRegistration.CHUNK_ELEMENTS);
        for (var entry : data.getContainers().entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            BlockPos pos = BlockPos.of(entry.getKey());
            if (level.getBlockState(pos).is(Blocks.FROSTED_ICE)) {
                BlockElementHelper.trackFrozen(level, pos);
            }
        }
    }
}