# 实体 AI 指南 —— 原版 Goal 原理、可复用清单与自研 Goal 写法

> 适用版本：Minecraft **26.2** / NeoForge **26.2.0.88** / GeckoLib **5.5.6**（Java 25）
> 本文是**测试更新（技术研讨）**的产物，配套代码：
> `content/entities/ai/`（通用 Goal / MoveControl）、`content/entities/misc/`（test1 演示实体）、
> `core/asset/`（类别 + id 资源解析）、`client/render/geo/`（统一布局模型缓存）。
> 本文所有原版结论都核对过 `build/moddev/artifacts/minecraft-patched-26.2.0.88-sources.jar`。

---

## 目录

> 全部条目都可点击跳转；锚点由标题文本自动生成，**改标题就要顺带改锚点**。

- [0. 一句话结论](#0-一句话结论)

- [1. Goal 是怎么被驱动的](#1-goal-是怎么被驱动的)
  - [1.1 三个对象](#11-三个对象)
  - [1.2 一 tick 里发生了什么](#12-一-tick-里发生了什么)
  - [1.3 标志位：Goal 之间唯一的仲裁手段](#13-标志位goal-之间唯一的仲裁手段)
  - [1.4 优先级](#14-优先级)
  - [1.5 回调节奏](#15-回调节奏)
  - [1.6 构造期陷阱：`registerGoals()` 在构造函数里被调用](#16-构造期陷阱registergoals-在构造函数里被调用)

- [2. 写一个 Goal 的模板](#2-写一个-goal-的模板)

- [3. 原版 Goal 清单（26.2）](#3-原版-goal-清单262)
  - [3.1 `target` 子包：目标选择（挂在 `targetSelector`）](#31-target-子包目标选择挂在-targetselector)
  - [3.2 战斗类](#32-战斗类)
  - [3.3 移动 / 巡逻类](#33-移动--巡逻类)
  - [3.4 环境交互 / 感知类](#34-环境交互--感知类)
  - [3.5 水中 / 特殊位移](#35-水中--特殊位移)
  - [3.6 需要子类化才划算的，以及原版真的没有的](#36-需要子类化才划算的以及原版真的没有的)
  - [3.7 「不能用」的：`ai.behavior` 包](#37-不能用的aibehavior-包)

- [4. 另一套体系：Brain + Behavior](#4-另一套体系brain--behavior)
  - [4.1 一句话区别](#41-一句话区别)
  - [4.2 两套体系可以并存](#42-两套体系可以并存)
  - [4.3 Brain 的三张表](#43-brain-的三张表)
  - [4.4 一 tick 里 Brain 做了什么](#44-一-tick-里-brain-做了什么)
  - [4.5 Memory：黑板](#45-memory黑板)
    - [4.5.1 `MemoryModuleType` 是注册表对象](#451-memorymoduletype-是注册表对象)
    - [4.5.2 `MemoryStatus`：三态，不是两态](#452-memorystatus三态不是两态)
    - [4.5.3 读写的四条 API](#453-读写的四条-api)
  - [4.6 Sensor：谁往黑板上写字](#46-sensor谁往黑板上写字)
  - [4.7 Activity：状态机的大档位](#47-activity状态机的大档位)
    - [4.7.1 档位是怎么切的](#471-档位是怎么切的)
    - [4.7.2 两个容易忽略的机制](#472-两个容易忽略的机制)
    - [4.7.3 `ActivityData.create` 的优先级分配](#473-activitydatacreate-的优先级分配)
  - [4.8 Timeline：26.2 新增，老教程的 Schedule 已经没了](#48-timeline262-新增老教程的-schedule-已经没了)
    - [4.8.1 变化对比](#481-变化对比)
    - [4.8.2 Timeline 长什么样](#482-timeline-长什么样)
    - [4.8.3 它是怎么生效的](#483-它是怎么生效的)
    - [4.8.4 想给自定义怪做日程](#484-想给自定义怪做日程)
  - [4.9 Behavior：和 Goal 逐条对照](#49-behavior和-goal-逐条对照)
    - [4.9.1 生命周期](#491-生命周期)
    - [4.9.2 三个必须记住的坑](#492-三个必须记住的坑)
    - [4.9.3 `Behavior` 的两种子类型](#493-behavior-的两种子类型)
  - [4.10 组合器：把多个行为打包](#410-组合器把多个行为打包)
    - [4.10.1 `RunOne` —— 加权随机选一个](#4101-runone--加权随机选一个)
    - [4.10.2 `GateBehavior` —— 组合器的基类](#4102-gatebehavior--组合器的基类)
    - [4.10.3 `TriggerGate` 与 `BehaviorBuilder`（声明式 DSL）](#4103-triggergate-与-behaviorbuilder声明式-dsl)
  - [4.11 案例研究：村民的一天（完整拆解）](#411-案例研究村民的一天完整拆解)
    - [4.11.1 它的大脑由什么组成](#4111-它的大脑由什么组成)
    - [4.11.2 三个入口方法](#4112-三个入口方法)
    - [4.11.3 时间线：村民一天到底在干什么](#4113-时间线村民一天到底在干什么)
    - [4.11.4 逐条读 CORE 包（`getCorePackage`）](#4114-逐条读-core-包getcorepackage)
    - [4.11.5 链路一：一个村民怎么找到床并睡下](#4115-链路一一个村民怎么找到床并睡下)
    - [4.11.6 链路二：村民被打之后发生了什么](#4116-链路二村民被打之后发生了什么)
    - [4.11.7 为什么每个包最后都有一个优先级 99 的 `UpdateActivityFromSchedule`](#4117-为什么每个包最后都有一个优先级-99-的-updateactivityfromschedule)
  - [4.12 自己写一个 Brain 怪：最小骨架](#412-自己写一个-brain-怪最小骨架)
  - [4.13 Brain 的调试手段](#413-brain-的调试手段)
  - [4.14 结论：本 MOD 的怪物还是用 Goal](#414-结论本-mod-的怪物还是用-goal)

- [5. 移动层：Goal 之外的四个控制器](#5-移动层goal-之外的四个控制器)

- [6. 本 MOD 的落地约定](#6-本-mod-的落地约定)

- [7. 范例拆解（test1 / test2 / 大型冰史莱姆）](#7-范例拆解test1--test2--大型冰史莱姆)
  - [7.1 目标配置（`Test1Entity#registerGoals`）](#71-目标配置test1entityregistergoals)
  - [7.2 三层职责划分](#72-三层职责划分)
  - [7.3 `LeapSmashGoal` 的状态机](#73-leapsmashgoal-的状态机)
  - [7.4 动画状态（`TestAction`）](#74-动画状态testaction)
  - [7.5 范例二：test2 —— 随机远程技能（召唤冰块砸人）](#75-范例二test2--随机远程技能召唤冰块砸人)
    - [7.5.1 一次技能拆成两半](#751-一次技能拆成两半)
    - [7.5.2 随机 + CD 的写法](#752-随机--cd-的写法)
    - [7.5.3 这个 Goal 只占 LOOK、不占 MOVE](#753-这个-goal-只占-look不占-move)
    - [7.5.4 为什么没用 `Display.BlockDisplay`](#754-为什么没用-displayblockdisplay)
    - [7.5.5 伤害](#755-伤害)
    - [7.5.6 资源](#756-资源)
  - [7.6 范例三：大型冰史莱姆（Large Cryo Slime）](#76-范例三大型冰史莱姆large-cryo-slime)
    - [7.6.1 它是什么](#761-它是什么)
    - [7.6.2 代码地图](#762-代码地图)
    - [7.6.3 Goal 清单与优先级](#763-goal-清单与优先级)
    - [7.6.4 撞击：为什么直接复用 `LeapSmashGoal`](#764-撞击为什么直接复用-leapsmashgoal)
    - [7.6.5 三个技能：`CryoSlimeSkillGoal` 基类](#765-三个技能cryoslimeskillgoal-基类)
    - [7.6.6 冰刺：`IceBlockProjectile` 的三段式](#766-冰刺iceblockprojectile-的三段式)
    - [7.6.7 冰雾与落点：两个 skillnode](#767-冰雾与落点两个-skillnode)
    - [7.6.8 砸落：四步状态机](#768-砸落四步状态机)
    - [7.6.9 护盾怎么接上去](#769-护盾怎么接上去)
    - [7.6.10 自挂冰附着](#7610-自挂冰附着)
    - [7.6.11 配置表（`config/minegenshin/entity.toml` 的 `[large-cryo-slime]` 段）](#7611-配置表configminegenshinentitytoml-的-large-cryo-slime-段)
    - [7.6.12 这一类怪踩过的坑（浓缩版）](#7612-这一类怪踩过的坑浓缩版)

- [8. 冲突与排查清单](#8-冲突与排查清单)

- [9. 资源契约（配合 `core/asset`）](#9-资源契约配合-coreasset)
  - [9.1 名字是「查出来的」，不是「猜出来的」](#91-名字是查出来的不是猜出来的)
  - [9.2 贴图位置的一个反直觉点](#92-贴图位置的一个反直觉点)
  - [9.3 动画名契约](#93-动画名契约)
  - [9.4 模型缓存链](#94-模型缓存链)

- [10. 下一步（test2 / test3 的扩展点）](#10-下一步test2--test3-的扩展点)

---

## 0. 一句话结论

原版实体的「什么时候做什么」= **一堆 `Goal` 挂在两个 `GoalSelector` 上，靠 `Flag` 抢控制权、靠优先级抢执行权**。
原版有 70 多个现成 Goal，**移动、巡逻、看人、近战、远程、开门、恐慌这些都不用自己写**；
真正需要自研的只有两类：**「原版没有的动作编排」**（如 test1 的「跳出→撞击→跳回」）
和**「原版有但判定/数值不合口味」**（子类化原版 Goal 改两个方法）。

---

## 1. Goal 是怎么被驱动的

### 1.1 三个对象

| 对象 | 位置 | 职责 |
|---|---|---|
| `Goal` | `net.minecraft.world.entity.ai.goal.Goal` | 一个「想做的事」：能不能做、开始、每 tick 做什么、结束 |
| `GoalSelector` | 同上 | 装 Goal 的容器，负责**仲裁**和**调度** |
| `WrappedGoal` | 同上 | Goal + 优先级 + 是否正在运行的包装 |

每个 `Mob` 有两个选择器（`Mob.java` 第 134–135 行，都是 `public final`）：

```java
public final GoalSelector goalSelector;   // 「做什么」：移动、攻击、看人……
public final GoalSelector targetSelector; // 「打谁」：只负责 setTarget
```

挂 Goal 的地方是 `Mob#registerGoals()`（`protected`，默认空实现）。

### 1.2 一 tick 里发生了什么

`Mob#serverAiStep()`（**final，不能被覆写**，第 742–781 行）顺序固定：

```
sensing.tick()                       感知
  ↓
[tickCount + id 为奇数]  targetSelector.tickRunningGoals(false)   只跑「正在运行」的 Goal
                         goalSelector.tickRunningGoals(false)
[为偶数]                 targetSelector.tick()                   完整仲裁
                         goalSelector.tick()
  ↓
navigation.tick()                    沿路径移动
  ↓
customServerAiStep(level)            ← 子类唯一的 AI 钩子
  ↓
moveControl.tick() → lookControl.tick() → jumpControl.tick()      把「意图」变成速度/朝向
  ↓
（之后 LivingEntity.aiStep 里的 travel 才真正施加物理）
```

三条必须记住的推论：

1. **`canUse()` 每隔一 tick 才评估一次**（选择器只在偶数 tick 完整仲裁），
   但 `requiresUpdateEveryTick() == true` 的**正在运行**的 Goal 仍然每 tick 都被 `tick()`。
2. **Goal 先跑，MoveControl 后跑**。所以 Goal 里写的 `setDeltaMovement` 不会被它自己覆盖，
   但会被 `MoveControl` 的 `setZza/setSpeed` 影响 —— 自研位移类 Goal 要在 `tick()` 里
   **每 tick 清零 `setSpeed(0)/setZza(0)/setXxa(0)`**，否则会和 `aiStep` 的移动输入叠加。
3. **`customServerAiStep(ServerLevel)` 是唯一可靠的「每 tick 服务端钩子」**，
   test1 用它每 tick 推导「待机 / 蠕动 / 小跳」的动画状态。

### 1.3 标志位：Goal 之间唯一的仲裁手段

```java
public enum Goal.Flag { MOVE, LOOK, JUMP, TARGET }
```

仲裁逻辑在 `GoalSelector#tick()`（第 80–90 行）：

- 一个 Goal 想要运行，它声明的**每一个** Flag 都必须「当前没有被别人占用」，
  或者占用者 `canBeReplacedBy(它)`。
- 一旦运行，它就把声明的所有 Flag **锁住**，别人想插队必须优先级更小（数值更小 = 优先级更高）
  且自己 `isInterruptable() == true`。

`WrappedGoal#canBeReplacedBy`（第 16–18 行）：

```java
return this.isInterruptable() && goal.getPriority() < this.getPriority();
```

经验规则：

| Flag | 谁该占 |
|---|---|
| `MOVE` | 所有要寻路 / 自己写位移的 Goal（`MeleeAttackGoal`、`RandomStrollGoal`、自研扑击） |
| `LOOK` | 想让头跟着目标转的 Goal（`LookAtPlayerGoal`、`MeleeAttackGoal`） |
| `JUMP` | 主动起跳的 Goal（`LeapAtTargetGoal`、自研扑击） |
| `TARGET` | **只有** `targetSelector` 上的 Goal 该占 |

**不要**给一个 Goal 声明它用不到的 Flag —— 声明了 `LOOK` 却不转头，会把 `RandomLookAroundGoal` 挤掉，
表现就是「站着脖子僵住」。

### 1.4 优先级

`addGoal(int priority, Goal goal)`：**数字越小越优先**，和 `targetSelector` 一样。

原版惯例（照抄就对了）：

```
goalSelector
  0   FloatGoal               ← 会游泳的必放 0，别让任何东西挤掉它
  1   自研的大招 / 扑击
  2   MeleeAttackGoal         ← 贴脸打
  3   LeapAtTargetGoal        ← 小跳（可选）
  4   WaterAvoidingRandomStrollGoal / RandomStrollGoal
  5   TemptGoal / FollowOwnerGoal
  8   LookAtPlayerGoal
  9   RandomLookAroundGoal

targetSelector
  1   HurtByTargetGoal
  2   NearestAttackableTargetGoal
```

### 1.5 回调节奏

| 方法 | 默认 | 作用 |
|---|---|---|
| `requiresUpdateEveryTick()` | `false` | false 时，正在运行的 Goal 只在「奇数 tick」被 tick 一次（约每 2 tick），省性能 |
| `isInterruptable()` | `true` | false = 运行期间谁都抢不走它的 Flag |
| `adjustedTickDelay(int)` | `ticks` 或 `ceil(ticks/2)` | 内部计时器要跟着上面的节奏走，**自己数 tick 的冷却必须过这个方法**，否则 `requiresUpdateEveryTick=false` 时冷却会莫名其妙翻倍 |

`MeleeAttackGoal` 就是标准范例：`this.ticksUntilNextAttack = this.adjustedTickDelay(20)`。

### 1.6 构造期陷阱：`registerGoals()` 在构造函数里被调用

`Mob` 的构造函数（第 153–166 行）末尾有：

```java
if (level instanceof ServerLevel) {
    this.registerGoals();
}
```

也就是说 **`registerGoals()` 跑在子类字段初始化之前**。所以：

- ✅ 可以在里面 `new XxxGoal(this)`、读 `static final` 常量；
- ❌ 不能在里面的 Goal 构造参数里读子类的实例字段（那时还是 `null` / `0`）。
  需要配置就传 `static final` 常量（test1 传的就是 `Test1Entity.SMASH_DISTANCE`）。

---

## 2. 写一个 Goal 的模板

```java
public class MyGoal extends Goal {

    public enum Phase { NONE, WINDUP, STRIKE, RECOVER }   // 有始有终的动作一律用状态机

    private final PathfinderMob mob;   // 需要 navigation / lookControl 就收 PathfinderMob
    private Phase phase = Phase.NONE;
    private int elapsed;
    private long nextAllowedTick;

    public MyGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));  // 只声明真正要用的
    }

    @Override
    public boolean canUse() {
        if (this.mob.level().getGameTime() < this.nextAllowedTick) return false;  // 冷却
        if (!this.mob.onGround() || this.mob.isPassenger()) return false;         // 前置状态
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && this.mob.distanceToSqr(target) <= 4.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return this.phase != Phase.NONE;        // 用状态机决定，而不是重新跑一遍 canUse
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;                            // 需要逐 tick 精确控制的动作
    }

    @Override
    public void start() {
        this.elapsed = 0;
        this.phase = Phase.WINDUP;
        this.mob.getNavigation().stop();         // 抢走 MOVE 就必须主动停掉寻路
        this.mob.setAggressive(true);
    }

    @Override
    public void tick() {
        this.elapsed++;
        // 清掉 aiStep 的移动输入，自己写位移/朝向
        this.mob.setSpeed(0.0F);
        this.mob.setZza(0.0F);
        this.mob.setXxa(0.0F);
        switch (this.phase) {
            case WINDUP -> { if (this.elapsed >= 8) this.phase = Phase.STRIKE; }
            case STRIKE -> { strike(); this.phase = Phase.RECOVER; this.elapsed = 0; }
            case RECOVER -> { if (this.elapsed >= 10) this.phase = Phase.NONE; }
            case NONE -> { }
        }
    }

    @Override
    public void stop() {
        this.phase = Phase.NONE;
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
        this.nextAllowedTick = this.mob.level().getGameTime() + 20;   // 用 gameTime 做冷却，不需要额外 tick 计数器
    }

    private void strike() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(getServerLevel(this.mob), target);  // 要元素伤害就换成 ModDamageSource
        }
    }
}
```

六个容易踩的点：

1. **`canContinueToUse()` 不要复刻 `canUse()`**。复刻的后果是目标一走出范围就立刻中止，
   动作演一半；正确做法是「只要状态机没走完就继续」（test1 的扑击在空中即使目标死了也会落回原位）。
2. **`stop()` 一定会被调用**（`GoalSelector` 在 cleanup 阶段保证），把资源释放/冷却放这里，
   不要放在「动作自然结束」的分支里，否则被打断时状态就脏了。
3. **`getServerLevel(Entity)`** 是 `Goal` 提供的 `protected static` 工具，只在服务端用（Goal 本来就只跑服务端）。
4. **冷却用 `level().getGameTime()`**，别自己数 tick —— `requiresUpdateEveryTick()` 为 false 时自数会不准。
5. **抢占 `MOVE` 之后必须 `getNavigation().stop()`**，否则导航还会继续推进目标点。
6. **`isInterruptable()` 返回 false 要慎重**：一旦返回 false，高优先级 Goal 也只能等它跑完；
   只适合「演出必须完整」的短动作。

---

## 3. 原版 Goal 清单（26.2）

下表是 `net.minecraft.world.entity.ai.goal` 包里的全部内容。**「直接用」= 不需要子类，`new` 出来 `addGoal` 就行。**

### 3.1 `target` 子包：目标选择（挂在 `targetSelector`）

| 类 | 作用 | 关键参数 | 用法 |
|---|---|---|---|
| `NearestAttackableTargetGoal<T>` | 找最近的某类生物当目标 | `(mob, Class<T>, mustSee, mustReach)` / 完整版带 `randomInterval` 与 `TargetingConditions.Selector` | ✅ 直接用，敌对小怪的标准配置 |
| `HurtByTargetGoal` | 谁打我就打谁 | `(mob, Class<?>... 忽略的伤害来源)`；`.setAlertOthers()` 呼叫同类 | ✅ 直接用 |
| `OwnerHurtByTargetGoal` / `OwnerHurtTargetGoal` | 宠物帮主人 | 需要 `TamableAnimal` | ✅ 直接用 |
| `NonTameRandomTargetGoal` | 只在未驯服时选目标 | 同上 | ✅ 直接用 |
| `NearestAttackableWitchTargetGoal` / `NearestHealableRaiderTargetGoal` | 袭击体系专用 | — | ✅ 直接用 |
| `DefendVillageTargetGoal` | 铁傀儡护村 | 需要村庄 | ✅ 直接用 |
| `ResetUniversalAngerTargetGoal` | 猪灵「全场仇恨」 | 需要 `UniversalAnger` | ✅ 直接用 |
| `TargetGoal` | 上面这些的抽象基类 | — | ❌ 抽象类 |

> 目标选择**永远不要**自己写 —— `TargetGoal` 里处理了「看不到 300 tick 后放弃」「不能打创造模式」
> 「死亡即失效」这些琐碎逻辑，重写一定漏。

### 3.2 战斗类

| 类 | 作用 | 关键参数 | 用法 |
|---|---|---|---|
| `MeleeAttackGoal` | 走位贴脸打，带寻路重算与挥臂 | `(PathfinderMob, speedModifier, followingTargetEvenIfNotSeen)` | ✅ 直接用；需要改攻击间隔/判定就子类化覆写 `resetAttackCooldown()` / `canPerformAttack()` |
| `ZombieAttackGoal` | 上面 + 抬手动画（`setAggressive`） | `(Zombie, speed, track)` | ✅ 但泛型锁 `Zombie`，非僵尸用不了 |
| `LeapAtTargetGoal` | 4~16 格内随机起跳扑向目标 | `(Mob, yd 竖直初速)`，典型 `0.4F` | ✅ 直接用，**落点不保证**，也不回原位 |
| `RangedAttackGoal` | 通用远程（需实现 `RangedAttackMob`） | `(RangedAttackMob, speed, minInterval, maxInterval, attackRadius)` | ✅ 直接用 |
| `RangedBowAttackGoal<T>` | 弓：拉弓 + 走位 | `(T, speed, attackIntervalMin, attackRadius)` | ✅ 直接用 |
| `RangedCrossbowAttackGoal<T>` | 弩：装填 + 齐射 | `(T, speed, attackRadius)` | ✅ 直接用 |
| `SpearUseGoal<T>`（26.2 新增） | 长枪：冲锋 → 交战 → 后撤循环 | `(T, 冲锋速度, 重新站位速度, 接近距离, 目标半径)` | ✅ 直接用（需要主手带 `KINETIC_WEAPON` 组件）；**这是「多阶段战斗循环」的最佳参考实现** |
| `OcelotAttackGoal` | 豹猫式：靠近 → 停顿 → 扑 | `(Mob)` | ✅ 直接用 |
| `SwellGoal` | 苦力怕膨胀引爆 | `(Creeper)` | ✅ 直接用（绑定苦力怕） |
| `RunAroundLikeCrazyGoal` | 骑手失控乱跑 | `(Mob, speed)` | ✅ 直接用 |

### 3.3 移动 / 巡逻类

| 类 | 作用 | 关键参数 | 用法 |
|---|---|---|---|
| `RandomStrollGoal` | 随机漫步（间隔默认 120 刻） | `(PathfinderMob, speed[, interval, checkNoActionTime])` | ✅ 可直接用（会游泳的生物用下面那个） |
| `WaterAvoidingRandomStrollGoal` | 漫步且避开水面 | 同上 | ✅ **不能游泳的生物就用它**（test1 用的就是它） |
| `WaterAvoidingRandomFlyingGoal` | 飞行版避水漫游 | `(PathfinderMob, speed)` | ✅ 直接用 |
| `RandomSwimmingGoal` | 水里随机游 | `(PathfinderMob, speed, interval)` | ✅ 直接用 |
| `MoveTowardsTargetGoal` | 往目标点走，带距离窗口 | `(PathfinderMob, speed, within)` | ✅ 直接用，但它是「走到目标附近的随机点」，**不是追人** |
| `MoveTowardsRestrictionGoal` | 走回「家」范围 | `(PathfinderMob, speed)` | ✅ 直接用（配合 `restrictTo`） |
| `MoveBackToVillageGoal` / `MoveThroughVillageGoal` / `StrollThroughVillageGoal` / `GolemRandomStrollInVillageGoal` | 村庄巡逻 | 各自不同 | ✅ 直接用（需要村庄/POI 体系） |
| `FollowMobGoal` | 跟着某类生物走 | `(Mob, speed, stopDistance, areaSize)` | ✅ 直接用 |
| `FollowFlockLeaderGoal` | 鱼群跟随 | `(AbstractSchoolingFish)` | ✅ 直接用 |
| `FollowParentGoal` / `BreedGoal` | 幼崽跟父母 / 繁殖 | — | ✅ 直接用 |
| `FollowOwnerGoal` | 宠物跟主人 | `(TamableAnimal, speed, min, max[, canFly])` | ✅ 直接用 |
| `FollowPlayerRiddenEntityGoal` | 跟随玩家骑着的实体 | — | ✅ 直接用 |
| `LlamaFollowCaravanGoal` | 羊驼车队 | `(Llama, speed)` | ✅ 直接用 |
| `LandOnOwnersShoulderGoal` | 鹦鹉落肩 | `(ShoulderRidingEntity)` | ✅ 直接用 |
| `SitWhenOrderedToGoal` | 坐下 | `(TamableAnimal)` | ✅ 直接用 |
| `RandomStandGoal` | 随机站起（骆驼） | `(AbstractChestedHorse)` | ✅ 直接用 |

### 3.4 环境交互 / 感知类

| 类 | 作用 | 关键参数 | 用法 |
|---|---|---|---|
| `LookAtPlayerGoal` | 盯着玩家（范围默认 8） | `(Mob, Class<T>, 距离[, 概率])` | ✅ 直接用 |
| `LookAtTradingPlayerGoal` | 盯着正在交易的玩家 | `(AbstractVillager)` | ✅ 直接用 |
| `RandomLookAroundGoal` | 随机东张西望 | `(Mob)` | ✅ 直接用 |
| `InteractGoal` / `DoorInteractGoal` / `OpenDoorGoal` | 靠近并操作门 | `(Mob[, boolean])` | ✅ 直接用 |
| `BreakDoorGoal` | 砸门（需要 `canBreakDoors`） | `(Mob, Predicate<Difficulty>)` | ✅ 直接用 |
| `RemoveBlockGoal` | 啃方块（僵尸踩龟蛋等） | `(Block, PathfinderMob, speed, verticalSearchRange)` | ✅ 直接用 |
| `EatBlockGoal` | 吃草（羊） | `(Mob)` | ✅ 直接用 |
| `TemptGoal` | 被手持物品吸引跟随 | `(PathfinderMob, speed, Ingredient, canScare)` | ✅ 直接用 |
| `AvoidEntityGoal<T>` | 躲开某类生物（猫→苦力怕） | `(PathfinderMob, Class<T>, 距离, 逃跑速度, 追击速度[, Predicate, Predicate])` | ✅ 直接用，最灵活的「恐惧」实现 |
| `PanicGoal` | 受伤逃跑 | `(PathfinderMob, speed[, 伤害来源 Predicate])` | ✅ 直接用；`PathfinderMob#isPanicking()` 能查状态 |
| `RestrictSunGoal` | 白天躲太阳 | `(PathfinderMob)` | ✅ 直接用 |
| `FleeSunGoal` | 白天找阴凉 | `(PathfinderMob, speed)` | ✅ 直接用 |
| `MoveToBlockGoal` | 走到指定方块附近 | `(PathfinderMob, speed, searchRange, verticalRange)` | ✅ 直接用（起点，常被继承） |
| `CatSitOnBlockGoal` / `CatLieOnBedGoal` | 猫坐箱子/躺床 | — | ✅ 直接用 |
| `UseItemGoal<T>` | 满足条件时用物品 | `(Mob, ItemStack, SoundEvent, Predicate)` | ✅ 直接用 |
| `OfferFlowerGoal` / `BegGoal` | 铁傀儡献花 / 狼讨食 | — | ✅ 直接用 |
| `TradeWithPlayerGoal` | 村民交易 | — | ✅ 直接用 |

### 3.5 水中 / 特殊位移

| 类 | 作用 | 关键参数 | 用法 |
|---|---|---|---|
| `FloatGoal` | 浮在水面（会游泳的生物都挂 0 优先级） | `(Mob)` | ✅ **不能游泳的生物绝对不要挂它** |
| `TryFindWaterGoal` | 找不到水就往水边挪 | `(PathfinderMob)` | ✅ 直接用 |
| `BreathAirGoal` | 需要出水换气（海豚） | `(PathfinderMob)` | ✅ 直接用 |
| `DolphinJumpGoal` | 海豚跃出水面 | `(Dolphin, interval)` | ✅ 直接用 |
| `JumpGoal` | 事件驱动的跳跃（海豚/山羊起跳） | `(Mob, interval, 落地方块判定)` | ✅ 可直接用 |
| `ClimbOnTopOfPowderSnowGoal` | 爬上细雪 | `(Mob, Level)` | ✅ 直接用 |
| `PathfindToRaidGoal<T>` | 袭击者寻路去袭击点 | `(T)` | ✅ 直接用 |

### 3.6 需要子类化才划算的，以及原版真的没有的

| 类 / 缺口 | 为什么 | 常见覆写点 |
|---|---|---|
| `MeleeAttackGoal` | 改攻击间隔、改判定、加前摇 | `resetAttackCooldown()`、`canPerformAttack()`、`checkAndPerformAttack()` |
| `MoveToBlockGoal` | 加「哪些方块算目标」 | `isValidTarget()`、`getMoveToTarget()` |
| `AvoidEntityGoal` | 加「什么情况下才怕」 | 构造函数的 `Predicate` 参数通常就够，不必继承 |
| `RandomStrollGoal` | 让漫游点满足额外条件 | `getPosition()` |
| **「只接近、不出手」** | **原版没有**：`MeleeAttackGoal` 把走位和出手绑在一起 | 见 `content/entities/ai/goal/ApproachTargetGoal` |
| **「带位移的攻击演出」** | **原版没有**：`LeapAtTargetGoal` 不保证命中也不回位 | 见 `content/entities/ai/goal/LeapSmashGoal` |

### 3.7 「不能用」的：`ai.behavior` 包

`net.minecraft.world.entity.ai.behavior` 下的 100 多个类（`MeleeAttack`、`SetWalkTargetFromAttackTargetIfTargetOutOfTarget`、
`PrepareRamNearestTarget`、`LongJumpToRandomPos`……）**不是 `Goal`**，它们实现的是 `Behavior<? extends LivingEntity>`，
只能挂到 **Brain** 上（见第 4 节）。看到名字很像也不要在 `goalSelector` 里 `addGoal`。

---

## 4. 另一套体系：Brain + Behavior

这一节讲**另一套完全不同的 AI 框架**。它不是 Goal 的替代品，而是一套「**黑板 + 日程 + 行为包**」的架构。
如果你只想改本 MOD 的怪，第 4.14 节有结论可以直接跳过；但**想真正吃透原版实体的行为逻辑，这一节才是主菜**。

> ⚠️ **26.2 的重大变化**：网上所有教程里的 `Schedule` / `ScheduleBuilder.villagerDefaulted()` **在 26.2 已经被删除**，
> 换成了 `Timeline` + `EnvironmentAttribute<Activity>`。这一节按 26.2 的真实代码写，不是照抄老教程。

### 4.1 一句话区别

| | Goal 体系 | Brain 体系 |
|---|---|---|
| 挂载点 | `Mob#goalSelector` / `targetSelector` | `LivingEntity#brain`（`Brain`） |
| 调度单元 | `Goal`（自带 `canUse`/`canContinueToUse`） | `Behavior`（**没有** `canUse`，靠「记忆」触发） |
| 状态存放 | 字段（`private int phase`） | **`MemoryModuleType` 黑板**（跨实体可存档） |
| 并发仲裁 | **`Flag` 独家占用**（`MOVE`/`LOOK`/`JUMP`/`TARGET`） | **没有仲裁**，靠「记忆的读写约定」自觉错开 |
| 状态机大档位 | 无（优先级即一切） | **`Activity`**（九个档位切换，换档 = 换一整套行为包） |
| 日程 | 自己数时间 | **`Timeline`**（按世界时钟的关键帧驱动 `Activity`） |
| 单元数量 | **71 个** Goal 类（`ai/goal/`，含 11 个 `target/` 子包） | **116 个** Behavior 类（`ai/behavior/`）+ **114 个** `MemoryModuleType` |
| 典型使用者 | 僵尸、骷髅、苦力怕、**绝大多数敌对生物** | 村民、猪灵、监守者、青蛙、山羊、嗅探兽、**26.2 新增的铜傀儡/快乐恶魂/鹦鹉螺** |
| 上手成本 | 低，所见即所得 | 高，要先理解 Memory / Activity / Timeline |
| 调试 | `getAvailableGoals()` 一看就懂 | 只有 `/data get entity` 和记忆列表 |

**最本质的一句**：Goal 是「**我能不能做**」，Brain 是「**我知道什么**」。
Goal 体系里，「知道什么」是散落在实体字段和 sensing 里的；Brain 体系把它全部抽成一张**显式的黑板**，行为只声明「我需要黑板上有哪些值」。

### 4.2 两套体系可以并存

这一点很多人没意识到：**`Brain` 就挂在 `LivingEntity` 上，和 `goalSelector` 是并列的两套东西**，
两者**每 tick 都会跑**。挂接点如下（`Mob#serverAiStep`，见 1.2）：

```
sensing.tick()              ← Goal 体系的感知（Sensing）
targetSelector.tick()       ← Goal 体系
goalSelector.tick()         ← Goal 体系
navigation.tick()
customServerAiStep()        ← ★ Brain 体系在这里跑：Villager 覆写它并调用 getBrain().tick(level, this)
moveControl.tick()          ← 两套体系共用
lookControl.tick()          ← 两套体系共用
jumpControl.tick()          ← 两套体系共用
```

村民的 `customServerAiStep`（`Villager.java` 第 238 行）只有一句核心代码：

```java
this.getBrain().tick(level, this);
```

**所以 Brain 不是「另一条 AI 管线」，而是「占用了 `customServerAiStep` 这个钩子的一套逻辑」**，
走的是同一个移动层（`navigation` / `moveControl` / `lookControl`）。
反过来说：**一个实体同时挂 Goal 和 Brain 是合法的**，只是没人这么干（会互相打架）。

### 4.3 Brain 的三张表

`Brain<E>`（`Brain.java` 第 38–49 行）本身非常简单，就是个容器，内部只有三个 `Map`：

```java
private final Map<MemoryModuleType<?>, MemorySlot<?>> memories = Maps.newHashMap();
private final Map<SensorType<? extends Sensor<? super E>>, Sensor<? super E>> sensors = Maps.newLinkedHashMap();
private final Map<Integer, Map<Activity, Set<BehaviorControl<? super E>>>> availableBehaviorsByPriority = Maps.newTreeMap();
```

| 表 | 结构 | 含义 |
|---|---|---|
| `memories` | 类型 → 槽位 | **黑板**。`MemoryModuleType` 是钥匙，`MemorySlot` 是格子 |
| `sensors` | 类型 → 实例 | 每个感知器的实例（构造时 `create()` 出来，带随机相位） |
| `availableBehaviorsByPriority` | **优先级 → 活动 → 行为集合** | 注意是**两层分组**：先按优先级，再按 `Activity` |

另外还有几个状态字段：

```java
private @Nullable EnvironmentAttribute<Activity> schedule;              // 26.2：日程是一个「环境属性」
private final Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryStatus>>> activityRequirements;  // 每个活动的准入条件
private final Map<Activity, Set<MemoryModuleType<?>>> activityMemoriesToEraseWhenStopped;        // 换档时要清的记忆
private Set<Activity> coreActivities = Sets.newHashSet();               // 永远激活的活动
private final Set<Activity> activeActivities = Sets.newHashSet();       // 当前激活 = core + 当前档
private Activity defaultActivity = Activity.IDLE;                       // 兜底档
```

**关键理解**：`availableBehaviorsByPriority` 是 `TreeMap<Integer, ...>`，所以优先级**是排序用的**，
但和 Goal 不同的是 —— 它**不是抢占式的**，见 4.4。

### 4.4 一 tick 里 Brain 做了什么

`Brain#tick`（第 384 行）只有四步，请看：

```java
public void tick(ServerLevel level, E body) {
    this.forgetOutdatedMemories();       // ① 过期记忆自动清除
    this.tickSensors(level, body);       // ② 所有感知器（各自按 scanRate 跳着跑）
    this.startEachNonRunningBehavior(level, body);  // ③ 试着启动还没跑的行为
    this.tickEachRunningBehavior(level, body);      // ④ 推进正在跑的行为
}
```

**① `forgetOutdatedMemories()`** —— 遍历所有槽位调 `MemorySlot#tick()`，把 TTL 到期的值清掉。
所以 `setMemoryWithExpiry(type, value, ttl)` 写入的记忆会**自动过期**，不需要自己数 tick。

**② `tickSensors()`** —— 每个感知器自己管自己的节奏（见 4.6）。

**③ `startEachNonRunningBehavior()`**（第 409 行）—— 这是**和 Goal 差别最大的地方**：

```java
for (每个优先级) {
    for (每个 Activity) {
        if (this.activeActivities.contains(activity)) {          // 只跑「当前激活的活动」
            for (BehaviorControl behavior : 该活动下的行为) {
                if (behavior.getStatus() == STOPPED) {           // 只试「没在跑」的
                    behavior.tryStart(level, body, time);        // ★ 全都试一遍，不是只试第一个
                }
            }
        }
    }
}
```

**没有 Flag、没有「谁抢到谁独占」**：所有属于当前活动、且当前 `STOPPED` 的行为，
**每个 tick 都会按优先级顺序被 `tryStart` 一次**，能起来的都起来。

那并发怎么办？靠两件事：

1. **`Behavior` 自己的进入条件**（`entryCondition` 里的 `MemoryStatus`），
   例如 `MoveToTargetSink` 要求 `WALK_TARGET` **存在**且 `PATH` **不存在**；
2. **组合器**（`RunOne` / `GateBehavior`）成组地做「只跑一个」。

> ⚠️ **这是最容易写错的地方**：Brain 里两个行为都想控制移动时，**不会报错、不会仲裁**，
> 结果是它们互相覆盖同一个 `WALK_TARGET` 记忆槽 —— 表现为「实体抽搐」。
> 所以 Brain 里设计行为的第一件事是想清楚：**我读哪些记忆、我写哪些记忆、别人会不会也写**。

**④ `tickEachRunningBehavior()`** —— 收集所有 `status == RUNNING` 的行为（`getRunningBehaviors()`），
逐个 `tickOrStop()`。

### 4.5 Memory：黑板

#### 4.5.1 `MemoryModuleType` 是注册表对象

`MemoryModuleType`（110+ 个）全部注册在 `BuiltInRegistries.MEMORY_MODULE_TYPE`：

```java
public static final MemoryModuleType<GlobalPos> HOME = register("home", GlobalPos.CODEC);
public static final MemoryModuleType<LivingEntity> ATTACK_TARGET = register("attack_target");
private static <U> MemoryModuleType<U> register(String name, Codec<U> codec) { ... }  // 带 codec = 可序列化
private static <U> MemoryModuleType<U> register(String name) { ... }                  // 不带 = 不存档
```

> ⚠️ **带不带 `Codec` 的差别是「存不存档」**：`Brain#pack()` 只导出 `canSerialize()` 为 true 的记忆，
> 实体卸载再加载时只有这些能恢复。所以 `HOME`（`GlobalPos`）能跨重载记住，
> 而 `ATTACK_TARGET`（`LivingEntity`）**重载就丢** —— 不是 bug。

常用记忆（挑重点，完整列表看 `MemoryModuleType.java`）：

| 分类 | 记忆 | 类型 |
|---|---|---|
| 目标 | `ATTACK_TARGET` / `INTERACTION_TARGET` / `BREED_TARGET` / `AVOID_TARGET` | `LivingEntity` 等 |
| 移动 | `WALK_TARGET` / `LOOK_TARGET` / `PATH` / `CANT_REACH_WALK_TARGET_SINCE` | `WalkTarget` / `PositionTracker` / `Path` |
| 感知结果 | `NEAREST_LIVING_ENTITIES` / `NEAREST_VISIBLE_LIVING_ENTITIES` / `NEAREST_PLAYERS` / `NEAREST_VISIBLE_PLAYER` | `List<...>` / 单个 |
| 受击 | `HURT_BY` / `HURT_BY_ENTITY` | `DamageSource` / `LivingEntity` |
| 地点 | `HOME` / `JOB_SITE` / `POTENTIAL_JOB_SITE` / `MEETING_POINT` / `HIDING_PLACE` | `GlobalPos` |
| 冷却 | `ATTACK_COOLING_DOWN` / `RAM_COOLDOWN_TICKS` / `ITEM_PICKUP_COOLDOWN_TICKS` | `Boolean` / `Integer` |
| 哨兵值 | `IS_SNIFFING` / `IS_EMERGING` / `SNIFF_COOLDOWN` / `BREEZE_SHOOT` | `Unit`（**只有「有/没有」，没有值**） |

`Unit` 这个类型很值得学：**「这个状态只要存在与否」**时用它，
比 `Boolean` 更省事（不用关心 true/false，`VALUE_PRESENT` 就是真、抹掉就是假）。

#### 4.5.2 `MemoryStatus`：三态，不是两态

```java
public enum MemoryStatus { VALUE_PRESENT, VALUE_ABSENT, REGISTERED; }
```

`Brain#checkMemory`（第 242 行）的判定：

```java
return status == REGISTERED
    || status == VALUE_PRESENT && slot.hasValue()
    || status == VALUE_ABSENT  && !slot.hasValue();
```

| 状态 | 含义 | 用途 |
|---|---|---|
| `VALUE_PRESENT` | **必须有值**，否则行为不启动 | 「我要打人」→ `ATTACK_TARGET` 必须有 |
| `VALUE_ABSENT` | **必须没有值** | 「别重复下令」→ `WALK_TARGET` 必须为空才设 |
| `REGISTERED` | **只要这个槽存在就行**（不管有没有值） | 「我要写它」→ 声明所有权，顺便让 Brain 自动注册这个槽 |

> ⚠️ **`REGISTERED` 不是「必须注册」而是「只要求注册」**。它在 `Behavior` 构造里出现，
> 意思是「这个槽我要用，请替我登记」；Brain 会自动 `registerMemory`，
> 所以**不用手动在 `provider(...)` 里列一遍**（第 87、364 行都会自动注册）。

#### 4.5.3 读写的四条 API

```java
brain.setMemory(type, value);                              // 写（写 null / 空集合 = 抹掉）
brain.setMemoryWithExpiry(type, value, ttlTicks);          // 写 + TTL（自动过期）
brain.eraseMemory(type);                                   // 抹掉
Optional<U> getMemory(type);                               // 读
boolean hasMemoryValue(type);                              // 有没有值
boolean isMemoryValue(type, value);                        // 值是不是它
```

⚠️ 有个隐藏行为：**写入空集合等于抹掉**（`isEmptyCollection`，第 450 行）。
所以「列表型记忆」不用特意调 `eraseMemory`，写一个空 `List` 就等效了。

### 4.6 Sensor：谁往黑板上写字

`Sensor<E>`（默认 `scanRate = 20`）负责**把世界写进黑板**。骨架只有三个方法：

```java
public abstract Set<MemoryModuleType<?>> requires();   // 我要写这些槽
protected abstract void doTick(ServerLevel level, E body);  // 实际扫描

public final void tick(ServerLevel level, E body) {
    if (--this.timeToTick <= 0L) {        // ★ 不是每 tick 都扫
        this.timeToTick = this.scanRate;
        this.updateTargetingConditionRanges(body);
        this.doTick(level, body);
    }
}
```

三个关键点：

1. **扫描是有节奏的**：默认 20 tick 一次，而且构造时 `randomlyDelayStart()` 给了**随机初始相位**
   （`Sensor.java` 第 40 行）—— 所以**不会所有村民在同一 tick 一起扫描**，这是刻意的性能设计。
2. **`requires()` 会连带注册记忆**：`Brain` 构造时（第 87 行）遍历所有 sensor 的 `requires()`，
   把它们声明的槽自动 `registerMemory`。所以 **`Brain.provider(...)` 里的 memoryTypes 参数基本可以留空**。
3. **检测距离来自 `FOLLOW_RANGE` 属性**：`NearestLivingEntitySensor` 用 `AABB.inflate(followRange)`，
   `PlayerSensor` 用 `closerThan(player, FOLLOW_RANGE)`。
   **所以想改 Brain 怪的视野，改属性 `FOLLOW_RANGE`，不要去改 Sensor。**

原版内置的 `SensorType`（26.2 共 23 个，`SensorType.java`）：

| 分类 | Sensor | 写什么记忆 |
|---|---|---|
| 通用 | `NEAREST_LIVING_ENTITIES` | `NEAREST_LIVING_ENTITIES`、`NEAREST_VISIBLE_LIVING_ENTITIES` |
| | `NEAREST_PLAYERS` | `NEAREST_PLAYERS`、`NEAREST_VISIBLE_PLAYER`、`NEAREST_VISIBLE_ATTACKABLE_PLAYER(S)` |
| | `NEAREST_ITEMS` | 最近地上的物品 |
| | `HURT_BY` | `HURT_BY`、`HURT_BY_ENTITY` |
| | `NEAREST_BED` | `NEAREST_BED` |
| | `IS_IN_WATER` | `IS_IN_WATER` |
| 村民专用 | `VILLAGER_HOSTILES` | `NEAREST_HOSTILE`（**每种敌人有各自的距离阈值**） |
| | `VILLAGER_BABIES` | `VISIBLE_VILLAGER_BABIES` |
| | `SECONDARY_POIS` | `SECONDARY_JOB_SITE` |
| | `GOLEM_DETECTED` | `GOLEM_DETECTED_RECENTLY` |
| 猪灵/疣猪兽 | `PIGLIN_SPECIFIC_SENSOR`、`PIGLIN_BRUTE_SPECIFIC_SENSOR`、`HOGLIN_SPECIFIC_SENSOR` | 金甲玩家、成年同类、可猎杀的疣猪兽…… |
| 动物 | `NEAREST_ADULT`、`FOOD_TEMPTATIONS`、`AXOLOTL_ATTACKABLES`、`FROG_ATTACKABLES`、`FROG_TEMPTATIONS`…… | |
| BOSS 专用 | `WARDEN_ENTITY_SENSOR`、`BREEZE_ATTACK_ENTITY_SENSOR` | |
| 兜底 | `DUMMY` | 什么都不写（`DummySensor`） |

> **自定义 Sensor 的写法**：继承 `Sensor<E>` 覆写 `requires()` + `doTick()`，
> 然后 **注册一个 `SensorType`**（`Registry.register(BuiltInRegistries.SENSOR_TYPE, id, new SensorType<>(MySensor::new))`），
> 最后在 `Brain.provider(...)` 里列出。⚠️ **`SensorType` 的构造参数是 `Supplier<U>` 工厂，不是实例** ——
> 因为每个实体要一份独立的 sensor 实例。

### 4.7 Activity：状态机的大档位

`Activity` 是**注册表对象**（`BuiltInRegistries.ACTIVITY`），26.2 有 26 个：

| Activity | 谁在用 | 含义 |
|---|---|---|
| `CORE` | 所有 Brain 怪 | **永远激活**，基础能力（游泳、开门、看人、被吓到） |
| `IDLE` | 村民等 | 默认档（`defaultActivity`） |
| `WORK` / `PLAY` / `REST` / `MEET` | 村民 | 日程四档 |
| `PANIC` / `HIDE` / `PRE_RAID` / `RAID` | 村民 | 危险与袭击 |
| `FIGHT` / `CELEBRATE` / `ADMIRE_ITEM` / `AVOID` / `RIDE` | 猪灵等 | 战斗与社交 |
| `PLAY_DEAD` / `LONG_JUMP` / `RAM` / `TONGUE` / `SWIM` / `LAY_SPAWN` | 动物 | 特殊动作（**直接对应一个技能**） |
| `SNIFF` / `INVESTIGATE` / `ROAR` / `EMERGE` / `DIG` | 嗅探兽 / 监守者 | 嗅探、咆哮、钻出、挖掘 |

**「一个 Activity 就是一个技能或一段生活状态」** —— 这是理解 Brain 的捷径。
青蛙的「舌头攻击」就是 `TONGUE` 档、山羊的「冲撞」就是 `RAM` 档。

#### 4.7.1 档位是怎么切的

```java
brain.setActiveActivityIfPossible(Activity.PANIC);   // 有条件才切，否则退回 defaultActivity
brain.setActiveActivityToFirstValid(List.of(...));   // 依次试，第一个条件满足的胜出
brain.useDefaultActivity();                          // 强制回 defaultActivity
boolean active = brain.isActive(Activity.REST);      // 行为里可以问「现在是不是这个档」
```

`setActiveActivity`（第 305 行）内部做三件事：

```java
if (!this.isActive(activity)) {                       // 已经是这个档就不动
    this.eraseMemoriesForOtherActivitesThan(activity); // ① 清掉「旧档」声明的待清记忆
    this.activeActivities.clear();
    this.activeActivities.addAll(this.coreActivities); // ② CORE 永远在
    this.activeActivities.add(activity);               // ③ 当前档
}
```

**CORE 永远激活**这一点非常重要：`VillagerPanicTrigger`（切 PANIC 的那个）本身就挂在 CORE 里，
所以**不管村民在哪个档，挨打都能立刻切到 PANIC** —— 这就是 Brain 版的「最高优先级」。

#### 4.7.2 两个容易忽略的机制

**① 准入条件（`activityRequirements`）**：`WORK` 要求 `JOB_SITE` 有值、`MEET` 要求 `MEETING_POINT` 有值。
不满足时 `setActiveActivityIfPossible` 会**退回 `defaultActivity`（IDLE）**：

```java
// Villager.java 第 147–163 行
ActivityData.create(Activity.WORK, getWorkPackage(...),
        ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT)));
ActivityData.create(Activity.MEET, getMeetPackage(...),
        ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));
```

**所以「无业游民村民白天不工作」不是特判，是准入条件没满足自动落到 IDLE。**

**② 换档清理（`memoriesToEraseWhenStopped`）**：切走时清掉旧档留下的记忆，避免污染新档。
上面 `ActivityData.create` 还有个四参重载，会**自动**把「准入条件那个记忆」加进待清列表 ——
所以 `WORK` 切走时会顺手抹掉 `JOB_SITE`。（村民确实会重新找工作点，逻辑自洽。）

#### 4.7.3 `ActivityData.create` 的优先级分配

```java
ActivityData.create(Activity.CORE, 0, 行为列表);
// → 内部 createPriorityPairs(0, list)：第一个行为优先级 0，第二个 1，第三个 2……
```

源码就一行循环（`ActivityData.java` 第 72 行）：

```java
for (BehaviorControl<? super E> behavior : behaviorList) listBuilder.add(Pair.of(nextPrio++, behavior));
```

⚠️ **注意**：`VillagerGoalPackages` 里返回的是**手写 `Pair.of(优先级, 行为)` 的列表**，
而 `getCorePackage` 里有一大堆**全是 `Pair.of(0, ...)`** ——
因为 Brain 里同优先级不是「谁先谁赢」，而是**每 tick 全部按顺序试一遍**（见 4.4）。
**所以「优先级 0」= 「每 tick 都要检查」，不是「最高优先级」。** 这是和 Goal 最反直觉的差别。

### 4.8 Timeline：26.2 新增，老教程的 Schedule 已经没了

#### 4.8.1 变化对比

| | 1.21 及以前 | **26.2** |
|---|---|---|
| 日程类型 | `Schedule`（`ScheduleBuilder.villagerDefaulted()`） | **`Timeline`**（`Registries.TIMELINE`） |
| 挂在 Brain 上的是 | `Schedule` | **`EnvironmentAttribute<Activity>`** |
| 推进方式 | `brain.updateActivityFromSchedule(level, gameTime, pos)` | `brain.updateActivityFromSchedule(level.environmentAttributes(), gameTime, pos)` |
| 怎么设 | `brain.setSchedule(Schedule.VILLAGER_DEFAULT)` | `brain.setSchedule(EnvironmentAttributes.VILLAGER_ACTIVITY)` |

**`Schedule` 和 `ScheduleBuilder` 这两个类在 26.2 的源码里已经完全不存在了**
（`world/entity/schedule/` 包里只剩一个 `Activity.java`）。

#### 4.8.2 Timeline 长什么样

`Timeline` 是「**按世界时钟的关键帧轨道表**」，注册在 `Registries.TIMELINE`。
村民的日程定义在 `Timelines.java` 第 183–206 行：

```java
int workStartTime = 2000;
int totalWorkTime = 7000;
context.register(
    VILLAGER_SCHEDULE,
    Timeline.builder(overworldClock)               // ★ 绑定到主世界时钟
        .setPeriodTicks(24000)                     // ★ 周期一天
        .addTrack(EnvironmentAttributes.VILLAGER_ACTIVITY, track ->
            track.addKeyframe(10, Activity.IDLE)
                 .addKeyframe(2000, Activity.WORK)
                 .addKeyframe(9000, Activity.MEET)
                 .addKeyframe(11000, Activity.IDLE)
                 .addKeyframe(12000, Activity.REST))
        .addTrack(EnvironmentAttributes.BABY_VILLAGER_ACTIVITY, track ->
            track.addKeyframe(10, Activity.IDLE)
                 .addKeyframe(3000, Activity.PLAY)
                 .addKeyframe(6000, Activity.IDLE)
                 .addKeyframe(10000, Activity.PLAY)
                 .addKeyframe(12000, Activity.REST))
        .build()
);
```

**同一个 `Timeline` 里有多条轨道**：`OVERWORLD_DAY` 那条注册了太阳角度、雾色、星空亮度、
「怪物会不会烧」（`MONSTERS_BURN`）、「蜜蜂会不会回巢」（`BEES_STAY_IN_HIVE`）……
**村民日程只是其中一条轨道上的一个属性**。这是 26.2 把「时间相关的一切」统一到 Timeline 的结果。

#### 4.8.3 它是怎么生效的

`Brain#updateActivityFromSchedule`（第 327 行）：

```java
public void updateActivityFromSchedule(EnvironmentAttributeSystem env, long gameTime, Vec3 pos) {
    if (gameTime - this.lastScheduleUpdate > 20L) {          // ★ 20 tick 才查一次
        this.lastScheduleUpdate = gameTime;
        Activity scheduled = this.schedule != null ? env.getValue(this.schedule, pos) : Activity.IDLE;
        if (!this.activeActivities.contains(scheduled)) {     // ★ 已经是这个档就不动
            this.setActiveActivityIfPossible(scheduled);
        }
    }
}
```

两个要点：

1. **`env.getValue(attribute, pos)`** —— 值是**按位置查询环境属性系统**得到的。
   也就是说日程理论上可以随维度/生物群系不同而不同（属性可以带位置修正）。
2. **「已经是这个档就不动」** —— 这正是 PANIC 能压住日程的原因：
   恐慌期间 `activeActivities` 里有 `PANIC` 没有 `REST`，日程想切 `REST` 会走
   `setActiveActivityIfPossible(REST)`…… 但 `REST` 没有准入条件所以会切成功？
   **不会**，因为恐慌时 `VillagerCalmDown`/`VillagerPanicTrigger` 会持续把档位按回 PANIC，
   而且真正常用的是**反向**逻辑：`UpdateActivityFromSchedule` 是**优先级 99 的兜底行为**，
   每 tick 调一次（受 20 tick 节流），只在「已经不是当前日程档」时才尝试切 —— 见 4.11.6。

#### 4.8.4 想给自定义怪做日程

```java
ResourceKey<Timeline> MY_TIMELINE = ResourceKey.create(Registries.TIMELINE, Identifier.fromNamespaceAndPath("mymod", "my_schedule"));
// 用 datapack 或 BootstrapContext 注册一个 Timeline，轨道写 EnvironmentAttribute<Activity>
brain.setSchedule(MY_ACTIVITY_ATTRIBUTE);   // 记得同时注册一个 EnvironmentAttribute<Activity>
```
**代价**：你得同时注册 `Timeline` + `EnvironmentAttribute`，还要在 datapack 里给出关键帧。
**如果只是想「白天/晚上行为不同」，自己数 `level.getGameTime()` 反而更省事。**

### 4.9 Behavior：和 Goal 逐条对照

`Behavior<E>` 是所有 Brain 行为的最小基类（`Behavior.java`，113 行）。

#### 4.9.1 生命周期

```java
tryStart()   : hasRequiredMemories() && checkExtraStartConditions() → RUNNING
               ★ 同时抽一个随机时长 duration ∈ [minDuration, maxDuration]
tickOrStop() : if (!timedOut(timestamp) && canStillUse(...)) tick(); else doStop();
doStop()     : status = STOPPED; stop();
```

方法对照表（**左边是 Goal，右边是 Behavior**）：

| Goal | Behavior | 说明 |
|---|---|---|
| `canUse()` | `entryCondition`（**声明式，不是方法**） | 用 `Map<MemoryModuleType, MemoryStatus>` 声明前置记忆 |
| — | `checkExtraStartConditions(level, body)` | 额外的代码判断（**默认 `true`**） |
| `start()` | `start(level, body, timestamp)` | |
| `tick()` | `tick(level, body, timestamp)` | |
| `canContinueToUse()` | `canStillUse(level, body, timestamp)` | ⚠️ **默认 `false`！** |
| `stop()` | `stop(level, body, timestamp)` | |
| `isInterruptable()` | **不存在** | Brain 没有「打断」概念，是靠换 Activity 换整包 |
| `getFlags()` | **不存在** | 没有 Flag 仲裁，见 4.4 |
| — | `timedOut(timestamp)` | **Goal 没有的新东西**，默认 `timestamp > endTimestamp` |

#### 4.9.2 三个必须记住的坑

> ⚠️ **坑 1：`canStillUse` 默认 `false`。**
> 也就是说：**一个不覆写 `canStillUse` 的 `Behavior`，跑完一段随机时长后就会自己停**。
> 不覆写它 = 一次性行为（这正是 `OneShot` 的语义）。

> ⚠️ **坑 2：`Behavior` 有超时，默认 60 tick。**
> 构造函数 `Behavior(entryCondition)` → `this(entryCondition, 60)`，
> 然后 `duration = min + random.nextInt(max + 1 - min)`。
> **所以要「一直跑」的行为必须显式把 `timedOut` 覆写成 `return false`。**
> 原版范例 `SleepInBed`（第 99 行）就是：
> ```java
> @Override protected boolean timedOut(long timestamp) { return false; }
> ```
> 不然村民会每 60 tick 醒一次。

> ⚠️ **坑 3：`checkExtraStartConditions` 默认 `true`，`entryCondition` 空 `Map` = 无条件。**
> `VillagerPanicTrigger` 就是 `super(ImmutableMap.of())`（无条件），
> 真正的判断全塞在 `start()` 里。**能无条件启动，是很强的写法，要谨慎用。**

#### 4.9.3 `Behavior` 的两种子类型

| 类型 | 语义 | 例子 |
|---|---|---|
| `Behavior<E>` | 有持续时间的「状态」 | `SleepInBed`、`MoveToTargetSink`、`LookAtTargetSink` |
| `OneShot<E>` | **启动一次，下一 tick 立刻停** | `SetWalkTargetFromBlockMemory`、`AcquirePoi` |

`OneShot`（39 行）的实现极简：

```java
public final boolean tryStart(...) { if (this.trigger(level, body, timestamp)) { status = RUNNING; return true; } ... }
public final void tickOrStop(...) { this.doStop(level, body, timestamp); }   // ★ 下一 tick 直接停
```

所以 **「设定一个目标就结束」的行为一律用 `OneShot`**，例如
`SetWalkTargetFromBlockMemory`（设完 `WALK_TARGET` 就停）、`WorkAtPoi`。

### 4.10 组合器：把多个行为打包

#### 4.10.1 `RunOne` —— 加权随机选一个

```java
new RunOne<>(ImmutableList.of(
    Pair.of(workAtPoi, 7),                    // ★ 第二个参数是「权重」，不是优先级！
    Pair.of(StrollAroundPoi.create(...), 2),
    Pair.of(StrollToPoi.create(...), 5),
    Pair.of(new DoNothing(20, 40), 2)))
```

`RunOne`（20 行）就是 `GateBehavior` 的预设：

```java
super(entryCondition, ImmutableSet.of(), OrderPolicy.SHUFFLED, RunningPolicy.RUN_ONE, weightedBehaviors);
```

`ShufflingList`（第 71 行）的排序键是：

```java
this.randWeight = -Math.pow(random, 1.0F / this.weight);   // 权重越大越靠前，但带随机性
```

**所以权重 7 的 `workAtPoi` 只是「最可能被选中」，不是「一定」** —— 这正是村民「干一会儿活、走一会儿神」的来源。

#### 4.10.2 `GateBehavior` —— 组合器的基类

```java
new GateBehavior<>(
    ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT),   // 进入条件
    ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET),                 // ★ 退出时清哪些记忆
    GateBehavior.OrderPolicy.ORDERED,                                     // 顺序固定 / 每次洗牌
    GateBehavior.RunningPolicy.RUN_ONE,                                   // 只起一个 / 全起
    ImmutableList.of(Pair.of(new TradeWithVillager(), 1)));
```

`GateBehavior` 的三个机制（第 68–92 行）：

1. **`tryStart` 里对整包做一次选择**：`orderPolicy.apply` 排序 → `runningPolicy.apply` 决定起几个。
   `RUN_ONE` = 起第一个成功的；`TRY_ALL` = 全都试。
2. **`tickOrStop` 只推进 `RUNNING` 的子行为**；**子行为全停了，自己就停**。
3. **`doStop` 会把子行为全停掉，并清空 `exitErasedMemories`** —— 这就是把
   「交易结束后清掉 `INTERACTION_TARGET`」这件事声明化的写法。

#### 4.10.3 `TriggerGate` 与 `BehaviorBuilder`（声明式 DSL）

```java
// 村民袭击包：先判断「袭击在进行」，再走后面的摇签
Pair.of(0, BehaviorBuilder.sequence(
        BehaviorBuilder.triggerIf(VillagerGoalPackages::raidExistsAndNotVictory),
        TriggerGate.triggerOneShuffled(ImmutableList.of(
            Pair.of(MoveToSkySeeingSpot.create(speedModifier), 5),
            Pair.of(VillageBoundRandomStroll.create(speedModifier * 1.1F), 2)))))
```

`BehaviorBuilder` 是一套**声明式构建器**，`i.present(记忆)` / `i.absent(记忆)` / `i.registered(记忆)`
对应上面三个 `MemoryStatus`，编译期就把前置条件写进行为。
`AcquirePoi`（180 行）是它的典型用法：

```java
OneShot<PathfinderMob> acquirePoi = BehaviorBuilder.create(
    i -> i.group(i.absent(memoryToAcquire))
          .apply(i, toAcquire -> (level, body, timestamp) -> {
              // 节流：不在 20 tick 内重复寻路
              if (level.getGameTime() < nextScheduledStart.longValue()) return false;
              // 找 POI → 建路径 → 走得到就 take() 并写记忆
          }));
```

它内部甚至带了一个 `JitteredLinearRetry`（第 140 行，**延迟从 40 起步、每次 +40、上限 400**）
来避免村庄里所有村民一起对同一个 POI 疯狂寻路。
**这就是 Brain 体系的真实复杂度：机制不难，难在到处是这种性能保护。**

### 4.11 案例研究：村民的一天（完整拆解）

选村民是因为它是 Brain 的**教科书用例**：9 个 Activity、完整的日程、POI 记忆、
两条主线（作息 + 恐慌）。读完这一段，Brain 就吃透了。

#### 4.11.1 它的大脑由什么组成

`Villager.java` 第 129–172 行：

```java
private static final Brain.Provider<Villager> BRAIN_PROVIDER = Brain.<Villager>provider(
    List.of(                                     // ★ 9 个感知器
        SensorType.NEAREST_LIVING_ENTITIES,      // 附近生物
        SensorType.NEAREST_PLAYERS,              // 附近玩家
        SensorType.NEAREST_ITEMS,                // 附近物品
        SensorType.NEAREST_BED,                  // 附近的床（NEAREST_BED 记忆）
        SensorType.HURT_BY,                      // 谁打的我
        SensorType.VILLAGER_HOSTILES,            // 附近敌人（分类型有不同距离）
        SensorType.VILLAGER_BABIES,              // 附近小孩
        SensorType.SECONDARY_POIS,               // 次要 POI（农田等）
        SensorType.GOLEM_DETECTED),              // 附近有没有铁傀儡
    body -> {                                    // ★ ActivitySupplier：按实体状态现造活动表
        Holder<VillagerProfession> profession = body.getVillagerData().profession();
        List<ActivityData<Villager>> activities = new ArrayList<>();
        if (body.isBaby()) {
            activities.add(ActivityData.create(Activity.PLAY, getPlayPackage(0.5F)));   // 小孩只玩
        } else {
            activities.add(ActivityData.create(Activity.WORK, getWorkPackage(profession, 0.5F),
                    ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT))));
        }
        activities.add(ActivityData.create(Activity.CORE,   getCorePackage(profession, 0.5F)));
        activities.add(ActivityData.create(Activity.MEET,   getMeetPackage(0.5F),
                ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT))));
        activities.add(ActivityData.create(Activity.REST,   getRestPackage(0.5F)));
        activities.add(ActivityData.create(Activity.IDLE,   getIdlePackage(0.5F)));
        activities.add(ActivityData.create(Activity.PANIC,  getPanicPackage(0.5F)));
        activities.add(ActivityData.create(Activity.PRE_RAID, getPreRaidPackage(0.5F)));
        activities.add(ActivityData.create(Activity.RAID,   getRaidPackage(0.5F)));
        activities.add(ActivityData.create(Activity.HIDE,   getHidePackage(0.5F)));
        return activities;
    });
```

三个值得学的点：

1. **`ActivitySupplier` 是回调**（`body -> ...`）：**每个实体构造时现算自己的一套活动表**。
   所以「小孩只玩、大人工作」不是运行时 if，而是**小孩的脑子里压根没有 WORK 这一档**。
2. **感知器列表是「这个生物能知道什么」的完整清单** —— 村民没有 `IS_IN_WATER` 以外的战斗感知，
   所以它**永远不可能主动攻击**：黑板上根本没有「可以打的目标」这个槽。
3. **`profession` 是从实体身上读的**，所以**职业会影响行为包内容**（农民用 `WorkAtComposter`，其他人用 `WorkAtPoi`）。

#### 4.11.2 三个入口方法

```java
@Override protected Brain<Villager> makeBrain(Brain.Packed packedBrain) {
    Brain<Villager> brain = BRAIN_PROVIDER.makeBrain(this, packedBrain);  // ★ 从存档恢复黑板
    this.registerBrainGoals(brain);                                       // ★ 设日程 + 立即定位档位
    return brain;
}

private void registerBrainGoals(Brain<Villager> brain) {
    if (this.isBaby()) brain.setSchedule(EnvironmentAttributes.BABY_VILLAGER_ACTIVITY);
    else               brain.setSchedule(EnvironmentAttributes.VILLAGER_ACTIVITY);
    brain.updateActivityFromSchedule(this.level().environmentAttributes(), this.level().getGameTime(), this.position());
}

public void refreshBrain(ServerLevel level) {           // 长大成人时重建整个大脑
    Brain<Villager> oldBrain = this.getBrain();
    oldBrain.stopAll(level, this);                       // ★ 先把所有行为停干净
    this.brain = BRAIN_PROVIDER.makeBrain(this, oldBrain.pack());  // ★ 带着记忆重造
    this.registerBrainGoals(this.getBrain());
}
```

> ⚠️ **`refreshBrain` 是 Brain 体系独有的需求**：因为「活动表」是构造时算好写死的，
> **小孩长大了要换一整套活动表，只能整个 Brain 重建**。
> 注意它是 `oldBrain.pack()` 保留记忆的 —— 不然长大就失忆了。
> 触发点是 `ageBoundaryReached()`（第 222 行）。

#### 4.11.3 时间线：村民一天到底在干什么

把 `Timelines.VILLAGER_SCHEDULE` 的关键帧翻译成时间（Minecraft 里 `0` 刻 = 06:00）：

| 刻 | 现实时间 | Activity | 村民在干什么 |
|---|---|---|---|
| 10 | 06:00 | `IDLE` | 起床后先待机 |
| 2000 | 08:00 | `WORK` | 去自己的**工作点**（`JOB_SITE`），农民去堆肥桶 |
| 9000 | 15:00 | `MEET` | 去**集会点**（`MEETING_POINT`，通常是钟）社交 |
| 11000 | 17:00 | `IDLE` | 闲逛 |
| 12000 | 18:00 | `REST` | 回**家**（`HOME`）睡觉 |

小孩版（`BABY_VILLAGER_ACTIVITY`）：`10 IDLE → 3000 PLAY → 6000 IDLE → 10000 PLAY → 12000 REST`。

再加上**不在时间线里的三个档**：

```
PANIC    ← 挨打或看到敌人时插播（CORE 里的 VillagerPanicTrigger 触发）
PRE_RAID ← 袭击前的 30 秒准备期
RAID     ← 袭击进行中
HIDE     ← 躲藏（袭击胜利后 / 特定情况）
```

**这就是村民「看起来活生生」的全部秘密** —— 九个档位 + 五条时间线关键帧。

#### 4.11.4 逐条读 CORE 包（`getCorePackage`）

CORE 是**永远激活**的档，所以它装的是「任何时候都该做的事」：

| 优先级 | Behavior | 干什么 |
|---|---|---|
| 0 | `Swim(0.8F)` | 掉水里就往上浮 |
| 0 | `InteractWithDoor` | 开关门（村民能开门，`navigation.setCanOpenDoors(true)`） |
| 0 | `LookAtTargetSink(45, 90)` | **`LOOK_TARGET` 记忆 → `lookControl.setLookAt()`** |
| 0 | `VillagerPanicTrigger` | **挨打/看到敌人 → 切 `PANIC` 档** |
| 0 | `WakeUp` | 早上该起了 |
| 0 | `ReactToBell` | 听到钟声的反应 |
| 0 | `SetRaidStatus` | 维护袭击状态 |
| 0 | `ValidateNearbyPoi` ×2 | 已有的工作点还合法吗，不合法就抹掉记忆 |
| 1 | `MoveToTargetSink` | **`WALK_TARGET` 记忆 → `navigation.moveTo(路径)`** |
| 2 | `PoiCompetitorScan` | 抢工作点 |
| 3 | `LookAndFollowTradingPlayerSink` | 交易时盯着玩家 |
| 5 | `GoToWantedItem` | 去捡想捡的物品 |
| 6 | `AcquirePoi(JOB_SITE)` | **没工作点 → 扫描 48 格找一个** |
| 7 | `GoToPotentialJobSite` | 去看看候选工作点 |
| 8 | `YieldJobSite` | 把工作点让给需要的人 |
| 10 | `AcquirePoi(HOME)` / `AcquirePoi(MEETING_POINT)` / `AssignProfessionFromJobSite` / `ResetProfession` | 找床位、找集会点、按工作点定职业、重置职业 |

**看这两行，就明白 Brain 的移动是怎么发生的**：

```java
Pair.of(0, new LookAtTargetSink(45, 90)),   // LOOK_TARGET → lookControl
Pair.of(1, new MoveToTargetSink()),         // WALK_TARGET → navigation
```

`LookAtTargetSink`（25 行）核心：

```java
protected void tick(...) {
    body.getBrain().getMemory(MemoryModuleType.LOOK_TARGET)
        .ifPresent(target -> body.getLookControl().setLookAt(target.currentPosition()));
}
```

`MoveToTargetSink`（第 93 行）：

```java
body.getBrain().setMemory(MemoryModuleType.PATH, this.path);
body.getNavigation().moveTo(this.path, this.speedModifier);
```

> **这两个「Sink（下沉）」是 Brain 与移动层的唯一桥梁**：
> 所有其他行为都**不直接操作 `navigation`**，只往 `WALK_TARGET` 里写一个「我想去哪」；
> 由 `MoveToTargetSink` 统一算路径并下发给 `navigation`。
> **这是 Brain 体系里最漂亮的设计** —— 「下决定」和「执行移动」彻底解耦，
> 所以 `SetWalkTargetFromBlockMemory` / `VillageBoundRandomStroll` / `SetWalkTargetFromLookTarget`
> 这些「下决定」的行为都只写一个记忆槽，几行就写完了。
>
> **对照第 5 节**：Goal 体系里 `MoveToTargetSink` 的对应物是 `Goal` 里的 `nav.moveTo(...)` 直接调用 ——
> Goal 是「我说了算」，Brain 是「我说完就走，谁执行我不管」。

#### 4.11.5 链路一：一个村民怎么找到床并睡下

这条链路把 POI、记忆、行为、日程全串起来了，值得逐步跟一遍。

```
18:00，Timeline 关键帧把 EnvironmentAttributes.VILLAGER_ACTIVITY 变成 Activity.REST
  ↓
UpdateActivityFromSchedule（优先级 99 的兜底行为）每 tick 调 updateActivityFromSchedule()
  ↓ env.getValue(VILLAGER_ACTIVITY, pos) = REST
  ↓ setActiveActivityIfPossible(REST)  → REST 无准入条件 → 切档成功
activeActivities = {CORE, REST}          ← 此时只有 CORE 和 REST 的行为会被 tryStart
  ↓
【REST 包，优先级 2】SetWalkTargetFromBlockMemory.create(HOME, 0.5F, 1, 150, 1200)
  ↓ 前置：HOME 存在 + WALK_TARGET 为空 + CANT_REACH_WALK_TARGET_SINCE 已注册
  ↓ 写 WALK_TARGET(HOME 坐标, 速度 0.5, 靠近到 1 格)，然后 OneShot 立刻停
  ↓
【CORE 包，优先级 1】MoveToTargetSink
  ↓ 前置：WALK_TARGET 存在 + PATH 为空
  ↓ 算路径 → navigation.moveTo(path, speed) → 把 PATH 写回记忆 → 一直 tick 直到到达
  ↓ 到达 → 抹掉 WALK_TARGET 和 CANT_REACH_WALK_TARGET_SINCE（第 59–61 行）
  ↓
【REST 包，优先级 3】SleepInBed
  ↓ 前置：HOME 必须有值（entryCondition）
  ↓ checkExtraStartConditions：
  ↓   ① 不能是乘客  ② 床位坐标必须在 2 格内且是没人的床
  ↓   ③ 刚被叫醒（LAST_WOKEN）后 100 tick 内不睡
  ↓ canStillUse：还在 REST 档 && 站在床的上方 && 距离床中心 1.14 格内
  ↓ start()：body.startSleeping(床坐标)，写 LAST_SLEPT，抹掉 WALK_TARGET
  ↓ timedOut() → false（永不超时，一直睡到 canStillUse 变 false）
```

**这条链路里有三处设计值得记住**：

1. **`HOME` 记忆不是自动有的** —— 是 CORE 包里优先级 10 的
   `AcquirePoi.create(p -> p.is(PoiTypes.HOME), HOME, false, Optional.of((byte)14), VillagerGoalPackages::validateBedPoi)`
   在平时（任何档位，因为 CORE 永远激活）偷偷扫描 POI 存下来的。
   **所以「村民记住自己的床」= 一个常驻行为往黑板上写了一个 `GlobalPos`。**
2. **`ValidateNearbyPoi`（优先级 3，REST 包）会在床被占用/被拆时抹掉 `HOME` 记忆** ——
   抹掉之后 `SleepInBed` 的 `entryCondition` 不满足，自动不睡；
   而 `HOME` 为空又会触发 REST 包里优先级 5 的 `RunOne`（条件正是 `HOME: VALUE_ABSENT`）：
   `SetClosestHomeAsWalkTarget` / `InsideBrownianWalk` / `GoToClosestVillage` / `DoNothing`
   —— **「今晚没床睡」的分支是靠记忆的「有/无」自然分出来的，没有一行 if。**
3. **`SleepInBed` 覆写了 `timedOut → false`** —— 这是它和默认 `Behavior` 的唯一区别，
   也是「能一直跑」的必要条件（见 4.9.2 坑 2）。

#### 4.11.6 链路二：村民被打之后发生了什么

```
玩家揍了村民一下
  ↓
HurtBySensor（scanRate 默认 20）把伤害写进黑板
  ↓ HURT_BY = DamageSource；HURT_BY_ENTITY = 攻击者
  ↓
【CORE 包，优先级 0】VillagerPanicTrigger.tryStart()
  ↓ 它 super(ImmutableMap.of()) —— 无进入条件，每 tick 都在试
  ↓ start()：if (isHurt(body) || hasHostile(body))
  ↓   抹掉 PATH / WALK_TARGET / LOOK_TARGET / BREED_TARGET / INTERACTION_TARGET  ★ 清场
  ↓   brain.setActiveActivityIfPossible(Activity.PANIC)
activeActivities = {CORE, PANIC}          ← 档位换了，整套行为包换掉
  ↓
【PANIC 包，优先级 0】VillagerCalmDown
  ↓ 三个记忆都 REGISTERED，逻辑：只要不害怕了就
  ↓   erase(HURT_BY) + erase(HURT_BY_ENTITY) + updateActivityFromSchedule()  ★ 手动回到日程
  ↓
【PANIC 包，优先级 1】SetWalkTargetAwayFrom.entity(NEAREST_HOSTILE, 速度×1.5, 6, false)
                     SetWalkTargetAwayFrom.entity(HURT_BY_ENTITY, 速度×1.5, 6, false)
  ↓ 背对着敌人跑，速度是平时的 1.5 倍
  ↓ （这两个又是「只写 WALK_TARGET 记忆」，执行还是 MoveToTargetSink 干的）
  ↓
【CORE 包，优先级 1】MoveToTargetSink → navigation.moveTo(逃跑路径)
```

**两个关键设计和 Goal 体系的对比**：

- **「切换活动」代替了「抢占 Flag」**：Goal 里「逃跑」要靠 `getFlags()` 抢 `MOVE`，
  抢不到就站着挨打；Brain 里直接**换掉整个活动包**，旧包的行为连 `tryStart` 的机会都没有。
  **哪个更强？Goal 更细，Brain 更粗暴但更可靠。**
- **`VillagerCalmDown` 要手动 `updateActivityFromSchedule`**：
  因为日程更新有 20 tick 节流，而且「不害怕了」这件事没有对应的传感器去写记忆 ——
  所以**恢复档位必须自己动手**。这是 Brain 体系里「谁来退档」这个问题的标准答案：
  **在最高优先级放一个「条件不满足就退档」的行为**（`VillagerCalmDown` 就是干这个的）。

#### 4.11.7 为什么每个包最后都有一个优先级 99 的 `UpdateActivityFromSchedule`

回头数一下 `VillagerGoalPackages`：**WORK、PLAY、REST、MEET、IDLE 五条包的末尾全是**：

```java
Pair.of(99, UpdateActivityFromSchedule.create())
```

它每 tick 执行 `body.getBrain().updateActivityFromSchedule(...)`。
**这是「让日程能正常轮转」的引擎** —— 如果没有它，切进 REST 之后 Brain 再也不会主动查日程，
村民会永远睡下去（除了 PANIC 能插播）。

> ⚠️ **所以给自定义 Brain 怪做日程时，别忘了在包里加这个兜底行为**。
> 而 PANIC / PRE_RAID / RAID / HIDE 四条包**故意没有**它 —— 因为这四个档就该「赖着不走，
> 由别的行为负责退档」。**「有不有这一行」本身就是一种设计表达。**

### 4.12 自己写一个 Brain 怪：最小骨架

```java
public class MyMob extends PathfinderMob {
    private static final Brain.Provider<MyMob> BRAIN_PROVIDER = Brain.provider(
        List.of(SensorType.NEAREST_PLAYERS, SensorType.HURT_BY),   // ① 感知器
        mob -> List.of(                                            // ② 活动表
            ActivityData.create(Activity.CORE, 0, ImmutableList.of(
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink())),
            ActivityData.create(Activity.IDLE, 0, ImmutableList.of(
                VillageBoundRandomStroll.create(0.6F),
                Pair.of(99, UpdateActivityFromSchedule.create())))
        ));

    @Override protected Brain<MyMob> makeBrain(Brain.Packed packedBrain) {
        Brain<MyMob> brain = BRAIN_PROVIDER.makeBrain(this, packedBrain);
        brain.setSchedule(EnvironmentAttributes.VILLAGER_ACTIVITY);   // 不想做日程可以省掉
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));      // 默认就已经是 CORE
        brain.setDefaultActivity(Activity.IDLE);                      // 默认就是 IDLE
        return brain;
    }

    @Override protected void customServerAiStep(ServerLevel level) {
        this.getBrain().tick(level, this);      // ★ 唯一的必须动作
        super.customServerAiStep(level);
    }
}
```

⚠️ 注意 `ActivityData.create(activity, 优先级起点, 行为列表)` 会把列表里的行为**依次编号**，
所以上面 CORE 包里 `LookAtTargetSink` 得 0、`MoveToTargetSink` 得 1。

**如果只是想快速实验**，直接抄原版现成的包最省事（`PiglinAi`、`FrogAi`、`GoatAi` 都是独立文件，很好读）。

### 4.13 Brain 的调试手段

Brain 比 Goal 难调，因为它没有「当前在跑哪个 Goal」这种一目了然的东西。四个手段：

1. **`/data get entity @e[type=villager,limit=1] Brain`** —— 直接看黑板上所有记忆。
   **这是最有效的**：看到 `HOME` 为空，就知道它为什么不去睡觉。
2. **`brain.getActiveActivities()`**（`@VisibleForDebug`）—— 看当前在哪个档。
3. **`brain.getRunningBehaviors()`**（`@VisibleForDebug`）—— 看正在跑哪些行为。
   `Goal#toString()` 的等价物是 `BehaviorControl#debugString()`，`GateBehavior` 会打印出
   `RunOne: [WorkAtPoi]` 这种「组合器 + 当前选中的子行为」。
4. **`brain.isBrainDead()`** —— `memories`、`sensors`、`behaviors` 全空 = 这个实体压根没脑子。
   用它来排查「我以为注册了但其实没生效」。

还有一个非调试但很实用的 API：**`brain.checkMemory(type, status)`** ——
在外部代码（比如 `mobInteract`）里判断「这个村民现在知不知道有床」，比读 `Optional` 更直白。

### 4.14 结论：本 MOD 的怪物还是用 Goal

回到实用层面。两套体系的取舍：

| 需求 | 选哪个 |
|---|---|
| 追人、近战、远程、逃跑、巡逻 | **Goal**（原版都有现成的，改数值即可） |
| 一个「跳出→撞击→跳回」的动作编排 | **Goal**（`LeapSmashGoal`，一个文件搞定） |
| 靠「记忆」驱动的复杂日程（作息、上班、社交） | Brain + Timeline |
| 需要「知道 8 种东西、按组合决定做什么」的 BOSS（监守者） | Brain |
| 实体状态要跨存档保留（记住家在哪、记住谁打我） | Brain 的黑板（Goal 也能做，但得自己写 NBT） |

**本 MOD 的答案**：
第 7 节的 test1 / test2 / 大型冰史莱姆**全部用 Goal**，理由：

1. 它们的逻辑是「**满足一个条件 → 放一个技能**」，本来就是 Goal 的形状；
2. Goal 有 `Flag` 仲裁，**技能之间不会互相抢移动**（Brain 里得自己设计记忆隔离）；
3. Goal 调试成本极低（`getAvailableGoals()` + `isRunning()`）；
4. 本项目自己的元素/护盾/削韧体系（`StatusContainer`、`ShieldService`）都挂在实体的
   **Data Attachment** 上，和「Brain 用记忆存状态」是两套并行的状态管理 ——
   **再引入 Brain 等于一个实体上跑三套状态**，得不偿失。

**但如果要做「村民 AI 级别的 NPC」或「有记忆的 BOSS」，Brain + Timeline 才是正路**，
这一节的 4.11 案例就是可以直接照抄的模板。

---

## 5. 移动层：Goal 之外的四个控制器

`Goal` 说「去哪」，下面四个控制器说「怎么去」。**改「走法」不要写 Goal，改 MoveControl。**

| 控制器 | 字段 | 干什么 | 什么时候动它 |
|---|---|---|---|
| `PathNavigation` | `navigation` | A\* 寻路，给 MoveControl 下「目标点」 | 换飞行/游泳/攀爬导航；`setCanFloat(false)` 关漂浮；`setSpeedModifier` |
| `MoveControl` | `moveControl` | 把「目标点」变成每 tick 的速度与朝向 | **改走法**（蠕动、冲刺、滑行）；test1 的 `WriggleMoveControl` 就在这层 |
| `JumpControl` | `jumpControl` | 起跳（`jump()`） | 一般不动；换「什么时候起跳」的节奏改 MoveControl（如 test1 的小跳） |
| `LookControl` | `lookControl` | 头/身体转向 | 一般不动；Goal 里用 `mob.getLookControl().setLookAt(...)` |

替换 MoveControl 只需在构造函数里赋值（`Mob#moveControl` 是 `protected`）：

```java
public Test1Entity(EntityType<? extends TeyvatMonster> type, Level level) {
    super(type, level);
    this.moveControl = new WriggleMoveControl<Test1Entity>(this);  // 换掉默认 MoveControl
    this.navigation.setCanFloat(false);                            // 不能游泳
    this.setPathfindingMalus(PathType.WATER, -1.0F);               // 寻路层面直接禁掉水面
}
```

「不能飞」不需要做什么 —— 只要不 `createNavigation` 成 `FlyingPathNavigation` 就永远不会飞。
「不能游泳」要做三件事：不挂 `FloatGoal`、`setCanFloat(false)`、把 `PathType.WATER` 的寻路代价设为 `-1`。

---

## 6. 本 MOD 的落地约定

| 约定 | 位置 | 说明 |
|---|---|---|
| 怪物基类 | `content/entities/teyvat/monster/TeyvatMonster` | 继承 `Monster` 并实现 `TeyvatHostile` |
| 元素/等级/战斗计时 | `TeyvatLiving`（`TeyvatHostile` 的上层接口） | `getEntityStats()`、`resetCombat()`、`isInCombat()` |
| 战斗计时自动维护 | `content/entities/ai/CombatTimerHandler` | 全局事件：受伤/换目标时自动 `resetCombat()`，不需要每个实体自己写 |
| 伤害源 | `ModDamageSource.from(spec, causer)` | ⚠️ **只适用于角色攻击者**。怪物用它会打出 0 伤害，见第 8 节最后两行 |
| 怪物伤害 | `target.hurtServer(level, mob.damageSources().mobAttack(mob), 数值)` | 非角色攻击者的正确写法；数值取自 `ATTACK_DAMAGE` |
| 伤害规格 | `ModDamageSpec.builder(AttackType, GenshinElement)` | 怪物近战照 `TeyvatSlime#dealDamage` 的写法 |
| 动画状态同步 | `TestMonster` 的 `DATA_ACTION` | `Mob#getTarget()` 不同步到客户端，动画必须依赖同步数据 |
| 资源 | `entity/<id>/<id>.{json,animation.json,png}` | 见第 9 节 |

**动画状态的同步是新手最容易卡住的点**：客户端 `mob.getTarget()` 恒为 `null`
（原版只同步 `DATA_MOB_FLAGS_ID` 里的 `aggressive/noAI/leftHanded`），
所以「有目标就播 X 动画」必须写成一个同步的 int/enum，再在动画控制器里读它。

---

## 7. 范例拆解（test1 / test2 / 大型冰史莱姆）

`minegenshin:test1` —— 不能飞不能游泳的敌对生物，常态蠕动位移，进入 2 格大跳撞击后回原位。

### 7.1 目标配置（`Test1Entity#registerGoals`）

```java
this.goalSelector.addGoal(1, new Test1LeapSmashGoal(this));          // ≤2 格：大跳撞击
this.goalSelector.addGoal(2, new ApproachTargetGoal(this, 0.6D, Test1Entity.SMASH_DISTANCE)); // >2 格：走近
this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6D));  // 没目标：蠕动漫游（避水）
this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

this.targetSelector.addGoal(1, new HurtByTargetGoal(this));                        // 挨打还手
this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true)); // 找玩家
```

> **为什么多了一个 `ApproachTargetGoal`**：原版把「走过去」和「出手」缝在
> `MeleeAttackGoal` 里，而 test1 的攻击表现不是挥手而是位移撞击，所以必须拆开。
> 1 号与 2 号的距离阈值取同一个值（`SMASH_DISTANCE`），一个管 `> 2`、一个管 `≤ 2`，
> 中间没有「谁都不管」的空档。

### 7.2 三层职责划分

| 层 | 文件 | 负责 |
|---|---|---|
| 目标 | 两个原版 target Goal | 「打谁」 |
| 动作 | `LeapSmashGoal`（通用）+ `Test1LeapSmashGoal`（接动画与伤害） | 「2 格内起跳撞击并回位」 |
| 走位 | `ApproachTargetGoal` | 「还差得远就先走过去」 |
| 走法 | `WriggleMoveControl` | 「无目标蠕动、有目标小跳」 |

**「有目标就跳」不是 Goal，而是 MoveControl 的行为**：`WriggleMoveControl#tick()` 里
`mob.getTarget() != null` 就走`hopForward()`（定时 `getJumpControl().jump()`），否则 `creepForward()`。
这样它天然和上层的追人 Goal 组合，不需要任何额外仲裁。

### 7.3 `LeapSmashGoal` 的状态机

```
canUse: 目标存活 && 在地面 && 不在水里 && 冷却好 && 水平距离 <= 2.0
   ↓
start: 记录 origin、朝目标归一化方向、airSpeed = 距离 / 滞空刻数（夹在 0.12~0.85）
   ↓
OUT : 每 tick 重设水平速度（heading * airSpeed，竖直分量交给重力）
      │  碰撞箱外扩 smashPadding 后与目标相交 → smash() → beginReturn()
      │  目标死了 / 落地仍未接触 → beginReturn()（不贴地滑行）
      │  超时 40 刻 → 结束
   ↓
BACK: 方向指向 origin；落地后若离 origin > arriveDistance 就补一小跳
      │  贴回原位或超时 → phase = DONE
   ↓
canContinueToUse() == false → GoalSelector 自动 stop() → 记冷却
```

关键设计点，值得复用到其他「带位移的大招」：

1. **水平速度每 tick 重写**，所以摩擦和重力只影响竖直分量，落点可控；
   「距离 ÷ 滞空刻数」让 1 格和 2 格都能命中，不需要为不同距离写两套。
2. **回程不是瞬移**：记录 `origin`，撞完把方向反转、走同一条弧线回去，
   落地贴不回去就补跳 —— 这就是「跳起 → 重击 → 落地顺势回落到原位置」。
3. **目标中途死了照样回原位**：`tickOut()` 里目标失效直接 `beginReturn()`，不会悬停。
4. **`isInterruptable() == false`**：半空被打断会僵住。
5. **每一 tick 清 `setSpeed(0)/setZza(0)/setXxa(0)`**：防止 `aiStep` 的移动输入叠加到扑击速度上。

### 7.4 动画状态（`TestAction`）

| 枚举 | 动画名 | 循环 | 触发时机 |
|---|---|---|---|
| `IDLE` | `idle` | ✅ | 没在动（每 tick 由 `defaultAction()` 推导） |
| `WRIGGLE` | `move.wriggle` | ✅ | 无目标且在移动 |
| `HOP` | `move.hop` | ✅ | 有目标且在移动 |
| `LEAP` | `attack.leap` | ❌ | 扑击起跳（`onLeapStarted` 锁定） |
| `SMASH` | `attack.smash` | ❌ | 撞上目标（`smash()` 里锁定） |
| `LAND` | `attack.land` | ❌ | 掉头回位（`beginReturn()` 里锁定） |

锁定用 `lockAction(action)` / `unlockAction()`：扑击期间禁止 `customServerAiStep` 的
默认动作推导覆盖它，否则会出现「跳到一半变蠕动」。

### 7.5 范例二：test2 —— 随机远程技能（召唤冰块砸人）

`minegenshin:test2` —— 身体和 test1 一样（蠕动 / 小跳 / 接近），但**不贴身**：
随机发动、抬手召唤一块原版冰，冰块在头上旋转蓄力后飞出去砸向目标。

#### 7.5.1 一次技能拆成两半

| 谁 | 管什么 |
|---|---|
| `SummonIceBlockGoal`（Goal） | 随机发动、冷却、抬手动作、朝向目标、在抬手结束那一刻生成投射物 |
| `IceBlockProjectile`（实体） | 悬停跟随施法者头顶 → 自转 → 制导飞行 → 碎裂（粒子 + 音效 + 范围伤害） |

**为什么拆**：抬手是「怪物自己的行为」，归 Goal；冰块是「一个会自己飞的东西」，
归实体。这样施法者被打断 / 死亡也不会留下一个永远悬停的冰块 ——
投射物自己会检测施法者消失然后碎掉。

#### 7.5.2 随机 + CD 的写法

```java
// 冷却：记「下次最早可用时刻」，和 requiresUpdateEveryTick() 无关，不用自己数 tick
if (mob.level().getGameTime() < nextAllowedTick) return false;

// 随机：canUse() 每隔一 tick 被问一次，不能每次都为真，否则 CD 一到就立刻放
if (mob.getRandom().nextInt(CHECK_INTERVAL) != 0) return false;
```

`stop()` 里把 `nextAllowedTick` 推到 `now + 60~100 刻`。

#### 7.5.3 这个 Goal 只占 LOOK、不占 MOVE

远程技能不需要位移，所以 `setFlags(EnumSet.of(Flag.LOOK))` ——
于是它能和优先级 2 的 `ApproachTargetGoal` **同时运行**：边走边放。
习惯性写上 `MOVE` 的话两者会互相抢占，表现就是「走两步停一下、技能老放不出来」。

#### 7.5.4 为什么没用 `Display.BlockDisplay`

`BlockDisplay` 看起来最合适（自带 `Transformation` 旋转、自动同步、零渲染器代码），
但 26.2 里 `Display#setTransformation` / `BlockDisplay#setBlockState` **全是 `private`**，
只能通过 `/summon` 的 NBT 配置 —— 代码里既设不了方块、也改不了旋转。所以走自定义实体：

- 实体：`extends Entity`，只同步一个 `DATA_FLYING` 布尔；**位置交给引擎同步、旋转角度两端各自拿 `tickCount` 算**，不用为「转了多少度」发包。
- 渲染器：照抄原版 `FallingBlockRenderer` —— 渲染状态用 `FallingBlockRenderState`（自带 `MovingBlockRenderState`），提交走 `SubmitNodeCollector#submitMovingBlock`，自己只多做一件事：绕 Y 轴主转 + 叠加 0.7 倍的 X 轴旋转，做出「翻滚着砸过来」。要换方块只改 `IceBlockProjectile#blockState()`，渲染层不用动。

#### 7.5.5 伤害

**必须用原版伤害源**（见第 9 节最后两行）：`caster.damageSources().mobAttack(caster)`。
碎裂时对落点周围 4×3×4 的活体结算一次伤害，施法者自己排除在外。

#### 7.5.6 资源

`entity/test2/` 下放 `test2.json` / `test2.animation.json` / `test2.png`。
**没放也能看**：`CategoryGeoModel.withFallback(ENTITY, "test1")` 会按「项」借用 test1 的模型与贴图
（自己有模型缺贴图时只借贴图）。动画名多一个 `attack.cast`：

`idle`、`move.wriggle`、`move.hop`、`attack.cast`

---

### 7.6 范例三：大型冰史莱姆（Large Cryo Slime）

这是本项目第一个**完整形态**的怪物：元素生物 + 元素护盾 + 四个攻击手段 + 可调机制参数。
和 test1/test2 的区别是它不是「一个 Goal 的演示」，而是**一整套 Goal 怎么分工协作**的样板。

#### 7.6.1 它是什么

| 维度 | 取值 |
|---|---|
| 实体 id | `minegenshin:large_cryo_slime` |
| 基类 | `TeyvatMonster`（→ `Monster`）+ `ElementalCreature` + `GeoEntity` |
| 元素 | 冰（`ModElements.CYRO`） |
| 元素生物规则 | **永久免疫冰元素伤害**，含「冻」（冻的主元素是冰） |
| 数值 | 生命 ×**2**、攻击 ×**1.4**（乘在按等级查表的基础值上） |
| 护盾 | 冰元素 · 全抵挡 · 纯元素盾 · 罩型 · 永续 |
| 战斗定位 | 远中近都有手段，但**主要输出是撞击**，技能是点缀 |

#### 7.6.2 代码地图

| 文件 | 职责 |
|---|---|
| `monster/slime/LargeCryoSlime.java` | 实体本体：元素、系数、属性、Goal 编排、自挂冰、护盾、动画控制器 |
| `monster/slime/CryoSlimeSkillGoal.java` | **技能基类**：冷却 + 随机 + 攻击欲望 |
| `monster/slime/CryoSlimeShardGoal.java` | 技能一：冰刺三连 |
| `monster/slime/CryoSlimeMistGoal.java` | 技能二：冰雾 |
| `monster/slime/CryoSlimeSlamGoal.java` | 技能三：跃起砸落 |
| `monster/slime/CryoSlimeShieldRestoreGoal.java` | 护盾恢复 |
| `entities/test/IceBlockProjectile.java` | 冰块投射物（生成 → 升起 → 自转 → 飞 → 碎裂） |
| `skill_node/IceMistField.java` | 技能节点：3×3×1 持续冰雾 |
| `skill_node/GroundMarker.java` | 技能节点：地面落点提示（固定圈 + 扩散圈） |
| `core/system/shield/**` | 护盾系统（分类、结算、状态同步） |
| `content/entities/teyvat/ElementalCreature.java` | 元素生物接口（同元素免疫） |
| `config/entity/MobBehaviorConfig.java` | 全部可调参数 |

#### 7.6.3 Goal 清单与优先级

```java
// 1：普通攻击 —— 向前大跳撞击，撞到之后落回起跳点
goalSelector.addGoal(1, new LeapSmashGoal<>(this, MobBehaviorConfig.collideRange()));
// 2~4：三个技能，冷却长、发动率低
goalSelector.addGoal(2, new CryoSlimeSlamGoal(this));
goalSelector.addGoal(3, new CryoSlimeMistGoal(this));
goalSelector.addGoal(4, new CryoSlimeShardGoal(this));
// 5：护盾恢复
goalSelector.addGoal(5, new CryoSlimeShieldRestoreGoal(this));
// 6：只走不打
goalSelector.addGoal(6, new ApproachTargetGoal(this, 0.6D, MobBehaviorConfig.collideRange()));
// 8/9：看人与东张西望
goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
goalSelector.addGoal(9, new RandomLookAroundGoal(this));

targetSelector.addGoal(1, new HurtByTargetGoal(this));
targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
```

**为什么撞击排第一、技能排后面**：撞击要求「距离 ≤ 4 且朝着目标」，条件一满足就该立刻打；
技能是远程/范围手段，不该抢近身输出的机会。技能 Goal 都**不占 MOVE**，
所以它们能和优先级 6 的接近 Goal 并行 —— 一边走位一边放技能。

> ⚠️ **踩过的坑**：接近 Goal 的停下距离和撞击的触发距离**必须是同一个值**。
> 曾经接近 Goal 停在 6 格、而撞击要 4 格内，于是史莱姆在 6 格站定，
> 永远等不到能起跳的距离 —— 表现就是「普攻一次都不放」。
> 现在两处都取 `MobBehaviorConfig.collideRange()`。

#### 7.6.4 撞击：为什么直接复用 `LeapSmashGoal`

撞击的规格是「跳起来、划一条 1/4 圆弧撞过去、然后落回原位」，这**正好就是**
test1 用的 `LeapSmashGoal`（见 7.3 的状态机），所以没有新写一个 Goal，而是直接复用：

```java
new LeapSmashGoal<>(this, MobBehaviorConfig.collideRange())
```

它是泛型 `LeapSmashGoal<T extends PathfinderMob>`，任何 `PathfinderMob` 都能挂。

**朝向判断**是 `canUse` 里加的（只判距离的话，它会横着飞出去）：

```java
if (this.horizontalDistanceTo(target) > this.triggerDistance) return false;
return this.isFacing(target);        // 身体朝向与目标方向夹角 ≤ 45°
```

这里有一个必须一起考虑的联动：史莱姆停在触发距离内时，`ApproachTargetGoal` 不再运行，
没人给它转头 → 会卡死。但实际上 `LookAtPlayerGoal`（优先级 8）会接管 LOOK，
把脑袋转向最近的玩家，身体朝向跟着转，转到 45° 内就起跳。

**伤害**走 `LeapSmashGoal.smash()` 的默认实现（`mob.doHurtTarget`），
也就是**原版伤害源** —— 怪物不是 `PGCharacter`，用 `ModDamageSource` 会被伤害管线算成 0；
交给 `LivingEntityHurtMixin` 按 `damage / vanillaAttack * teyvatAttack` 换算，
最终伤害 = 配置表 ATK × 1.4 × 1.0（倍率 1）。

#### 7.6.5 三个技能：`CryoSlimeSkillGoal` 基类

三个技能 Goal 长得几乎一样，所以把公共部分抽成基类，子类只填差异：

```java
protected abstract class CryoSlimeSkillGoal extends Goal {
    public final boolean canUse() {
        if (slime.level().getGameTime() < nextAllowedTick) return false;          // ① 冷却
        if (!onGround || passenger || inWater) return false;
        if (random.nextInt(checkInterval) != 0) return false;                     // ② 采样
        if (random.nextFloat() >= chance() * MobBehaviorConfig.attackDesire()) return false;  // ③ 欲望
        LivingEntity target = slime.currentTarget();
        return target != null && extraCanUse(target);                             // ④ 子类条件
    }
    protected float chance() { return 1.0f; }              // 子类改概率
    protected boolean extraCanUse(LivingEntity t) { return true; }   // 子类改距离条件
    protected void onStopped() {}                          // 子类收尾
}
```

三个要点：

1. **冷却**用「下次最早可用时刻」，和 `requiresUpdateEveryTick()` 无关，不用自己数 tick；
   `stop()` 里推到 `now + cooldown + random(cooldown/2)`，避免三只史莱姆同步放招。
2. **随机**不能每次 `canUse` 都为真（那等于「冷却一到立刻放」），
   所以先按 `checkInterval` 抽样，再乘一次**攻击欲望**。攻击欲望是配置项，
   深渊环境把它调大就是「同一只怪更凶」。
3. **`isInterruptable()` 返回 false**：抬手到一半被打断会出现「跳一半停住」。

三个子类的差异：

| 技能 | 冷却 / 采样 | 概率 | 额外条件 | 做了什么 |
|---|---|---|---|---|
| 冰刺 | 5s / 14 | 0.6 | 距离 3~24 | 抬手 12 刻后，每 10 刻发一根，共 3 根 |
| 冰雾 | 7s / 16 | 0.55 | 距离 2~14 | 抬手 14 刻后在身前 3 格铺一片冰雾 |
| 砸落 | 9s / 20 | 0.45 | 距离 ≤ 16 | 压缩 2s → 升空 → 滞空 → 落点 → 砸 |

#### 7.6.6 冰刺：`IceBlockProjectile` 的三段式

冰刺就是缩小到 1/3 的冰块投射物，走的是同一个实体，靠两个参数区分：

```java
IceBlockProjectile.create(serverLevel, slime, target,
        SHARD_SCALE /* 1/3 */, IceBlockProjectile.CHARGE_TICKS /* 36 */)
        .withChargeOffset(shardOffset(index));   // 左 / 中 / 右错开
serverLevel.addFreshEntity(shard);
```

投射物自己分三段：

```
CHARGE  36 刻：从施法者<b>身体内部</b>生成（Y + 身高×0.4），线性升到头顶，
               一边升一边自转（20°/刻 × 36 刻 = 720° = 正好两圈）；
               这段 <b>noPhysics = true</b>，没有碰撞箱
  ↓ 升到位
FLY     解除穿透、开碰撞，朝目标制导飞（0.5 格/刻）
  ↓ 撞到 / 撞墙 / 到达
碎裂    方块破坏粒子 + 玻璃碎裂音 + 落点周围伤害 → discard
```

**这里踩过的坑**：三连冰刺原来传的是 `chargeTicks = 0`（生成即飞），
所以「蓄力升起来转两圈」这一整段根本不存在，观感就是「凭空直接射过来」。

`withChargeOffset` 是必要的：蓄力阶段每 tick 都会把位置重设到施法者身上，
不把偏移存进投射物就会被冲掉，三根冰刺会叠在同一个点上升。

#### 7.6.7 冰雾与落点：两个 skillnode

**`IceMistField`（冰雾）** —— 3×3×**1** 的区域（只有最底下一层，站进去才吃伤害）：

| 参数 | 值 |
|---|---|
| 范围 | 半径 1.5（3×3） |
| 持续 | 5 秒（可配） |
| 结算 | 每 10 刻（0.5s）一次，伤害 = 攻击力 × **0.3** |
| 附着 | 给玩家的**场上角色**挂弱冰（不是玩家本体） |

元素附着挂在 `PGCharacter` 的容器上（`StatusAccessor.of(character.getData())`），
所以「切角色 = 换一个附着」；配合 `CharacterChillHandler` 每 tick 重算
「出战角色有没有冰附着」，就自然实现了**切人取消减速、切回来依旧**。

**`GroundMarker`（落点提示）** —— 两层粒子：

```java
// ① 固定外圈：范围（一直不变）
for (i in 0..24) sendParticles(OUTER, center + cos/sin * radius);
// ② 从中心往外扩散的 3 层圆盘：进度（扩满 = 砸下来）
for (ring in 1..3) for (i in 0..24) sendParticles(INNER, center + cos/sin * innerRadius*ring/3);
```

到点后回调 `onImpact`，由调用方决定砸什么 —— 所以它不只服务史莱姆，
任何「延迟落点」技能都能用。

#### 7.6.8 砸落：四步状态机

```
COMPRESS 2s   原地压缩蓄力（给玩家的「要跳了」信号）
RISE          起跳上升（关重力，保持可控）
HOVER 0.6s    悬停定位
MARKING 1.5s  在目标当前位置画 5×5 落点，扩满即落地
LAND          砸下去，5×5 内结算 攻击力 × 3.0
```

⚠️ 空中阶段把 `noGravity` 打开了，所以 `onStopped()` 里**必须恢复**，
否则技能被打断会留下一只永远浮空的史莱姆。

#### 7.6.9 护盾怎么接上去

**套盾**：`tick()` 的第一帧 `grantCryoShield()` → `ShieldService.grant(...)`。

**同步（这里有个大坑）**：NeoForge 的附件是「**原地改对象不算改**」——
不调 `entity.setData(type, state)` 的话，服务端自己有新数值、客户端永远拿旧值。
所以 `ShieldService` 在 `grant / absorbDamage / onElementalAttack / tick / clear`
里都会 `push(entity, state)` 推一次。项目里 `StatusTickHandler` 每 tick 手动 `setData`
是同一个原因。**血条下面的盾条不显示，就是漏了这个。**

**火破冰盾的真实链路**（这是设计上最容易搞错的地方）：

```
盾还在 → 每秒给自己挂一层弱冰附着（"冰盾自挂元素"）
火打上来 → ① 照常挂火
           ② 和自挂的冰触发融化 → 双方被反应消耗（所以不留火）
           ③ 同时盾按元素表扣 2.0 × 元素量（外加 0.15 削韧）
```

注意③和②是**两件事**：反应消耗的是「盾自己挂的那层冰」，
盾量的扣除走的是 `ShieldProfile` 的元素消耗表，不是反应写进去的。

**冰盾的元素表**（`ShieldProfiles.CRYO_ELEMENT`）：

| 元素 | 每单位消耗 | 说明 |
|---|---|---|
| 冰 | 0 | 免疫：不附着、不消耗、无效果 |
| 水 | 0 | **附着被整个吞掉**：不附着、不冻结、不消耗 |
| 火 | 2.0 | 1 单位火消耗 2 点冰 |
| 雷 / 风 / 岩 | 0.1 | 反应照常 |
| 草 | 0 | 无反应 |
| 物理 | 0 | 只吃削韧 |

**破盾 / 补盾的帽子动画**：`hat` 骨骼通过动画控制（GeckoLib 5 没有
`GeoBone.setHidden`，骨骼显隐由动画的 `frameSnapshot` 决定，所以「隐藏」=
把骨骼 scale 成 0）：

```
第一帧      → 只记录持盾状态，不播任何动画（帽子 = 模型默认，在）
盾破的那帧  → shield.down（hat scale 1→0，停最后一帧）
补盾的那帧  → shield.up  （hat scale 0→1，停最后一帧）
```

**为什么必须只在状态翻转时播、且第一帧不播**：
第一帧客户端还没收到盾的同步（`hasShield()` 是 false），按状态播会立刻把帽子收掉
（表现「刚生成就没帽子」）；而盾恢复后如果只是 `STOP` 控制器，
上一帧留下的 scale 0 不会自己还原（表现「补盾后帽子回不来」）。

#### 7.6.10 自挂冰附着

```java
AttachmentProfile weak = AttachmentProfile.WEAK;
AttachmentProfile full = new AttachmentProfile(
        weak.getBaseQuantity(), 1.0f, weak.getDecayPerSecond(), weak.getDurationSeconds());
ElementalAttachmentHelper.attach(this, container, ModElements.CYRO.get(),
        AttachmentSource.SELF_ATTACH, full);
```

两个刻意的选择：

- **`AttachmentSource.SELF_ATTACH`**：不走 `NORMAL_ATTACK`，所以没有 0.8 损耗、
  也**不遵守「后手不残留」**——自挂的元素就该留在身上，而不是反应一次就没了。
- **`lossMultiplier = 1.0`**：全额附着，不是先手那套 0.8。

**它不会被自己减速**：生物的减速/冻结效果来自**寒元素**（`ColdAura` 每 tick 给"有冰或冻"的生物补一条寒，
`ColdElement` 据此挂 -10% 移速、有冻时禁 AI）。补寒同样要走宿主筛查，而
`ElementalCreature.acceptsElementAttachment` 对寒是按「是否免疫冰」答复的 ——
所以「免疫冰 = 不受冰影响」：它给自己挂冰，但挂不上寒，既不会被拖慢也不会被冻住。

#### 7.6.11 配置表（`config/minegenshin/entity.toml` 的 `[large-cryo-slime]` 段）

| 键 | 默认 | 作用 |
|---|---|---|
| `attack-desire` | 1.0 | 攻击欲望倍率：乘在所有技能概率上 |
| `collide-range` | 4.0 | 撞击触发距离（也是接近 Goal 的停下距离） |
| `shard-count` | 3 | 冰刺枚数 |
| `shard-cooldown` | 5.0 | 冰刺冷却（秒） |
| `mist-duration` | 5.0 | 冰雾持续（秒） |
| `mist-cooldown` | 7.0 | 冰雾冷却（秒） |
| `slam-cooldown` | 9.0 | 砸落冷却（秒） |
| `shield-value` | 8.0 | 盾量 |
| `shield-restore-seconds` | 30.0 | 失盾后多久开始补盾 |
| `shield-idle-restore-seconds` | 10.0 | 多久没挨打就开始补盾 |
| `shield-cast-seconds` | 2.0 | 补盾抬手时长 |

深渊那类环境只要改这一段，就能做出「同一只怪、更强机制」。

#### 7.6.12 这一类怪踩过的坑（浓缩版）

1. **接近距离 ≠ 触发距离 → 技能永远放不出来**（见 7.6.3）。
2. **附件原地改不同步 → 盾条不显示**（见 7.6.9）。
3. **投射物 `chargeTicks = 0` → 蓄力过程不存在**（见 7.6.6）。
4. **按状态每帧播动画 → 生成时帽子就没了、补盾后回不来**（见 7.6.9）。
5. **两段圆环共面 z-fighting → 盾条一闪一闪**（血条那边槽用 `0.0f`、
   填充用 `FILL_Z_OFFSET` 就是为了避这个）。
6. **`Map.copyOf` 遇到 null 值抛 NPE → 整个客户端资源重载失败**
   （GeckoLib 的 loader 解析失败时返回 null）—— 详见第 9 节。

---

## 8. 冲突与排查清单

**症状 → 病因**

| 症状 | 常见原因 |
|---|---|
| 实体站着不动 | 抢占 `MOVE` 的 Goal 没 `getNavigation().stop()`；或 MoveControl 里 `operation` 不是 `MOVE_TO` |
| 实体原地抽搐 | 到点后没停下（`MoveControl` 里要做「距离小于阈值就 `setZza(0)`」）；两个 Goal 抢同一 Flag 轮流启动 |
| 动画一直不播 | 动画名和 `.animation.json` 对不上；或者 `RawAnimation` 每次新建导致 `setAnimation` 判重失效反复重播 |
| 客户端动画不跟随状态 | 用了不同步的 `getTarget()` / `isAggressive()` 之外的字段 |
| 大招演一半僵住 | `isInterruptable()` 为 true 被高优先级 Goal 抢了 Flag；或 `canContinueToUse()` 复刻了 `canUse()` |
| 冷却时间不对 | 自己数 tick 而没过 `adjustedTickDelay()`，`requiresUpdateEveryTick()==false` 时慢一倍 |
| 实体在 1 格高的台阶前反复跳 | `attributes` 里 `STEP_HEIGHT` 太低（默认 0.6），或 `maxUpStep` 被改小 |
| 目标永远是 null（客户端） | 目标不同步，这是原版设计，不是 bug |
| 怪物打人**伤害恒为 0** | 用了 `ModDamageSource`。伤害管线的基础伤害区是 `ATK×倍率`，ATK 只有角色（`PGCharacter`）有；怪物 `attacker == null` → `baseDamage = 0f`。**非角色攻击者必须用原版伤害源** |
| 模型/贴图紫黑 | 资源没被索引到。看启动日志 `[AssetGeoCache]` 那几行目录清单；注意贴图 Identifier **不含** `textures/` 前缀（见 9.2） |

**调试手段**

1. `mob.setNoAi(true)` / `/data get entity @e[...]` 先确认「是不是 AI 的问题」而不是渲染/属性问题。
2. `goalSelector.disableControlFlag(Flag.LOOK)` 临时关掉某一类仲裁，看异常是否消失，能快速定位是哪个 Flag 在打架。
3. `Goal#toString()` 默认返回类名 —— 打印 `goalSelector.getAvailableGoals()` 就能看到谁在跑、
   `WrappedGoal#isRunning()` 和 `getPriority()` 一目了然。
4. `TeyvatLiving#setAiEnabled(false)` 是项目里现成的「停 AI」开关。
5. `F3 + I`（实体调试）能看到实体的 `yBodyRot`、速度、寻路状态。

---

## 9. 资源契约（配合 `core/asset`）

统一布局（`AssetCategory` + `ModAssetPaths` + `AssetSet` + `AssetPathResolver`）：

```
assets/minegenshin/
├── entity/<id>/    <id>.geo.json | <id>.animation.json   ← 实体三件套（入口按对象）
│                   textures/<id>.png
├── item/<id>/      definition.json | model.json          ← 原版入口文件
│                   textures/texture.png | icon.png | <id>.png（geo 物品）
├── block/<id>/     blockstate.json | model.json
│                   textures/texture.png | icon.png
│                   blockitem/{definition,model}.json + blockitem/textures/…
├── character/<id>/ <id>.animation.json | textures/*.png | sounds/*.ogg
├── gui/<名字>.png  共用界面贴图
├── icon/<分类>/<名字>.png  跨对象图标
└── lang/           原版语言文件（唯一剩下的原版入口）
```

原版那四个入口（`blockstates/`、`items/`、`models/`、`textures/`）**不再需要留在原版根目录**：
本 MOD 的重定向层（`core/asset/AssetRedirects` + `FileToIdConverterRedirectMixin`）在读取时把它们
改写到上面的布局（只对本 MOD 命名空间生效）：`blockstates/<id>.json` ← `block/<id>/blockstate.json`、
`items/<id>.json` ← `item/<id>/definition.json`、`models/<路径>.json` ← `<路径>.json`、
`textures/<路径>.png` ← `<路径>.png`（对象目录里的 `textures/` 那一层会去掉）。
对应关系与实测日志见 `docs/systems/render-asset.md`。

代码侧只需要一行：

```java
new CategoryGeoModel<Test1Entity>(AssetCategory.ENTITY, Test1Entity.ASSET_ID)
```

### 9.1 名字是「查出来的」，不是「猜出来的」

`AssetGeoCache` 会把 `item/`、`block/`、`entity/` 下的文件按**目录**索引起来，
`CategoryGeoModel` 先问「这个目录里到底有什么」，再决定三个路径。所以下面这些都成立：

```
entity/test1/test1.json          ← 标准写法
entity/test1/test1.geo.json      ← GeckoLib 原生后缀（两种后缀剥出来是同一个键）
entity/test1/test.json           ← 目录里只有一个 json 时，叫什么都行
entity/test1/whatever.png        ← 贴图同理；同名（基名 == 目录名）的优先
```

都没找到时才回落到约定路径，并打一条**带目录清单**的警告 ——
「什么都不报，只显示紫黑」是排查资源问题最贵的形态。启动日志里会有：

```
[AssetGeoCache] 已索引 1 个目录 / 1 个模型 / 1 个动画文件
[AssetGeoCache]   entity/test1  model=minegenshin:entity/test1/test1.json  animation=...  texture=...
```

### 9.2 贴图位置的一个反直觉点

**原版 `TextureManager` 不会给 Identifier 加 `textures/` 前缀**，
它直接 `resourceManager.getResourceOrThrow(location)`（`SimpleTexture#loadContents` →
`TextureContents.load`）。所以贴图 Identifier 与文件路径是**一字不差**的对应：

| Identifier | 实际文件 |
|---|---|
| `minegenshin:entity/test1/test1.png` | `assets/minegenshin/entity/test1/test1.png` |
| `minegenshin:textures/entity/test1/test1.png` | `assets/minegenshin/textures/entity/test1/test1.png` |

容易踩的地方在于 **GeckoLib 自己的 `DefaultedGeoModel` 是带 `textures/` 的**
（`basePath.withPath("textures/" + subtype() + "/" + … + ".png")`），
所以看 GeckoLib 示例时容易以为少写了一截。本 MOD 的约定是**不带** `textures/`：

- 模型 / 动画 / 贴图标识符统一从 `assets/minegenshin/` 根算起（`entity/test1/test1.png`）；
- 共用界面贴图同理（`minegenshin:gui/short_character_hp_green.png`）；
- **唯一带 `textures/` 的**是被原版模型 JSON 引用的贴图 —— 那是原版图集的规则，不是本 MOD 的约定。

LDLib2 的 `SpriteTexture` 走的也是完整路径语义（Identifier 直接交给 `TextureManager`），
所以 `minegenshin:gui/x.png` 就对应 `assets/minegenshin/gui/x.png`，LSS 里的 `sprite(...)` 同理。

### 9.3 动画名契约

`entity/test1/test1.animation.json` 里必须存在这 6 个名字，
缺了只是「不播放」不会崩（`CategoryGeoModel` 会警告一次）：

`idle`、`move.wriggle`、`move.hop`、`attack.leap`、`attack.smash`、`attack.land`

`entity/test2/test2.animation.json` 需要 4 个：

`idle`、`move.wriggle`、`move.hop`、`attack.cast`

> ⚠️ **最容易踩的坑**：动画文件在、但里面**没有代码请求的那个名字**时，
> GeckoLib 报的是「Unable to find animation **file**」。原因见本文档之前的排查记录：
> 我们的动画是自烘的，GeckoLib 自己的缓存里没有这个文件键，
> 于是它的查找在「文件」这一步就失败了，根本走不到「名字」那一步。
> `CategoryGeoModel` 现在会把「文件里实际有哪些名字」一起打出来。

### 9.4 模型缓存链

`CategoryGeoModel`：`AssetGeoCache`（统一布局）→ `GenshinGeoCache`（角色 / GeckoLib 原生根）
→ GeckoLib 自带缓存。

---

## 10. 下一步（test2 / test3 的扩展点）

> ⚠️ **本节写于早期测试阶段**：其中的 `Test1Entity` / `Test2Entity` / `TestMonster` / `TestAction` /
> `TestEntityRenderers` 等测试类已在结构整理中删除。现在要照抄的活例子是大型冰史莱姆
> （`content/entities/teyvat/monster/slime/LargeCryoSlime`，渲染器在 `MinegenshinClient` 里
> `new CategoryGeoModel<>(AssetCategory.ENTITY, "large_cryo_slime")`），资源在 `entity/large_cryo_slime/`。

新增一个测试实体的完整工作量：

1. `ModEntities` 加一个 `DeferredHolder<EntityType<?>, EntityType<Test2Entity>>`；
2. 新建 `Test2Entity extends TestMonster`，覆写 `assetId()` 与 `registerGoals()`、
   按需覆写 `defaultAction()`；
3. `TestEntityRenderers#registerEntityRenderers` 加一行 `CategoryGeoModel`；
4. 放 `assets/minegenshin/entity/<实体id>/`：`<id>.geo.json`、`<id>.animation.json`、`textures/<id>.png`。

已经预留好的扩展位：

- **新的走法**：换一个 `MoveControl`（`content/entities/ai/control/`）。
- **新的动作编排**：`content/entities/ai/goal/` 加一个状态机 Goal；
  `LeapSmashGoal` 的构造参数（触发距离、滞空刻数、起跳竖直速度、回程比例）已经全部开放，
  改数值就能做出「贴身连撞」「远程飞扑」等变体。
  `ApproachTargetGoal` 是纯走位的，任何需要「先靠近再出招」的实体都能直接复用。
- **新的投射物**：抄 `IceBlockProjectile` —— 换 `blockState()` 就变成另一个方块；
  换 `SummonIceBlockGoal` 的 CD / 概率 / 抬手刻数就变成另一种释放节奏；
  想做成抛射物而不是直线制导，改 `tickFlying` 里的速度计算即可。
- **新的动画状态**：`TestAction` 末尾追加枚举（**只能追加，顺序号写进了同步数据**）。
- **新的资源类别用法**：`block/<id>/` 与 `item/<id>/` 的解析已经就绪，
  等方块与物品注册接入时直接 `AssetSet.block(...)` / `AssetSet.blockItem(...)`。
- **资源没画好先上线**：`CategoryGeoModel.withFallback(category, id)` 按项借别的目录。
