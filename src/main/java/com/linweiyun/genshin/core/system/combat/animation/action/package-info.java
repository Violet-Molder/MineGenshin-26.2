/**
 * 动作编排层 —— 「按下某个键之后，状态机该切成什么」。
 *
 * <ul>
 *   <li>{@link com.linweiyun.genshin.core.system.combat.animation.action.CharacterActionHandler}
 *       —— 一个角色的动作响应接口（普攻 / 反击 / 战技 / 大招 / 闪避 / 蓄力）。</li>
 *   <li>{@link com.linweiyun.genshin.core.system.combat.animation.action.CharacterActions}
 *       —— 角色 ID → 动作编排 + 动画配置的注册表。</li>
 *   <li>{@link com.linweiyun.genshin.core.system.combat.animation.action.ResourceDrivenActionHandler}
 *       —— 本项目所有角色共用的实现：时序取自角色的 {@code ActionSet}
 *       （{@code XxxResources.ACTION_DATA} → {@code XxxSkill}），不硬编码在客户端。</li>
 * </ul>
 */
package com.linweiyun.genshin.core.system.combat.animation.action;
