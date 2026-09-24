# 更新日志

> **本日志自 1.0.0 重新建立。** 1.0.0 之前的 0.x 记录不再维护（需要时见 git 历史）。
>
> 记录约定：**每个版本的条目只在该版本发布时写一次**，后续版本只写"新增 / 变更 / 修复"，不重复 1.0.0 已记录的内容。新增版本时复制文末模板。

---

## 1.0.1 — 2026-09-24

本版两条主线：**资源布局统一**（项目自己的「入口按对象、对象内部按类型」布局成为唯一真相，
原版四个写死路径的入口改由本 MOD 的重定向层供料）与**元素起源**
（元素附着与元素反应收敛成单一入口，生物、方块、出战角色共用同一套「可附着宿主」，
元素不再只是挂在某个方块上的旁路）。

### 新增

- **资源重定向层**（`core/asset/AssetRedirects` + `mixin/mixins/FileToIdConverterRedirectMixin`）：
  在 `FileToIdConverter` 上补进虚拟入口，让原版四条通道都读我们的布局 ——
  `blockstates/<id>.json` ← `block/<id>/blockstate.json`、
  `items/<id>.json` ← `item/<id>/definition.json`（方块物品回落 `block/<方块id>/blockitem/definition.json`）、
  `models/<路径>.json` ← `<路径>.json`、`textures/<路径>.png` ← `<路径>.png`。
  **只对本 MOD 命名空间生效**，整合包里其它 MOD 的目录完全不受影响。
- **对象目录内的 `textures/` 与 `sounds/` 子目录**：一个角色以后会有很多张图与很多条语音，
  所以对象内部按类型再分一层（模型与动画留在对象根，文件名自带类型后缀）。
- **元素载体「可附着宿主」抽象**（`core/system/about/host/`）：生物、方块、出战角色共用同一个附着入口
  `ElementalAttachmentHelper.attach(宿主, ...)` —— 「附着 → 附着内反应 → 反应引发效果」这条链只有一份实现。
  新增一种"与元素有关的方块"只需要注册两条（能不能被附着、挂上之后变成什么），**不用改核心代码**。
- **环境自附着**：完整水源自带水元素、冰族自带冰元素，不衰减。"冰打水面能冻结"靠的是水这边这份先手元素，
  而不是"方块状态对不上就改状态"。
- **攻击范围附着**：原神模式每次攻击的伤害点按这一招的攻击距离取一次范围，把范围内的可附着方块
  送进同一个附着入口 —— 方块不再是一条只能靠左键点击去补的支线。
- **寒元素（`COLD`）与伴随机制**：冰的减速、冻的禁 AI 不再由冰/冻元素各自硬编码，而是集中到"效果载体"
  子元素寒身上 —— 冰/冻只负责附着，寒由 `ColdAura` 每 tick 伴随同步（有冰/冻就补、冰和冻都没了就撤）。

### 变更

- **资源布局全面收敛**：`assets/minegenshin/` 下只剩 `character/ item/ block/ entity/ gui/ icon/ lss/ lang/`
  七个顶层目录。原版的 `items/`、`models/`、`blockstates/`、`textures/` 在本命名空间下**不再存在**，
  `lang/` 是唯一保留的原版硬性入口（一种语言一个文件，不做重定向）。
- **物品资源全部进 `item/<物品id>/`**：`definition.json`（物品定义）、`model.json`（平面模型）、
  `textures/{texture,icon}.png`（平面贴图 / GUI 图标），geo 物品另有 `<id>.geo.json` 与 `textures/<id>.png`。
- **方块通道就位**：`block/<方块id>/` 下 `blockstate.json`、`model.json`、`textures/`、
  `blockitem/{definition,model}.json` + `blockitem/textures/`。
- **数据生成改产出我们的布局**：`ModModeProvider` 不再继承原版 `ModelProvider`，
  直接写 `item/<物品id>/{definition,model}.json`（模型 `layer0` 指向 `item/<物品id>/textures/texture.png`），
  并自带「本命名空间每个物品都要有定义」的等价校验。
- **角色美术归位**：头像 / HUD 头像 / 两种立绘 / 技能图标进 `character/<角色id>/textures/`，
  共用界面贴图进 `gui/`，元素图标进 `icon/elemental/`，实体三件套进 `entity/<实体id>/`。
- `ItemIcons` 解析顺序调整为「对象目录图标 → 通用图标目录 → 平面贴图 → geo 贴图兜底」。
- **免疫只拦伤害，不再吞附着**：元素生物（冰史莱姆、冰方块）照样会被挂上火/水并正常反应，同元素伤害仍为 0。
- **拒收附着 = 不反应**：修复"大型冰史莱姆明确拒绝水、水打上去却照样冻结"这类规则与行为不一致。
- **冻结对原版怪也生效**：冻元素存在期间关掉 AI 并清掉位移 —— 以前只对本 MOD 生物生效，
  而且水流推动/浮力仍会累积位移，解冻瞬间会"弹"回原位。
- **方块上的冻结/融化不显示反应文字**：水结冰、冰化水是形态变化，不再飘"冻结/融化"；
  同样的反应打在生物身上照常出字。
- **流动水完全不参与元素体系**：只有完整水源会被冻成浮冰（流动水既不自带水、也不收冰）——
  从根上避免"化开之后水位对不上 / 被原版水流灌回满水方块"。
- 附着档位映射修正：2.0U 现在走 `STRONG`（1.6U / 12s），此前被错误映射成 `MEDIUM`（1.2U / 10.75s）。
- 雷 + 冰的**超导反应**已注册并接入伤害（实现一直存在，却从未注册，配置项是死的）。
- **元素生物"免疫冰"的落地方式改了**：从"跳过减速"改成**不收寒**
  （`ElementalCreature.acceptsElementAttachment`）。冰史莱姆照样给自己挂冰、也能被挂冻，
  但收不到寒 → 既不被拖慢也不被冻住；豁免只需表达一次，不必在每个效果里各判一遍。
- `CryoElement` / `FrozenElement` 已删除（效果搬进寒之后只剩空壳，留着就是两份真相）。
- 冻结期间的位移锁判据改为"有寒且有冻"（`StatusTickHandler` → `ColdAura`）——
  冻元素没被宿主接受寒的单位本就不该被冻住，位移自然也不该锁。

### 修复

- `AssetGeoCache` 原先在客户端初始化阶段预热（那时资源还没就绪），索引恒为空、
  每次都回退到 `GenshinGeoCache`；现在注册为客户端资源重载监听器，异常全兜住，空扫描不落锚、最多重扫 5 次。
- `ModBlocks.register(modEventBus)` 从来没被主类调用（空注册器死脚手架），已接线。
- `GenshinAssets` 的 javadoc 声称「通过 `GeckoLibResourcesMixin` 把 `character/**` 加进 GeckoLib 扫描范围」——
  该 mixin 并不存在，已改写成真实机制（`GenshinGeoCache` 自扫自烘 + `GenshinGeoModel` 覆写两个 public 方法）。
- 删除 22 个死文件与误粘文件（`blockstates/flower_block.json`、`textures/block/*`、
  `textures/icon/**` 的逐字节副本、两张 `slime_cyro`、`textures/item/img.png`、`textures/gui/shenhe.png`、
  `character/vesna/{vesna.json, vesna_texture.png, a.json}`、`assets/color.txt`、`code.txt`、`render.txt`、
  `latest.log`、`data/.../untitled-1.java`）；其中不可从 git 恢复的 `vesna/a.json` 已另行归档。
- 方块元素的推进从"原版方块 tick 链"改为集中式推进（`BlockElementTicker`）：修掉"一片冰里边缘逐个化、
  中间一直冻着"（排期链断掉）与"一起结的冰不同时化"（同一 tick 被重复推进一次衰减）。
- 浮冰的融化判据只看**冻元素**是否耗尽，不再看冰元素 —— 此前任何一份残留冰元素都会把浮冰永久钉住（永不化）。
- 自然融化收尾时的掉帧：以前每 tick 都 `setData` 把整张 chunk 元素表同步给客户端（掉帧元凶）；
  现在去掉该同步、去重改为纯内存、提交幂等化。
- 原版旁路切断：已在元素体系里的冰被**破坏**时不再直接变成水（原版 `IceBlock.playerDestroy` 的行为），
  浮冰也不再被"周围同类少于 2 个"的原版规则旁路融化；没有元素容器的浮冰仍交还原版。
- `AttachmentProfile.permanent()` 的常驻标志恒为 `false`（时长参数与判据不一致），已修正。
- `LargeCryoSlime` 直接覆盖 `onAttachElement`（只拒水）导致**整体绕过**元素生物的默认筛查，
  寒照样被收下 → 冰史莱姆会被减速甚至冻住；已改为"先拒水、其余委托默认筛查"。

### 兼容性

- **引用过旧资源路径的资源包 / 材质包需要迁移**：`textures/character_avatar/**`、`textures/character_party_pose/**`、
  `textures/skill/**`、`textures/elemental/**`、`geckolib/models|animations/entity/**`、
  `items/*.json`、`models/item/*.json`、`textures/item/*.png` 这些旧位置**全部失效**，请按新布局放置。
- **存档与配置不受影响**：没有改动注册 ID、数据组件、序列化格式与网络协议字段。
- 数据生成产物位置变了：跑 `runData` 现在产出到 `src/generated/resources/assets/minegenshin/item/<id>/`。
- **方块元素容器换了格式**（坐标 → 完整状态容器）：旧存档里的方块元素态**不做迁移** ——
  它本来就是秒级瞬态（浮冰几秒化回水），重进世界重新附着即可。
- 浮冰的寿命与之前的手感可能不同：冻元素现在与实体走同一套衰减（0.4U/s 起、冻结中逐秒加快），
  不再是固定 0.2U/s。

### 文档

- `docs/systems/render-asset.md` 按新布局重写：四条重定向通道的对应关系、实测注入日志、
  以及「还剩下的原版硬性项（只剩 `lang/`）」。
- `CHARACTER_SYSTEM.md` 第九部分同步布局与图标解析顺序，并补第三轮迁移对照表；
  `docs/entity-ai-goal-guide.md` 的资源契约一节同步，并标注早期测试实体已被删除。
- 更新日志新增 1.0.1 小节；`gradle.properties`、`version.json`、README 与站点首页的版本号同步抬到 1.0.1。
- 新增 `docs/systems/element-host.md`（可附着宿主：宿主契约、三段判断、环境自附着、攻击范围附着、性能纪律）；
  `docs/systems/element-reaction.md` 与 `docs/systems/combat-attack.md` 同步反应入口、飘字开关与方块附着路径；
  `CHARACTER_SYSTEM.md` 4.7 补环境自附着与攻击范围附着。

---

## 1.0.0 — 2026-09-24

首个正式版本：把原神的核心玩法机制完整接入 Minecraft 26.2 / NeoForge。

### 玩法内容

| 系统 | 内容 |
|---|---|
| 角色 | 薇斯娜、申鹤、阿蕾奇诺、雷电将军、哥伦比娅、沃雅妮莎；等级、突破、天赋、命座、编队 |
| 属性 | HP/ATK/DEF 等属性与修饰器（固定/百分比、临时/永久），支持上限放宽 |
| 元素 | 元素附着与量级、附着冷却（衰减序列） |
| 元素反应 | 蒸发、融化、冻结、超导等，倍率可配置 |
| 战斗 | 普攻/战技/闪避/爆发四键动作系统；攻击判定与伤害管线；索敌锁；动作打断 |
| 伤害表现 | 屏幕空间伤害飘字：普通/暴击/反应/治疗/文本五类，元素配色与渐变 |
| 圣遗物 | 配置驱动的套装、主副词条、升级经验、背包与装备栏 |
| 武器 | 按类别（单手剑/法器/长柄/弓/双手剑）的等级与词条、精炼 |
| 祈愿 | 原石抽卡与重复角色返还 |
| 怪物 | 等级与防御注入、可按入侵状态生成 |
| 护盾 | 护盾吸收、元素与形状配置、每 tick 结算 |
| 状态 | 附着状态容器，作用于生物、方块与物品 |
| 掉落 | 入侵状态下生物额外掉落（原石、随机圣遗物） |
| 界面 | 角色信息、背包、升格、编队、祈愿界面与 HUD |

### 架构与规范（1.0.0 起确立）

- **分层与依赖方向**：`config`/`enums` → `core` → `content` → 客户端（`client`/`render`）；公共侧不得依赖客户端。
- **钩子归属规范**：跨模块入口放 `event/`，单模块监听点下沉到模块包并命名 `XxxHandler`；同一事件允许多个监听点（前提是归属清晰、不是同一职责被写两遍）。
- **攻击统一**：攻击相关处理一律并入 `core/system/combat/attack`，技能与其它模块不得自行注册攻击监听。
- **分类维度**：武器/角色按武器类型分包；实体按"生物 / 领域 / 杂项"分包；配置按域分包。
- **测试内容清理**：移除早期的测试实体、测试物品与相关资源，并把其中混用的真功能（冰块投射物、实体渲染器、资源缓存预热）迁到正式位置。
- **命名与路径整理**：修正包名拼写、包与目录不一致、双 `render` 目录、监听类命名等问题。
- **结构整理（全量，2026-09-24 完成）**：事件监听按"跨模块入口留 `event/`、单模块下沉到模块包"归位；顶层 `enums` 包按功能拆分归位（`ElementalsGIM`→`core.element`、`AttackType`→`combat.attack`、`AttachmentType`→`core.system.about`、`ElementalReactionType`→`core.system.reaction`、`CharacterAscendAttribute`→`core.character`）；`AttackType` 与 `DamageTypeEnum` 的重叠合并（删除零引用重复定义）；公共侧引用客户端类的问题清零——飘字改走 `api/damage` 契约、`getActiveCharacterId` 下沉到 `CharacterHelper`、动作与按键相关类整体迁到 `client.combat.{action,state}`；四个大类完成拆分或判定：`CharacterActionData`（684→146 行 + 10 个同包顶层类型）、`ResourceDrivenActionHandler`（707→554 行 + 2 个新类）、`ActionStateMachine`（716→680 行 + 2 个新类）、`ModDamageSpec` 判定不拆（内聚的不可变规格）。

### 文档

- 新增文档站（`docs/index.html`，带侧边栏导航）：技术文档、实体开发文档。
- README 改为项目介绍与开源协议；原 README 的开发者内容重写进技术文档。
- 更新日志从本版本重新建立。
- 文档站改为 `web/` 模块（Spring Boot + Java），Markdown 请求时实时渲染，菜单由后端 `/api/docs` 提供（`DocCatalog` 为唯一来源）；系统详解按模块拆成 11 篇。
- **全量同步（2026-09-24，两轮）**：按 20+ 个"结构整理中改过名的符号"扫描全部文档面（根目录 Markdown、`docs/**`、`web/**` 的源码与手写页），修正此前累积的滞后——`CHARACTER_SYSTEM.md`/`CHARACTER_IMPLEMENTATIONS.md` 的旧监听类名与旧路径、`entity-ai-goal-guide.md` 提到的已删除测试实体与旧资源路径、`README.md` 指向已不存在静态页的链接、`web/README.md` 路由表、站点首页指向已删除 `/technical.html` 的死链卡片，以及 `CHANGELOG` 的待办清单（重写为四条真实未决项）。

### 已知待办

- 客户端手感需实机确认：本轮把动作与按键相关类整体迁到 `client.combat.{action,state}`，已通过编译与冒烟加载（14 个 Mixin 全部注入），但按键与动作手感需在游戏中实测。
- `textures/skill/raiden_shogun_skill.png` 资源缺失（客户端日志会报 Missing resource，与本次结构整理无关）。
- `ClaymoreCharacter` 暂无子类（第四个武器类别的结构占位）；`MaskGenerator` 是生成飘字遮罩贴图的开发工具（带 `main()`），都非死代码，保留。
- 命令行执行 `runClient` 需注意 `build.gradle` 里的 `-XX:+AllowEnhancedClassRedefinition` 是 JBR 专有参数：用 IDEA（自带 JBR）可直接运行，用命令行 JDK 会拒绝启动。


---

## 新版本模板

```markdown
## x.y.z — YYYY-MM-DD

### 新增
- 

### 变更
- 

### 修复
- 

### 兼容性
- 需要版本迁移的存档 / 数据包 / 网络协议变化
```
