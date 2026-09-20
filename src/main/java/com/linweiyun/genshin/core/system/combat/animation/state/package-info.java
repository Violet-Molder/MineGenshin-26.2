/**
 * 运行状态层 —— 「这一帧到底播什么」。
 *
 * <ul>
 *   <li>{@link com.linweiyun.genshin.core.system.combat.animation.state.ActionStateMachine}
 *       —— 客户端动作状态机，只服务本地玩家。三计时器（动画总时长 / 前摇硬直 / 定身）
 *       + 优先级 + 连击 + 延迟任务 + 收尾动画排队。</li>
 *   <li>{@link com.linweiyun.genshin.core.system.combat.animation.state.PlayerAnimationController}
 *       —— GeckoLib 控制器：逐帧决定播哪个动画，本地读状态机、远端读同步附件。</li>
 *   <li>{@link com.linweiyun.genshin.core.system.combat.animation.state.AnimationStateSync}
 *       —— 状态读取入口与远端音效补播。</li>
 *   <li>{@link com.linweiyun.genshin.core.system.combat.animation.state.AnimationAvailability}
 *       —— 动画存在性校验，防止把不存在的动画名交给 GeckoLib（会变成原始姿态）。</li>
 * </ul>
 */
package com.linweiyun.genshin.core.system.combat.animation.state;
