package com.linweiyun.genshin.client.gui.screens;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ScreenGenshinBackpack extends Screen {
    final ModularUI modularUI;

    public ScreenGenshinBackpack(ModularUI modularUI) {
        super(Component.empty());
        this.modularUI = modularUI;
    }

    public void init() {
        super.init();
        ModularUIClientAccess.setScreenAndInit(this.modularUI, this);
        this.addRenderableWidget(ModularUIClientAccess.getWidget(modularUI));
    }

    public static ModularUI createModularUI(Player player) {

//        var backpack = player.getData(AttachmentRegistration.GENSHIN_BACKPACK_ATTACHMENT);

        UIElement root = new UIElement().setId("root");

        var ui = UI.of(root);

        return ModularUI.of(ui, player);
    }
}
