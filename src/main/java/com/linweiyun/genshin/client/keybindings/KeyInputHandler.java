package com.linweiyun.genshin.client.keybindings;

import com.linweiyun.genshin.client.gui.screens.GUIServerHelperGIM;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class KeyInputHandler {


  private static boolean wasXKeyDown = false;
  private static long longPressStartTick = 0;

  @SubscribeEvent
  public static void onKeyInput(ClientTickEvent.Post event) {
    Minecraft mc = Minecraft.getInstance();
    Player player = mc.player;

    if (player == null) return;



    boolean isInGenshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
    PlayerCharactersAttachment charactersAttachment =
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    var character = charactersAttachment.getCurrentCharacter();

    if (KeyMappingRegistry.R_KEY.get().consumeClick()) {
      NetworkManager.wishEventToServer();
    }

    if (KeyMappingRegistry.G_KEY.get().consumeClick()) {
      boolean newMode = !isInGenshinMode;
      NetworkManager.setGenshinModeToServer(newMode);
    }

    //AI O_KEY 移到后面，取消原神模式限制（退出原神模式也能开配队面板）
    if (!isInGenshinMode) {
      KeyMappingRegistry.V_KEY.get().consumeClick();
      KeyMappingRegistry.X_KEY.get().consumeClick();
      KeyMappingRegistry.C_KEY.get().consumeClick();
    }

    if (KeyMappingRegistry.V_KEY.get().consumeClick()) {
      switchToNextAvailableCharacter(player, charactersAttachment);
    }
    boolean isXKeyDown = false;
    if (character != null) {
      if (character.getSkillShortMaxCooldownTick() == character.getSkillLongMaxCooldownTick()){
        if (KeyMappingRegistry.X_KEY.get().consumeClick()) {
          triggerCharacterSkill(player, -1);
        }
      } else {
        KeyMappingRegistry.X_KEY.get().consumeClick();
        isXKeyDown = KeyMappingRegistry.X_KEY.get().isDown();
        if (isXKeyDown && !wasXKeyDown) {
          longPressStartTick = System.currentTimeMillis();
        } else if (isXKeyDown) {
          int time = (int) (System.currentTimeMillis() - longPressStartTick);
          if (time >= 1000) {
            triggerCharacterSkill(player, time);
          }
        }
        else if (!isXKeyDown && wasXKeyDown) {
          int time = (int) (System.currentTimeMillis() - longPressStartTick);
          if (time < 1000) {
            triggerCharacterSkill(player, time);
          }
          longPressStartTick = 0;
        }
        wasXKeyDown = isXKeyDown;
      }
    }
    if (KeyMappingRegistry.C_KEY.get().consumeClick()) {
      triggerCharacterBurst(player);
    }
    if (KeyMappingRegistry.O_KEY.get().consumeClick()) {
      GUIServerHelperGIM.openCharacterPartyScreen(player);
    }
    if (KeyMappingRegistry.CHARACTER_INFO_SCREEN_KEY.get().consumeClick()) {
      GUIServerHelperGIM.openCharacterInfoScreen(player);
    }
  }

  private static void switchToNextAvailableCharacter(Player player, PlayerCharactersAttachment attachment) {
    wasXKeyDown = false;
    longPressStartTick = 0;
    int currentIndex = attachment.getCurrentCharacterIndex();
    for (int i = 1; i <= 4; i++) {
      int nextIndex = (currentIndex + i) % 4;
      PGCharacter character = attachment.getPartyCharacter(nextIndex);

      if (character != null && character.getData().getCurrentHP() > 0) {
        attachment.setCurrentCharacterIndex(nextIndex);
        NetworkManager.setCharacterSelectionToServer(nextIndex);
        player.sendSystemMessage(
                Component.literal("已切换到角色 " + character.getName().getString()));
        return;
      }
    }
  }
  private static void triggerCharacterSkill(Player player, int isLong) {
    PlayerCharactersAttachment attachment =
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);

    var currentChar = attachment.getCurrentCharacter();
    if (currentChar != null) {
      currentChar.performElementalSkill(player, isLong);
    }
    NetworkManager.triggerCharacterSkill(isLong);
  }

  private static void triggerCharacterBurst(Player player) {
    PlayerCharactersAttachment attachment =
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    PGCharacter currentChar = attachment.getCurrentCharacter();
    if (currentChar != null) {
      currentChar.performElementalBurst(player);
    }
    NetworkManager.triggerCharacterBurst();
  }
}