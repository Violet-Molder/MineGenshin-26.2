package com.linweiyun.genshin.event.server;

import com.linweiyun.genshin.content.effect.character.CharacterEffectHelper;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.PGCharacterData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public class CharacterEffectEvent {
    @SubscribeEvent
    public static void characterEffectTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        PlayerCharactersAttachment charactersAttachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);

        for (int partyInt = 0; partyInt < 4; partyInt++) {
            PGCharacter character = charactersAttachment.getPartyCharacter(partyInt);
            if (character != null) {
                // 修复：先复制一份列表，避免ConcurrentModificationException
                List<CharacterEffectInstance> effects = new ArrayList<>(character.getData().getEffectContainer().getEffects());
                for (CharacterEffectInstance effect : effects) {
                    if (effect != null && !(effect.getEffect() instanceof CharacterEffectInstance.DummyEffect)) {
                        effect.getEffect().onEffectBackTick(player, character, effect);
                        effect.getEffect().onEffectFrontTick(player, character, effect);
                        if (!effect.getEffect().onEffectTick(player, character, effect)) {
                            CharacterEffectHelper.removeEffect(player, character, effect.getEffect());
                        }
                    }
                }
            }
        }
    }
}
