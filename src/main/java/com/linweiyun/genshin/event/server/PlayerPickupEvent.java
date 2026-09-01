package com.linweiyun.genshin.event.server;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

@EventBusSubscriber
public class PlayerPickupEvent {
    @SubscribeEvent
    public static void onPickupXpOrb(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()){
            boolean isGenshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);

            if (isGenshinMode) {
                int amount = event.getAmount();
                PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                PGCharacter character = attachment.getCurrentCharacter();
                if (character == null) return;
                character.addExp(amount);
            }
        }

    }
}
