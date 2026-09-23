package com.linweiyun.genshin.event.server;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.about.block.BlockElementHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 玩家左键攻击方块时，把当前角色的元素附着到目标方块上。
 *
 * 例如火元素角色左键冰块 → PYRO 附着 → 反应系统自动触发融化 → 冰变水。
 */
@EventBusSubscriber
public class BlockElementAttackHandler {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;

        PlayerCharactersAttachment chars = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (chars == null) return;

        PGCharacter character = chars.getCurrentCharacter();
        if (character == null) return;

        GenshinElement element = character.getElemental();
        if (element == null) return;

        BlockElementHelper.applyElement(level, event.getPos(), element, 1.0f, 0.2f);
    }
}