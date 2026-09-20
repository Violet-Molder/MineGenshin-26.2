package com.linweiyun.genshin.client.render.character;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public final class AttachmentHelper {

    private AttachmentHelper() {}

    public static boolean isGenshinMode(Player player) {
        return player.hasData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT)
                && player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
    }

    @Nullable
    public static String getActiveCharacterId(Player player) {
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter current = attachment.getCurrentCharacter();
        if (current == null) return null;
        String id = current.getTextureId();
        return (id == null || id.isEmpty()) ? null : id;
    }
}