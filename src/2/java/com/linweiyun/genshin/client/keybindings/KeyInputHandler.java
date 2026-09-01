package com.linweiyun.genshin.client.keybindings;

import com.linweiyun.genshin.client.gui.screens.GUIServerHelperGIM;
import com.linweiyun.genshin.content.entities.attachments.AttachmentRegistration;
import com.linweiyun.genshin.content.entities.attachments.attachment.CharacterParty;
import com.linweiyun.genshin.content.entities.attachments.attachment.PlayerGenshinModeAttachment;
import com.linweiyun.genshin.content.items.character.player_character.PlayerCharacter;
import com.linweiyun.genshin.core.system.combat.CombatHelper;
import com.linweiyun.genshin.core.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class KeyInputHandler {

  //长按检测
  private static boolean wasFKeyDown = false;

  @SubscribeEvent
  public static void onKeyInput(ClientTickEvent.Post event) {
    Minecraft mc = Minecraft.getInstance();
    Player player = mc.player;

    if (player == null) return;

    //长按检测部分

    boolean isKeyDown = KeyMappingRegistry.F_KEY.isDown();
    // 刚按下
    if (isKeyDown && !wasFKeyDown) {
      // 按下瞬间的逻辑（类似开始举盾）
      player.sendSystemMessage(Component.literal("按下F键"));
    }
    // 持续按住中
    else if (isKeyDown) {
      player.sendSystemMessage(Component.literal("已按下"));
    }
    // 刚松开
    else if (!isKeyDown && wasFKeyDown) {
      // 松开瞬间的逻辑（类似放下盾牌）
      player.sendSystemMessage(Component.literal("已松开F键"));
    }

    wasFKeyDown = isKeyDown;
    PlayerGenshinModeAttachment genshinModeAttachment =
        player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT.get());
    CharacterParty characterParty =
        player.getData(AttachmentRegistration.CHARACTER_PARTY_ATTACHMENT.get());
    if (KeyMappingRegistry.R_KEY.consumeClick()) {
      NetworkManager.wishEventToServer();
    }
    boolean isInGenshinMode = genshinModeAttachment.isGenshinMode();
    if (KeyMappingRegistry.G_KEY.consumeClick()) {
      genshinModeAttachment.setClientGenshinMode(!isInGenshinMode);
      if (!isInGenshinMode) {
        player.sendSystemMessage(Component.literal("已进入原神模式"));
      } else {
        player.sendSystemMessage(Component.literal("已退出原神模式"));
      }
    }
    if (!isInGenshinMode) {
      KeyMappingRegistry.V_KEY.consumeClick();
      KeyMappingRegistry.X_KEY.consumeClick();
      KeyMappingRegistry.C_KEY.consumeClick();
      KeyMappingRegistry.O_KEY.consumeClick();
      return;
    }
    if (genshinModeAttachment.isGenshinMode()) {
      if (KeyMappingRegistry.V_KEY.consumeClick()) {
        switchToNextAvailableCharacter(player, characterParty);
      }
      if (KeyMappingRegistry.X_KEY.consumeClick()) {
        NetworkManager.triggerCharacterSkill(1);
        triggerCharacterSkill(player, characterParty, 1);
      }
      if (KeyMappingRegistry.C_KEY.consumeClick()) {
        NetworkManager.triggerCharacterSkill(2);
        triggerCharacterSkill(player, characterParty, 2);
      }
      if (KeyMappingRegistry.O_KEY.consumeClick()) {
        GUIServerHelperGIM.openCharacterPartyScreen(player);
        //        TutorialScreenThreePointTwo.openScreen();

      }
    }
  }

  private static void switchToNextAvailableCharacter(Player player, CharacterParty party) {
    int nextAliveCharacterIndex = CombatHelper.findNextAliveCharacterIndex(party);
    ItemStack nextCharacter = party.getStackInSlot(nextAliveCharacterIndex);
    party.setClientCurrentCharacter(nextAliveCharacterIndex + 1);
    player.sendSystemMessage(
        Component.literal("已切换到角色 " + nextCharacter.getHoverName().getString()));
  }

  private static void triggerCharacterSkill(
      Player player, CharacterParty characterParty, int skill) {

    int characterId = characterParty.getCurrentCharacterSlot();
    ItemStack characterItem = characterParty.getStackInSlot(characterId - 1);
    if (!characterItem.isEmpty()
        && characterItem.getItem() instanceof PlayerCharacter playerCharacter) {
      if (skill == 1) {
        playerCharacter.performElementalSkill(player);
      } else if (skill == 2) {
        playerCharacter.performElementalBurst(player);
      }
    }
  }
}
