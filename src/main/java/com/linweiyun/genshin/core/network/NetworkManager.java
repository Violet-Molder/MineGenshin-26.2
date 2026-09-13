package com.linweiyun.genshin.core.network;

import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.content.items.component.ArtifactStatsComponent;
import com.linweiyun.genshin.core.attachment.*;
import com.linweiyun.genshin.content.items.artifact.inventory.ArtifactInventory;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.system.registry.register.ModCharacters;
import com.linweiyun.genshin.core.system.registry.register.ModDataComponents;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
      player.sendSystemMessage(Component.literal(isGenshinMode ? "已进入原神模式" : "已退出原神模式"));
    }
  }

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

  @RPCPacket("playerCharactersRPCPacket")
  public static void playerCharactersRPCPacket(RPCSender sender, CompoundTag data) {
    if (sender.isServer()) {
      ClientHandler.playerCharactersClientHandler(data);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      attachment.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), data));
      attachment.fixCharacterTypes();
      attachment.bindAllOwners(player);
    }
  }
  public static void setPlayerCharactersToServer(CompoundTag data) {
    RPCPacketDistributor.rpcToServer("playerCharactersRPCPacket", data);
  }
  public static void setPlayerCharactersToPlayer(ServerPlayer player, CompoundTag data) {
    RPCPacketDistributor.rpcToPlayer(player, "playerCharactersRPCPacket", data);
  }

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

  @RPCPacket("addCharacterRPCPacket")
  public static void addCharacterRPCPacket(RPCSender sender, CompoundTag characterData) {
    if (sender.isServer()) {
      ClientHandler.addCharacterClientHandler(characterData);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter character = new PGCharacter();
      character.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), characterData));
      attachment.addCharacter(character, player);
    }
  }

  public static void addCharacterToServer(CompoundTag characterData) {
    RPCPacketDistributor.rpcToServer("addCharacterRPCPacket", characterData);
  }

  public static void addCharacterToPlayer(ServerPlayer player, CompoundTag characterData) {
    RPCPacketDistributor.rpcToPlayer(player, "addCharacterRPCPacket", characterData);
  }

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

  @RPCPacket("artifactLevelUpRPCPacket")
  public static void artifactLevelUpRPCPacket(RPCSender sender, ItemStack stack, int expAmount) {
    if (sender.isServer()) {
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      if (stack.isEmpty() || !(stack.getItem() instanceof ArtifactItem art)) return;
      ArtifactStatsComponent stats = stack.getOrDefault(ModDataComponents.ARTIFACT_STATS.get(), ArtifactStatsComponent.DEFAULT);
      int star = art.getStar();
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter currentChar = attachment.getCurrentCharacter();
      if (currentChar != null) {
        PGCharacterData charData = currentChar.getData();
        if (charData != null) {
          ArtifactInventory inv = charData.getArtifactInventory();
          int slot = ArtifactInventory.typeToSlot(art.getType());
          stats.setOnStatsChanged(() -> inv.markDirty(slot));
        }
      }
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

  @RPCPacket("equipOrSwapArtifactRPCPacket")
  public static void equipOrSwapArtifactRPCPacket(RPCSender sender, int artifactSlotIndex, int inventorySlotIndex) {
    if (sender.isServer()) {
      ClientHandler.equipOrSwapArtifactClientHandler(artifactSlotIndex, inventorySlotIndex);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter currentChar = attachment.getCurrentCharacter();
      if (currentChar == null || currentChar.getData() == null) return;
      PGCharacterData charData = currentChar.getData();
      ArtifactInventory artifactInv = charData.getArtifactInventory();

      Backpack backpack = player.getData(AttachmentRegistration.BACKPACK_ATTACHMENT);
      var artifactList = backpack.getCategoryList(Backpack.Category.ARTIFACTS);
      if (inventorySlotIndex < 0 || inventorySlotIndex >= artifactList.size()) return;
      ItemStack newArtifact = artifactList.get(inventorySlotIndex);
      if (newArtifact.isEmpty() || !(newArtifact.getItem() instanceof ArtifactItem)) return;
      if (artifactSlotIndex < 0 || artifactSlotIndex >= ArtifactInventory.SLOT_COUNT) return;
      if (!ArtifactInventory.isValidForSlot(artifactSlotIndex, newArtifact)) return;

      ItemStack oldArtifact = artifactInv.getItem(artifactSlotIndex);

      artifactList.set(inventorySlotIndex, ItemStack.EMPTY);
      artifactInv.setItem(artifactSlotIndex, newArtifact.copy());

      if (!oldArtifact.isEmpty()) {
        backpack.addItemToCategory(Backpack.Category.ARTIFACTS, oldArtifact.copy());
      }

      currentChar.recalculateDirtyArtifactSlots();
      attachment.syncToPlayer(player);
    }
  }

  public static void sendEquipOrSwapArtifactToServer(int artifactSlotIndex, int inventorySlotIndex) {
    RPCPacketDistributor.rpcToServer("equipOrSwapArtifactRPCPacket", artifactSlotIndex, inventorySlotIndex);
  }

  @RPCPacket("unequipArtifactRPCPacket")
  public static void unequipArtifactRPCPacket(RPCSender sender, int artifactSlotIndex) {
    if (sender.isServer()) {
      ClientHandler.unequipArtifactClientHandler(artifactSlotIndex);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter currentChar = attachment.getCurrentCharacter();
      if (currentChar == null || currentChar.getData() == null) return;
      PGCharacterData charData = currentChar.getData();
      ArtifactInventory artifactInv = charData.getArtifactInventory();

      if (artifactSlotIndex < 0 || artifactSlotIndex >= ArtifactInventory.SLOT_COUNT) return;
      ItemStack oldArtifact = artifactInv.getItem(artifactSlotIndex);
      if (oldArtifact.isEmpty()) return;

      artifactInv.setItem(artifactSlotIndex, ItemStack.EMPTY);
//      LOGGER.info("NetSetItem");

      Backpack backpack = player.getData(AttachmentRegistration.BACKPACK_ATTACHMENT);
      backpack.addItemToCategory(Backpack.Category.ARTIFACTS, oldArtifact.copy());

      currentChar.recalculateDirtyArtifactSlots();
      attachment.syncToPlayer(player);
    }
  }

  public static void sendUnequipArtifactToServer(int artifactSlotIndex) {
    RPCPacketDistributor.rpcToServer("unequipArtifactRPCPacket", artifactSlotIndex);
  }

//
  

  @RPCPacket("openBackpackRPCPacket")
  public static void openBackpackRPCPacket(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer serverPlayer = sender.asPlayer();
      if (serverPlayer == null) return;
      PlayerUIMenuType.openUI(serverPlayer,
              Identifier.fromNamespaceAndPath("minegenshin", "backpack"));
    }
  }
  public static void openBackpackMenuToServer() {
    RPCPacketDistributor.rpcToServer("openBackpackRPCPacket");
  }

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
        charactersAttachment.addCharacter(rolledCharacter, serverPlayer);

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
          return new com.linweiyun.genshin.render.gui.menu.CharacterInfoMenu(containerId, inventory);
        }
      });
    }
  }

  public static void openCharacterInfoScreenToServer() {
    RPCPacketDistributor.rpcToServer("openCharacterInfoRPCPacket");
  }

  @RPCPacket("activateArtifactRPCPacket")
  public static void activateArtifactRPCPacket(RPCSender sender, int inventorySlotIndex) {
    if (sender.isServer()) {
      // 服务端 → 客户端的同步分支：服务端完成激活后，把结果同步回客户端
      ClientHandler.activateArtifactClientHandler(inventorySlotIndex);
    } else {
      // 客户端 → 服务端：请求服务端权威执行激活
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      Backpack backpack = player.getData(AttachmentRegistration.BACKPACK_ATTACHMENT);
      int globalSlot = getCategoryOffset(Backpack.Category.ARTIFACTS) + inventorySlotIndex;
      ItemStack stack = backpack.getItem(globalSlot);
      if (stack.isEmpty() || !(stack.getItem() instanceof ArtifactItem)) return;

      ArtifactStatsComponent stats = stack.getOrDefault(
              ModDataComponents.ARTIFACT_STATS.get(), ArtifactStatsComponent.DEFAULT);
      // 已激活的不重复激活
      if (stats.activated) return;

      // 抽取词条并标记为已激活
      ArtifactItem.initializeArtifactStackIfNeeded(stack);
      // 写回背包（持久化）
      backpack.setItem(globalSlot, stack);

      LOGGER.info("服务端激活圣遗物: 背包索引 {}", inventorySlotIndex);
    }
  }

  public static void sendActivateArtifactToServer(int inventorySlotIndex) {
    RPCPacketDistributor.rpcToServer("activateArtifactRPCPacket", inventorySlotIndex);
  }

  public static void sendActivateArtifactToPlayer(ServerPlayer player, int inventorySlotIndex) {
    RPCPacketDistributor.rpcToPlayer(player, "activateArtifactRPCPacket", inventorySlotIndex);
  }

  /**
   * 计算某分类在 Backpack 中的全局起始槽位索引。
   * 与 BackpackMenu 里的同名方法保持一致。
   */
  private static int getCategoryOffset(Backpack.Category target) {
    int offset = 0;
    for (var category : Backpack.Category.values()) {
      if (category == target) break;
      offset += category.maxCapacity;
    }
    return offset;
  }

  @RPCPacket("backpackTakeOutFromSlotRPCPacket")
  public static void backpackTakeOutFromSlotRPCPacket(RPCSender sender, int slotIndex) {
    if (sender.isServer()) {
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      Backpack backpack = player.getData(AttachmentRegistration.BACKPACK_ATTACHMENT);
      ItemStack removed = backpack.getItem(slotIndex);
      if (removed.isEmpty()) return;
      backpack.setItem(slotIndex, ItemStack.EMPTY);
      giveItemToPlayer(player, removed);
    }
  }

  public static void sendBackpackTakeOutFromSlotToServer(int slotIndex) {
    RPCPacketDistributor.rpcToServer("backpackTakeOutFromSlotRPCPacket", slotIndex);
  }
  
  public static void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
    int remaining = stack.getCount();

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

    if (remaining > 0) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
        if (remaining <= 0) break;

        ItemStack existing = inventory.getItem(i);
        if (existing.isEmpty()) {
          int toAdd = Math.min(remaining, stack.getMaxStackSize());
          inventory.setItem(i, stack.copyWithCount(toAdd));
//          LOGGER.info("NetSetItem4");
          remaining -= toAdd;
        }
      }
    }

    if (remaining > 0) {
      ItemStack dropStack = stack.copyWithCount(remaining);
      net.minecraft.world.entity.item.ItemEntity itemEntity =
              new net.minecraft.world.entity.item.ItemEntity(
                      player.level(), player.getX(), player.getY() + 0.5, player.getZ(), dropStack);
      player.level().addFreshEntity(itemEntity);
    }
  }

  @RPCPacket("ascendAdventureRankRPCPacket")
  public static void ascendAdventureRankRPCPacket(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      if (player.experienceLevel < 30) {
        player.sendSystemMessage(Component.literal("经验等级不足30级，无法进行冒险等级突破"));
        return;
      }
      AdventurerInfoAttachment advInfo = player.getData(AttachmentRegistration.ADVENTURER_INFO_ATTACHMENT);
      if (!advInfo.canBreakthroughWorldLevel()) {
        player.sendSystemMessage(Component.literal("不满足冒险等级突破条件"));
        return;
      }
      player.giveExperienceLevels(-30);
      advInfo.breakthroughWorldLevel();
      advInfo.syncToPlayer(player);
      player.sendSystemMessage(Component.literal("冒险等级突破成功！当前世界等级: " + advInfo.getWorldLevel()));
    }
  }

  public static void sendAscendAdventureRankToServer() {
    RPCPacketDistributor.rpcToServer("ascendAdventureRankRPCPacket");
  }

  @RPCPacket("ascendCharacterRPCPacket")
  public static void ascendCharacterRPCPacket(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      if (player.experienceLevel < 30) {
        player.sendSystemMessage(Component.literal("经验等级不足30级，无法进行角色突破"));
        return;
      }
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter character = attachment.getCurrentCharacter();
      if (character == null || character.getData() == null) {
        player.sendSystemMessage(Component.literal("当前没有选中角色"));
        return;
      }
      int maxLevelForPhase = character.getData().getAscensionPhase() == 0
              ? 20
              : Math.min((character.getData().getAscensionPhase() + 3) * 10, 90);
      if (character.getData().getLevel() < maxLevelForPhase) {
        player.sendSystemMessage(Component.literal("角色未达到当前突破阶段最大等级，无法突破"));
        return;
      }
      if (character.getData().getLevel() >= 90) {
        player.sendSystemMessage(Component.literal("角色已达最高等级"));
        return;
      }
      player.giveExperienceLevels(-30);
      character.ascend();
      attachment.syncToPlayer(player);
      player.sendSystemMessage(Component.literal("角色突破成功！当前突破阶段: " + character.getData().getAscensionPhase()));
    }
  }

  public static void sendAscendCharacterToServer() {
    RPCPacketDistributor.rpcToServer("ascendCharacterRPCPacket");
  }

  @RPCPacket("downgradeWorldLevelRPCPacket")
  public static void downgradeWorldLevelRPCPacket(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      AdventurerInfoAttachment advInfo = player.getData(AttachmentRegistration.ADVENTURER_INFO_ATTACHMENT);
      if (!advInfo.canDowngradeWorldLevel()) {
        player.sendSystemMessage(Component.literal("不满足降低世界等级条件"));
        return;
      }
      advInfo.downgradeWorldLevel();
      advInfo.syncToPlayer(player);
      player.sendSystemMessage(Component.literal("世界等级已降低至 " + advInfo.getWorldLevel()));
    }
  }

  public static void sendDowngradeWorldLevelToServer() {
    RPCPacketDistributor.rpcToServer("downgradeWorldLevelRPCPacket");
  }

  @RPCPacket("restoreWorldLevelRPCPacket")
  public static void restoreWorldLevelRPCPacket(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      AdventurerInfoAttachment advInfo = player.getData(AttachmentRegistration.ADVENTURER_INFO_ATTACHMENT);
      if (!advInfo.canRestoreWorldLevel()) {
        player.sendSystemMessage(Component.literal("世界等级无需还原"));
        return;
      }
      advInfo.restoreWorldLevel();
      advInfo.syncToPlayer(player);
      player.sendSystemMessage(Component.literal("世界等级已还原至 " + advInfo.getWorldLevel()));
    }
  }

  public static void sendRestoreWorldLevelToServer() {
    RPCPacketDistributor.rpcToServer("restoreWorldLevelRPCPacket");
  }
}