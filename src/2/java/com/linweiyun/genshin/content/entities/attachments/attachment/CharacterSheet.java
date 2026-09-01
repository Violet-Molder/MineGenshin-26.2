package com.linweiyun.genshin.content.entities.attachments.attachment;

import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public class CharacterSheet implements IItemHandler, IPersistedSerializable {
  // 存储物品的数组，大小为16
  @Persisted(key = "items")
  private ItemStack[] items;

  // 槽位总数
  @Persisted(key = "slots")
  private final int slots = 16;

  @Persisted(key = "sortMethod")
  private SortMethod sortMethod = SortMethod.STAR;

  public CharacterSheet() {
    this.items = new ItemStack[slots];
    for (int i = 0; i < slots; i++) {
      items[i] = ItemStack.EMPTY;
    }
  }

  public ItemStack[] getItems() {
    return items;
  }

  public void setItems(ItemStack[] items) {
    this.items = items;
  }

  public void setSortMethodToPlayer(ServerPlayer player, SortMethod method) {
    this.sortMethod = method;
    NetworkManager.setCharacterSheetSortMethodToPlayer(player, method);
  }

  public void setSortMethodToServer(SortMethod method, Player player) {
    this.sortMethod = method;
    NetworkManager.setCharacterSheetSortMethodToServer(method);
    sortInventory(player);
  }

  public void setPacketSortMethod(SortMethod method) {
    this.sortMethod = method;
  }

  public SortMethod getSortMethod() {
    return sortMethod;
  }

  public void sortInventory(Player player) {
    List<ItemStack> itemList = new ArrayList<>();
    for (ItemStack stack : items) {
      itemList.add(stack == null ? ItemStack.EMPTY : stack);
    }
    itemList.sort(
        (stack1, stack2) -> {
          if (stack1.isEmpty() && stack2.isEmpty()) return 0;
          if (stack1.isEmpty()) return 1;
          if (stack2.isEmpty()) return -1;
          int priority1 = getSortPriority(stack1);
          int priority2 = getSortPriority(stack2);
          if (priority1 != priority2) {
            return Integer.compare(priority2, priority1);
          }
          PlayerCharacter character1 = (PlayerCharacter) stack1.getItem();
          PlayerCharacter character2 = (PlayerCharacter) stack2.getItem();
          int uuid1 = character1.getCharacterUUID();
          int uuid2 = character2.getCharacterUUID();
          return Integer.compare(uuid1, uuid2);
        });
    for (int i = 0; i < slots; i++) {
      if (i < itemList.size()) {
        items[i] = itemList.get(i);
      } else {
        items[i] = ItemStack.EMPTY;
      }
    }
    if (player != null) {
      if (player instanceof ServerPlayer serverPlayer) {
        NetworkManager.setCharacterSheetToPlayer(
            serverPlayer, serializeNBT(player.registryAccess()));
      } else {

        NetworkManager.setCharacterSheetToServer(serializeNBT(player.registryAccess()));
      }
    }
  }

  private int getSortPriority(ItemStack stack) {
    if (stack.isEmpty()) return 0;
    PlayerCharacter item = (PlayerCharacter) stack.getItem();
    return switch (sortMethod) {
      case STAR -> item.getStarRating();
      case LEVEL -> 12;
    };
  }

  public int getSlots() {
    return slots;
  }

  @Override
  public @NotNull ItemStack getStackInSlot(int slot) {
    if (slot < 0 || slot >= items.length) return ItemStack.EMPTY;
    return items[slot] == null ? ItemStack.EMPTY : items[slot];
  }

  public ItemStack insertAndSortItem(int slot, ItemStack stack, boolean simulate, Player player) {
    ItemStack result = insertItem(slot, stack, simulate);
    if (!simulate) {
      sortInventory(player);
    }
    return result;
  }

  @Override
  public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
    if (stack.isEmpty()) return ItemStack.EMPTY;
    if (slot < 0 || slot >= items.length) return stack;
    ItemStack existing = items[slot];
    ItemStack result = ItemStack.EMPTY;
    if (existing.isEmpty()) {
      if (!simulate) {
        items[slot] = stack.copy();
      }
    } else if (!existing.is(stack.getItem())) {
      result = stack;
    } else {
      int limit = Math.min(getSlotLimit(slot), existing.getMaxStackSize());
      int currentSize = existing.getCount();
      int addedSize = Math.min(stack.getCount(), limit - currentSize);
      if (addedSize <= 0) {
        result = stack;
      } else {
        if (!simulate) {
          items[slot] = existing.copyWithCount(currentSize + addedSize);
        }
        result = stack.copyWithCount(stack.getCount() - addedSize);
      }
    }

    return result;
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

  private CharacterSheet copy() {
    CharacterSheet copy = new CharacterSheet();
    for (int i = 0; i < slots; i++) {
      copy.items[i] = this.items[i].copy();
    }
    copy.sortMethod = this.sortMethod;
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

  public boolean hasCharacter(ItemStack characterStack) {
    if (characterStack.isEmpty() || !(characterStack.getItem() instanceof PlayerCharacter)) {
      return false;
    }

    for (ItemStack stack : items) {
      if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, characterStack)) {
        return true;
      }
    }
    return false;
  }

  public void addCharacter(ItemStack characterStack, Player player) {
    if (characterStack.isEmpty() || !(characterStack.getItem() instanceof PlayerCharacter)) {
      return;
    }
    for (int i = 0; i < slots; i++) {
      if (items[i].isEmpty()) {
        items[i] = characterStack.copy();
        break;
      }
    }

    sortInventory(player);
  }

  public enum SortMethod {
    STAR("star", "按星级排序"),
    LEVEL("level", "按等级排序");

    private final String value;
    private final String description;

    SortMethod(String value, String description) {
      this.value = value;
      this.description = description;
    }

    @Override
    public String toString() {
      return this.description;
    }

    public String getValue() {
      return value;
    }

    public String getDescription() {
      return description;
    }
  }
}
