package com.linweiyun.genshin.client.action;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.action.ActionDefinition;
import com.linweiyun.genshin.core.system.combat.action.ActionSet;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端本地动作锁 —— 保证"按键当 tick 就锁移动"。
 * <p>
 * 【为什么需要它】
 * 服务端权威的 ActionManager 只在服务端跑。客户端要想知道"现在有没有动作在执行"，
 * 得等服务端把 phase 同步过来 —— 这至少有 1 tick 延迟。前摇往往只有 1~5 tick，
 * 这 1 tick 延迟会让玩家在最开始能动一下，手感很差。
 * <p>
 * 【解决方案】
 * 客户端按键时立即自己在本地记一个倒计时（precast + active），不依赖任何网络同步。
 * 每 client tick 递减，减到 0 自动解锁。跳跃时 clear() 立即解锁。
 * <p>
 * 单一实例，全静态。Mixin 通过 {@link #isMovementBlocked()} 读取。
 */
public final class ClientActionLock {

    private ClientActionLock() {}

    /** 剩余锁移动的 tick 数。> 0 表示当前应当锁住水平移动。 */
    private static int movementLockTicks = 0;

    /**
     * 加锁。
     * <p>
     * 取 max 而不是覆盖：如果正在锁的剩余时间比新动作需要的还长，保留原值，
     * 避免"快速连按导致锁时长反而缩短"的问题。
     *
     * @param precastTicks 前摇 tick 数
     * @param activeTicks  执行期 tick 数（至少 1）
     */
    public static void lock(int precastTicks, int activeTicks) {
        int total = Math.max(0, precastTicks) + Math.max(1, activeTicks);
        movementLockTicks = Math.max(movementLockTicks, total);
    }

    /** 每 client tick 递减。由 KeyInputHandler.onClientTickPre 调用。 */
    public static void tick() {
        if (movementLockTicks > 0) movementLockTicks--;
    }

    /** Mixin 查询入口。 */
    public static boolean isMovementBlocked() {
        return movementLockTicks > 0;
    }

    /** 立即解锁。跳跃、切人时调用，让玩家的移动不再被锁。 */
    public static void clear() {
        movementLockTicks = 0;
    }

    // ============================================================
    // 便捷入口 —— 从角色当前动作集里读取 precast/active 时长
    // ============================================================
    // 说明：这里用 getActionSet(player) 而不是硬编码时长，
    //      因为角色可能有多套动作集（状态 key 不同），
    //      当前状态下的实际时长由当前动作集决定。

    /** 普攻按下时调用。取第 0 段（第一段）的时长作为锁的基准。 */
    public static void lockForNormalAttack(Player player, PGCharacter character) {
        ActionSet set = character.getActionSet(player);
        if (set == null) return;
        ActionDefinition def = set.getNormalAttack(0);
        if (def != null) lock(def.precastTicks, def.activeTicks);
    }

    /** 重击触发时调用。 */
    public static void lockForChargedAttack(Player player, PGCharacter character) {
        ActionSet set = character.getActionSet(player);
        if (set == null) return;
        ActionDefinition def = set.getChargedAttack();
        if (def != null) lock(def.precastTicks, def.activeTicks);
    }

    /**
     * E 键按下时调用。
     * <p>
     * 按键那一刻还不知道玩家会不会变成长按，所以先用短按的参数锁 ——
     * 长短按的 precast 通常差不多，够用。长按真的触发时不需要重新锁。
     */
    public static void lockForSkill(Player player, PGCharacter character, boolean longPress) {
        ActionSet set = character.getActionSet(player);
        if (set == null) return;
        ActionDefinition def = longPress ? set.getElementalSkillHold() : set.getElementalSkillTap();
        if (def != null) lock(def.precastTicks, def.activeTicks);
    }

    /** Q 键按下时调用。 */
    public static void lockForBurst(Player player, PGCharacter character) {
        ActionSet set = character.getActionSet(player);
        if (set == null) return;
        ActionDefinition def = set.getElementalBurst();
        if (def != null) lock(def.precastTicks, def.activeTicks);
    }
}