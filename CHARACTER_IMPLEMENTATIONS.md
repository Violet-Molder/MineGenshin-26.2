# 薇斯娜 & 沃雅妮莎 · 完整实现详解

> MineGenshin 26.2 · NeoForge 26.2.0.88 · Java 25
> 本文逐项解释两位五星角色的完整实现，覆盖：普通攻击、重击、元素战技、元素爆发、突破天赋（只突破1和突破4）、常驻接口天赋、命座、专武特效。
> 每个效果都会明确标注**实现机制**（字段 / CharacterEffect / 接口 getter / DamageSpec / 属性临时修饰符）。
> 参考文档：`CHARACTER_SYSTEM.md`（框架层解释）。

---

## 目录

- [第一部分 · 薇斯娜（Vesna）](#第一部分--薇斯娜vesna)
  - [1.1 基本信息](#11-基本信息)
  - [1.2 普通攻击：六段连击](#12-普通攻击六段连击)
  - [1.3 重击](#13-重击)
  - [1.4 元素战技：操典·制胜有道](#14-元素战技操典制胜有道)
  - [1.5 翔风剑：三阶递进机制](#15-翔风剑三阶递进机制)
  - [1.6 元素爆发：致礼·献予女皇陛下](#16-元素爆发致礼献予女皇陛下)
  - [1.7 突破天赋 1：仪典·春之行列 → 大权](#17-突破天赋-1仪典春之行列--大权)
  - [1.8 突破天赋 4：仪典·冬之凯风 → 队伍元素加成](#18-突破天赋-4仪典冬之凯风--队伍元素加成)
  - [1.9 常驻接口天赋：星扩散户口](#19-常驻接口天赋星扩散户口)
  - [1.10 命座](#110-命座)
  - [1.11 整肃：实现机制详解](#111-整肃实现机制详解)
  - [1.12 变移窗口：如何拦截原本的技能](#112-变移窗口如何拦截原本的技能)
- [第二部分 · 沃雅妮莎（Vodyanitsa）](#第二部分--沃雅妮莎vodyanitsa)
  - [2.1 基本信息](#21-基本信息)
  - [2.2 普通攻击：水色咏叹四段](#22-普通攻击水色咏叹四段)
  - [2.3 重击](#23-重击)
  - [2.4 元素战技：宣叙·晨声纷流](#24-元素战技宣叙晨声纷流)
  - [2.5 遥久之歌：持续定时效果](#25-遥久之歌持续定时效果)
  - [2.6 元素爆发：终奏·伴尔沉沦](#26-元素爆发终奏伴尔沉沦)
  - [2.7 突破天赋 1：流荡风旋](#27-突破天赋-1流荡风旋)
  - [2.8 突破天赋 4：领唱 / 重唱（≥ 突破 4）](#28-突破天赋-4领唱--重唱-突破-4)
  - [2.9 命座](#29-命座)
  - [2.10 所有效果实现机制汇总](#210-所有效果实现机制汇总)
- [第三部分 · 漩流颂歌（WhirlflowHymn）专武](#第三部分--漩流颂歌whirlflowhymn专武)
  - [3.1 武器面板](#31-武器面板)
  - [3.2 武器被动 ①：治疗加成 +4%](#32-武器被动--治疗加成-4)
  - [3.3 武器被动 ②：告真的蜜酿](#33-武器被动--告真的蜜酿)
  - [3.4 武器被动 ③：冻结/星扩散 1.75× 强化窗口](#34-武器被动--冻结星扩散-175-强化窗口)
  - [3.5 所有效果实现机制汇总](#35-所有效果实现机制汇总)

---

# 第一部分 · 薇斯娜（Vesna）

## 1.1 基本信息

| 项目 | 值 |
|---|---|
| ID | `vesna` |
| UID | `115001` |
| 武器类型 | 单手剑（SwordCharacter） |
| 元素 | 风（Anemo） |
| 突破属性 | 暴击率（CR） |
| 战技冷却 | 18 秒 |
| 大招冷却 | 15 秒 / 能量 60 |

**核心机制**：所有技能围绕「巡风列装」模式 —— 进入模式后连续释放不同阶级的「翔风剑」，消耗「剑气」能量槽。

### 角色状态字段一览（全部是 `@DescSynced @Persisted` 字段）

| 字段 | 类型 | 用途 |
|---|---|---|
| `vesnaEnergy` / `vesnaMaxEnergy` | float | 剑气槽（初始 0 / 上限 18） |
| `windriderActive` | boolean | 是否处于巡风列装模式 |
| `windriderRemainingTicks` | int | 模式剩余刻数 |
| `xiangfengJianLevel` | int | 下一次翔风剑的阶级（0/1/2 = 一/二/三阶） |
| `lv3UsesInWindrider` | int | 本次模式已释放三阶翔风剑次数 |
| `decreeTicks[6]` | int[] | 六层「整肃」独立倒计时（每层 400 刻） |
| `bianyiWindowEndTick` | long | 满命「变移」窗口的**绝对到期时刻**（双端用本地游戏时间比） |
| `entryCast` | boolean（**非持久化**） | 这一刀是不是入门刀（`onCastStart` 钉死，伤害点只读它） |

## 1.2 普通攻击：六段连击

### 动作时序

**数据来源**：`VesnaResources.ACTION_DATA`，每段都是独立的 `ActionStep`，由 `CharacterActionDefinition` 按 combo stage 选择。

| 段 | 动画名 | 总时长 | 伤害点 delay | 位移 Move(distance, power) | 能量回复 |
|---|---|---|---|---|---|
| 1 | `attack_1` | 40 刻 | 3 | (0, 1.2) | 2 |
| 2 | `attack_2` | 48 刻 | 3 | (0, 1.2) | 2 |
| 3 | `attack_3` | 25 刻 | 3, 6 | (0, 1.3) | 3 |
| 4 | `attack_4` | 30 刻 | 10, 12, 14, 16 | (2, 1.5) | 4 |
| 5 | `attack_5` | 50 刻 | 2, 4, 6, 8 | (4, 1.8) | 5 |
| 6 | `attack_2` | 48 刻 | 5, 10, 15, 20 | (6, 2.0) | 6 |

### 伤害结算

**入口**：`VesnaTalent.attack(Player, PGCharacter, int stage)`

**倍率来源**：**TOML 配置文件**（`config/minegenshin/character.toml`），不走 `VesnaTalent` 硬编码表：

```java
// VesnaTalent.java:246-248
float multiplier = (float) (
    ShenheTalentConfig.getNABase(stage)           // TOML: nab1 = 0.433
    + ShenheTalentConfig.getNAPerLevel(stage)     // TOML: nap1 = 0.0482
    * (naLevel - 1));
```

**索敌**：`AreaEntityCollector(level, startPos, endPos, 1.0f)` —— 沿视线方向 2.5 格、半径 1.0 的线段。

**DamageSpec 构造**（VesnaTalent.java:258-262）：
```java
ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ModElements.ANEMO.get())
    .multiplier(multiplier)                      // 倍率区
    .elementAmount(AttachmentType.ULTRA_STRONG.getInitialAmount())
    .attackerCharacter(character)
    .build();
```
**没有**大权加成（大权只给灵剑段和大招），**没有**星扩散转化。

### 巡风列装模式下的额外效果

同样在 `attack()` 里，基础伤害结算完之后（stage 循环外面）：

```java
// VesnaTalent.java:282-293
if (!vesna.isWindriderActive()) return;          // 模式外完全没有

int bellCount = getBellCountForStage(stage);    // 各段不同（3段产2个、6段产3个）
for (int i = 0; i < bellCount; i++) {
    VesnaAttackProjectile projectile = VesnaAttackProjectile.create(level, vesna, player.position(), skillLevel);
    level.addFreshEntity(projectile);
    vesna.addEnergy(1);                           // 每个风铃回 1 点剑气
}
```

**风铃是实体**，倍率在 `VesnaAttackProjectile` 内部读 `VesnaTalent.WIND_BELL_DAMAGE`（Lv1 10.4% → Lv15 24.7%），衰减组别独立为 `VESNA_WIND_BELL_DECAY`。

## 1.3 重击

**入口**：`VesnaTalent.chargeAttack(Player, PGCharacter)`

**索敌**：`TargetSeeker(player, 10.0, LINE_OF_SIGHT)` —— 10 格锁最近目标。

**DamageSpec 构造**（VesnaTalent.java:324-327）—— **强制走星扩散转化**：
```java
ModDamageSpec spec = ModDamageSpec.stellarDirect(
    ElementalReactionType.STELLAR_SWIRL_ICE,    // 文案设定：重击按星扩散转化处理
    ModElements.CYRO.get(), 1.0f, 0.5f);         // 倍率仍由 TOML 配置
spec.setStellarContributors(List.of(character));
```

**巡风列装模式额外效果**：产 2 个风铃弹射物（同普攻的追踪实体）。

## 1.4 元素战技：操典·制胜有道

### 触发即生效（`onCastStart`）

**入口**：`VesnaTalent.onCastStart(Player, PGCharacter, ActionKind)`

E 键按下的那一刻（早于伤害点），只做两件事：

```java
// VesnaTalent.java:382-396（非模式时）
vesna.clearDecree();             // 整肃清空（仅入门刀清，翔风剑不清）
vesna.activateWindriderMode();   // 设置 windriderActive=true、剑气=12、阶级=0
vesna.markEntryCast();          // 钉死：这一刀是入门刀（非持久化字段）
```

**为什么不能在伤害点再判断？** 模式在 `onCastStart` 里就开了（比伤害点早 6 刻）。如果在伤害点才读 `isWindriderActive()`，就会把入门刀误判成翔风剑 —— 扣剑气、打错倍率、推进阶级。所以必须用 `entryCast` 在触发那一刻钉死。

### 入门刀伤害

**入口**：`dealWindriderEnterDamage()` —— 伤害点到来时读 `consumeEntryCast()`：
- 返回 `true` → 入门刀：结算 2.5 格 AoE 风伤，倍率走 `SKILL_DAMAGE` 表
- 返回 `false` → 翔风剑：走下面的翔风剑分支

### 冷却时机

**入口**：`Vesna.applyElementalSkillCooldown(Player, int)` —— 覆写基类的 E 冷却方法：

```java
// Vesna.java:571-575
if (entryCast) {
    getData().setElementalSkillCooldownTick(18 * 20);    // 只有入门刀进 CD
    syncRealtimeState();
}
```

模式内翔风剑不转 E 的 CD（消费剑气循环到顶自动退出）。

## 1.5 翔风剑：三阶递进机制

### 阶级推进逻辑

**入口**：`VesnaTalent.elementalSkill()` → `advanceAfterCast(Vesna, castLevel)`

```
当前阶级 0 → 打一阶 → setXiangfengJianLevel(1)
当前阶级 1 → 打二阶 → setXiangfengJianLevel(2)
当前阶级 2 → 打三阶 → lv3UsesInWindrider++
    达到 maxLv3UsesInWindrider()（0命3次 / C1 4次）→ exitWindriderMode()
    否则保持阶级 2，继续消耗剑气
```

### 各阶级伤害构成（DamageSpec 构造）

**一阶（castLevel=0）**：单段 AoE 风伤
```java
// VesnaTalent.java:529-533
dealAoeAnemoDamage(player, vesna, center, aoeRange,
    mult = at(XFJ_LV1, skillLevel),       // Lv1 40% → Lv15 95%
    AttachmentType.WEAK, false);            // 无星扩散转化，无大权加成
```

**二阶（castLevel=1）**：两段
1. 主伤害：`XFJ_LV2_MAIN`（Lv1 60% → Lv15 142.5%），AoE 风伤
2. 灵剑：`XFJ_LV2_SWORD`（Lv1 112% → Lv15 266%），通过 `spawnSpiritSword()` 生成 `VesnaSpiritSwordEntity`
   ```java
   spawnSpiritSword(player, vesna, center, aoeRange, swordMult,
       stellarSwirl, 0f, vesna.getSovereigntyBonus());  // 吃大权加成 ✔
   ```

**三阶（castLevel=2）**：五段
1. 四段连续 AoE 风伤（循环 4 次），倍率 `XFJ_LV3_SWORD`（Lv1 44.8% × 4 → Lv15 106.4% × 4）。**第一段有元素附着，后续三段 0 附着但仍吃大权**
2. 收尾灵剑：倍率 `XFJ_LV3_FINAL`（Lv1 156.8% → Lv15 372.4%），生成实体射向目标。**吃大权加成**

### 大权加成是怎么塞进 DamageSpec 的？

**入口**：`VesnaTalent.dealAoeAnemoDamage()` 和 `spawnSpiritSword()` 的最后一个参数 `sovereigntyBonus`：

```java
// VesnaTalent.java:690（普通风伤路径）
spec = ModDamageSpec.builder(attackType, ModElements.ANEMO.get())
    .multiplier(multiplier).elementAmount(elementAmount).attackerCharacter(character).build()
    .withSovereignty(sovereigntyBonus);        // ← 塞进 spec

// VesnaTalent.java:676-682（星扩散路径）
spec = ModDamageSpec.stellarDirect(STELLAR_SWIRL_WIND, ...)
    .withStellarBaseBonusMult(StellarGlimmer.swirlBaseBonusMult(level))
    .withSovereignty(sovereigntyBonus);        // ← 也塞进 spec
```

**不是** effect、**不是** getter。是**在构造 DamageSpec 的那一刻**，把 `vesna.getSovereigntyBonus()`（= 整肃层数 × 0.10）直接写进去。结算链读到 `spec.sovereignty` 就加乘。

### 剑气消耗

```java
// VesnaTalent.java:421-426
if (vesna.lv3CastIsFreeNow()) {          // C1: 本次进入模式以来第一次三阶，免剑气
    // 什么都不做
} else {
    vesna.consumeEnergy(Vesna.SPECIAL_SKILL_ENERGY_COST);  // 扣 6 点
}
```

判据（`Vesna.java:333-337`）：
```java
public boolean lv3CastIsFreeNow() {
    return hasConstellation(1)
        && xiangfengJianLevel >= 2      // 已经到三阶了
        && lv3UsesInWindrider == 0;     // 本次模式还没放过三阶
}
```

### 三阶翔风剑的突进时序

**入口**：`VesnaTalent.buildActionSet()`（覆写基类的 ActionSet 构建）

三阶的 `ActionStep.dashStartDelay = 8` —— 前 8 刻是「起跳 → 人消失 → 化作细长螺旋」的变身动画，第 9 刻才开始冻结并突进。执行期也跟着延后：

```java
// VesnaTalent.java:208-215
int dashDelay = threeStage ? THREE_STAGE_DASH_DELAY : 0;
int protect = threeStage
    ? Math.min(oldTap.duration, dashDelay + lastHitDelay(oldTap) + 2)  // 变身期+伤害点+余量
    : oldTap.protectDuration;
```

**三阶特化的 ActionStep** 只换了 `skillAnim`（换成 `heavy_3` 动画名）、`dashStartDelay`、`protectDuration`，其余字段照抄原版。

## 1.6 元素爆发：致礼·献予女皇陛下

**入口**：`VesnaTalent.elementalBurst(Player, PGCharacter)`

### 落点同步

```java
// VesnaTalent.java:608
Vec3 landing = BurstLanding.consume(player);
if (landing == null) landing = player.position();   // 兜底：包丢了按脚下打
```

客户端在**开始下坠那一刻**（跃起 8 刻 + 下坠 10 刻 = 第 18 刻）锁好落点发给服务端。伤害与落地同一帧结算。

### DamageSpec 构造

```java
// VesnaTalent.java:624-632
float sovereignty = vesna.getSovereigntyBonus();    // 先取，叠层在伤害之后
vesna.addEnergy(6f);                                 // 大招自带 6 剑气，模式外也生效

dealAoeAnemoDamage(player, vesna, landing, BURST_RADIUS,   // 12 格范围
    burstMultiplier = at(BURST_SWORD_DAMAGE, burstLevel),  // Lv1 263.2% → Lv15 625.1%
    AttachmentType.STRONG, stellarSwirl,
    AttackType.ELEMENTAL_BURST, DecayGroups.DEFAULT_ELEMENTAL_BURST,
    sovereignty);                                          // 大权加成也给大招 AoE ✔

vesna.grantDecree();   // 伤害**之后**叠层（这一层自己吃不到）
```

### 大招的自愈机制

**问题**：旧存档里大招冷却是 10 秒 / 能量 80，代码已改成 15 秒 / 60，但数据持久化了。

**解法**：在 getter 里做自愈 —— 发现不一致就强制改回去：

```java
// Vesna.java:532-536
@Override
public int getBurstMaxCooldownTick() {
    if (getData().getBurstMaxCooldownTick() != BURST_COOLDOWN_TICKS) {
        getData().setBurstMaxCooldownTick(BURST_COOLDOWN_TICKS);
    }
    return BURST_COOLDOWN_TICKS;
}
```

## 1.7 突破天赋 1：仪典·春之行列 → 大权

**突破等级门槛**：`getData().getAscensionPhase() >= 1`

**效果**：整肃层数 × 10% → 大权区加成

**实现方式**：**纯字段 + getter**，**不是 effect**

### 整肃叠层

**入口**：`Vesna.grantDecree()` —— 在翔风剑的伤害结算前调（`VesnaTalent.java:418`），大招在伤害后调（`VesnaTalent.java:636`）。

```java
// Vesna.java:139-157
public void grantDecree() {
    int slot = 0; int lowest = Integer.MAX_VALUE;
    for (int i = 0; i < decreeTicks.length; i++) {
        if (decreeTicks[i] <= 0) { slot = i; lowest = 0; break; }  // 有空位就用空位
        if (decreeTicks[i] < lowest) { lowest = decreeTicks[i]; slot = i; }  // 满了挤掉最早那层
    }
    decreeTicks[slot] = DECREE_DURATION_TICKS;    // 400 刻 = 20 秒
    syncSkillState();
}
```

### 整肃衰减

**入口**：`Vesna.tickDecree()` —— 在 `Vesna.tick()` 每刻调：

```java
// Vesna.java:171-181
private void tickDecree() {
    boolean changed = false;
    for (int i = 0; i < decreeTicks.length; i++) {
        if (decreeTicks[i] > 0 && --decreeTicks[i] == 0) changed = true;
    }
    if (changed) syncSkillState();     // 整包同步客户端
}
```

### 整肃清空

- **进入巡风列装时**：`vesna.clearDecree()`（VesnaTalent.java:392，只清一次）
- **退场（切角色）时**：每刻 tick 检查 `currentCharacter.getCharacterUUID() != vesna.getCharacterUUID()` 就清（Vesna.java:613-615）
- **翔风剑不清** —— 只叠层（grantDecree 在伤害前调）

### 大权加成读取

**入口**：`Vesna.getSovereigntyBonus()` —— **getter 即时计算**：
```java
// Vesna.java:190-192
@Override
public float getSovereigntyBonus() {
    return decreeStacks() * DECREE_BONUS_PER_STACK;   // DECREE_BONUS_PER_STACK = 0.10f
}
```

`decreeStacks()` 就是数 `decreeTicks[]` 里 > 0 的格子。

**谁在读这个 getter？** `VesnaTalent.castXiangFengJian()` 和 `elementalBurst()` 在**构造 DamageSpec 的那一刻**读出来，塞进 `spec.withSovereignty()`。结算链读到 spec 里的 `sovereignty` 就乘进大权区。

### 整肃的字段同步机制

`decreeTicks` 是 `@DescSynced @Persisted`，但客户端处理增量同步的回调需要 `ownerPlayer`，而 Vesna 在客户端没有绑 ownerPlayer → `syncRealtimeState()` 直接返回。

所以依赖**整包同步**路径：`PGCharacterData.markDirty()` → `CharacterTickEvent` → 整包发给客户端。

`syncSkillState()` 同时调这两个（Vesna.java:404-407），**只在事件型的改状态方法里调**，不要在每刻 tick 里调，避免每刻整包同步。

## 1.8 突破天赋 4：仪典·冬之凯风 → 队伍元素加成

**突破等级门槛**：`getData().getAscensionPhase() >= 4 && StellarGlimmer.hasSwirl(this)`

**效果**：按队伍元素构成给自己加临时属性修饰符。

**实现方式**：**每刻 tick 里动态加/删临时属性修饰符**，**不是 effect**，**不是 getter**

### 加属性的时刻

**入口**：`Vesna.updateA4Bonuses(Player)` —— 在 `Vesna.tick()` 每刻调：

```java
// Vesna.java:262-303
private void updateA4Bonuses(Player player) {
    boolean active = getData().getAscensionPhase() >= A4_ASCENSION
            && StellarGlimmer.hasSwirl(this);          // 判据：在星扩散状态

    if (!active) {
        // 退出星扩散时只移除自己那两个来源，不碰别人的
        getData().removeAttributeModifier(ATK, "vesna_a4_atk");
        getData().removeAttributeModifier(ELEMENTAL_MASTERY, "vesna_a4_em");
        return;
    }

    // 遍历队伍 4 个角色，数冰/风角色和其他角色
    for (int i = 0; i < 4; i++) {
        var member = attachment.getPartyCharacter(i);
        var element = member.getElemental();
        if (element.getId().equals("anemo") || element.getId().equals("cryo")) windOrIce++;
        else others++;
    }

    getData().removeAttributeModifier(ATK, "vesna_a4_atk");  // 先清旧的（队伍变了）
    getData().removeAttributeModifier(ELEMENTAL_MASTERY, "vesna_a4_em");
    // C4 时三倍
    float scale = hasConstellation(4) ? C4_WINTER_RITE_MULTIPLIER : 1f;  // C4 = 3f
    if (windOrIce > 0)
        getData().addAttributeTempPercentModifier(ATK, "vesna_a4_atk", 0.06f * windOrIce * scale);
    if (others > 0)
        getData().addAttributeTempFlatModifier(ELEMENTAL_MASTERY, "vesna_a4_em", 25f * others * scale);
}
```

### 移除属性的时机

- **退出星扩散状态**：`StellarGlimmer.hasSwirl(this)` 返回 false → 调 `removeAttributeModifier`
- **突破等级不够**：`ascensionPhase < 4` → 同上

**不需要在清层的地方显式移除** —— 每刻 tick 会自动判断，条件不满足就摘。

### 为什么不用 effect？

因为这是**每刻动态变化的**（队伍换了人、星扩散状态进进出出），用 effect 挂/摘的成本比直接 tick 里重算高。而且这个修饰符只在薇斯娜自己身上，不需要 effect 的跨角色传递能力。

## 1.9 常驻接口天赋：星扩散户口

**实现方式**：**角色类实现 `IStellarHousehold` 接口**，整个角色对象就是户口提供者。

### 两个接口方法

```java
// Vesna.java:226-232
@Override
public IStellarHousehold.StellarHousehold stellarHousehold() {
    return new IStellarHousehold.StellarHousehold(
        StellarGlimmerBranch.SWIRL,     // 提供星扩散分支
        ModElements.CYRO.get(),         // 把谁转成星扩散？冰扩散
        ModElements.ANEMO.get(),        // 用什么转？风
        stellarSwirlBaseBonusMult());   // 给全队星扩散基础伤害的加成
}

// Vesna.java:213-217
public float stellarSwirlBaseBonusMult() {
    double atk = getData().getAttributeTotalValue(ModAttributes.ATK.value());
    int stages = (int) Math.floor(atk / 100.0);     // 每满 100 点攻击力提升一档
    return Math.min(0.14f, stages * 0.007f);        // 每档 +0.7%，上限 14%
}
```

### 什么时候被读？

反应系统在处理**每一次扩散反应**（冰+风 触发扩散时），遍历队伍里所有实现了 `IStellarHousehold` 的角色，看谁能把这次扩散转化成星扩散。薇斯娜的户口只对「冰+风」的扩散生效。

### 为什么是常驻接口不是突破天赋？

因为整个角色类从创建那一刻起就实现了 `IStellarHousehold` —— 接口是构造器里 `implements` 声明的，不是突破后才有的。她**从 1 级起**就能让全队冰扩散转星扩散（但星扩散状态的进入权限由 `IStellarStateHolder` 另一个接口控制）。

## 1.10 命座

### C1 · 巡风列装下星扩散 +20% + 三阶次数 +1 + 第一次三阶免剑气

**实现方式 1（伤害加成）：接口 getter**

```java
// Vesna.java:341-346
@Override
public float getStellarGlimmerBonus(StellarGlimmerBranch branch) {
    if (branch == StellarGlimmerBranch.SWIRL && windriderActive && hasConstellation(1))
        return 0.20f;
    return 0f;
}
```

反应系统在结算星烁加成区时，对**队伍每个角色**调 `getStellarGlimmerBonus(SWIRL)`，把返回值加进星烁加成。

**实现方式 2（三阶次数）：getter 判断**

```java
// Vesna.java:323-325
public int maxLv3UsesInWindrider() {
    return BASE_LV3_USES_IN_WINDRIDER + (hasConstellation(1) ? C1_EXTRA_LV3_USES : 0);  // 3 + (C1 ? 1 : 0)
}
```

翔风剑结束时 `advanceAfterCast()` 读这个值决定是否 exit 模式。

**实现方式 3（免剑气）：getter 判断**

```java
// Vesna.java:333-337
public boolean lv3CastIsFreeNow() {
    return hasConstellation(1) && xiangfengJianLevel >= 2 && lv3UsesInWindrider == 0;
}
```

翔风剑扣剑气前先调这个。

### C2 · 满层整肃 → 攻击力 +40% + 进入模式直接拉满整肃

**实现方式 1（攻击力加成）：每刻 tick 动态加/删临时属性修饰符**（和突破4同模式）

```java
// Vesna.java:378-383
private void updateC2Bonus() {
    getData().removeAttributeModifier(ATK, "vesna_c2_atk");
    if (c2GrantsMaxDecree() && decreeStacks() >= MAX_DECREE_STACKS)
        getData().addAttributeTempPercentModifier(ATK, "vesna_c2_atk", 0.40f);
}
```

`c2GrantsMaxDecree()` = `hasConstellation(2) && hasSpringRiteTalent()` —— 前置需要突破1的大权。

**实现方式 2（拉满整肃）：覆写 activateWindriderMode**

```java
// Vesna.java:432-435
public void activateWindriderMode() {
    // ... 设置模式状态 ...
    if (c2GrantsMaxDecree()) fillDecreeToMax();    // 清完之后立刻填满
}
```

`fillDecreeToMax()` 就是 `Arrays.fill(decreeTicks, DECREE_DURATION_TICKS)` —— 入门刀在清层之后、填满之前结算，所以入门刀不吃满层加成（时机正确）。

### C4 · 仪典·冬之凯风 强化（突破 4 的三倍）

**实现方式**：在 `updateA4Bonuses()` 里读 constellation 状态改 scale：

```java
// Vesna.java:294
float scale = hasConstellation(4) ? C4_WINTER_RITE_MULTIPLIER : 1f;  // 3f vs 1f
```

突破4那套逻辑的**原地放大 3 倍**，不需要单独写 C4 的 buff。

### C6 · 翔风剑·变移 + 星扩散擢升 20%

**实现方式 1（变移窗口）：字段 + getActionStateKey 拦截**

```java
// Vesna.java:508-513
@Override
public String getActionStateKey(Player player) {
    if (hasBianyiWindow(player)) return "bianyi";       // ← 拦截！
    if (!windriderActive) return "default";
    return "windrider_" + xiangfengJianLevel;
}
```

然后 `VesnaTalent.buildActionSet()` 里：
```java
// VesnaTalent.java:176-187
if ("bianyi".equals(stateKey)) {
    ActionDefinition bianyi = ActionDefinition.builder(ActionKind.SPECIAL)
        .step(VesnaResources.BIANYI_STEP)
        .onCastStart(ctx -> bianyiUser.consumeBianyiWindow())  // 出手立刻关窗口
        .onActiveStart(ctx -> bianyiDamage(ctx.player, ctx.character))
        .build();
    return setBuilder(base)
        .clearNormalCombo()              // 清空普攻队列
        .addNormalAttack(bianyi)         // 普攻槽换成变移
        .addSkillTap(bianyi)             // 战技槽也换成变移
        .build();
}
```

**不是**用 `onCastStart` 里写 if/else 来拦截。**是**通过 `getActionStateKey` 返回不同的状态键 → `buildActionSet` 重建整个 ActionSet → 普攻槽和战技槽**全部换成**变移那一段。客户端和服务端读同一份 ActionSet，天然同步。

变移窗口的打开时机：`VesnaTalent.elementalSkill()` 里，三阶翔风剑结算完、`advanceAfterCast` 之后（因为那个方法可能已经 exit 模式了，窗口在模式外也允许存在）：

```java
// VesnaTalent.java:434-436
if (castLevel >= 2) vesna.openBianyiWindow(player);
```

**实现方式 2（星扩散擢升）：接口 getter**

```java
// Vesna.java:352-357
@Override
public float getOwnElevationBonus(StellarGlimmerBranch branch) {
    if (branch == StellarGlimmerBranch.SWIRL && hasConstellation(6))
        return 0.20f;
    return 0f;
}
```

反应系统在结算**擢升区**时读这个 getter（独立的乘区，不是反应加成区）。

## 1.11 整肃：实现机制详解

### 完整链路

```
[触发点]
  翔风剑伤害前 → grantDecree()       ← VesnaTalent.elementalSkill()
  大招伤害后   → grantDecree()       ← VesnaTalent.elementalBurst()
  入场（退场后回来）不会自动叠，要等下次翔风剑/大招

[叠层机制]
  grantDecree() 在 decreeTicks[] 里找空位（==0）或最早那格（最小值）
  decreeTicks[slot] = 400   ← DECREE_DURATION_TICKS = 20×20

[衰减]
  tick() 每刻调 tickDecree() → decreeTicks[i]-- 到 0 时触发 change

[同步客户端]
  grantDecree/tickDecree/clearDecree 里调 syncSkillState()
    → syncRealtimeState() + markDirty()

[大权区被谁读取]
  翔风剑 castXiangFengJian() 和大招 elementalBurst()
    在构造 DamageSpec 的那一刻 vesna.getSovereigntyBonus()
    → 塞进 spec.withSovereignty(sovereigntyBonus)
  结算链读到 spec.sovereignty 就乘进大权区
  ← 不是 effect 结算的，是直接写进 spec 里的
```

### 整肃 vs 4命HP加成 vs 突破4加成的实现方式对比

| 效果 | 存储方式 | 加成到哪里 | 谁来读取 |
|---|---|---|---|
| 整肃 | `int[] decreeTicks`（字段，持久化） | **DamageSpec 的 sovereignty 乘区** | VesnaTalent 构造 spec 时直接读 getter |
| C4 HP | `int[] c4HpTicks`（字段，非持久化？） | **MAX_HP 属性临时修饰符** | 属性总值计算链 |
| C2 攻击力 | 无（每刻重算） | **ATK 属性临时修饰符** | 属性总值计算链 |
| 突破 4 | 无（每刻重算） | **ATK / EM 属性临时修饰符** | 属性总值计算链 |

**关键区别**：整肃**只往 DamageSpec 的乘区塞**，不碰角色属性。而 C4 HP / C2 攻击力 / 突破 4 **直接写进属性修饰符**，所有读取属性总值的地方（普攻倍率 / 战技倍率 / 反应倍率 / 等等）都会自动吃到。

## 1.12 变移窗口：如何拦截原本的技能

### 窗口字段

```java
// Vesna.java:484-485
@DescSynced @Persisted(key = "bianyiWindowEndTick")
protected long bianyiWindowEndTick;    // 绝对到期刻，不是倒计时
```

用绝对时刻的好处：客户端不用等服务端每刻同步，`hasBianyiWindow(Player)` 用**双端本地的 `player.level().getGameTime()`** 比一下就行：

```java
// Vesna.java:488-490
public boolean hasBianyiWindow(Player player) {
    return bianyiWindowEndTick > 0L && player.level().getGameTime() < bianyiWindowEndTick;
}
```

### 拦截流程

```
1. Vesna 放过三阶翔风剑 → openBianyiWindow()
   bianyiWindowEndTick = 当前gameTime + 100刻

2. 下一次玩家按普攻键或E键
   → 客户端先问 Vesna.getActionStateKey()
   → hasBianyiWindow() == true → 返回 "bianyi"

3. buildActionSet() 收到 stateKey="bianyi"
   → 用 clearNormalCombo() 清空原来的普攻槽（6段连击）
   → 用 addNormalAttack(bianyiDefinition) 把普攻槽换成变移
   → 用 addSkillTap(bianyiDefinition) 把战技槽也换成变移

4. 玩家放变移那一下
   → onCastStart 里 consumeBianyiWindow() 把 bianyiWindowEndTick 置 0
   → 窗口关了，getActionStateKey 下次返回 "windrider_X" 或 "default"
   → 普攻槽恢复原来的 6 段连击
```

### 为什么不直接在 onCastStart 里写 if 拦截？

因为动作系统的**整个时序**（动画名、位移、伤害点、执行期）都定义在 ActionStep 里。如果只在 onCastStart 里写 if 判断，那客户端放不出变移的动画、位移也不会对、伤害点也不对 —— 客户端和服务端读的是不同的东西。

换成 `getActionStateKey → buildActionSet 重建` 的做法，**客户端和服务端读同一份 ActionSet**，天然同步。代价是 `getActionStateKey` 会被频繁调用（每次玩家想按键时），但返回的是 String，很快。

---

# 第二部分 · 沃雅妮莎（Vodyanitsa）

## 2.1 基本信息

| 项目 | 值 |
|---|---|
| ID | `vodyanitsa` |
| UID | `145002` |
| 武器类型 | 法器（CatalystCharacter） |
| 元素 | 水（Hydro） |
| 突破属性 | 生命值上限（HP） |
| 战技冷却 | 16 秒 |
| 大招冷却 | 15 秒 / 能量 60 |

**核心机制**：法器远程角色，所有技能围绕「遥久之歌」持续效果展开 —— 施放 E 后自动攻击、回血、降低水/冰抗。战技和大招**都以生命值上限为倍率**（不是攻击力倍率）。

### 角色状态字段一览

| 字段 | 类型 | 实现机制 | 用途 |
|---|---|---|---|
| `songTicks` | int | `@DescSynced @Persisted` | 遥久之歌剩余刻数（0=没在唱） |
| `songAttackTimer` | int | **未持久化** | 下一次定时攻击还剩多少刻 |
| `songHealTimer` | int | **未持久化** | 下一次回血还剩多少刻 |
| `c4HpTicks[3]` | int[] | **未持久化？** | C4 HP +20% 每层倒计时（6秒） |
| `songShredUntil` | `Map<UUID, Long>` | **未持久化** | 水/冰抗降低的每目标到期时刻 |

## 2.2 普通攻击：水色咏叹四段

### 法器远程索敌机制

**数据来源**：`VodyanitsaResources` 里所有普攻 step 都配置了：
```java
Engagement.melee()
    .withDash(false)            // 法器远程，不突进
    .withAdhesion(0, 0)         // 不吸附一小步
    .withAcquireRange(13.0)     // 索敌 13 格
    .withKeepRange(15.0)        // 保持距离
.withAttackRange(13.0)          // 生效距离 13 格
```

**伤害结算索敌链**（`VodyanitsaTalent.resolveAttackTarget()`）：
1. 优先**玩家当前锁定的目标**（`SummonTargeting.defaultMode()` 里先查锁定）
2. 没锁 → `TargetSeeker(player, 13.0, RADIUS)` —— 13 格内找最近的
3. **锁到了就直接在目标身上结算伤害**，不需要贴脸（这是法器远程的核心差异）

### DamageSpec 构造

**普攻/重击**：用 `hurt()` → 攻击力倍率
```java
// VodyanitsaTalent.java:290-296
ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ModElements.HYDRO.get())
    .multiplier(multiplier)                    // 倍率区
    .elementAmount(AttachmentType.WEAK)
    .attackerCharacter(character)
    .build();
```

**战技/大招**：用 `hurtHpScaling()` → **生命值上限倍率**
```java
// VodyanitsaTalent.java:318-327
ModDamageSpec spec = ModDamageSpec.builder(AttackType.ELEMENTAL_SKILL, ModElements.HYDRO.get())
    .hpMultiplier(multiplier)                  // ← hpMultiplier，不是 multiplier
    .elementAmount(AttachmentType.STRONG)
    .attackerCharacter(character)
    .build();
if (damageBonus != 0f) spec.withDamageBonus(damageBonus);   // 大招的歌增伤（增伤区）
```

`hpMultiplier` 在 DamageZones 的基础区里会算成 `沃雅妮莎.MAX_HP × 倍率`。

## 2.3 重击

同普攻索敌链（先锁目标，锁不到退化成水球判定框）。倍率 `CHARGE` 表（Lv1 123.76% → Lv15 293.93%）。

## 2.4 元素战技：宣叙·晨声纷流

**入口**：`VodyanitsaTalent.elementalSkill(Player, PGCharacter, int)`

### 完整执行顺序

```
① 唤春角笛伤害（两段）
   a. 13 格内索敌到的目标 → hurtHpScaling 直接结算
   b. 自身周围 4 格 AoE → hurtHpScaling 对贴脸溅射
   ↓ 命中敌人？
② grantDuetBuff(player, vodyanitsa)
   → C2: 给当前场上角色挂 DuetElement 或 DuetStellar
   ↓
③ startSong() → songTicks = 320 + (C2 ? 180 : 0)
   ↓
④ grantSongStacks(player, vodyanitsa)  [仅突破 ≥ 4]
   → 领唱：给当前场上角色挂 Antiphon（25 层，30 秒）
   → 重唱：给其余 3 个角色挂 Refrain（10 层，30 秒）
   ↓
⑤ 给全队挂 VODYANITSA_GLIMMER_EFFECT  [仅 C6]
   → 每个角色挂一个 Glimmer 效果，时长 = songTicksRemaining()
   ↓
⑥ 把附近 StellarVortexEntity.markFlowingSwirl()  [仅突破 ≥ 1]
```

### 唤春角笛的 HP 倍率

倍率表 `SKILL_DAMAGE`：Lv1 3.27% → Lv15 7.77%。所以如果沃雅妮莎 Lv90 MAX_HP ≈ 14818，入门唤春角笛的基础伤害 ≈ 14818 × 0.0327 ≈ 484。

## 2.5 遥久之歌：持续定时效果

### tick 循环

**入口**：`Vodyanitsa.tick(Player)` —— 每刻服务端执行：

```java
// Vodyanitsa.java:97-129
public void tick(Player player) {
    super.tick(player);
    if (player.level().isClientSide()) return;

    tickC4Hp();                      // ← 4命HP层先调（歌结束后也继续倒计时）
    if (songTicks <= 0) return;

    songTicks--;
    if (songTicks <= 0) return;

    if (--songAttackTimer <= 0) {
        songAttackTimer = 60;  // 3秒
        VodyanitsaTalent.songAttack(player, this);
    }
    if (--songHealTimer <= 0) {
        songHealTimer = 30;    // 1.5秒
        VodyanitsaTalent.songHeal(player, this);
    }

    clearExpiredSongShred(player);   // 摘掉到期的水/冰抗降低
}
```

### 定时攻击：每 3 秒一次

**入口**：`VodyanitsaTalent.songAttack(Player, Vodyanitsa)`

```java
// VodyanitsaTalent.java:542-557
hurtHpScaling(player, vodyanitsa, target,
    at(SKILL_DAMAGE, skillLevel),
    AttackType.ELEMENTAL_SKILL, AttachmentType.WEAK);

grantDuetBuff(player, vodyanitsa);    // C2: 每次命中都刷新双音buff

// 减抗（百分比修饰符，写在目标实体身上）
stats.attributes().setPercentModifier(HYDRO_RES, "vodyanitsa_song_res", -shred);
stats.attributes().setPercentModifier(CYRO_RES, "vodyanitsa_song_res", -shred);

vodyanitsa.markSongShred(target, player.level().getGameTime() + 120);  // 6秒到期刻记账
```

### 减抗的记账机制

**实现方式**：**目标实体属性修饰符** + **每目标独立记账 Map**

- 加减抗：直接写到目标的 `ENTITY_STATS.attributes().setPercentModifier(RES, SOURCE, -百分比)`
- 记账：`vodyanitsa.songShredUntil.put(target.getUUID(), expireTick)` —— 每一个被减抗的敌人都独立记账
- 清除：tick 里 `clearExpiredSongShred()` 遍历 map，到期时 `removeModifier` 从目标身上摘

为什么需要每目标独立记账？因为歌每 3 秒挑一个目标打，16 秒里可能打好几个不同敌人 —— 如果只记最后一个，先被打的那些减抗会摘不掉。

### 定时回血：每 1.5 秒一次

**入口**：`VodyanitsaTalent.songHeal(Player, Vodyanitsa)`

公式：
```java
baseAmount = at(HEAL_BASE, skillLevel) + at(HEAL_PCT, skillLevel) * vodyanitsa.MAX_HP
baseAmount *= 1.0 + vodyanitsa.getHealingBonus()   // 漩流颂歌 +4% 在这里吃到

// 循环全队 4 个角色
data.setCurrentHP(Math.min(healMax, data.getCurrentHP() + amount));
```

**4 命效果**（回血循环里）：
```java
// VodyanitsaTalent.java:582-589
if (vodyanitsa.hasConstellation(4)) {
    double ratio = healMax <= 0 ? 1.0 : (data.getCurrentHP() / healMax);
    if (ratio < 0.40)
        amount *= 1.5;                  // HP 低于 40%：回血 × 1.5
    else
        vodyanitsa.addC4HpStack();      // HP ≥ 40%：给沃雅妮莎加一层 HP 加成
}
```

**1 命效果**（回血循环外）：
```java
// VodyanitsaTalent.java:596-610
if (vodyanitsa.hasConstellation(1)) {
    for (int i = 0; i < 4; i++) {
        CharacterEffectHelper.addEffect(player, member,
            new CharacterEffectInstance(VODYANITSA_SPOTLIGHT_EFFECT, 100, 0, false));
    }
}
```

### 武器触发

回血循环最后调 `WeaponItem.notifyHeal(player, vodyanitsa, player, (float) baseAmount)` —— 漩流颂歌武器被动从这里分发。

## 2.6 元素爆发：终奏·伴尔沉沦

**入口**：`VodyanitsaTalent.elementalBurst(Player, PGCharacter)`

```java
// VodyanitsaTalent.java:258-266
float multiplier = at(BURST_DAMAGE, burstLevel);                   // HP 倍率基础
float songBonus = vodyanitsa.isSongActive() ? at(BURST_SONG_BONUS, burstLevel) : 0f;

if (locked != null) {
    hurtHpScaling(player, character, locked, multiplier,
        AttackType.ELEMENTAL_BURST, AttachmentType.STRONG, songBonus);  // songBonus 塞进增伤区
}
```

**增伤区 vs 倍率区**：`songBonus`（遥久之歌期间的额外一档，Lv1 48% → Lv15 114%）通过 `spec.withDamageBonus()` 加进**增伤区**（`1 + 元素伤害加成 + 效果加成 + 招式增伤`），不是倍率区。倍率区是 `skillMultiplierBonus`，和基础倍率乘算；增伤区是加算。

## 2.7 突破天赋 1：流荡风旋

**突破等级门槛**：`getData().getAscensionPhase() >= 1`

**效果**：把附近的星辉风旋转化为「流荡风旋」，引爆时额外降低附近敌人 35% 风抗（6 秒）。

### 转化入口

**入口**：`VodyanitsaTalent.elementalSkill()` 最后一段：
```java
for (var vortex : level.getEntitiesOfClass(StellarVortexEntity.class, box)) {
    vortex.markFlowingSwirl();   // 只是给实体标个 flag
}
```

### 降风抗入口

**入口**：`VodyanitsaTalent.shredWindAround(ServerLevel, x, y, z)` —— 由 `SwirlReaction` 在引爆流荡风旋时调用。

**实现方式**：**目标实体属性修饰符** + **静态 HashMap 记账**
```java
// VodyanitsaTalent.java:406-424
public static void shredWindAround(ServerLevel level, double x, double y, double z) {
    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
        stats.attributes().setPercentModifier(ANEMO_RES, "vodyanitsa_flowing_wind", -0.35f);
        WIND_SHRED_UNTIL.put(target.getUUID(), until);   // 静态 HashMap
    }
}
```

**清除**：`clearExpiredWindShred()` —— 由在唱歌的沃雅妮莎每刻 tick 调一次。用**静态 Map**（不是实例字段），因为风旋引爆和沃雅妮莎实例解耦 —— 引爆时可能已经没沃雅妮莎在场了。

### 星扩散持续时间 +4 秒（辅助效果）

**入口**：`VodyanitsaTalent.songCovers(ServerLevel, x, y, z)` —— 队伍里任一沃雅妮莎在唱歌、且离位置 ≤ 13 格。由 `StellarGlimmer` 进入判定时读这个方法加时长。

## 2.8 突破天赋 4：领唱 / 重唱（≥ 突破 4）

**突破等级门槛**：`getData().getAscensionPhase() >= 4`

**效果**：给**当前场上角色**25层「领唱」、**其余3个角色**各10层「重唱」，各30秒。

### 实现方式：CharacterEffect（效果系统）

领唱和重唱都是**挂在受益人身上**的 `ICharacterEffect`：

| 效果 | 类 | 挂给谁 | 层数 | 时长 |
|---|---|---|---|---|
| 领唱 | `VodyanitsaSongEffects.Antiphon` | 当前场上角色 | 25 | 30 秒 |
| 重唱 | `VodyanitsaSongEffects.Refrain extends Antiphon` | 其余 3 个 | 10 | 30 秒 |

两者逻辑完全一致（Refrain 直接继承 Antiphon），区别只在**挂给谁**和**层数**。

### 加层入口

**入口**：`VodyanitsaTalent.grantSongStacks()` —— 战技成功时调：

```java
// VodyanitsaTalent.java:472-481
var instance = new CharacterEffectInstance(effect, DURATION_TICKS, 0, false);
instance.setIntData("vodyanitsa_song_stacks", stacks);    // 层数存在效果实例的 intData 里
CharacterEffectHelper.addEffect(player, member, instance);
```

已有同效果实例时走 `Antiphon.onEffectOverride()`：
```java
// VodyanitsaSongEffects.java:96-103
newInstance.setDuration(Math.max(existingInstance.getDuration(), newInstance.getDuration()));   // 取更长
newInstance.setIntData(STACK_KEY, Math.max(existing, new));                                      // 取更多
```

### 附加伤害在哪里生效？

**入口**：`Antiphon.onAttacked(Player holder, PGCharacter character, LivingEntity target, CharacterEffectInstance instance, ModDamageSource damageSource)`

每次**受益人受到伤害**时，这个回调被效果系统触发。它**在结算链里直接修改 DamageSpec**：

```java
// VodyanitsaSongEffects.java:69-93
@Override
public void onAttacked(...) {
    ModDamageSpec spec = damageSource.getSpec();

    // 判元素类型 + 判流荡风旋模式
    boolean stellarMode = flowingSwirlActive(holder);
    if (stellarMode ? !spec.isStellarReactionDamage()
                    : !(element == HYDRO || element == CYRO)) return;   // 不满足就不触发也不扣层

    float bonus = flatBonus(holder, stellarMode);                       // 按 HP 算这一层值多少
    if (bonus <= 0f) return;

    // 直接把固定值塞进本次伤害的 DamageSpec 基础区
    damageSource.setSpec(spec.withFlatDamageBonus(
        spec.getFlatDamageBonus() + bonus));                            // 叠加到已有附加伤害上

    consumeOneStack(holder, character, instance);                       // 层数 -1，耗尽自动移除效果
}
```

**结算链顺序**：`baseDamage → multiplier → flatDamageBonus（这里塞） → 元素加成 → 反应加成 → 防御 → ...`。`flatDamageBonus` 和基础值**加算**（和申鹤冰凌同位置），走 `withFlatDamageBonus`。

### flatBonus 怎么算？

```java
// VodyanitsaSongEffects.java:124-134
private static float flatBonus(Player holder, boolean stellar) {
    PGCharacter vodyanitsa = findVodyanitsa(holder);
    double hp = vodyanitsa.MAX_HP;                  // 沃雅妮莎自己的生命值上限
    double over = Math.max(0.0, hp - 40000.0);      // 门槛 40000
    float steps = (float) (over / 1000.0);          // 每 1000 点一档
    float perStep = stellar ? 260f : 140f;          // 星扩散 260 / 水冰 140
    float cap = stellar ? 6500f : 3500f;
    return Math.min(cap, steps * perStep);
}
```

### 流荡风旋判定

**入口**：`VodyanitsaSongEffects.flowingSwirlActive(Player)` —— `Antiphon.onAttacked` 每一次伤害都调：

```java
// VodyanitsaSongEffects.java:171-199
public static boolean flowingSwirlActive(Player holder) {
    // 判据 1：刚引爆完 5 秒内（由 StellarVortexEntity 引爆时盖章）
    if (lastDetonationTick >= 0L && now - lastDetonationTick <= 100) return true;
    // 判据 2：以玩家为球心半径 20 格内有 isFlowingSwirl() 的实体
    for (var vortex : level.getEntitiesOfClass(StellarVortexEntity.class, box)) {
        if (vortex.isFlowingSwirl()) return true;
    }
    return false;
}
```

**踩过的坑**：`lastDetonationTick` 原来初始化成 `Long.MIN_VALUE`，从没引爆过时 `gameTime - MIN_VALUE` 直接溢出成负数 → 恒返回 true → 沃雅妮莎自己的水伤永远走「星扩散模式」，一层都不消耗。现在改成 `-1L` 哨兵值并且先判 `>= 0L`。

## 2.9 命座

### C1 · 聚光（Spotlight）：攻击力 = HP × 1%

**实现方式**：**CharacterEffect —— 效果系统**

**效果类**：`VodyanitsaBuffs.Spotlight implements ICharacterEffect`

```java
// VodyanitsaBuffs.java:37-40
@Override
public void onEffectAdded(Player holder, PGCharacter character, CharacterEffectInstance instance) {
    float bonus = VodyanitsaSongEffects.onePercentOfMaxHp(holder);   // 读沃雅妮莎当前 HP × 1%
    character.getData().addAttributeTempFlatModifier(ATK, "vodyanitsa_spotlight", bonus);
}

@Override
public void onEffectRemoved(Player holder, PGCharacter character, CharacterEffectInstance instance) {
    character.getData().removeAttributeModifier(ATK, "vodyanitsa_spotlight");
}

@Override
public void onEffectOverride(Player holder, PGCharacter character, ...) {
    existingInstance.setDuration(Math.max(existingInstance.getDuration(), newInstance.getDuration()));   // 只刷新时长
}
```

**关键细节**：`onEffectOverride` **不重算属性值** —— 只刷新时长。这意味着：
- 每次回血（每 1.5 秒）都 `addEffect` → existingInstance 存在 → 走 `onEffectOverride`
- 属性值**还是第一次挂效果那一刻采的 HP**（因为 `onEffectAdded` 只在第一次调）
- 如果中间沃雅妮莎的 HP 变了（比如 C4 加层/减层），聚光的攻击力值**不会跟着变**，除非效果彻底掉了重新挂

### C2 · 穿彻风雪的余响

**效果 1（时长 +9 秒）：字段 + getter**

```java
// Vodyanitsa.java:79-85
public void startSong() {
    this.songTicks = 320 + (hasConstellation(2) ? 180 : 0);   // 320=16秒, 180=9秒
    this.songAttackTimer = 60;
    this.songHealTimer = 30;
}
```

**效果 2（黑与白的双音）：CharacterEffect —— 效果系统**

**效果类**：`VodyanitsaBuffs.DuetElement` / `DuetStellar implements ICharacterEffect`

```java
// DuetElement（无流荡风旋时挂）
@Override
public float getCritDamageBonus(GenshinElement element, boolean stellarReaction) {
    if (element == HYDRO || element == CYRO) return 0.50f;   // 水/冰暴击伤害 +50%
    return 0f;
}

// DuetStellar（有流荡风旋时挂）
@Override
public float getCritDamageBonus(GenshinElement element, boolean stellarReaction) {
    return stellarReaction ? 0.60f : 0f;                     // 星扩散反应暴击伤害 +60%
}
```

**加伤伤口径**：`ICharacterEffect.getCritDamageBonus()` —— **接口 getter**，伤害结算链在算暴击区时遍历角色身上所有 effect 调这个方法。

**挂效果时机**：每次「命里敌人」就挂一次（战技召唤角笛时 + 遥久之歌每次命中时），每次重新判断 `flowingSwirlActive()` 决定挂哪个。**不是动态切换，是每次挂新的**。

**范围**：2 命时只给当前场上角色（`grantDuetBuff` 里 `!hasConstellation(6) && member != onField`），6 命时去掉这个限制给全队。

### C4 · 40% HP 以下治疗 ×1.5 + HP +20%/层

**效果 1（治疗 ×1.5）：直接写在回血逻辑里**

```java
// VodyanitsaTalent.java:582-585
if (ratio < 0.40) amount *= 1.5;
```

**效果 2（HP +20%/层）：字段数组 + 属性修饰符**（和薇斯娜整肃同形态）

```java
// Vodyanitsa.java:169-182
private void refreshC4HpBonus() {
    int stacks = 0;
    for (int ticks : c4HpTicks) if (ticks > 0) stacks++;
    if (stacks > 0)
        getData().setAttributeTempPercentModifier(MAX_HP, "vodyanitsa_c4_hp", 0.20f * stacks);
    else
        getData().removeAttributeModifier(MAX_HP, "vodyanitsa_c4_hp");
}
```

**用 setAttributeTempPercentModifier 而不是 remove + add**：避免 MAX_HP 先降后升导致 currentHP 振荡（`preserveHpIfMaxHp` 检测到 MAX_HP 变动就同步比例，但中间值是 0% stacks → MAX_HP 先掉 → currentHP 先掉 → 再加回来 → currentHP 再涨，看起来就是血条抖了一下）。

**关键区别**：c4HpTicks 的 tick 放在 `if (songTicks <= 0) return` **之前** —— 歌结束后 HP 层继续独立倒计时。

### C6 · 全队星扩散擢升 30% + 水冰增伤 60%

**实现方式**：**CharacterEffect —— 效果系统**

**效果类**：`VodyanitsaBuffs.Glimmer implements ICharacterEffect`

```java
// VodyanitsaBuffs.java:92-103
@Override
public float getElevationBonus(StellarGlimmerBranch branch) {
    return branch == StellarGlimmerBranch.SWIRL ? 0.30f : 0f;   // 星扩散擢升 +30%
}

@Override
public float getDamageBonus(AttackType attackType, GenshinElement element) {
    if (element == HYDRO || element == CYRO) return 0.60f;      // 水冰增伤 +60%
    return 0f;
}
```

**加伤伤口径**：两个都是**接口 getter** —— 结算链在相应乘区遍历所有 effect。

**挂效果时机**：战技成功时一次性给全队 4 人挂，时长 = `vodyanitsa.songTicksRemaining()` —— 和遥久之歌同步到期，不用单独清理。

## 2.10 所有效果实现机制汇总

| 效果 | 实现方式 | 加在什么地方 | 叠层/加效果入口 | 移除时机 |
|---|---|---|---|---|
| 遥久之歌 | `@Persisted int songTicks` + 每刻 tick | 无，只是开关定时器 | `startSong()` | tick 到 0 自动停 |
| C4 HP +20%/层 | `int[] c4HpTicks` 字段 + 属性临时修饰符 | **MAX_HP 属性** | `addC4HpStack()` 在回血里 | tick 到 0 → `refreshC4HpBonus()` 自动清 |
| C1 聚光 | CharacterEffect | **ATK 属性临时固定值修饰符** | `songHeal()` 每 1.5 秒 addEffect | effect 到期自动 remove |
| C2 黑与白的双音 | CharacterEffect | **暴击区 getCritDamageBonus** | `songAttack()` + 战技召唤角笛时 | effect 到期自动 remove |
| C6 Glimmer | CharacterEffect | **擢升区 getElevationBonus** + **增伤区 getDamageBonus** | 战技成功时一次性挂 | effect 到期（= songTicksRemaining） |
| 突破 2 领唱/重唱 | CharacterEffect | **DamageSpec 的 flatDamageBonus（直接塞进 spec）** | 战技成功时一次性挂 | effect 到期自动 remove（耗尽层数也会 remove） |
| 流荡风旋降风抗 | 目标实体属性修饰符 + 静态 HashMap | **目标 ANEMO_RES -35%** | 流荡风旋引爆时 | 到期刻自动 clear |
| 遥久之歌减水冰抗 | 目标实体属性修饰符 + 实例字段 Map | **目标 HYDRO_RES/CYRO_RES -shred** | songAttack 每 3 秒 | 每目标独立到期刻自动 clear |

---

# 第三部分 · 漩流颂歌（WhirlflowHymn）专武

## 3.1 武器面板

| 项目 | 值 |
|---|---|
| 武器类型 | 法器（Catalyst） |
| 基础攻击力（Lv1 / Lv90） | 49 / 741 |
| 副词条（Lv1 / Lv90） | 生命值上限 16.5% / 66.2% |

## 3.2 武器被动 ①：治疗加成 +4%

**实现方式**：**角色接口 getter** —— 覆写 `getHealingBonus()`：

武器装配在沃雅妮莎身上时，`getHealingBonus()` 额外返回 0.04f。这是**基类接口**，不是 effect。`VodyanitsaTalent.songHeal()` 里的 `baseAmount *= (1.0 + vodyanitsa.getHealingBonus())` 直接吃到。

## 3.3 武器被动 ②：告真的蜜酿

### 实现方式：CharacterEffect（效果系统），拆成两个独立的 effect

一次治疗要同时给**两个不同的人**加东西（装备者加 HP、当前场上角色加攻击力），所以拆成两个 effect 实例。

#### Mead（HP 层数，挂装备者）

| 项 | 内容 |
|---|---|
| 效果类 | `WhirlflowHymnEffects.Mead implements ICharacterEffect` |
| 挂给谁 | **装备者本人**（可能不是当前场上） |
| 层数存储 | `instance.setIntData("whirlflow_hymn_mead_stacks", stacks)` |
| 时长 | 10 秒（每 1.5 秒回血都 refresh → 一直续） |
| 层数上限 | 3 |
| 加属性方式 | `character.getData().setAttributeTempPercentModifier(MAX_HP, "whirlflow_hymn_mead_hp", value)` |

`value = 0.04 × stacks × reactionMultiplier(character)` —— 每层 +4%，触发 1.75× 窗口时乘 1.75。

#### MeadAtk（攻击力，挂当前场上角色）

| 项 | 内容 |
|---|---|
| 效果类 | `WhirlflowHymnEffects.MeadAtk implements ICharacterEffect` |
| 挂给谁 | **当前场上角色**（可能不是装备者） |
| 装备者记录 | `instance.setIntData("whirlflow_hymn_mead_owner", equipperUuid)` |
| 基准 HP 记录 | `instance.setIntData("whirlflow_hymn_mead_base_hp", baseHp)` |
| 层数上限 | 3 |
| 加属性方式 | `removeAttributeModifier(ATK, ...)` → `addAttributeTempPercentModifier(ATK, "whirlflow_hymn_mead_atk", percent)` |

**为什么先 remove 再 add？** 因为换人时蜜酿的 owner 变了，旧 owner 的攻击力加成要摘掉。

#### 每层攻击力百分比的计算

```java
// WhirlflowHymnEffects.java:242-250
double hpForAtk = baseHp * (1.0 + HP_PERCENT_PER_STACK * (stacks - 1));  // 上一层的 HP 含蜜酿加成
double over = Math.max(0.0, hpForAtk - 40000.0);
double perStack = Math.min(ATK_PERCENT_CAP, (over / 1000.0) * 0.004);   // 每 1000 点 +0.4%，上限 8%/层
double percent = perStack * stacks * reactionMultiplier;                // 每层值 × 层数 × 1.75（如果窗口开）
```

**为什么要记录 baseHp？** 每层的蜜酿 HP 加成会反过来喂给攻击力换算。如果不记录基准 HP，每次加层会把上一层的加成又吃一次，越叠越离谱。第一次挂效果时用当前 HP 除回去得到 `baseHp = maxHp / (1 + 0.04 × stacks)`，之后一直用它当基准。

### 触发入口

```java
// WhirlflowHymnEffects.java:153-162
public static void onEquipperHeal(Player player, PGCharacter equipper, PGCharacter onField) {
    addMead(player, equipper, WHIRLFLOW_MEAD_EFFECT, equipperUuid);       // 装备者加 HP 层
    if (onField != null)
        addMead(player, onField, WHIRLFLOW_MEAD_ATK_EFFECT, equipperUuid); // 当前场上加攻击力层
}
```

每次 `songHeal()` 结束时调 `WeaponItem.notifyHeal()` → 这里被分发到。

### 刷新机制

`addMead()` 每次都创建新的 `CharacterEffectInstance`。如果已有同效果实例 → 效果系统自动调 `onEffectOverride()`：

```java
// Mead.onEffectOverride
newInstance.setDuration(10 * 20);                                           // 重置 10 秒
newInstance.setIntData(STACK_KEY, Math.min(MAX_STACKS, existingStacks + 1)); // 层数 +1（封顶 3）
applyHpBonus(character, newInstance);                                       // 重算属性

// MeadAtk.onEffectOverride
newInstance.setIntData(STACK_KEY, Math.min(MAX_STACKS, existingStacks + 1));
newInstance.setIntData(OWNER_KEY, 保留旧的或新的);                            // 换人时切换 owner
newInstance.setIntData(BASE_HP_KEY, 保留旧的);                               // 基准 HP 不变
applyAtkBonus(holder, character, newInstance);                               // 重算属性
```

### tick 兜底

`onEffectTick()` 每刻都重算属性值。这是为了**强化窗口的开关** —— 窗口开了立刻生效、窗口关了自动降档（不需要等下一次治疗触发 refresh）。

## 3.4 武器被动 ③：冻结/星扩散 1.75× 强化窗口

### 窗口存储

```java
// PGCharacterData.java
long whirlflowReactionWindowEnd;   // 绝对到期刻，0 = 窗口没开
```

用绝对时刻和变移窗口同做法 —— 双端用 `player.level().getGameTime()` 直接比。

### 触发入口

```java
// WhirlflowHymnEffects.java:165-167
public static void markReactionWindow(PGCharacter equipper, long gameTime) {
    equipper.getData().setWhirlflowReactionWindowEnd(gameTime + REACTION_WINDOW_TICKS);  // 5 秒
}
```

由 `WhirlflowHymn#markReactionTriggers` 在**冻结或星扩散反应**执行时调用（不管反应成功与否，只要触发了就开）。

### 倍率读取

```java
// WhirlflowHymnEffects.java:255-260
private static double reactionMultiplier(PGCharacter character) {
    Player owner = character.getData().getOwnerPlayer();
    if (owner == null) return 1.0;
    return character.getData().isWhirlflowReactionWindowActive(owner.level().getGameTime())
            ? 1.75 : 1.0;
}
```

被 `applyHpBonus()` 和 `applyAtkBonus()` 每刻 tick 时读取。

### 窗口关闭

**自动** —— `onEffectTick()` 每刻重算属性，`reactionMultiplier()` 检测到 `gameTime >= windowEnd` 就自动返回 1.0，属性自动降回。不需要显式移除什么。

## 3.5 所有效果实现机制汇总

| 效果 | 实现方式 | 加在什么地方 | 叠层/加效果入口 | 移除时机 |
|---|---|---|---|---|
| 治疗 +4% | 角色接口 getter（`getHealingBonus`） | 无加成，只是 getter 多返回 0.04f | 武器装配时自动生效 | 卸下武器自动失效 |
| Mead（HP 层数） | CharacterEffect（`ICharacterEffect`） | **装备者 MAX_HP 临时百分比修饰符** | `onEquipperHeal()` 每次回血 | effect 到期自动 remove |
| MeadAtk（攻击力） | CharacterEffect（`ICharacterEffect`） | **当前场上角色 ATK 临时百分比修饰符** | `onEquipperHeal()` 每次回血 | effect 到期自动 remove |
| 1.75× 强化窗口 | `long whirlflowReactionWindowEnd`（绝对时刻） | 无加成，只是判断依据 | `markReactionWindow()` 在冻结/星扩散时 | 自动到期（下一次 onEffectTick 重算时变 1.0） |

---

# 附录 · 各效果实现方式一览表

> 快速查阅：这个 buff 到底是怎么加到角色身上的？
> 符号说明：**字段** = 角色类字段存储层数/状态；**Effect** = CharacterEffect 挂在受益人身上；**Getter** = 接口 getter，结算时被读；**Spec** = 直接塞进 DamageSpec；**属性** = 直接写进 PGCharacterData 的属性修饰符容器。

## 薇斯娜

| 效果 | 实现方式 | 加成落点 | 结算链里谁读 |
|---|---|---|---|
| 整肃（大权区） | **字段**（`decreeTicks[]`）+ **Spec** | `spec.sovereignty` 乘区 | VesnaTalent 构造 spec 时 getter 读 `getSovereigntyBonus()` |
| C2 攻击力 +40% | **每刻 tick 动态写属性** | ATK 临时百分比修饰符 | 属性总值计算链 |
| 突破 4 队伍加成 | **每刻 tick 动态写属性** | ATK + EM 临时修饰符 | 属性总值计算链 |
| C4 突破 4 三倍 | **同突破 4**（scale 字段 +3×） | 同上 | 同上 |
| C1 星扩散 +20% | **Getter** | `getStellarGlimmerBonus(SWIRL)` | 反应系统结算星烁加成区时遍历 |
| C6 星扩散擢升 | **Getter** | `getOwnElevationBonus(SWIRL)` | 反应系统结算擢升区时遍历 |
| 星扩散户口 | **接口实现** | 常驻：冰扩散→星扩散的转档 | 反应系统处理扩散时遍历全队 |
| 翔风剑·变移 | **字段**（`bianyiWindowEndTick`）+ **ActionSet 重建** | 无属性加成，拦截技能 | `getActionStateKey()` 返回 `"bianyi"` |

## 沃雅妮莎

| 效果 | 实现方式 | 加成落点 | 结算链里谁读 |
|---|---|---|---|
| 遥久之歌 | **字段**（`songTicks`）+ tick | 无加成，只是开关定时器 | tick 每刻 |
| 突破 4 领唱/重唱 | **Effect** | **Spec 的 `flatDamageBonus` 基础区** | `onAttacked` 时直接塞本次 spec |
| C1 聚光 | **Effect** | ATK 临时固定值修饰符 | 属性总值计算链 |
| C2 黑与白的双音 | **Effect** | **Getter** `getCritDamageBonus()` | 结算链算暴击区时遍历所有 effect |
| C4 HP +20%/层 | **字段**（`c4HpTicks[]`）+ **属性** | MAX_HP 临时百分比修饰符 | 属性总值计算链 |
| C6 Glimmer | **Effect** | **Getter** `getElevationBonus()` + `getDamageBonus()` | 结算链相应乘区遍历所有 effect |
| 流荡风旋降风抗 | **目标实体属性** + **静态 Map** | 目标 ANEMO_RES 修饰符 | 敌人属性总值计算链 |
| 遥久之歌减水冰抗 | **目标实体属性** + **实例 Map** | 目标 HYDRO_RES/CYRO_RES 修饰符 | 敌人属性总值计算链 |
| 大招歌期间增伤 | **Spec** | `spec.damageBonus` 增伤区 | 结算链算增伤区时读 spec |

## 漩流颂歌

| 效果 | 实现方式 | 加成落点 | 结算链里谁读 |
|---|---|---|---|
| 治疗 +4% | **Getter**（覆写） | `getHealingBonus()` 多返回 0.04f | `songHeal()` 回血公式直接乘 |
| Mead（HP） | **Effect** | MAX_HP 临时百分比修饰符 | 属性总值计算链 |
| MeadAtk（攻击力） | **Effect** | ATK 临时百分比修饰符 | 属性总值计算链 |
| 1.75× 窗口 | **字段**（绝对时刻） | 无加成，只是倍率系数 | `onEffectTick` 每刻重算属性时乘 |