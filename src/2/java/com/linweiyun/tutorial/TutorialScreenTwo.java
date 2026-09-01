package com.linweiyun.tutorial;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TutorialScreenTwo extends Screen {
  protected TutorialScreenTwo(Component title) {
    super(title);
  }

  @Override
  protected void init() {
    super.init();
    var modularUI = createModularUI();
    modularUI.setScreenAndInit(this);
    this.addRenderableOnly(modularUI.getWidget());
  }

  private static ModularUI createModularUI() {
    var root = new UIElement();
    root.addChildren(
            new Label()
                .setText("演示UI1")
                .textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER)),
            new Button().setText("演示Button1"),
            new UIElement()
                .layout(layout -> layout.width(80).height(80))
                .style(style -> style.background(SpriteTexture.of("ldlib2:textures/gui/icon.png"))))
        .style(style -> style.background(Sprites.BORDER))
        .layout(layout -> layout.paddingAll(7).gapAll(5));

    var ui = UI.of(root);
    return ModularUI.of(ui);
  }

  public static void openScreen() {
    var modularUI = createModularUI();
    Minecraft.getInstance().setScreen(new ModularUIScreen(modularUI, Component.empty()));
  }
}
