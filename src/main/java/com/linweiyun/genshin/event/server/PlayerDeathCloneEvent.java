package com.linweiyun.genshin.event.server;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.network.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public class PlayerDeathCloneEvent {

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var original = event.getOriginal();
        var entity = event.getEntity();

        // 简单 immutable 类型 — 只能 setData
        entity.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT,
                original.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT));
        entity.setData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT,
                original.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT));

        // 复杂对象 — getData 拿到目标引用，直接改内部状态
        PlayerCharactersAttachment dstChar = entity.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PlayerCharactersAttachment srcChar = original.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        dstChar.getOwnedCharacters().clear();
        dstChar.getOwnedCharacters().addAll(srcChar.getOwnedCharacters());
        dstChar.getSheetCharacterUUIDs().clear();
        dstChar.getSheetCharacterUUIDs().addAll(srcChar.getSheetCharacterUUIDs());
        dstChar.getPartyCharacterUUIDs().clear();
        dstChar.getPartyCharacterUUIDs().addAll(srcChar.getPartyCharacterUUIDs());
        dstChar.setCurrentCharacterIndex(srcChar.getCurrentCharacterIndex());

        StatusContainer dstStatus = entity.getData(AttachmentRegistration.CONTAINER);
        StatusContainer srcStatus = original.getData(AttachmentRegistration.CONTAINER);
        dstStatus.clear();
        for (var inst : srcStatus.getAll()) {
            dstStatus.add(inst.copy());
        }
        // === 同步到客户端（关键！之前缺的就是这个） ===
        NetworkManager.setPrimogemToPlayer(player, player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT));
        NetworkManager.setGenshinModeToPlayer(player, player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT));
        dstChar.syncToPlayer(player);
        dstChar.fixCharacterTypes();
    }
}