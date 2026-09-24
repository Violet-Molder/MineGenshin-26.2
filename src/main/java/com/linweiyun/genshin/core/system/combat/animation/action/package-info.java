/**
 * 动作编排契约 —— 「按下某个键之后，状态机该切成什么」。
 *
 * <p>本包只放<b>双端可用</b>的两样东西：
 * <ul>
 *   <li>{@link CharacterActionHandler}
 *       —— 一个角色的动作响应接口（普攻 / 反击 / 战技 / 大招 / 闪避 / 蓄力）。</li>
 *   <li>{@link CharacterActions}
 *       —— 角色 ID → 动作编排 + 动画配置的注册表。</li>
 * </ul>
 *
 * <p><b>实现与登记在客户端</b>：数据驱动的通用实现 {@code ResourceDrivenActionHandler}
 * 与角色登记表 {@code CharacterAnimationRegistry} 都要碰 {@code net.minecraft.client}
 * （本地玩家、客户端状态机），因此放在 {@code client.combat.action}，
 * 由 {@code MinegenshinClient.onClientSetup} 完成登记；运行状态机在 {@code client.combat.state}。
 * 时序数据本身仍是双端的：{@code XxxResources.ACTION_DATA} → {@code XxxSkill} → {@code ActionSet}，
 * 客户端只负责按这份数据播动画，伤害结算在服务端。
 */
package com.linweiyun.genshin.core.system.combat.animation.action;
