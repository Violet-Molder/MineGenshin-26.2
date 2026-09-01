package com.linweiyun.genshin.event.server;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.CharacterHelper;
import com.linweiyun.genshin.core.character.PGCharacter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class CharacterTickEvent {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (!player.level().isClientSide()) {
            for (int uuid : attachment.getPartyCharacterUUIDs()) {
                PGCharacter character = CharacterHelper.getCharacterByUUID(player, uuid);
                if (character != null) {
                    character.tick(player);

                }
            }
        }
        for (PGCharacter character : attachment.getOwnedCharacters()) {
            if (character.getData().isDirty()) {
                character.getData().clearDirty();
                if (player instanceof ServerPlayer serverPlayer) {
                    attachment.syncSingleCharacterToPlayer(serverPlayer, character);
                } else {
                    attachment.syncSingleCharacterToServer(character);
                }

            }
        }
    }
}