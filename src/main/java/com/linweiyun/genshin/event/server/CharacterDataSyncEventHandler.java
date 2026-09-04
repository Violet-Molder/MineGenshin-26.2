package com.linweiyun.genshin.event.server;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.system.registry.register.ModCharacters;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class CharacterDataSyncEventHandler {
    public static void
    handle(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            //原石数据同步
            int primogem = player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
            NetworkManager.setPrimogemToPlayer(player, primogem);
            //原神模式同步
            boolean isGenshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
            NetworkManager.setGenshinModeToPlayer(player, isGenshinMode);
            //冒险者信息同步
            var adventurerInfo = player.getData(AttachmentRegistration.ADVENTURER_INFO_ATTACHMENT);
            adventurerInfo.syncToPlayer(player);
            //角色数据同步
            PlayerCharactersAttachment playerData = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            playerData.fixCharacterTypes();

            PGCharacter shenhe = ModCharacters.getByUUID(135001);
            if (shenhe != null && !playerData.hasCharacter(135001)) {
                playerData.addCharacterToPlayer(player, shenhe);
                playerData.setPartyCharacterToPlayer(player, 0, 135001);
            }

            playerData.syncToPlayer(player);

        }
    }
}