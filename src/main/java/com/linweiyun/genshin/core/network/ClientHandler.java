package com.linweiyun.genshin.core.network;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueInput;

public class ClientHandler {
  public static void primogemClientHandler(int amount) {
    Player player = Minecraft.getInstance().player;
    if (player == null) return;
    player.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, amount);
  }

  public static void genshinModeClientHandler(boolean isGenshinMode) {
    Player player = Minecraft.getInstance().player;
    if (player == null) return;
    player.setData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT, isGenshinMode);
  }
  public static void playerCharactersClientHandler(CompoundTag data) {
    Player player = Minecraft.getInstance().player;
    if (player == null) return;
    PlayerCharactersAttachment attachment =
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    attachment.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), data));
  }
  public static void characterSelectionClientHandler(int index) {
    Player player = Minecraft.getInstance().player;
    if (player == null) return;
    PlayerCharactersAttachment attachment =
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    attachment.setCurrentCharacterIndex(index);
  }
}
