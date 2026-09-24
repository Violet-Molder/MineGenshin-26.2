# 注册中心与内容注册

## 这个系统负责什么

把本 MOD 的所有游戏内容（物品、方块、实体、属性、效果、伤害类型、数据组件、菜单、角色数据、元素与反应）注册进 NeoForge 的注册表，并保证注册 ID 的唯一与稳定。

## 目录与分工

| 位置 | 内容 |
|---|---|
| `core/system/registry/ModRegistries.java` | 自定义注册表的宿主（本 MOD 自己维护的注册表） |
| `core/system/registry/register/` | 逐个注册类，一个域一个文件 |
| `content/entities/ModEntities.java` | 实体类型 + 属性 + 生成位置（内容侧自带，不放在 registry 包） |
| `Minegenshin.java` | 唯一的挂载点：构造函数里依次调用各注册类的 `register(modEventBus)` |

`register/` 下的注册类：

| 类 | 注册内容 |
|---|---|
| `ModItems` | 物品与物品栏图标来源，另含 `getArtifactSetItemsMap()` 供掉落系统使用 |
| `ModBlocks` / `ModItemGroups` | 方块、创造物品栏页签 |
| `ModAttributes` | 自定义属性（如元素增伤、护盾强效） |
| `ModMobEffects` | 状态效果（如眩晕 `STUN`） |
| `ModDamageTypes` | 伤害类型（`DamageType` + `DamageSource` 键） |
| `ModDataComponents` / `ModStatusDataComponents` | 物品数据组件（圣遗物词条、武器词条、状态数据） |
| `ModMenus` | 容器菜单类型（背包、角色信息） |
| `ModCharacters` | 角色数据注册（UUID → `PGCharacter`），并提供 `getByUUID` |
| `ModElements` / `ModElementalReactions` | 元素与元素反应 |
| `ModStatusInstanceTypes` | 附着/状态实例类型 |

## 注册的数据流

1. `Minegenshin` 的构造函数拿到 `IEventBus modEventBus`。
2. 依次调用各注册类的 `register(modEventBus)`；每个注册类内部把 `DeferredRegister` 挂到该总线。
3. 游戏加载阶段 NeoForge 统一触发注册事件，`DeferredHolder` 才真正有值。
4. 因此**任何注册类的静态字段在构造期都不能当作已就绪使用**——只能传 `DeferredHolder`/`Supplier`，真正取值要用 `.get()`。

```java
// 典型写法：注册的是"工厂"，不是实例
public static final DeferredHolder<Item, Item> PRIMOGEM =
        ITEMS.register("primogem", () -> new Item(new Item.Properties()));
```

## 注册 ID 的稳定性规则

注册名会写进存档、数据包与网络协议，属于兼容性契约：

| 可以改 | 不能改 |
|---|---|
| 类名、包名、方法名、文件位置 | `register("...")` 里的字符串 |
| 注册类内部的组织方式 | `ResourceKey` / `ResourceLocation` 的路径段 |
| 注册类的拆分与合并 | 数据组件 id、语言文件 key、战利品表路径 |

改 ID 的后果：旧存档里的物品/实体变成"未知物品"，数据包引用失效，联机双方版本不一致时报错。

## 加一个新内容要动哪里

以"新物品"为例：

1. 在 `content/items/<类别>/` 下写物品类（继承 `TeyvatItem` 或原版 `Item`）。
2. 在 `ModItems` 里 `register(...)`，命名遵循现有风格。
3. 需要出现在创造物品栏 → 在 `ModItemGroups` 的对应页签里加一项。
4. 资源：`assets/minegenshin/items/<name>.json`（物品定义）、模型与贴图；语言文件补 `item.minegenshin.<name>`（`zh_cn.json` 与 `en_us.json` 都要）。
5. 掉落/配方需要的话，走数据生成或数据包。

实体、属性、伤害类型、菜单的加法和物品同构：**类写在内容包，注册写在对应注册类，资源与语言补齐**。

## 常见坑

| 现象 | 原因 |
|---|---|
| 启动报 `Registry is already frozen` | 在注册事件之后才去注册，或在错误的阶段调用 `register` |
| 创造栏里没有新物品 | 忘了加到 `ModItemGroups`，或物品栏页签用的语言 key 缺失 |
| 数据组件为 `null` | 组件类型没注册，或从旧存档读到时类型对不上 |
| 联机时客户端不认识某物品 | 双方 MOD 版本/注册名不一致 |
| 专用服务器加载崩溃 | 注册类里 import 了 `net.minecraft.client.*` 或本项目的 `client.*`（注册类属于公共侧） |
