package com.linweiyun.genshin.client.gui.screens;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ScreenArtifactInfo extends Screen {
    final ModularUI modularUI;
    protected ScreenArtifactInfo(ModularUI modularUI) {
        super(Component.empty());
        this.modularUI = modularUI;
    }

    public void init() {
        super.init();
        ModularUIClientAccess.setScreenAndInit(this.modularUI, this);
        this.addRenderableWidget(ModularUIClientAccess.getWidget(modularUI));
    }
    public static ModularUI createModularUI(Player player, ItemStack artifactStack) {
        var root = new UIElement();
        var stylesheet = StylesheetManager.INSTANCE.getStylesheetSafe(
                Identifier.parse("minegenshin:lss/artifact_info.lss"));
        var backGround = new UIElement();
        var levelLabel = new Label();





        root.addChildren(
                backGround.addChildren(

                        )
        );

        var ui = UI.of(root, stylesheet);
        return ModularUI.of(ui, player);
    }
}
