package com.linweiyun.genshin.client.keybindings;

import com.linweiyun.genshin.client.action.ClientActionLock;
import com.linweiyun.genshin.render.gui.screens.GUIServerHelperGIM;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.network.ActionServer;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.world.TeyvatWorldInvasion;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

@EventBusSubscriber(value = Dist.CLIENT)
public class KeyInputHandler {
  private static final Logger LOGGER = LogUtils.getLogger();

  private static boolean wasRKeyDown = false;
  private static boolean wasGKeyDown = false;
  private static boolean wasVKeyDown = false;
  private static boolean wasXKeyDown = false;
  private static boolean wasCKeyDown = false;
  private static boolean wasOKeyDown = false;
  private static boolean wasCharInfoKeyDown = false;
  private static boolean wasArtifactKeyDown = false;
  private static boolean wasArtifactKey2Down = false;
  private static boolean wasConfigKeyDown = false;
  private static boolean wasJumpDown = false;

  private static long longPressStartTick = 0;
  private static boolean xSkillTriggered = false;

  private static boolean wasAttackDown = false;
  private static long attackPressStartTick = 0;
  private static boolean attackChargedTriggered = false;

  @SubscribeEvent
  public static void onClientTickPre(ClientTickEvent.Pre event) {
    Minecraft mc = Minecraft.getInstance();
    LocalPlayer player = mc.player;
    if (player == null) return;
    if (!TeyvatWorldInvasion.isClientInvaded()) return;

    boolean isInGenshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
    if (isInGenshinMode) {
      mc.options.keyAttack.consumeClick();
    }

    ClientActionLock.tick();
  }

  @SubscribeEvent
  public static void onKeyInput(ClientTickEvent.Post event) {
    Minecraft mc = Minecraft.getInstance();
    Player player = mc.player;

    if (player == null) return;
    if (!TeyvatWorldInvasion.isClientInvaded()) return;

    boolean isInGenshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
    PlayerCharactersAttachment charactersAttachment =
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    var character = charactersAttachment.getCurrentCharacter();

    boolean isRDown = KeyMappingRegistry.R_KEY.get().isDown();
    if (isRDown && !wasRKeyDown) {
      NetworkManager.wishEventToServer();
    }
    wasRKeyDown = isRDown;

    boolean isGDown = KeyMappingRegistry.G_KEY.get().isDown();
    if (isGDown && !wasGKeyDown) {
      NetworkManager.setGenshinModeToServer(!isInGenshinMode);
    }
    wasGKeyDown = isGDown;

    boolean isVDown = KeyMappingRegistry.V_KEY.get().isDown();
    if (isVDown && !wasVKeyDown && isInGenshinMode) {
      switchToNextAvailableCharacter(player, charactersAttachment);
    }
    wasVKeyDown = isVDown;

    boolean isJumpDown = mc.options.keyJump.isDown();
    if (isJumpDown && !wasJumpDown && isInGenshinMode && character != null) {
      ClientActionLock.clear();
      ActionServer.interruptActionToServer(
              com.linweiyun.genshin.core.system.combat.action.InterruptReason.JUMP.ordinal());
    }
    wasJumpDown = isJumpDown;

    // 攻击键
    if (isInGenshinMode && character != null) {
      boolean isAttackDown = mc.options.keyAttack.isDown();

      if (isAttackDown && !wasAttackDown) {
        if (!ClientActionLock.isActionInputBlocked()) {
          attackPressStartTick = System.currentTimeMillis();
          attackChargedTriggered = false;
          ClientActionLock.lockForNormalAttack(player, character);
        } else {
          attackPressStartTick = 0;
        }

      } else if (isAttackDown && !attackChargedTriggered) {
        if (attackPressStartTick != 0) {
          long elapsed = System.currentTimeMillis() - attackPressStartTick;
          int chargeTicks = character.getChargedAttackChargeTicks();
          int chargeMs = chargeTicks * 50;
          if (elapsed >= chargeMs) {
            ActionServer.performChargedAttackToServer();
            attackChargedTriggered = true;
            wasXKeyDown = false;
            longPressStartTick = 0;
            xSkillTriggered = false;
            ClientActionLock.lockForChargedAttack(player, character);
          }
        }

      } else if (!isAttackDown && wasAttackDown) {
        if (!attackChargedTriggered && attackPressStartTick != 0) {
          ActionServer.performNormalAttackToServer(0);
        }
        attackPressStartTick = 0;
      }
      wasAttackDown = isAttackDown;
    }

    // X：E
    boolean isXDown = KeyMappingRegistry.X_KEY.get().isDown();
    if (isInGenshinMode && character != null
            && !ClientActionLock.isActionInputBlocked()) {

      if (character.getSkillShortMaxCooldownTick() == character.getSkillLongMaxCooldownTick()) {
        if (isXDown && !wasXKeyDown) {
          ClientActionLock.lockForSkill(player, character, false);
          triggerCharacterSkill(player, -1);
        }
      } else {
        if (isXDown && !wasXKeyDown) {
          ClientActionLock.lockForSkill(player, character, false);
          longPressStartTick = System.currentTimeMillis();
          xSkillTriggered = false;
        } else if (isXDown && !xSkillTriggered) {
          int time = (int) (System.currentTimeMillis() - longPressStartTick);
          if (time >= 1000) {
            triggerCharacterSkill(player, time);
            xSkillTriggered = true;
          }
        } else if (!isXDown && wasXKeyDown) {
          if (!xSkillTriggered) {
            int time = (int) (System.currentTimeMillis() - longPressStartTick);
            if (time < 1000) {
              triggerCharacterSkill(player, time);
            }
          }
          longPressStartTick = 0;
        }
      }
    }
    wasXKeyDown = isXDown;

    // C：Q
    boolean isCDown = KeyMappingRegistry.C_KEY.get().isDown();
    if (isCDown && !wasCKeyDown && isInGenshinMode
            && !ClientActionLock.isActionInputBlocked()) {
      if (character != null) {
        ClientActionLock.lockForBurst(player, character);
      }
      triggerCharacterBurst(player);
    }
    wasCKeyDown = isCDown;

    boolean isODown = KeyMappingRegistry.O_KEY.get().isDown();
    if (isODown && !wasOKeyDown) {
      GUIServerHelperGIM.openCharacterPartyScreen(player);
    }
    wasOKeyDown = isODown;

    boolean isCharInfoDown = KeyMappingRegistry.CHARACTER_INFO_SCREEN_KEY.get().isDown();
    if (isCharInfoDown && !wasCharInfoKeyDown) {
      GUIServerHelperGIM.openArtifactEquipScreen(player, -1);
    }
    wasCharInfoKeyDown = isCharInfoDown;

    boolean isArtifactDown = KeyMappingRegistry.ARTIFACT_EQUIP_SCREEN_KEY.get().isDown();
    if (isArtifactDown && !wasArtifactKeyDown) {
      GUIServerHelperGIM.openBackpackScreen(player);
    }
    wasArtifactKeyDown = isArtifactDown;

    boolean isArtifact2Down = KeyMappingRegistry.ARTIFACT_EQUIP_SCREEN_KEY_2.get().isDown();
    if (isArtifact2Down && !wasArtifactKey2Down) {
      GUIServerHelperGIM.openAscensionScreen(player);
    }
    wasArtifactKey2Down = isArtifact2Down;

    boolean isConfigDown = KeyMappingRegistry.CONFIG_SCREEN_KEY.get().isDown();
    wasConfigKeyDown = isConfigDown;
  }

  private static void switchToNextAvailableCharacter(Player player, PlayerCharactersAttachment attachment) {
    wasXKeyDown = false;
    longPressStartTick = 0;
    xSkillTriggered = false;
    ClientActionLock.clear();

    int currentIndex = attachment.getCurrentCharacterIndex();
    for (int i = 1; i <= 4; i++) {
      int nextIndex = (currentIndex + i) % 4;
      PGCharacter character = attachment.getPartyCharacter(nextIndex);
      if (character != null && character.getData().getCurrentHP() > 0) {
        attachment.setCurrentCharacterIndex(nextIndex);
        NetworkManager.setCharacterSelectionToServer(nextIndex);
        player.sendSystemMessage(
                Component.translatable("key.minegenshin.switched_character",
                        character.getName().getString()));
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
    ActionServer.triggerCharacterSkill(isLong);
  }

  private static void triggerCharacterBurst(Player player) {
    PlayerCharactersAttachment attachment =
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    PGCharacter currentChar = attachment.getCurrentCharacter();
    if (currentChar != null) {
      currentChar.performElementalBurst(player);
    }
    ActionServer.triggerCharacterBurst();
  }
}