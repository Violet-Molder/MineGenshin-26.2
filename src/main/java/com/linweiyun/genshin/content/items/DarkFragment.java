package com.linweiyun.genshin.content.items;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.system.registry.register.ModCharacters;
import com.linweiyun.genshin.core.world.TeyvatWorldInvasion;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class DarkFragment extends Item {

    public DarkFragment(Properties properties) {
        super(properties.rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;

        TeyvatWorldInvasion invasion = TeyvatWorldInvasion.get(serverLevel);
        if (invasion.isInvaded()) {
            serverPlayer.sendSystemMessage(Component.literal("提瓦特世界已经入侵！"));
            return InteractionResult.FAIL;
        }

        invasion.setInvaded(true);

        for (ServerPlayer p : serverLevel.getServer().getPlayerList().getPlayers()) {
            NetworkManager.setInvasionStatusToPlayer(p, true);
        }

        PlayerCharactersAttachment playerData = serverPlayer.getData(
                AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter shenhe = ModCharacters.getByUUID(135001);
        if (shenhe != null && !playerData.hasCharacter(135001)) {
            playerData.addCharacterToPlayer(serverPlayer, shenhe);
            playerData.setPartyCharacterToPlayer(serverPlayer, 0, 135001);
            playerData.syncToPlayer(serverPlayer);
        }

        ItemStack stack = player.getItemInHand(hand);
        stack.shrink(1);

        serverPlayer.sendSystemMessage(Component.literal("提瓦特世界入侵已激活！"));
        serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("提瓦特的力量降临于此世..."), false);

        return InteractionResult.CONSUME;
    }
}