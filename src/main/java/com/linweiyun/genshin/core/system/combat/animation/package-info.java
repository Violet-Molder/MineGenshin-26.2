/**
 * 角色动作 / 动画系统 —— 本项目所有「角色怎么动」的代码都在这里。
 *
 * <h2>为什么和 {@code combat.action} 分开</h2>
 * {@code combat.action} 是<b>结算</b>：动作定义、ActionSet、运行时状态机、伤害时序。
 * 这里这一层是<b>表现</b>：这一帧播哪个动画、什么时候能打断、按键怎么进来、状态怎么同步给别人。
 * 两者只通过「客户端按键 → RPC → 服务端 ActionManager」这一条线相连。
 *
 * <h2>包结构</h2>
 * <pre>
 * animation/
 * ├── action/   动作编排契约：CharacterActionHandler 接口 + CharacterActions 注册表
 * │            （实现与角色登记表在客户端：client.combat.action）
 * ├── config/   动画数据：CharacterAnimations 接口 + 常态动画记录类 + 兜底配置
 * ├── animatable/ 动画对象与玩家的互查接口
 * └── server/   服务端侧：动画状态广播与到期复位兜底
 * </pre>
 *
 * <h2>数据流</h2>
 * <pre>
 * 按键（GLFW 回调，比 tick 早）
 *   → ActionStateMachine（客户端，动画唯一权威，0 延迟）
 *       ├─ PlayerAnimationController → GeckoLib 逐帧选动画
 *       └─ NetworkManager RPC → 服务端 → 同步给其他玩家
 *   → ActionServer RPC → ActionManager（服务端，伤害唯一权威）
 *       → ActionSet（由角色 Skill 从 XxxResources 构建）→ SkillBase 回调
 * </pre>
 *
 * <h2>注意</h2>
 * <b>实现全在客户端</b>：动作实现与角色登记表在 {@code client.combat.action}，
 * 运行状态机（{@code ActionStateMachine} / {@code PlayerAnimationController} /
 * {@code AnimationStateSync} / {@code AnimationAvailability}）在 {@code client.combat.state}；
 * 本包只留双端可用的契约与数据，公共侧不引用任何客户端类，专用服务器加载时不会碰到它们。
 * {@code server} 子包是纯服务端逻辑。
 */
package com.linweiyun.genshin.core.system.combat.animation;
