package com.linweiyun.genshin.client.gui.screens;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class ScreenCharacterInfo extends Screen {
    final ModularUI modularUI;
    protected ScreenCharacterInfo(ModularUI modularUI) {
        super(Component.empty());
        this.modularUI = modularUI;
    }

    public void init() {
        super.init();
        ModularUIClientAccess.setScreenAndInit(this.modularUI, this);
        this.addRenderableWidget(ModularUIClientAccess.getWidget(modularUI));
    }
    public static ModularUI createModularUI(Player player) {
        var root = new UIElement();
        var stylesheet = StylesheetManager.INSTANCE.getStylesheetSafe(
                Identifier.parse("minegenshin:lss/character_info.lss"));
        var backGround = new UIElement();
        var character_list_container = new UIElement();

        root.addChildren(
                backGround.addChildren(
                        character_list_container
                )
        );

        var ui = UI.of(root, stylesheet);
        return ModularUI.of(ui, player);
    }
}
