/**
 * 客户端动作编排 —— 按键按下之后「播哪个动画、什么时候上锁、什么时候把请求发给服务端」。
 *
 * <ul>
 *   <li>{@link CharacterAnimationRegistry} —— 角色登记表（<b>加角色就在这里加一行</b>），
 *       由 {@code MinegenshinClient.onClientSetup} 调用；某角色没登记就整套动作静默失效。</li>
 *   <li>{@link ResourceDrivenActionHandler} —— 所有角色共用的数据驱动实现，
 *       时序取自角色的 {@code ActionSet}（{@code XxxResources.ACTION_DATA} → {@code XxxSkill}），
 *       不硬编码在客户端。</li>
 *   <li>{@link ActionCastGuard} —— 播动画之前的出手校验与失败提示限流。</li>
 *   <li>{@link ActionSoundScheduler} —— 把 {@code ActionStep} 的音效编排排期成本地播放任务。</li>
 * </ul>
 *
 * <p>契约（{@code CharacterActionHandler} / {@code CharacterActions}）留在公共侧
 * {@code core.system.combat.animation.action}，运行状态机在 {@link com.linweiyun.genshin.client.combat.state}：
 * 本包只做「按键 → 客户端表现 + 服务端请求」这一段，伤害结算仍然只在服务端。
 */
package com.linweiyun.genshin.client.combat.action;
