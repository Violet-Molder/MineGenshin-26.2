package com.linweiyun.genshin.core.character;

import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.content.items.artifact.ArtifactType;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ArtifactInventory implements Container, IPersistedSerializable {

    private static final Logger LOGGER = LoggerFactory.getLogger("MineGenshin/ArtifactInventory");

    public static final int SLOT_COUNT = 5;
    public static final int SLOT_FLOWER = 0;
    public static final int SLOT_PLUME = 1;
    public static final int SLOT_SANDS = 2;
    public static final int SLOT_GOBLET = 3;
    public static final int SLOT_CIRCLET = 4;

    @Persisted(key = "flower")
    private ItemStack flower = ItemStack.EMPTY;
    @Persisted(key = "plume")
    private ItemStack plume = ItemStack.EMPTY;
    @Persisted(key = "sands")
    private ItemStack sands = ItemStack.EMPTY;
    @Persisted(key = "goblet")
    private ItemStack goblet = ItemStack.EMPTY;
    @Persisted(key = "circlet")
    private ItemStack circlet = ItemStack.EMPTY;

    private final boolean[] dirtyFlags = new boolean[SLOT_COUNT];
    private Runnable onChange = () -> {};

    public ArtifactInventory() {}

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public ResourceHandler<ItemResource> asResourceHandler() {
        return VanillaContainerWrapper.of(this);
    }

    public static int typeToSlot(ArtifactType type) {
        return switch (type) {
            case FLOWER -> SLOT_FLOWER;
            case PLUME -> SLOT_PLUME;
            case SANDS -> SLOT_SANDS;
            case GOBLET -> SLOT_GOBLET;
            case CIRCLET -> SLOT_CIRCLET;
        };
    }

    public static ArtifactType slotToType(int slot) {
        return switch (slot) {
            case SLOT_FLOWER -> ArtifactType.FLOWER;
            case SLOT_PLUME -> ArtifactType.PLUME;
            case SLOT_SANDS -> ArtifactType.SANDS;
            case SLOT_GOBLET -> ArtifactType.GOBLET;
            case SLOT_CIRCLET -> ArtifactType.CIRCLET;
            default -> throw new IndexOutOfBoundsException("Invalid artifact slot: " + slot);
        };
    }

    public static boolean isValidForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (!(stack.getItem() instanceof ArtifactItem artifact)) return false;
        ArtifactType expected = slotToType(slot);
        return artifact.getType() == expected;
    }

    public void markDirty(int slot) {
        if (slot >= 0 && slot < SLOT_COUNT) {
            dirtyFlags[slot] = true;
            LOGGER.info("ArtifactInventory.markDirty: slot={}", slot);
        }
    }

    public boolean isDirty(int slot) {
        return slot >= 0 && slot < SLOT_COUNT && dirtyFlags[slot];
    }

    public void clearDirty(int slot) {
        if (slot >= 0 && slot < SLOT_COUNT) {
            dirtyFlags[slot] = false;
        }
    }

    public boolean hasDirtySlots() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (dirtyFlags[i]) return true;
        }
        return false;
    }

    private ItemStack getStackBySlot(int slot) {
        return switch (slot) {
            case SLOT_FLOWER -> flower;
            case SLOT_PLUME -> plume;
            case SLOT_SANDS -> sands;
            case SLOT_GOBLET -> goblet;
            case SLOT_CIRCLET -> circlet;
            default -> throw new IndexOutOfBoundsException("Invalid artifact slot: " + slot);
        };
    }

    private void setStackBySlot(int slot, ItemStack stack) {
        switch (slot) {
            case SLOT_FLOWER -> flower = stack;
            case SLOT_PLUME -> plume = stack;
            case SLOT_SANDS -> sands = stack;
            case SLOT_GOBLET -> goblet = stack;
            case SLOT_CIRCLET -> circlet = stack;
            default -> throw new IndexOutOfBoundsException("Invalid artifact slot: " + slot);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return flower.isEmpty() && plume.isEmpty() && sands.isEmpty() && goblet.isEmpty() && circlet.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return getStackBySlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack existing = getStackBySlot(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int toRemove = Math.min(amount, existing.getCount());
        ItemStack result = existing.copyWithCount(toRemove);
        if (toRemove >= existing.getCount()) {
            setStackBySlot(slot, ItemStack.EMPTY);
            markDirty(slot);
        } else {
            existing.shrink(toRemove);
        }
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack existing = getStackBySlot(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        setStackBySlot(slot, ItemStack.EMPTY);
        markDirty(slot);
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && !isValidForSlot(slot, stack)) return;
        setStackBySlot(slot, stack);
        markDirty(slot);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isValidForSlot(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        onChange.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        flower = ItemStack.EMPTY;
        plume = ItemStack.EMPTY;
        sands = ItemStack.EMPTY;
        goblet = ItemStack.EMPTY;
        circlet = ItemStack.EMPTY;
        for (int i = 0; i < SLOT_COUNT; i++) {
            markDirty(i);
        }
        setChanged();
    }

    public List<ItemStack> getAllArtifactsAsList() {
        return List.of(flower, plume, sands, goblet, circlet);
    }

    public ArtifactInventory copy() {
        ArtifactInventory inv = new ArtifactInventory();
        inv.flower = this.flower.copy();
        inv.plume = this.plume.copy();
        inv.sands = this.sands.copy();
        inv.goblet = this.goblet.copy();
        inv.circlet = this.circlet.copy();
        return inv;
    }
}