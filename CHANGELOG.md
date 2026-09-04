# MineGenshin 更新日志

---

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
| `mixin/MixinConfig.java` | 跳过 TeyvatLivingEntity 的 LivingEntityHurtMixin / MonsterLevelMixin |
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

**配置文件**：`./config/minegenshin/monster_level.toml`

**新增文件**：

| 文件 | 说明 |
|------|------|
| `mixin/interfaces/IMonsterLevel.java` | Mixin 接口 |
| `mixin/mixins/MonsterLevelMixin.java` | 注入 Monster.class |
| `config/MonsterLevelConfig.java` | 配置类 |
| `core/monster/MonsterLevelCalculator.java` | 等级计算核心 + 三个 API 入口 |
| `core/monster/MonsterLevelSpawnHandler.java` | FinalizeSpawnEvent 监听 |

**修改文件**：

| 文件 | 说明 |
|------|------|
| `Minegenshin.java` | 注册新配置 + SpawnHandler |
| `minegenshin.mixins.json` | 注册 MonsterLevelMixin |