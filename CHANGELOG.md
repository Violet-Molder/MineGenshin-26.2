# MineGenshin 更新日志

---

## v0.4.0 — 2026-09-11

### 伤害飘字系统（全新）

引入完整的屏幕空间伤害飘字系统，在实体受伤时显示浮动数字/反应名称/自定义文本。采用**服务端 → 客户端 RPC 单向下发**架构，客户端通过 LDLib2 的 `ModularHudLayer` 渲染。

#### 五种飘字类型 + 三种颜色模式

| 类型 | Style 枚举 | 用途 |
|------|-----------|------|
| NORMAL | 普通伤害数字 | 伤害飘字 |
| CRIT | 暴击伤害数字 | 暴击飘字，附加 `.crit` class |
| REACTION | 元素反应名称 | "蒸发""融化"等反应名飘字 |
| HEAL | 治疗数字 | 治疗量飘字 |
| TEXT | 纯文本 | "免疫"等自定义消息 |

| 颜色模式 | 说明 |
|----------|------|
| Default | 按元素/反应类型从 `damage-indicator.toml` 取色 |
| Custom | 传入单个十六进制颜色 |
| Gradient | 传入顶部+底部两个颜色，客户端垂直渐变 |

#### 动画系统

- **缩放**：`startScale=6.2 → baseScale=2.2`，easeOutCubic 缓出曲线（`1-(1-t)³`）
- **飞向落点**：200ms 内从攻击者与目标中间飞到目标上方落点
- **上升**：整个生命周期匀速上升 `RISE_HEIGHT=0.23` 格
- **淡出**：最后 300ms alpha 1→0
- **REACTION 额外偏移**：反应名飘字额外抬高 +0.2 Y 轴，与伤害数字错开
- **随机散布**：每次落点 ±0.275 格，避免连续命中数字重叠

#### 可配置参数（Options）

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `baseScale` | `2.2` | 稳定后基础大小 |
| `startScale` | `6.2` | 出场瞬间大小 |
| `durationMs` | `950` | 总持续时间（毫秒） |

#### 距离缩放

| 常量 | 值 | 说明 |
|------|-----|------|
| `NEAR_DISTANCE` | `4.0` | 此距离内保持原大小 |
| `FAR_DISTANCE` | `32.0` | 此距离外缩至 `FAR_SCALE` |
| `FAR_SCALE` | `0.25` | 远端缩放下限 |
| `MAX_RENDER_DISTANCE` | `128.0` | 超过不绘制 |
| `BROADCAST_RADIUS` | `48.0` | RPC 发送半径 |

#### 颜色配置

配置文件 `./config/minegenshin/damage-indicator.toml`，分两大组：

- **元素伤害颜色**（8 项）：火/冰/水/风/雷/草/岩/物理，可自定义十六进制颜色
- **元素反应颜色**（17 项）：融化、蒸发、碎冰、超导、扩散、感电、超载、燃烧、绽放、超绽放、烈绽放、原激化、超激化、蔓激化、冻结、结晶

类元素（FROZEN/DENDRO_QUICKEN/BURNING）自动归并到主元素取色。

### 文件变动

**新增**：

| 文件 | 说明 |
|------|------|
| `core/system/combat/damage/DamageIndicatorFactory.java` | 飘字工厂，20+ 便捷方法统一入口 |
| `config/DamageIndicatorConfig.java` | 元素色 + 反应色配置（8+17 项） |
| `core/network/DamageIndicatorRpc.java` | 服务端→客户端 RPC 通道 |
| `client/damage/DamageIndicator.java` | 客户端动画状态（位置/缩放/透明度） |
| `client/damage/DamageIndicatorManager.java` | 活跃飘字列表管理 |
| `client/damage/DamageIndicatorRenderer.java` | HUD 渲染（世界坐标→屏幕投影） |
| `client/damage/ClientHudRegistration.java` | ModularHudLayer 注册 |
| `resources/assets/minegenshin/lss/hud/damage_indicator.lss` | 飘字样式表（5 种 class） |

**修改**：

| 文件 | 改动 |
|------|------|
| `core/system/combat/attack/HurtEntityHelper.java` | processPipeline 中调用 `DamageIndicatorFactory.damage()` |
| `mixin/mixins/LivingEntityHurtMixin.java` | 受伤后触发飘字生成 |

---

## v0.3.0 — 2026-09-06

### 圣遗物系统（全新）

从 v0.2.0 到 v0.3.0 最大的更新是引入了完整的圣遗物系统，还原原神圣遗物的生成、升级和装备机制。

#### 配置驱动架构

圣遗物所有数值均可通过 `./config/minegenshin/artifact.toml` 修改，无需改代码。

| 配置类 | 管理内容 |
|--------|----------|
| `ArtifactConfig` | 主入口，管理共享 Builder |
| `ArtifactLevelConfig` | 3/4/5 星升级经验值 |
| `ArtifactMainStatConfig` | 主词条 base+growth（3/4/5 星，18 种属性） |
| `ArtifactMainStatConfig.mainStatWeight` | 时之沙/空之杯/理之冠主词条抽取权重 |
| `ArtifactSubStatConfig` | 副词条 4 档数值（5 星，10 种属性） |

#### 五种圣遗物类型

| 类型 | 英文 | 主词条 |
|------|------|--------|
| 生之花 | FLOWER | 固定数值生命值 |
| 死之羽 | PLUME | 固定数值攻击力 |
| 时之沙 | SANDS | 随机 HP%/ATK%/DEF%/元素精通/元素充能 |
| 空之杯 | GOBLET | 随机 HP%/ATK%/DEF%/8 种元素伤害/元素精通 |
| 理之冠 | CIRCLET | 随机 HP%/ATK%/DEF%/暴击率/暴击伤害/治疗加成/元素精通 |

#### 主词条系统

- **数值公式**：`value = base + level × growth`，从 0 级开始随等级线性增长
- **加权随机抽取**：时之沙/空之杯/理之冠的主词条按配置权重抽取（如 HP% 权重 26.68、元素精通 10.0），而非等概率
- 生之花/死之羽主词条固定，无需抽取

#### 副词条系统

- **档位机制**：10 种副词条属性 × 4 个档位，初始随机 1-4 档
- **生成流程**：从候选池不放回随机抽取 2-4 个副词条（1-2星:2个, 3星:3个, 4-5星:4个），排除与主词条冲突的属性
- **初始解锁概率**：25% 全部解锁，75% 锁定最后一个（还原原神 3 词条/4 词条掉落机制）

#### 副词条升级机制

每 4 级触发一次（4/8/12/16/20 级）：

1. 存在未解锁副词条 → 优先解锁（4 级时触发）
2. 全部已解锁 → 随机选一条升级

**升级公式**：`value = 档位基础值 × (upgradeCount + 1)`

| 档位 | 小攻击基础值 | 升级 1 次 | 升级 2 次 |
|------|-------------|-----------|-----------|
| 1 档 | 14 | 14×2 = 28 | 14×3 = 42 |
| 3 档 | 18 | 18×2 = 36 | 18×3 = 54 |

升级次数通过 `TeyvatItemStat.upgradeCount` 持久化存储。

#### 圣遗物套装

首套实装 **炽烈的炎之魔女（Crimson Witch of Flames）**，包含 5 件。

#### 圣遗物升级

- 使用原石右键圣遗物可喂 1000 经验
- 经验累积到阈值自动升级，同时更新主词条数值和触发副词条提升
- 升级经验值按星级区分（3/4/5 星各自独立配置）

#### 角色装备

- `PGCharacterData` 新增 5 个圣遗物槽位（flower/plume/sands/goblet/circlet）
- 角色信息界面（`ScreenCharacterInfo`）显示已装备圣遗物

#### 悬停文本

圣遗物物品悬浮提示显示：类型、星级（★）、等级、主词条、副词条（含锁定/解锁状态）。

### TeyvatItemStat 扩展

| 新增字段 | 用途 |
|----------|------|
| `tier` | 副词条档位（1-4），持久化存储 |
| `upgradeCount` | 升级次数，用于计算当前值 = 档位值 × (upgradeCount + 1) |
| `unlocked` | 副词条是否解锁 |

### Bug 修复

| 问题 | 修复 |
|------|------|
| 创造模式物品栏查看圣遗物崩溃 | `ArtifactItem.appendHoverText` 改用 `getOrDefault` 安全取值 |
| 原石喂圣遗物时组件为 null 崩溃 | `ItemPrimogem.use` 增加组件 null 检查 |
| 暴击率/暴击伤害/充能/治疗加成属性值显示为 0 | `ModAttributes` 修正 AttributeType ID 与注册表一致 |

### 文件变动

**新增**：

| 文件 | 说明 |
|------|------|
| `config/ArtifactConfig.java` | 圣遗物配置主入口 |
| `config/ArtifactLevelConfig.java` | 升级经验配置 |
| `config/ArtifactMainStatConfig.java` | 主词条数值 + 权重配置 |
| `config/ArtifactSubStatConfig.java` | 副词条档位配置 |
| `content/items/artifact/ArtifactType.java` | 圣遗物类型枚举 |
| `content/items/artifact/ArtifactSet.java` | 套装枚举 |
| `content/items/artifact/ArtifactStatData.java` | 属性数据管理器 |
| `content/items/artifact/ArtifactLevelData.java` | 等级数据管理 |
| `content/items/artifact/ArtifactMainStatGenerator.java` | 主词条生成器（加权随机） |
| `content/items/artifact/ArtifactSubStatGenerator.java` | 副词条生成器（档位抽取） |
| `content/items/artifact/ArtifactItem.java` | 圣遗物物品基类 |
| `content/items/component/ArtifactStatsComponent.java` | 圣遗物数据组件 |
| `content/items/artifact/FlowerArtifact.java` | 生之花 |
| `content/items/artifact/PlumeArtifact.java` | 死之羽 |
| `content/items/artifact/SandsArtifact.java` | 时之沙 |
| `content/items/artifact/GobletArtifact.java` | 空之杯 |
| `content/items/artifact/CircletArtifact.java` | 理之冠 |
| `content/items/artifact/crimson_witch/CrimsonFlower.java` | 魔女之花 |
| `content/items/artifact/crimson_witch/CrimsonPlume.java` | 魔女之羽 |
| `content/items/artifact/crimson_witch/CrimsonSands.java` | 魔女之时 |
| `content/items/artifact/crimson_witch/CrimsonGoblet.java` | 魔女之心 |
| `content/items/artifact/crimson_witch/CrimsonCirclet.java` | 魔女之帽 |

**修改**：

| 文件 | 改动 |
|------|------|
| `content/stat/TeyvatItemStat.java` | 新增 tier/upgradeCount/unlocked 字段及方法 |
| `content/items/custom/ItemPrimogem.java` | 新增圣遗物喂经验功能 |
| `core/system/registry/register/ModDataComponents.java` | 注册 ARTIFACT_STATS 组件 |
| `core/system/registry/register/ModAttributes.java` | 修正 CR/ER/CDG/HB 属性 ID |
| `core/character/PGCharacterData.java` | 新增 5 个圣遗物槽位 |
| `client/gui/screens/ScreenCharacterInfo.java` | 新增圣遗物槽位显示 |

## v0.2.0 — 2026-09-04

### 架构重构

#### 伤害管线入口迁移至实体 hurtServer

- 移除 `HurtEntityHelper.hurtEntityForPlayer` 统一入口模式
- 新增 `LivingEntityHurtMixin`，在 `LivingEntity.hurtServer` HEAD 拦截 `ModDamageSource` 伤害
- 新增 `PlayerHurtInterceptor`，在 `Player.actuallyHurt` HEAD 拦截非 MOD 伤害（原神模式下扣 PGCharacter 血）
- `PlayerAttackInterceptor` 改为调用 `target.hurtServer(level, modSource, 0f)` 触发管线
- `TeyvatLivingEntity` 子类自行重写 `hurtServer`，不走 Mixin

#### HurtEntityHelper 重构

- 重构为纯计算工具类，移除所有伤害入口逻辑
- 新入口：`calculateFinalModDamage(ModDamageSource, PGCharacter, LivingEntity)`
- 内部拆解为 `processPipeline`（衰减→附着→反应→乘区）和 `calculateFinalDamage`（暴击区→加成区→防御区→抗性区）

#### ModDamageSpec 扩展

- 新增 `attackerCharacter`（@Nullable），攻击者角色信息封装进伤害规格
- Builder 新增 `attackerCharacter(PGCharacter)` 方法

#### PGCharacter 新增受伤与倒下逻辑

- 新增 `hurt(float amount)` —— 扣角色血量，返回是否倒下
- 新增 `incapacitate(PlayerCharactersAttachment)` —— 倒下后自动切到队伍下一个角色

#### 新增 CombatMath / CombatEntityAccessor

- `CombatMath` —— 防御区、抗性区、等级系数公式集中管理
- `CombatEntityAccessor` —— 统一从 LivingEntity / PGCharacter / IMonsterLevel 读取等级、防御、抗性

### Bug 修复

| 问题 | 修复 |
|------|------|
| flatBonus 在效果处理后丢失 | 效果循环结束后刷新 `spec = damageSource.getSpec()` |
| 效果遍历 ConcurrentModificationException | 改为 `new ArrayList<>(effects)` 快照遍历 |
| IcyQuillEffect 冰凌数量不消耗 | 每次成功应用后 -1，为 0 时 `removeEffect` 清除 |
| invulnerableTime 挡住连续攻击 | LivingEntityHurtMixin 在 hurtServer HEAD 处 cancel 原版流程 |
| 伤害有造成但无标记（markHurt 缺失） | 使用 `level.broadcastDamageEvent(target, source)` 同步客户端 |

### 文件变动

**新增**：

| 文件 | 说明 |
|------|------|
| `mixin/mixins/LivingEntityHurtMixin.java` | 拦截 LivingEntity.hurtServer，ModDamageSource 伤害应用 |
| `mixin/mixins/PlayerHurtInterceptor.java` | 拦截 Player.actuallyHurt，非 MOD 伤害扣 PGCharacter 血 |
| `core/system/combat/damage/CombatMath.java` | 伤害数学工具类 |
| `core/system/combat/damage/CombatEntityAccessor.java` | 实体属性读取器 |

**修改**：

| 文件 | 改动 |
|------|------|
| `core/system/combat/attack/HurtEntityHelper.java` | 重构为纯计算工具类 |
| `core/system/combat/damage/ModDamageSpec.java` | 新增 attackerCharacter 字段 + Builder 方法 |
| `core/character/PGCharacter.java` | 新增 hurt() / incapacitate() |
| `content/effect/character/shenhe/IcyQuillEffect.java` | 冰凌消耗逻辑 + 快照遍历 |
| `content/effect/character/CharacterEffectHelper.java` | removeEffect 新增 Player 参数重载 |
| `content/entities/teyvat/monster/slime/TeyvatSlime.java` | 重写 hurtServer + dealDamage 使用 ModDamageSource |
| `mixin/MixinConfig.java` | 跳过 TeyvatLivingEntity 的 LivingEntityHurtMixin / MonsterMixin |
| `resources/minegenshin.mixins.json` | 注册 LivingEntityHurtMixin、PlayerHurtInterceptor |

---

## v0.1.0 — 2026-09-04

### 新功能

#### 怪物等级与防御系统

通过 Mixin 为所有 `Monster` 子类（排除本 Mod 自定义的 `TeyvatMonster`）注入等级与防御力属性。

- 怪物生成时等级一次性确定，永久固定
- 防御力公式：`等级 × 500 + 500`
- 无经验值、无升级功能
- 冒险等阶计算模式：`NEAREST` / `HIGHEST` / `LOWEST` / `COMPREHENSIVE`（4 种）
- 生成模式：`NATURAL` / `FIXED` / `BIAS`（3 种）

**配置文件**：`./config/minegenshin/monster-level.toml`

**新增文件**：

| 文件 | 说明 |
|------|------|
| `mixin/interfaces/IMonsterLevel.java` | Mixin 接口 |
| `mixin/mixins/MonsterMixin.java` | 注入 Monster.class |
| `config/MonsterLevelConfig.java` | 配置类 |
| `core/monster/MonsterLevelCalculator.java` | 等级计算核心 + 三个 API 入口 |
| `core/monster/MonsterLevelSpawnHandler.java` | FinalizeSpawnEvent 监听 |

**修改文件**：

| 文件 | 说明 |
|------|------|
| `Minegenshin.java` | 注册新配置 + SpawnHandler |
| `minegenshin.mixins.json` | 注册 MonsterMixin |