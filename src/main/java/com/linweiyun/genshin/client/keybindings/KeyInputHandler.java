package com.linweiyun.genshin.client.keybindings;

import com.linweiyun.genshin.client.gui.screens.GUIServerHelperGIM;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.character.PGCharacterDefine;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.skill.CharacterSkillHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class KeyInputHandler {


  private static boolean wasFKeyDown = false;
  private static long longPressStartTick = 0;
  @SubscribeEvent
  public static void onKeyInput(ClientTickEvent.Post event) {
    Minecraft mc = Minecraft.getInstance();
    Player player = mc.player;

    if (player == null) return;

    boolean isFKeyDown = KeyMappingRegistry.F_KEY.get().isDown();
    if (isFKeyDown && !wasFKeyDown) {
      player.sendSystemMessage(Component.literal("按下F键"));
      longPressStartTick = System.currentTimeMillis();
    } else if (isFKeyDown) {

      player.sendSystemMessage(Component.literal("已按下"));
    } else if (!isFKeyDown && wasFKeyDown) {
      int time = (int) (System.currentTimeMillis() - longPressStartTick);
      player.sendSystemMessage(Component.literal("已松开F键，持续时间：" + time + "ms"));
    }
    wasFKeyDown = isFKeyDown;

    boolean isInGenshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
    PlayerCharactersAttachment charactersAttachment =
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);

    if (KeyMappingRegistry.R_KEY.get().consumeClick()) {
      NetworkManager.wishEventToServer();
    }

    if (KeyMappingRegistry.G_KEY.get().consumeClick()) {
      boolean newMode = !isInGenshinMode;
      NetworkManager.setGenshinModeToServer(newMode);
      player.sendSystemMessage(Component.literal(newMode ? "已进入原神模式" : "已退出原神模式"));
    }

    if (!isInGenshinMode) {
      KeyMappingRegistry.V_KEY.get().consumeClick();
      KeyMappingRegistry.X_KEY.get().consumeClick();
      KeyMappingRegistry.C_KEY.get().consumeClick();
      KeyMappingRegistry.O_KEY.get().consumeClick();
      return;
    }

    if (KeyMappingRegistry.V_KEY.get().consumeClick()) {
      switchToNextAvailableCharacter(player, charactersAttachment);
    }
    if (KeyMappingRegistry.X_KEY.get().consumeClick()) {
      NetworkManager.triggerCharacterSkill(1);
      triggerCharacterSkill(player, 1);
    }
    if (KeyMappingRegistry.C_KEY.get().consumeClick()) {
      NetworkManager.triggerCharacterSkill(2);
    }
    if (KeyMappingRegistry.O_KEY.get().consumeClick()) {
      GUIServerHelperGIM.openCharacterPartyScreen(player);
    }
  }

  private static void switchToNextAvailableCharacter(Player player, PlayerCharactersAttachment attachment) {
    int currentIndex = attachment.getCurrentCharacterIndex();
    for (int i = 1; i <= 4; i++) {
      int nextIndex = (currentIndex + i) % 4;
      PGCharacterData character = attachment.getPartyCharacter(nextIndex);
      if (character != null && character.getCurrentHP() > 0) {
        attachment.setCurrentCharacterIndex(nextIndex);
        NetworkManager.setCharacterSelectionToServer(nextIndex);
        PGCharacterData def = character;
        player.sendSystemMessage(
                Component.literal("已切换到角色 " + def.getDefinition().getName().getString()));
        return;
      }
    }
  }
  private static void triggerCharacterSkill(Player player, int skill) {
    PlayerCharactersAttachment attachment =
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    PGCharacterData currentChar = attachment.getCurrentCharacter();
    if (currentChar != null) {
      PGCharacterDefine def = currentChar.getDefinition();
      if (def != null) {
        if (skill == 1) {
          CharacterSkillHandler.performElementalSkill(player, currentChar, def);
        } else if (skill == 2) {
          CharacterSkillHandler.performElementalBurst(player, currentChar, def);
        }
      }
    }
  }
}
