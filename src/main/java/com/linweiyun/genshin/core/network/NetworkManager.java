package com.linweiyun.genshin.core.network;

import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.content.items.component.ArtifactStatsComponent;
import com.linweiyun.genshin.core.attachment.AdventurerInfoAttachment;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.system.registry.register.ModCharacters;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.registry.register.ModDataComponents;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class NetworkManager {
  private static final Random RANDOM = new Random();
  private static final Logger LOGGER = LogUtils.getLogger();
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

  // ========== 冒险者信息同步 ==========
  @RPCPacket("adventurerInfoRPCPacket")
  public static void adventurerInfoRPCPacket(RPCSender sender, CompoundTag data) {
    if (sender.isServer()) {
      ClientHandler.adventurerInfoClientHandler(data);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      AdventurerInfoAttachment attachment = player.getData(AttachmentRegistration.ADVENTURER_INFO_ATTACHMENT);
      attachment.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), data));
    }
  }

  public static void setAdventurerInfoToServer(CompoundTag data) {
    RPCPacketDistributor.rpcToServer("adventurerInfoRPCPacket", data);
  }

  public static void setAdventurerInfoToPlayer(ServerPlayer player, CompoundTag data) {
    RPCPacketDistributor.rpcToPlayer(player, "adventurerInfoRPCPacket", data);
  }

  // ========== 游戏模式同步 ==========
  //AI genshinModeRPCPacket 服务端增加开启校验：party 里没有 currentHP > 0 的角色则拒绝开启
  @RPCPacket("genshinModeRPCPacket")
  public static void genshinModeRPCPacket(RPCSender sender, boolean isGenshinMode) {
    if (sender.isServer()) {
      ClientHandler.genshinModeClientHandler(isGenshinMode);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      if (isGenshinMode && !hasAlivePartyCharacter(player)) {
        player.sendSystemMessage(Component.literal("队伍中没有生命值大于0的角色，无法开启原神模式"));
        setGenshinModeToPlayer(player, false);
        return;
      }
      player.setData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT.get(), isGenshinMode);
      //test 统一由服务端发消息，防止客户端先发成功消息再收到拒绝提示的乱序问题
      player.sendSystemMessage(Component.literal(isGenshinMode ? "已进入原神模式" : "已退出原神模式"));
    }
  }

  // 检查 party 中是否存在至少一个 currentHP > 0 的角色
  private static boolean hasAlivePartyCharacter(ServerPlayer player) {
    PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    for (int uuid : attachment.getPartyCharacterUUIDs()) {
      if (uuid == 0) continue;
      PGCharacter c = attachment.getCharacterByUUID(uuid);
      if (c != null && c.getData().getCurrentHP() > 0) return true;
    }
    return false;
  }

  public static void setGenshinModeToPlayer(ServerPlayer player, boolean isGenshinMode) {
    RPCPacketDistributor.rpcToPlayer(player, "genshinModeRPCPacket", isGenshinMode);
  }

  public static void setGenshinModeToServer(boolean isGenshinMode) {
    RPCPacketDistributor.rpcToServer("genshinModeRPCPacket", isGenshinMode);
  }

  // ========== 角色整体同步（替代旧的 party + sheet + characterData） ==========
  @RPCPacket("playerCharactersRPCPacket")
  public static void playerCharactersRPCPacket(RPCSender sender, CompoundTag data) {
    if (sender.isServer()) {
      ClientHandler.playerCharactersClientHandler(data);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      attachment.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), data));

      attachment.fixCharacterTypes();
      attachment.bindAllOwners(player); //AI 反序列化后重新绑定所有角色的 ownerPlayer
    }
  }
  public static void setPlayerCharactersToServer(CompoundTag data) {
    RPCPacketDistributor.rpcToServer("playerCharactersRPCPacket", data);
  }
  public static void setPlayerCharactersToPlayer(ServerPlayer player, CompoundTag data) {
    RPCPacketDistributor.rpcToPlayer(player, "playerCharactersRPCPacket", data);
  }

  // ========== 角色数据同步（单个角色） ==========
  @RPCPacket("characterDataRPCPacket")
  public static void characterDataRPCPacket(RPCSender sender, int uuid, CompoundTag data) {
    if (sender.isServer()) {
      ClientHandler.characterDataClientHandler(uuid, data);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter character = attachment.getCharacterByUUID(uuid);
      if (character != null) {
        character.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), data));
      }
    }
  }

  public static void setCharacterDataToServer(int uuid, CompoundTag data) {
    RPCPacketDistributor.rpcToServer("characterDataRPCPacket", uuid, data);
  }

  public static void setCharacterDataToPlayer(ServerPlayer player, int uuid, CompoundTag data) {
    RPCPacketDistributor.rpcToPlayer(player, "characterDataRPCPacket", uuid, data);
  }

  // ========== 角色编队 - 设置队伍角色 ==========
  @RPCPacket("setPartyCharacterRPCPacket")
  public static void setPartyCharacterRPCPacket(RPCSender sender, int index, int characterUUID) {
    if (sender.isServer()) {
      ClientHandler.setPartyCharacterClientHandler(index, characterUUID);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      attachment.setPartyCharacter(index, characterUUID);
    }
  }

  public static void setPartyCharacterToServer(int index, int characterUUID) {
    RPCPacketDistributor.rpcToServer("setPartyCharacterRPCPacket", index, characterUUID);
  }

  public static void setPartyCharacterToPlayer(ServerPlayer player, int index, int characterUUID) {
    RPCPacketDistributor.rpcToPlayer(player, "setPartyCharacterRPCPacket", index, characterUUID);
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

  // ========== 角色编队 - 移除队伍角色 ==========
  @RPCPacket("removePartyCharacterRPCPacket")
  public static void removePartyCharacterRPCPacket(RPCSender sender, int index) {
    if (sender.isServer()) {
      ClientHandler.removePartyCharacterClientHandler(index);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      attachment.removePartyCharacter(index);
    }
  }

  public static void removePartyCharacterToServer(int index) {
    RPCPacketDistributor.rpcToServer("removePartyCharacterRPCPacket", index);
  }

  public static void removePartyCharacterToPlayer(ServerPlayer player, int index) {
    RPCPacketDistributor.rpcToPlayer(player, "removePartyCharacterRPCPacket", index);
  }

  // ========== 角色库存 - 添加角色 ==========
  @RPCPacket("addCharacterRPCPacket")
  public static void addCharacterRPCPacket(RPCSender sender, CompoundTag characterData) {
    if (sender.isServer()) {
      ClientHandler.addCharacterClientHandler(characterData);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter character = new PGCharacter();
      character.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), characterData));
      attachment.addCharacter(character, player); //AI 绑定 Player
    }
  }

  public static void addCharacterToServer(CompoundTag characterData) {
    RPCPacketDistributor.rpcToServer("addCharacterRPCPacket", characterData);
  }

  public static void addCharacterToPlayer(ServerPlayer player, CompoundTag characterData) {
    RPCPacketDistributor.rpcToPlayer(player, "addCharacterRPCPacket", characterData);
  }

  // ========== 角色库存 - 移除角色 ==========
  @RPCPacket("removeCharacterRPCPacket")
  public static void removeCharacterRPCPacket(RPCSender sender, int uuid) {
    if (sender.isServer()) {
      ClientHandler.removeCharacterClientHandler(uuid);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      attachment.removeCharacter(uuid);
    }
  }

  public static void removeCharacterToServer(int uuid) {
    RPCPacketDistributor.rpcToServer("removeCharacterRPCPacket", uuid);
  }

  public static void removeCharacterToPlayer(ServerPlayer player, int uuid) {
    RPCPacketDistributor.rpcToPlayer(player, "removeCharacterRPCPacket", uuid);
  }

  // ==== 元素战技 ====
  @RPCPacket("characterActiveSkillRPCPacket")
  public static void characterActiveSkillRPCPacket(RPCSender sender, int isLong) {
    if (!sender.isServer()) {
      ServerPlayer serverPlayer = sender.asPlayer();
      PlayerCharactersAttachment attachment =
              serverPlayer.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter currentChar = attachment.getCurrentCharacter();
      if (currentChar != null) {
            currentChar.performElementalSkill(serverPlayer, isLong);

      }
    }
  }
  public static void triggerCharacterSkill(int isLong) {
    RPCPacketDistributor.rpcToServer("characterActiveSkillRPCPacket", isLong);
  }

  // ==== 元素爆发 ====
  @RPCPacket("characterActiveBurstRPCPacket")
  public static void characterActiveBurstRPCPacket(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer serverPlayer = sender.asPlayer();

      PlayerCharactersAttachment attachment =
              serverPlayer.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter currentChar = attachment.getCurrentCharacter();
      if (currentChar != null) {
          currentChar.performElementalBurst(serverPlayer);
      }
    }
  }
  public static void triggerCharacterBurst() {
    RPCPacketDistributor.rpcToServer("characterActiveBurstRPCPacket");
  }

  // ========== 圣遗物升级 ==========
  @RPCPacket("artifactLevelUpRPCPacket")
  public static void artifactLevelUpRPCPacket(RPCSender sender, ItemStack stack, int expAmount) {
    if (sender.isServer()) {
      // S→C：把服务器处理后的 ItemStack 同步给客户端 Screen
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      if (stack.isEmpty() || !(stack.getItem() instanceof ArtifactItem art)) return;
      ArtifactStatsComponent stats = stack.getOrDefault(ModDataComponents.ARTIFACT_STATS.get(), ArtifactStatsComponent.DEFAULT);
      int star = art.getStar();  // star 还在 Item 单例上
      long added = stats.addExp(expAmount, star, art.getType());
      if (added <= 0) return;
      stack.set(ModDataComponents.ARTIFACT_STATS.get(), stats);
      RPCPacketDistributor.rpcToPlayer(player, "artifactLevelUpRPCPacket", stack, 0);
    }
  }

  public static void sendArtifactLevelUpToServer(ItemStack artifact, int expAmount) {
    RPCPacketDistributor.rpcToServer("artifactLevelUpRPCPacket", artifact, expAmount);
  }

  public static void sendArtifactLevelUpToPlayer(ServerPlayer player, ItemStack artifact) {
    RPCPacketDistributor.rpcToPlayer(player, "artifactLevelUpRPCPacket", artifact, 0);
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

      List<PGCharacter> allCharacters = new ArrayList<>(ModCharacters.getAllCharacters());
      if (allCharacters.isEmpty()) {
        serverPlayer.sendSystemMessage(
                Component.translatable("message.pixel_genshin.wish.no_reward"));
        return;
      }

      var rolledCharacter = allCharacters.get(RANDOM.nextInt(allCharacters.size()));
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
        charactersAttachment.addCharacter(rolledCharacter, serverPlayer); //AI 绑定 Player


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

  // ========== 打开角色信息菜单 ==========
  @RPCPacket("openCharacterInfoRPCPacket")
  public static void openCharacterInfoRPCPacket(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer serverPlayer = sender.asPlayer();
      if (serverPlayer == null) return;
      serverPlayer.openMenu(new net.minecraft.world.MenuProvider() {
        @Override
        public Component getDisplayName() {
          return Component.literal("Character Info");
        }
        @Override
        public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player p) {
          return new com.linweiyun.genshin.client.gui.menu.CharacterInfoMenu(containerId, inventory);
        }
      });
    }
  }

  public static void openCharacterInfoScreenToServer() {
    RPCPacketDistributor.rpcToServer("openCharacterInfoRPCPacket");
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