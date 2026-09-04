package com.linweiyun.genshin.core.network;

import com.linweiyun.genshin.core.attachment.AdventurerInfoAttachment;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.Logger;

import java.util.Objects;

public class ClientHandler {
  private static final Logger LOGGER = LogUtils.getLogger();
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

  public static void adventurerInfoClientHandler(CompoundTag data) {
    Minecraft mc = Minecraft.getInstance();
    mc.execute(() -> {
      if (mc.player == null || mc.player.isRemoved()) {
        mc.execute(() -> adventurerInfoClientHandler(data));
        return;
      }
      AdventurerInfoAttachment attachment =
              mc.player.getData(AttachmentRegistration.ADVENTURER_INFO_ATTACHMENT);
      attachment.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, mc.player.registryAccess(), data));
    });
  }
  public static void playerCharactersClientHandler(CompoundTag data) {
    Minecraft mc = Minecraft.getInstance();
    mc.execute(() -> {
      if (mc.player == null || mc.player.isRemoved()) {
        mc.execute(() -> playerCharactersClientHandler(data));
        return;
      }
      PlayerCharactersAttachment attachment =
              mc.player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      attachment.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, mc.player.registryAccess(), data));
      attachment.fixCharacterTypes();
    });
  }

  public static void characterDataClientHandler(int uuid, CompoundTag data) {
    Player player = net.minecraft.client.Minecraft.getInstance().player;
    if (player == null) return;
    PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    PGCharacter character = attachment.getCharacterByUUID(uuid);
    if (character != null) {
      character.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), data));
    }
  }

  public static void characterSelectionClientHandler(int index) {
    Player player = Minecraft.getInstance().player;
    if (player == null) return;
    PlayerCharactersAttachment attachment =
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    attachment.setCurrentCharacterIndex(index);
  }

  public static void setPartyCharacterClientHandler(int index, int characterUUID) {
    Player player = Minecraft.getInstance().player;
    if (player == null) return;
    PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    attachment.setPartyCharacter(index, characterUUID);
  }

  public static void removePartyCharacterClientHandler(int index) {
    Player player = Minecraft.getInstance().player;
    if (player == null) return;
    PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    attachment.removePartyCharacter(index);
  }

  public static void addCharacterClientHandler(CompoundTag characterData) {
    Player player = Minecraft.getInstance().player;
    if (player == null) return;
    PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    PGCharacter character = new PGCharacter();
    character.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), characterData));
    attachment.addCharacter(character);
  }

  public static void removeCharacterClientHandler(int uuid) {
    Player player = Minecraft.getInstance().player;
    if (player == null) return;
    PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    attachment.removeCharacter(uuid);
  }
}