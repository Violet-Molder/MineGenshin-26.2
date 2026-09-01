package com.linweiyun.genshin.content.entities.attachments.attachment;

import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public class CharacterParty implements IItemHandler, IPersistedSerializable {
  @Persisted(key = "items")
  private ItemStack[] items;

  @Persisted(key = "slots")
  private final int slots = 4;

  public CharacterParty() {
    this.items = new ItemStack[slots];
    for (int i = 0; i < slots; i++) {
      items[i] = ItemStack.EMPTY;
    }
  }

  @Override
  public int getSlots() {
    return slots;
  }

  public ItemStack[] getItems() {
    return items;
  }

  public void setItems(ItemStack[] items) {
    this.items = items;
  }

  public ItemStack getStackByCharacter(PlayerCharacter character) {
    for (ItemStack itemStack : items) {
      if (itemStack.getItem() == character) {
        return itemStack;
      }
    }
    return ItemStack.EMPTY;
  }

  public int getSlotByCharacter(ItemStack stack) {
    for (int i = 0; i < items.length; i++) {
      if (items[i] == stack) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public @NotNull ItemStack getStackInSlot(int slot) {
    if (slot < 0 || slot >= items.length) return ItemStack.EMPTY;
    return items[slot] == null ? ItemStack.EMPTY : items[slot];
  }

  public ItemStack changeAndSortItem(int slot, ItemStack stack, boolean simulate, Player player) {
    extractItem(slot, getStackInSlot(slot).getCount(), simulate);
    ItemStack result = insertItem(slot, stack, simulate);
    compactSlots();
    if (player != null) {
      if (player instanceof ServerPlayer serverPlayer) {
        NetworkManager.setCharacterPartyToPlayer(
            serverPlayer, serializeNBT(player.registryAccess()));
      } else {
        NetworkManager.setCharacterPartyToServer(serializeNBT(player.registryAccess()));
      }
    }
    return result;
  }

  public ItemStack insertAndSortItem(int slot, ItemStack stack, boolean simulate, Player player) {
    ItemStack result = insertItem(slot, stack, simulate);
    compactSlots();
    if (player != null) {
      if (player instanceof ServerPlayer serverPlayer) {
        NetworkManager.setCharacterPartyToPlayer(
            serverPlayer, serializeNBT(player.registryAccess()));
      } else {
        NetworkManager.setCharacterPartyToServer(serializeNBT(player.registryAccess()));
      }
    }
    return result;
  }

  public ItemStack extractAndSortItem(int slot, boolean simulate, Player player) {
    ItemStack result = extractItem(slot, getStackInSlot(slot).getCount(), simulate);
    compactSlots();
    if (player != null) {
      if (player instanceof ServerPlayer serverPlayer) {
        NetworkManager.setCharacterPartyToPlayer(
            serverPlayer, serializeNBT(player.registryAccess()));
      } else {
        NetworkManager.setCharacterPartyToServer(serializeNBT(player.registryAccess()));
      }
    }
    return result;
  }

  @Override
  public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
    if (stack.isEmpty()) return ItemStack.EMPTY;
    if (slot < 0 || slot >= items.length) return stack;
    ItemStack existing = items[slot];
    if (existing.isEmpty()) {
      if (!simulate) {
        items[slot] = stack.copy();
      }
      return ItemStack.EMPTY;
    }
    if (!existing.is(stack.getItem())) return stack;
    int limit = Math.min(getSlotLimit(slot), existing.getMaxStackSize());
    int currentSize = existing.getCount();
    int addedSize = Math.min(stack.getCount(), limit - currentSize);
    if (addedSize <= 0) return stack;
    if (!simulate) {
      items[slot] = existing.copyWithCount(currentSize + addedSize);
    }
    return stack.copyWithCount(stack.getCount() - addedSize);
  }

  @Override
  public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
    if (slot < 0 || slot >= items.length) return ItemStack.EMPTY;
    ItemStack existing = items[slot];
    if (existing.isEmpty()) return ItemStack.EMPTY;
    int extracted = Math.min(amount, existing.getCount());
    if (!simulate) {
      items[slot] = existing.copyWithCount(existing.getCount() - extracted);
    }
    return existing.copyWithCount(extracted);
  }

  private CharacterParty copy() {
    CharacterParty copy = new CharacterParty();
    for (int i = 0; i < slots; i++) {
      copy.items[i] = this.items[i].copy();
    }
    copy.currentCharacter = this.currentCharacter;
    return copy;
  }

  @Override
  public int getSlotLimit(int slot) {
    return 64;
  }

  @Override
  public boolean isItemValid(int slot, @NotNull ItemStack stack) {
    return true;
  }

  public void compactSlots() {
    ItemStack[] nonEmptyItems = new ItemStack[slots];
    int nonEmptyCount = 0;
    for (int i = 0; i < slots; i++) {
      ItemStack stack = getStackInSlot(i);
      if (!stack.isEmpty()) {
        nonEmptyItems[nonEmptyCount++] = stack;
      }
    }
    for (int i = 0; i < slots; i++) {
      items[i] = ItemStack.EMPTY;
    }
    for (int i = 0; i < nonEmptyCount; i++) {
      items[i] = nonEmptyItems[i].copy();
    }
  }

  @Override
  public void writeToBuff(ByteBuf buf) {
    PersistedParser.writeBuff(buf, this);
  }

  @Override
  public void readFromBuff(ByteBuf buf) {
    PersistedParser.readBuff(buf, this);
  }

  @Persisted(key = "current_character")
  private int currentCharacter = 1;

  public int getCurrentCharacterSlot() {
    return currentCharacter;
  }

  public ItemStack getCurrentCharacter() {
    return this.getStackInSlot(currentCharacter - 1);
  }

  public void setCurrentCharacter(int characterId, ServerPlayer player) {
    this.currentCharacter = Math.max(1, Math.min(4, characterId));
    NetworkManager.setCharacterSelectionToPlayer(player, characterId);
  }

  public void setClientCurrentCharacter(int characterId) {
    this.currentCharacter = Math.max(1, Math.min(4, characterId));
    NetworkManager.setCharacterSelectionToServer(characterId);
  }

  public void setPacketCurrentCharacter(int characterId) {
    this.currentCharacter = Math.max(1, Math.min(4, characterId));
  }
}
