# 掉落与怪物等级

## 掉落系统负责什么

给"入侵状态下"的生物补额外掉落（原石、随机圣遗物）。**实现只有一处**：硬编码在 `EntityDeathDropHandler` 里。

> 早期还有一条「全局战利品修饰器」的路（`InvasionLootModifier` + `LTModifiers` + `data/.../loot_modifier/invasion_loot.json`）。
> 那条路**从来没生效过**——文件放在 `loot_modifier/`（单数）目录，而 NeoForge 只读 `loot_modifiers/`（复数）。
> 现已连同它压掉原版掉落的那份覆盖表一起删除；要新增掉落规则就写在 `EntityDeathDropHandler` 里。

| 类 | 位置 | 职责 |
|---|---|---|
| `EntityDeathDropHandler` | `core/system/loot/` | 监听 `LivingDropsEvent`：判定入侵状态 → 按生物类型追加掉落 |
| `ModEntityLootSubProvider` | `data/generators/` | 数据生成：实体战利品表 |
| `ModItems.getArtifactSetItemsMap()` | `content/items/ModItems` | 掉落随机圣遗物时用的候选池 |

### 掉落判定顺序

1. `LivingDropsEvent` 触发 → 客户端直接返回。
2. 取 `TeyvatWorldInvasion`，**未入侵则完全不干预**（不影响原版体验）。
3. 按击杀来源与生物类型分支：特定 Boss（凋灵、循声守卫、末影龙）给固定组合，普通怪给小额原石，另有随机圣遗物一件。
4. 生成的 `ItemStack` 先过 `ArtifactItem.initializeArtifactStackIfNeeded(stack)` 把词条补全，再塞进 `event.getDrops()`。

### 加一条掉落规则

1. 固定的 → 改数据包战利品表（或数据生成里的 provider）。
2. 有条件的（按入侵状态、按来源、按生物类别）→ 在 `EntityDeathDropHandler` 里加分支。若要新写 `LootModifier`，目录名必须是 `loot_modifiers`（复数），否则静默不加载。
3. 新物品要能作为掉落 → 确保它在 `ModItems` 注册且模型/语言齐全，物品实例化后按类型做初始化。

## 怪物等级系统负责什么

把原版怪物的等级与防御力按配置注入，并决定生成时的强弱。

| 类 | 位置 | 职责 |
|---|---|---|
| `MonsterLevelCalculator` | `core/monster/` | 等级与防御计算核心，对外提供查询 API |
| `MonsterLevelSpawnHandler` | `core/monster/` | 监听生成事件（`FinalizeSpawn`），给新生成的怪物定级 |
| `IMonsterLevel` | `mixin/interfaces/` | 注入到原版怪物上的接口（读写等级） |
| `MonsterMixin` | `mixin/mixins/` | 注入实现，并在 `minegenshin.mixins.json` 里登记 |
| 配置 | `config/` → `monster-level.toml` | 计算模式、生成模式、搜索半径等 |

### 等级从哪来

配置提供多种计算模式（按玩家等级、按距离、按固定区间等）；生成时由 `MonsterLevelSpawnHandler` 取当前世界与附近玩家状态算出等级，写入 `IMonsterLevel`，随后防御力与生命值按等级缩放。

### 加一种等级规则

1. 在 `MonsterLevelCalculator` 里加计算分支（或在配置里加模式枚举值）。
2. 需要新的配置项 → 加进对应 Config 类，客户端与服务端都要能读到。
3. 影响生成 → 调 `MonsterLevelSpawnHandler` 的调用点。
4. 别绕过注入接口直接写字段：等级存在 `IMonsterLevel` 上，其它系统通过它读取。

## 常见坑

| 现象 | 原因 |
|---|---|
| 掉落在未入侵的世界里也出现 | 漏了入侵判定；掉落逻辑应统一先过 `TeyvatWorldInvasion` |
| 掉出的圣遗物没有词条 | 忘了 `initializeArtifactStackIfNeeded` |
| 怪物等级不生效 | Mixin 没在 `minegenshin.mixins.json` 里登记（静默失效），或生成事件没走到 |
| 专用服务器报 `NoClassDefFoundError` | 注入/掉落类里引用了客户端类 |
| 改了战利品表没变化 | 数据生成产物在 `src/generated/`，需要重跑 `runData` 才会覆盖 |
