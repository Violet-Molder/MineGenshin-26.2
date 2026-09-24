package com.linweiyun.genshin.core.system.combat;

import com.linweiyun.genshin.content.entities.teyvat.TeyvatLiving;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber
public class CombatTimerHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        if (!(victim instanceof Player) && victim instanceof TeyvatLiving victimTeyvat) {
            victimTeyvat.resetCombat();
        }

        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && !(attacker instanceof Player)
                && attacker instanceof TeyvatLiving attackerTeyvat) {
            attackerTeyvat.resetCombat();
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;
        if (living instanceof Player) return;
        if (!(living instanceof TeyvatLiving teyvat)) return;

        boolean hasTarget = living instanceof Mob mob && mob.getTarget() != null;
        if (teyvat.isTargeting() != hasTarget) {
            teyvat.setTargeting(hasTarget);
        }

        int ticks = teyvat.getCombatTicks();
        if (ticks > 0) {
            teyvat.setCombatTicks(ticks - 1);
        }
    }
}