# BLEEDTHROUGH 实施计划

## 目标与原则

近期目标不是制作完整内容，而是证明动态世界状态能在真实存档中安全、可迁移、可回退地运行。每个阶段采用小闭环：代码、数据、自动测试、客户端验证和专用服务器验证一起完成。

除修复逻辑矛盾、明确边界或根据实测收缩方案外，总体设计在技术验证期间冻结。

## 当前状态

2026-09-10 8.1 复核：本地实施分支基于已合并 PR #15 的 `983796e`；ADR #16 的提交因远端目标分支未回到 main，已显式纳入本分支，避免遗漏前置契约。`test`（70 项）与 `runGameTestServer`（28 项）通过。真实客户端、真实专服玩家往返、重启后的玩家级往返、死亡掉落与多人网络验证仍为 `[~]`，不以自动测试替代。具体任务、依赖和后续 Phase 8–10 拆分见 [后续开发路线](ROADMAP_REMAINING.md)。

- [x] Forge 1.20.1 / Forge 47.4.22 MDK 已导入
- [x] JDK 17 与 Gradle 8.8 构建通过
- [x] `./gradlew build` 在新工作区路径下通过
- [x] MDK 示例身份已替换为 `meatscape` / `com.bleedthrough.meatscape`
- [x] JUnit 最小测试与 Forge GameTest 已建立并通过
- [x] Phase 0 已达到完成定义（2026-08-18）
- [x] Phase 1 Maw Coherence 数据 Spike 已达到完成定义（2026-08-18）
- [x] Phase 2 Rift 抽象数值源 Spike 已达到完成定义（2026-08-18）
- [x] Phase 3 空 Evolution Scheduler Spike 已达到完成定义（2026-08-19）
- [x] Phase 4 Provenance 与安全转换 Spike 已达到完成定义（2026-08-19）
- [x] Phase 5 Rollback／Severance 原型已达到完成定义（2026-08-20）
- [x] Phase 6 Vertical Slice 技术范围已实现并通过自动化验证（2026-08-28）
- [ ] Phase 6 体验平衡待模型、贴图和环境表现具备后，在 Alpha／Beta 由测试玩家评估；不阻塞 Phase 7
- [ ] Phase 7 Core Alpha 进行中，分项范围见下文和 `PHASE_7_CORE_ALPHA.md`

## Phase 0 — 仓库与 Forge 骨架

### 0.1 正式项目身份

- [x] 将 `examplemod` 改为 `meatscape`。
- [x] 将 Java 包改为 `com.bleedthrough.meatscape`。
- [x] 更新 Mod 名称、作者、描述、许可证决策和归档名。
- [x] 删除或重写 MDK 示例逻辑与弃用调用。

### 0.2 仓库约束

- [x] 增加仓库级 `AGENTS.md`。
- [x] 建立 `core/coherence/world/ecology/bioindustry/client/debug` 包边界。
- [x] 建立测试源码集、GameTest 入口和 dedicated server 启动检查。
- [x] 确认 Gradle 依赖与缓存继续使用全局持久目录，不使用系统临时目录。

### 完成定义

- [x] `./gradlew build` 无示例代码警告并成功输出 `meatscape-0.1.0-alpha.1.jar`。
- [x] 客户端开发运行配置能启动到主菜单。
- [x] dedicated GameTest server 能启动、加载 Mod、执行测试并正常关闭。
- [x] JUnit 最小自动测试在 `test` 任务中执行，而非 `NO-SOURCE`。

### 验证记录（2026-08-18）

- JDK：OpenJDK 17.0.19。
- `./gradlew build --stacktrace`：通过，JUnit `test` 任务执行，产物生成。
- `./gradlew runGameTestServer --console=plain`：通过；1/1 required tests passed，服务器保存全部维度后正常关闭。
- `./gradlew runClient --console=plain`：`meatscape` 成功加载，OpenAL、资源包和纹理图集初始化完成；到达主菜单后人工终止测试实例。
- 已知环境提示：Linux 可选旁白库 `libflite.so` 缺失；不属于 Meatscape 代码错误，不影响主菜单和服务端验证。

## Phase 1 — Spike 1：Maw Coherence 数据

### 实现

- [x] 世界级 `MeatscapeWorldData`：`schemaVersion`、World Stage 占位值。
- [x] Chunk 级 `MawCoherenceData`：数值、脏标记、序列化边界。
- [x] 服务端查询／设置 API 与最小网络同步 DTO。
- [x] `/meatscape coherence get|set` 和数据检查日志。
- [x] 实现一次模拟 v0 → v1 迁移。

### 测试

- [x] 新区块默认值。
- [x] 边界值钳制与非法数据恢复。
- [x] 保存、卸载、重载和服务器重启。
- [x] 模拟旧 schema 升级。
- [x] 两名玩家观察同一区块时同步一致。

### 退出条件

- [x] 不修改任何方块；上述测试全部通过，旧 schema 测试夹具可重复执行。

### 验证记录（2026-08-18）

- `./gradlew build --stacktrace`：通过；13 个 JUnit 测试执行成功。
- `./gradlew runGameTestServer --console=plain`：3/3 required tests passed；验证区块 Capability、世界 SavedData 和 dedicated server 加载／保存。
- 专服重启测试：在 `[0,0]` 设置 47%，正常保存关闭；重启后 `/meatscape coherence get` 仍返回 47%。
- 同步测试：服务端在 `ChunkWatchEvent.Watch` 时发送权威 DTO，变更时向所有 tracking players 广播；协议测试验证两个独立接收者解码得到完全相同的维度、区块和值。GameTest 的 mock connection 无 Netty channel，不能替代真实网络连接，因此未把 mock 发送失败计为产品缺陷。
- `./gradlew runClient --console=plain`：网络通道、客户端缓存和退出清理事件成功加载到主菜单。
- 代码审计未发现任何方块写入调用；Phase 1 只改变抽象数据。
- Netty 原生运行目录已固定为工作区 `run/natives`，不使用系统临时目录。

## Phase 2 — Spike 2：Rift 抽象数值源

### 实现

- [x] 无正式美术的测试 Rift。
- [x] Rift 唯一 ID、位置、半径、强度、生命周期和空间索引。
- [x] 仅影响抽象 Maw Coherence 的扩散计算。
- [x] `/meatscape rift create|remove|inspect` 与全局 pause。

### 测试

- [x] 单 Rift 扩散、多个 Rift 叠加和边界稳定性。
- [x] Chunk unload/load、服务器重启和 Rift 删除。
- [x] 未加载区块只更新抽象值。
- [x] 多人同步不泄漏全量世界数据。

### 退出条件

- [x] 运行数小时后数值稳定、无持续内存增长，重启前后结果在规定误差内一致。

### 验证记录（2026-08-18）

- `./gradlew build --stacktrace`：通过；JUnit 覆盖 schema v1 → v2、Rift NBT、空间索引、扩散叠加、边界、生命周期、未加载区块累计和 6 小时模拟。
- `./gradlew runGameTestServer`：4/4 required tests passed；Rift 扩散／删除测试使用独立 batch，服务器正常保存所有维度并关闭。
- 未加载区块不被强制加载；只在世界 `SavedData` 中保存维度／区块键和有界数值，区块加载后合并。
- 持久化回合在重启等价的 NBT 重载后保持 Rift、pause 和 pending coherence 精确一致（误差 0）。
- 客户端仅接收已有的“维度／区块／Coherence” DTO，不同步 Rift 索引或全量世界数据；双观察者协议测试仍通过。
- `./gradlew runClient`：Mod、资源和 OpenAL 成功加载到主菜单，随后人工终止开发实例；`libflite.so` 仍是已知可选旁白库提示。

## Phase 3 — Spike 3：空 Evolution Scheduler

### 实现

- [x] 全局与单 Rift tick budget。
- [x] 活跃区块队列、候选采样器和 chunk 生命周期处理。
- [x] 第一版只记录“本 tick 将处理的位置”，不替换方块。
- [x] 队列可重建；避免持久化大量瞬态位置。
- [x] `/meatscape debug stats` 输出处理量、队列长度、耗时和跳过原因。

### 测试

- [x] 大量 Rift 下预算不被突破。
- [x] 卸载区块不会保留强引用。
- [x] 暂停、恢复、重启和队列重建。
- [x] 玩家移动导致区块快速装卸时无重复风暴。

### 退出条件

- [x] 调度器能在测试负载下保持服务器 tick 可控；没有方块变化和不可回收引用。

### 验证记录（2026-08-19）

- 默认预算为全局 64 个候选位置／server tick、单 Rift 8 个，可在 common config 中调整。
- `./gradlew build runGameTestServer --stacktrace`：通过；30 个 JUnit 测试无失败，Forge GameTest 5/5 required tests passed。
- 压力测试为 10,000 个 Rift 任务连续运行 200 tick，每 tick 从未突破 64 的全局预算，队列长度保持稳定。
- 队列仅持有 Rift UUID、维度 ID 和打包的 chunk 坐标；`ChunkEvent.Unload` 立即移除对应任务，server stop 清空并移除调度器实例。
- 重启不序列化瞬态队列；任务规划器从持久化 Rift 与已加载区块确定性重建，并有独立测试。
- 10,000 次重复区块装载入队最终只保留 1 个任务；暂停不消费队列，恢复后继续轮转。
- GameTest 验证真实服务端能记录候选位置且不修改方块；代码审计未发现任何方块写入 API。
- `/meatscape debug stats` 输出上一 tick 处理量、累计处理量、队列长度、耗时与分类跳过计数。
- `./gradlew runClient`：新配置、事件订阅和命令注册成功加载到主菜单，随后人工终止开发实例。

## Phase 4 — Spike 4：Provenance 与安全转换

### 实现顺序

1. [x] `#meatscape:natural_replaceable` 与绝对保护 tags。
2. [x] Chunk terrain trust：Trusted、Player-Modified／Untrusted、Legacy／Unknown。
3. [x] 仅为候选材料记录紧凑 provenance。
4. [x] Base Anchor／Protected Settlement。
5. [x] 管理员 `protect`、`trust`、`inspect`。
6. [x] 少量 placeholder 转换方块和可移除 attachment fallback。
7. [x] 通用 `markUntrusted(region)` 接口；Create 实际接入放在独立测试配置。

### 必测场景

- [x] Stone、Logs、Dirt、Ice 玩家建筑。
- [x] 容器、机器和任意 BlockEntity。
- [x] The Bleeding 前建立的 Base Anchor。
- [x] 活塞移动和模拟批量移动落地。
- [x] Create contraption 的可选集成配置（通过无依赖的通用区域接口）。
- [x] 旧存档未知区块。
- [x] 管理员 trust／protect 的覆盖优先级。
- [x] 方块移除后的 provenance 清理。

### 退出条件

- [x] 受保护区域无主体破坏；可信自然地形能有限转换；未知区块正确回退为 attachment；存档增长经过记录并可接受。

### 验证记录（2026-08-19）

- Chunk Safety Capability 独立于 Maw Coherence；只有服务端首次生成区块经 `ChunkEvent.Load#isNewChunk()` 标为 Trusted，旧存档与缺少 schema 的区块保持 Unknown。
- provenance 仅保存自然候选位置的 section 位图，不记录玩家 UUID、时间戳或逐方块 CompoundTag；单位置序列化 fixture 小于 300 字符，删除后空 section 会被回收。
- 绝对保护（包括任意 BlockEntity）优先于管理员 trust；Protected Settlement 只允许放置可移除覆膜，不替换主体。
- Base Anchor 默认保护半径 32；保护区进入世界 schema v3 并有 v2 迁移和往返测试。
- 玩家放置／破坏、活塞移动和通用 `markUntrusted(min,max)` 统一写入 provenance；Create 仅提供可关闭的无依赖兼容入口，不向核心引入其类型。
- `./gradlew clean build --console=plain`：通过；37 个 JUnit 测试无失败，产出可重混淆 jar。
- `./gradlew runGameTestServer --console=plain`：8/8 required tests passed；覆盖可信自然转换、Stone／Logs／Dirt／Ice 玩家结构、BlockEntity、保护区、旧区块和模拟批量移动。
- `./gradlew runClient --console=plain`：注册表、方块模型与材质图集加载到主菜单后人工终止；`runServer` 正常进入 `Done` 并加载 schema v3。

## Phase 5 — Spike 5：Rollback／Severance 原型

### 实现

- [x] 与正向演化共享预算调度器。
- [x] 轻量来源类别与恢复映射。
- [x] 玩家后续建设检测。
- [x] `/meatscape rollback` 范围、速率和 dry-run 模式。

### 测试

- [x] 不能覆盖肉化后玩家放置的方块。
- [x] 不一次更新大量方块。
- [x] 中途停止、区块卸载和服务器重启后可安全继续。
- [x] 永久 Scar／Organ 内容不会被错误恢复。

### 退出条件

- [x] 正向转换和有限回退在同一测试世界中反复执行，存档与服务器保持稳定。

### 验证记录（2026-08-20）

- Chunk Safety schema v2 只为安全转换位置保存 `STONE／SOIL／WOOD／ICE／ATTACHMENT` 五种粗粒度来源；不保存完整 BlockState、BlockEntity NBT 或灾难前快照。schema v1 迁移不会虚构恢复历史。
- 世界 schema v4 保存 rollback job 的维度、边界、速率、dry-run、线性游标与标量统计；v3 迁移默认为空 job 集。重载夹具验证游标和统计可精确续跑。
- 正向演化与 rollback 在同一 server tick 中共享全局预算。有恢复任务时正向演化最多预留一半，恢复使用剩余额度；每个 job 还受自身 rate 限制。
- 遇到卸载区块时 job 保持当前游标并等待，不强制加载、不跳过；全局 pause 可中止处理，`rollback cancel` 可显式取消。
- rollback 仅在当前位置仍为对应 Meatscape 占位方块时执行。玩家后续放置／破坏会清理恢复记录，即使记录残留，方块身份不匹配也会判为 `PLAYER_OVERRIDE` 而不覆盖。
- `#meatscape:rollback_permanent`、任意 BlockEntity 与未来 Scar／Organ 绝对保护内容不会恢复；当前测试以 Base Anchor 验证永久内容优先级。
- `/meatscape rollback start <radius> <rate> [dryRun]`、`cancel <id>`、`status` 已实现；`debug stats` 同时显示恢复处理量、可恢复量、活跃 job 和等待区块状态。
- `./gradlew clean build runGameTestServer --console=plain`：通过；43 个 JUnit 测试无失败，11/11 required GameTest passed。
- GameTest 覆盖两轮正向转换／有限恢复、attachment 清除、dry-run、玩家覆盖、永久内容、单 job 速率和卸载区块等待。
- `./gradlew runClient --console=plain`：公共代码与资源加载至主菜单后人工终止；`runServer` 正常进入 `Done`、加载 schema v4，并通过 `stop` 正常保存关闭。

## 技术验证门

只有 Phase 0–5 全部通过，才开始正式内容生产：

1. **世界状态门**：Coherence、Rift、队列能持久化或安全重建。
2. **安全转换门**：四层保护契约全部通过自动与人工测试。
3. **迁移门**：至少一次真实旧 schema 升级测试通过。
4. **性能门**：有可重复基准、队列指标、MSPT 影响、内存趋势与存档增长记录。

## Phase 6 — Vertical Slice

范围严格限制为：

- [x] 1 个真实可扩散 Rift Core；方块生命周期创建／删除持久化抽象 Rift。
- [x] 4 档可观察 Coherence：`QUIET／EMERGING／ACTIVE／SATURATED`。
- [x] 10 个核心方块：Base Anchor、Changed Stone、Dermal Film、Dermal Soil、Ossified Stone、Vascular Mat、Nutrient Mound、Gestation Pod、Rift Core、Heart Pump。
- [x] 1 种 Maw Grazer 与 1 种 Immune Organism；均使用 Vanilla AI 加一个局部角色行为。
- [x] 1 个 Heart Pump；核心逻辑不依赖 Create／IE。
- [x] “Grazer Raw Tissue → Heart Pump Collagen → Living Poultice 再生收益”最短生物工业链。
- [x] 4 步 Advancement 引导 Rift → 采集 → 加工 → 明显收益。
- [x] 资源包级雾色／视距、粒子和环境声音；未引入 Shader。
- [x] 客户端主菜单、普通专服启停、服务端保存、GameTest 和既有多人同步／重启／区块重载回归通过。

Alpha／Beta 体验验收（2026-09-08 按项目所有者安排调整）：

- [ ] 模型、贴图和环境表现具备可评估条件后，由测试玩家验证收益与风险。保留、清除和融合的选择沿用原始 DOCX 第十九节三路线设计；个别玩家选择清除不代表验收失败。关注是否某条路线全面占优，使其他选择失去意义。
- 此项保持未验证，安排到 Alpha／Beta，不阻塞 Phase 7 技术开发；不把自动测试或占位材质运行当成体验通过。

### 验证记录（2026-08-28）

- JDK：OpenJDK 17.0.19；Gradle 依赖与构建缓存使用持久化 `~/.gradle`，Netty native 使用工作区 `run/natives`。
- `./gradlew clean build runGameTestServer --console=plain`：通过；44 个 JUnit 测试无失败，14/14 required GameTest passed，产出 151 KiB 单一 Core JAR。
- Phase 6 GameTest 覆盖 Rift Core 源生命周期、Heart Pump 加工和两种生态实体属性；旧 Phase 4/5 安全转换与 rollback 回归仍通过。
- 数据包加载 7 个自定义配方与 4 个自定义 Advancement，无解析错误；全部 JSON 经 `jq` 验证。
- `runClient`：实体 renderer、方块／物品模型、声音引擎和纹理图集成功加载至主菜单；未见 Meatscape 资源缺失。
- `runServer`：普通专服进入 `Done`，加载 world schema v4，通过 `stop` 完成全维度保存。
- 尚未勾选体验门：自动化测试不能证明玩家会自愿保留 Bleed Zone，需要人工 10–15 分钟游玩记录。

## Phase 7 — Core Alpha

按小闭环逐项实现，完整清单见 [Phase 7 详细计划](PHASE_7_CORE_ALPHA.md)。Phase 6 技术基线沿用；人工体验评估按上述安排进行。

- [x] 7.1 Dormant → Active Rift 与可恢复的首次返回触发技术闭环（2026-09-08）；真人演出验证待 Alpha；
- [x] 7.2 稀少 Dormant Rift 自然生成与 The Bleeding 演出技术闭环（2026-09-08）；真人表现验收待 Alpha；
- [x] 7.3 Cauterization 与 White Sanctuary 技术闭环（2026-09-09）；真人操作与平衡验收待 Alpha／Beta；
- [x] 7.4 营养膏与基础再生膜墙 Living Architecture 技术闭环（2026-09-09）；建筑手感、成本和平衡验收待 Alpha／Beta；
- [x] 7.5 Grazer／Immune 第一批 Overworld 生态技术闭环（2026-09-09）；正式模型、贴图与主观生态平衡待 Alpha／Beta；
- [~] 7.6 知识观察状态与条件函数已实现；研究可见反馈补齐见 7.6-R，真实玩家／多人验证待进行；
- [ ] 7.7 Core 独立专服长时间测试。

这一阶段仍不要求完整 The Maw、三结局或正式 Shader。

## Phase 8 — The Maw 与中后期系统

- [x] 8.0 The Maw 维度契约（2026-09-10）：注册键、生成高度、开发入口映射、安全落点、重生／死亡、schema v8 迁移与票释放边界已在 [ADR 0001](decisions/0001-the-maw-dimension-contract.md) 固定；尚未实现维度或传送。
- [~] 8.1 最小 The Maw 与安全往返（2026-09-10）：`meatscape:maw`、固定 Subdermal Expanse 占位群系、管理员首次绑定的 Maw Gateway、v7→v8 空集合迁移、持久化一对一同坐标链接、有界安全落点、短冷却与 `try/finally` portal ticket 释放已实现。JUnit 70 项和专服 GameTest 28 项通过；真实客户端／玩家专服／重启往返／死亡掉落／多人网络验证待进行。
- [~] 8.2 早期生存循环（2026-09-11）：Subdermal Expanse 的低密度 Nutrient Mound 提供可再生 Raw Tissue；Raw Tissue 可烹饪为 Prepared Tissue，Nutrient Paste 提供短暂 Maw Adaptation，未适应者承受 Maw Pressure，离开维度立即清除两种临时效果；Dermal Panel 为第一种基础建材。`test` 通过，专服 GameTest 29 项通过（含收获／再生）；真实 Maw 远征的资源密度、压力强度、离开效果与掉落／资源复制验证待进行。
- [~] 8.3 Burning Wound（2026-09-13）：稀有 Nether 生成的无物品入口、焦痕、独立 GatewayId、共享有界传送和短暂 Spatial Shear 已实现；成功通过时记录烧灼与 Maw 观察。`test` 与专服 GameTest 29 项通过；自然发现、进入／返回、重启、目标缺失、边界与真实危险体验待进行。
- [~] 8.4 End Wormhole（2026-09-13）：仅 Outer End 新生成区块中的稀有 End Stone 表面、无物品入口，独立 GatewayId 与既有有界安全往返已实现；成功通过时记录 Wormhole 与 Maw 观察，边界见 [ADR 0003](decisions/0003-end-wormhole-contract.md)。Java 17 `test` 通过，专服 GameTest 30 项通过（含无 BlockItem 契约）；真实发现、进入／返回、重启、边界与多人体验待进行。
- [~] 8.5 Vascular Canopy 场景（2026-09-13）：现有 Subdermal Expanse 中的低频、worldgen-only 静态 Vascular Mat 树冠已实现；单场景最多 27 方块、只写入生成中心 chunk、冲突时拒绝且无运行时扩散，边界见 [ADR 0004](decisions/0004-vascular-canopy-scene-contract.md)。Java 17 `test` 通过，专服 GameTest 31 项通过；真实新区块密度、导航可读性、卸载／重载、资源与正式视觉资产待进行。
- [~] 8.6 Hematic 基础（2026-09-13）：保留 Heart Pump ID 与旧加工行为，新增有界 Hematic 容量、定向 Artery 和耗液红石 Actuator；边界见 [ADR 0005](decisions/0005-hematic-foundation-contract.md)。Java 17 `test` 与专服 GameTest 32 项通过；真实拆接／重启、玩家布局、平衡和正式资产待进行。
- [~] 8.7 Living 网络（2026-09-14）：Artery 可向相邻 Regenerative Membrane 输送 Hematic，250 mB 精确转换为一格既有营养；Actuator 保持第二个可见耗液用途，边界见 [ADR 0006](decisions/0006-living-network-membrane-contract.md)。Java 17 `test` 与专服 GameTest 33 项通过；真实布局、循环、长时间卸载／重启、平衡和正式资产待进行。
- [~] 8.8 Enzymatic（2026-09-16）：Enzyme Vat 使用不消耗的 Collagen 催化 Raw Tissue 回收为 Nutrient Paste，保存有界输入／输出／进度并在背包满时掉落产物；边界见 [ADR 0007](decisions/0007-enzyme-vat-contract.md)。Java 17 `test` 与专服 GameTest 34 项通过；实际配方 reload、停机恢复、背包满领取、平衡与正式资产待进行。
- [~] 8.9 Compatibility（2026-09-16）：独立玩家 Compatibility 能力、由可逆 Maw Adaptation 逐步获得的适应路径，以及仅在外来方块上方生成的局部 Stoneblight 标记已实现；边界见 [ADR 0008](decisions/0008-compatibility-and-stoneblight-contract.md)。Java 17 `test` 与专服 GameTest 34 项通过；真实死亡／重连／维度切换、可读性、Immune 遭遇和多人体验待进行。
- [~] 8.10 Neural（2026-09-23）：Hematic 储量传感器、逐格衰减的神经纤维、输出红石的神经执行器及空手开关已实现；边界见 [ADR 0009](decisions/0009-neural-local-signal-contract.md)。Java 17 `test` 与专服 GameTest 36 项通过；真实玩家搭线、长线负载、卸载／重载、可读性与正式资产待进行。

- The Maw 最小可长期生存版本；
- Burning Wound 与 End Wormhole；
- 代表性 Maw 群系与生态 Archetype；
- Hematic、Enzymatic、Neural 技术扩展；
- Compatibility、Immune Response 与 Stoneblight 局部排异；
- End Revelation 与 Ancient Anchors。

每新增一个群系或生物都必须带资源、行为、生成规则、自动测试和专服验证，不批量生成未验证内容。

## Phase 9 — 可选集成

按以下顺序推进，均不得反向成为核心依赖：

1. Create 最小 Biological Pressure → Rotation 适配器；
2. Create 配方与 Ponder；
3. IE 重工业／流体桥梁；
4. TaCZ Gun Pack，必要时薄 Java 层；
5. Refurbished Furniture 与 Backpacked 的数据层适配；
6. 最终组合渲染与性能矩阵。

附属 Mod 应采用独立 Gradle 子项目或独立仓库，但共享版本目录和测试整合包。核心 JAR 不直接类加载外部 API。

## Phase 10 — Endgame 与发布准备

- Great Bleeding 的最终触发设计；
- Severance、Symbiosis、Convergence 的确认点与世界状态；
- 少量代表性 Interwoven Lands；
- 正式 Shader／许可审查；
- 综合压力测试、存档迁移、备份说明和服务器管理文档；
- 许可、第三方资产、发布清单和升级策略。

## 暂不排期事项

- 真正的双向 Stoneblight Spread；
- 多加载器支持；
- 每个群系的融合变体；
- 自研跨平台世界快照系统；
- 完整生态种群模拟；
- 自研大型 Blockbench AI 插件。

这些事项只能在核心版本稳定、性能预算明确并有单独立项后进入计划。
