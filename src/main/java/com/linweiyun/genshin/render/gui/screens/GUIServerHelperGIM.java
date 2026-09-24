package com.linweiyun.genshin.render.gui.screens;

import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.render.gui.screens.artifact.ScreenArtifactEquip;
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
  public static void openCharacterInfoScreen(Player player) {
    NetworkManager.openCharacterInfoScreenToServer();
  }

  public static void openArtifactEquipScreen(Player player, int slotIndex) {
    var modularUI = ScreenArtifactEquip.createModularUI(player, slotIndex);
    Minecraft.getInstance().setScreenAndShow(new ScreenArtifactEquip(modularUI));
  }
  public static void openBackpackScreen(Player player) {
    NetworkManager.openBackpackMenuToServer();
  }

  public static void openAscensionScreen(Player player) {
    Minecraft.getInstance().setScreenAndShow(new ScreenAscension(player));
  }
}