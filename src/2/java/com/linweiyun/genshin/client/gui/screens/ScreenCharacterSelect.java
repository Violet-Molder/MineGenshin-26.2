package com.linweiyun.genshin.client.gui.screens;

import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterSheet;
import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ScreenCharacterSelect extends Screen {
  final ModularUI modularUI;

  public ScreenCharacterSelect(ModularUI modularUI) {
    super(Component.empty());
    this.modularUI = modularUI;
  }

  @OnlyIn(Dist.CLIENT)
  public void init() {
    super.init();
    modularUI.setScreenAndInit(this);
    this.addRenderableWidget(modularUI.getWidget());
  }

  private static final float SCROLL_COEFFICIENT = 20f;

  public static ModularUI createModularUI(Player player, int index) {
    var stylesheet =
        StylesheetManager.INSTANCE.getStylesheetSafe(
            ResourceLocation.parse("minegenshin:lss/character_select.lss"));
    AtomicReference<Float> number = new AtomicReference<>(0.0f);
    var characterSheet = player.getData(AttachmentRegistration.CHARACTER_SHEET_ATTACHMENT);
    var characterParty = player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);
    var root = new UIElement().setId("root");
    var characterView = new UIElement().setId("character-view");
    var verticalScroller = new Scroller.Vertical();
    var characterContainer = new UIElement().setId("character-container");
    var sortSelector = new Selector<CharacterSheet.SortMethod>();
    var characterListContainer = new UIElement().setId("character-list-container");
    var characterList = new UIElement().setId("character-list");
    AtomicReference<ItemStack> selectedCharacter = new AtomicReference<>(ItemStack.EMPTY);
    var deployButton = new Button();
    var removeCharacterButton = new Button();
    var switchCharacterButton = new Button();
    var currentCharacter = new UIElement();

    AtomicReference<UIElement> selectedCharacterButton = new AtomicReference<>();

    root.layout(
        layout -> {
          layout.widthPercent(100f);
          layout.heightPercent(100f);
        });

    verticalScroller.bindObserver(
        value -> {
          number.set(value);
          characterList.layout(l -> l.bottom(number.get() * SCROLL_COEFFICIENT));
          characterList.markAsInternal();
        });

    deployButton
        .setText("出战")
        .setOnClick(
            e -> {
              int emptyIndex = -1;
              boolean canAdd = true;
              for (int i = 0; i < characterParty.getSlots(); i++) {
                ItemStack stack = characterParty.getStackInSlot(i);
                if (stack.getItem() == selectedCharacter.get().getItem()) {
                  canAdd = false;
                  break;
                } else if (emptyIndex == -1 && stack.isEmpty()) {
                  emptyIndex = i;
                }
              }
              if (canAdd) {
                characterParty.insertAndSortItem(
                    emptyIndex, selectedCharacter.get(), false, player);
                GUIServerHelperGIM.openCharacterPartyScreen(player);
              }
            })
        .setId("deploy-button")
        .setDisplay(false);

    removeCharacterButton
        .setText("换下")
        .setOnClick(
            e -> {
              characterParty.extractAndSortItem(index, false, player);
              int currentCharacterSlot = characterParty.getCurrentCharacterSlot();
              if (currentCharacterSlot != 1) {
                characterParty.setClientCurrentCharacter(currentCharacterSlot - 1);
              }
              GUIServerHelperGIM.openCharacterPartyScreen(player);
            })
        .setDisplay(false);
    switchCharacterButton
        .setText("更换")
        .setOnClick(
            e -> {
              boolean canSwitch = false;
              int slot = 0;
              for (int i = 0; i < characterParty.getSlots(); i++) {
                ItemStack stackInSlot = characterParty.getStackInSlot(i);
                if (stackInSlot.getItem() == selectedCharacter.get().getItem()) {
                  slot = i;
                  canSwitch = true;

                  break;
                }
              }

              ItemStack character = characterParty.getStackInSlot(index);
              characterParty.changeAndSortItem(index, selectedCharacter.get(), false, player);
              if (canSwitch) {
                characterParty.changeAndSortItem(slot, character, false, player);
              }
              GUIServerHelperGIM.openCharacterPartyScreen(player);
            })
        .setDisplay(false);
    if (index != -1) {
      removeCharacterButton.setDisplay(true);
      ItemStack stack = characterParty.getStackInSlot(index);
      if (stack.getItem() instanceof PlayerCharacter character) {
        String characterID = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        currentCharacter
            .setId("Pose-Stand-" + character.getCharacterName())
            .addClass("character-pose")
            .style(
                style ->
                    style.background(
                        SpriteTexture.of(
                            "minegenshin:textures/character_party_pose/"
                                + characterID
                                + "_prepare.png")));
      }
    }
    Runnable renderCharacterAvatars =
        () -> {
          characterList.clearAllChildren();
          for (int i = 0; i < characterSheet.getSlots(); i++) {
            ItemStack stack = characterSheet.getStackInSlot(i);
            Item item = stack.getItem();
            String characterID = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (item instanceof PlayerCharacter character) {
              var characterButton =
                  new UIElement()
                      .addClass("character-avatar")
                      .style(
                          style ->
                              style.background(
                                  SpriteTexture.of(
                                      "minegenshin:textures/character_avatar/"
                                          + characterID
                                          + ".png")));
              characterButton
                  .addEventListener(
                      UIEvents.MOUSE_ENTER,
                      e -> {
                        if (selectedCharacterButton.get() != characterButton) {
                          characterButton.style(
                              style ->
                                  style.overlay(
                                      SpriteTexture.of(
                                          "minegenshin:textures/character_avatar/selected_border.png")));
                          characterButton.transform(transform -> transform.scale(1.08f));
                        }
                      })
                  .addEventListener(
                      UIEvents.MOUSE_LEAVE,
                      e -> {
                        if (selectedCharacterButton.get() != characterButton) {
                          characterButton.style(style -> style.overlay(null));
                          characterButton.transform(transform -> transform.scale(1f));
                        }
                      })
                  .addEventListener(
                      UIEvents.CLICK,
                      e -> {
                        currentCharacter
                            .setId("Pose-Stand-" + character.getCharacterName())
                            .addClass("character-pose")
                            .style(
                                style ->
                                    style.background(
                                        SpriteTexture.of(
                                            "minegenshin:textures/character_party_pose/"
                                                + characterID
                                                + "_prepare.png")));

                        if (selectedCharacterButton.get() != null
                            && selectedCharacterButton.get() != characterButton) {
                          selectedCharacterButton.get().transform(transform -> transform.scale(1f));
                          selectedCharacterButton.get().style(style -> style.overlay(null));
                          selectedCharacterButton.get().removeClass("character-selected");
                        }
                        selectedCharacterButton.set(characterButton);
                        characterButton.addClass("character-selected");
                        characterButton.transform(transform -> transform.scale(1.08f));
                        characterButton.style(
                            style ->
                                style.overlay(
                                    SpriteTexture.of(
                                        "minegenshin:textures/character_avatar/selected_border.png")));
                        if (index == -1) {
                          deployButton.setDisplay(true);
                        } else if (characterParty.getStackInSlot(index).getItem()
                            == stack.getItem()) {
                          removeCharacterButton.setDisplay(true);
                          switchCharacterButton.setDisplay(false);
                        } else {
                          switchCharacterButton.setDisplay(true);
                          removeCharacterButton.setDisplay(false);
                        }

                        selectedCharacter.set(stack);
                      });
              characterList.addChild(characterButton);
            }
          }

          characterList.markAsInternal();
        };

    sortSelector
        .setSelected(characterSheet.getSortMethod(), false)
        .setCandidates(Arrays.asList(CharacterSheet.SortMethod.values()))
        .setOnValueChanged(
            selectedSortMethod -> {
              characterSheet.setSortMethodToServer(selectedSortMethod, player);
              number.set(0.0f);
              characterList.layout(l -> l.bottom(number.get()));
              renderCharacterAvatars.run();
              characterList.markAsInternal();
            });

    root.addChildren(
        characterView.addChildren(
            verticalScroller,
            characterContainer.addChildren(
                sortSelector,
                characterListContainer.addChildren(characterList),
                deployButton,
                removeCharacterButton,
                switchCharacterButton)),
        currentCharacter);

    renderCharacterAvatars.run();

    var ui = UI.of(root, stylesheet);

    return ModularUI.of(ui, player);
  }
}
