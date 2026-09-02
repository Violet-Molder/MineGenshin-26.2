package com.linweiyun.genshin.event.server;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.mojang.logging.LogUtils;
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
        if (event.getEntity() instanceof LivingEntity living
                && !living.level().isClientSide()) {
            StatusContainer c = living.getData(AttachmentRegistration.CONTAINER);
            c.tick();
        }
    }
}
