package com.linweiyun.genshin.content.entities.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber
public class CombatTimerHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (entity instanceof Player) return;
        if (!(entity instanceof ICombatTimer timer)) return;
        timer.genshin$resetCombat();
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;
        if (living instanceof Player) return;
        if (!(living instanceof ICombatTimer timer)) return;
        int ticks = timer.genshin$getCombatTicks();
        if (ticks > 0) {
            timer.genshin$setCombatTicks(ticks - 1);
        }
    }
}