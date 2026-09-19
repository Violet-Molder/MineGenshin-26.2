package com.linweiyun.genshin.client.action;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.action.ActionDefinition;
import com.linweiyun.genshin.core.system.combat.action.ActionSet;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * 客户端本地动作状态。
 * <p>
 * 承担三件事：
 * <ol>
 *   <li><b>移动锁</b>：前摇 + 执行期锁 WASD。Mixin 读 {@link #isMovementBlocked()}。</li>
 *   <li><b>挥剑调度</b>：按键时不立即 swing，延迟到 precast 结束才 swing，
 *       让动画和伤害对齐。</li>
 *   <li><b>输入阻塞</b>：前摇/执行期拒绝非连招输入（E/Q），
 *       避免客户端本地执行了动作而服务端拒绝。</li>
 * </ol>
 * 全部静态、零延迟、不依赖服务端同步。
 */
public final class ClientActionLock {
    private static final Logger LOGGER = LogUtils.getLogger();
    private ClientActionLock() {}

    /** 剩余锁移动的 tick 数。> 0 表示锁住水平移动。 */
    private static int movementLockTicks = 0;

    /** 挥剑倒计时。> 0 表示正在等 precast 结束；减到 0 时置 swingFlag。 */
    private static int pendingSwingTicks = 0;

    /** 减到 0 的那一 tick 置 true，外部 consumeSwingFlag() 消费后清除。 */
    private static boolean swingFlag = false;

    // ============================================================
    // 移动锁
    // ============================================================

    /**
     * 加锁移动。
     * 取 max 而不是覆盖 —— 防止"快速连按导致锁时长反而缩短"。
     *
     * @param precastTicks 前摇 tick 数
     * @param activeTicks  执行期 tick 数（至少 1）
     */
    public static void lock(int precastTicks, int activeTicks) {
        int total = Math.max(0, precastTicks) + Math.max(1, activeTicks);
        movementLockTicks = Math.max(movementLockTicks, total);
    }

    /** 查询：Mixin 用它决定是否还原 X/Z。 */
    public static boolean isMovementBlocked() {
        return movementLockTicks > 0;
    }

    /**
     * 查询：前摇/执行期是否应拒绝新的非连招输入。
     * 后摇期间返回 false —— 允许玩家在连招窗口里继续操作。
     */
    public static boolean isActionInputBlocked() {
        return movementLockTicks > 0;
    }

    // ============================================================
    // 挥剑调度
    // ============================================================

    /**
     * 安排一次挥剑 —— 从当前时刻起等 precastTicks 后再挥。
     * 取 max 是为了处理"连续两次攻击"的情形，不让新攻击的挥剑提前。
     */
    public static void scheduleSwing(Player player, int precastTicks) {
        pendingSwingTicks = Math.max(pendingSwingTicks, Math.max(0, precastTicks));

    }
    /**
     * 每 tick 递减。由 KeyInputHandler.onClientTickPre 调用。
     * 递减到 0 时置 swingFlag。
     */
    public static void tick() {
        if (movementLockTicks > 0) movementLockTicks--;

        if (pendingSwingTicks > 0) {
            pendingSwingTicks--;
            if (pendingSwingTicks == 0) {
                swingFlag = true;
            }
        }
    }

    /** 外部每 tick 调用一次，返回 true 表示这一 tick 应该 swing。 */
    public static boolean consumeSwingFlag() {
        if (swingFlag) {
            swingFlag = false;
            return true;
        }
        return false;
    }

    // ============================================================
    // 清空
    // ============================================================

    /**
     * 立即解锁并取消挥剑。跳跃、切人时调用。
     * 前摇被打断 → 后续的 swing 也不该发生。
     */
    public static void clear() {
        movementLockTicks = 0;
        pendingSwingTicks = 0;
        swingFlag = false;
    }

    // ============================================================
    // 便捷入口 —— 从角色当前动作集里读取 precast/active 时长
    // ============================================================

    /** 普攻按下时调用。取第 0 段的时长作为锁的基准。 */
    public static void lockForNormalAttack(Player player, PGCharacter character) {
        ActionDefinition def = firstNormalAttack(player, character);
        if (def != null) lock(def.precastTicks, def.activeTicks);
    }

    /** 重击触发时调用。 */
    public static void lockForChargedAttack(Player player, PGCharacter character) {
        ActionDefinition def = chargedAttack(player, character);
        if (def != null) lock(def.precastTicks, def.activeTicks);
    }

    /** E 键按下时调用。长短按都用短按参数 —— 反正按键那一刻还不知道会不会变长按。 */
    public static void lockForSkill(Player player, PGCharacter character, boolean longPress) {
        ActionDefinition def = skillDef(player, character, longPress);
        if (def != null) lock(def.precastTicks, def.activeTicks);
    }

    /** Q 键按下时调用。 */
    public static void lockForBurst(Player player, PGCharacter character) {
        ActionDefinition def = burstDef(player, character);
        if (def != null) lock(def.precastTicks, def.activeTicks);
    }

    // ============================================================
    // 挥剑调度的便捷入口 —— 按键时直接读动作集里的 precast
    // ============================================================

    /** 普攻：安排一次在 precast 之后挥剑。 */
    public static void scheduleNormalSwing(Player player, PGCharacter character) {
        ActionDefinition def = firstNormalAttack(player, character);
        scheduleSwing(player, def != null ? def.precastTicks : 0);
    }

    /** 重击：安排一次在 precast 之后挥剑。 */
    public static void scheduleChargedSwing(Player player, PGCharacter character) {
        ActionDefinition def = chargedAttack(player, character);
        scheduleSwing(player, def != null ? def.precastTicks : 0);
    }

    // ============================================================
    // 内部：从动作集里取定义
    // ============================================================

    private static ActionDefinition firstNormalAttack(Player player, PGCharacter character) {
        ActionSet set = character.getActionSet(player);
        return set != null ? set.getNormalAttack(0) : null;
    }

    private static ActionDefinition chargedAttack(Player player, PGCharacter character) {
        ActionSet set = character.getActionSet(player);
        return set != null ? set.getChargedAttack() : null;
    }

    private static ActionDefinition skillDef(Player player, PGCharacter character, boolean longPress) {
        ActionSet set = character.getActionSet(player);
        if (set == null) return null;
        return longPress ? set.getElementalSkillHold() : set.getElementalSkillTap();
    }

    private static ActionDefinition burstDef(Player player, PGCharacter character) {
        ActionSet set = character.getActionSet(player);
        return set != null ? set.getElementalBurst() : null;
    }
}