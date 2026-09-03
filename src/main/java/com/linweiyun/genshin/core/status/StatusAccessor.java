package com.linweiyun.genshin.core.status;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.system.registry.register.ModStatusDataComponents;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

/**
 * 宿主访问器 —— 从四种宿主身上拿到 StatusContainer
 *
 * LivingEntity / BlockEntity → NeoForge Attachment
 * PGCharacterData → 自己的 @Persisted 字段
 * ItemStack → DataComponent
 * 三种方式封装在本类里，对外一个统一入口。
 */
public class StatusAccessor {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static StatusContainer of(LivingEntity entity) {
        StatusContainer c = entity.getData(AttachmentRegistration.CONTAINER);
        return c;
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
}
