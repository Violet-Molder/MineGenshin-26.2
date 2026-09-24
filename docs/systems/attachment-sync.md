# 附件与数据同步

## 这个系统负责什么

把"属于玩家的数据"和"属于其他实体的数据"存起来、跨死亡与登录保持、并在服务端与客户端之间保持一致。本项目所有玩家级状态都走附件（Attachment），不放在玩家实体字段里。

## 附件：数据存在哪

| 附件 | 内容 |
|---|---|
| `PLAYER_CHARACTERS_ATTACHMENT` | 队伍 4 格、持有角色、编队索引、当前出战 |
| `PRIMOGEM_ATTACHMENT` | 原石数量 |
| `GENSHIN_MODE_ATTACHMENT` | 是否处于原神模式 |
| `ADVENTURER_INFO_ATTACHMENT` | 冒险等阶、世界等级、突破等级、当前经验 |
| `CONTAINER`（`StatusContainer`） | 附着/状态容器（生物与玩家都有） |
| `Backpack` | 背包数据（圣遗物、武器、材料） |

访问方式统一是 `player.getData(...)` / `entity.setData(...)`；注册入口在 `core/attachment/AttachmentRegistration`，**不要在别处另开注册表**。

## 同步通道

| 方向 | 机制 | 用途 |
|---|---|---|
| 服务端 → 客户端 | `core/network/NetworkManager` 的各类 `set...ToPlayer` | 原石、模式、入侵状态、编队、角色整包 |
| 客户端 → 服务端 | `NetworkManager` 的 `...ToServer`（走 LDLib2 RPC） | 攻击请求、切人、祈愿、开界面 |
| 双向（数据对象） | `core/sync/` 的 `ISyncCharacter` / `ISyncManagedEntity` / `ModSyncAccessors` | 需要按字段同步的数据对象 |
| 客户端表现指令 | `core/network/DamageIndicatorRpc` | 伤害飘字（纯表现，单向下发） |

同步契约：**字段名与顺序一旦发布就是网络协议**，改名/删字段要安排版本迁移。

## 三个关键时机的数据流

### 登录（`event/PlayerLoginEventListeners` → `core/sync/CharacterDataSyncEventHandler`）

1. 读取世界入侵状态并下发（`setInvasionStatusToPlayer`）
2. 未入侵则到此为止
3. 已入侵：下发原石、原神模式、冒险信息（`adventurerInfo.syncToPlayer`）
4. 修复角色类型、绑定归属（`fixCharacterTypes` / `bindAllOwners`）
5. 首次进入的玩家补发默认角色（申鹤 135001）并入队
6. 整包下发角色数据（`playerData.syncToPlayer`）

### 死亡重生（`core/attachment/PlayerDeathCloneHandler`，监听 `PlayerEvent.Clone`）

把原实体上的不可变数据（原石、模式）与持久对象（冒险信息、角色、状态容器）搬到新实体，最后统一下发一次，避免客户端看到重生后的旧数据。注意：这一步同样只在入侵状态下执行。

### 每 tick 的增量同步

角色数据用**脏标记**驱动：改动时 `markDirty()`，`CharacterTickHandler` 每 tick 检查并整包同步该角色（`syncSingleCharacterToPlayer`）。不是字段级增量，所以"频繁改"会很贵——批量改动时先改完再标脏。

## 加一个玩家级数据要动哪里

1. 写附件类（继承 NeoForge 的 `AttachmentType` 载体，参考 `AdventurerInfoAttachment`）。
2. 在 `AttachmentRegistration` 注册，拿到 `DeferredHolder`。
3. 需要下发 → 在 `NetworkManager` 加一个 `set...ToPlayer`，并在登录流程里补一次（否则重连后客户端是旧值）。
4. 需要跨死亡保留 → 在 `PlayerDeathCloneHandler` 里补搬迁。
5. 需要持久化 → 在附件类里实现序列化（`Codec`/`ValueInput`-`ValueOutput`），**字段名不能随意改**。

## 常见坑

| 现象 | 原因 |
|---|---|
| 重连后数据不对 | 只做了运行时同步，没在登录流程补齐 |
| 死亡后数据丢失 | 附件没有在 `Clone` 事件里搬迁，或没做持久化 |
| 客户端显示旧值 | 改完没下发；或下发时机早于附件写入 |
| 专用服务器崩溃 | 附件类里引用了客户端类（附件属于公共侧） |
| 同步字段错位 | 改过字段顺序/类型却没做版本迁移 |
