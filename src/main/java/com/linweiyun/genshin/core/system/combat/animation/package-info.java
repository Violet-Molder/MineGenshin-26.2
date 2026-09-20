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
 * ├── CharacterAnimationRegistry   角色登记表（加角色就在这里加一行）
 * ├── action/   动作编排：CharacterActionHandler 接口 + 注册表 + 数据驱动的通用实现
 * ├── config/   动画数据：CharacterAnimations 接口 + 常态动画记录类 + 兜底配置
 * ├── state/    运行状态：ActionStateMachine（客户端状态机）、PlayerAnimationController（逐帧选动画）、
 * │            AnimationStateSync（状态读取）、AnimationAvailability（动画存在性校验）
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
 *       → ActionSet（由角色 Talent 从 XxxResources 构建）→ TalentBase 回调
 * </pre>
 *
 * <h2>注意</h2>
 * {@code state} 与 {@code action} 里的多数类只在客户端有意义（会碰到 {@code Minecraft}），
 * 但它们被 {@code @EventBusSubscriber(Dist.CLIENT)} 和渲染路径引用，服务端不会加载；
 * {@code server} 子包是纯服务端逻辑。
 */
package com.linweiyun.genshin.core.system.combat.animation;
