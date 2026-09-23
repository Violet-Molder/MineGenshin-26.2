package com.linweiyun.genshin.core.status;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.about.block.ChunkBlockElements;
import com.linweiyun.genshin.core.system.registry.register.ModStatusDataComponents;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;

/**
 * 宿主访问器 —— 五种宿主 → StatusContainer
 *
 * LivingEntity / BlockEntity → NeoForge Attachment
 * PGCharacterData           → @Persisted 字段
 * ItemStack                 → DataComponent
 * BlockPos（方块）           → Chunk Attachment 中提取 ElementalAttachmentInstance
 */
public class StatusAccessor {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static StatusContainer of(LivingEntity entity) {
        return entity.getData(AttachmentRegistration.CONTAINER);
    }

    public static StatusContainer of(BlockEntity blockEntity) {
        return blockEntity.getData(AttachmentRegistration.CONTAINER);
    }

    public static StatusContainer of(PGCharacterData data) {
        return data.getStatusContainer();
    }

    public static StatusContainer of(ItemStack stack) {
        StatusContainer c = stack.get(ModStatusDataComponents.CONTAINER);
        return c != null ? c : StatusContainer.EMPTY;
    }

    public static StatusContainer of(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(AttachmentRegistration.CHUNK_ELEMENTS)) {
            return StatusContainer.EMPTY;
        }
        ChunkBlockElements data = chunk.getData(AttachmentRegistration.CHUNK_ELEMENTS);
        if (data == null || data.isEmpty()) {
            return StatusContainer.EMPTY;
        }
        ElementalAttachmentInstance inst = data.get(pos);
        if (inst == null || inst.isFinished()) {
            return StatusContainer.EMPTY;
        }
        StatusContainer container = new StatusContainer();
        container.add(inst.copy());
        return container;
    }
}