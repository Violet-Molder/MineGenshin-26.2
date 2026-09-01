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
import org.appliedenergistics.yoga.YogaFlexDirection;

public class TutorialScreenThree extends Screen {
  protected TutorialScreenThree(Component title) {
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
    var image = new UIElement();
    root.addChildren(
            new Label()
                .setText("演示UI1")
                .textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER)),
            image
                .layout(layout -> layout.width(80).height(80))
                .style(style -> style.background(SpriteTexture.of("ldlib2:textures/gui/icon.png"))),
            new UIElement()
                .layout(l -> l.flexDirection(YogaFlexDirection.ROW))
                .addChildren(
                    new Button()
                        .setText("-45°")
                        .setOnClick(
                            event -> {
                              image.transform(
                                  transform -> transform.rotation(transform.rotation() - 45));
                            }),
                    new UIElement().layout(l -> l.flex(1)),
                    new Button()
                        .setText("+45°")
                        .setOnClick(
                            event -> {
                              image.transform(
                                  transform -> transform.rotation(transform.rotation() + 45));
                            })))
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
