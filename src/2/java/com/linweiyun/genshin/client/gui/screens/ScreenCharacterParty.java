package com.linweiyun.genshin.client.gui.screens;

import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ScreenCharacterParty extends Screen {
  final ModularUI modularUI;

  public ScreenCharacterParty(ModularUI modularUI) {
    super(Component.empty());
    this.modularUI = modularUI;
  }

  @OnlyIn(Dist.CLIENT)
  public void init() {
    super.init();
    modularUI.setScreenAndInit(this);
    this.addRenderableWidget(modularUI.getWidget());
  }

  public static ModularUI createModularUI(Player player) {
    var stylesheet =
        StylesheetManager.INSTANCE.getStylesheetSafe(
            ResourceLocation.parse("minegenshin:lss/character_party.lss"));
    var characterParty = player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT);

    UIElement root = new UIElement().setId("root");
    UIElement characterPartyContainer = new UIElement().setId("character-party-container");

    root.layout(
        layout -> {
          layout.widthPercent(100);
          layout.heightPercent(100);
        });
    for (int i = 0; i < 4; i++) {

      UIElement characterButton = new UIElement().setId("character-button");
      UIElement characterButtonImage = new UIElement().setId("character-button-image-empty");
      boolean haveCharacter = false;
      ItemStack stack = characterParty.getStackInSlot(i);
      if (!stack.isEmpty() && stack.getItem() instanceof PlayerCharacter character) {
        String characterID = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        characterButtonImage
            .setId("character-button-image-" + character.getCharacterName())
            .addClass("character-button-image-pose")
            .style(
                style ->
                    style.overlay(
                        SpriteTexture.of(
                            "minegenshin:textures/character_party_pose/"
                                + characterID
                                + "_already.png")));
        haveCharacter = true;
      }
      int index;
      if (haveCharacter) {
        index = i;
      } else {
        index = -1;
      }
      characterButton
          .addChild(characterButtonImage)
          .addEventListener(
              UIEvents.CLICK, e -> GUIServerHelperGIM.openCharacterSelectScreen(player, index));
      characterPartyContainer.addChild(characterButton);
    }

    root.addChild(characterPartyContainer);

    var ui = UI.of(root, stylesheet);

    return ModularUI.of(ui, player);
  }
}
