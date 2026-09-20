/**
 * 服务端侧 —— 远端玩家看到的动作动画从哪来。
 *
 * <p>本地玩家的动画由客户端状态机自己驱动（0 延迟），服务端只负责把这些状态
 * 广播给其他玩家，并在客户端崩溃/丢包时到期复位。
 *
 * <ul>
 *   <li>{@link com.linweiyun.genshin.core.system.combat.animation.server.ServerAnimationTicker}
 *       —— 客户端上报 → 写入同步附件 → 广播给跟踪者；倒计时到期强制复位。</li>
 * </ul>
 *
 * <p>状态数据本身是 {@code core.attachment.AnimationState}（LDLib2 {@code @Persisted} 字段），
 * 挂在 {@code AttachmentRegistration.ANIMATION_STATE_ATTACHMENT} 上。
 */
package com.linweiyun.genshin.core.system.combat.animation.server;
