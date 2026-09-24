# 属性与角色效果

## 属性系统

玩家与角色身上的所有数值（HP/ATK/DEF、元素增伤、护盾强效、暴击等）统一走属性容器，而不是散落的字段。

| 类 | 位置 | 职责 |
|---|---|---|
| `AttributeType` | `content/attribute/` | 属性类型定义（含 `@Persisted` 字段，供持久化解析识别） |
| `AttributeContainer` | `content/attribute/` | 一个实体/角色的属性集合，提供取值与叠加 |
| `AttributeInstance` | `content/attribute/` | 单条属性：基础值 + 修饰器列表 |
| `ModAttributes` | `core/system/registry/register/` | 自定义属性注册（元素增伤、护盾强效等） |
| `content/stat/TeyvatItemStat` | `content/stat/` | 物品（武器/圣遗物）提供的属性词条 |
| `config/entity/EntityAttributeCapConfig` | `config/entity/` | 属性上限放宽（默认 1_000_000，可整体关闭） |

修饰器分两类维度：**固定 / 百分比**，**临时 / 永久**。临时修饰器（如冰附着减速）在效果结束时必须移除，否则会出现"切人以后还减速"这类残留。

### 加一条属性来源

1. 词条型来源（武器/圣遗物）→ 在对应数据里加词条，经 `TeyvatItemStat` 汇聚到容器。
2. 临时来源（元素、护盾、状态）→ 用 `AttributeModifier` + 唯一 `Identifier`，效果结束时 `removeModifier`。
3. 上限不够用 → 调 `EntityAttributeCapConfig`，不要改原版属性表。

## 角色效果系统

效果 = 挂在角色上、按 tick 回调的一段逻辑（Buff/Debuff/套装效果/技能状态）。

| 类 | 位置 | 职责 |
|---|---|---|
| `ICharacterEffect` | `content/effect/character/interfaces/` | 效果契约：`onEffectTick` / `onEffectBackTick` / `onEffectFrontTick` |
| `CharacterEffectInstance` | `content/effect/character/` | 一条效果实例（含 `DummyEffect` 占位） |
| `CharacterEffectHelper` | `content/effect/character/` | 增删查效果的统一入口 |
| `CharacterEffectHandler` | `content/effect/character/` | 每 tick 驱动（`PlayerTickEvent.Post`，只在入侵状态） |
| `content/effect/character/impl/` | — | 通用效果实现 |
| `content/effect/character/artifact/` | — | 圣遗物套装效果 |
| `content/effect/character/<角色>/` | — | 角色专属效果（如 `shenhe/IcyQuillEffect`、`vodyanitsa/Antiphon`） |
| `content/effect/mob/` | — | 怪物效果（如 `StunMobEffect`，配合 `ModMobEffects`） |

驱动顺序（每 tick，对每个队伍角色）：

```
复制一份效果列表（避免并发修改）
  → onEffectBackTick  （后台：结算、计时）
  → onEffectFrontTick （前台：表现为前提的逻辑）
  → onEffectTick      （返回 false 表示结束 → CharacterEffectHelper.removeEffect）
```

三个回调的语义来自"前台/后台角色"：队伍里非出战的角色也不是完全不 tick，需要区分"只有在场才生效"的逻辑。

### 加一个角色效果

1. 在 `content/effect/character/impl/`（通用）或角色包（专属）里实现 `ICharacterEffect`。
2. 挂载用 `CharacterEffectHelper.addEffect(...)`，移除用 `removeEffect`。
3. 需要改属性 → 在效果生效/失效时增删修饰器，不要直接改基础值。
4. 需要参与伤害 → 在效果的 tick 或伤害管线钩子里改 `ModDamageSpec`，不要在效果里自己造伤害。
5. 套装效果放 `artifact/`，并在圣遗物套装定义里声明件数条件。

## 常见坑

| 现象 | 原因 |
|---|---|
| 效果不结束 | `onEffectTick` 一直返回 `true`，或没走 `Helper.removeEffect` |
| 切换角色后效果残留 | 用了玩家级状态而不是角色级；修饰器没在失效时移除 |
| 属性越叠越高 | 每次 tick 都加修饰器却没做幂等（同一 `Identifier` 会覆盖，不同 IID 会叠加） |
| 效果在未入侵时也生效 | 驱动入口有入侵守卫，但效果自己被别处 tick 了 |
| 数值改了没生效 | 修饰器是百分比还是固定值选错了维度 |
