/**
 * 动画数据层 —— 「这个角色有哪些动画、叫什么名字」。
 *
 * <ul>
 *   <li>{@link com.linweiyun.genshin.core.system.combat.animation.config.CharacterAnimations}
 *       —— 一个角色的动画配置接口：常态动画、动作动画名单、过渡刻数、状态→音效表。</li>
 *   <li>{@link com.linweiyun.genshin.core.system.combat.animation.config.LocomotionAnims}
 *       —— 常态动画名（站 / 走 / 跑 / 蹲 / 睡 / 爬 / 游 / 跳）。</li>
 *   <li>{@link com.linweiyun.genshin.core.system.combat.animation.config.DefaultCharacterAnimations}
 *       —— 没写动画系统的角色用的兜底配置。</li>
 * </ul>
 *
 * <p>角色的具体实现放在各自的角色包里（如
 * {@code core.character.sword.vesna.VesnaAnimations}），和 {@code XxxResources} 放一起。
 */
package com.linweiyun.genshin.core.system.combat.animation.config;
