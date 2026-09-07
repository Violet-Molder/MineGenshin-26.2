package com.linweiyun.genshin.render.gui.screens;

import com.linweiyun.genshin.content.items.TeyvatItem;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.GenshinBackpack;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.render.gui.menu.GenshinBackpackMenu;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.concurrent.atomic.AtomicReference;

public class ScreenGenshinBackpack extends AbstractContainerScreen<GenshinBackpackMenu> {

    public ScreenGenshinBackpack(GenshinBackpackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.empty());
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = -9999;
        this.titleLabelY = -9999;
        this.inventoryLabelX = -9999;
        this.inventoryLabelY = -9999;
        if (this.menu instanceof com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu holder) {
            var modularUI = createModularUI(minecraft.player);
            holder.setModularUI(modularUI);
            ModularUIClientAccess.setScreenAndInit(modularUI, this);
            this.clearWidgets();
            this.addRenderableWidget(ModularUIClientAccess.getWidget(modularUI));
        }
    }

    private static final float SCROLL_COEFFICIENT = 20f;
    private static final int SLOT_MODE_VISIBLE_SLOTS = 100;
    private static final GenshinBackpack.Category[] CATEGORIES = GenshinBackpack.Category.values();

    private ModularUI createModularUI(Player player) {
        var stylesheet = StylesheetManager.INSTANCE.getStylesheetSafe(
                Identifier.parse("minegenshin:lss/genshin_backpack.lss"));

        GenshinBackpack backpack = player.getData(AttachmentRegistration.GENSHIN_BACKPACK_ATTACHMENT);

        var root = new UIElement().setId("root");
        root.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });

        var window = new UIElement().setId("window");

        AtomicReference<GenshinBackpack.Category> currentCategory =
                new AtomicReference<>(GenshinBackpack.Category.WEAPONS);
        AtomicReference<Integer> selectedIndex = new AtomicReference<>(-1);
        AtomicReference<UIElement> selectedListElement = new AtomicReference<>();
        AtomicReference<Float> scrollValue = new AtomicReference<>(0.0f);
        AtomicReference<Boolean> slotMode = new AtomicReference<>(false);

        var topToggleContainer = new UIElement().setId("top-toggle-container");

        var iconModeSection = new UIElement().setId("icon-mode-section");

        var listScrollerContainer = new UIElement().setId("list-scroller-container");
        var verticalScroller = new Scroller.Vertical();
        var itemListContainer = new UIElement().setId("item-list-container");

        verticalScroller.bindObserver(value -> {
            scrollValue.set(value);
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
        modeSwitchBtn.setOnClick(e -> {
            boolean newMode = !slotMode.get();
            slotMode.set(newMode);
            iconModeSection.setDisplay(!newMode);
            slotModeSection.setDisplay(newMode);
            modeSwitchBtn.setText(newMode ? "图标模式" : "槽位模式");
            if (newMode) {
                refreshSlotMode(slotModeLeft, slotModeRight, backpack, currentCategory, this.menu);
            }
            modeSwitchBtn.markAsInternal();
        });

        window.addChildren(topToggleContainer, iconModeSection, slotModeSection, modeSwitchBtn);
        root.addChild(window);

        refreshTopToggle(topToggleContainer, currentCategory, backpack, player,
                selectedIndex, selectedListElement, itemListContainer, detailPanel,
                slotModeLeft, slotModeRight, slotMode);

        refreshItemList(itemListContainer, backpack, player, currentCategory,
                selectedIndex, selectedListElement, detailPanel);

        refreshDetailPanel(detailPanel, backpack, player, currentCategory, selectedIndex);

        var ui = UI.of(root, stylesheet);
        return ModularUI.of(ui, player);
    }

    private static void refreshTopToggle(
            UIElement topToggleContainer,
            AtomicReference<GenshinBackpack.Category> currentCategory,
            GenshinBackpack backpack,
            Player player,
            AtomicReference<Integer> selectedIndex,
            AtomicReference<UIElement> selectedListElement,
            UIElement itemListContainer,
            UIElement detailPanel,
            UIElement slotModeLeft,
            UIElement slotModeRight,
            AtomicReference<Boolean> slotMode) {

        topToggleContainer.clearAllChildren();

        for (GenshinBackpack.Category cat : CATEGORIES) {
            var btn = new Button();
            btn.setText(cat.displayName);
            btn.setOnClick(e -> {
                currentCategory.set(cat);
                selectedIndex.set(-1);
                selectedListElement.set(null);
                refreshTopToggle(topToggleContainer, currentCategory, backpack, player,
                        selectedIndex, selectedListElement, itemListContainer, detailPanel,
                        slotModeLeft, slotModeRight, slotMode);
                refreshItemList(itemListContainer, backpack, player, currentCategory,
                        selectedIndex, selectedListElement, detailPanel);
                refreshDetailPanel(detailPanel, backpack, player, currentCategory, selectedIndex);
                if (slotMode.get()) {
                    refreshSlotMode(slotModeLeft, slotModeRight, backpack, currentCategory, null);
                }
                topToggleContainer.markAsInternal();
            });
            btn.addClass("category-toggle-btn");
            if (cat == currentCategory.get()) {
                btn.addClass("category-toggle-active");
            }

            topToggleContainer.addChild(btn);
        }
        topToggleContainer.markAsInternal();
    }

    private static void refreshItemList(
            UIElement itemListContainer,
            GenshinBackpack backpack,
            Player player,
            AtomicReference<GenshinBackpack.Category> currentCategory,
            AtomicReference<Integer> selectedIndex,
            AtomicReference<UIElement> selectedListElement,
            UIElement detailPanel) {

        itemListContainer.clearAllChildren();

        GenshinBackpack.Category cat = currentCategory.get();
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
                itemElement.addClass("backpack-item-selected");
                selectedListElement.set(itemElement);
            }

            itemElement.addEventListener(UIEvents.MOUSE_ENTER, e -> {
                if (!itemElement.hasClass("backpack-item-selected")) {
                    itemElement.addClass("backpack-item-hover");
                }
            });
            itemElement.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                itemElement.removeClass("backpack-item-hover");
            });
            itemElement.addEventListener(UIEvents.CLICK, e -> {
                if (selectedListElement.get() != null) {
                    selectedListElement.get().removeClass("backpack-item-selected");
                }
                selectedListElement.set(itemElement);
                itemElement.removeClass("backpack-item-hover");
                itemElement.addClass("backpack-item-selected");
                selectedIndex.set(idx);

                refreshDetailPanel(detailPanel, backpack, player, currentCategory, selectedIndex);
            });

            itemListContainer.addChild(itemElement);
        }

        if (selectedIndex.get() < 0 && firstNonEmpty >= 0) {
            selectedIndex.set(firstNonEmpty);
            refreshDetailPanel(detailPanel, backpack, player, currentCategory, selectedIndex);
        }

        itemListContainer.markAsInternal();
    }

    private static void refreshDetailPanel(
            UIElement detailPanel,
            GenshinBackpack backpack,
            Player player,
            AtomicReference<GenshinBackpack.Category> currentCategory,
            AtomicReference<Integer> selectedIndex) {

        detailPanel.clearAllChildren();

        GenshinBackpack.Category cat = currentCategory.get();
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
        takeOutBtn.setOnClick(e -> {
            NetworkManager.sendBackpackTakeOutToServer(catOrdinal, idx);
            backpack.removeItemFromCategory(cat, idx);
            selectedIndex.set(-1);
            var itemListContainer = findParentById(detailPanel, "item-list-container");
            if (itemListContainer != null) {
                refreshItemList(itemListContainer, backpack, player, currentCategory,
                        selectedIndex, new AtomicReference<>(), detailPanel);
            }
            refreshDetailPanel(detailPanel, backpack, player, currentCategory, selectedIndex);
        });

        var detailBtn = new Button();
        detailBtn.setText("详情");
        detailBtn.addClass("detail-info-btn");

        buttonContainer.addChildren(takeOutBtn, detailBtn);
        infoContainer.addChild(buttonContainer);

        detailPanel.addChild(infoContainer);
        detailPanel.markAsInternal();
    }

    private static void refreshSlotMode(
            UIElement slotModeLeft,
            UIElement slotModeRight,
            GenshinBackpack backpack,
            AtomicReference<GenshinBackpack.Category> currentCategory,
            GenshinBackpackMenu menu) {

        slotModeLeft.clearAllChildren();
        slotModeRight.clearAllChildren();

        GenshinBackpack.Category cat = currentCategory.get();

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
        int cols = 10;
        int totalSlots = Math.min(cat.maxCapacity, SLOT_MODE_VISIBLE_SLOTS);
        int rows = (int) Math.ceil((double) totalSlots / cols);

        ResourceHandler<ItemResource> handler = backpack.asResourceHandler();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int localIdx = row * cols + col;
                if (localIdx >= totalSlots) break;
                int flatIndex = categoryOffset + localIdx;
                var slot = new ItemSlot();
                slot.bind(handler, flatIndex);
                slotGridLeft.addChild(slot);
            }
        }

        slotModeLeft.addChild(leftScrollerContainer);

        var rightLabel = new Label().setText("玩家背包").addClass("slot-mode-label");
        slotModeRight.addChild(rightLabel);

        var inventorySlots = new InventorySlots();
        slotModeRight.addChild(inventorySlots);

        slotModeLeft.markAsInternal();
        slotModeRight.markAsInternal();
    }

    private static UIElement findParentById(UIElement start, String targetId) {
        return null;
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
}