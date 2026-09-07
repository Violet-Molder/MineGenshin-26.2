package com.linweiyun.genshin.render.gui.screens.atrifact;

import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.content.items.artifact.ArtifactType;
import com.linweiyun.genshin.content.items.component.ArtifactStatsComponent;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.GenshinBackpack;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.ArtifactInventory;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.system.registry.register.ModDataComponents;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scroller;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ScreenArtifactEquip extends Screen {

    final ModularUI modularUI;

    public ScreenArtifactEquip(ModularUI modularUI) {
        super(Component.empty());
        this.modularUI = modularUI;
    }

    @Override
    public void init() {
        super.init();
        ModularUIClientAccess.setScreenAndInit(this.modularUI, this);
        this.addRenderableWidget(ModularUIClientAccess.getWidget(modularUI));
    }

    private static final float SCROLL_COEFFICIENT = 20f;
    private static final ArtifactType[] ARTIFACT_TYPES = ArtifactType.values();
    private static final String[] TYPE_TOGGLE_LABELS = {
            "生之花", "死之羽", "时之沙", "空之杯", "理之冠"
    };

    public static ModularUI createModularUI(Player player, int initialSlotIndex) {
        var stylesheet = StylesheetManager.INSTANCE.getStylesheetSafe(
                Identifier.parse("minegenshin:lss/artifact_equip.lss"));

        PlayerCharactersAttachment charactersAttachment =
                player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter currentChar = charactersAttachment.getCurrentCharacter();

        var root = new UIElement().setId("root");
        root.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });

        if (currentChar == null || currentChar.getData() == null) {
            var emptyLabel = new Label().setText("未选择角色");
            root.addChild(emptyLabel);
            var ui = UI.of(root, stylesheet);
            return ModularUI.of(ui, player);
        }

        PGCharacterData charData = currentChar.getData();
        ArtifactInventory artifactInv = charData.getArtifactInventory();
        GenshinBackpack genshinBackpack = player.getData(AttachmentRegistration.GENSHIN_BACKPACK_ATTACHMENT);

        AtomicReference<Integer> selectedSlotIndex = new AtomicReference<>(initialSlotIndex);
        AtomicReference<ItemStack> selectedInventoryArtifact = new AtomicReference<>(ItemStack.EMPTY);
        AtomicReference<Integer> selectedInventorySlotIndex = new AtomicReference<>(-1);
        AtomicReference<ArtifactType> currentFilterType = new AtomicReference<>(
                initialSlotIndex >= 0 && initialSlotIndex < ArtifactInventory.SLOT_COUNT
                        ? ArtifactInventory.slotToType(initialSlotIndex)
                        : ArtifactType.FLOWER);
        AtomicReference<UIElement> selectedListElement = new AtomicReference<>();
        AtomicReference<Float> scrollValue = new AtomicReference<>(0.0f);
        AtomicReference<Boolean> topSectionVisible = new AtomicReference<>(true);

        var window = new UIElement().setId("window");

        var topSection = new UIElement().setId("top-section");
        var artifactSlotsRow = new UIElement().setId("artifact-slots-row");

        for (int i = 0; i < ArtifactInventory.SLOT_COUNT; i++) {
            int slotIdx = i;
            ItemStack equipped = artifactInv.getItem(i);
            var slotElement = new UIElement().addClass("equipped-slot");

            if (!equipped.isEmpty()) {
                String texturePath = getArtifactTexturePath(equipped);
                slotElement.style(style -> style.background(
                        SpriteTexture.of(texturePath)));
            } else {
                slotElement.addClass("empty-slot");
            }

            slotElement.addEventListener(UIEvents.MOUSE_ENTER, e -> {
                slotElement.style(style -> style.overlay(
                        SpriteTexture.of("minegenshin:textures/character_avatar/selected_border.png")));
                slotElement.transform(transform -> transform.scale(1.1f));
            });
            slotElement.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                boolean isSelectedEquipped = selectedSlotIndex.get() == slotIdx
                        && selectedInventoryArtifact.get().isEmpty()
                        && topSectionVisible.get();
                if (!isSelectedEquipped) {
                    slotElement.style(style -> style.overlay(null));
                    slotElement.transform(transform -> transform.scale(1f));
                }
            });
            slotElement.addEventListener(UIEvents.CLICK, e -> {
                selectedSlotIndex.set(slotIdx);
                selectedInventoryArtifact.set(ItemStack.EMPTY);
                selectedInventorySlotIndex.set(-1);
                ArtifactType clickedType = ArtifactInventory.slotToType(slotIdx);
                currentFilterType.set(clickedType);
                topSectionVisible.set(false);
                topSection.setDisplay(false);

                var detailSection = findChildById(window, "detail-section");
                if (detailSection != null) {
                    detailSection.setDisplay(true);
                    var artifactListContainer = findChildById(detailSection, "artifact-list-container");
                    var typeToggleContainer = findChildById(detailSection, "type-toggle-container");

                    autoSelectFirst(artifactInv, genshinBackpack, currentFilterType,
                            selectedInventoryArtifact, selectedInventorySlotIndex);

                    refreshTypeToggle(typeToggleContainer, currentFilterType,
                            artifactInv, genshinBackpack, selectedSlotIndex,
                            selectedInventoryArtifact, selectedInventorySlotIndex,
                            selectedListElement, artifactListContainer,
                            detailSection, charData, scrollValue);
                    refreshArtifactList(artifactListContainer, artifactInv, genshinBackpack,
                            selectedSlotIndex, selectedInventoryArtifact,
                            selectedInventorySlotIndex, currentFilterType,
                            selectedListElement, detailSection, charData, scrollValue);
                    refreshDetailPanel(detailSection, artifactInv, genshinBackpack,
                            selectedSlotIndex, selectedInventoryArtifact,
                            selectedInventorySlotIndex, currentFilterType,
                            selectedListElement, charData);
                    detailSection.markAsInternal();
                }
            });

            artifactSlotsRow.addChild(slotElement);
        }

        topSection.addChild(artifactSlotsRow);

        var detailSection = new UIElement().setId("detail-section");
        detailSection.setDisplay(initialSlotIndex >= 0);

        var leftPanel = new UIElement().setId("left-panel");
        var typeToggleContainer = new UIElement().setId("type-toggle-container");

        var listScrollerContainer = new UIElement().setId("list-scroller-container");
        var verticalScroller = new Scroller.Vertical();
        var artifactListContainer = new UIElement().setId("artifact-list-container");

        verticalScroller.bindObserver(value -> {
            scrollValue.set(value);
            artifactListContainer.layout(l -> l.bottom(value * SCROLL_COEFFICIENT));
            artifactListContainer.markAsInternal();
        });

        listScrollerContainer.addChild(verticalScroller);
        listScrollerContainer.addChild(artifactListContainer);
        leftPanel.addChildren(typeToggleContainer, listScrollerContainer);

        var middlePanel = new UIElement().setId("middle-panel");
        var rightPanel = new UIElement().setId("right-panel");

        detailSection.addChildren(leftPanel, middlePanel, rightPanel);
        window.addChildren(topSection, detailSection);
        root.addChild(window);

        if (initialSlotIndex >= 0) {
            topSectionVisible.set(false);
            topSection.setDisplay(false);

            autoSelectFirst(artifactInv, genshinBackpack, currentFilterType,
                    selectedInventoryArtifact, selectedInventorySlotIndex);

            refreshTypeToggle(typeToggleContainer, currentFilterType,
                    artifactInv, genshinBackpack, selectedSlotIndex,
                    selectedInventoryArtifact, selectedInventorySlotIndex,
                    selectedListElement, artifactListContainer,
                    detailSection, charData, scrollValue);
            refreshArtifactList(artifactListContainer, artifactInv, genshinBackpack,
                    selectedSlotIndex, selectedInventoryArtifact,
                    selectedInventorySlotIndex, currentFilterType,
                    selectedListElement, detailSection, charData, scrollValue);
            refreshDetailPanel(detailSection, artifactInv, genshinBackpack,
                    selectedSlotIndex, selectedInventoryArtifact,
                    selectedInventorySlotIndex, currentFilterType,
                    selectedListElement, charData);
        }

        var ui = UI.of(root, stylesheet);
        return ModularUI.of(ui, player);
    }

    private static void autoSelectFirst(
            ArtifactInventory artifactInv,
            GenshinBackpack genshinBackpack,
            AtomicReference<ArtifactType> currentFilterType,
            AtomicReference<ItemStack> selectedInventoryArtifact,
            AtomicReference<Integer> selectedInventorySlotIndex) {

        ArtifactType filterType = currentFilterType.get();
        int filterSlotIdx = ArtifactInventory.typeToSlot(filterType);
        ItemStack equipped = artifactInv.getItem(filterSlotIdx);

        if (!equipped.isEmpty()) {
            selectedInventoryArtifact.set(equipped.copy());
            selectedInventorySlotIndex.set(-1);
            return;
        }

        ItemStack[] artifactArr = genshinBackpack.getCategoryArray(GenshinBackpack.Category.ARTIFACTS);
        for (int invSlot = 0; invSlot < artifactArr.length; invSlot++) {
            ItemStack stack = artifactArr[invSlot];
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ArtifactItem artItem)) continue;
            if (artItem.getType() != filterType) continue;
            selectedInventoryArtifact.set(stack.copy());
            selectedInventorySlotIndex.set(invSlot);
            return;
        }

        selectedInventoryArtifact.set(ItemStack.EMPTY);
        selectedInventorySlotIndex.set(-1);
    }

    private static UIElement findChildById(UIElement parent, String id) {
        if (id.equals(parent.getId())) return parent;
        for (var child : parent.getChildren()) {
            var found = findChildById(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private static void refreshTypeToggle(
            UIElement typeToggleContainer,
            AtomicReference<ArtifactType> currentFilterType,
            ArtifactInventory artifactInv,
            GenshinBackpack genshinBackpack,
            AtomicReference<Integer> selectedSlotIndex,
            AtomicReference<ItemStack> selectedInventoryArtifact,
            AtomicReference<Integer> selectedInventorySlotIndex,
            AtomicReference<UIElement> selectedListElement,
            UIElement artifactListContainer,
            UIElement detailSection,
            PGCharacterData charData,
            AtomicReference<Float> scrollValue) {

        typeToggleContainer.clearAllChildren();

        for (int i = 0; i < ARTIFACT_TYPES.length; i++) {
            ArtifactType type = ARTIFACT_TYPES[i];
            var toggleBtn = new Button();
            toggleBtn.setText(TYPE_TOGGLE_LABELS[i]);
            toggleBtn.setOnClick(e -> {
                currentFilterType.set(type);
                selectedInventoryArtifact.set(ItemStack.EMPTY);
                selectedInventorySlotIndex.set(-1);
                selectedListElement.set(null);

                int slotIdx = ArtifactInventory.typeToSlot(type);
                selectedSlotIndex.set(slotIdx);

                autoSelectFirst(artifactInv, genshinBackpack, currentFilterType,
                        selectedInventoryArtifact, selectedInventorySlotIndex);

                refreshTypeToggle(typeToggleContainer, currentFilterType,
                        artifactInv, genshinBackpack, selectedSlotIndex,
                        selectedInventoryArtifact, selectedInventorySlotIndex,
                        selectedListElement, artifactListContainer,
                        detailSection, charData, scrollValue);
                refreshArtifactList(artifactListContainer, artifactInv, genshinBackpack,
                        selectedSlotIndex, selectedInventoryArtifact,
                        selectedInventorySlotIndex, currentFilterType,
                        selectedListElement, detailSection, charData, scrollValue);
                refreshDetailPanel(detailSection, artifactInv, genshinBackpack,
                        selectedSlotIndex, selectedInventoryArtifact,
                        selectedInventorySlotIndex, currentFilterType,
                        selectedListElement, charData);
                typeToggleContainer.markAsInternal();
            });
            toggleBtn.addClass("type-toggle-btn");
            if (type == currentFilterType.get()) {
                toggleBtn.addClass("type-toggle-active");
            }

            typeToggleContainer.addChild(toggleBtn);
        }
        typeToggleContainer.markAsInternal();
    }

    private static void refreshArtifactList(
            UIElement artifactListContainer,
            ArtifactInventory artifactInv,
            GenshinBackpack genshinBackpack,
            AtomicReference<Integer> selectedSlotIndex,
            AtomicReference<ItemStack> selectedInventoryArtifact,
            AtomicReference<Integer> selectedInventorySlotIndex,
            AtomicReference<ArtifactType> currentFilterType,
            AtomicReference<UIElement> selectedListElement,
            UIElement detailSection,
            PGCharacterData charData,
            AtomicReference<Float> scrollValue) {

        artifactListContainer.clearAllChildren();
        selectedListElement.set(null);

        ArtifactType filterType = currentFilterType.get();
        int slotIdx = ArtifactInventory.typeToSlot(filterType);
        ItemStack equipped = artifactInv.getItem(slotIdx);

        List<ArtifactListEntry> entries = new ArrayList<>();

        if (!equipped.isEmpty()) {
            entries.add(new ArtifactListEntry(equipped.copy(), true, -1));
        }

        ItemStack[] artifactArr = genshinBackpack.getCategoryArray(GenshinBackpack.Category.ARTIFACTS);
        for (int invSlot = 0; invSlot < artifactArr.length; invSlot++) {
            ItemStack stack = artifactArr[invSlot];
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ArtifactItem artItem)) continue;
            if (artItem.getType() != filterType) continue;
            entries.add(new ArtifactListEntry(stack.copy(), false, invSlot));
        }

        for (int i = 0; i < entries.size(); i++) {
            ArtifactListEntry entry = entries.get(i);
            var itemElement = new UIElement().addClass("artifact-list-item");

            String texturePath = getArtifactTexturePath(entry.stack);
            itemElement.style(style -> style.background(SpriteTexture.of(texturePath)));

            boolean shouldBeSelected = false;
            ItemStack currentSelected = selectedInventoryArtifact.get();
            if (!currentSelected.isEmpty()) {
                shouldBeSelected = ItemStack.isSameItemSameComponents(entry.stack, currentSelected);
            } else if (entry.isEquipped) {
                shouldBeSelected = true;
            }

            if (shouldBeSelected) {
                itemElement.addClass("artifact-list-selected");
                selectedListElement.set(itemElement);
            }

            itemElement.addEventListener(UIEvents.MOUSE_ENTER, e -> {
                if (!itemElement.hasClass("artifact-list-selected")) {
                    itemElement.addClass("artifact-list-hover");
                }
            });
            itemElement.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                itemElement.removeClass("artifact-list-hover");
            });
            itemElement.addEventListener(UIEvents.CLICK, e -> {
                if (selectedListElement.get() != null) {
                    selectedListElement.get().removeClass("artifact-list-selected");
                }
                selectedListElement.set(itemElement);
                itemElement.removeClass("artifact-list-hover");
                itemElement.addClass("artifact-list-selected");

                if (entry.isEquipped) {
                    selectedInventoryArtifact.set(entry.stack.copy());
                    selectedInventorySlotIndex.set(-1);
                } else {
                    selectedInventoryArtifact.set(entry.stack.copy());
                    selectedInventorySlotIndex.set(entry.inventorySlotIndex);
                }

                refreshDetailPanel(detailSection, artifactInv, genshinBackpack,
                        selectedSlotIndex, selectedInventoryArtifact,
                        selectedInventorySlotIndex, currentFilterType,
                        selectedListElement, charData);
            });

            artifactListContainer.addChild(itemElement);
        }

        artifactListContainer.markAsInternal();
    }

    private static void refreshDetailPanel(
            UIElement detailSection,
            ArtifactInventory artifactInv,
            GenshinBackpack genshinBackpack,
            AtomicReference<Integer> selectedSlotIndex,
            AtomicReference<ItemStack> selectedInventoryArtifact,
            AtomicReference<Integer> selectedInventorySlotIndex,
            AtomicReference<ArtifactType> currentFilterType,
            AtomicReference<UIElement> selectedListElement,
            PGCharacterData charData) {

        UIElement middlePanel = null;
        UIElement rightPanel = null;

        for (var child : detailSection.getChildren()) {
            String id = child.getId();
            if ("middle-panel".equals(id)) middlePanel = child;
            else if ("right-panel".equals(id)) rightPanel = child;
        }

        if (middlePanel == null || rightPanel == null) return;

        middlePanel.clearAllChildren();
        rightPanel.clearAllChildren();

        int slotIdx = selectedSlotIndex.get();
        if (slotIdx < 0 || slotIdx >= ArtifactInventory.SLOT_COUNT) return;

        ItemStack selectedFromInv = selectedInventoryArtifact.get();
        ItemStack equippedInSlot = artifactInv.getItem(slotIdx);

        ItemStack displayStack;
        boolean isSelectedFromEquipped;

        if (!selectedFromInv.isEmpty()) {
            displayStack = selectedFromInv;
            isSelectedFromEquipped = selectedInventorySlotIndex.get() < 0;
        } else if (!equippedInSlot.isEmpty()) {
            displayStack = equippedInSlot;
            isSelectedFromEquipped = true;
        } else {
            var emptyLabel = new Label().setText("该槽位无圣遗物").setId("info-empty");
            rightPanel.addChild(emptyLabel);
            rightPanel.markAsInternal();
            return;
        }

        var iconElement = new UIElement().addClass("artifact-detail-icon");
        String detailTexturePath = getArtifactDetailTexturePath(displayStack);
        iconElement.style(style -> style.background(SpriteTexture.of(detailTexturePath)));
        middlePanel.addChild(iconElement);
        middlePanel.markAsInternal();

        if (!(displayStack.getItem() instanceof ArtifactItem artItem)) return;
        ArtifactStatsComponent stats = displayStack.getOrDefault(
                ModDataComponents.ARTIFACT_STATS.get(), ArtifactStatsComponent.DEFAULT);
        int star = artItem.getStar();

        var infoContainer = new UIElement().setId("info-container");

        var nameLabel = new Label()
                .setText(displayStack.getHoverName())
                .addClass("info-name");
        infoContainer.addChild(nameLabel);

        var starLabel = new Label()
                .setText(Component.literal("★".repeat(star)).withStyle(ChatFormatting.GOLD))
                .addClass("info-star");
        infoContainer.addChild(starLabel);

        var levelLabel = new Label()
                .setText(Component.literal("等级: +" + stats.level).withStyle(ChatFormatting.GRAY))
                .addClass("info-level");
        infoContainer.addChild(levelLabel);

        long expToNext = stats.getExpToNextLevel(star);
        var expLabel = new Label()
                .setText(Component.literal(
                        expToNext > 0
                                ? "经验: " + stats.exp + " / " + expToNext
                                : "经验: 已满级"
                ).withStyle(ChatFormatting.GRAY))
                .addClass("info-exp");
        infoContainer.addChild(expLabel);

        if (stats.mainStat != null && stats.mainStat.isInitialized()) {
            var mainStatLabel = new Label()
                    .setText(Component.literal(buildStatText(stats.mainStat)).withStyle(ChatFormatting.YELLOW))
                    .addClass("info-main-stat");
            infoContainer.addChild(mainStatLabel);
        }

        if (stats.subStats != null) {
            for (TeyvatItemStat subStat : stats.subStats) {
                if (!subStat.isInitialized()) continue;
                ChatFormatting color = subStat.isUnlocked() ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY;
                var subLabel = new Label()
                        .setText(Component.literal(buildStatText(subStat)).withStyle(color))
                        .addClass("info-sub-stat");
                infoContainer.addChild(subLabel);
            }
        }

        var buttonContainer = new UIElement().setId("button-container");

        var actionButton = new Button();
        actionButton.addClass("action-button");

        if (isSelectedFromEquipped) {
            if (!equippedInSlot.isEmpty()) {
                actionButton.setText("卸下");
                ItemStack toUnequip = equippedInSlot.copy();
                actionButton.setOnClick(e -> {
                    NetworkManager.sendUnequipArtifactToServer(slotIdx);
                    artifactInv.setItem(slotIdx, ItemStack.EMPTY);
                    genshinBackpack.addItemToCategory(GenshinBackpack.Category.ARTIFACTS, toUnequip);
                    selectedInventoryArtifact.set(ItemStack.EMPTY);
                    selectedInventorySlotIndex.set(-1);
                    refreshArtifactList(findChildById(detailSection, "artifact-list-container"),
                            artifactInv, genshinBackpack, selectedSlotIndex,
                            selectedInventoryArtifact, selectedInventorySlotIndex,
                            currentFilterType, selectedListElement, detailSection, charData,
                            new AtomicReference<>(0.0f));
                    refreshDetailPanel(detailSection, artifactInv, genshinBackpack,
                            selectedSlotIndex, selectedInventoryArtifact,
                            selectedInventorySlotIndex, currentFilterType,
                            selectedListElement, charData);
                });
            } else {
                actionButton.setDisplay(false);
            }
        } else {
            if (equippedInSlot.isEmpty()) {
                actionButton.setText("穿戴");
            } else {
                actionButton.setText("更换");
            }
            int invSlotIdx = selectedInventorySlotIndex.get();
            ItemStack toEquip = selectedFromInv.copy();
            actionButton.setOnClick(e -> {
                NetworkManager.sendEquipOrSwapArtifactToServer(slotIdx, invSlotIdx);
                ItemStack old = artifactInv.getItem(slotIdx);
                genshinBackpack.removeItemFromCategory(GenshinBackpack.Category.ARTIFACTS, invSlotIdx);
                artifactInv.setItem(slotIdx, toEquip);
                if (!old.isEmpty()) {
                    genshinBackpack.addItemToCategory(GenshinBackpack.Category.ARTIFACTS, old.copy());
                }
                selectedInventoryArtifact.set(ItemStack.EMPTY);
                selectedInventorySlotIndex.set(-1);
                refreshArtifactList(findChildById(detailSection, "artifact-list-container"),
                        artifactInv, genshinBackpack, selectedSlotIndex,
                        selectedInventoryArtifact, selectedInventorySlotIndex,
                        currentFilterType, selectedListElement, detailSection, charData,
                        new AtomicReference<>(0.0f));
                refreshDetailPanel(detailSection, artifactInv, genshinBackpack,
                        selectedSlotIndex, selectedInventoryArtifact,
                        selectedInventorySlotIndex, currentFilterType,
                        selectedListElement, charData);
            });
        }

        var upgradeButton = new Button();
        upgradeButton.setText("升级");
        upgradeButton.addClass("upgrade-button");
        upgradeButton.setOnClick(e -> {
            NetworkManager.sendArtifactLevelUpToServer(displayStack, 10000);
        });

        if (stats.level >= stats.getMaxLevel(star)) {
            upgradeButton.setDisplay(false);
        }

        buttonContainer.addChildren(actionButton, upgradeButton);
        infoContainer.addChild(buttonContainer);

        rightPanel.addChild(infoContainer);
        rightPanel.markAsInternal();
    }

    private static String buildStatText(TeyvatItemStat stat) {
        if (!stat.isInitialized()) return "";
        String attrName = Component.translatable(stat.getAttribute().translationKey()).getString();
        if (stat.getKind() == TeyvatItemStat.StatKind.PERCENT) {
            return attrName + " +" + String.format("%.1f%%", stat.getValue() * 100);
        } else {
            return attrName + " +" + String.format("%.0f", stat.getValue());
        }
    }

    private static String getArtifactTexturePath(ItemStack stack) {
        if (stack.isEmpty()) return "minegenshin:textures/empty.png";
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace() + ":textures/item/" + id.getPath() + ".png";
    }

    private static String getArtifactDetailTexturePath(ItemStack stack) {
        if (stack.isEmpty()) return "minegenshin:textures/empty.png";
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace() + ":textures/item/" + id.getPath() + ".png";
    }

    private record ArtifactListEntry(ItemStack stack, boolean isEquipped, int inventorySlotIndex) {}
}