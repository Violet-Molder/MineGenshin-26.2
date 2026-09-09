package com.linweiyun.genshin.render.gui.screens;

import com.linweiyun.genshin.render.gui.menu.CharacterInfoMenu;
import com.linweiyun.genshin.content.items.artifact.inventory.ArtifactInventory;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;

public class ScreenArtifaceInfo extends AbstractContainerScreen<CharacterInfoMenu> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public ScreenArtifaceInfo(CharacterInfoMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.empty());
    }

    @Override
    public void init() {
        super.init();
        // 隐藏标题
        this.titleLabelX = -9999;
        this.titleLabelY = -9999;
        this.inventoryLabelX = -9999;
        this.inventoryLabelY = -9999;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < ArtifactInventory.SLOT_COUNT; i++) {
            Slot slot = this.menu.getSlot(i);
        }
        return super.mouseClicked(event, doubleClick);
    }
}