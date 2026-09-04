# MineGenshin 更新日志

---

## v0.1.0 — 2026-09-04

| 版本类型 | 值 |
|---------|---|
| 内部版本 | `v0.1.0` |
| 外部版本 | —（预留，尚未发布首个公开版本） |

### ✨ 新功能

#### 怪物等级与防御系统

通过 Mixin 为 **本 Mod 以外** 的所有 `Monster` 子类注入等级与防御力属性（`TeyvatMonster` 自行实现，不受影响）。

**等级属性**：
- 怪物生成时一次性确定，永久固定（不可升级、不可修改）
- 防御力公式：`等级 × 500 + 500`
- 无经验值、无升级功能

**冒险等阶计算模式（4种，配置决定）**：

搜索范围使用 **MC 模拟距离（simulation distance）**，与自然生成机制一致，确保只有能影响到该怪物生成的玩家才参与计算。

| 模式 | 行为 |
|------|------|
| `NEAREST` | 生成点距离最近的玩家 AR → 世界等级 |
| `HIGHEST` | 模拟距离内最高 AR 玩家 → 世界等级 |
| `LOWEST` | 模拟距离内最低 AR 玩家 → 世界等级 |
| `COMPREHENSIVE` | 排除最高，取第二高，低于第二高 25 级以上排除，剩余取平均 |

**三个 API 入口（代码生成时调用）**：

| API | 场景 | 等级来源 |
|-----|------|---------|
| `getMonsterLevelNatural(level, pos, seed)` | 野外自然刷新 | 配置文件的四计算模式 |
| `getMonsterLevelFixed(level)` | 副本固定等级/Boss | 调用方硬编码 |
| `getMonsterLevelWithBias(level, pos, seed, bias)` | 副本指定等级段范围 | 玩家 AR + 调用方指定的偏移 |

**世界等级映射表**：

| 冒险等阶 | 世界等级 | 怪物等级 |
|---------|---------|---------|
| 01 ~ 20 | 0 | 1 ~ 10 |
| 20 ~ 25 | 1 | 11 ~ 21 |
| 25 ~ 30 | 2 | 21 ~ 40 |
| 30 ~ 35 | 3 | 41 ~ 50 |
| 35 ~ 40 | 4 | 51 ~ 60 |
| 40 ~ 45 | 5 | 61 ~ 70 |
| 45 ~ 50 | 6 | 71 ~ 80 |
| 50 ~ 55 | 7 | 81 ~ 89 |
| 55 ~ 58 | 8 | 90 ~ 91 |
| 58 ~ 60 | 9 | 92 ~ 103 |

> 加权随机：同一世界等级范围内，AR 越高越容易刷出高等级怪物。

**配置文件**：`./config/minegenshin/monster_level.toml`
```toml
[monsterSpawnLogic.spawnLevelCalculation]
calculationMode = "NEAREST"
```

**新增文件**：

| 文件 | 说明 |
|------|------|
| `mixin/interfaces/IMonsterLevel.java` | Mixin 接口 |
| `mixin/mixins/MonsterLevelMixin.java` | 注入 `Monster.class` |
| `config/MonsterLevelConfig.java` | 配置类（仅 CalculationMode） |
| `core/monster/MonsterLevelCalculator.java` | 等级计算核心 + 三个 API 入口 |
| `core/monster/MonsterLevelSpawnHandler.java` | `FinalizeSpawnEvent` 监听（自然生成） |

**修改文件**：

| 文件 | 说明 |
|------|------|
| `Minegenshin.java` | 注册新配置 + SpawnHandler |
| `minegenshin.mixins.json` | 注册 `MonsterLevelMixin` |
| `README.md` | 新增 4.12 章节 |