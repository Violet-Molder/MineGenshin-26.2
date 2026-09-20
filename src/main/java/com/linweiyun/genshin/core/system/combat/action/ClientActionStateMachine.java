package com.linweiyun.genshin.core.system.combat.action;

import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.ActionStep;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端动作状态机。
 * <p>
 * 管理角色当前动画状态，处理优先级/打断/连击窗口/移动锁定。
 * 架构参考 ClientActionStateMachine（参考2），整合了原来的 ClientActionLock 功能。
 * <p>
 * 每客户端 tick 调用 {@link #tick()}.
 */
public final class ClientActionStateMachine {

    private ClientActionStateMachine() {}

    // ==================== 状态 ====================

    /** 当前播放的特殊动画名（attack_1, skill, burst...），null/"default" 表示常态 */
    private static String currentAnim = null;

    /** 当前动作优先级 */
    private static int currentPriority = 0;

    /** 动画剩余 tick */
    private static int animTicks = 0;

    /** 保护期剩余 tick（期间不可被打断） */
    private static int protectTicks = 0;

    /** 连击窗口剩余 tick（结束后可接下一段） */
    private static int comboWindowTicks = 0;

    /** 当前连段数（1-based） */
    private static int comboStage = 0;

    /** 最大连段数 */
    private static int maxCombo = 1;

    /** 移动锁定剩余 tick */
    private static int moveLockTicks = 0;

    /** 当前动作类型 */
    private static ActionKind currentKind = null;

    /** 当前 step 配置引用 */
    private static ActionStep currentStep = null;

    // ==================== tick ====================

    public static void tick() {
        if (animTicks > 0) {
            animTicks--;
            if (animTicks <= 0) {
                onAnimFinished();
            }
        }
        if (protectTicks > 0) protectTicks--;
        if (comboWindowTicks > 0) comboWindowTicks--;
        if (moveLockTicks > 0) moveLockTicks--;
    }

    private static void onAnimFinished() {
        if (currentStep != null && currentStep.comboWindow > 0) {
            comboWindowTicks = currentStep.comboWindow;
        }
    }

    // ==================== 请求动作 ====================

    /**
     * 请求进入一个新动作。返回 true 表示接受。
     */
    public static boolean request(String anim, ActionStep step, ActionKind kind) {
        int reqPriority = step != null ? step.priority : 0;
        if (!canInterrupt(reqPriority)) return false;
        changeState(anim, step, kind);
        return true;
    }

    /**
     * 强制切换状态（无视打断规则）。
     * 用于服务端推送的动画同步（"syncAnimationRPCPacket" → ActionClient → 这里）。
     */
    public static void forceRequest(String anim, ActionStep step, ActionKind kind) {
        changeState(anim, step, kind);
    }

    /**
     * 仅传动画名强制切换（用于服务端 RPC 推送，无 step 信息）。
     */
    public static void forceRequest(String animationName) {
        if (animationName != null && !animationName.isEmpty() && !"default".equals(animationName)) {
            currentAnim = animationName;
            currentKind = null;
            currentStep = null;
            currentPriority = 99;
            animTicks = 30;
            protectTicks = 15;
            moveLockTicks = 15;
            comboWindowTicks = 0;
        }
    }

    private static void changeState(String anim, ActionStep step, ActionKind kind) {
        currentAnim = anim;
        currentKind = kind;
        currentStep = step;

        if (step != null) {
            currentPriority = step.priority;
            animTicks = step.duration;
            protectTicks = step.protectDuration;
            moveLockTicks = step.protectDuration;
            comboWindowTicks = 0;
        }

        // 普攻连段管理
        if (kind == ActionKind.NORMAL_ATTACK) {
            comboStage++;
            if (maxCombo > 0 && comboStage > maxCombo) comboStage = 1;
        } else if (kind != null) {
            comboStage = 0;
        }
    }

    /**
     * 判断请求的优先级是否允许打断当前动作。
     */
    private static boolean canInterrupt(int requestPriority) {
        if (currentAnim == null || "default".equals(currentAnim)) return true;
        if (protectTicks > 0) return false;
        return requestPriority > currentPriority;
    }

    // ==================== 连段管理 ====================

    public static void setComboData(int maxCombo) {
        ClientActionStateMachine.maxCombo = maxCombo;
        if (comboStage > maxCombo) comboStage = 0;
    }

    public static int comboStage() { return comboStage; }

    public static void resetCombo() { comboStage = 0; }

    // ==================== 查询 ====================

    /** 当前动画名，仅在动画计时器存活时返回，否则返回 null 表示常态 */
    public static String currentAnimation() {
        return isInSpecialAnim() ? currentAnim : null;
    }

    /** 是否在特殊动画中（非常态） */
    public static boolean isInSpecialAnim() {
        return currentAnim != null && !"default".equals(currentAnim) && animTicks > 0;
    }

    /** 是否处于连击窗口 */
    public static boolean isInComboWindow() { return comboWindowTicks > 0; }

    /** 是否被移动锁定 */
    public static boolean isMovementLocked() { return moveLockTicks > 0; }

    /** 是否被输入阻塞 */
    public static boolean isInputBlocked() { return moveLockTicks > 0; }

    /** 是否在大招中（不可移动/打断） */
    public static boolean isInBurst() { return currentKind == ActionKind.ELEMENTAL_BURST && animTicks > 0; }

    // ==================== 重置 ====================

    public static void clear() {
        currentAnim = null;
        currentPriority = 0;
        animTicks = 0;
        protectTicks = 0;
        comboWindowTicks = 0;
        moveLockTicks = 0;
        comboStage = 0;
        maxCombo = 1;
        currentKind = null;
        currentStep = null;
    }
}