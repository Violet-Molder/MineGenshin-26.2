package com.linweiyun.genshin.render.gui.menu;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.GenshinBackpack;
import com.linweiyun.genshin.core.system.registry.register.ModMenus;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GenshinBackpackMenu extends AbstractContainerMenu {

    private final GenshinBackpack backpack;
    private final int backpackSlotCount;
    public GenshinBackpackMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.GENSHIN_BACKPACK_MENU.get(), containerId);
        Player player = playerInventory.player;
        this.backpack = player.getData(AttachmentRegistration.GENSHIN_BACKPACK_ATTACHMENT);

        for (GenshinBackpack.Category cat : GenshinBackpack.Category.values()) {
            int offset = getCategoryOffset(cat);
            for (int i = 0; i < cat.maxCapacity; i++) {
                this.addSlot(new GenshinBackpackSlot(backpack, offset + i, -9999, -9999));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, -9999, -9999));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, -9999, -9999));
        }
        this.backpackSlotCount = GenshinBackpack.Category.values().length * 100;

        var modularUI = createModularUI(player);
        if (this instanceof IModularUIHolderMenu holder) {
            holder.setModularUI(modularUI);
        }
    }

    private ModularUI createModularUI(Player player) {
        var root = new UIElement().setId("root");
        var stylesheet = StylesheetManager.INSTANCE.getStylesheetSafe(
                Identifier.parse("minegenshin:lss/genshin_backpack.lss"));
        var ui = UI.of(root, stylesheet);
        return ModularUI.of(ui, player);
    }

    public GenshinBackpack getBackpack() {
        return backpack;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack slotStack = slot.getItem();
        ItemStack result = slotStack.copy();

        int backpackSlots = backpackSlotCount;
        int totalSlots = this.slots.size();

        if (index < backpackSlots) {
            // 从原神背包 → 玩家背包
            if (!this.moveItemStackTo(slotStack, backpackSlots, totalSlots, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 从玩家背包 → 原神背包（需要找到匹配分类的空槽位）
            if (!this.moveItemStackTo(slotStack, 0, backpackSlots, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static int getCategoryOffset(GenshinBackpack.Category category) {
        int offset = 0;
        for (GenshinBackpack.Category cat : GenshinBackpack.Category.values()) {
            if (cat == category) return offset;
            offset += cat.maxCapacity;
        }
        return offset;
    }

    private static class GenshinBackpackSlot extends Slot {
        private final GenshinBackpack backpack;

        GenshinBackpackSlot(GenshinBackpack backpack, int flatIndex, int x, int y) {
            super(backpack, flatIndex, x, y);
            this.backpack = backpack;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            int flatIndex = getContainerSlot();
            GenshinBackpack.Category cat = GenshinBackpack.getCategoryFromFlatIndex(flatIndex);
            return GenshinBackpack.isValidForCategory(cat, stack);
        }
        @Override
        public void set(ItemStack stack) {
            backpack.setSuppressDirty(true);
            super.set(stack);
            backpack.setSuppressDirty(false);
        }
        @Override
        public void setByPlayer(ItemStack stack) {
            backpack.setSuppressDirty(true);
            super.setByPlayer(stack);
            backpack.setSuppressDirty(false);
        }
    }
}