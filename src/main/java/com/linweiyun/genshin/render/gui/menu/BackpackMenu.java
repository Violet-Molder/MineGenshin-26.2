package com.linweiyun.genshin.render.gui.menu;

import com.linweiyun.genshin.core.attachment.Backpack;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.render.gui.components.CustomToggle;
import com.linweiyun.genshin.render.gui.components.state_bind_com.CustomItemSlot;
import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class BackpackMenu {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SLOTS_PER_ROW = 14;

    public static ModularUI createUI(Player player, Backpack backpack) {
        var root = new UIElement().setId("root");
        var stylesheet = StylesheetManager.INSTANCE.getStylesheetSafe(
                Identifier.parse("minegenshin:lss/backpack.lss"));

        // ========== Window ==========
        var window = new UIElement().setId("window");

        // ========== Panel ==========
        var sidebarPanel = new UIElement().setId("sidebar-panel");
        var contentPanel = new UIElement().setId("content-panel");

        // ========== Section（分类切换区） ==========
        var categorySection = new UIElement().setId("category-section");
        var categoryGroup = new ToggleGroupElement();
        for (var category : Backpack.Category.values()) {
            String toggleId = category.name().toLowerCase() + "-toggle";
            var toggle = new CustomToggle();
            toggle.setId(toggleId).addClass("category-toggle");
            toggle.setButtonText(category.displayName);
            categoryGroup.addChild(toggle);
        }
        UIElement firstChild = categoryGroup.getChildren().get(0);
        if (firstChild instanceof Toggle firstToggle) {
            firstToggle.setOn(true);
        }
        var backpackContainer = new UIElement().setId("backpack_container");

        AtomicReference<Backpack.Category> currentCategoryRef =
                new AtomicReference<>(Backpack.Category.WEAPONS);
        categoryGroup.setId("category_group");
        categoryGroup.toggleGroup.setAllowEmpty(false);

        // ========== 模式容器 ==========
        var iconModeSection = new UIElement().setId("icon-mode-section");
        var iconGenshinBackpackWindow = new ScrollerView();
        iconGenshinBackpackWindow.setId("icon-genshin-backpack-window");
        var iconPlayerInventoryWindow = new UIElement().setId("icon-player-inventory-window");

        var detailPanel = new UIElement().setId("detail-panel-container");

        // 持久化追踪当前选中的槽位索引（全局 slot）
        AtomicReference<Integer> selectedSlotRef = new AtomicReference<>(-1);

        AtomicReference<UIElement> iconGrid = new AtomicReference<>(null);
        UIElement initialGrid = refreshIconList(backpack, Backpack.Category.WEAPONS, detailPanel,
                player, iconGenshinBackpackWindow, iconGrid, currentCategoryRef, selectedSlotRef);
        iconGrid.set(initialGrid);
        iconGrid.get().setId("icon-grid-container");

        var slotModeSection = new UIElement().setId("slot-mode-section");
        slotModeSection.setDisplay(false);

        var modeSwitch = new Switch();
        modeSwitch.setId("mode-switch");
        modeSwitch.setOnSwitchChanged(isOn -> {
            slotModeSection.setDisplay(isOn);
            iconModeSection.setDisplay(!isOn);
            if (!isOn) {
                iconGenshinBackpackWindow.clearAllScrollViewChildren();
                iconGrid.set(refreshIconList(backpack, currentCategoryRef.get(), detailPanel,
                        player, iconGenshinBackpackWindow, iconGrid, currentCategoryRef, selectedSlotRef));
                iconGenshinBackpackWindow.addScrollViewChild(iconGrid.get());
            }
        });

        // ========== 玩家背包 ==========
        var playerInventoryWindow = new UIElement().setId("player_inventory_window");

        // ========== 槽位网格 ==========
        ResourceHandler<ItemResource> handler = backpack.asResourceHandler();
        var genshinBackpackWindow = new ScrollerView();
        genshinBackpackWindow.setId("genshin_backpack_window");

        for (var category : Backpack.Category.values()) {
            var grid = buildGridForCategory(category, handler);
            grid.setId(category.name().toLowerCase() + "_grid");
            grid.setDisplay(false);
            genshinBackpackWindow.addScrollViewChild(grid);
        }

        String firstGridId = Backpack.Category.values()[0].name().toLowerCase() + "_grid";
        for (var child : genshinBackpackWindow.viewContainer.getChildren()) {
            if (child.getId().equals(firstGridId)) {
                child.setDisplay(true);
                break;
            }
        }

        // 监听分类切换
        Map<String, Backpack.Category> toggleCategoryMap = new HashMap<>();
        for (var category : Backpack.Category.values()) {
            toggleCategoryMap.put(category.name().toLowerCase() + "-toggle", category);
        }

        for (var child : categoryGroup.getChildren()) {
            if (child instanceof Toggle toggle) {
                toggle.setOnToggleChanged(isOn -> {
                    if (!isOn) return;
                    Toggle current = categoryGroup.toggleGroup.getCurrentToggle();
                    if (current == null) return;
                    Backpack.Category cat = toggleCategoryMap.get(current.getId());
                    currentCategoryRef.set(cat);

                    for (var grid : genshinBackpackWindow.viewContainer.getChildren()) {
                        grid.setDisplay(grid.getId().equals(cat.name().toLowerCase() + "_grid"));
                    }
                    iconGenshinBackpackWindow.clearAllScrollViewChildren();
                    iconGrid.set(refreshIconList(backpack, cat, detailPanel,
                            player, iconGenshinBackpackWindow, iconGrid, currentCategoryRef, selectedSlotRef));
                    iconGenshinBackpackWindow.addScrollViewChild(iconGrid.get());
                });
            }
        }

        root.layout(layout -> {
            layout.widthPercent(100f);
            layout.heightPercent(100f);
        });

        // ========== 组装 UI 树 ==========
        root.addChildren(
                window.addChildren(
                        sidebarPanel.addChildren(
                                categorySection.addChildren(categoryGroup)
                        ),
                        contentPanel.addChildren(
                                backpackContainer.addChildren(
                                        iconModeSection.addChildren(
                                                iconGenshinBackpackWindow.addScrollViewChild(iconGrid.get()),
                                                iconPlayerInventoryWindow.addChildren(detailPanel)
                                        ),
                                        slotModeSection.addChildren(
                                                genshinBackpackWindow,
                                                playerInventoryWindow.addChildren(new InventorySlots())
                                        ),
                                        modeSwitch
                                )
                        )
                )
        );

        var ui = UI.of(root, stylesheet);
        return ModularUI.of(ui, player);
    }

    // ========================================================================
    //  辅助方法
    // ========================================================================

    private static int getCategoryOffset(Backpack.Category target) {
        int offset = 0;
        for (var category : Backpack.Category.values()) {
            if (category == target) break;
            offset += category.maxCapacity;
        }
        return offset;
    }

    // ========================================================================
    //  图标模式
    // ========================================================================

    private static UIElement refreshIconList(
            Backpack backpack, Backpack.Category category,
            UIElement detailPanel,
            Player player,
            ScrollerView iconGenshinBackpackWindow,
            AtomicReference<UIElement> iconGrid,
            AtomicReference<Backpack.Category> currentCategoryRef,
            AtomicReference<Integer> selectedSlotRef) {

        var gridContainer = new UIElement().setId("icon-grid-container");
        AtomicReference<UIElement> selectedElementRef = new AtomicReference<>();

        int offset = getCategoryOffset(category);
        int totalSlots = category.maxCapacity;

        // 如果当前选中的 slot 已空或不在本分类内，清除选中
        int curSlot = selectedSlotRef.get();
        if (curSlot < offset || curSlot >= offset + totalSlots || backpack.getItem(curSlot).isEmpty()) {
            curSlot = -1;
        }

        // 遍历构建图标
        for (int i = 0; i < totalSlots; i++) {
            ItemStack stack = backpack.getItem(offset + i);
            if (stack.isEmpty()) continue;

            int globalSlot = offset + i;

            // 如果还没选中任何物品，就选第一个非空的
            if (curSlot < 0) {
                curSlot = globalSlot;
            }

            var itemElement = new UIElement().addClass("backpack-icon-item");
            itemElement.style(s -> s.background(SpriteTexture.of(getItemTexturePath(stack))));

            // 高亮当前选中的
            if (globalSlot == curSlot) {
                itemElement.transform(t -> t.scale(1.1f));
                itemElement.style(s -> s.overlay(new ColorBorderTexture(1, 0xFFFFFFFF)));
                selectedElementRef.set(itemElement);
            }

            itemElement.addEventListener(UIEvents.MOUSE_ENTER, e -> {
                if (selectedElementRef.get() != itemElement) {
                    itemElement.transform(t -> t.scale(1.1f));
                    itemElement.style(s -> s.overlay(new ColorBorderTexture(1, 0xFFFFFFFF)));
                }
            });
            itemElement.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                if (selectedElementRef.get() != itemElement) {
                    itemElement.transform(t -> t.scale(1f));
                    itemElement.style(s -> s.overlay(null));
                }
            });
            itemElement.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                itemElement.transform(t -> t.scale(1f));
            });
            itemElement.addEventListener(UIEvents.CLICK, e -> {
                UIElement prev = selectedElementRef.getAndSet(itemElement);
                if (prev != null && prev != itemElement) {
                    prev.transform(t -> t.scale(1f));
                    prev.style(s -> s.overlay(null));
                }
                itemElement.transform(t -> t.scale(1.1f));
                itemElement.style(s -> s.overlay(new ColorBorderTexture(1, 0xFFFFFFFF)));
                selectedSlotRef.set(globalSlot);
                refreshDetailPanelWithButtons(detailPanel, backpack.getItem(globalSlot), globalSlot,
                        backpack, player, iconGenshinBackpackWindow, iconGrid, currentCategoryRef, selectedSlotRef);
            });

            gridContainer.addChild(itemElement);
        }

        // 更新选中状态
        selectedSlotRef.set(curSlot);
        if (curSlot >= 0) {
            refreshDetailPanelWithButtons(detailPanel, backpack.getItem(curSlot), curSlot,
                    backpack, player, iconGenshinBackpackWindow, iconGrid, currentCategoryRef, selectedSlotRef);
        } else {
            detailPanel.clearAllChildren();
            detailPanel.addChild(new Label().setText("未选中物品"));
            detailPanel.markAsInternal();
        }

        return gridContainer;
    }

    private static void refreshDetailPanelWithButtons(
            UIElement container, ItemStack stack, int globalSlotIndex,
            Backpack backpack, Player player,
            ScrollerView iconGenshinBackpackWindow,
            AtomicReference<UIElement> iconGrid,
            AtomicReference<Backpack.Category> currentCategoryRef,
            AtomicReference<Integer> selectedSlotRef) {

        container.clearAllChildren();

        if (stack.isEmpty()) {
            container.addChild(new Label().setText("未选中物品"));
            return;
        }

        // 物品信息
        var scroller = new ScrollerView();
        scroller.setId("detail-scroller");

        var nameLabel = new Label().setText(stack.getHoverName());
        nameLabel.addClass("detail-line");
        scroller.addScrollViewChild(nameLabel);

        List<Component> tooltipLines = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT,
                tooltipLines::add, TooltipFlag.NORMAL);

        for (Component line : tooltipLines) {
            String text = line.getString();
            String[] lines = text.split("\n");
            for (String singleLine : lines) {
                var label = new Label().setText(Component.literal(singleLine).withStyle(line.getStyle()));
                label.addClass("detail-line");
                scroller.addScrollViewChild(label);
            }
        }

        container.addChild(scroller);

        // ========== 按钮区 ==========
        var buttonContainer = new UIElement().setId("button-container");

        var takeOutBtn = new Button();
        takeOutBtn.setText("取出");
        takeOutBtn.addClass("item-action-btn");
        takeOutBtn.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            NetworkManager.sendBackpackTakeOutFromSlotToServer(globalSlotIndex);
            backpack.setItem(globalSlotIndex, ItemStack.EMPTY);
            iconGenshinBackpackWindow.clearAllScrollViewChildren();
            iconGrid.set(refreshIconList(backpack, currentCategoryRef.get(), container,
                    player, iconGenshinBackpackWindow, iconGrid, currentCategoryRef, selectedSlotRef));
            iconGenshinBackpackWindow.addScrollViewChild(iconGrid.get());
        });

        var detailBtn = new Button();
        detailBtn.setText("详情");
        detailBtn.addClass("item-action-btn");

        buttonContainer.addChildren(takeOutBtn, detailBtn);
        container.addChild(buttonContainer);
    }


    private static String getItemTexturePath(ItemStack stack) {
        if (stack.isEmpty()) return "minegenshin:textures/empty.png";
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace() + ":textures/item/" + id.getPath() + ".png";
    }

    // ========================================================================
    //  槽位网格
    // ========================================================================

    private static UIElement buildGridForCategory(Backpack.Category category,
                                                  ResourceHandler<ItemResource> handler) {
        var gridContainer = new UIElement().setId("grid_container");
        int offset = getCategoryOffset(category);
        int totalSlots = category.maxCapacity;
        int rowCount = (totalSlots + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW;

        for (int row = 0; row < rowCount; row++) {
            var rowContainer = new UIElement().setId("row_" + row + "_container").addClass("row_container");
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int slotId = row * SLOTS_PER_ROW + col;
                if (slotId >= totalSlots) break;
                var wrapper = new UIElement().addClass("backpack-slot").setId("backpack_slot");
                var slot = new CustomItemSlot().bind(handler, offset + slotId);
                slot.layout(l -> l.widthPercent(100).heightPercent(100));
                wrapper.addChild(slot);
                rowContainer.addChild(wrapper);
            }
            gridContainer.addChild(rowContainer);
        }
        return gridContainer;
    }
}