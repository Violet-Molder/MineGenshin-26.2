package com.linweiyun.genshin.event.server;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.CharacterHelper;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.system.combat.targeting.CombatTargeting;
import com.linweiyun.genshin.core.world.TeyvatWorldInvasion;
import net.minecraft.server.level.ServerLevel;
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

        // 服务端也要每刻校验索敌锁：客户端那份由 ActionStateMachine 驱动，
        // 服务端这份是「攻击请求包喂进来」的，没有人校验它就会**永远不松**——
        // 后果是 ActionManager.scheduleStepMovement 里那句 `if (isLocked) return`
        // 从此一直成立，服务端再也不执行动作自带的 moves 位移（两端行为分叉）。
        if (!player.level().isClientSide()) {
            CombatTargeting.tick(player);
        }

        if (!player.level().isClientSide() && player.level() instanceof ServerLevel sl
                && !TeyvatWorldInvasion.get(sl).isInvaded()) {
            return;
        }
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (!player.level().isClientSide()) {
            for (int uuid : attachment.getPartyCharacterUUIDs()) {
                PGCharacter character = CharacterHelper.getCharacterByUUID(player, uuid);
                if (character != null) {
                    character.tick(player);
                }
            }

            Boolean genshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
            if (Boolean.TRUE.equals(genshinMode)) {
                boolean allDead = true;
                for (int i = 0; i < 4; i++) {
                    PGCharacter pc = attachment.getPartyCharacter(i);
                    if (pc != null && pc.getData().getCurrentHP() > 0) {
                        allDead = false;
                        break;
                    }
                }
                if (allDead) {
                    player.setData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT, false);
                    if (player instanceof ServerPlayer sp) {
                        NetworkManager.setGenshinModeToPlayer(sp, false);
                    }
                }
            }
        }
        for (PGCharacter character : attachment.getOwnedCharacters()) {
            if (character.getData().isDirty()) {
                character.getData().clearDirty();
                if (player instanceof ServerPlayer serverPlayer) {
                    attachment.syncSingleCharacterToPlayer(serverPlayer, character);
                }

            }
        }
    }
}