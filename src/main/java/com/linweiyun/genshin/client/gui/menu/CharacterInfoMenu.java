package com.linweiyun.genshin.client.gui.menu;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.ArtifactInventory;
import com.linweiyun.genshin.core.character.ArtifactSlot;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.registry.register.ModMenus;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharacterInfoMenu extends AbstractContainerMenu {

    private static final Logger LOGGER = LoggerFactory.getLogger("MineGenshin/CharacterInfoMenu");

    private final ArtifactInventory artifactInventory;

    public CharacterInfoMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.CHARACTER_INFO_MENU.get(), containerId);
        Player player = playerInventory.player;
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter character = attachment.getCurrentCharacter();
        if (character != null && character.getData() != null) {
            this.artifactInventory = character.getData().getArtifactInventory();
        } else {
            this.artifactInventory = new ArtifactInventory();
        }

        for (int i = 0; i < ArtifactInventory.SLOT_COUNT; i++) {
            this.addSlot(new ArtifactSlot(artifactInventory, i, -9999, -9999));
            LOGGER.info("Menu addSlot: index={}, slotClass=ArtifactSlot, x=-9999, y=-9999");
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, -9999, -9999));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, -9999, -9999));
        }
        LOGGER.info("Menu addSlot: added player inventory slots, totalSlots={}", this.slots.size());

        var modularUI = createModularUI(player);
        if (this instanceof IModularUIHolderMenu holder) {
            holder.setModularUI(modularUI);
            LOGGER.info("Menu: IModularUIHolderMenu setModularUI done, modularUI width={}, height={}",
                    modularUI.getWidth(), modularUI.getHeight());
        } else {
            LOGGER.warn("Menu: this is NOT IModularUIHolderMenu!");
        }
    }

    private ModularUI createModularUI(Player player) {
        var root = new UIElement();
        var stylesheet = StylesheetManager.INSTANCE.getStylesheetSafe(
                Identifier.parse("minegenshin:lss/character_info.lss"));
        var backGround = new UIElement();
        var character_list_container = new UIElement().setId("character_list_container")
                .layout(layoutStyle -> layoutStyle.flexDirection(FlexDirection.ROW));

        var artifact_container = new UIElement().setId("artifact_container")
                .layout(layoutStyle -> layoutStyle.flexDirection(FlexDirection.ROW));
        ResourceHandler<ItemResource> artifactHandler = artifactInventory.asResourceHandler();
        for (int i = 0; i < ArtifactInventory.SLOT_COUNT; i++) {
            var slot = new ItemSlot();
            slot.bind(artifactHandler, i);
            LOGGER.info("ItemSlot.bind(handler, {}): handlerClass={}", i, artifactHandler.getClass().getSimpleName());
            artifact_container.addChildren(slot);
        }

        root.addChildren(
                backGround.addChildren(
                        character_list_container,
                        artifact_container,
                        new InventorySlots()
                )
        );

        var ui = UI.of(root, stylesheet);
        return ModularUI.of(ui, player);
    }

    public ArtifactInventory getArtifactInventory() {
        return artifactInventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        LOGGER.info("Menu.quickMoveStack: index={}, slotClass={}, slotX={}, slotY={}, hasItem={}",
                index,
                slot != null ? slot.getClass().getSimpleName() : "null",
                slot != null ? slot.x : "null",
                slot != null ? slot.y : "null",
                slot != null && slot.hasItem());
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack slotStack = slot.getItem();
        ItemStack result = slotStack.copy();

        int artifactSlots = ArtifactInventory.SLOT_COUNT;
        int totalSlots = this.slots.size();

        if (index < artifactSlots) {
            LOGGER.info("Menu.quickMoveStack: moving artifact slot {} to player inventory", index);
            if (!this.moveItemStackTo(slotStack, artifactSlots, totalSlots, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            LOGGER.info("Menu.quickMoveStack: moving player slot {} to artifact slots", index);
            if (!this.moveItemStackTo(slotStack, 0, artifactSlots, false)) {
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
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex >= 0 && slotIndex < ArtifactInventory.SLOT_COUNT) {
            Slot slot = slotIndex < this.slots.size() ? this.getSlot(slotIndex) : null;
            LOGGER.info("Menu.clicked [ARTIFACT]: slotIndex={}, buttonNum={}, containerInput={}, slotClass={}, slotX={}, slotY={}, item={}",
                    slotIndex, buttonNum, containerInput,
                    slot != null ? slot.getClass().getSimpleName() : "null",
                    slot != null ? slot.x : "null",
                    slot != null ? slot.y : "null",
                    slot != null ? slot.getItem() : "null");
        } else {
            LOGGER.info("Menu.clicked [OTHER]: slotIndex={}, buttonNum={}, containerInput={}, totalSlots={}",
                    slotIndex, buttonNum, containerInput, this.slots.size());
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}