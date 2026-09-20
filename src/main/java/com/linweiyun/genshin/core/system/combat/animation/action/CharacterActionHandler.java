package com.linweiyun.genshin.core.system.combat.animation.action;

import net.minecraft.world.entity.player.Player;

/**
 * 一个角色的「动作编排」：给状态机喂 {@code changeState(状态名, 优先级, 总刻数, 硬直刻数)}。
 *
 * <p>移植自参考2 的同名接口，<b>去掉了格挡（block）</b> —— 本项目不带格挡。
 *
 * <p>所有方法都是 {@code default} 空实现：只做渲染、不需要动作的角色直接登记 {@link #EMPTY} 即可。
 */
public interface CharacterActionHandler {

    /** 什么都不做的动作编排：角色照常渲染，但按攻击/技能/闪避键不会有任何反应。 */
    CharacterActionHandler EMPTY = new CharacterActionHandler() {
    };

    /** 普攻（含连段推进）。 */
    default void attack(Player player) {
    }

    /** 弹反成功后的反击。 */
    default void counterAttack(Player player) {
    }

    /**
     * 战技。
     *
     * @return 有没有真的放出这个动作（例如角色没配长按变体时返回 {@code false}，
     *         状态机据此判断「长按还没成功」，下一 tick 会继续尝试而不是把这次按住吞掉）
     */
    default boolean skill(Player player, boolean longPress) {
        return false;
    }

    /** 大招。 */
    default void ultimate(Player player) {
    }

    /** 闪避。 */
    default void dodge(Player player) {
    }

    /** 蓄力过程中每 tick 调用（左键按住时）。 */
    default void tickCharge(Player player, int holdTicks) {
    }

    /** 左键松开。 */
    default void releaseAttack(Player player, int chargeTicks) {
    }

    /** 某个状态被打断/被覆盖时调用，用于清理这个状态留下的残留。 */
    default void onStateInterrupted(String interruptedState, Player player) {
    }

    /** 每客户端 tick 的被动处理，仅在当前角色是这个角色时调用（如捕获服务端的弹反成功状态）。 */
    default void passiveTick(Player player) {
    }
}
