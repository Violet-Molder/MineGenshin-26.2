package com.linweiyun.genshin.render.gui.menu;

import com.linweiyun.genshin.content.items.TeyvatItem;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.GenshinBackpack;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.system.registry.register.ModMenus;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scroller;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

import java.util.concurrent.atomic.AtomicReference;

public class GenshinBackpackMenu extends AbstractContainerMenu {

    private final GenshinBackpack backpack;
    private static final float SCROLL_COEFFICIENT = 20f;
    private static final GenshinBackpack.Category[] CATEGORIES = GenshinBackpack.Category.values();

    public GenshinBackpackMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.GENSHIN_BACKPACK_MENU.get(), containerId);
        Player player = playerInventory.player;
        this.backpack = player.getData(AttachmentRegistration.GENSHIN_BACKPACK_ATTACHMENT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, -9999, -9999));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, -9999, -9999));
        }

        var modularUI = createModularUI(player);
        if (this instanceof IModularUIHolderMenu holder) {
            holder.setModularUI(modularUI);
        }
    }

    private ModularUI createModularUI(Player player) {
        var stylesheet = StylesheetManager.INSTANCE.getStylesheetSafe(
                Identifier.parse("minegenshin:lss/genshin_backpack.lss"));

        var root = new UIElement().setId("root");
        root.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });

        var window = new UIElement().setId("window");

        AtomicReference<GenshinBackpack.Category> currentCategoryRef =
                new AtomicReference<>(GenshinBackpack.Category.WEAPONS);
        AtomicReference<Integer> selectedIndex = new AtomicReference<>(-1);
        AtomicReference<UIElement> selectedListElement = new AtomicReference<>();
        AtomicReference<Boolean> slotModeRef = new AtomicReference<>(false);

        var topToggleContainer = new UIElement().setId("top-toggle-container");

        var iconModeSection = new UIElement().setId("icon-mode-section");

        var listScrollerContainer = new UIElement().setId("list-scroller-container");
        var verticalScroller = new Scroller.Vertical();
        var itemListContainer = new UIElement().setId("item-list-container");

        verticalScroller.bindObserver(value -> {
            itemListContainer.layout(l -> l.bottom(value * SCROLL_COEFFICIENT));
            itemListContainer.markAsInternal();
        });

        listScrollerContainer.addChild(verticalScroller);
        listScrollerContainer.addChild(itemListContainer);

        var detailPanel = new UIElement().setId("detail-panel");

        iconModeSection.addChildren(listScrollerContainer, detailPanel);

        var slotModeSection = new UIElement().setId("slot-mode-section");
        slotModeSection.setDisplay(false);

        var slotModeLeft = new UIElement().setId("slot-mode-left");
        var slotModeRight = new UIElement().setId("slot-mode-right");
        slotModeSection.addChildren(slotModeLeft, slotModeRight);

        var modeSwitchBtn = new Button();
        modeSwitchBtn.setId("mode-switch-btn");
        modeSwitchBtn.setText("槽位模式");
        modeSwitchBtn.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            boolean newMode = !slotModeRef.get();
            slotModeRef.set(newMode);
            iconModeSection.setDisplay(!newMode);
            slotModeSection.setDisplay(newMode);
            modeSwitchBtn.setText(newMode ? "图标模式" : "槽位模式");
            if (newMode) {
                refreshSlotMode(slotModeLeft, slotModeRight, currentCategoryRef);
            } else {
                refreshItemList(itemListContainer, currentCategoryRef,
                        selectedIndex, selectedListElement, detailPanel);
                refreshDetailPanel(detailPanel, itemListContainer, currentCategoryRef, selectedIndex);
            }
            modeSwitchBtn.markAsInternal();
        });

        window.addChildren(topToggleContainer, iconModeSection, slotModeSection, modeSwitchBtn);
        root.addChild(window);

        refreshTopToggle(topToggleContainer, currentCategoryRef,
                selectedIndex, selectedListElement, itemListContainer, detailPanel,
                slotModeLeft, slotModeRight, slotModeRef);

        refreshItemList(itemListContainer, currentCategoryRef,
                selectedIndex, selectedListElement, detailPanel);

        refreshDetailPanel(detailPanel, itemListContainer, currentCategoryRef, selectedIndex);

        var ui = UI.of(root, stylesheet);
        return ModularUI.of(ui, player);
    }

    private void refreshTopToggle(
            UIElement topToggleContainer,
            AtomicReference<GenshinBackpack.Category> currentCategoryRef,
            AtomicReference<Integer> selectedIndex,
            AtomicReference<UIElement> selectedListElement,
            UIElement itemListContainer,
            UIElement detailPanel,
            UIElement slotModeLeft,
            UIElement slotModeRight,
            AtomicReference<Boolean> slotModeRef) {

        topToggleContainer.clearAllChildren();

        for (GenshinBackpack.Category cat : CATEGORIES) {
            var btn = new Button();
            btn.setText(cat.displayName);
            btn.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                currentCategoryRef.set(cat);
                selectedIndex.set(-1);
                selectedListElement.set(null);
                refreshTopToggle(topToggleContainer, currentCategoryRef,
                        selectedIndex, selectedListElement, itemListContainer, detailPanel,
                        slotModeLeft, slotModeRight, slotModeRef);
                refreshItemList(itemListContainer, currentCategoryRef,
                        selectedIndex, selectedListElement, detailPanel);
                refreshDetailPanel(detailPanel, itemListContainer, currentCategoryRef, selectedIndex);
                if (slotModeRef.get()) {
                    refreshSlotMode(slotModeLeft, slotModeRight, currentCategoryRef);
                }
                topToggleContainer.markAsInternal();
            });
            btn.addClass("category-toggle-btn");
            if (cat == currentCategoryRef.get()) {
                btn.addClass("category-toggle-active");
            }

            topToggleContainer.addChild(btn);
        }
        topToggleContainer.markAsInternal();
    }

    private void refreshItemList(
            UIElement itemListContainer,
            AtomicReference<GenshinBackpack.Category> currentCategoryRef,
            AtomicReference<Integer> selectedIndex,
            AtomicReference<UIElement> selectedListElement,
            UIElement detailPanel) {

        itemListContainer.clearAllChildren();

        GenshinBackpack.Category cat = currentCategoryRef.get();
        ItemStack[] items = backpack.getCategoryArray(cat);

        int firstNonEmpty = -1;
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = items[i];
            if (stack.isEmpty()) continue;
            if (firstNonEmpty < 0) firstNonEmpty = i;

            int idx = i;
            var itemElement = new UIElement().addClass("backpack-item");

            String texturePath = getItemTexturePath(stack);
            itemElement.style(style -> style.background(SpriteTexture.of(texturePath)));

            if (selectedIndex.get() == idx) {
                itemElement.style(style -> style.overlay(
                        SpriteTexture.of("minegenshin:textures/character_avatar/selected_border.png")));
                itemElement.transform(transform -> transform.scale(1.1f));
                selectedListElement.set(itemElement);
            }

            itemElement.addEventListener(UIEvents.MOUSE_ENTER, e -> {
                if (selectedListElement.get() != itemElement) {
                    itemElement.style(style -> style.overlay(
                            SpriteTexture.of("minegenshin:textures/character_avatar/selected_border.png")));
                    itemElement.transform(transform -> transform.scale(1.1f));
                }
            });
            itemElement.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                if (selectedListElement.get() != itemElement) {
                    itemElement.style(style -> style.overlay(null));
                    itemElement.transform(transform -> transform.scale(1f));
                }
            });
            itemElement.addEventListener(UIEvents.CLICK, e -> {
                if (selectedListElement.get() != null && selectedListElement.get() != itemElement) {
                    selectedListElement.get().style(style -> style.overlay(null));
                    selectedListElement.get().transform(transform -> transform.scale(1f));
                }
                selectedListElement.set(itemElement);
                itemElement.style(style -> style.overlay(
                        SpriteTexture.of("minegenshin:textures/character_avatar/selected_border.png")));
                itemElement.transform(transform -> transform.scale(1.1f));
                selectedIndex.set(idx);

                refreshDetailPanel(detailPanel, itemListContainer, currentCategoryRef, selectedIndex);
            });

            itemListContainer.addChild(itemElement);
        }

        if (selectedIndex.get() < 0 && firstNonEmpty >= 0) {
            selectedIndex.set(firstNonEmpty);
            refreshDetailPanel(detailPanel, itemListContainer, currentCategoryRef, selectedIndex);
        }

        itemListContainer.markAsInternal();
    }

    private void refreshDetailPanel(
            UIElement detailPanel,
            UIElement itemListContainer,
            AtomicReference<GenshinBackpack.Category> currentCategoryRef,
            AtomicReference<Integer> selectedIndex) {

        detailPanel.clearAllChildren();

        GenshinBackpack.Category cat = currentCategoryRef.get();
        int idx = selectedIndex.get();
        ItemStack stack = backpack.getItemInCategory(cat, idx);

        if (stack.isEmpty()) {
            var emptyLabel = new Label().setText("未选中物品").setId("detail-empty");
            detailPanel.addChild(emptyLabel);
            detailPanel.markAsInternal();
            return;
        }

        var infoContainer = new UIElement().setId("detail-info-container");

        var nameLabel = new Label()
                .setText(stack.getHoverName())
                .addClass("detail-name");
        infoContainer.addChild(nameLabel);

        if (stack.getItem() instanceof TeyvatItem teyvatItem) {
            int star = teyvatItem.getStar();
            if (star > 0) {
                var starLabel = new Label()
                        .setText(Component.literal("★".repeat(star)).withStyle(ChatFormatting.GOLD))
                        .addClass("detail-star");
                infoContainer.addChild(starLabel);
            }
        }

        var countLabel = new Label()
                .setText(Component.literal("数量: " + stack.getCount()).withStyle(ChatFormatting.GRAY))
                .addClass("detail-count");
        infoContainer.addChild(countLabel);

        var buttonContainer = new UIElement().setId("detail-button-container");

        var takeOutBtn = new Button();
        takeOutBtn.setText("取出");
        takeOutBtn.addClass("detail-takeout-btn");
        int catOrdinal = cat.ordinal();
        takeOutBtn.addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
            NetworkManager.sendBackpackTakeOutToServer(catOrdinal, idx);
            backpack.removeItemFromCategory(cat, idx);
            selectedIndex.set(-1);
            refreshItemList(itemListContainer, currentCategoryRef,
                    selectedIndex, new AtomicReference<>(), detailPanel);
            refreshDetailPanel(detailPanel, itemListContainer, currentCategoryRef, selectedIndex);
        });

        var detailBtn = new Button();
        detailBtn.setText("详情");
        detailBtn.addClass("detail-info-btn");

        buttonContainer.addChildren(takeOutBtn, detailBtn);
        infoContainer.addChild(buttonContainer);

        detailPanel.addChild(infoContainer);
        detailPanel.markAsInternal();
    }

    private void refreshSlotMode(
            UIElement slotModeLeft,
            UIElement slotModeRight,
            AtomicReference<GenshinBackpack.Category> currentCategoryRef) {

        slotModeLeft.clearAllChildren();
        slotModeRight.clearAllChildren();

        GenshinBackpack.Category cat = currentCategoryRef.get();

        var leftLabel = new Label().setText(cat.displayName).addClass("slot-mode-label");
        slotModeLeft.addChild(leftLabel);

        var leftScrollerContainer = new UIElement().setId("left-scroller-container");
        var leftScroller = new Scroller.Vertical();
        var slotGridLeft = new UIElement().setId("slot-grid-left");

        leftScroller.bindObserver(value -> {
            slotGridLeft.layout(l -> l.bottom(value * SCROLL_COEFFICIENT));
            slotGridLeft.markAsInternal();
        });

        leftScrollerContainer.addChild(leftScroller);
        leftScrollerContainer.addChild(slotGridLeft);

        int categoryOffset = getCategoryOffset(cat);
        int cols = 8;
        int totalSlots = cat.maxCapacity;
        int rows = (int) Math.ceil((double) totalSlots / cols);

        ResourceHandler<ItemResource> backpackHandler = backpack.asResourceHandler();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int localIdx = row * cols + col;
                if (localIdx >= totalSlots) break;
                int flatIndex = categoryOffset + localIdx;
                var slot = new ItemSlot();
                slot.bind(backpackHandler, flatIndex);
                slotGridLeft.addChild(slot);
            }
        }

        slotModeLeft.addChild(leftScrollerContainer);

        var rightLabel = new Label().setText("玩家背包").addClass("slot-mode-label");
        slotModeRight.addChild(rightLabel);

        var inventorySlots = new InventorySlots();
        slotModeRight.addChild(inventorySlots);

//        var rightScrollerContainer = new UIElement().setId("right-scroller-container");
//        var rightScroller = new Scroller.Vertical();
//        var slotGridRight = new UIElement().setId("slot-grid-right");
//
//        rightScroller.bindObserver(value -> {
//            slotGridRight.layout(l -> l.bottom(value * SCROLL_COEFFICIENT));
//            slotGridRight.markAsInternal();
//        });
//
//        rightScrollerContainer.addChild(rightScroller);
//        rightScrollerContainer.addChild(slotGridRight);
//
//        ResourceHandler<ItemResource> playerHandler =
//                VanillaContainerWrapper.of(player.getInventory());
//        int playerCols = 9;
//        int playerRows = 4;
//
//        for (int row = 0; row < playerRows; row++) {
//            for (int col = 0; col < playerCols; col++) {
//                int invIdx = row < 3 ? (row + 1) * 9 + col : col;
//                var slot = new ItemSlot();
//                slot.bind(playerHandler, invIdx);
//                slotGridRight.addChild(slot);
//            }
//        }
//
//        slotModeRight.addChild(rightScrollerContainer);

        slotModeLeft.markAsInternal();
        slotModeRight.markAsInternal();
    }

    private static int getCategoryOffset(GenshinBackpack.Category category) {
        int offset = 0;
        for (GenshinBackpack.Category cat : GenshinBackpack.Category.values()) {
            if (cat == category) return offset;
            offset += cat.maxCapacity;
        }
        return offset;
    }

    private static String getItemTexturePath(ItemStack stack) {
        if (stack.isEmpty()) return "minegenshin:textures/empty.png";
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace() + ":textures/item/" + id.getPath() + ".png";
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public GenshinBackpack getBackpack() {
        return backpack;
    }
}