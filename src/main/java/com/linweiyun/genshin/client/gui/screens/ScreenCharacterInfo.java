package com.linweiyun.genshin.client.gui.screens;

import com.linweiyun.genshin.client.gui.menu.CharacterInfoMenu;
import com.linweiyun.genshin.core.character.ArtifactInventory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScreenCharacterInfo extends AbstractContainerScreen<CharacterInfoMenu> {

    private static final Logger LOGGER = LoggerFactory.getLogger("MineGenshin/ScreenCharacterInfo");

    public ScreenCharacterInfo(CharacterInfoMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        LOGGER.info("Screen.mouseClicked: event={}, doubleClick={}, hoveredSlot={}",
                event, doubleClick,
                this.hoveredSlot != null
                        ? "index=" + this.hoveredSlot.index
                        + " class=" + this.hoveredSlot.getClass().getSimpleName()
                        + " x=" + this.hoveredSlot.x
                        + " y=" + this.hoveredSlot.y
                        + " item=" + this.hoveredSlot.getItem()
                        : "null");
        for (int i = 0; i < ArtifactInventory.SLOT_COUNT; i++) {
            Slot slot = this.menu.getSlot(i);
            LOGGER.info("Screen.mouseClicked [ARTIFACT slot {}]: slotX={}, slotY={}, slotClass={}, isActive={}, item={}",
                    i, slot.x, slot.y, slot.getClass().getSimpleName(), slot.isActive(), slot.getItem());
        }
        LOGGER.info("Screen.mouseClicked: totalMenuSlots={}", this.menu.slots.size());
        return super.mouseClicked(event, doubleClick);
    }
}