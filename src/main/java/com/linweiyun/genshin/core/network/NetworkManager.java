package com.linweiyun.genshin.core.network;

import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.content.items.weapon.WeaponItem;
import com.linweiyun.genshin.content.items.component.ArtifactStatsComponent;
import com.linweiyun.genshin.core.attachment.*;
import com.linweiyun.genshin.content.items.artifact.inventory.ArtifactInventory;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.system.combat.action.ActionManager;
import com.linweiyun.genshin.core.system.combat.action.InterruptReason;
import com.linweiyun.genshin.core.system.registry.register.ModDataComponents;
import com.linweiyun.genshin.render.gui.menu.BackpackMenu;
import com.linweiyun.genshin.render.gui.menu.CharacterInfoMenu;
import com.linweiyun.genshin.core.system.wish.WishSystem;
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

  public static void init() {}

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
        player.sendSystemMessage(Component.translatable("message.minegenshin.no_alive_character"));
        setGenshinModeToPlayer(player, false);
        return;
      }
      if (isGenshinMode) {
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter current = attachment.getCurrentCharacter();
        if (current == null || current.getData().getCurrentHP() <= 0) {
          for (int i = 0; i < 4; i++) {
            PGCharacter c = attachment.getPartyCharacter(i);
            if (c != null && c.getData().getCurrentHP() > 0) {
              attachment.setCurrentCharacterIndex(i);
              break;
            }
          }
        }
        attachment.syncToPlayer(player);
        setGenshinModeToPlayer(player, true);
      } else {
        setGenshinModeToPlayer(player, false);
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

      // ⭐ 切换角色：服务端无条件打断当前动作
      // 防止"服务端还没同步新角色、普攻 RPC 先到、request() 被旧角色动作拒绝"的情况
      ActionManager.get(player).interrupt(InterruptReason.SWITCH_CHARACTER);

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

  // ===== 注意：characterActiveSkillRPCPacket / characterActiveBurstRPCPacket /
  //       characterNormalAttackRPCPacket / characterChargedAttackRPCPacket
  //       已迁移至 ActionServer。 =====

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

      if (artifactSlotIndex < 0 || artifactSlotIndex >= ArtifactInventory.SLOT_COUNT) return;

      Backpack backpack = player.getData(AttachmentRegistration.BACKPACK_ATTACHMENT);

      if (artifactSlotIndex == ArtifactInventory.SLOT_WEAPON) {
        var weaponList = backpack.getCategoryList(Backpack.Category.WEAPONS);
        if (inventorySlotIndex < 0 || inventorySlotIndex >= weaponList.size()) return;
        ItemStack newWeapon = weaponList.get(inventorySlotIndex);
        if (newWeapon.isEmpty() || !(newWeapon.getItem() instanceof WeaponItem)) return;
        if (!ArtifactInventory.isValidForSlot(artifactSlotIndex, newWeapon)) return;
        if (!isWeaponCompatibleWithCharacter(currentChar, newWeapon)) return;

        ItemStack oldWeapon = artifactInv.getItem(artifactSlotIndex);
        weaponList.set(inventorySlotIndex, ItemStack.EMPTY);
        artifactInv.setItem(artifactSlotIndex, newWeapon.copy());
        if (!oldWeapon.isEmpty()) {
          backpack.addItemToCategory(Backpack.Category.WEAPONS, oldWeapon.copy());
        }
      } else {
        var artifactList = backpack.getCategoryList(Backpack.Category.ARTIFACTS);
        if (inventorySlotIndex < 0 || inventorySlotIndex >= artifactList.size()) return;
        ItemStack newArtifact = artifactList.get(inventorySlotIndex);
        if (newArtifact.isEmpty() || !(newArtifact.getItem() instanceof ArtifactItem)) return;
        if (!ArtifactInventory.isValidForSlot(artifactSlotIndex, newArtifact)) return;

        ItemStack oldArtifact = artifactInv.getItem(artifactSlotIndex);
        artifactList.set(inventorySlotIndex, ItemStack.EMPTY);
        artifactInv.setItem(artifactSlotIndex, newArtifact.copy());
        if (!oldArtifact.isEmpty()) {
          backpack.addItemToCategory(Backpack.Category.ARTIFACTS, oldArtifact.copy());
        }
      }

      currentChar.recalculateDirtyArtifactSlots();
      attachment.syncToPlayer(player);
    }
  }

  private static boolean isWeaponCompatibleWithCharacter(PGCharacter character, ItemStack weaponStack) {
    if (weaponStack.isEmpty() || !(weaponStack.getItem() instanceof WeaponItem)) return false;
    Class<? extends WeaponItem> allowedClass = character.getAllowedWeaponClass();
    return allowedClass.isInstance(weaponStack.getItem());
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

      Backpack backpack = player.getData(AttachmentRegistration.BACKPACK_ATTACHMENT);
      if (artifactSlotIndex == ArtifactInventory.SLOT_WEAPON) {
        backpack.addItemToCategory(Backpack.Category.WEAPONS, oldArtifact.copy());
      } else {
        backpack.addItemToCategory(Backpack.Category.ARTIFACTS, oldArtifact.copy());
      }

      currentChar.recalculateDirtyArtifactSlots();
      attachment.syncToPlayer(player);
    }
  }

  public static void sendUnequipArtifactToServer(int artifactSlotIndex) {
    RPCPacketDistributor.rpcToServer("unequipArtifactRPCPacket", artifactSlotIndex);
  }

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
      WishSystem.performWish(serverPlayer);
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
          return new CharacterInfoMenu(containerId, inventory);
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
      ClientHandler.activateArtifactClientHandler(inventorySlotIndex);
    } else {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      Backpack backpack = player.getData(AttachmentRegistration.BACKPACK_ATTACHMENT);
      int globalSlot = getCategoryOffset(Backpack.Category.ARTIFACTS) + inventorySlotIndex;
      ItemStack stack = backpack.getItem(globalSlot);
      if (stack.isEmpty() || !(stack.getItem() instanceof ArtifactItem)) return;

      ArtifactStatsComponent stats = stack.getOrDefault(
              ModDataComponents.ARTIFACT_STATS.get(), ArtifactStatsComponent.DEFAULT);
      if (stats.activated) return;

      ArtifactItem.initializeArtifactStackIfNeeded(stack);
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
        player.sendSystemMessage(Component.translatable("message.minegenshin.adventure_rank_exp_low"));
        return;
      }
      AdventurerInfoAttachment advInfo = player.getData(AttachmentRegistration.ADVENTURER_INFO_ATTACHMENT);
      if (!advInfo.canBreakthroughWorldLevel()) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.adventure_rank_breakthrough_not_met"));
        return;
      }
      player.giveExperienceLevels(-30);
      advInfo.breakthroughWorldLevel();
      advInfo.syncToPlayer(player);
      player.sendSystemMessage(Component.translatable("message.minegenshin.adventure_rank_breakthrough_success", advInfo.getWorldLevel()));
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
        player.sendSystemMessage(Component.translatable("message.minegenshin.character_exp_low"));
        return;
      }
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter character = attachment.getCurrentCharacter();
      if (character == null || character.getData() == null) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.no_character_selected"));
        return;
      }
      int maxLevelForPhase = character.getData().getAscensionPhase() == 0
              ? 20
              : Math.min((character.getData().getAscensionPhase() + 3) * 10, 90);
      if (character.getData().getLevel() < maxLevelForPhase) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.character_not_max_level"));
        return;
      }
      if (character.getData().getLevel() >= 90) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.character_max_level_reached"));
        return;
      }
      player.giveExperienceLevels(-30);
      character.ascend();
      attachment.syncToPlayer(player);
      player.sendSystemMessage(Component.translatable("message.minegenshin.character_breakthrough_success", character.getData().getAscensionPhase()));
    }
  }

  public static void sendAscendCharacterToServer() {
    RPCPacketDistributor.rpcToServer("ascendCharacterRPCPacket");
  }

  @RPCPacket("ascendWeaponRPCPacket")
  public static void ascendWeaponRPCPacket(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      int primogem = player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
      if (primogem < 1600) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.weapon_primogem_low"));
        return;
      }
      if (player.experienceLevel < 10) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.weapon_exp_low"));
        return;
      }
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter character = attachment.getCurrentCharacter();
      if (character == null) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.no_character_selected"));
        return;
      }
      ItemStack weaponStack = character.getData().getWeapon();
      if (weaponStack.isEmpty() || !(weaponStack.getItem() instanceof WeaponItem weapon)) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.no_weapon_equipped"));
        return;
      }
      if (!weapon.canAscend(weaponStack)) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.weapon_not_meet_requirements"));
        return;
      }
      if (!weapon.ascend(weaponStack)) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.weapon_breakthrough_failed"));
        return;
      }
      character.getData().getArtifactInventory().markDirty(3);
      character.recalculateWeaponSlot();
      player.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, primogem - 1600);
      setPrimogemToPlayer(player, primogem - 1600);
      player.giveExperienceLevels(-10);
      attachment.syncToPlayer(player);
      player.sendSystemMessage(Component.translatable("message.minegenshin.weapon_breakthrough_success"));
    }
  }

  public static void sendAscendWeaponToServer() {
    RPCPacketDistributor.rpcToServer("ascendWeaponRPCPacket");
  }

  @RPCPacket("upgradeNormalAttackRPCPacket")
  public static void upgradeNormalAttackRPCPacket(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      int primogem = player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
      if (primogem < 320) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.normal_attack_primogem_low"));
        return;
      }
      if (player.experienceLevel < 5) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.normal_attack_exp_low"));
        return;
      }
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter character = attachment.getCurrentCharacter();
      if (character == null || character.getData() == null) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.no_character_selected"));
        return;
      }
      if (!character.getData().canUpgradeNormalAttack()) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.normal_attack_max_level"));
        return;
      }
      character.upgradeNormalAttack();
      player.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, primogem - 320);
      setPrimogemToPlayer(player, primogem - 320);
      player.giveExperienceLevels(-5);
      attachment.syncToPlayer(player);
      player.sendSystemMessage(Component.translatable("message.minegenshin.normal_attack_upgrade_success", character.getData().getNormalAttackLevel()));
    }
  }

  public static void sendUpgradeNormalAttackToServer() {
    RPCPacketDistributor.rpcToServer("upgradeNormalAttackRPCPacket");
  }

  @RPCPacket("upgradeElementalSkillRPCPacket")
  public static void upgradeElementalSkillRPCPacket(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      int primogem = player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
      if (primogem < 320) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.elemental_skill_primogem_low"));
        return;
      }
      if (player.experienceLevel < 5) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.elemental_skill_exp_low"));
        return;
      }
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter character = attachment.getCurrentCharacter();
      if (character == null || character.getData() == null) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.no_character_selected"));
        return;
      }
      if (!character.getData().canUpgradeElementalSkill()) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.elemental_skill_max_level"));
        return;
      }
      character.upgradeElementalSkill();
      player.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, primogem - 320);
      setPrimogemToPlayer(player, primogem - 320);
      player.giveExperienceLevels(-5);
      attachment.syncToPlayer(player);
      player.sendSystemMessage(Component.translatable("message.minegenshin.elemental_skill_upgrade_success", character.getData().getElementalSkillLevel()));
    }
  }

  public static void sendUpgradeElementalSkillToServer() {
    RPCPacketDistributor.rpcToServer("upgradeElementalSkillRPCPacket");
  }

  @RPCPacket("upgradeElementalBurstRPCPacket")
  public static void upgradeElementalBurstRPCPacket(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      int primogem = player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
      if (primogem < 320) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.elemental_burst_primogem_low"));
        return;
      }
      if (player.experienceLevel < 5) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.elemental_burst_exp_low"));
        return;
      }
      PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
      PGCharacter character = attachment.getCurrentCharacter();
      if (character == null || character.getData() == null) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.no_character_selected"));
        return;
      }
      if (!character.getData().canUpgradeElementalBurst()) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.elemental_burst_max_level"));
        return;
      }
      character.upgradeElementalBurst();
      player.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, primogem - 320);
      setPrimogemToPlayer(player, primogem - 320);
      player.giveExperienceLevels(-5);
      attachment.syncToPlayer(player);
      player.sendSystemMessage(Component.translatable("message.minegenshin.elemental_burst_upgrade_success", character.getData().getElementalBurstLevel()));
    }
  }

  public static void sendUpgradeElementalBurstToServer() {
    RPCPacketDistributor.rpcToServer("upgradeElementalBurstRPCPacket");
  }

  @RPCPacket("downgradeWorldLevelRPCPacket")
  public static void downgradeWorldLevelRPCPacket(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer player = Objects.requireNonNull(sender.asPlayer());
      AdventurerInfoAttachment advInfo = player.getData(AttachmentRegistration.ADVENTURER_INFO_ATTACHMENT);
      if (!advInfo.canDowngradeWorldLevel()) {
        player.sendSystemMessage(Component.translatable("message.minegenshin.world_level_downgrade_not_met"));
        return;
      }
      advInfo.downgradeWorldLevel();
      advInfo.syncToPlayer(player);
      player.sendSystemMessage(Component.translatable("message.minegenshin.world_level_downgrade_success", advInfo.getWorldLevel()));
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
        player.sendSystemMessage(Component.translatable("message.minegenshin.world_level_no_restore"));
        return;
      }
      advInfo.restoreWorldLevel();
      advInfo.syncToPlayer(player);
      player.sendSystemMessage(Component.translatable("message.minegenshin.world_level_restore_success", advInfo.getWorldLevel()));
    }
  }

  public static void sendRestoreWorldLevelToServer() {
    RPCPacketDistributor.rpcToServer("restoreWorldLevelRPCPacket");
  }

  @RPCPacket("invasionStatusRPCPacket")
  public static void invasionStatusRPCPacket(RPCSender sender, boolean invaded) {
    if (sender.isServer()) {
      ClientHandler.invasionStatusClientHandler(invaded);
    }
  }

  public static void setInvasionStatusToPlayer(ServerPlayer player, boolean invaded) {
    RPCPacketDistributor.rpcToPlayer(player, "invasionStatusRPCPacket", invaded);
  }

  @RPCPacket("minegenshin:entity_sync")
  public static void entitySyncRPCPacket(RPCSender sender, int entityId, CompoundTag payload) {
    if (sender.isServer()) {
      ClientHandler.entitySyncClientHandler(entityId, payload);
    }
  }

  public static void sendEntitySyncToPlayer(ServerPlayer player, int entityId, CompoundTag payload) {
    RPCPacketDistributor.rpcToPlayer(player, "minegenshin:entity_sync", entityId, payload);
  }

  @RPCPacket("minegenshin:character_sync")
  public static void characterSyncRPCPacket(RPCSender sender, int characterUUID, CompoundTag payload) {
    if (sender.isServer()) {
      ClientHandler.characterSyncClientHandler(characterUUID, payload);
    }
  }

  public static void sendCharacterSyncToPlayer(ServerPlayer player, int characterUUID, CompoundTag payload) {
    RPCPacketDistributor.rpcToPlayer(player, "minegenshin:character_sync", characterUUID, payload);
  }
}