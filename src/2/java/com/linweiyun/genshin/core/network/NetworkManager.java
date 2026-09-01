package com.linweiyun.genshin.core.network;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.effects.itemstacks.ItemStackEffectList;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.*;
import com.linweiyun.genshin.content.items.character.data.PlayerCharacterData;
import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.linweiyun.genshin.content.items.components.DataComponentRegistryCharacter;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class NetworkManager {

  // 原石同步
  @RPCPacket("primogemRPCPacket")
  public static void primogemRPCPacket(RPCSender sender, long amount) {
    if (sender.isServer()) {
      ClientHandler.primogemClientHandler(amount);

    } else {
      PlayerPrimogemAttachment primogem =
          Objects.requireNonNull(sender.asPlayer())
              .getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
      primogem.changePacketPrimogem(amount);
    }
  }

  public static void setPrimogemToServer(long amount) {
    RPCPacketDistributor.rpcToServer("primogemRPCPacket", amount);
  }

  public static void setPrimogemToPlayer(ServerPlayer player, long amount) {
    RPCPacketDistributor.rpcToPlayer(player, "primogemRPCPacket", amount);
  }

  // 角色队伍同步
  @RPCPacket("characterPartyRPCPacket")
  public static void characterPartyRPCPacket(RPCSender sender, CompoundTag inventoryData) {
    if (sender.isServer()) {
      ClientHandler.characterPartyClientHandler(inventoryData);
    } else {
      ServerPlayer serverPlayer = sender.asPlayer();
      CharacterParty characterParty =
          Objects.requireNonNull(serverPlayer)
              .getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
      characterParty.deserializeNBT(serverPlayer.registryAccess(), inventoryData);
    }
  }

  public static void setCharacterPartyToServer(CompoundTag partyData) {
    RPCPacketDistributor.rpcToServer("characterPartyRPCPacket", partyData);
  }

  public static void setCharacterPartyToPlayer(ServerPlayer player, CompoundTag partyData) {
    RPCPacketDistributor.rpcToPlayer(player, "characterPartyRPCPacket", partyData);
  }

  // 角色图鉴同步
  @RPCPacket("characterSheetRPCPacket")
  public static void characterSheetRPCPacket(RPCSender sender, CompoundTag inventoryData) {
    if (sender.isServer()) {
      ClientHandler.characterSheetClientHandler(inventoryData);
    } else {
      ServerPlayer serverPlayer = sender.asPlayer();
      CharacterSheet characterSheet =
          Objects.requireNonNull(serverPlayer)
              .getData(AttachmentRegistration.CHARACTER_SHEET_ATTACHMENT);
      characterSheet.deserializeNBT(serverPlayer.registryAccess(), inventoryData);
    }
  }

  public static void setCharacterSheetToServer(CompoundTag sheetData) {
    RPCPacketDistributor.rpcToServer("characterSheetRPCPacket", sheetData);
  }

  public static void setCharacterSheetToPlayer(ServerPlayer player, CompoundTag sheetData) {
    RPCPacketDistributor.rpcToPlayer(player, "characterSheetRPCPacket", sheetData);
  }

  // 角色图鉴排序方式同步
  @RPCPacket("characterSheetSortMethodRPCPacket")
  public static void characterSheetSortMethodRPCPacket(
      RPCSender sender, CharacterSheet.SortMethod sortMethod) {
    if (sender.isServer()) {
      // 客务端接收：服务端发送的数据到达客户端
      ClientHandler.characterSheetSortMethodClientHandler(sortMethod);
    } else {
      // 服务端接收：客户端发送数据到达服务端
      ServerPlayer serverPlayer = sender.asPlayer();
      CharacterSheet characterSheet =
          Objects.requireNonNull(serverPlayer)
              .getData(AttachmentRegistration.CHARACTER_SHEET_ATTACHMENT);
      characterSheet.setPacketSortMethod(sortMethod);
    }
  }

  public static void setCharacterSheetSortMethodToServer(CharacterSheet.SortMethod sortMethod) {
    RPCPacketDistributor.rpcToServer("characterSheetSortMethodRPCPacket", sortMethod);
  }

  public static void setCharacterSheetSortMethodToPlayer(
      ServerPlayer player, CharacterSheet.SortMethod sortMethod) {
    RPCPacketDistributor.rpcToPlayer(player, "characterSheetSortMethodRPCPacket", sortMethod);
  }

  // 游戏模式同步
  @RPCPacket("genshinModeRPCPacket")
  public static void genshinModeRPCPacket(RPCSender sender, boolean isGenshinMode) {
    if (sender.isServer()) {
      ClientHandler.genshinModeClientHandler(isGenshinMode);
    } else {
      ServerPlayer serverPlayer = sender.asPlayer();
      PlayerGenshinModeAttachment modeAttachment =
          Objects.requireNonNull(serverPlayer)
              .getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT.get());
      modeAttachment.setPacketGenshinMode(isGenshinMode);
    }
  }

  public static void setGenshinModeToPlayer(ServerPlayer player, boolean isGenshinMode) {
    RPCPacketDistributor.rpcToPlayer(player, "genshinModeRPCPacket", isGenshinMode);
  }

  public static void setGenshinModeToServer(boolean isGenshinMode) {
    RPCPacketDistributor.rpcToServer("genshinModeRPCPacket", isGenshinMode);
  }

  // 角色选择同步
  @RPCPacket("characterSelectionRPCPacket")
  public static void characterSelectionRPCPacket(RPCSender sender, int characterId) {
    if (sender.isServer()) {
      ClientHandler.characterSelectionClientHandler(characterId);
    } else {
      ServerPlayer serverPlayer = sender.asPlayer();
      CharacterParty selectionAttachment =
          Objects.requireNonNull(serverPlayer)
              .getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT.get());
      selectionAttachment.setPacketCurrentCharacter(characterId);
    }
  }

  public static void setCharacterSelectionToPlayer(ServerPlayer player, int characterId) {
    RPCPacketDistributor.rpcToPlayer(player, "characterSelectionRPCPacket", characterId);
  }

  public static void setCharacterSelectionToServer(int characterId) {
    RPCPacketDistributor.rpcToServer("characterSelectionRPCPacket", characterId);
  }

  @RPCPacket("characterActiveSkillRPCPacket")
  public static void characterActiveSkillRPCPacket(RPCSender sender, int skill) {
    if (!sender.isServer()) {
      ServerPlayer serverPlayer = sender.asPlayer();
      CharacterParty characterParty =
          Objects.requireNonNull(serverPlayer)
              .getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT.get());
      int characterId = characterParty.getCurrentCharacterSlot();
      ItemStack characterItem = characterParty.getStackInSlot(characterId - 1);
      if (!characterItem.isEmpty()
          && characterItem.getItem() instanceof PlayerCharacter playerCharacter) {
        if (skill == 1) {
          playerCharacter.performElementalSkill(serverPlayer);
        } else if (skill == 2) {
          playerCharacter.performElementalBurst(serverPlayer);
        }
      }
    }
  }

  public static void triggerCharacterSkill(int skill) {
    RPCPacketDistributor.rpcToServer("characterActiveSkillRPCPacket", skill);
  }

  // 角色数据同步
  @RPCPacket("characterDataRPCPacket")
  public static void characterDataRPCPacket(RPCSender sender, CompoundTag inventoryData, int slot) {
    if (sender.isServer()) {
      ClientHandler.characterDataClientHandler(inventoryData, slot);
    } else {

      ServerPlayer serverPlayer = sender.asPlayer();
      CharacterParty characterParty =
          Objects.requireNonNull(serverPlayer)
              .getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
      ItemStack stack = characterParty.getStackInSlot(slot);
      PlayerCharacterData characterData = CombatHelper.getCharacterData(stack);
      Objects.requireNonNull(characterData)
          .deserializeNBT(serverPlayer.registryAccess(), inventoryData);
    }
  }

  public static void setCharacterDataToServer(CompoundTag inventoryData, int slot) {
    RPCPacketDistributor.rpcToServer("characterDataRPCPacket", inventoryData, slot);
  }

  public static void setCharacterDataToPlayer(
      ServerPlayer player, CompoundTag inventoryData, int slot) {
    RPCPacketDistributor.rpcToPlayer(player, "characterDataRPCPacket", inventoryData, slot);
  }

  // 物品效果同步
  @RPCPacket("itemStackEffectsRPCPacket")
  public static void itemStackEffectsRPCPacket(RPCSender sender, ListTag effectsData, int slot) {
    if (sender.isServer()) {
      ClientHandler.itemStackEffectsClientHandler(effectsData, slot);
    } else {
      ServerPlayer serverPlayer = sender.asPlayer();
      CharacterParty characterParty =
          Objects.requireNonNull(serverPlayer)
              .getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
      ItemStack stack = characterParty.getStackInSlot(slot);
      ItemStackEffectList effectList =
          ItemStackEffectList.CODEC
              .parse(NbtOps.INSTANCE, effectsData)
              .result()
              .orElse(ItemStackEffectList.EMPTY);
      stack.set(DataComponentRegistryCharacter.ITEM_STACK_EFFECTS.get(), effectList);
    }
  }

  public static void setItemStackEffectsToServer(ListTag effectsData, int slot) {
    RPCPacketDistributor.rpcToServer("itemStackEffectsRPCPacket", effectsData, slot);
  }

  public static void setItemStackEffectsToPlayer(
      ServerPlayer player, ListTag effectsData, int slot) {
    RPCPacketDistributor.rpcToPlayer(player, "itemStackEffectsRPCPacket", effectsData, slot);
  }

  // 消耗原石抽奖
  @RPCPacket("primogemWish")
  public static void primogemWish(RPCSender sender) {
    if (!sender.isServer()) {
      ServerPlayer serverPlayer = sender.asPlayer();
      if (serverPlayer != null) {
        ServerLevel serverLevel = (ServerLevel) serverPlayer.level();
        PlayerPrimogemAttachment primogemAttachment =
            serverPlayer.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT.get());
        if (primogemAttachment.getPrimogem() >= 160) {
          primogemAttachment.removePrimogem(160, serverPlayer);
          ResourceKey<LootTable> lootTableKey =
              ResourceKey.create(Registries.LOOT_TABLE, Minegenshin.id("wish"));
          LootTable lootTable =
              Objects.requireNonNull(serverLevel.getServer())
                  .reloadableRegistries()
                  .getLootTable(lootTableKey);
          LootParams.Builder params =
              new LootParams.Builder(serverLevel)
                  .withParameter(LootContextParams.ORIGIN, serverPlayer.position())
                  .withParameter(LootContextParams.THIS_ENTITY, serverPlayer);
          List<ItemStack> drops =
              lootTable.getRandomItems(params.create(LootContextParamSets.CHEST));
          if (!drops.isEmpty()) {
            ItemStack result = drops.getFirst();
            if (result.getItem() instanceof PlayerCharacter) {
              CharacterSheet characterSheet =
                  serverPlayer.getData(AttachmentRegistration.CHARACTER_SHEET_ATTACHMENT.get());
              Component characterName = result.getHoverName().copy().withStyle(ChatFormatting.GOLD);

              if (characterSheet.hasCharacter(result)) {
                ItemStack diamondStack = new ItemStack(Items.DIAMOND, 3);
                Component itemCount =
                    Component.literal(String.valueOf(diamondStack.getCount()))
                        .withStyle(ChatFormatting.AQUA);
                Component itemDisplayName =
                    diamondStack.getHoverName().copy().withStyle(ChatFormatting.AQUA);
                serverPlayer.sendSystemMessage(
                    Component.translatable(
                        "message.minegenshin.wish.owned_character",
                        characterName,
                        itemDisplayName,
                        itemCount));

                giveItemToPlayer(serverPlayer, diamondStack);
              } else {

                serverPlayer.sendSystemMessage(
                    Component.translatable(
                        "message.minegenshin.wish.draw_character", characterName));
                characterSheet.addCharacter(result, serverPlayer);
                CompoundTag tag = characterSheet.serializeNBT(serverPlayer.registryAccess());
                NetworkManager.setCharacterSheetToPlayer(serverPlayer, tag);
              }

            } else {
              Component itemName = result.getHoverName().copy().withStyle(ChatFormatting.GRAY);
              Component itemCount =
                  Component.literal(String.valueOf(result.getCount()))
                      .withStyle(ChatFormatting.GRAY);
              serverPlayer.sendSystemMessage(
                  Component.translatable(
                      "message.minegenshin.wish.draw_item", itemName, result.getCount()));
              giveItemToPlayer(serverPlayer, result);
            }
          } else {
            serverPlayer.sendSystemMessage(
                Component.translatable("message.minegenshin.wish.no_reward"));
          }
        } else {
          serverPlayer.sendSystemMessage(
              Component.translatable("message.minegenshin.wish.not_enough_primogem"));
        }
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
