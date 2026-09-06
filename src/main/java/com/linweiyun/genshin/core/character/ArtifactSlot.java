package com.linweiyun.genshin.core.character;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactSlot extends Slot {

    private static final Logger LOGGER = LoggerFactory.getLogger("MineGenshin/ArtifactSlot");

    private final int artifactSlotIndex;

    public ArtifactSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.artifactSlotIndex = slot;
        LOGGER.info("ArtifactSlot created: slotIndex={}, x={}, y={}, container={}", slot, x, y, container.getClass().getSimpleName());
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        boolean result = ArtifactInventory.isValidForSlot(artifactSlotIndex, stack);
        LOGGER.info("ArtifactSlot.mayPlace: slotIndex={}, item={}, result={}", artifactSlotIndex, stack.getItem(), result);
        return result;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void set(ItemStack stack) {
        LOGGER.info("ArtifactSlot.set: slotIndex={}, item={}, count={}", artifactSlotIndex, stack.getItem(), stack.getCount());
        super.set(stack);
    }

    @Override
    public ItemStack remove(int amount) {
        LOGGER.info("ArtifactSlot.remove: slotIndex={}, amount={}", artifactSlotIndex, amount);
        return super.remove(amount);
    }
}