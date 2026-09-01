package com.linweiyun.genshin.content.items.custom;

import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.content.entities.attachments.attachment.PlayerPrimogemAttachment;
import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RPCMethod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemPrimogem extends Item {

  public ItemPrimogem(Properties properties) {
    super(properties);
  }

  @Override
  @RPCMethod
  public @NotNull InteractionResultHolder<ItemStack> use(
      Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
    if (!level.isClientSide) {
      ItemStack stack = player.getItemInHand(usedHand);
      CharacterParty characterParty = player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
      ItemStack characterStack = characterParty.getCurrentCharacter();
      if (characterStack.getItem() instanceof PlayerCharacter character) {
        character.ascend(characterStack, player);
      }

      PlayerPrimogemAttachment primogem =
          player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
      primogem.addPrimogem(stack.getCount(), (ServerPlayer) player);
      player.sendSystemMessage(
          Component.literal(
              "你获得了 " + stack.getCount() + " 个原石，" + "当前原石数量为 " + primogem.getPrimogem()));
      stack.shrink(stack.getCount());
    }
    return InteractionResultHolder.success(player.getItemInHand(usedHand));
  }
}
