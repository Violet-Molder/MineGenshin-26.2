package com.linweiyun.genshin.event.server;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.world.TeyvatWorldInvasion;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.slf4j.Logger;

@EventBusSubscriber
public class StatusTickEvent {

    public static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;
        if (living.level() instanceof ServerLevel sl && !TeyvatWorldInvasion.get(sl).isInvaded()) return;

        StatusContainer c = living.getData(AttachmentRegistration.CONTAINER);
        c.tick();

        // 强制触发附件同步：NeoForge 只在 setData 时同步，可变对象内部修改不会被感知
        living.setData(AttachmentRegistration.CONTAINER.get(), c);
    }
}