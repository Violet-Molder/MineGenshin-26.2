package com.linweiyun.genshin.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class GUIServerHelperGIM {
  public static void openCharacterSelectScreen(Player player, int index) {
    var modularUI = ScreenCharacterSelect.createModularUI(player, index);
    Minecraft.getInstance().setScreenAndShow(new ScreenCharacterSelect(modularUI));
  }

  public static void openCharacterPartyScreen(Player player) {
    var modularUI = ScreenCharacterParty.createModularUI(player);
    Minecraft.getInstance().setScreenAndShow(new ScreenCharacterParty(modularUI));
  }

//  public static void openTestGraphScreen(Player player) {
//    var modularUI = TestGraphScreen.createModularUI(player);
//    Minecraft.getInstance().setScreenAndShow(new TestGraphScreen(modularUI));
//  }
}
