package com.linweiyun.genshin.core.system.shield;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 护盾的时长结算 —— 有期限的盾到点自己消失。
 *
 * <p>单独一个订阅者，不复用 {@code StatusTickEvent}：那个只在「入侵中」的世界才跑，
 * 而护盾不该有那个限制。
 *
 * <p>{@code hasData} 先判一次，避免给每个生物都凭空挂一个护盾附件。
 */
@EventBusSubscriber
public final class ShieldTickHandler {

    //TEMP
    private ShieldTickHandler() {
    }

    //TEMP
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (living.level().isClientSide()) {
            return;
        }
        if (!living.hasData(AttachmentRegistration.SHIELD.get())) {
            return;
        }
        ShieldState state = ShieldService.get(living);
        if (state.isActive() && !state.isForever()) {
            ShieldService.tick(living);
        }
    }
}
