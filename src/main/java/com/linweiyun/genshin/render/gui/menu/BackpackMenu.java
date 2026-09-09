package com.linweiyun.genshin.render.gui.menu;

import com.linweiyun.genshin.core.attachment.Backpack;
import com.linweiyun.genshin.render.gui.components.CustomToggle;
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

    public static ModularUI createUI(Player player, Backpack backpack) {
        var root = new UIElement().setId("root");
        var stylesheet = StylesheetManager.INSTANCE.getStylesheetSafe(
                Identifier.parse("minegenshin:lss/backpack.lss"));

        // ========== Window ==========
        var window = new UIElement().setId("window");

        // ========== Panel（上下分栏） ==========
        var sidebarPanel = new UIElement().setId("sidebar-panel");
        var contentPanel = new UIElement().setId("content-panel");

        // ========== Section（分类切换区） ==========
        var categorySection = new UIElement().setId("category-section");
        // ========== ToggleGroup ==========
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
        // 图标模式容器（默认显示，先空着）
        var iconModeSection = new UIElement().setId("icon-mode-section");
        // 图标模式的左侧（图标列表区，对应槽位模式的 genshinBackpackWindow）
        var iconGenshinBackpackWindow = new ScrollerView();
        iconGenshinBackpackWindow.setId("icon-genshin-backpack-window");

        // 改为
        var detailPanel = new UIElement().setId("detail-panel-container");
        AtomicReference<UIElement> iconGrid = new AtomicReference<>(
                refreshIconList(backpack, Backpack.Category.WEAPONS, detailPanel));
        iconGrid.get().setId("icon-grid-container");

        // 图标模式的右侧（详情面板区，对应槽位模式的 playerInventoryWindow）
        var iconPlayerInventoryWindow = new UIElement().setId("icon-player-inventory-window");


        // 槽位模式容器（默认隐藏，包裹原来的槽位网格 + 玩家背包）
        var slotModeSection = new UIElement().setId("slot-mode-section");
        var modeSwitch = new Switch();

        // ========== 玩家背包 ==========
        var playerInventoryWindow = new UIElement().setId("player_inventory_window");

        var itemInfoPanel = new UIElement().setId("item_info_panel");
        modeSwitch.setId("mode-switch");
        modeSwitch.setOnSwitchChanged(isOn -> {
            slotModeSection.setDisplay(isOn);
            iconModeSection.setDisplay(!isOn);
            if (!isOn) {
                iconGenshinBackpackWindow.clearAllScrollViewChildren();
                iconGrid.set(refreshIconList(backpack, currentCategoryRef.get(), detailPanel));
                iconGenshinBackpackWindow.addScrollViewChild(iconGrid.get());
            }
        });

        // ========== 槽位网格 ==========
        ResourceHandler<ItemResource> handler = backpack.asResourceHandler();
        var genshinBackpackWindow = new ScrollerView();
        genshinBackpackWindow.setId("genshin_backpack_window");
        // 遍历所有分类，为每个分类创建槽位网格
        for (var category : Backpack.Category.values()) {
            var grid = buildGridForCategory(category, handler, genshinBackpackWindow);
            grid.setId(category.name().toLowerCase() + "_grid");
            grid.setDisplay(false);
            genshinBackpackWindow.addScrollViewChild(grid);
        }

        // 默认显示第一个分类的网格
        String firstGridId = Backpack.Category.values()[0].name().toLowerCase() + "_grid";
        for (var child : genshinBackpackWindow.viewContainer.getChildren()) {
            if (child.getId().equals(firstGridId)) {
                child.setDisplay(true);
                break;
            }
        }

        // ========== 窗口大小变化后稳定再重建 ==========
        final long[] lastResizeTime = {0};
        final long DEBOUNCE_MS = 50;
        int[] currentSlotsPerRow = {0};

        // LAYOUT_CHANGED：只记录时间，不重建
        genshinBackpackWindow.viewContainer.addEventListener(UIEvents.LAYOUT_CHANGED, e -> {
            lastResizeTime[0] = System.currentTimeMillis();
        });

        // TICK：每 tick 检查是否已稳定 500ms
        genshinBackpackWindow.viewContainer.addEventListener(UIEvents.TICK, e -> {
            if (lastResizeTime[0] == 0) return;
            if (System.currentTimeMillis() - lastResizeTime[0] < DEBOUNCE_MS) return;
            lastResizeTime[0] = 0; // 重置，防止重复触发

            int slotSize = 18;
            int newSlotsPerRow = (int) (genshinBackpackWindow.getContainerWidth() / slotSize);
            if (newSlotsPerRow <= 0) newSlotsPerRow = 8;
            if (newSlotsPerRow == currentSlotsPerRow[0]) return;
            currentSlotsPerRow[0] = newSlotsPerRow;

            // 记住当前显示的网格
            String visibleGridId = null;
            for (var child : genshinBackpackWindow.viewContainer.getChildren()) {
                if (child.isDisplayed()) {
                    visibleGridId = child.getId();
                    break;
                }
            }

            // 清空并重建所有网格
            genshinBackpackWindow.clearAllScrollViewChildren();
            for (var category : Backpack.Category.values()) {
                var grid = buildGridForCategory(category, handler, genshinBackpackWindow);
                grid.setId(category.name().toLowerCase() + "_grid");
                grid.setDisplay(grid.getId().equals(visibleGridId));
                genshinBackpackWindow.addScrollViewChild(grid);
            }
        });

        // 监听分类切换，显示/隐藏对应网格
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
                        grid.setDisplay(grid.getId().equals(
                                toggleCategoryMap.get(current.getId()).name().toLowerCase() + "_grid"));
                    }
                    iconGenshinBackpackWindow.clearAllScrollViewChildren();
                    iconGrid.set(refreshIconList(backpack, toggleCategoryMap.get(current.getId()), detailPanel));
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
                                                iconGenshinBackpackWindow.addScrollViewChildren(iconGrid.get()),
                                                iconPlayerInventoryWindow.addChildren(detailPanel)
                                        ),                              // ← 新增
                                        slotModeSection.addChildren(                   // ← 新增包裹
                                                genshinBackpackWindow,
                                                playerInventoryWindow.addChildren(
                                                        new InventorySlots()
                                                )
                                        ).setDisplay(false),
                                        modeSwitch
                                )
                        )
                )
        );

        var ui = UI.of(root, stylesheet);
        return ModularUI.of(ui, player);
    }

    /**
     * 计算指定分类的全局偏移量（前面所有分类容量之和）
     */
    private static int getCategoryOffset(Backpack.Category target) {
        int offset = 0;
        for (var category : Backpack.Category.values()) {
            if (category == target) break;
            offset += category.maxCapacity;
        }
        return offset;
    }

    // ========================================================================
    //  图标模式 - 渲染物品图标列表
    // ========================================================================

    private static UIElement refreshIconList(Backpack backpack, Backpack.Category category,
                                             UIElement detailPanel) {
        var gridContainer = new UIElement().setId("icon-grid-container");
        AtomicReference<UIElement> selectedRef = new AtomicReference<>();

        int offset = getCategoryOffset(category);
        int totalSlots = category.maxCapacity;
        UIElement firstNonEmpty = null;

        for (int i = 0; i < totalSlots; i++) {
            ItemStack stack = backpack.getItem(offset + i);
            if (stack.isEmpty()) continue;

            var itemElement = new UIElement().addClass("backpack-icon-item");
            itemElement.style(s -> s.background(SpriteTexture.of(getItemTexturePath(stack))));

            // 默认选中第一个
            if (firstNonEmpty == null) {
                firstNonEmpty = itemElement;
                itemElement.transform(t -> t.scale(1.1f));
                itemElement.style(s -> s.overlay(new ColorBorderTexture(1, 0xFFFFFFFF)));
                selectedRef.set(itemElement);
                refreshDetailPanel(detailPanel, stack);
            }

            // 悬停放大 + 白色边框（未选中时）
            int finalI = i;
            itemElement.addEventListener(UIEvents.MOUSE_ENTER, e -> {
                if (selectedRef.get() != itemElement) {
                    itemElement.transform(t -> t.scale(1.1f));
                    itemElement.style(s -> s.overlay(new ColorBorderTexture(1, 0xFFFFFFFF)));
                }
            });
            itemElement.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                if (selectedRef.get() != itemElement) {
                    itemElement.transform(t -> t.scale(1f));
                    itemElement.style(s -> s.overlay(null));
                }
            });

            // 按下时回缩到 1.0
            itemElement.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                itemElement.transform(t -> t.scale(1f));
            });

            // 点击选中：放大 + 白色边框 + 更新详情
            itemElement.addEventListener(UIEvents.CLICK, e -> {
                UIElement prev = selectedRef.getAndSet(itemElement);
                if (prev != null && prev != itemElement) {
                    prev.transform(t -> t.scale(1f));
                    prev.style(s -> s.overlay(null));
                }
                itemElement.transform(t -> t.scale(1.1f));
                itemElement.style(s -> s.overlay(new ColorBorderTexture(1, 0xFFFFFFFF)));

                int globalSlot = offset + finalI;
                refreshDetailPanel(detailPanel, backpack.getItem(globalSlot));
            });

            gridContainer.addChild(itemElement);
        }

        return gridContainer;
    }
    private static String getItemTexturePath(ItemStack stack) {
        if (stack.isEmpty()) return "minegenshin:textures/empty.png";
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace() + ":textures/item/" + id.getPath() + ".png";
    }

    private static void refreshDetailPanel(UIElement container, ItemStack stack) {
        container.clearAllChildren();

        if (stack.isEmpty()) {
            container.addChild(new Label().setText("未选中物品"));
            return;
        }

        // 收集物品的 tooltip 文本
        List<Component> tooltipLines = new ArrayList<>();
        // 第一行：物品名称
        tooltipLines.add(stack.getHoverName());
        // 用 appendHoverText 收集其余信息
        stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT,
                tooltipLines::add, TooltipFlag.NORMAL);

        // 创建 ScrollerView 使详情可滚动
        var scroller = new ScrollerView();
        scroller.setId("detail-scroller");

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
    }

    /**
     * 为指定分类生成槽位网格
     */
    private static UIElement buildGridForCategory(
            Backpack.Category category,
            ResourceHandler<ItemResource> handler,
            ScrollerView scrollerView) {                       // ← 新增参数

        var gridContainer = new UIElement().setId("grid_container");
        int offset = getCategoryOffset(category);
        int totalSlots = category.maxCapacity;

        // 动态计算每行槽位数：容器宽度 / 18
        int slotSize = 18;
        int slotsPerRow = (int) (scrollerView.getContainerWidth() / slotSize);
        if (slotsPerRow <= 0) slotsPerRow = 8;                 // fallback 默认值

        int rowCount = (totalSlots + slotsPerRow - 1) / slotsPerRow;

        for (int row = 0; row < rowCount; row++) {
            var rowContainer = new UIElement().setId("row_" + row + "_container").addClass("row_container");
            for (int col = 0; col < slotsPerRow; col++) {      // ← 用动态值
                int slotId = row * slotsPerRow + col;           // ← 用动态值
                if (slotId >= totalSlots) break;
                rowContainer.addChild(new ItemSlot().bind(handler, offset + slotId));
            }
            gridContainer.addChild(rowContainer);
        }
        return gridContainer;
    }
}