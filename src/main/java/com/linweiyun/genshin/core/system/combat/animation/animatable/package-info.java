/**
 * 动画对象层 —— {@link com.linweiyun.genshin.core.system.combat.animation.animatable.IPlayerAnimatableProxy}：
 * 让 GeckoLib 的动画对象能反查回玩家实体。
 *
 * <p>动画控制器需要知道「这一帧在给谁播动画」，而不是每帧去遍历世界。
 * 实现放在渲染层（{@code client.render.character.GenshinReplacedPlayer}）。
 */
package com.linweiyun.genshin.core.system.combat.animation.animatable;
