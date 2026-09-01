package com.linweiyun.genshin.content.entities.attachments.attachment;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public class GenshinBackpack implements IItemHandler, IPersistedSerializable {
    @Persisted(key = "items")
    private ItemStack[] items;

    public GenshinBackpack() {
        this.items = new ItemStack[] { ItemStack.EMPTY };
    }
    @Override
    public int getSlots() {
        return items.length;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= items.length) return ItemStack.EMPTY;
        return items[slot] == null ? ItemStack.EMPTY : items[slot];
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (slot < 0 || slot >= items.length) return stack;

        ItemStack existing = items[slot];
        if (existing.isEmpty()) {
            if (!simulate) {
                items[slot] = stack.copy();
                compactSlots();
            }
            return ItemStack.EMPTY;
        }

        if (!existing.is(stack.getItem())) return stack;
        int limit = Math.min(getSlotLimit(slot), existing.getMaxStackSize());
        int addedSize = Math.min(stack.getCount(), limit - existing.getCount());
        if (addedSize <= 0) return stack;
        if (!simulate) {
            items[slot] = existing.copyWithCount(existing.getCount() + addedSize);
            compactSlots();
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
            compactSlots();
        }
        return existing.copyWithCount(extracted);
    }

    @Override
    public int getSlotLimit(int i) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }

    private void compactSlots() {
        int nonEmptyCount = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                nonEmptyCount++;
            }
        }
        ItemStack[] newItems = new ItemStack[nonEmptyCount + 1];
        int index = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                newItems[index++] = stack.copy();
            }
        }
        newItems[nonEmptyCount] = ItemStack.EMPTY;
        this.items = newItems;
    }
}
