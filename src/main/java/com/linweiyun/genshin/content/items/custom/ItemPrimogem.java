package com.linweiyun.genshin.content.items.custom;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RPCMethod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
  public InteractionResult use(
      Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
    if (!level.isClientSide()) {
      ItemStack stack = player.getItemInHand(usedHand);

      int primogem =
          player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT.get()) + stack.getCount();
      player.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT.get(), primogem);
      System.out.println(player);
      NetworkManager.setPrimogemToPlayer((ServerPlayer) player, primogem);
      player.sendSystemMessage(
          Component.literal(
              "你获得了 " + stack.getCount() + " 个原石，" + "当前原石数量为 " + player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT.get())));
      stack.shrink(stack.getCount());
    }
    return InteractionResult.SUCCESS;
  }
}
