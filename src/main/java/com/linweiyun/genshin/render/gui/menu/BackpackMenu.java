package com.linweiyun.genshin.render.gui.menu;

import com.linweiyun.genshin.core.attachment.Backpack;
import com.linweiyun.genshin.render.gui.components.CustomToggle;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ToggleGroupElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.HashMap;
import java.util.Map;

public class BackpackMenu {

    private static final int SLOTS_PER_ROW = 16;

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



        categoryGroup.setId("category_group");
        categoryGroup.toggleGroup.setAllowEmpty(false);

        // ========== 槽位网格 ==========
        ResourceHandler<ItemResource> handler = backpack.asResourceHandler();
        var genshinBackpackWindow = new ScrollerView();
        genshinBackpackWindow.setId("genshin_backpack_window");
        // 遍历所有分类，为每个分类创建槽位网格
        for (var category : Backpack.Category.values()) {
            var grid = buildGridForCategory(category, handler);
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

                    for (var grid : genshinBackpackWindow.viewContainer.getChildren()) {
                        grid.setDisplay(grid.getId().equals(
                                toggleCategoryMap.get(current.getId()).name().toLowerCase() + "_grid"));
                    }
                });
            }
        }

        // ========== 玩家背包 ==========
        var playerInventoryWindow = new UIElement().setId("player_inventory_window");

        var itemInfoPanel = new UIElement().setId("item_info_panel");

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
                                        genshinBackpackWindow,
                                        playerInventoryWindow.addChildren(
                                                new InventorySlots()
                                        )
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

    /**
     * 为指定分类生成槽位网格
     */
    private static UIElement buildGridForCategory(Backpack.Category category, ResourceHandler<ItemResource> handler) {
        var gridContainer = new UIElement().setId("grid_container");
        int offset = getCategoryOffset(category);
        int totalSlots = category.maxCapacity;
        int rowCount = (totalSlots + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW;

        for (int row = 0; row < rowCount; row++) {
            var rowContainer = new UIElement().setId("row_" + row + "_container").addClass("row_container");
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int slotId = row * SLOTS_PER_ROW + col;
                if (slotId >= totalSlots) break;
                rowContainer.addChild(new ItemSlot().bind(handler, offset + slotId));
            }
            gridContainer.addChild(rowContainer);
        }
        return gridContainer;
    }
}