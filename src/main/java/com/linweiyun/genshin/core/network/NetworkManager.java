package com.linweiyun.genshin.core.network;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.registry.register.CharacterRegister;
import com.linweiyun.genshin.core.character.PGCharacterDefine;
import com.linweiyun.genshin.core.skill.CharacterSkillHandler;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class NetworkManager {
  private static final Random RANDOM = new Random();

  // ========== 原石同步 ==========
  @RPCPacket("primogemRPCPacket")
  public static void primogemRPCPacket(RPCSender sender, int amount) {
    if (sender.isServer()) {
      ClientHandler.primogemClientHandler(amount);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      player.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT.get(), amount);
    }
  }

  public static void setPrimogemToServer(int amount) {
    RPCPacketDistributor.rpcToServer("primogemRPCPacket", amount);
  }

  public static void setPrimogemToPlayer(ServerPlayer player, int amount) {
    RPCPacketDistributor.rpcToPlayer(player, "primogemRPCPacket", amount);
  }

  // ========== 游戏模式同步 ==========
  @RPCPacket("genshinModeRPCPacket")
  public static void genshinModeRPCPacket(RPCSender sender, boolean isGenshinMode) {
    if (sender.isServer()) {
      ClientHandler.genshinModeClientHandler(isGenshinMode);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      player.setData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT.get(), isGenshinMode);
    }
  }

  public static void setGenshinModeToPlayer(ServerPlayer player, boolean isGenshinMode) {
    RPCPacketDistributor.rpcToPlayer(player, "genshinModeRPCPacket", isGenshinMode);
  }

  public static void setGenshinModeToServer(boolean isGenshinMode) {
    RPCPacketDistributor.rpcToServer("genshinModeRPCPacket", isGenshinMode);
  }

  // ========== 角色数据整体同步（替代旧的 party + sheet + characterData） ==========
  @RPCPacket("playerCharactersRPCPacket")
  public static void playerCharactersRPCPacket(RPCSender sender, CompoundTag data) {
    if (sender.isServer()) {
      ClientHandler.playerCharactersClientHandler(data);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      attachment.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), data));
    }
  }

  public static void setPlayerCharactersToServer(CompoundTag data) {
    RPCPacketDistributor.rpcToServer("playerCharactersRPCPacket", data);
  }

  public static void setPlayerCharactersToPlayer(ServerPlayer player, CompoundTag data) {
    RPCPacketDistributor.rpcToPlayer(player, "playerCharactersRPCPacket", data);
  }

  // ========== 角色选择同步 ==========
  @RPCPacket("characterSelectionRPCPacket")
  public static void characterSelectionRPCPacket(RPCSender sender, int index) {
    if (sender.isServer()) {
      ClientHandler.characterSelectionClientHandler(index);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      attachment.setCurrentCharacterIndex(index);
    }
  }

  public static void setCharacterSelectionToPlayer(ServerPlayer player, int index) {
    RPCPacketDistributor.rpcToPlayer(player, "characterSelectionRPCPacket", index);
  }

  public static void setCharacterSelectionToServer(int index) {
    RPCPacketDistributor.rpcToServer("characterSelectionRPCPacket", index);
  }


  // ========== 角色技能触发 ==========
  // 1代表元素战技，2代表元素burst
  @RPCPacket("characterActiveSkillRPCPacket")
  public static void characterActiveSkillRPCPacket(RPCSender sender, int skill) {
    if (!sender.isServer()) {
      ServerPlayer serverPlayer = sender.asPlayer();
      PlayerCharactersAttachment attachment =
              serverPlayer.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacterData currentChar = attachment.getCurrentCharacter();
      if (currentChar != null) {
        PGCharacterDefine def = currentChar.getDefinition();
        if (def != null) {
          if (skill == 1) {
            CharacterSkillHandler.performElementalSkill(serverPlayer, currentChar, def);
          } else if (skill == 2) {
            CharacterSkillHandler.performElementalBurst(serverPlayer, currentChar, def);
          }
        }
      }
    }
  }

  public static void triggerCharacterSkill(int skill) {
    RPCPacketDistributor.rpcToServer("characterActiveSkillRPCPacket", skill);
  }

  // ========== 祈愿系统 ==========
  @RPCPacket("primogemWish")
  public static void primogemWish(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer serverPlayer = sender.asPlayer();
      if (serverPlayer == null) return;

      int primogem = serverPlayer.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
      if (primogem < 160) {
        serverPlayer.sendSystemMessage(
                Component.translatable("message.pixel_genshin.wish.not_enough_primogem"));
        return;
      }

      serverPlayer.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, primogem - 160);
      setPrimogemToPlayer(serverPlayer, primogem - 160);

      PlayerCharactersAttachment charactersAttachment =
              serverPlayer.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);

      List<PGCharacterDefine> allCharacters = new ArrayList<>(CharacterRegister.getAllCharacters());
      if (allCharacters.isEmpty()) {
        serverPlayer.sendSystemMessage(
                Component.translatable("message.pixel_genshin.wish.no_reward"));
        return;
      }

      PGCharacterDefine rolledCharacter = allCharacters.get(RANDOM.nextInt(allCharacters.size()));
      Component characterName = rolledCharacter.getName().copy().withStyle(ChatFormatting.GOLD);

      if (charactersAttachment.hasCharacter(rolledCharacter.getCharacterUUID())) {
        int compensation = 75;
        int newPrimogem = serverPlayer.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
        serverPlayer.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, newPrimogem + compensation);
        setPrimogemToPlayer(serverPlayer, newPrimogem + compensation);

        serverPlayer.sendSystemMessage(
                Component.translatable("message.pixel_genshin.wish.owned_character",
                        characterName,
                        Component.literal(String.valueOf(compensation)).withStyle(ChatFormatting.AQUA)));
      } else {
        double baseHP = rolledCharacter.getBaseStat(
                ModAttributes.MAX_HP.value());
        double baseATK = rolledCharacter.getBaseStat(
                ModAttributes.ATK.value());
        double baseDEF = rolledCharacter.getBaseStat(
                ModAttributes.DEF.value());
        PGCharacterData newChar = new PGCharacterData(
                rolledCharacter.getCharacterUUID(), baseHP, baseATK, baseDEF);
        charactersAttachment.addCharacter(newChar);


        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, serverPlayer.registryAccess());
        charactersAttachment.serialize(output);
        setPlayerCharactersToPlayer(serverPlayer, output.buildResult());

        serverPlayer.sendSystemMessage(
                Component.translatable("message.pixel_genshin.wish.draw_character", characterName));
      }
    }
  }

  public static void wishEventToServer() {
    RPCPacketDistributor.rpcToServer("primogemWish");
  }

  public static void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
    int remaining = stack.getCount();

    // 1. 先尝试堆叠到已有的相同物品
    net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
    for (int i = 0; i < inventory.getContainerSize(); i++) {
      if (remaining <= 0) break;

      ItemStack existing = inventory.getItem(i);
      if (existing.isEmpty()) continue;

      if (ItemStack.isSameItemSameComponents(existing, stack)
          && existing.getCount() < existing.getMaxStackSize()) {
        int canAdd = Math.min(remaining, existing.getMaxStackSize() - existing.getCount());
        existing.grow(canAdd);
        remaining -= canAdd;
      }
    }

    // 2. 如果还有剩余，找空槽位放入
    if (remaining > 0) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
        if (remaining <= 0) break;

        ItemStack existing = inventory.getItem(i);
        if (existing.isEmpty()) {
          int toAdd = Math.min(remaining, stack.getMaxStackSize());
          inventory.setItem(i, stack.copyWithCount(toAdd));
          remaining -= toAdd;
        }
      }
    }

    // 3. 如果还有剩余，掉落在玩家附近
    if (remaining > 0) {
      ItemStack dropStack = stack.copyWithCount(remaining);
      net.minecraft.world.entity.item.ItemEntity itemEntity =
          new net.minecraft.world.entity.item.ItemEntity(
              player.level(), player.getX(), player.getY() + 0.5, player.getZ(), dropStack);
      player.level().addFreshEntity(itemEntity);
    }
  }
}
