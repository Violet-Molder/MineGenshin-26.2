# 元素附着与元素反应

## 这个系统负责什么

把"这次攻击带什么元素、在目标身上留多少附着、什么时候触发反应、反应算多少伤害"这一整条链管起来。

附着入口只有一个：`ElementalAttachmentHelper.attach(宿主, ...)`。真正接了写入的载体有三种，都实现
`ElementalHost`：**生物（`EntityHost`，容器是实体附件）**、**方块（`BlockHost`，容器按坐标存在 Chunk 数据里）**、
**出战角色（`CharacterHost`，容器跟着角色走）**。物品（`ItemStack`）侧只有读取口（`StatusAccessor` 能读），
全库没有任何一处写入 —— 别按"已经支持"来规划功能。载体与两段筛查见「元素载体：可附着宿主」。

## 元素本体

| 类 | 说明 |
|---|---|
| `core/element/GenshinElement` | 元素对象：主元素、表现色、附着规则；含 `isNonPlayerLiving` 之类的过滤器 |
| `core/system/registry/register/ModElements` | 元素注册入口 |
| `core/element/ElementalsGIM` | 历史枚举，**已停用**：全库零引用。类元素（如"激元素算雷"）以 `ModElements` 注册时的 `setMainElement` 为准 |

## 附着的三层结构

```
载体（生物 / 方块）
  └── StatusContainer            ← 每载体一个，持久化 + 同步
        └── StatusInstance       ← 一条状态（附着/护盾/减益…）
              ├── StatusInstanceType（类型注册表里的类型）
              └── ElementalAttachmentInstance（附着专用：元素、量级、衰减）
```

| 类 | 位置 | 职责 |
|---|---|---|
| `StatusContainer` | `core/attachment/` | 一容器存全部状态，`tick()` 推进、序列化、同步 |
| `StatusInstance` | `core/status/` | 单条状态的生命周期（是否 `isFinished`） |
| `StatusInstanceType(s)` | `core/status/`（注册在 `core/system/registry/register/ModStatusInstanceTypes`） | 类型注册表；**目前只注册了 `elemental_attachment` 一条**，"冻结 / 眩晕"这些是计划，尚未实现 |
| `StatusAccessor` | `core/status/` | 取/改容器内容的统一入口 |
| `ElementalAttachmentInstance` | `core/system/about/` | 附着的元素、量级（`getUnit`）、衰减进度 |
| `StatusTickHandler` | `core/status/` | 每个活体实体每 tick 推进容器（`EntityTickEvent.Post`） |

附着的**主**写入点在战斗管线里（见"战斗 · 攻击与伤害管线"）：一次攻击先问衰减计数器，再由 `ElementalAttachmentHelper.attach(...)` 写进目标容器。

但写入点不止战斗管线这一处，**反应内部也会二次写附着**：冻结反应要往目标写 `FROZEN`（`builtin/FreezeReaction`），
扩散反应要把扩出去的元素写到新目标上（`builtin/SwirlReaction`）；方块侧的附着来自
`BlockElementHelper.applyElement`。这些都走同一个宿主入口，所以"统一入口"覆盖的不再只是生物侧的主路径。

## 谁能被附着：宿主与两段筛查

附着不是一个只属于生物的动作。所有载体（生物 / 方块 / 出战角色）都实现
`core/system/about/host/ElementalHost`，附着与反应只认这个接口：

```
ElementalAttachmentHelper.attach(host, element, source, profile)
   ① host.acceptsElement(...)      第一段筛查：这次附着收不收 —— 不收就到此为止（也不反应）
   ② 写进 host.container()
   ③ host.acceptsReaction(...)     第二段筛查：挂上了，但这个反应能不能出
```

两段筛查互相独立，各自回答一个以前没地方写的问题：

| 第一段：这次附着收不收 | 第二段：这个反应能不能出 | 另有第三件事：伤害吃不吃 |
|---|---|---|
| 生物：`ElementalAttachable.onAttachElement`（mixin 默认 `true`，怪物可覆盖） | 生物：`ElementalAttachable.onReactElement`（默认 `true`） | `ElementalAttachable.isImmuneToElementDamage`（默认 `false`） |
| 方块：`BlockElementRules` 规则表（没有规则命中 = 不收） | 默认全允许，需要时在自建宿主上覆盖 | **只归零伤害，不拦附着** |
| 例：大型冰史莱姆拒绝水；水方块只收冰 | 例：允许挂水但不接受冻结 → 冰水共存、不生成冻元素 | 例：冰史莱姆照常被挂冰、照样反应，只是冰元素伤害为 0 |

两条由此确立的硬规则：**拒收附着 = 不反应**；**免疫不吞附着**。
细节（宿主契约、方块的两条注册、加新载体的步骤）见「元素载体：可附着宿主」。

## 强附着与弱附着：衰减计数器

| 类 | 位置 | 职责 |
|---|---|---|
| `DecayCounterManager` | `core/system/combat/decay/` | 每实体每元素的计数器集合 |
| `DecayCounterWorker` | 同上 | 推进冷却、判定本次附着是否要用掉一次机会 |
| `DecayGroups` / `DecayGroup` | 同上 | 元素分组（哪些元素互相消耗） |
| `DecayCounterService` | 同上 | 对外查询接口 |
| `DecaySequence` | `core/system/combat/damage/` | 衰减序列本身（几次衰减、间隔多久） |

一次攻击带来的附着量要先问衰减计数器：还在冷却里就只补量级、不走新的衰减序列——这就是"强/弱附着"的手感来源。

## 反应怎么算

| 位置 | 内容 |
|---|---|
| `core/system/reaction/` | 反应基类与公共逻辑（读取两侧元素、返回倍率与结果类型） |
| `core/system/reaction/builtin/` | 具体实现：蒸发、融化、冻结、超导等 |
| `core/system/reaction/ElementalReactionType` | 反应枚举（供表现层与飘字使用） |
| `config/reaction/ReactionConfig` | 倍率配置 → 生成的 `minegenshin/reaction.toml` |

反应伤害的输入是"目标身上的附着 + 本次攻击元素 + 触发者属性"，输出是伤害修正与结果类型；**数值不要写死在实现里**，走配置。

### 入口与优先级

- 公开入口只有一个：`ElementalReactionManager.tryReactFor(host, ctx)`，由**附着入口在写完附着之后**调用
  （见「元素载体：可附着宿主」的附着链）—— 调用方不需要自己记得触发反应。
  以前是调用方各自决定调不调，于是"给怪挂冰后推进水里"这类环境路径忘了调，冻结根本不发生。
  飘字出口按宿主类型自动选：实体 → 目标头上，方块 → `DamageIndicatorFactory.reactionAtBlock`，角色宿主 → 静默。
  加新载体**不用**再补一个飘字出口。
- **反应可以声明"这次不出字"**：`ElementalReaction.showsIndicator(ctx)` 默认 `true`，
  冻结与融化在**方块宿主**上返回 `false` —— "水结冰 / 冰化水"是形态变化，不显示反应文字；
  同样的反应打在生物身上照常出字（怪被冻住、怪被火融化都有字）。
- 默认优先级顺序表只有一份：`ReactionPriorityCalculator` 的 `DEFAULT_ORDER`。
  注册反应时 `basePriority` 填非负数就按它排；填 `-1` 表示"按默认顺序现算"（目前只有月感电）。
- 执行体把反应实现返回的 `null` 当作"没发生"（不再直接在 `isReacted()` 上崩）；反应实现一律应返回 `ReactionResult`。
- 反应飘字默认都发（`showsIndicator` 默认 `true`）；`LUNAR_CHARGED` 与星烁分支
  （`StellarGlimmerBranch.isStellarGlimmer`）例外，它们由各自的伤害链出字；
  方块宿主上的冻结/融化也例外，见上一条。
- 扩散（`SwirlReaction`）以实体为中心向周围传播，**方块端（`targetEntity == null`）不参与**，直接在 `isBlocked` 返回 true。

### 超导

- 雷 + 冰，注册比 1:1（`minegenshin:superconduct`），剧变反应。
- 伤害走 `TransformativeDamage`，倍率取 `ReactionConfig.SUPERCONDUCT`（`minegenshin/reaction.toml` 的
  `reaction-superconduct`，默认 1.5），伤害元素是**冰**。
- 同一目标两次超导伤害至少隔 10 tick（0.5 秒）：冷却期内反应照常发生、元素照常消耗，只是不再结算伤害。
- 方块端没有实体，只做消耗、不出伤害。

## 一个具体例子：冰附着减速（效果载体：寒）

元素本体不挂效果，效果归**寒（`COLD`）** —— 细节见「元素载体：可附着宿主」的"附加效果子元素"一节。
两边各有一个落点：

- **玩家 / 出战角色**：`core/status/CharacterChillHandler` 每 tick 取当前出战角色，
  问 `StatusAccessor` 这个角色身上有没有冰/冻附着，有则给玩家加 `character_cryo_slow`（-15%），没有则移除 ——
  切人 → 新角色没有附着 → 减速自然移除；切回来 → 附着还在 → 减速恢复。
- **生物**：`ColdAura.tick` 每 tick 同步寒（有冰/冻就补、都没了就撤），效果由 `ColdElement` 应用
  （`cryo_slow` 修饰符 -10%；有冻则 `setNoAi(true)`）。

两侧都是**按需重算、不维护"谁被减速了"这类影子状态**，这是附着派生效果的推荐写法；
区别只在宿主 —— 角色宿主不挂寒，所以角色那条直接读附着。

## 加一种新反应 / 新附着

1. 新附着类型：在 `ModStatusInstanceTypes` 注册，实现 `StatusInstanceType`；如果要参与元素规则，写进 `ElementalAttachmentInstance` 的读取路径。
2. 新反应：在 `core/system/reaction/builtin/` 加实现 + 在 `ModElementalReactions` 注册，倍率放进 `ReactionConfig`。
3. 表现：飘字/音效按 `ElementalReactionType` 分支处理，通常只需补配置。
4. 新载体（新方块、以后可能的物品）：实现 `ElementalHost` 并复用 `ElementalAttachmentHelper.attach`。
   方块只需在 `BlockElementRules`（能不能附着）与 `BlockElementMigrations`（挂上之后变成什么）各注册一条，
   **不必改核心代码**；细节与代码示例见「元素载体：可附着宿主」。物品侧目前完全没有接入点。

## 常见坑

| 现象 | 原因 |
|---|---|
| 反应不触发 | 目标身上没有先手附着、附着已被消耗完、后手与先手的**主元素**配不上（配对只认主元素，见 `ElementalReactionManager.canElementReact`），或宿主筛查拦下（拒收附着 = 不反应）。注意附加效果（减速 / 禁 AI）与反应无关：那由效果载体**寒**承担，`isNonPlayerLiving` 只决定元素钩子要不要作用到非玩家生物 |
| 元素挂上了但反应不出 | 宿主在第二段筛查（`ElementalAttachable.onReactElement`）拒绝了该反应；被拒的只是这一个反应，先手元素会保留成共存 |
| 附着立刻消失 | 写进容器后没走 `setData` 回写（容器是数据对象，改完要回写；方块侧是 `BlockElementStore.commit`） |
| 客户端看不到附着特效 | 容器没同步，或同步时机早于写入 |
| 挂上元素却零伤害 | 目标免疫该元素伤害。免疫只归零伤害，附着与反应照常发生 —— 想连附着一起拒掉必须用 `onAttachElement` |
| 方块不响应元素 | 三条依次检查：① 这次攻击没过 `ElementalAttackSweep` 的门禁（非原神模式 / 物理系角色 / 不在服务端）；② 方块在 `BlockElementRules` 里没登记过（不匹配即不收）；③ 方块不在这一招的攻击距离内。物品侧则完全没有接入点 |
| 反应伤害异常放大 | 倍率既写在实现里又写在配置里，两处叠加 |
