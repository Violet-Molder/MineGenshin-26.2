package com.linweiyun.genshin.event.client;

import com.linweiyun.genshin.core.system.registry.register.ModMobEffects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class KeyInterceptionEvent {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (player.hasEffect(ModMobEffects.STUN)) {
            for (KeyMapping k : StunKeys()) {
                k.setDown(false);
                while (k.consumeClick()) {}
            }
        }
    }

    public static KeyMapping[] StunKeys() {
        Minecraft mc = Minecraft.getInstance();
        return new KeyMapping[] {
                mc.options.keyUp,
                mc.options.keyDown,
                mc.options.keyLeft,
                mc.options.keyRight,
                mc.options.keyInventory,
                mc.options.keyUse
        };
    }

}