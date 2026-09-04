# MineGenshin 开发者文档

> Minecraft NeoForge 原神风格 Mod 开发指南
> 版本：26.2 | Java 21

---

## 目录

1. [项目概览](#1-项目概览)
2. [开发环境](#2-开发环境)
3. [架构总览](#3-架构总览)
4. [核心系统详解](#4-核心系统详解)
    - 4.1 [注册中心与自定义注册表](#41-注册中心与自定义注册表)
    - 4.2 [角色系统](#42-角色系统)
    - 4.3 [属性系统](#43-属性系统)
    - 4.4 [效果系统 (CharacterEffect)](#44-效果系统-charactereffect)
    - 4.5 [伤害系统与衰减机制](#45-伤害系统与衰减机制)
    - 4.6 [玩家附件 (Attachment)](#46-玩家附件-attachment)
    - 4.7 [网络同步系统](#47-网络同步系统)
    - 4.8 [事件系统](#48-事件系统)
    - 4.9 [实体系统](#49-实体系统)
    - 4.10 [元素附着系统](#410-元素附着系统)
    - 4.11 [元素反应系统](#411-元素反应系统)
    - 4.12 [怪物等级与防御系统](#412-怪物等级与防御系统)
5. [扩展开发指南](#5-扩展开发指南)
    - 5.1 [添加新角色](#51-添加新角色)
    - 5.2 [添加新角色效果](#52-添加新角色效果)
    - 5.3 [添加新实体](#53-添加新实体)
    - 5.4 [添加新伤害类型](#54-添加新伤害类型)
6. [Mixin 系统](#6-mixin-系统)
7. [常见问题与设计决策](#7-常见问题与设计决策)
8. [关键文件索引](#8-关键文件索引)

---

## 1. 项目概览

MineGenshin 是一个将原神核心玩法机制移植到 Minecraft 的 Mod。

### 核心系统

| 系统 | 说明 |
|------|------|
| **角色系统** | 可收集的原神角色，支持等级、突破、技能等级 |
| **属性系统** | HP/ATK/DEF + 修饰器（固定/百分比，临时/永久） |
| **效果系统** | Buff/Debuff 框架，支持伤害修改、前后台 Tick |
| **伤害系统** | 带元素类型、倍率、衰减序列的伤害管线 |
| **衰减系统** | 附着冷却机制，还原原神附着规则 |
| **祈愿系统** | 原石抽卡，角色重复返还原石 |
| **网络系统** | RPC 双向同步角色、编队、原石等数据 |

### 技术栈

- **Minecraft 版本**: NeoForge 26.2
- **Java 版本**: 21
- **依赖库**: LowDragLib2（同步框架、RPC 网络）
- **混合器**: SpongePowered Mixin

### 目录结构

```
src/main/java/com/linweiyun/genshin/
├── Minegenshin.java              // Mod 主入口
├── MinegenshinClient.java        // 客户端入口
├── Config.java                   // 配置文件（EXP、角色属性）
├── core/                         // 核心系统
│   ├── character/                // 角色系统基类
│   ├── attribute/                // 属性系统
│   ├── system/combat/            // 战斗系统
│   ├── attachment/               // 玩家附件
│   ├── network/                  // 网络同步
│   └── ...
├── content/                      // 内容实现
│   ├── effect/character/         // 角色效果
│   ├── entities/                 // 实体
│   └── ...
├── registry/                     // 注册表
├── event/                        // 事件监听
├── mixin/                        // 混合器
└── client/                       // 客户端 GUI
```

---

## 2. 开发环境

### 构建命令

```bash
./gradlew build          # 编译构建
./gradlew classes        # 只编译
./gradlew runClient      # 开发运行客户端
./gradlew runServer      # 开发运行服务端
```

### 配置文件

运行后在 `./config/minegenshin/` 下生成：

| 文件 | 内容 |
|------|------|
| `exp.toml` | 角色升级经验（1-90级，共88个值） |
| `attribute.toml` | 各角色各等级 HP/ATK/DEF（95级基础，含突破跃升） |

---

## 3. 架构总览

### 3.1 核心数据流

#### 攻击伤害管线

```
玩家攻击（原神模式开）
  │
  ▼
PlayerAttackInterceptor (Mixin, 注入 Player.attack HEAD)
  ├─ 取当前角色 PGCharacter
  ├─ 构建 ModDamageSpec(AttackType, Element, ..., attackerCharacter)
  ├─ 构建 ModDamageSource.from(spec, player)
  └─ target.hurtServer(serverLevel, modSource, 0f)    ← 传 0f，MOD 管线自己算
        │
        ▼
目标实体 hurtServer 分发：
  │
  ├─ 目标是 TeyvatLivingEntity（本 Mod 实体）
  │     └─ 自行重写 hurtServer，内部直接调用 HurtEntityHelper.calculateFinalModDamage
  │
  ├─ 目标是非 TeyvatLivingEntity（原版实体 / 其他 Mod 实体）
  │     └─ LivingEntityHurtMixin (Mixin, 注入 LivingEntity.hurtServer HEAD)
  │           ├─ source instanceof ModDamageSource？
  │           │     ├─ 是 → HurtEntityHelper.calculateFinalModDamage(...)
  │           │     │         └─ [完整伤害管线见 4.5 节]
  │           │     ├─ 扣血：玩家(原神模式开) → character.hurt()；普通目标 → setHealth()
  │           │     ├─ level.broadcastDamageEvent(target, source)  ← 客户端同步
  │           │     └─ cir.setReturnValue(true)  ← cancel 原版流程
  │           │
  │           └─ 否 → return  ← 放行原版 hurtServer 逻辑
  │
  └─ 目标是玩家（非 MOD 伤害，原神模式开）
        └─ PlayerHurtInterceptor (Mixin, 注入 Player.actuallyHurt HEAD)
              ├─ source 不是 ModDamageSource + 原神模式开
              ├─ character.hurt(damage) + incapacitate(attachment)
              └─ ci.cancel()  ← 跳过原版 actuallyHurt，玩家血量不变
```

#### 角色倒下自动切换管线

```
LivingEntityHurtMixin / PlayerHurtInterceptor 扣血后
  │
  ├─ character.hurt(finalDamage)
  │     ├─ before = data.getCurrentHP()
  │     ├─ data.hurtHP(amount)
  │     └─ 返回 data.getCurrentHP() <= 0 && before > 0
  │
  └─ 如果返回 true（倒下）：
        └─ character.incapacitate(attachment)
              ├─ 确保血量归零
              └─ 从当前角色下一个位置开始，找队伍中第一个有角色的位置切换
```

#### 元素附着 Tick 管线

```
StatusTickEvent (每 tick, EntityTickEvent.Post)
  └─ 遍历 LivingEntity → getData(CONTAINER) → container.tick()
        └─ 遍历 StatusInstance → inst.tick() → 衰减 quantity
        └─ isFinished() → 清掉耗尽的实例
```

### 3.2 角色-玩家关系模型

```
Player (MC 原生)
    └── PlayerCharactersAttachment (附件)
            ├── ownedCharacters: List<PGCharacter>    // 拥有的角色
            ├── sheetCharacterUUIDs: List<Integer>     // 角色图鉴
            ├── partyCharacterUUIDs: List<Integer>     // 队伍编队 (最多4人)
            └── currentCharacterIndex: int             // 当前前台角色
```

### 3.3 角色内部结构

```
PGCharacter (可序列化原型)
    └── PGCharacterData data
        ├── characterLevel / maxExp / currentExp
        ├── currentHP
        ├── ascensionPhase
        ├── attributes: AttributeContainer            // HP/ATK/DEF + 修饰器
        ├── skillLevels (normal/charged/plunging/skill/burst)
        ├── currentObtainingEnergy                     // 元素能量
        ├── elementalSkillCooldownTick                 // E技能CD
        ├── elementalBurstCooldownTick                 // Q技能CD
        ├── effectContainer: CharacterEffectContainer   // 效果容器
        └── dirty: boolean                             // 脏标记
```

---

## 4. 核心系统详解

### 4.1 注册中心与自定义注册表

#### 注册表定义

位置：`registry/ModRegistries.java`

本 Mod 创建了三个自定义注册表，通过 NeoForge 的 `RegistryBuilder` 构建并在 `NewRegistryEvent` 中注册：

```java
// 属性类型注册表
public static final Registry<AttributeType> ATTRIBUTE_TYPE_REGISTRY =
    new RegistryBuilder<>(ATTRIBUTE_TYPE_REGISTRY_KEY)
        .sync(true)   // 客户端-服务端同步
        .create();

// 角色注册表
public static final Registry<PGCharacter> CHARACTER_REGISTRY =
    new RegistryBuilder<>(CHARACTER_REGISTRY_KEY)
        .sync(true)
        .create();

// 角色效果注册表
public static final Registry<ICharacterEffect> CHARACTER_EFFECT_REGISTRY =
    new RegistryBuilder<>(CHARACTER_EFFECT_REGISTRY_KEY)
        .sync(true)
        .defaultKey(Minegenshin.id("empty"))  // 默认值占位
        .maxId(256)                            // 最多256种效果
        .create();
```

每个注册表同时创建对应的 `DeferredRegister` 供子类注册：

```java
public static final DeferredRegister<AttributeType> ATTRIBUTE_TYPES =
    DeferredRegister.create(ATTRIBUTE_TYPE_REGISTRY, Minegenshin.MOD_ID);

public static final DeferredRegister<PGCharacter> CHARACTERS =
    DeferredRegister.create(CHARACTER_REGISTRY, Minegenshin.MOD_ID);

public static final DeferredRegister<ICharacterEffect> CHARACTER_EFFECTS =
    DeferredRegister.create(CHARACTER_EFFECT_REGISTRY, Minegenshin.MOD_ID);
```

#### 注册示例

位置：`registry/register/CharacterEffectRegister.java`

```java
public class CharacterEffectRegister {
    public static final DeferredRegister<ICharacterEffect> CHARACTER_EFFECTS = 
        ModRegistries.CHARACTER_EFFECTS;

    public static final DeferredHolder<ICharacterEffect, IcyQuillEffect> 
        ICY_QUILL_EFFECT = CHARACTER_EFFECTS.register("icy_quill", IcyQuillEffect::new);

    public static void register(IEventBus eventBus) {
        CHARACTER_EFFECTS.register(eventBus);
    }
}
```

#### 主入口注册

位置：`Minegenshin.java` 构造函数

```java
public Minegenshin(IEventBus modEventBus, ModContainer modContainer) {
    CharacterRegister.CHARACTERS.register(modEventBus);
    AttachmentRegistration.register(modEventBus);
    CharacterEffectRegister.register(modEventBus);
    ModAttributes.ATTRIBUTES.register(modEventBus);
    EntityRegister.register(modEventBus);
    DamageTypeRegister.register(modEventBus);
    // ...
}
```

### 4.2 角色系统

#### PGCharacter 基类构造

```java
public PGCharacter(
    int characterUUID,                          // 唯一标识（如 135001）
    int weaponType,                             // 武器类型
    Component name,                             // 显示名称
    ElementalsGIM element,                      // 元素类型
    CharacterAscendAttribute ascendAttribute,   // 突破属性类型
    int skillCooldownTick,                      // E技能CD (tick)
    int burstCooldownTick,                     // Q技能CD (tick)
    int maxEnergy,                              // 元素能量上限
    float defaultSkillDamage,                   // 默认技能伤害系数
    String characterId,                         // 注册名
    Map<Identifier, Supplier<List<? extends Integer>>> statGrowthMap  // 属性成长曲线
)
```

#### 关键方法

| 方法 | 说明 |
|------|------|
| `getData()` | 获取角色状态数据 PGCharacterData |
| `addExp(int amount)` | 增加经验值，自动触发升级检查 |
| `performElementalSkill(Player, int skillTime)` | RPC 调用入口，处理 CD 后调用 triggerElementalSkill |
| `performElementalBurst(Player)` | RPC 调用入口，处理能量后调用 triggerElementalBurst |
| `triggerElementalSkill(Player, int skillTime)` | **子类实现点**：E 技能具体逻辑 |
| `triggerElementalBurst(Player)` | **子类实现点**：Q 技能具体逻辑 |
| `tick(Player)` | 每 tick 更新（CD、能量、效果 duration） |
| `getCharacterUUID()` / `getName()` / `getElement()` | 基础属性读取 |

#### 角色 Tick 逻辑链

```
CharacterTickEvent.onPlayerTick(PlayerTickEvent.Post)
    │
    ├─ 读取 PlayerCharactersAttachment
    │     └─ attachment = player.getData(PLAYER_CHARACTERS_ATTACHMENT)
    │
    ├─ 服务端：tick 队伍角色（最多4人）
    │     └─ for uuid in partyCharacterUUIDs
    │           └─ character = CharacterHelper.getCharacterByUUID(player, uuid)
    │                 └─ PGCharacter.tick(player)
    │                       ├─ elementalSkillCooldownTick--  (到0可按E)
    │                       ├─ elementalBurstCooldownTick--  (到0可按Q)
    │                       ├─ currentObtainingEnergy = min(maxEnergy, current + perTick)
    │                       └─ effectContainer.tick() → 逐效果递减 duration
    │
    └─ 同步脏数据
          └─ for character in ownedCharacters
                └─ characterData.isDirty()? → syncSingleCharacterToPlayer/Server()
```

#### 角色实现示例

位置：`core/character/polearm/Shenhe.java`

```java
public class Shenhe extends PGCharacter {
    public Shenhe() {
        super(
            135001,                              // UUID
            5,                                    // 长柄武器
            Component.translatable("character.name.shenhe"),
            ElementalsGIM.CYRO,
            CharacterAscendAttribute.ATK,
            10 * 20,                              // E技能CD 10秒
            15 * 20,                              // Q技能CD 15秒
            10 * 20,                              // 能量恢复
            80f,                                  // 默认技能伤害系数
            "shenhe",                             // 注册名
            Map.of(
                ModAttributes.MAX_HP.getId(), Config.SHENHE_HP,
                ModAttributes.ATK.getId(), Config.SHENHE_ATK,
                ModAttributes.DEF.getId(), Config.SHENHE_DEF
            )
        );
        data.setElementalSkillStacks(2);  // 战技可存储2层
    }

    @Override
    protected void triggerElementalSkill(Player player, int skillTime) {
        PlayerCharactersAttachment attachment = 
            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        
        // 为队伍4人分别添加冰凌效果
        for (int i = 0; i < 4; i++) {
            PGCharacter partyChar = attachment.getPartyCharacter(i);
            if (partyChar != null) {
                CharacterEffectInstance effect = new CharacterEffectInstance(
                    CharacterEffectRegister.ICY_QUILL_EFFECT.get(),
                    200,    // 持续 200 tick = 10秒
                    1       // amplifier
                );
                effect.setIntData(IcyQuillEffect.ICY_QUILL_COUNT_KEY, 7);
                CharacterEffectHelper.addEffect(player, partyChar, effect);
            }
        }
    }

    @Override
    protected void triggerElementalBurst(Player player) {
        TalismanSpiritArea field = EntityRegister.FIELD_TALISMAN_SPIRIT.get()
            .create(player.level(), EntitySpawnReason.EVENT);
        field.setPos(player.position());
        player.level().addFreshEntity(field);
    }

    @Override
    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of(
            ModAttributes.MAX_HP.getId(), Config.SHENHE_HP,
            ModAttributes.ATK.getId(), Config.SHENHE_ATK,
            ModAttributes.DEF.getId(), Config.SHENHE_DEF
        );
    }
}
```

#### 从玩家获取角色

```java
// 获取当前前台角色
PGCharacter currentChar = CharacterHelper.getCurrentCharacter(player);

// 根据 UUID 获取特定角色
PGCharacter shenhe = CharacterHelper.getCharacterByUUID(player, 135001);

// 直接从附件获取队伍角色
PlayerCharactersAttachment attachment = 
    player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
PGCharacter partyChar = attachment.getPartyCharacter(0);  // 队伍第1人
boolean hasChar = attachment.hasCharacter(135001);        // 是否拥有某角色
```

### 4.3 属性系统

#### AttributeType —— 属性类型标识

`AttributeType` 是 Java Record，作为属性的唯一标识：

```java
public record AttributeType(
    Identifier id,           // 注册表 ID (如 minegenshin:max_hp)
    String translationKey,   // 翻译键
    float defaultValue       // 默认值
)

// 快捷构造
new AttributeType("max_hp", "attribute.minegenshin.max_hp", 0)
```

注册方式：

```java
public class ModAttributes {
    public static final DeferredHolder<AttributeType, AttributeType> MAX_HP =
        ATTRIBUTES.register("max_hp", 
            () -> new AttributeType("max_hp", "attribute.minegenshin.max_hp", 0));

    public static final DeferredHolder<AttributeType, AttributeType> ATK =
        ATTRIBUTES.register("atk",
            () -> new AttributeType("atk", "attribute.minegenshin.atk", 0));

    public static final DeferredHolder<AttributeType, AttributeType> DEF =
        ATTRIBUTES.register("def",
            () -> new AttributeType("def", "attribute.minegenshin.def", 0));
}
```

#### AttributeContainer —— 属性容器

管理角色的基础值和修饰器：

```java
AttributeContainer container = character.getData().getAttributeContainer();

// 计算总属性值
double totalAtk = container.getTotalValue(ModAttributes.ATK.value());

// 修改基础值（通常只在等级变化时）
container.setBaseValue(ModAttributes.ATK.value(), 300);

// 添加永久修饰器（来源名作为 key，可用于后续移除）
container.addFlatModifier(ModAttributes.ATK.value(), "weapon", 200);
container.addPercentModifier(ModAttributes.ATK.value(), "artifact", 0.2f);

// 添加临时修饰器（效果用，效果过期后需手动移除）
container.addTempFlatModifier(ModAttributes.ATK.value(), "buff", 100);

// 移除修饰器
container.removeModifier(ModAttributes.ATK.value(), "weapon");
```

#### 总属性值计算公式

```
total = baseValue * (1 + permanentPercent + tempPercent)
      + permanentFlat + tempFlat
```

#### AttributeInstance —— 单个属性实例

```java
public class AttributeInstance {
    private double baseValue;           // 基础值
    private double permanentFlat;       // 永久固定加成
    private double permanentPercent;    // 永久百分比加成
    private double tempFlat;            // 临时固定加成
    private double tempPercent;         // 临时百分比加成

    public double getTotalValue() {
        return baseValue * (1 + permanentPercent + tempPercent)
             + permanentFlat + tempFlat;
    }
}
```

### 4.4 效果系统 (CharacterEffect)

#### ICharacterEffect 接口

位置：`content/effect/character/ICharacterEffect.java`

所有角色效果的实现接口，方法均为 default 可选择性实现：

| 方法 | 触发时机 | 典型用途 |
|------|----------|----------|
| `onEffectAdded()` | 效果首次添加 | 初始化状态、应用临时修饰器 |
| `onEffectRemoved()` | 效果移除 | 清理修饰器、恢复原状态 |
| `onEffectOverride()` | 重复添加相同效果 | 自定义叠加规则 |
| `onEffectFrontTick()` | 角色处于前台时每 tick | 前台专属逻辑 |
| `onEffectBackTick()` | 角色处于后台时每 tick | 后台逻辑（如璃月共鸣） |
| `onEffectTick()` | 不管前后台都调用 | 默认递减 duration，返回 false 移除效果 |
| `onAttacked()` | 角色造成攻击时 | 修改伤害规格（加伤、减伤） |
| `isInstantaneous()` | 查询效果性质 | 标记一次性效果 |

#### 实现效果 —— 完整示例

位置：`content/effect/character/shenhe/IcyQuillEffect.java`

```java
public class IcyQuillEffect implements ICharacterEffect {
    public static final String ICY_QUILL_COUNT_KEY = "icy_quill_count";

    @Override
    public void onAttacked(Player holder, PGCharacter character, 
                           LivingEntity target, CharacterEffectInstance instance,
                           ModDamageSource damageSource) {
        
        ModDamageSpec oldSpec = damageSource.getSpec();
        
        // 只对冰伤生效
        if (oldSpec.getElement() == ElementalsGIM.CYRO) {
            PlayerCharactersAttachment attachment = 
                holder.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            PGCharacter shenhe = attachment.getCharacterByUUID(
                CharacterRegister.SHENHE.get().getCharacterUUID());

            if (shenhe != null) {
                float bonus = (float)(
                    shenhe.getData().getAttributeTotalValue(ModAttributes.ATK.value()) 
                    * 0.776f
                );
                ModDamageSpec newSpec = oldSpec.withFlatDamageBonus(bonus);
                damageSource.setSpec(newSpec);
            }
        }
    }

    @Override
    public void onEffectOverride(Player holder, PGCharacter character,
                                 CharacterEffectInstance existing,
                                 CharacterEffectInstance newInstance) {
        // 持续时间取较大值
        newInstance.setDuration(Math.max(
            existing.getDuration(), newInstance.getDuration()));
        // 冰凌数量取较大值
        newInstance.setIntData(ICY_QUILL_COUNT_KEY, 
            Math.max(
                existing.getIntData(ICY_QUILL_COUNT_KEY), 
                newInstance.getIntData(ICY_QUILL_COUNT_KEY)));
    }
}
```

#### CharacterEffectInstance —— 效果实例

```java
// 创建效果实例
CharacterEffectInstance effect = new CharacterEffectInstance(
    CharacterEffectRegister.ICY_QUILL_EFFECT.get(),  // ICharacterEffect 实现
    200,                                              // 持续 200 tick (10秒)
    1,                                                // amplifier
    false,                                            // 是否在 HUD 隐藏
    new CompoundTag()                                 // 额外数据
);

// 快捷构造（不带额外数据）
CharacterEffectInstance effect = new CharacterEffectInstance(
    CharacterEffectRegister.ICY_QUILL_EFFECT.get(), 200, 1);

// 设置效果特有数据
effect.setIntData("icy_quill_count", 7);
effect.setBooleanData("some_flag", true);
effect.setFloatData("bonus", 1.5f);

// 深拷贝
CharacterEffectInstance copy = effect.copy();

// INFINITE 常量表示无限持续
CharacterEffectInstance.INFINITE
```

#### CharacterEffectHelper —— 效果操作工具类

```java
// 添加效果（自动调用 onEffectAdded 或 onEffectOverride）
CharacterEffectHelper.addEffect(player, character, effectInstance);

// 移除效果
CharacterEffectHelper.removeEffect(player, character, IcyQuillEffect.INSTANCE);

// 清除所有效果
CharacterEffectHelper.clearEffects(player, character);

// 查询效果
boolean hasEffect = CharacterEffectHelper.hasEffect(characterData, IcyQuillEffect.INSTANCE);
CharacterEffectInstance inst = CharacterEffectHelper.getEffectInstance(characterData, IcyQuillEffect.INSTANCE);
```

#### 注册效果

位置：`registry/register/CharacterEffectRegister.java`

```java
public class CharacterEffectRegister {
    public static final DeferredHolder<ICharacterEffect, IcyQuillEffect> 
        ICY_QUILL_EFFECT = CHARACTER_EFFECTS.register("icy_quill", IcyQuillEffect::new);

    public static void register(IEventBus eventBus) {
        CHARACTER_EFFECTS.register(eventBus);
    }
}
```

#### 效果 Tick 逻辑链

```
CharacterEffectEvent.characterEffectTick(PlayerTickEvent.Post)
    │
    └─ 队伍角色循环（最多4人）
          └─ for i in 0..3
                └─ character = attachment.getPartyCharacter(i)
                      │
                      └─ 复制 effects 列表（避免并发修改）
                            └─ for effect in copiedEffects
                                  ├─ ICharacterEffect.onEffectBackTick()    ← 后台逻辑
                                  ├─ ICharacterEffect.onEffectFrontTick()   ← 前台逻辑
                                  │
                                  └─ ICharacterEffect.onEffectTick()         ← 返回 false 移除
                                        ├─ true  → 保留（内部 duration--）
                                        └─ false → CharacterEffectHelper.removeEffect()
                                              ├─ onEffectRemoved() 清理修饰器
                                              └─ 从 container 列表移除
```

#### 添加效果逻辑链

```
CharacterEffectHelper.addEffect(player, character, instance)
    │
    ├─ 检查是否已存在同类效果
    │     ├─ 不存在 → effectContainer.add(instance)
    │     │              └─ instance.getEffect().onEffectAdded()  ← 初始化修饰器
    │     │
    │     └─ 已存在 → 合并
    │           └─ existing.getEffect().onEffectOverride(existing, instance)
    │                 └─ example: duration = max(existing.duration, new.duration)
```

### 4.5 伤害系统与衰减机制

#### 概述

伤害管线设计参考原神的攻击衰减系统：

1. **ModDamageSpec** —— 纯数据类，携带一次攻击的完整参数
2. **ModDamageSource** —— 继承 MC 的 `DamageSource`，嵌入 ModDamageSpec
3. **DecayCounterManager** —— 目标实体上的计时计数器管理器
4. **DecaySequence** —— 三种序列（元素/伤害/削韧）的系数数组
5. **DecayGroup** —— 清除时间 + 三个序列
6. **HurtEntityHelper** —— 整合所有计算的最终入口

#### ModDamageSpec —— 伤害规格

```java
// 方式1：快速工厂方法（物理伤害）
ModDamageSpec physicalSpec = ModDamageSpec.physical(AttackType.NORMAL_ATTACK, 1.5f);

// 方式2：快速工厂方法（元素伤害，默认元素量1.0）
ModDamageSpec elementalSpec = ModDamageSpec.elemental(
    AttackType.ELEMENTAL_SKILL,   // 攻击类型
    ElementalsGIM.PYRO,           // 元素类型
    2.5f                           // 伤害倍率
);

// 方式3：Builder 模式（最灵活）
ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ElementalsGIM.CYRO)
    .multiplier(1.5f)          // ATK 乘以的系数
    .flatBonus(0.0f)           // 固定额外伤害
    .elementAmount(1.0f)       // 元素附着量
    .decayGroup(DecayGroup.DEFAULT)  // 衰减组别（null = 默认）
    .build();
```

**关键字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `attackType` | `AttackType` | 攻击分类，携带衰减标签 |
| `element` | `ElementalsGIM` | 元素类型（FYSIKOS=物理） |
| `damageMultiplier` | `float` | 伤害倍率 |
| `flatDamageBonus` | `float` | 固定额外伤害 |
| `elementAmount` | `float` | 元素附着基础量 |
| `decayGroup` | `DecayGroup` | 自定义衰减组别（null = 默认） |

#### AttackType —— 攻击分类（带衰减标签）

| 类型 | 衰减标签 | 说明 |
|------|----------|------|
| `NORMAL_ATTACK` | `"normal_attack"` | 普通攻击 |
| `CHARGED_ATTACK` | `"charged_attack"` | 重击 |
| `PLUNGING_ATTACK` | `"plunging_attack"` | 下落攻击 |
| `ELEMENTAL_SKILL` | `"elemental_skill"` | 元素战技 |
| `ELEMENTAL_BURST` | `"elemental_burst"` | 元素爆发 |
| `SPECIAL` | `null` | 不参与附着冷却 |
| `MONSTER` | `null` | 怪物伤害，不参与附着冷却 |

#### ElementalsGIM —— 元素类型

| 枚举值 | 说明 |
|--------|------|
| `FYSIKOS` | 物理（希腊语 "Physical"，不参与元素反应） |
| `PYRO` / `HYDRO` / `CYRO` / `ELECTRO` / `ANEMO` / `GEO` / `DENDRO` | 火/水/冰/雷/风/岩/草 |

#### DecaySequence —— 衰减序列

序列是系数数组，按攻击命中次数查表得到系数。超过序列长度后系数为 0。

```java
// 默认序列
DecaySequence.DEFAULT_ELEMENT    // [1,0,0, 1,0,0, ...] 长度24 → 每3次附着1次
DecaySequence.DEFAULT_DAMAGE     // [1,1,1, ...] 长度15 → 伤害不衰减
DecaySequence.DEFAULT_POISE      // [1,1,1, ...] 长度14 → 削韧不衰减

// 自定义序列
// 申鹤战技：只有第1次有附着
DecaySequence shenheElement = DecaySequence.of(1f, 0f, 0f, 0f, 0f, 0f, 0f);

// 长柄重击：只有第1次有伤害
DecaySequence polearmHeavy = DecaySequence.of(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);

// 绫华重击：前3次有伤害
DecaySequence ayakaHeavy = DecaySequence.createRepeating(
    new float[]{1f, 1f, 1f, 0f}, 7
);
```

#### DecayGroup —— 衰减组别

一个衰减组别定义了计时器和三组序列：

```java
// 默认组别（大多数攻击使用）
DecayGroup.DEFAULT

// 自定义组别
DecayGroup shenheGroup = new DecayGroup(
    2,                                                              // 清除时间：2 tick = 0.1秒
    DecaySequence.of(1f, 0f, 0f, 0f, 0f, 0f, 0f),                  // 元素量序列
    DecaySequence.DEFAULT_DAMAGE,                                    // 伤害序列
    DecaySequence.DEFAULT_POISE                                     // 削韧序列
);
```

默认组别参数：
- 清除时间 50 tick = 2.5 秒
- 元素量序列 [1,0,0] x 8 = 每3次附着1次
- 伤害序列 全1，长度15
- 削韧序列 全1，长度14

#### HurtEntityHelper —— 完整伤害管线

v0.2.0 重构为纯计算工具类，暴露唯一入口 `calculateFinalModDamage(ModDamageSource, PGCharacter, LivingEntity)`，内部拆解为三个私有阶段。**整个管线只在 LivingEntityHurtMixin 中被调用一次**，不经过原版 `target.hurt()` 避免二次计算。

```
calculateFinalModDamage(damageSource, attacker, target)
    │
    ├─ Phase 1: 效果修改（onAttacked）
    │     └─ for effect in new ArrayList<>(attacker effects)   ← 快照遍历防并发
    │           └─ effect.onAttacked(player, attacker, target, effect, damageSource)
    │     └─ spec = damageSource.getSpec()   ← 刷新引用，防止 flatBonus 丢失
    │
    ├─ Phase 2: calculateCharacterDamage —— 基础伤害区
    │     ├─ 读取 ATK / HP / DEF / EM 属性快照
    │     └─ base = (
    │           atk * atkMult + hp * hpMult + def * defMult + em * emMult
    │         ) * (1 + skillMultBonus) + flatBonus
    │
    └─ Phase 3: processPipeline —— 衰减→附着→反应→乘区
          │
          ├─ 衰减
          │     ├─ spec.hasDecayTag() → manager.processHit()
          │     └─ decayResult → elementCoef / damageCoef / poiseCoef
          │
          ├─ 元素附着
          │     ├─ hasAuraPotential() && elementCoef > 0
          │     └─ ElementalAttachmentHelper.attach(target, element, source, profile)
          │
          ├─ 元素反应
          │     └─ ElementalReactionManager.tryReactAfterAttach(ctx)
          │
          ├─ calculateFinalDamage —— 乘区计算
          │     ├─ critZone(attacker)          → crit
          │     ├─ dmgBonusZone(attacker, spec) → bonus
          │     ├─ defenseZone(attacker, target) → def    (CombatMath.levelCoefficient + defenseZone)
          │     ├─ resistanceZone(element, target) → res   (CombatEntityAccessor 统一取抗性)
          │     │
          │     └─ 反应修正
          │           ├─ MELT / VAPORIZE → crit × amplifyMult × emReactionBonus × bonus × def × res
          │           └─ 其他 → crit × bonus × def × res
          │
          └─ finalDamage ×= decayResult.getDamageCoefficient()
             return finalDamage
```

#### 乘区公式

```
critZone()       → 1.0 + CRIT_RATE
dmgBonusZone()   → 1.0 + DMG_BONUS
defenseZone()    → atkLevelCoef / (atkLevelCoef + targetDefense)
resistanceZone() → 1.0 - elementResistance
CombatMath.levelCoefficient(level) → 500 + level × 50
```

#### 攻击→伤害→衰减→附着→反应 端到端流程

```
玩家攻击（原神模式开）
    │
    ▼
PlayerAttackInterceptor (Mixin, 注入 Player.attack HEAD)
    ├─ ci.cancel()
    ├─ buildModDamageSource(spec + attackerCharacter)
    └─ target.hurtServer(serverLevel, modSource, 0f)    ← 传 0f，MOD 管线自己算
          │
          ▼
目标实体 hurtServer 分发：
    │
    ├─ TeyvatLivingEntity → 子类自行重写 hurtServer → 直接调 calculateFinalModDamage
    │
    └─ 非 TeyvatLivingEntity → LivingEntityHurtMixin (Mixin, 注入 LivingEntity.hurtServer HEAD)
          │
          ├─ source instanceof ModDamageSource？
          │     ├─ 是 → calculateFinalModDamage(modSource, attackerChar, target)
          │     │     ├─ Phase 1: onAttacked 效果处理
          │     │     ├─ Phase 2: 基础伤害区 (ATK * mult + flatBonus + HP/DEF/EM 乘区 + skillMultBonus)
          │     │     └─ Phase 3: 衰减 → 附着 → 反应 → 防御区 → 抗性区 → 最终乘区
          │     │
          │     ├─ 扣血
          │     │     ├─ Player + 原神模式开 → character.hurt() → incapacitate()
          │     │     └─ 其他 → target.setHealth(max(0, health - finalDamage))
          │     │
          │     ├─ level.broadcastDamageEvent(target, source)  ← 客户端同步
          │     └─ cir.setReturnValue(true)  ← 阻断原版 hurtServer 全部后续（invulnerableTime / actuallyHurt）
          │
          └─ 否 → return（放行原版 hurtServer）
```

#### 衰减计数器清理逻辑链

```
DecayCounterWorker (后台线程，服务端启动时初始化)
    │
    └─ 定期扫描所有已注册的 DecayCounterManager
          └─ for counter in manager.allCounters
                └─ currentTick - startTime > clearTime?
                      ├─ true → 从 map 移除 counter
                      └─ false → 保留继续累积 hitCount
```

#### ModDamageSource —— 自定义伤害源

```java
// 最常用：从 DamageSpec 和攻击者创建
ModDamageSource source = ModDamageSource.from(damageSpec, player);

// 手动指定 DamageType
ModDamageSource source = new ModDamageSource(
    DamageTypeRegister.NORMAL_ATTACK_TYPE,  // DamageType Holder
    player,                                  // 直接伤害源
    spec                                     // DamageSpec
);

// 获取和修改内部 Spec
ModDamageSpec spec = source.getSpec();
source.setSpec(newSpec);
```

#### DecayCounterManager —— 计时计数器管理

每个 LivingEntity 身上都有一个 `DecayCounterManager`（通过 Mixin 注入），存储多个独立的计数器。

计数器 Key 格式：`attackerUuid:characterId:decayTag:groupId`

```java
IDecayCounterHolder holder = (IDecayCounterHolder) targetEntity;
DecayCounterManager manager = holder.getDecayCounterManager();

// 主线程处理攻击 → 获取衰减结果
DecayResult result = manager.processHit(attacker, character, spec, gameTime);
float elementCoef = result.getElementCoefficient();
float damageCoef  = result.getDamageCoefficient();
float poiseCoef   = result.getPoiseCoefficient();
```

Worker 线程（`DecayCounterWorker`）定期扫描超时计数器并清理，在服务端启动时初始化。

### 4.6 玩家附件 (Attachment)

位置：`core/attachment/AttachmentRegistration.java`

附件是 NeoForge 推荐的存储玩家数据的方式。

#### 注册附件

```java
// 原石附件（简单整数）
ATTACHMENTS.register("player_primogem",
    () -> AttachmentType.builder(() -> 0)
        .serialize(Codec.INT.fieldOf("primogem"))
        .sync(StreamCodec.of(
            FriendlyByteBuf::writeInt,
            FriendlyByteBuf::readInt
        ))
        .copyOnDeath()
        .build()
);

// 角色附件（复杂对象，使用 LowDragLib 的 IPersistedSerializable）
ATTACHMENTS.register("player_characters",
    () -> AttachmentType.serializable(PlayerCharactersAttachment::new)
        .copyOnDeath()
        .build()
);
```

#### 使用附件

```java
// 读取数据
int primogem = player.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);

// 写入数据
player.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, 160);

// 角色附件
PlayerCharactersAttachment attachment = 
    player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
PGCharacter currentChar = attachment.getCurrentCharacter();
List<Integer> partyUuids = attachment.getPartyCharacterUUIDs();
attachment.setPartyCharacterToServer(0, shenheUuid);
```

### 4.7 网络同步系统

#### RPC 框架

使用 LowDragLib2 的 RPC 注解驱动机制：

```java
public class NetworkManager {

    // 定义 RPC 方法 —— sender.isServer() 判断当前在哪一侧
    @RPCPacket("primogemRPCPacket")
    public static void primogemRPCPacket(RPCSender sender, int amount) {
        if (sender.isServer()) {
            // 服务端收到请求 → 修改数据
            ServerPlayer player = sender.asPlayer();
            player.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT.get(), amount);
        } else {
            // 客户端收到响应 → 更新显示
            ClientHandler.primogemClientHandler(amount);
        }
    }

    // 客户端 → 服务端
    public static void setPrimogemToServer(int amount) {
        RPCPacketDistributor.rpcToServer("primogemRPCPacket", amount);
    }

    // 服务端 → 客户端
    public static void setPrimogemToPlayer(ServerPlayer player, int amount) {
        RPCPacketDistributor.rpcToPlayer(player, "primogemRPCPacket", amount);
    }
}
```

#### 同步模式

```
客户端 ──rpcToServer──▶ 服务端（修改数据 + 持久化）
服务端 ──rpcToPlayer──▶ 客户端（更新显示）
```

#### 角色数据脏标记同步

位置：`event/server/CharacterTickEvent.java`

角色数据内部有 `dirty` 标记，每 tick 检查：

```java
for (PGCharacter character : attachment.getOwnedCharacters()) {
    if (character.getData().isDirty()) {
        character.getData().clearDirty();
        if (player instanceof ServerPlayer serverPlayer) {
            attachment.syncSingleCharacterToPlayer(serverPlayer, character);
        } else {
            attachment.syncSingleCharacterToServer(character);
        }
    }
}
```

#### ClientHandler —— 客户端处理侧

位置：`core/network/ClientHandler.java`

所有从服务端收到的同步请求都在这里处理：

```java
public class ClientHandler {
    public static void primogemClientHandler(int amount) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, amount);
        }
    }
    // ...
}
```

### 4.8 事件系统

所有事件监听类都使用 `@EventBusSubscriber` 注解。

#### CharacterTickEvent —— 角色每 tick

位置：`event/server/CharacterTickEvent.java`

更新队伍角色 tick（CD、能量等），并同步脏数据：

```java
@SubscribeEvent
public static void onPlayerTick(PlayerTickEvent.Post event) {
    Player player = event.getEntity();
    PlayerCharactersAttachment attachment = 
        player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);

    // 更新队伍角色 tick（仅服务端）
    if (!player.level().isClientSide()) {
        for (int uuid : attachment.getPartyCharacterUUIDs()) {
            PGCharacter character = CharacterHelper.getCharacterByUUID(player, uuid);
            if (character != null) {
                character.tick(player);
            }
        }
    }

    // 同步脏数据
    for (PGCharacter character : attachment.getOwnedCharacters()) {
        if (character.getData().isDirty()) {
            character.getData().clearDirty();
            // 双向同步...
        }
    }
}
```

#### CharacterEffectEvent —— 效果每 tick

位置：`event/server/CharacterEffectEvent.java`

遍历所有队伍角色的所有效果，调用前后台 Tick 和默认 Tick：

```java
@SubscribeEvent
public static void characterEffectTick(PlayerTickEvent.Post event) {
    // ...
    for (int i = 0; i < 4; i++) {
        PGCharacter character = attachment.getPartyCharacter(i);
        if (character != null) {
            List<CharacterEffectInstance> effects = new ArrayList<>(
                character.getData().getEffectContainer().getEffects()
            );
            for (CharacterEffectInstance effect : effects) {
                effect.getEffect().onEffectBackTick(player, character, effect);
                effect.getEffect().onEffectFrontTick(player, character, effect);
                if (!effect.getEffect().onEffectTick(player, character, effect)) {
                    CharacterEffectHelper.removeEffect(player, character, effect.getEffect());
                }
            }
        }
    }
}
```

#### PlayerLoginEventListeners —— 玩家登录全量同步

位置：`event/server/PlayerLoginEventListeners.java`

登录时触发 `CharacterDataSyncEventHandler.handle()` 进行全量同步。

### 4.9 实体系统

#### 实体注册

位置：`registry/register/EntityRegister.java`

```java
public class EntityRegister {
    public static final Supplier<EntityType<TalismanSpiritArea>> FIELD_TALISMAN_SPIRIT =
        ENTITIES.register("talisman_spirit",
            () -> EntityType.Builder.of(TalismanSpiritArea::new, MobCategory.CREATURE)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Minegenshin.id("talisman_spirit")))
        );

    public static final Supplier<EntityType<SlimeCyro>> SLIME_CYRO =
        ENTITIES.register("slime_cyro",
            () -> EntityType.Builder.of(SlimeCyro::new, MobCategory.MONSTER)
                .sized(1.3964844F, 1.6F)
                .eyeHeight(1.52F)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Minegenshin.id("slime_cyro")))
        );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
        eventBus.addListener(EntityRegister::registerEntityAttributes);
    }
}
```

#### 生成实体

```java
TalismanSpiritArea field = EntityRegister.FIELD_TALISMAN_SPIRIT.get()
    .create(player.level(), EntitySpawnReason.EVENT);
if (field != null) {
    field.setPos(player.position());
    field.setOwner(player, characterUUID);
    player.level().addFreshEntity(field);
}
```

### 4.10 元素附着系统

#### 概述

元素附着系统是还原原神七元素附着机制的核心系统。攻击命中目标后，根据元素类型和附着量在目标身上创建附着效果实例（ElementalAttachmentInstance），实例随时间自动衰减，可被其他元素反应消耗。

**两层架构**：

| 层 | 包 | 职责 |
|----|----|------|
| **桥接层** | `core.status` + `core.attachment` | 定义 StatusInstance 基类、StatusContainer 容器、StatusAccessor 宿主访问器 |
| **元素层** | `core.system.about` | ElementalAttachmentInstance（元素特有字段）、ElementalAttachmentHelper（附着入口）、AttachmentProfile（衰减预设） |

依赖方向：元素层 → 桥接层（元素层继承 StatusInstance、往 StatusContainer 里 add）。

#### 宿主兼容性

附着可以附加到四种宿主，通过 StatusAccessor 统一入口：

| 宿主类型 | 存储方式 | 说明 |
|----------|----------|------|
| `LivingEntity` | NeoForge Attachment | `AttachmentRegistration.CONTAINER` |
| `BlockEntity` | NeoForge Attachment | 同上 |
| `PGCharacterData` | `@Persisted` 字段 | `PGCharacterData.statusContainer` |
| `ItemStack` | DataComponent | `StatusDataComponents.CONTAINER` |

```java
StatusAccessor.of(livingEntity);   // LivingEntity → 自动创建/读取 Attachment
StatusAccessor.of(blockEntity);    // BlockEntity → 同上
StatusAccessor.of(characterData);  // PGCharacterData → 直接返回字段
StatusAccessor.of(itemStack);      // ItemStack → get DataComponent，null 返回 EMPTY
```

#### StatusInstance —— 叠加实例基类

所有能存进 StatusContainer 的东西都必须继承 StatusInstance：

```java
public class StatusInstance implements IPersistedSerializable {
    @Persisted(key = "type_id")
    protected String typeId;

    public String getTypeId() { return typeId; }
    public void tick() {}           // 每 tick 让生命周期流逝
    public boolean isFinished() { return false; }  // 是否该被移除
    public void onRemove() {}       // 移除时钩子（自然结束/强制清除）
    public StatusInstance copy() { return null; }   // 深拷贝
}
```

LDLib2 多态序列化机制：`@Persisted List<StatusInstance>` 序列化时每条先写 `type_id` 字段，反序列化时按 `type_id` 值找到对应子类实例化再读取字段。**子类必须提供无参构造并在其中设置 `this.typeId = "xxx"`**。

#### ElementalAttachmentInstance —— 元素附着实例

| 字段 | 类型 | 说明 |
|------|------|------|
| `element` | `ElementalsGIM` | 元素类型（HYDRO/CYRO/PYRO/...） |
| `source` | `AttachmentSource` | 附着来源（NORMAL_ATTACK/ELEMENTAL_SKILL/...） |
| `profile` | `AttachmentProfile` | 附着预设（衰减速率、持续时间等） |
| `quantity` | `float` | 当前附着量（单位 U） |
| `currentDecayPerSecond` | `float` | 当前每秒衰减速率 U/s |
| `permanent` | `boolean` | 是否永久附着（false = 会衰减消失） |

#### AttachmentProfile —— 附着预设

预设决定附着的衰减参数：

```java
// 弱附着：1U，衰减 0.084U/s，约 7 秒消失
AttachmentProfile.WEAK     // normal(1.0f)

// 中附着：1.5U，衰减更快，约 9.5 秒
AttachmentProfile.MEDIUM   // normal(1.5f)

// 强附着：2U，衰减最快，约 12 秒
AttachmentProfile.STRONG   // normal(2.0f)

// 超强附着：4U
AttachmentProfile.ULTRA_STRONG  // normal(4.0f)

// 永久附着：4U，不衰减，自动补充
AttachmentProfile.PERMANENT
```

公式：
```
baseQuantity = x
actualQuantity = baseQuantity × 0.8    // 损耗系数
decayPerSecond = (0.8 × x) / (7 + 2.5 × x)    // 原神还原公式
duration = baseQuantity / decayPerSecond
permanent = durationSeconds < 0
```

#### 附着入口 —— ElementalAttachmentHelper

```java
// 四种宿主的 attach 方法
ElementalAttachmentHelper.attach(livingEntity, element, source, profile);
ElementalAttachmentHelper.attach(blockEntity, element, source, profile);
ElementalAttachmentHelper.attach(characterData, element, source, profile);
ElementalAttachmentHelper.attach(itemStack, element, source, profile);

// 消耗（元素反应调用）
float consumed = ElementalAttachmentHelper.consume(livingEntity, element, amount);
```

#### 附着逻辑链（doAttach 内部）

```
doAttach(container, element, source, profile)
    │
    ├─ 1. actualQuantity = profile.actualQuantity()  ← base × lossMultiplier(0.8)
    │
    ├─ 2. 查找已有实例 findMatching()
    │     └─ 条件：同元素 && 同 source && 未 finished
    │
    ├─ 3. existing == null → 新建实例
    │     └─ new ElementalAttachmentInstance(element, source, profile, actualQuantity)
    │     └─ container.add(newInst)
    │
    └─ 4. existing != null
          ├─ actualQuantity ≤ existing.quantity → 不覆盖，直接 return
          └─ actualQuantity > existing.quantity → 量多覆盖
                ├─ existing.refreshQuantity(actualQuantity)
                └─ element.canOverrideDecay()?
                      ├─ true（PYRO/ELECTRO/燃元素）→ 替换衰减速率
                      └─ false → 继承原衰减速率
```

#### 完整攻击→附着逻辑链

```
PlayerAttackInterceptor.onPlayerAttack()
    │
    ├─ buildModDamageSource()
    │     └─ genshinMode && characterUUID == 145001 → ModDamageSpec.elemental(NORMAL_ATTACK, HYDRO, 1.0f)
    │
    └─ target.hurtServer(serverLevel, modSource, 0f)
          │
          ▼
LivingEntityHurtMixin (hurtServer HEAD)
    ├─ calculateFinalModDamage(modSource, attackerChar, target)
    │     ├─ [效果处理...]
    │     ├─ [基础伤害区...]
    │     └─ processPipeline
    │           ├─ [衰减...]
    │           │
    │           └─ ★ 元素附着 ★
    │                 ├─ spec.hasAuraPotential()  ← elementAmount > 0 && element != FYSIKOS
    │                 ├─ decayResult.getElementCoefficient() > 0  ← 衰减序列允许本次附着
    │                 │     └─ DEFAULT 序列 [1,0,0] → 每 3 次攻击 1 次有附着
    │                 │
    │                 ├─ chooseProfile(spec.getElementAmount())
    │                 │     └─ 1.0U → WEAK / 1.5U → MEDIUM / 2.0U → STRONG / 4.0U+ → ULTRA_STRONG
    │                 │
    │                 └─ ElementalAttachmentHelper.attach(target, HYDRO, NORMAL_ATTACK, profile)
    │                       └─ ElementalAttachmentHelper.doAttach(container, HYDRO, NORMAL_ATTACK, profile)
    │                             └─ (上面的 doAttach 逻辑链)
    │
    ├─ [扣血 + 广播...]
    └─ cir.setReturnValue(true)
```

#### 附着衰减逻辑链（每 tick）

```
StatusTickEvent.onEntityTick(EntityTickEvent.Post)
    │
    ├─ 遍历所有 LivingEntity（仅服务端）
    │     └─ living.getData(AttachmentRegistration.CONTAINER).tick()
    │           │
    │           └─ StatusContainer.tick()
    │                 │
    │                 └─ 遍历 instances
    │                       ├─ ElementalAttachmentInstance.tick()
    │                       │     ├─ permanent?
    │                       │     │     ├─ true → replenishTimer-- 到点补充 quantity
    │                       │     │     └─ false → quantity -= (decayPerSecond / 20)
    │                       │     │
    │                       │     └─ // TEST: 每秒打印附着状态
    │                       │
    │                       └─ inst.isFinished()?
    │                             ├─ true → inst.onRemove() + 从 list 移除
    │                             └─ false → 保留继续衰减
```

#### 覆盖规则总结

| 条件 | 行为 |
|------|------|
| 不同 source 的同元素 | 独立实例，互不干扰 |
| 同 source + 新附着量 ≤ 旧附着量 | 不覆盖 |
| 同 source + 新附着量 > 旧附着量 | 覆盖 quantity |
| 覆盖时 element.canOverrideDecay() == true | 替换衰减速率（PYRO/ELECTRO） |
| 覆盖时 element.canOverrideDecay() == false | 继承原衰减速率（HYDRO/CYRO/DENDRO/ANEMO/GEO） |

#### 序列化机制

所有涉及类都实现 `IPersistedSerializable` + `@Persisted` 注解，Codec 由 LDLib2 自动生成：

```java
// StatusContainer 自身 Codec（用在 Attachment 和 DataComponent）
public static final Codec<StatusContainer> CODEC = PersistedParser.createCodec(StatusContainer::new);
public static final StreamCodec<ByteBuf, StatusContainer> STREAM_CODEC = PersistedParser.createStreamCodec(StatusContainer::new);
```

⚠️ **Attachment 注册必须显式指定 Codec**，不能用 `AttachmentType.serializable(factory)`：

```java
// ✅ 正确：显式指定
AttachmentType.builder(StatusContainer::new)
    .serialize(StatusContainer.CODEC)
    .sync(StatusContainer.STREAM_CODEC)
    .copyOnDeath()
    .build()

// ❌ 错误：用默认 IPersistedSerializable 序列化可能丢失数据
AttachmentType.serializable(StatusContainer::new).build()
```

#### 关键文件索引

| 类 | 路径 |
|----|------|
| StatusInstance（基类） | `core/status/StatusInstance.java` |
| StatusContainer（容器） | `core/attachment/StatusContainer.java` |
| StatusAccessor（访问器） | `core/status/StatusAccessor.java` |
| ElementalAttachmentInstance | `core/system/about/ElementalAttachmentInstance.java` |
| ElementalAttachmentHelper | `core/system/about/ElementalAttachmentHelper.java` |
| AttachmentProfile | `core/system/about/AttachmentProfile.java` |
| AttachmentSource | `core/system/about/AttachmentSource.java` |
| AttachmentRegistration | `core/attachment/AttachmentRegistration.java` |
| StatusDataComponents | `core/system/registry/register/StatusDataComponents.java` |
| StatusTickEvent | `event/server/StatusTickEvent.java` |

### 4.11 元素反应系统

#### 概述

元素反应系统是还原原神七元素相互作用机制的核心系统。当攻击命中目标并成功附着元素后，管理器立即扫描目标身上已有的先手元素，按优先级顺序尝试触发反应，消耗双方元素并执行对应的反应效果（增幅伤害、额外伤害、状态异常等）。

**两层架构**：

| 层 | 包 | 职责 |
|----|----|------|
| **基类与注册层** | `core.system.reaction` | ElementalReaction 基类、ReactionContext/ReactionResult 数据类、ElementalReactionManager 管理器、ReactionPriorityCalculator 优先级计算器 |
| **具体反应层** | `core.system.reaction.builtin` | VaporizeReaction（蒸发）、MeltReaction（融化）、FreezeReaction（冻结）等 |

依赖方向：具体反应 → 基类 + 管理器 + 附着系统（ElementalAttachmentHelper.consume 消耗元素）。

#### 核心概念

**先手/后手**：目标身上已有的元素是"先手"，本次附着的元素是"后手"。

**消耗比**：每个反应注册时声明的消耗参数。`ratioA:ratioB` 表示双方元素同时按比例消耗，不是一方消耗另一方。例如蒸发消耗比为火:水 = 1:2，1 份火同时消耗 2 份水。

**克制关系**：消耗比中，消耗少的一方是克制方。蒸发中火 1 份消耗水 2 份 → 火克制水。

**类元素自动归并**：反应匹配时通过 `getMainElement()` 将类元素映射到主元素：
- FROZEN（冻）→ CYRO（冰）
- AGGRAVATE（激）→ DENDRO（草）
- BURNING（燃）→ PYRO（火）

所以融化反应注册的是 `(PYRO, CYRO)`，它自然能匹配到目标身上的 FROZEN。

**后手不残留规则**：反应执行完后如果后手元素还有剩余：
- 来源为 `NORMAL_ATTACK`（常规攻击附着）→ 清 0，不残留
- 其他来源（SPECIAL/SELF_ATTACH/ENVIRONMENTAL/WEAPON_ENCHANT）→ 保留剩余量

#### ElementalReaction 基类

位置：`core/system/reaction/ElementalReaction.java`

```java
public abstract class ElementalReaction {
    protected final ElementalsGIM elementA;   // 元素1
    protected final ElementalsGIM elementB;   // 元素2
    protected final float ratioA;             // 元素1消耗系数
    protected final float ratioB;             // 元素2消耗系数
    protected final int basePriority;         // 注册时声明的优先级（>0 时直接使用，≤0 时从默认序列计算）
    protected final ElementalReactionType reactionType;

    // 判断后手+先手（主元素）是否能匹配这个反应
    public boolean canMatch(ElementalsGIM attackerMain, ElementalsGIM defenderMain);

    // 按比例同时消耗，返回 [A消耗, B消耗]
    public float[] calculateConsumption(float qtyA, float qtyB);

    // 子类实现：消耗元素 + 执行效果
    public abstract ReactionResult execute(ReactionContext context);

    // 子类可覆盖：当前状态下是否禁止发生此反应（例：冻结目标禁止蒸发）
    public boolean isBlocked(ReactionContext context);

    // 聚变反应冷却预留接口
    public int getDamageCooldownMs();    // 伤害冷却（同攻击者同反应）
    public int getReactionCooldownMs();  // 公共冷却（同目标同反应）
}
```

#### ReactionContext —— 反应执行上下文

```java
public class ReactionContext {
    public final LivingEntity target;           // 先手元素持有者
    public final ElementalsGIM attackerElement;  // 后手元素
    public final float attackerQuantity;        // 后手附着量（全额参与反应）
    public final AttachmentSource attackerSource;// 后手附着来源（用于判断后手不残留规则）
    public final AttachmentProfile attackerProfile;
    public final ModDamageSpec damageSpec;      // 本次攻击的伤害规格
    public final Entity attackerEntity;         // 攻击者实体
    public final StatusContainer targetContainer;// 目标的状态容器（消耗用）
}
```

#### ReactionResult —— 反应执行结果

```java
public class ReactionResult {
    public static Builder builder(ElementalReactionType type);

    public ElementalReactionType getReactionType();
    public boolean isReacted();                // 是否成功触发
    public float getConsumedAttacker();        // 后手消耗量
    public float getConsumedDefender();        // 先手消耗量
    public boolean isAmplified();              // 是否是增幅反应
    public float getAmplifyMultiplier();       // 增幅倍率
}
```

#### 优先级系统

位置：`core/system/reaction/ReactionPriorityCalculator.java`

**默认优先级序列**（后手元素依次与各先手元素反应的顺序，index 越小越优先）：

```
风(0) 冰(1) 雷(2) 水(3) 冻(4) 火(5) 草(6) 激(7) 岩(8)
```

优先级计算逻辑：
- 如果反应注册时 `basePriority > 0`，直接使用注册值（用于固定优先的特殊反应）
- 如果 `basePriority ≤ 0`，从默认序列中查先手主元素对应的 index

**例外因素**（`ReactionPriorityCalculator.computeFor` 中集中处理，当前框架已预留判断工具方法）：

| 例外 | 说明 |
|------|------|
| 冻结状态下碎冰最高 | 检查容器是否有 FROZEN，优先级改为 -1 |
| 蔓激化/超激化优先 | 检查容器是否有 AGGRAVATE，反应类型为激化时提升优先级 |
| 冻结目标禁止蒸发 | `VaporizeReaction.isBlocked()` 中检查 FROZEN |
| 冰与冻共同消耗 | `consumeDefenderMain()` 按主元素遍历所有实例（不区分 CYRO/FROZEN） |

#### 消耗计算逻辑

反应消耗不是"一方消耗另一方"，而是**双方同时按比例消耗**：

```
消耗比 A:B = ratioA:ratioB
→ rounds = min( qtyA/ratioA, qtyB/ratioB )
→ 实际消耗 A = rounds × ratioA
→ 实际消耗 B = rounds × ratioB
```

例：蒸发，火1份 水2份。目标身上有 0.8 水，后手附着 1.6 火：
```
rounds = min(1.6/1, 0.8/2) = min(1.6, 0.4) = 0.4
消耗火 = 0.4 × 1 = 0.4
消耗水 = 0.4 × 2 = 0.8
后手剩余火 = 1.6 - 0.4 = 1.2
→ NORMAL_ATTACK 来源 → 后手不残留，剩余清 0
```

#### 元素反应注册表

位置：`registry/ModRegistries.java`（声明）+ `registry/register/ElementalReactionRegister.java`（实例注册）

遵循项目标准的三层注册模式：

**ModRegistries.java —— 注册表声明**：
```java
public static final ResourceKey<Registry<ElementalReaction>> ELEMENTAL_REACTION_REGISTRY_KEY =
    ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(
        Minegenshin.MOD_ID, "elemental_reactions"));

public static final Registry<ElementalReaction> ELEMENTAL_REACTION_REGISTRY =
    new RegistryBuilder<>(ELEMENTAL_REACTION_REGISTRY_KEY)
        .sync(true)
        .maxId(64)
        .create();

public static final DeferredRegister<ElementalReaction> ELEMENTAL_REACTIONS =
    DeferredRegister.create(ELEMENTAL_REACTION_REGISTRY, Minegenshin.MOD_ID);
```

同时在 `@SubscribeEvent registerRegistries` 方法里加一行：
```java
event.register(ELEMENTAL_REACTION_REGISTRY);
```

**ElementalReactionRegister.java —— 具体反应注册**：
```java
public class ElementalReactionRegister {
    public static final DeferredRegister<ElementalReaction> ELEMENTAL_REACTIONS =
        ModRegistries.ELEMENTAL_REACTIONS;

    // 蒸发：火:水 = 1:2，火是克制方
    public static final DeferredHolder<ElementalReaction, VaporizeReaction> VAPORIZE =
        ELEMENTAL_REACTIONS.register("vaporize",
            () -> new VaporizeReaction(
                ElementalReactionType.VAPORIZE,
                ElementalsGIM.PYRO, ElementalsGIM.HYDRO,
                1f, 2f, 0));

    // 融化：火:冰 = 1:2，火是克制方
    public static final DeferredHolder<ElementalReaction, MeltReaction> MELT =
        ELEMENTAL_REACTIONS.register("melt",
            () -> new MeltReaction(
                ElementalReactionType.MELT,
                ElementalsGIM.PYRO, ElementalsGIM.CYRO,
                1f, 2f, 0));

    // 冻结：水:冰 = 1:1
    public static final DeferredHolder<ElementalReaction, FreezeReaction> FREEZE =
        ELEMENTAL_REACTIONS.register("freeze",
            () -> new FreezeReaction(
                ElementalReactionType.FROZEN,
                ElementalsGIM.HYDRO, ElementalsGIM.CYRO,
                1f, 1f, 0));

    public static void register(IEventBus eventBus) {
        ELEMENTAL_REACTIONS.register(eventBus);
    }
}
```

**Minegenshin.java 构造函数里调用**：
```java
ElementalReactionRegister.register(modEventBus);
```

#### ElementalReactionManager —— 反应管理器

位置：`core/system/reaction/ElementalReactionManager.java`

附着后触发反应的主入口：

```java
ReactionResult result = ElementalReactionManager.tryReactAfterAttach(context);
```

**核心流程**：

```
tryReactAfterAttach(context)
    │
    ├─ 1. 收集先手元素实例
    │     └─ 遍历 targetContainer.getAll()
    │     └─ 过滤：未 finished、是 ElementalAttachmentInstance、
    │            主元素 ≠ 后手主元素、≠ FYSIKOS
    │     └─ defenders.isEmpty() → 直接返回空结果
    │
    ├─ 2. 收集候选反应
    │     └─ 遍历注册表中所有 ElementalReaction
    │     └─ 对每个先手 defender：
    │           ├─ reaction.canMatch(后手主元素, 先手主元素)
    │           ├─ !reaction.isBlocked(context)
    │           └─ 计算 priority
    │                 ├─ reaction.basePriority > 0 → 直接用
    │                 └─ 否则 ReactionPriorityCalculator.computeFor(先手主元素, reaction)
    │
    ├─ 3. 排序：按 priority 从小到大
    │
    ├─ 4. 依次执行反应
    │     └─ remainingAttackerQty 递减
    │     └─ firstAmplified 记录第一个增幅反应
    │
    └─ 5. 后手残留处理
          └─ NORMAL_ATTACK → consume(target, 后手元素, MAX_VALUE) 清 0
          └─ 其他来源 → 保留（附着逻辑已处理）
```

#### 反应在伤害管线中的位置

附着 → 反应 → 增幅修正是 `HurtEntityHelper.processPipeline` 中的**关键时序**：

```
calculateFinalModDamage → processPipeline（衰减→附着→反应→乘区）
    │
    ├─ [Phase 1: onAttacked 效果处理]
    ├─ [Phase 2: calculateCharacterDamage 基础伤害区]
    ├─ [衰减] manager.processHit() → damageCoef
    │
    ├─ 1. ★ 附着（全额 × elementCoefficient）★
    │     └─ ElementalAttachmentHelper.attach(target, element, NORMAL_ATTACK, profile)
    │
    ├─ 2. ★ 触发反应 ★
    │     └─ canAttach && container != null
    │     └─ ElementalReactionManager.tryReactAfterAttach(context)
    │
    ├─ 3. ★ 增幅修正 ★
    │     └─ result.isAmplified() → 伤害乘区加入 amplifyMult × emReactionBonus
    │
    ├─ 4. calculateFinalDamage —— 完整乘区
    │     ├─ critZone × dmgBonusZone × defenseZone × resistanceZone
    │     ├─ 增幅反应额外 × amplifyMult × emReactionBonus
    │     └─ finalDamage ×= decayResult.getDamageCoefficient()
    │
    └─ return finalDamage → LivingEntityHurtMixin 扣血 + broadcastDamageEvent
```

#### 内置反应详解

##### 蒸发（VaporizeReaction）

消耗比：火:水 = 1:2（火克制水）

| 场景 | 倍率 | 说明 |
|------|------|------|
| 先手水 + 后手火 | **2.0** | 火是克制方 → 火蒸发 |
| 先手火 + 后手水 | **1.5** | 水是被克制方 → 水蒸发 |

特殊规则：
- 冻结状态的目标**禁止蒸发**（`isBlocked` 中检查容器是否有 FROZEN）
- 非攻击行为触发（环境伤害等）不增幅（需 `damageSpec` 存在且有伤害）

##### 融化（MeltReaction）

消耗比：火:冰 = 1:2（火克制冰）

| 场景 | 倍率 | 说明 |
|------|------|------|
| 先手冰/冻 + 后手火 | **2.0** | 火是克制方 → 火融化 |
| 先手火 + 后手冰/冻 | **1.5** | 冰是被克制方 → 冰融化 |

特殊规则：
- **冰和冻同时消耗**：`consumeDefenderMain()` 按 CYRO 主元素遍历容器，自动覆盖 CYRO 和 FROZEN 两类实例
- 火元素量足够多时可以一次性消耗完冻和冰全部元素

##### 冻结（FreezeReaction）

消耗比：水:冰 = 1:1

**非增幅、非聚变**，不修改伤害倍率，属于状态异常类反应。

效果：
- 按 1:1 同时消耗水和冰元素
- **生成总量 × 2 的 FROZEN（冻）元素**附加到目标（`AttachmentSource.SPECIAL`）

特殊规则：
- 后手为 FROZEN 元素时禁止发生冻结（冻不能再冻）
- 冻结反应后，目标可能残留额外的水或冰（冻结藏水/藏冰逻辑，TODO 待实现）
- 冻结效果（减速、冻结实体）暂不实现，TODO 待补充

###### A/B 槽位机制

所有继承 `ElementalReaction` 的反应在注册时都声明了 `elementA` / `elementB` 两个元素槽位和对应的消耗比 `ratioA` / `ratioB`。但在实际运行时，先后手是随机的——后手可能是 A 也可能是 B。`FreezeReaction.execute` 的第一行就用 `attackerIsA` 这个布尔变量把"运行时先后手"映射回"固定的 A/B 槽位"：

```
注册时: elementA = HYDRO, elementB = CYRO, ratioA = 1, ratioB = 1

运行时:
  后手是水 → attackerMain == elementA → attackerIsA = true  → 后手 = A槽, 先手 = B槽(冰)
  后手是冰 → attackerMain == elementA → attackerIsA = false → 后手 = B槽, 先手 = A槽(水)
```

这个布尔值是后续**所有分支判断的根基**——消耗哪边、传什么参数给 `calculateConsumption`、生成什么返回值，全都取决于它。

###### 四步执行流程

```
① 定向 ─→ attackerIsA 把先后手映射回 A/B 槽位
          └─ defenderTarget = attackerIsA ? elementB : elementA  (三元运算)
          └─ totalDefenderQty = sumMainElementQuantity(...)  (遍历容器求和，支持类元素归并)

② 算量 ─→ calculateConsumption(qtyA, qtyB, defenderIsA)
          └─ rounds = min(qtyA/ratioA, qtyB/ratioB)
          └─ consumedA = rounds × ratioA,  consumedB = rounds × ratioB
          └─ 调用顺序必须保证：第一个参数 = A槽的数量，第二个 = B槽的数量
          └─ attackerIsA=true  → calculateConsumption(attackerQty, totalDefenderQty, ...)
          └─ attackerIsA=false → calculateConsumption(totalDefenderQty, attackerQty, ...)

③ 扣减 ─→ consumeAttacker 消耗后手（直接 consume 按元素类型）
          └─ consumeDefenderMain 消耗先手（按主元素遍历所有同类实例逐个扣）
          └─ 两者分开的原因：先手可能分散在多个实例里，且需要类元素归并

④ 生成 ─→ frozenQty = (consumedA + consumedB) × FROZEN_MULTIPLIER(2.0)
          └─ attach(target, FROZEN, SPECIAL, new AttachmentProfile(frozenQty, ...))
```

###### 变量命名详解

| 变量 | 含义 | 为什么叫这个 |
|------|------|--------------|
| `attackerMain` | 后手元素的主元素（经过 `getMainElement()` 归并） | `attacker` = 后手（攻击方附着的那方）；`Main` = 主元素归并后的结果 |
| `attackerIsA` | 后手的主元素是否等于注册时 A 槽的元素 | 把"随机先后手"映射到"固定 A/B 槽位"，后续所有 `consume` 调用的分支条件 |
| `defenderTarget` | 要从先手身上找的元素类型 | `defender` = 先手（目标身上已有的那方）；`Target` = 我们要"找出来反应"的那个元素 |
| `totalDefenderQty` | 目标身上所有主元素匹配的附着量之和 | `total` = 多实例求和；`Qty` = Quantity（元素量，单位 U） |
| `attackerQty` | 后手元素本次附着的总量 | 来自 `ctx.attackerQuantity`，全额参与反应不衰减 |
| `consumedA` / `consumedB` | A/B 槽各自要消耗的数值 | **只算数值，还没扣减**，由 `calculateConsumption` 返回 |
| `totalConsumed` | A+B 消耗总量 | 用于生成 FROZEN 的量：`× 2` |
| `consumedAttacker` | 后手侧实际消耗了多少 | 返回给 `ReactionResult` 用；又一次 `attackerIsA` 分支决定是 `consumedA` 还是 `consumedB` |
| `frozenQty` | 最终生成的 FROZEN 元素量 | `totalConsumed × FROZEN_MULTIPLIER(2.0f)` |

###### 三元运算效果说明

代码中出现多次 `attackerIsA ? X : Y` 三元表达式，每次的效果不同：

| 位置 | 三元表达式 | attackerIsA=true（后手=水=A槽） | attackerIsA=false（后手=冰=B槽） |
|------|-----------|-------------------------------|--------------------------------|
| 定向 | `elementB : elementA` | `defenderTarget = CYRO`（冰） | `defenderTarget = HYDRO`（水） |
| 算量调用 | `calculateConsumption(A槽数量, B槽数量, ...)` | `(attackerQty=水, totalDefenderQty=冰, ...)` | `(totalDefenderQty=水, attackerQty=冰, ...)` |
| 后手消耗值 | `consumedA : consumedB` | `consumedAttacker = consumedA` | `consumedAttacker = consumedB` |
| 先手消耗值（返回结果用） | `consumedB : consumedA` | `consumedDefender = consumedB` | `consumedDefender = consumedA` |

###### 元素量消耗具体示例

**场景 1：先手水 0.8U，后手冰 1.6U**

```
attackerIsA = (CYRO == HYDRO) → false → 后手是 B 槽
defenderTarget = elementA = HYDRO
totalDefenderQty = 0.8（目标身上的水）
attackerQty = 1.6（本次附着的冰）

调用 calculateConsumption(totalDefenderQty=0.8, attackerQty=1.6, defenderIsA=true)
  ratioA = 1.0, ratioB = 1.0
  rounds = min(0.8/1.0, 1.6/1.0) = min(0.8, 1.6) = 0.8
  consumedA = 0.8 × 1.0 = 0.8   ← A槽(水)消耗
  consumedB = 0.8 × 1.0 = 0.8   ← B槽(冰)消耗

totalConsumed = 0.8 + 0.8 = 1.6
consumedAttacker = attackerIsA ? consumedA : consumedB = consumedB = 0.8

扣减:
  consumeAttacker(target, elementB=CYRO, 0.8)    → 后手冰扣 0.8，剩余 0.8
  consumeDefenderMain(container, elementA=HYDRO, 0.8) → 先手水全部扣完

生成: frozenQty = 1.6 × 2.0 = 3.2 FROZEN

后手残留: 本次附着冰 1.6U，反应消耗 0.8U → 剩余 0.8U
  由于是 NORMAL_ATTACK 来源 → 后手不残留（ElementalReactionManager 处理）
```

**场景 2：先手 1.2 冰 + 0.6 冻（主元素都是 CYRO），后手水 1.0U**

```
attackerIsA = (HYDRO == HYDRO) → true → 后手是 A 槽
defenderTarget = elementB = CYRO
totalDefenderQty = sumMainElementQuantity(CYRO) = 1.2(冰) + 0.6(冻) = 1.8
attackerQty = 1.0

调用 calculateConsumption(attackerQty=1.0, totalDefenderQty=1.8, defenderIsA=false)
  ratioA = 1.0, ratioB = 1.0
  rounds = min(1.0/1.0, 1.8/1.0) = min(1.0, 1.8) = 1.0
  consumedA = 1.0 × 1.0 = 1.0   ← A槽(水)消耗
  consumedB = 1.0 × 1.0 = 1.0   ← B槽(冰+冻)消耗

totalConsumed = 2.0
consumedAttacker = consumedA = 1.0

扣减:
  consumeAttacker(target, elementA=HYDRO, 1.0)      → 后手水全部扣完
  consumeDefenderMain(container, elementB=CYRO, 1.0)
    顺序扣：先扣最早附着的 CYRO(1.2U) → 扣 1.0U 剩 0.2U
    → 再扣 FROZEN(0.6U) 但 remaining 已经 0，跳过

生成: frozenQty = 2.0 × 2.0 = 4.0 FROZEN

先手残留: 原 CYRO 剩 0.2U，FROZEN 完好 0.6U
```

###### 两个 consume 方法的区别

| 方法 | 操作对象 | 遍历方式 | 为什么这样设计 |
|------|----------|----------|----------------|
| `consumeAttacker` | 后手元素 | 直接 `ElementalAttachmentHelper.consume(target, element, amount)` | 后手只来自本次附着，是单一实例，直接按元素类型扣即可 |
| `consumeDefenderMain` | 先手元素 | 遍历容器所有实例，`getMainElement()` 匹配则 `ea.consume(remaining)` | 先手可能分散在多个实例（不同 source），且需要类元素归并（CYRO + FROZEN 一起扣） |

#### 添加新反应 —— 完整指南

假设要添加"超载"（聚变反应，火+雷）：

**Step 1: 创建反应类**

```java
// core/system/reaction/builtin/OverloadReaction.java
public class OverloadReaction extends ElementalReaction {

    public OverloadReaction(ElementalReactionType type,
                            ElementalsGIM elementA, ElementalsGIM elementB,
                            float ratioA, float ratioB, int basePriority) {
        super(type, elementA, elementB, ratioA, ratioB, basePriority);
    }

    @Override
    public ReactionResult execute(ReactionContext ctx) {
        // 匹配后手+先手、计算消耗、扣元素...
        // 聚变反应的效果：额外造成一次伤害（需接入 ModDamageSource）
        // TODO: 聚变冷却系统（伤害冷却 + 公共冷却）
    }

    @Override
    public int getDamageCooldownMs() { return 500; }   // 同攻击者同反应 0.5s 最多1次
    @Override
    public int getReactionCooldownMs() { return 100; }  // 同目标同反应公共冷却 0.1s
}
```

**Step 2: 注册**

```java
// ElementalReactionRegister.java
public static final DeferredHolder<ElementalReaction, OverloadReaction> OVERLOAD =
    ELEMENTAL_REACTIONS.register("overload",
        () -> new OverloadReaction(
            ElementalReactionType.OVERLOAD,
            ElementalsGIM.PYRO, ElementalsGIM.ELECTRO,
            1f, 1f, 0));
```

**Step 3: （可选）覆盖 `isBlocked` 添加禁止条件**

```java
@Override
public boolean isBlocked(ReactionContext context) {
    // 示例：冻结状态禁止超载（如果需要）
    return ReactionPriorityCalculator.hasFrozen(context.targetContainer);
}
```

#### 聚变反应冷却系统（TODO 预留）

聚变反应（超载、超导、扩散、感电、碎冰等）有两种冷却机制，当前框架已预留接口，具体实现待后续补充：

| 冷却类型 | 说明 | 共用情况 |
|----------|------|----------|
| **伤害冷却** | 同攻击者在 0.5s 内对同防守者用同反应最多造成伤害次数 | 同攻击者+同防守者+同反应 共用；不同攻击者、不同反应不共用 |
| **公共冷却** | 同防守者触发同反应的最小间隔时间 | 按反应类型：碎冰 0.2s，超载/超导/扩散 0.1s；不同种扩散不共用 |

伤害冷却限额：

| 反应类别 | 0.5s 内最多伤害次数 |
|----------|---------------------|
| 超导 / 碎冰 / 四种扩散 | 2 次 |
| 超载 / 感电 | 1 次 |

#### 完整攻击→附着→反应→增幅 逻辑链

```
PlayerAttackInterceptor.onPlayerAttack()
    │
    ├─ buildModDamageSource()
    └─ target.hurtServer(serverLevel, modSource, 0f)
          │
          ▼
LivingEntityHurtMixin (hurtServer HEAD)
    ├─ calculateFinalModDamage(modSource, attackerChar, target)
    │     │
    │     ├─ Phase 1: onAttacked 效果处理
    │     ├─ Phase 2: calculateCharacterDamage()
    │     │     └─ base = (ATK × atkMult + flatBonus + HP×hpMult + DEF×defMult + EM×emMult) × (1 + skillMultBonus)
    │     │
    │     └─ Phase 3: processPipeline()
    │           │
    │           ├─ DecayCounterManager.processHit()
    │           │     └─ elementCoef / damageCoef / poiseCoef
    │           │
    │           ├─ ★ 附着 ★
    │           │     └─ ElementalAttachmentHelper.attach(target, element,
    │           │           NORMAL_ATTACK, chooseProfile(elementAmount))
    │           │
    │           ├─ ★ 反应触发 ★
    │           │     └─ ElementalReactionManager.tryReactAfterAttach(ctx)
    │           │           │
    │           │           ├─ collectDefenders()   ← 收集先手
    │           │           ├─ 遍历注册表匹配反应    ← canMatch + isBlocked
    │           │           ├─ priority 排序         ← ReactionPriorityCalculator
    │           │           ├─ 依次 reaction.execute() ← 消耗元素 + 执行效果
    │           │           │     └─ ElementalAttachmentHelper.consume()
    │           │           ├─ 记录 firstAmplified
    │           │           └─ applyAttackerResidual() ← 后手不残留
    │           │
    │           └─ calculateFinalDamage() —— 完整乘区
    │                 ├─ critZone × dmgBonusZone × defenseZone × resistanceZone
    │                 ├─ MELT/VAPORIZE → 额外 × amplifyMult × emReactionBonus
    │                 └─ finalDamage ×= decayResult.getDamageCoefficient()
    │
    ├─ 扣血
    │     ├─ Player + 原神模式开 → character.hurt() → incapacitate()
    │     └─ 其他 → target.setHealth(max(0, health - finalDamage))
    ├─ level.broadcastDamageEvent(target, source)
    └─ cir.setReturnValue(true)
```

#### 关键文件索引

| 类 | 路径 |
|----|------|
| ElementalReaction（基类） | `core/system/reaction/ElementalReaction.java` |
| ElementalReactionManager（管理器） | `core/system/reaction/ElementalReactionManager.java` |
| ReactionContext（上下文） | `core/system/reaction/ReactionContext.java` |
| ReactionResult（结果） | `core/system/reaction/ReactionResult.java` |
| ReactionPriorityCalculator（优先级） | `core/system/reaction/ReactionPriorityCalculator.java` |
| VaporizeReaction（蒸发） | `core/system/reaction/builtin/VaporizeReaction.java` |
| MeltReaction（融化） | `core/system/reaction/builtin/MeltReaction.java` |
| FreezeReaction（冻结） | `core/system/reaction/builtin/FreezeReaction.java` |
| 注册表声明 | `registry/ModRegistries.java` |
| 反应注册入口 | `registry/register/ElementalReactionRegister.java` |
| 主入口调用 | `Minegenshin.java` 构造函数 |

### 4.12 怪物等级与防御系统

#### 概述

怪物等级与防御系统通过 Mixin 为所有 `Monster` 子类（排除本 Mod 自定义的 `TeyvatMonster`）注入等级和防御力属性。等级在怪物生成时一次性确定（不可修改），防御力 = 等级 × 500 + 500。

**设计目标**：
- 为 **本 Mod 以外的怪物**（原版、其他 Mod）提供通用的等级+防御逻辑
- 本 Mod 内部的 `TeyvatMonster` 子类自行实现等级体系，不受本系统干预
- 等级计算与玩家冒险等阶挂钩，支持 4 种计算模式 + 3 种生成模式

#### 世界等级映射

玩家**冒险等阶（AR）** → **世界等级（WL）** → **怪物等级范围**：

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

> **加权随机**：同一世界等级范围内，AR 越高则出现较高等级怪物的概率越大。例如 WL0 中 AR20 遇到 10 级怪的概率远高于 AR1。

#### 冒险等阶计算方式（4种）

生成时根据附近玩家的冒险等阶计算"目标世界等级"：

| 模式 | 说明 |
|------|------|
| `NEAREST` | 取生成点距离最近玩家的 AR → 世界等级 |
| `HIGHEST` | 搜索半径内取最高 AR 的玩家 → 世界等级 |
| `LOWEST` | 搜索半径内取最低 AR 的玩家 → 世界等级 |
| `COMPREHENSIVE` | 排除最高 AR，取第二高 AR，低于第二高 25 级及以上的排除，剩余取平均 AR 再转世界等级 |

#### 生成模式（3种）

| 模式 | 说明 | 典型用途 |
|------|------|---------|
| `NATURAL` | 自然生成：按玩家 AR 加权随机一个等级 | 野外自然刷新 |
| `FIXED` | 固定等级：直接使用配置中指定的等级 | 副本/Boss 固定等级 |
| `BIAS` | 偏差值：先算出世界等级，再 + 偏移值（可正可负） | 不想完全固定等级但需限制范围 |

#### 配置文件

路径：`./config/minegenshin/monster_level.toml`

```toml
[monsterSpawnLogic.spawnLevelCalculation]
calculationMode = "NEAREST"     # NEAREST / HIGHEST / LOWEST / COMPREHENSIVE
searchRadius = 64.0             # 搜索半径（方块）

[monsterSpawnLogic.spawnMode]
spawnMode = "NATURAL"           # NATURAL / FIXED / BIAS
fixedLevel = 1                  # FIXED 模式使用
worldLevelBias = 0              # BIAS 模式使用，世界等级偏移
```

#### 核心文件

| 文件 | 职责 |
|------|------|
| `mixin/interfaces/IMonsterLevel.java` | Mixin 注入接口：`get/setMonsterLevel`、`getDefense` |
| `mixin/mixins/MonsterMixin.java` | 注入 `Monster.class`，实现接口，内置 `levelLocked` 防重复写入；另有 `MagmaCubeResistanceMixin` / `BlazeResistanceMixin` 加火抗 |
| `mixin/MonsterLevelConfig.java` | NeoForge 配置，枚举定义（CalculationMode / SpawnMode） |
| `core/monster/MonsterLevelCalculator.java` | 等级计算核心：世界等级映射、4 种计算模式、加权随机 |
| `core/monster/MonsterLevelSpawnHandler.java` | 监听 `FinalizeSpawnEvent`，给非 TeyvatMonster 设置等级 |

#### Mixin 注入接口

```java
public interface IMonsterLevel {
    int genshin$getMonsterLevel();   // 获取等级
    void genshin$setMonsterLevel(int level);  // 设置等级（仅第一次生效）
    int genshin$getDefense();        // 等级 * 500 + 500
}
```

#### 等级生成流程

```
FinalizeSpawnEvent 触发
    │
    ├─ TeyvatMonster？→ 跳过（子类自行实现）
    ├─ 已经有等级？→ 跳过（防重复）
    │
    └─ MonsterLevelCalculator.getMonsterLevel(serverLevel, spawnPos, seed)
          │
          ├─ SpawnMode == FIXED
          │     └─ 返回 fixedLevel
          │
          ├─ SpawnMode == NATURAL 或 BIAS
          │     ├─ 计算目标世界等级（4种 CalculationMode）
          │     ├─ NATURAL → 直接使用该世界等级
          │     ├─ BIAS → worldLevel + bias（clamp 到 0~9）
          │     ├─ 查世界等级对应的怪物等级范围
          │     └─ 加权随机出一个具体等级
          │
          └─ monster.genshin$setMonsterLevel(level)  ← levelLocked=true，永久固定
```

#### 排除 TeyvatMonster 的原因

`TeyvatMonster` 是本 Mod 自定义怪物的基类，具有独立的等级体系。`MonsterLevelSpawnHandler` 在第 23 行通过 `instanceof TeyvatMonster` 跳过，确保 Mixin 注入的 `IMonsterLevel` 逻辑只作用于外部怪物。

---

## 5. 扩展开发指南

### 5.1 添加新角色

假设要添加角色 "ExampleChar"：

**Step 1: 创建角色类**

```java
// src/main/java/com/linweiyun/genshin/core/character/sword/ExampleChar.java
public class ExampleChar extends PGCharacter {
    public ExampleChar() {
        super(
            135099,                              // UUID (保持唯一)
            0,                                   // 武器类型 (0=单手剑, 3=法器, 5=长柄)
            Component.translatable("character.name.example"),
            ElementalsGIM.HYDRO,                // 水元素
            CharacterAscendAttribute.HP,        // 突破属性是HP
            12 * 20,                            // E技能CD 12秒
            20 * 20,                            // Q技能CD 20秒
            80,                                 // 能量需求
            1.2f,                               // 默认技能倍率
            "example_char",                     // 注册名
            Map.of(
                ModAttributes.MAX_HP.getId(), Config.EXAMPLE_HP,
                ModAttributes.ATK.getId(), Config.EXAMPLE_ATK,
                ModAttributes.DEF.getId(), Config.EXAMPLE_DEF
            )
        );
    }

    @Override
    protected void triggerElementalSkill(Player player, int skillTime) {
        // TODO: 实现 E 技能逻辑
    }

    @Override
    protected void triggerElementalBurst(Player player) {
        // TODO: 实现 Q 技能逻辑
    }
}
```

**Step 2: 添加配置数据**

在 `Config.java` 中添加属性成长数据（95 个等级值，含 5 次突破跃升）：

```java
public static final ModConfigSpec.ConfigValue<List<? extends Integer>> EXAMPLE_HP;
public static final ModConfigSpec.ConfigValue<List<? extends Integer>> EXAMPLE_ATK;
public static final ModConfigSpec.ConfigValue<List<? extends Integer>> EXAMPLE_DEF;

static {
    CHARACTER_ATTRIBUTE_BUILDER.push("example_char");
    EXAMPLE_HP = CHARACTER_ATTRIBUTE_BUILDER
        .translation("config.attribute.example.hp")
        .defineList(List.of("hp"), 
            () -> List.of(1000, 1100, 1200, /* ... 95 个等级值 */),
            null, obj -> obj instanceof Integer,
            ModConfigSpec.Range.of(95, 95));
    // ... ATK、DEF 同理
}
```

**Step 3: 注册角色**

```java
// CharacterRegister.java
public static final DeferredHolder<PGCharacter, ExampleChar> EXAMPLE_CHAR = 
    CHARACTERS.register("example_char", ExampleChar::new);
```

**Step 4: 添加翻译文本**

```json
// resources/assets/minegenshin/lang/zh_cn.json
{
    "character.name.example": "示例角色",
    "character.name.example.skill": "示例战技",
    "character.name.example.burst": "示例爆发"
}
```

### 5.2 添加新角色效果

假设要添加 "ReflectEffect" —— 受击时反伤：

**Step 1: 创建效果类**

```java
// content/effect/character/example/ReflectEffect.java
public class ReflectEffect implements ICharacterEffect {

    public static final String REFLECT_PERCENT_KEY = "reflect_percent";

    @Override
    public void onAttacked(Player holder, PGCharacter character,
                           LivingEntity target, CharacterEffectInstance instance,
                           ModDamageSource damageSource) {
        // 示例：增加固定伤害加成
        float reflectPercent = instance.getFloatData(REFLECT_PERCENT_KEY);
        ModDamageSpec oldSpec = damageSource.getSpec();
        ModDamageSpec newSpec = oldSpec.withFlatDamageBonus(
            oldSpec.getFlatDamageBonus() + character.getData().getCurrentHP() * reflectPercent
        );
        damageSource.setSpec(newSpec);
    }

    @Override
    public void onEffectAdded(Player holder, PGCharacter character, 
                              CharacterEffectInstance instance) {
        // 可在这里添加临时属性修饰器
    }

    @Override
    public void onEffectRemoved(Player holder, PGCharacter character, 
                                CharacterEffectInstance instance) {
        // 清理修饰器等
    }
}
```

**Step 2: 注册效果**

```java
// CharacterEffectRegister.java
public static final DeferredHolder<ICharacterEffect, ReflectEffect> 
    REFLECT_EFFECT = CHARACTER_EFFECTS.register("reflect", ReflectEffect::new);
```

**Step 3: 在技能中使用**

```java
@Override
protected void triggerElementalSkill(Player player, int skillTime) {
    PGCharacter currentChar = CharacterHelper.getCurrentCharacter(player);
    if (currentChar != null) {
        CharacterEffectInstance effect = new CharacterEffectInstance(
            CharacterEffectRegister.REFLECT_EFFECT.get(),
            200,   // 10秒
            1
        );
        effect.setFloatData(ReflectEffect.REFLECT_PERCENT_KEY, 0.2f);
        CharacterEffectHelper.addEffect(player, currentChar, effect);
    }
}
```

### 5.3 添加新实体

**Step 1: 创建实体类**

```java
// content/entities/teyvat/monster/slime/ExampleMonster.java
public class ExampleMonster extends TeyvatLivingEntity {
    public ExampleMonster(EntityType<?> type, Level level) {
        super(type, level);
    }
    // 攻击逻辑、掉落物、AI行为...
}
```

**Step 2: 注册实体**

```java
// EntityRegister.java
public static final Supplier<EntityType<ExampleMonster>> EXAMPLE_MONSTER =
    ENTITIES.register("example_monster",
        () -> EntityType.Builder.of(ExampleMonster::new, MobCategory.MONSTER)
            .sized(0.6f, 1.8f)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Minegenshin.id("example_monster")))
    );

private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
    event.put(EXAMPLE_MONSTER.get(), ExampleMonster.createAttributes().build());
}
```

**Step 3: 创建资源文件**

- `assets/minegenshin/geo/example_monster.geo.json` —— 模型（或 GeckoLib）
- `assets/minegenshin/textures/entity/example_monster.png` —— 贴图
- `assets/minegenshin/lang/zh_cn.json` —— 翻译

### 5.4 添加新伤害类型

```java
// DamageTypeRegister.java
public static final DeferredHolder<DamageType, DamageType> CUSTOM_DAMAGE =
    DAMAGE_TYPES.register("custom_damage",
        () -> new DamageType("custom_damage", 0));

// 使用
ModDamageSource source = new ModDamageSource(
    DamageTypeRegister.CUSTOM_DAMAGE,  // DamageType Holder
    attacker,                            // 直接伤害源
    spec                                 // DamageSpec
);
```

---

## 6. Mixin 系统

### 总览

配置文件：`src/main/resources/minegenshin.mixins.json`

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.linweiyun.genshin.mixin.mixins",
  "compatibilityLevel": "JAVA_21",
  "plugin": "com.linweiyun.genshin.mixin.MixinConfig",
  "mixins": [
    "BlazeResistanceMixin",
    "DamageContainerMixin",
    "LivingEntityDecayMixin",
    "LivingEntityHurtMixin",
    "MagmaCubeResistanceMixin",
    "MonsterMixin",
    "PlayerAttackInterceptor",
    "PlayerHurtInterceptor"
  ]
}
```

使用 `MixinConfig` 插件在运行时跳过对 `TeyvatLivingEntity` 子类的 `MonsterMixin` 和 `LivingEntityHurtMixin` 应用（通过 ASM 遍历接口链判断），避免与本 Mod 自定义实现冲突。

### 完整 Mixin 清单

| # | Mixin 类 | 注入目标 | 位置 | 功能 |
|---|----------|----------|------|------|
| 1 | `PlayerAttackInterceptor` | `Player.attack()` | HEAD, cancellable | 原神模式开时拦截攻击，构建 `ModDamageSource` 调用 `target.hurtServer()`，不走原版攻击 |
| 2 | `LivingEntityHurtMixin` | `LivingEntity.hurtServer()` | HEAD, cancellable | 非 TeyvatLivingEntity 实体的 `ModDamageSource` 伤害入口：调 `HurtEntityHelper.calculateFinalModDamage`，扣血，广播，cancel |
| 3 | `PlayerHurtInterceptor` | `Player.actuallyHurt()` | HEAD, cancellable | 原神模式开 + 非 ModDamageSource 时，原版伤害改为扣 PGCharacter 血，cancel |
| 4 | `LivingEntityDecayMixin` | `LivingEntity` | 接口实现 | 注入 `DecayCounterManager` 字段，实现 `IDecayCounterHolder`，延迟初始化 |
| 5 | `MonsterMixin` | `Monster` | 接口实现 | 注入等级 + 防御 + 默认元素抗性，实现 `IMonsterLevel` |
| 6 | `MagmaCubeResistanceMixin` | `MagmaCube` | 接口实现 | 继承 MonsterMixin 等级机制，额外加 80% 火抗 |
| 7 | `BlazeResistanceMixin` | `Blaze` | 接口实现 | 继承 MonsterMixin 等级机制，额外加 80% 火抗 |
| 8 | `DamageContainerMixin` | NeoForge `DamageContainer` | HEAD, cancellable | 注入 `modifiedSource` 字段，拦截 `getSource()` 支持替换 DamageSource |

### 伤害管线三件套（v0.2.0 核心）

v0.2.0 将伤害入口从 `HurtEntityHelper.hurtEntityForPlayer` 迁移到实体的 `hurtServer` 方法，由三个 Mixin 协同完成：

#### PlayerAttackInterceptor —— 攻击拦截

注入 `Player.attack` HEAD，构建 `ModDamageSource` 后直接调 `target.hurtServer(serverLevel, modSource, 0f)`（传 0f，MOD 管线自己算伤害）。

```
玩家左键攻击
    │
    ▼
Player.attack (原版) → PlayerAttackInterceptor
    ├─ 非 LivingEntity 目标 → 放行
    ├─ 非原神模式 → 放行
    ├─ ci.cancel()
    └─ target.hurtServer(serverLevel, ModDamageSource, 0f)
```

#### LivingEntityHurtMixin —— 伤害拦截（目标侧）

注入 `LivingEntity.hurtServer` HEAD。收到 `ModDamageSource` 后接管整条伤害管线：调 `HurtEntityHelper.calculateFinalModDamage` → 扣血 → `level.broadcastDamageEvent` 广播 → cancel。非 ModDamageSource 放行原版。

```
LivingEntity.hurtServer (原版) → LivingEntityHurtMixin
    │
    ├─ !(source instanceof ModDamageSource) → return（放行）
    │
    ├─ HurtEntityHelper.calculateFinalModDamage(modSource, attackerChar, target)
    │     └─ [完整管线见 4.5 节]
    │
    ├─ 扣血
    │     ├─ Player + 原神模式开 → character.hurt() → incapacitate()
    │     └─ 其他 → target.setHealth(max(0, health - finalDamage))
    │
    ├─ level.broadcastDamageEvent(target, source)  ← 客户端同步
    └─ cir.setReturnValue(true)  ← 阻断原版 hurtServer 全部后续逻辑
```

**为什么一定要在 HEAD 处 cancel？** 原版 `hurtServer` 内部有 `invulnerableTime > 10 && damage <= lastHurt` 的冷却检查，不 cancel 会导致连续攻击被吞掉。同时 `actuallyHurt` 会被再次调起原版伤害计算。

#### PlayerHurtInterceptor —— 反向伤害拦截（玩家受击侧）

注入 `Player.actuallyHurt` HEAD。非 ModDamageSource（怪物打玩家）+ 原神模式开时，扣 PGCharacter 血而不是 Player 血，cancel 原版。

```
Player.actuallyHurt (原版) → PlayerHurtInterceptor
    │
    ├─ 非原神模式 → return（放行）
    ├─ source instanceof ModDamageSource → return（放 LivingEntityHurtMixin 处理）
    │
    ├─ current.hurt(damage)
    ├─ dead → current.incapacitate(attachment)
    └─ ci.cancel()
```

### LivingEntityDecayMixin —— 注入衰减计数器支持

注入目标：`LivingEntity`，实现 `IDecayCounterHolder`。

```java
@Mixin(LivingEntity.class)
public abstract class LivingEntityDecayMixin implements IDecayCounterHolder {

    @Unique
    private DecayCounterManager genshin$decayCounterManager;

    @Override
    public DecayCounterManager getDecayCounterManager() {
        if (genshin$decayCounterManager == null) {
            genshin$decayCounterManager = new DecayCounterManager(
                (LivingEntity)(Object) this);
            DecayCounterWorker.getInstance().registerManager(genshin$decayCounterManager);
        }
        return genshin$decayCounterManager;
    }
}
```

### MonsterMixin 系列 —— 怪物等级与抗性

三个 Mixin 都实现 `IMonsterLevel` 接口：

| Mixin | 注入目标 | 特殊抗性 |
|-------|----------|----------|
| `MonsterMixin` | `Monster` | 默认全元素 10% 抗性 |
| `MagmaCubeResistanceMixin` | `MagmaCube` | 火抗 80% |
| `BlazeResistanceMixin` | `Blaze` | 火抗 80% |

等级通过 `MonsterLevelSpawnHandler` 在 `FinalizeSpawnEvent` 中一次性写入（`levelLocked=true` 永久锁定）。防御公式：`level * 500 + 500`。

**跳过 TeyvatLivingEntity**：`MixinConfig.shouldApplyMixin` 通过 ASM 遍历目标类的接口链，若实现了 `TeyvatLivingEntity` 则不对其应用 `MonsterMixin` 和 `LivingEntityHurtMixin`（Teyvat 实体自行重写 `hurtServer`）。

### DamageContainerMixin —— 伤害源替换

注入 NeoForge 的 `DamageContainer`（原版伤害管线中包裹 DamageSource 的容器类）：

```java
@Mixin(DamageContainer.class)
public class DamageContainerMixin implements IDamageSourceModifier {

    @Final @Shadow private DamageSource source;
    @Unique private DamageSource modifiedSource;

    @Override
    public void setModifiedSource(DamageSource newSource) {
        this.modifiedSource = newSource;
    }

    @Inject(method = "getSource", at = @At("HEAD"), cancellable = true)
    public void onGetSource(CallbackInfoReturnable<DamageSource> cir) {
        cir.setReturnValue(modifiedSource != null ? modifiedSource : source);
    }
}
```

通过 `IDamageSourceModifier` 接口暴露 `setModifiedSource`，允许在伤害管线中用新的 DamageSource 替换原版的。

### MixinConfig —— 运行时过滤

位置：`mixin/MixinConfig.java`

```java
@Override
public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
    boolean skipTeyvat = mixinClassName.endsWith("MonsterMixin")
            || mixinClassName.endsWith("LivingEntityHurtMixin");
    if (!skipTeyvat) return true;
    return !implementsTeyvat(targetClassName.replace('.', '/'));
}
```

只对 `MonsterMixin` 和 `LivingEntityHurtMixin` 做 Teyvat 过滤。其他 Mixin（`PlayerAttackInterceptor`、`PlayerHurtInterceptor`、`LivingEntityDecayMixin` 等）按正常逻辑对所有目标应用。

---

## 7. 常见问题与设计决策

### 为什么 PGCharacter 使用原型模式？

角色注册时创建的是**原型实例**，玩家拥有的是从原型初始化数据后的实例副本。`CharacterRegister.getByUUID()` 从注册表查找原型，然后调用 `initBaseStats()` 用等级配置初始化属性。

### 为什么用 UUID 而不是 Identifier 区分角色？

UUID 是纯数字 (int)，在 Attachment 的持久化和 RPC 传输中更轻量。注册表中的 key 仍是 String（如 `"shenhe"`），但角色间通信时用 UUID (135001)。

### 效果是如何持久化的？

`CharacterEffectContainer` 在 `PGCharacterData` 中以 `ListTag` 存储。每次效果变化后调用 `syncEffectsToTag()` 同步。效果实例的反序列化通过 Registry ID 查找对应的 `ICharacterEffect` 实现。未注册的效果会用 `DummyEffect` 占位防止 NPE。

### 衰减计数器为什么存在目标实体上？

原神的附着冷却规则是"对同一个目标"的多个同类型攻击共享一个冷却计时器。因此计数器存储在**目标**身上，而不是攻击者身上。

### 为什么有 Dirty 标记？

角色数据只在 `dirty=true` 时才同步。每 tick 检查 dirty 标记并触发网络同步，避免每 tick 发送完整角色数据造成网络拥堵。

### 元素类型 FYSIKOS 是什么？

"Physical" 的希腊语，原神代码中表示物理伤害。`ElementalsGIM.FYSIKOS` 不参与元素反应。

### 为什么 DecaySequence 长度超过后返回 0？

原神中同类型攻击有附着上限（如15次普攻后不再附着），超过序列长度后系数为 0 实现这种"无限衰减"效果。使用 `DecaySequence.DEFAULT_DAMAGE`（全1）则不会衰减。

---

## 8. 关键文件索引

| 系统 | 文件路径 |
|------|----------|
| 主入口 | `Minegenshin.java` |
| 注册表总览 | `registry/ModRegistries.java` |
| 角色注册表 | `registry/register/CharacterRegister.java` |
| 效果注册表 | `registry/register/CharacterEffectRegister.java` |
| 实体注册表 | `registry/register/EntityRegister.java` |
| 伤害类型注册 | `registry/register/DamageTypeRegister.java` |
| 角色基类 | `core/character/PGCharacter.java` |
| 角色数据 | `core/character/PGCharacterData.java` |
| 角色帮助类 | `core/character/CharacterHelper.java` |
| 属性容器 | `core/attribute/AttributeContainer.java` |
| 属性实例 | `core/attribute/AttributeInstance.java` |
| 属性类型 | `core/attribute/AttributeType.java` |
| 属性注册 | `core/attribute/ModAttributes.java` |
| 效果接口 | `content/effect/character/ICharacterEffect.java` |
| 效果实例 | `content/effect/character/CharacterEffectInstance.java` |
| 效果容器 | `content/effect/character/CharacterEffectContainer.java` |
| 效果工具 | `content/effect/character/CharacterEffectHelper.java` |
| 伤害规格 | `core/system/combat/damage/ModDamageSpec.java` |
| 伤害源 | `core/system/combat/damage/ModDamageSource.java` |
| 衰减组别 | `core/system/combat/decay/DecayGroup.java` |
| 衰减序列 | `core/system/combat/damage/DecaySequence.java` |
| 计数器数据 | `core/system/combat/decay/DecayCounterData.java` |
| 计数器管理 | `core/system/combat/decay/DecayCounterManager.java` |
| 持有接口 | `core/system/combat/decay/IDecayCounterHolder.java` |
| 伤害计算 | `core/system/combat/attack/HurtEntityHelper.java` |
| 状态实例基类 | `core/status/StatusInstance.java` |
| 状态容器 | `core/attachment/StatusContainer.java` |
| 状态访问器 | `core/status/StatusAccessor.java` |
| 元素附着实例 | `core/system/about/ElementalAttachmentInstance.java` |
| 元素附着工具 | `core/system/about/ElementalAttachmentHelper.java` |
| 附着预设 | `core/system/about/AttachmentProfile.java` |
| 附着来源 | `core/system/about/AttachmentSource.java` |
| 状态 Tick | `event/server/StatusTickEvent.java` |
| 元素反应基类 | `core/system/reaction/ElementalReaction.java` |
| 反应管理器 | `core/system/reaction/ElementalReactionManager.java` |
| 反应上下文 | `core/system/reaction/ReactionContext.java` |
| 反应结果 | `core/system/reaction/ReactionResult.java` |
| 优先级计算器 | `core/system/reaction/ReactionPriorityCalculator.java` |
| 蒸发 | `core/system/reaction/builtin/VaporizeReaction.java` |
| 融化 | `core/system/reaction/builtin/MeltReaction.java` |
| 冻结 | `core/system/reaction/builtin/FreezeReaction.java` |
| 反应注册 | `core/system/registry/register/ElementalReactionRegister.java` |
| DataComponent 注册 | `core/system/registry/register/StatusDataComponents.java` |
| 玩家附件 | `core/attachment/PlayerCharactersAttachment.java` |
| 附件注册 | `core/attachment/AttachmentRegistration.java` |
| 网络同步 | `core/network/NetworkManager.java` |
| 客户端处理 | `core/network/ClientHandler.java` |
| 角色 Tick | `event/server/CharacterTickEvent.java` |
| 效果 Tick | `event/server/CharacterEffectEvent.java` |
| 玩家登录 | `event/server/PlayerLoginEventListeners.java` |
| Mixin 配置 | `resources/minegenshin.mixins.json` |
| Mixin 注入 | `mixin/mixins/LivingEntityDecayMixin.java` |
| Mixin 攻击拦截 | `mixin/mixins/PlayerAttackInterceptor.java` |
| 配置文件 | `Config.java` |
| 元素枚举 | `enums/ElementalsGIM.java` |
| 攻击类型 | `enums/AttackType.java` |
| 突破属性 | `enums/CharacterAscendAttribute.java` |