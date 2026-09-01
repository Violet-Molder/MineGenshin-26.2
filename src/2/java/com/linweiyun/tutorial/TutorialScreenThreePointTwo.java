package com.linweiyun.tutorial;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaFlexDirection;

public class TutorialScreenThreePointTwo extends Screen {
  protected TutorialScreenThreePointTwo(Component title) {
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
                    new UIElement()
                        .addChild(
                            new TextElement()
                                .setText("-45°")
                                .textStyle(
                                    style -> {
                                      style.adaptiveWidth(true).adaptiveHeight(true);
                                    }))
                        .style(style -> style.background(Sprites.RECT_RD))
                        .layout(
                            layout -> {
                              layout.width(22);
                              layout.height(14);
                              layout.justifyContent(AlignContent.CENTER);
                              layout.alignItems(AlignItems.CENTER);
                            })
                        .addEventListener(
                            UIEvents.MOUSE_DOWN,
                            event -> {
                              event.currentElement.style(
                                  style -> style.background(Sprites.RECT_RD_DARK));
                              image.transform(
                                  transform -> transform.rotation(transform.rotation() - 45));
                            })
                        .addEventListener(
                            UIEvents.MOUSE_UP,
                            event ->
                                event.currentElement.style(
                                    style -> style.background(Sprites.RECT_RD_LIGHT)))
                        .addEventListener(
                            UIEvents.MOUSE_ENTER,
                            event ->
                                event.currentElement.style(
                                    style -> style.background(Sprites.RECT_RD_LIGHT)))
                        .addEventListener(
                            UIEvents.MOUSE_LEAVE,
                            event ->
                                event.currentElement.style(
                                    style -> style.background(Sprites.RECT_RD))),
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
