package com.linweiyun.genshin.event.server;

import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.slf4j.Logger;

import java.util.Objects;

@EventBusSubscriber
public class LivingHurtEvent {
    private static final Logger LOGGER = LogUtils.getLogger();
    @SubscribeEvent
    public static void onLivingHurt(LivingDamageEvent.Post event) {
        if (event.getSource() instanceof ModDamageSource source) {
            LivingEntity entity = event.getEntity();
            float maxHealth = (float) Objects.requireNonNull(entity.getAttribute(Attributes.MAX_HEALTH)).getValue();
            float currentHealth = entity.getHealth();

        }
    }
}
