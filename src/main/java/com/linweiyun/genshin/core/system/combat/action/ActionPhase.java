package com.linweiyun.genshin.core.system.combat.action;

/**
 * 动作阶段。生命周期：PRECAST → ACTIVE → POSTCAST → IDLE
 * <ul>
 *     <li>PRECAST（前摇）：可被跳跃/击退/切人打断。主动移动不打断，但锁移动。
 *         被打断则 ACTIVE 不执行。</li>
 *     <li>ACTIVE（执行期）：不可打断。伤害、突进、生成领域都在这里。</li>
 *     <li>POSTCAST（后摇）：收尾动画期，同时是连招窗口。
 *         后摇期间按键 → 缓冲下一段；后摇结束未按键 → 连招重置。</li>
 * </ul>
 */
public enum ActionPhase {
    IDLE,
    PRECAST,
    ACTIVE,
    POSTCAST
}