# 护盾与状态

## 护盾系统

护盾在本项目里是一套完整的"配置 + 结算 + 表现"链路，不是简单的伤害减免。

| 类 | 职责 |
|---|---|
| `ShieldService` | 护盾服务：创建、吸收伤害、到期结算 |
| `ShieldProfile` / `ShieldProfiles` | 护盾配置：吸收量、持续时间、元素、形状等，`Profiles` 是内置配置集合 |
| `ShieldElement` | 护盾的元素属性（决定对哪些伤害更有效） |
| `ShieldShape` | 形状（球形/柱形/跟随角色等） |
| `ShieldState` | 运行期状态（当前吸收量、剩余时间） |
| `ShieldBreakType` | 破碎类型（到期消失 / 被打破 / 主动解除） |
| `ShieldEffect` | 护盾附带的效果 |
| `ShieldTickHandler` | 每 tick 推进（`EntityTickEvent.Post`） |

### 一次护盾伤害的结算顺序

```
攻击进入战斗管线
  → ShieldService 先看目标身上有没有生效中的护盾
      ├─ 有：按 ShieldElement 与伤害元素的关系修正吸收量
      │      ├─ 吸收后仍有剩余 → 只扣护盾，血量不变（飘字可走护盾专属样式）
      │      └─ 护盾被打破 → 按 ShieldBreakType 走破碎流程，剩余伤害继续
      └─ 无：走正常扣血
```

护盾在**伤害管线的前段**参与（见"战斗 · 攻击与伤害管线"的顺序图），因此不要在任何效果里自行扣护盾。

### 加一个护盾

1. 在 `ShieldProfiles` 里加一份配置（吸收量、时长、元素、形状）。
2. 触发点调用 `ShieldService` 的创建入口（技能、效果或命令里）。
3. 需要专属表现 → 在 HUD/渲染侧按 `ShieldState` 画；需要专属音效 → 在破碎类型分支里挂。
4. 数值不要写死在服务里，放配置以便调参。

## 状态系统

状态是"附着/减益/增益"的统一容器（护盾、附着、眩晕都住在里面）。

| 类 | 位置 | 职责 |
|---|---|---|
| `StatusContainer` | `core/attachment/` | 每实体的状态容器，`tick()` 推进、序列化、同步 |
| `StatusInstance` | `core/status/` | 单条状态：剩余时间、是否 `isFinished`、序列化 |
| `StatusInstanceType` / `StatusInstanceTypes` | `core/status/` | 状态类型契约与注册表 |
| `StatusAccessor` | `core/status/` | 读改容器的统一入口（不要直接摸 `getAll()`） |
| `StatusTickHandler` | `core/status/` | 每 tick 推进活体实体的容器 |
| `ModStatusDataComponents` / `ModStatusInstanceTypes` | `core/system/registry/register/` | 注册入口 |

### 一条状态的生命周期

1. 创建：由技能、效果、附着或护盾创建 `StatusInstance`（或其子类型）。
2. 写入：`StatusAccessor` 写进容器的 `StatusContainer`，**写完必须 `setData` 回写**实体数据。
3. 推进：`StatusTickHandler` 每 tick `tick()`，到期或条件满足则 `isFinished`。
4. 清理：容器移除已完成实例；附带的属性修饰器/效果要在这里一并撤销。
5. 同步：容器随实体数据同步到客户端（客户端表现依赖它）。

### 加一条状态

1. 类型：在 `ModStatusInstanceTypes` 注册，实现 `StatusInstanceType`（含序列化）。
2. 行为：继承 `StatusInstance`，在 `tick()` 里推进逻辑，在 `onAdded` / `onRemoved` 里处理修饰器与表现。
3. 联动：需要改属性 → 加/删 `AttributeModifier`；需要打伤害 → 走战斗管线。
4. 配置：数值尽量走配置或数据类型参数，便于调参。

## 常见坑

| 现象 | 原因 |
|---|---|
| 护盾吸收了但飘字不正确 | 表现层按"最终扣血量"生成飘字，护盾吸收部分需要单独标记 |
| 状态不消失 | `tick()` 里没把 `isFinished` 置位，或容器没被推进（实体的 `CONTAINER` 附件没注册） |
| 客户端没有状态图标 | 容器没同步，或 HUD 只监听了部分类型 |
| 护盾在切人后错位 | 护盾挂在玩家而不是角色，或 `ShieldShape` 与跟随目标不匹配 |
| 属性修饰器残留 | 状态被移除时没撤销修饰器（用唯一 `Identifier` 便于精确移除） |
