package com.linweiyun.genshin.client.render.character;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import net.minecraft.world.entity.player.Player;


public final class AttachmentHelper {

    private AttachmentHelper() {}

    public static boolean isGenshinMode(Player player) {
        return player.hasData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT)
                && player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
    }

}
