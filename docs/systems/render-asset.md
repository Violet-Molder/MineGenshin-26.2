# 资源、渲染与界面

## 资源路径规则

MineGenshin 不用 GeckoLib 默认目录，而是把资源按"类别 + id"统一布局，由 `core/asset/` 解析。

| 类 | 职责 |
|---|---|
| `GenshinAssets` | 客户端 setup 时 `installDefaults()`，安装路径规则 |
| `AssetPathResolver` | 按类别与 id 解析出实际资源路径 |
| `ModAssetPaths` | 本 MOD 的资源根与约定 |
| `AssetCategory` / `AssetSet` | 类别枚举（entity / item / character …）与资源集合 |
| `ItemIcons` | 物品 GUI 图标路径规则（GeckoLib 物品的贴图特殊处理） |

统一布局（**入口按对象，对象内部按类型**）—— 这套布局已经落到磁盘，不再是计划：

```
assets/minegenshin/
├── character/<角色id>/            一个角色 = 一个自包含文件夹，内部再按类型分
│      <id>.animation.json         主动画；<id>_fp.animation.json 等附加动画（模型/动画留对象根）
│      textures/<id>.png           角色模型贴图（默认共用 character/default/textures/default.png）
│      textures/avatar.png         列表头像
│      textures/avatar_hud.png     HUD / 圣遗物佩戴者叠加头像
│      textures/pose_prepare.png   编队立绘（可选中的角色）
│      textures/pose_already.png   编队立绘（已在队伍）
│      textures/skill.png / burst.png   元素战技 / 元素爆发图标
│      sounds.json / sounds/*.ogg  音效定义与音频（ogg 丢进 sounds/ 就生效）
│      local/<名字>.geo.json       自己的模型 / 动画放这里：明文直读，同名时优先
├── entity/<实体id>/     <id>.geo.json | <id>.animation.json | textures/<id>.png
├── item/<物品id>/       definition.json | model.json                     （原版入口文件）
│                        textures/texture.png | icon.png | <id>.png       （平面贴图 / 图标 / geo 贴图）
├── block/<方块id>/      blockstate.json | model.json                     （原版入口文件）
│                        textures/texture.png | icon.png | <id>.png       （图集贴图 / 图标 / geo 贴图）
│                        blockitem/{definition,model}.json | blockitem/textures/…
├── gui/<名字>.png                 共用界面贴图（血条 / 遮罩 / 边框 / 按钮 / 占位图）
├── icon/<分类>/<名字>.png         跨对象图标（elemental 元素图等）
├── lss/                           界面样式表
└── lang/                          原版语言文件（唯一剩下的原版入口层，见下）
```

### 模型与动画：资源包 + `local/` 明文目录

自带角色的模型与动画**不再以一份份可读 JSON 出现在仓库和发行包里**，而是收进一个资源包
（整包一个文件；它放在哪个目录属实现细节，不在本文展开）。对外只承诺三件事：

- **逻辑路径没有变**：代码里照旧写 `character/vesna/vesna.geo.json` 这样的路径，
  后缀判断、缓存键、目录索引全部沿用本文档后面描述的规则，读取侧只换了「字节从哪来」；
- **自己的模型 / 动画放 `local/`**：`character/<id>/local/vesna.geo.json`、
  `entity/<id>/local/<id>.animation.json` 这一类路径**明文直读**，放进对象目录重启即生效，
  不需要任何额外步骤；同名时**`local/` 里那份优先于资源包**；
- **磁盘优先于资源包**：同一个逻辑路径在仓库里也有文件时以文件为准（方便临时对照调试）。

`local/` 只影响「资源从哪读」和「同名谁优先」，**不改变资源身份**：
`character/vesna/local/vesna.geo.json` 与 `character/vesna/vesna.geo.json` 是同一个缓存键，
所以 `CharacterRenderData` 之类的配置一个字都不用改。

**明文放在哪、谁进仓库。** 自己手上的明文模型 / 动画放在它们正常的资源路径下就行
（`character/<角色id>/vesna.geo.json` 这一类），改完构建一次即可生效；
这些路径在仓库里由 `.gitignore` 排除，所以提交进仓库的**只有资源包那一个文件**，
唯一的明文口子是 `local/`。发行形态同样是单文件：模型与动画跟着 jar 一起发，
玩家把 jar 与依赖 Mod 放进 `mods/` 即可 —— 没有附加文件，也不需要单独下载资源。

### 原版入口层由重定向层供料

原版有四处资源入口是写死路径的。**本 MOD 不再把它们留在原版根目录，而是由 `AssetRedirects` 在读取时改写**：
`FileToIdConverterRedirectMixin` 注入 `FileToIdConverter#listMatchingResources` /
`listMatchingResourceStacks`，往返回的文件表里补进我们布局里的文件（只补 `minegenshin` 命名空间，
且原版已有同名真实文件时不覆盖，方便临时做 A/B 对照）。

| 原版入口 | 我们布局里的真身 |
|---|---|
| `blockstates/<方块id>.json` | `block/<方块id>/blockstate.json` |
| `items/<物品id>.json` | `item/<物品id>/definition.json`（方块物品回落 `block/<方块id>/blockitem/definition.json`） |
| `models/<路径>.json` | `<路径>.json`（去掉 `models/` 前缀） |
| `textures/<路径>.png` | `<路径>.png`（去掉 `textures/` 前缀；只接管 `textures/item` 与 `textures/block` 两个图集目录源） |

> 对象目录里的 `textures/` 是**布局**（对象内部按类型分类），不是资源身份的一部分：
> `item/<id>/textures/texture.png` 镜像给原版时会把这一层去掉，sprite id 仍是
> `minegenshin:item/<id>/texture` —— 所以**模型 JSON 与数据生成都不用因为这次调整而改动**。

实测这四条通道**都**经由 `FileToIdConverter` —— `BlockStateModelLoader`（blockstates）、
`ClientItemInfoLoader`（items）、`ModelManager`（models）、图集的 `DirectoryLister`
（`assets/minecraft/atlases/items.json` 的 `source: item` 与 `blocks.json` 的 `source: block`）——
所以一个注入点就让四条通道同时生效：原版加载逻辑一行未改，GeckoLib 也完全没碰。

实测证据（客户端 `run/logs/debug.log`）：

```
[AssetRedirects] 原版目录 'blockstates' 注入 1 条虚拟入口：minegenshin:blockstates/test_block.json
[AssetRedirects] 原版目录 'items'       注入 24 条虚拟入口：…
[AssetRedirects] 原版目录 'models'      注入 24 条虚拟入口：…
[AssetRedirects] 原版目录 'textures/item'  注入 25 条虚拟入口：…
[AssetRedirects] 原版目录 'textures/block' 注入 1 条虚拟入口：…
```

### 还剩下的原版硬性项

| 资源 | 读取方 | 路径自由度 |
|---|---|---|
| GeckoLib 模型 / 动画 / 贴图（角色、实体、geo 物品） | 本 MOD 自己的缓存 | 完全自由 |
| 代码里直接给 `Identifier` 的贴图（LDLib `SpriteTexture`、`guiGraphics.blit`） | 本项目代码 | 完全自由，Identifier 与文件路径**一字不差**（不带 `textures/` 前缀） |
| `lang/<语言>.json` | 原版 | 完全固定（一种语言一个文件，不做重定向） |

数据生成按同一套规则落盘：`ModModeProvider` 把物品定义与平面模型写进
`item/<物品id>/definition.json` 与 `item/<物品id>/model.json`，模型里的 `layer0` 指向
`item/<物品id>/texture.png`（sprite id `minegenshin:item/<物品id>/texture`）。
**改布局时要同步三处**：`AssetRedirects` 的规则表、`ModAssetPaths`/`AssetSet` 的路径助手、数据生成器的产出路径。

注意：`textures/item/*.png` 这类「原版根目录下的文件」在本 MOD 已全部搬空，
`items/`、`models/`、`blockstates/`、`textures/` 四个目录在 `assets/minegenshin/` 下**不再存在**。

### 还没做的（需要单独决定）

| 项 | 为什么没做 |
|---|---|
| 非角色音效 `sound/<分类>/` | 与 `CharacterSounds` 同形的扫描器还没写；仓库目前也没有 ogg，先立约定 |
| 实机观感 | 本环境只能跑到标题界面：背包/装备界面的物品图标、HUD、角色立绘需要人工看一眼（重定向层本身已由 `runClient` 日志验证注入正确、零 ERROR） |
| 第一次加真方块时的回归 | 方块通道已用临时 `test_block` 探针验证过（`blockstates` / `textures/block` / `models` 三处注入成功），探针已回退；等第一个真方块落地时按同一份日志再看一眼即可 |

> `block/<方块id>/blockstate.json` 曾经列为「做不了」，**第三轮已解决**：由 `AssetRedirects` +
> `FileToIdConverterRedirectMixin` 在读取时把 `blockstates/<id>.json` 改写到我们的布局，不需要数据生成复制，也不需要资源包垫片。

## GeckoLib 接管层

| 类 | 位置 | 职责 |
|---|---|---|
| `GenshinGeoCache` | `client/render/geo/` | **全局资源重载监听器**：扫自己目录、烘培模型与动画 |
| `CategoryGeoModel` | 同上 | "类别 + id" 驱动的模型：解析模型、动画与贴图，含可读的失败提示 |
| `AssetGeoCache` | 同上 | 统一布局资源的解析缓存（**已注册为客户端资源重载监听器**；异常全兜住，空扫描不落锚、最多重扫 5 次） |
| `GenshinGeoModel` / `GenshinItemGeoModel` | 同上 | 角色/物品模型的 GeckoLib 适配 |
| `AttachmentHelper` | `client/render/character/` | 挂件（武器/特效）的骨骼挂点计算 |

### 为什么 `AssetGeoCache` 不做全局资源重载监听器

它曾经挂在 `AddClientReloadListenersEvent` 上：**一旦抛异常，整个客户端资源重载失败**，于是所有资源驱动的表现（角色动作、动画、贴图）一起失效，表现是"某些角色左键没反应、动画也不播"，排查时完全指不到它。现在改为客户端 setup 预热 + 首次查询时同步扫一次，影响范围被限制在"读统一布局资源"这一件事上。

新增资源缓存时沿用这条经验：**不要顺手把它挂到全局重载事件上**。

## 客户端渲染分层

| 位置 | 内容 |
|---|---|
| `client/render/character/` | 角色渲染接管：`FirstPersonCharacterRenderer`（第一人称手臂）、`GenshinReplacedPlayer`、`CharacterRenderDispatcher` |
| `render/entity/` | 实体渲染器（`ElementalOrbRenderer`、`IceBlockProjectileRenderer`、`ThunderCloudRenderer` 等），在 `MinegenshinClient.registerEntityRenderers` 注册 |
| `client/damage/` | 伤害飘字：`DamageIndicator`（动画状态）、`DamageIndicatorManager`（活跃列表）、`DamageIndicatorRenderer`（HUD 构建） |
| `render/gui/hud/` | HUD 层：`DamageIndicatorHudRegistration`、`MGHud`、`MobHealthBarHud`、`VesnaEnergyHud`、`DebugInfoScreen`、`MyModularHudLayer`、`HealthBarTrail` |
| `render/gui/screens/` | 大界面：角色信息、背包、升格、编队、祈愿、圣遗物装备，`GUIServerHelperGIM` / `GUIClientHelperGIM` 负责打开与数据准备 |
| `render/gui/components/` | 可复用控件（进度条、图标、状态绑定组件 `state_bind_com/`） |
| `render/gui/menu/` | 容器菜单（背包、角色信息）与排序 |

### HUD 层的注册方式

每个 HUD 自己监听 `RegisterGuiLayersEvent` 并注册自己的层（`@EventBusSubscriber(Dist.CLIENT)`），因此**同一个事件有多个监听点**——这是允许的：每个 HUD 管自己的一块区域。`DamageIndicatorHudRegistration` 用的就是 LDLib2 的 `ModularHudLayer` + LSS 样式表（`lss/hud/damage_indicator.lss`）。

## 加一个界面 / 一个渲染器

1. 实体渲染器：写渲染器类放 `render/entity/`，在 `MinegenshinClient.registerEntityRenderers` 加一行，资源按 `entity/<id>/` 布局。
2. HUD 层：新建类实现自己的注册监听（参考 `MobHealthBarHud`），或复用 LDLib2 的 ModularUI + LSS。
3. 大界面：`Screen` 子类放 `render/gui/screens/`，需要容器数据时配 `AbstractContainerMenu`（`render/gui/menu/`）+ 在 `ModMenus` 注册 + 在 `MinegenshinClient.registerMenuScreens` 绑定界面类。
4. 样式：优先用 LSS/样式表，而不是在代码里堆颜色与尺寸。

## 常见坑

| 现象 | 原因 |
|---|---|
| 模型/动画不加载 | 资源不在统一布局里；或 `AssetGeoCache` 未预热、`GenshinGeoCache` 没扫到 |
| 动画解析失败但有文件 | `CategoryGeoModel` 的提示会区分"文件没扫到"与"键名对不上"，先看日志给出的可用动画名 |
| 界面打开是空的 | 菜单没在 `ModMenus` 注册，或 `registerMenuScreens` 没绑定 |
| 专用服务器崩溃 | 渲染/界面类被公共侧引用；所有 `client`/`render` 类只能在客户端侧被引用 |
| 贴图错位/物品图标空白 | 物品图标走 `ItemIcons` 规则，geo 物品的贴图路径与普通物品不同 |
