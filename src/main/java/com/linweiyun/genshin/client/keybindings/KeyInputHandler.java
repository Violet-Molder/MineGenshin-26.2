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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * 客户端按键处理。
 * <p>
 * 【职责】
 * <ul>
 *   <li>把按键转成 ActionServer 的 RPC（战斗动作）</li>
 *   <li>本地立即锁移动（ClientActionLock），不依赖服务端同步</li>
 *   <li>本地延迟挥剑（等 precast 结束），让动画和伤害对齐</li>
 *   <li>前摇/执行期阻塞非连招输入（E/Q）</li>
 * </ul>
 * <p>
 * 【设计原则】
 * 客户端只做"预测"和"输入转发"，真正的伤害/位移/打断都由服务端的 ActionManager 权威处理。
 * 这里所有的锁，都会被服务端的 RPC 结果最终覆盖或修正。
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class KeyInputHandler {
  private static final Logger LOGGER = LogUtils.getLogger();

  // ============================================================
  // 按键的"上一 tick 状态"记录 —— 用于边沿检测（按下瞬间才触发一次）
  // ============================================================
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

  // ============================================================
  // 长按判定状态
  // ============================================================
  /** X 键按下时刻（毫秒） */
  private static long longPressStartTick = 0;
  /** 长按是否已触发（防止重复触发） */
  private static boolean xSkillTriggered = false;

  // ============================================================
  // 攻击键长按判定状态（用于重击）
  // ============================================================
  private static boolean wasAttackDown = false;
  private static long attackPressStartTick = 0;
  private static boolean attackChargedTriggered = false;

  // ============================================================
  // Pre —— 每 tick 开头执行
  // ============================================================
  @SubscribeEvent
  public static void onClientTickPre(ClientTickEvent.Pre event) {
    Minecraft mc = Minecraft.getInstance();
    LocalPlayer player = mc.player;
      if (player == null) return;
    // 未入侵时原神模式不生效，直接跳过
    if (!TeyvatWorldInvasion.isClientInvaded()) return;

    boolean isInGenshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
    if (isInGenshinMode) {
      // 屏蔽原版攻击键 —— 原神模式下攻击键由我们接管
      mc.options.keyAttack.consumeClick();
    }

    // 本地动作锁倒计时递减。这是唯一驱动 ClientActionLock 生命周期的地方。
    ClientActionLock.tick();

    // 到 precast 结束的那一 tick 才 swing —— 让动画和伤害对齐
    if (ClientActionLock.consumeSwingFlag()) {
      player.swing(InteractionHand.MAIN_HAND);
    }
  }

  // ============================================================
  // Post —— 每 tick 末尾执行（处理所有按键输入）
  // ============================================================
  @SubscribeEvent
  public static void onKeyInput(ClientTickEvent.Post event) {
    Minecraft mc = Minecraft.getInstance();
    Player player = mc.player;

    if (player == null) return;
    if (!TeyvatWorldInvasion.isClientInvaded()) return;

    // ========================================================
    // 拉取当前状态
    // ========================================================
    boolean isInGenshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
    PlayerCharactersAttachment charactersAttachment =
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    var character = charactersAttachment.getCurrentCharacter();

    // ========================================================
    // R：抽卡
    // ========================================================
    boolean isRDown = KeyMappingRegistry.R_KEY.get().isDown();
    if (isRDown && !wasRKeyDown) {
      NetworkManager.wishEventToServer();
    }
    wasRKeyDown = isRDown;

    // ========================================================
    // G：切换原神模式
    // ========================================================
    boolean isGDown = KeyMappingRegistry.G_KEY.get().isDown();
    if (isGDown && !wasGKeyDown) {
      boolean newMode = !isInGenshinMode;
      NetworkManager.setGenshinModeToServer(newMode);
    }
    wasGKeyDown = isGDown;

    // ========================================================
    // V：切换队伍角色
    // ========================================================
    boolean isVDown = KeyMappingRegistry.V_KEY.get().isDown();
    if (isVDown && !wasVKeyDown && isInGenshinMode) {
      switchToNextAvailableCharacter(player, charactersAttachment);
    }
    wasVKeyDown = isVDown;

    // ========================================================
    // 跳跃：立即解锁本地动作锁，并通知服务端打断当前动作
    // ========================================================
    boolean isJumpDown = mc.options.keyJump.isDown();
    if (isJumpDown && !wasJumpDown && isInGenshinMode && character != null) {
      // 本地立即解锁，玩家这一帧就能动
      ClientActionLock.clear();
      // 通知服务端打断（前摇/后摇生效，执行期被忽略）
      ActionServer.interruptActionToServer(
              com.linweiyun.genshin.core.system.combat.action.InterruptReason.JUMP.ordinal());
    }
    wasJumpDown = isJumpDown;

    // ========================================================
    // 攻击键：短按普攻 / 长按重击
    // ========================================================
    if (isInGenshinMode && character != null) {
      boolean isAttackDown = mc.options.keyAttack.isDown();

      if (isAttackDown && !wasAttackDown) {
        // ---- 按下瞬间 ----
        attackPressStartTick = System.currentTimeMillis();
        attackChargedTriggered = false;
        // 本地立即锁移动
        ClientActionLock.lockForNormalAttack(player, character);

      } else if (isAttackDown && !attackChargedTriggered) {
        // ---- 按住中：检查是否达到重击阈值 ----
        long elapsed = System.currentTimeMillis() - attackPressStartTick;
        int chargeTicks = character.getChargedAttackChargeTicks();
        int chargeMs = chargeTicks * 50;   // tick → 毫秒

        if (elapsed >= chargeMs) {
          // 触发重击
          character.performChargedAttack(player);
          ActionServer.performChargedAttackToServer();
          attackChargedTriggered = true;
          // 复位 E 键长按状态（避免跨按键串扰）
          wasXKeyDown = false;
          longPressStartTick = 0;
          xSkillTriggered = false;
          // 重击的锁时长可能和普攻不同，重新锁一次
          ClientActionLock.lockForChargedAttack(player, character);
          // 挥剑等 precast 结束 —— 不在这里立即 swing
          ClientActionLock.scheduleChargedSwing(player, character);
        }

      } else if (!isAttackDown && wasAttackDown) {
        // ---- 松开：如果没触发重击，视为普攻 ----
        if (!attackChargedTriggered) {
          ActionServer.performNormalAttackToServer(0);
          // 挥剑等 precast 结束 —— 不在这里立即 swing
          ClientActionLock.scheduleNormalSwing(player, character);
        }
        attackPressStartTick = 0;
      }
      wasAttackDown = isAttackDown;
    }

    // ========================================================
    // X 键：短按 E / 长按 E
    // 前摇/执行期直接被阻塞（isActionInputBlocked），不进入判定逻辑
    // ========================================================
    boolean isXDown = KeyMappingRegistry.X_KEY.get().isDown();
    if (isInGenshinMode && character != null
            && !ClientActionLock.isActionInputBlocked()) {

      // 情况一：角色没有"长按区分"（技能短/长按 CD 相同）→ 一律短按
      if (character.getSkillShortMaxCooldownTick() == character.getSkillLongMaxCooldownTick()) {
        if (isXDown && !wasXKeyDown) {
          ClientActionLock.lockForSkill(player, character, false);
          triggerCharacterSkill(player, -1);
        }
      }
      // 情况二：角色区分短长按
      else {
        if (isXDown && !wasXKeyDown) {
          // 按下瞬间：先用短按参数锁，然后开始计时
          ClientActionLock.lockForSkill(player, character, false);
          longPressStartTick = System.currentTimeMillis();
          xSkillTriggered = false;

        } else if (isXDown && !xSkillTriggered) {
          // 按住中：检查是否达到长按阈值（1000ms）
          int time = (int) (System.currentTimeMillis() - longPressStartTick);
          if (time >= 1000) {
            triggerCharacterSkill(player, time);
            xSkillTriggered = true;
          }

        } else if (!isXDown && wasXKeyDown) {
          // 松开：如果长按没触发过，就是短按
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

    // ========================================================
    // C：元素爆发
    // 前摇/执行期直接被阻塞
    // ========================================================
    boolean isCDown = KeyMappingRegistry.C_KEY.get().isDown();
    if (isCDown && !wasCKeyDown && isInGenshinMode
            && !ClientActionLock.isActionInputBlocked()) {
      if (character != null) {
        ClientActionLock.lockForBurst(player, character);
      }
      triggerCharacterBurst(player);
    }
    wasCKeyDown = isCDown;

    // ========================================================
    // O：打开队伍界面
    // ========================================================
    boolean isODown = KeyMappingRegistry.O_KEY.get().isDown();
    if (isODown && !wasOKeyDown) {
      GUIServerHelperGIM.openCharacterPartyScreen(player);
    }
    wasOKeyDown = isODown;

    // ========================================================
    // U：打开角色信息界面
    // ========================================================
    boolean isCharInfoDown = KeyMappingRegistry.CHARACTER_INFO_SCREEN_KEY.get().isDown();
    if (isCharInfoDown && !wasCharInfoKeyDown) {
      GUIServerHelperGIM.openArtifactEquipScreen(player, -1);
    }
    wasCharInfoKeyDown = isCharInfoDown;

    // ========================================================
    // B：打开背包
    // ========================================================
    boolean isArtifactDown = KeyMappingRegistry.ARTIFACT_EQUIP_SCREEN_KEY.get().isDown();
    if (isArtifactDown && !wasArtifactKeyDown) {
      GUIServerHelperGIM.openBackpackScreen(player);
    }
    wasArtifactKeyDown = isArtifactDown;

    // ========================================================
    // N：打开突破界面
    // ========================================================
    boolean isArtifact2Down = KeyMappingRegistry.ARTIFACT_EQUIP_SCREEN_KEY_2.get().isDown();
    if (isArtifact2Down && !wasArtifactKey2Down) {
      GUIServerHelperGIM.openAscensionScreen(player);
    }
    wasArtifactKey2Down = isArtifact2Down;

    // ========================================================
    // K：配置界面（当前只记录状态）
    // ========================================================
    boolean isConfigDown = KeyMappingRegistry.CONFIG_SCREEN_KEY.get().isDown();
    wasConfigKeyDown = isConfigDown;
  }

  // ============================================================
  // 切换到下一个存活角色
  // ============================================================
  private static void switchToNextAvailableCharacter(Player player, PlayerCharactersAttachment attachment) {
    // 复位所有长按状态
    wasXKeyDown = false;
    longPressStartTick = 0;
    xSkillTriggered = false;
    // 切人时清除本地移动锁 + 待挥剑，新角色立即能动
    ClientActionLock.clear();

    int currentIndex = attachment.getCurrentCharacterIndex();
    // 从当前角色的下一个开始找，最多找一圈
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

  // ============================================================
  // 触发 E（本地预测 + 服务端 RPC）
  // ============================================================
  private static void triggerCharacterSkill(Player player, int isLong) {
    PlayerCharactersAttachment attachment =
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
    var currentChar = attachment.getCurrentCharacter();
    // 本地立即调一次：让客户端 UI 立刻反映（比如冷却数字）
    if (currentChar != null) {
      currentChar.performElementalSkill(player, isLong);
    }
    // RPC 通知服务端权威执行
    ActionServer.triggerCharacterSkill(isLong);
  }

  // ============================================================
  // 触发 Q（本地预测 + 服务端 RPC）
  // ============================================================
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