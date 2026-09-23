# Phase 7 — Core Alpha 实施与验收

## 基线与决策

- Minecraft 1.20.1、Forge 47.4.22、JDK 17、单一 Core JAR；缓存继续使用持久化用户目录／工作区。
- 设计依据：原始 DOCX 第六、七、八、十三、十四、十七、十九节及 `design/02`、`03`、`04`、`05`、`06`。
- 2026-09-08：项目所有者明确将体验平衡评估安排到模型／贴图具备后的 Alpha／Beta，不阻塞 Phase 7。三路线自由选择是原有设计，不是新增方向。DOCX 保留原件，工程验收安排以本记录为准。
- Git 基线：PR #7 合入 Phase 5 分支后，main 尚未包含 Phase 6。Phase 7 分支将 `origin/main` 与 `b667409` 合并，包含 Phase 6 的 `46416d1`；不直接更改远端 main。
- 每个子阶段独立验证并标记，不能把完成 7.1 写成整个 Phase 7 完成。

## 7.1 休眠源与首次触发闭环

### 实现

- [x] Rift 增加持久化 `Active`；休眠源不累加 Coherence、不生成演化任务，已有任务也检查活跃状态。
- [x] Rift Core 的 `active=false/true` 状态对应休眠／活跃；默认活跃以兼容 Phase 6，普通物品放置仍为活跃。休眠调试放置使用 `/setblock ~8 ~ ~ meatscape:rift_core[active=false]`。
- [x] 第一次 Nether → Overworld 返回只登记一个世界级事件；其他维度返回不触发，多人重复返回不重置。
- [x] 默认延迟 600 个有附近观察者的服务端 tick；全局 pause、关闭配置、附近无人均冻结倒计时。事件原点和剩余 tick 写入世界存档。
- [x] 延迟后激活原点水平 512 格内最近的未过期休眠源；无候选则等待，不强制加载、不创建或替换地形。只激活一个，世界推进到 BLEEDING；不自动推进更高 Stage。
- [x] 首个反馈使用本地化标题、原版低频心跳和方块发光变化；这不等于完整正式演出或美术验收。
- [x] `/meatscape bleeding status|schedule` 提供管理员诊断与一次性调试入口；不提供重置世界进度的隐式操作。
- [x] 新建 Core 的半径使用 96 格（6 chunk），修正旧常量把 chunk 数当成格数的问题。旧存档记录的半径不改写。

### 存档契约

世界 schema v4 → v5：增加可选 `BleedingOrigin`／`BleedingDelay`；已有 `Rifts` 条目增加 `Active`。缺失 `Active` 的历史源默认为活跃，缺失事件状态不虚构历史返回。原有 Stage、暂停、保护区、回退任务和 pending coherence 保持原义。休眠并不清除此前已经累计的 Coherence。

### 验收

- [x] JUnit：旧 schema、休眠无贡献／无任务、维度过滤、多人幂等、暂停／重载续跑、删除／过期／距离／维度筛选。
- [x] Forge GameTest：方块状态与来源一致、激活后实际扩散、区块角落有效、移除清理、调度重建。
- [x] JDK 17 clean build 与 Phase 0–6 全量自动回归。
- [x] 客户端资源加载和专服正常启停。
- [ ] 真人下界往返、标题／声音、远离／返回和真实多人网络演出验收，留作 Alpha 场景；主菜单启动不能代替这些验证。

7.1 交付时的边界：当时尚无自然生成，测试须先放置休眠 Core。自然发现、无候选时的处理方案、完整演出归下述 7.2。

### 自动验证记录（2026-09-08）

- `clean build runGameTestServer`：最终通过，49 个 JUnit、16/16 required GameTest；Core JAR 约 165 KiB。
- 重启验证曾发现加载回调中查询同一区块导致等待；线程栈确认后改为正常 tick 内 `getChunkNow` 非阻塞同步。同一开发 GameTest 存档再次启动、执行全部测试并保存退出通过。
- 旧 Core 半径测试更新为 96 格，并增加 `finally` 清理，避免失败的测试源影响后续场景。
- 全部资源 JSON 解析通过；客户端完成纹理图集、声音引擎与新增 blockstate 资源初始化，随后终止开发客户端。未将其记作入世界演出／美术验收。
- 普通专服 `runServer` 进入 `Done`，加载 schema v5；`meatscape bleeding status` 返回 DORMANT、无待触发事件。`stop` 后全维度保存，Gradle 正常退出。
- 原始本地日志：`run/phase7-verification.log`、`run/phase7-client.log`；`run` 为忽略的开发输出目录，不将大型运行日志提交到 Git。

## 7.2 自然前奏与完整演出

- [x] 新区块管线稀少生成休眠 Rift，Overworld 限定、数据驱动分布；不通过区块加载回调写入历史建筑。
- [x] 返回点附近没有候选时的明确策略，兼顾旧存档与探索，不强制加载大片区块。
- [x] 按设计顺序实现克制的变暗／动物／指南针异常与低频声音，限定时长／范围，结束后恢复正常表现。
- [x] 自动验证：生成／运行期隔离、存档迁移与演出恢复、客户端清理、同步 DTO、动物状态保留。
- [x] 最终构建、客户端加载、普通专服启停。
- [ ] Alpha 人工验收：真实下界往返、多人同时观察、断线／重连／切维度中的实际声音和画面；不以 DTO 单测或主菜单加载代替此项。

### 7.2 实现边界与默认参数

- 自然生成：`dormant_rift` Configured／Placed Feature + Forge biome modifier，仅 Overworld 新生成的中心 chunk；默认每 512 个候选 chunk 一次尝试，表面需满足 `#meatscape:dormant_rift_substrate`，目标必须为空气。`#meatscape:dormant_rift_biomes` 默认为 Overworld 标签。海面、树冠等无合适基底的位置会跳过，不能将尝试概率理解为保证生成率。
- Feature 仅写生成中的 ProtoChunk，拒绝运行期 `ServerLevel` 和已完成 chunk。它留下可序列化的一次性 scheduled block tick，在正常服务端 ticking 后注册 Rift；不从生成工作线程访问 SavedData，不扫描历史区块寻找或补写 Core。已生成但未开始 ticking 的 Core 会在以后 ticking 时注册。
- 无候选策略：保留 pending 事件。原点 512 格内没有休眠源时，每秒检查 Overworld 非旁观玩家周围的现有源，找到后将待触发事件衔接到该玩家所在地，保留剩余倒计时；演出开始后不迁移原点。旧存档可通过探索新区块自然推进，也可继续用管理员调试放置。不会在旧基地旁强行生成裂隙。
- 时间线：原有默认 600 个观察 tick 延迟后，进行 120 tick 前奏；前 40 tick 有缓慢、最高 16% 的屏幕亮度压暗与指南针扰动，动物短暂停止移动目标；第 40 tick 一次低频心跳；随后恢复安静，到 120 tick 后激活最近候选并显示本地化标题。候选被移除时不消费世界阶段，恢复等待，可再次衔接探索。
- 表现范围：非旁观玩家距事件原点 128 格内；动物为原点 32 格内。没有本地观察者、全局 pause 或关闭事件配置时不推进。动物通过临时 Goal 控制，不写 NoAI／NBT，不冻结物理或生命状态；指南针包装原有角度计算，不改物品 NBT；压暗不修改 gamma、世界光源或天气。声音复用原版资源，美术与最终氛围仍待人工验收。
- 世界 schema v5 → v6 新增 `PreludeElapsed`（-1 表示尚未开始，0–120 为进度）。旧 pending 事件从前奏开始前继续；重启保留剩余演出，不重置倒计时。网络协议从 1 升为 2，新旧客户端应一起更新；客户端只收到有界演出时间，不接收 Rift 索引或坐标。
- 客户端每 5 tick 接收快照、15 tick 无刷新自动清除；离开世界／断线清除。暂停后的同步恢复不会因效果租期过期重复播放心跳。真实网络与主观氛围测试仍列为人工项。

### 7.2 验证记录（2026-09-08）

- JDK 17 `clean build runGameTestServer`：54 个 JUnit、19/19 required Forge GameTest 全部通过。
- 使用加载的数据包 Configured Feature 对独立 ProtoChunk 验证休眠放置、scheduled tick、重复放置拒绝、不触碰世界数据及不强制加载；验证 Plains biome 已接入 Placed Feature。运行期 ServerLevel 拒绝同一 Feature，历史方块保持原样。
- 演出测试覆盖 v5 pending 迁移、进行到第 55 tick 后重载续跑、暂停、删除源、探索迁移约束、客户端租期／清理、重复快照和音效去重、两接收者 DTO 一致性及非法时间钳制。
- 动物 GameTest 验证重复 join 只有一个 Goal，无观察事件时不启动；启动／结束不改变已有 NoAI 标志。自动验证不等同于真人观察到动物停顿。
- 资源 JSON 全量解析及 `git diff --check` 通过；构建依赖使用持久化 `~/.gradle`，日志保留在工作区 `run/phase7-2-verification.log`。
- 客户端完成资源、纹理图集与声音引擎初始化后手动结束；未发现 Meatscape 加载错误。开发环境已有的 narrator／Realms 提示不作为本阶段玩法验收；未进行入世界或真人多人表现验收。日志：`run/phase7-2-client.log`。
- 普通专服加载 schema v6 并进入 `Done`，`meatscape bleeding status` 返回 DORMANT、无 pending 事件；`stop` 后全部维度保存，Gradle 正常退出。日志：`run/phase7-2-server.log`。

## 7.3 烧灼与 White Sanctuary

- Git 基线（2026-09-09）：从已合并 PR #9 的 main `cfc499b` 开始实现。

- [x] 可主动进行的烧灼实验、抑制效果与明确消耗；不把烧灼等同于世界快照恢复。
- [x] `#meatscape:white_sanctuary_biomes` 驱动极寒规则，精选默认群系；定义跨群系 chunk 的采样／边界。
- [x] 同步约束已加载与未加载抽象 Coherence，组织冻结与工业效率下降；避免卸载后绕过上限。
- [x] 来源恢复、玩家后建内容、永久 Scar 和保护区回归。
- [x] 客户端资源加载、普通专服启停与 schema v7 保存。
- [ ] Alpha／Beta 人工验收：烧灼手感、极寒代价、多人实际交互及正式美术。

### 7.3 自动验证记录（2026-09-09）

- JDK 17 `clean build runGameTestServer` 最终通过：59 个 JUnit、23/23 required Forge GameTest，包含 Phase 0–7.2 回归。资源 JSON 全量解析和 `git diff --check` 通过。
- 新增 JUnit 覆盖混合群系上限、v6 → v7 无历史迁移、biome ID 往返与可重新解释的标签成员、抑制保存／暂停／到期／刷新、未加载 pending 钳制。
- 新增 GameTest 覆盖实际 chunk 的冷／暖样本、冷区上限、pending 加载合并、未知区块 BiomeSource 查询不强制加载、未加载抑制、冷列阻止演化且保留安全恢复路径、8 → 1 冷区加工与输入不足、打火石耐久、交互 DENY、玩家修改、BlockEntity、保护区与可移除覆膜、永久焦痕回退拒绝。
- 初次全量回归发现烧灼测试的抑制状态污染后续共用 chunk；已用 `finally` 清理，重新全量执行通过。复查修正冷区慢速累积的时钟对齐，避免重启后世界时间与 server tick 偏移导致永久不累积。
- 原始构建／测试日志保留在忽略目录 `run/phase7-3-verification.log`；依赖继续使用持久化 `~/.gradle`，不提交大型运行日志。
- 客户端完成 Mod、纹理图集与 OpenAL 声音引擎初始化，无 Meatscape 资源缺失记录；随后手动终止开发实例。已知可选 flite 旁白库、原版音效／shader sampler 和开发 Realms 提示不计作本 Mod 错误。未将加载通过记为入世界美术／多人交互验收。日志：`run/phase7-3-client.log`。
- 普通专服进入 `Done`，加载 schema v7、DORMANT；`meatscape inspect` 输出当前暖区 cap=100、frozenColumn=false、suppressionTicks=0。执行 `reload` 后再次查询正常，`stop` 完成所有维度保存并正常退出。日志：`run/phase7-3-server.log`；最终 Core JAR 约 209 KiB。

### 7.3 实现边界与初始平衡值

- 实验入口：潜行持打火石右击 `#meatscape:cauterizable` 中、有轻量来源记录且仍为预期演化方块的组织。成功消耗 1 耐久，当前 chunk Coherence 最多下降 10，抑制 1200 个服务端 tick（正常 TPS 下 60 秒）；重复成功实验只刷新时长，不无限叠加。全局 pause 时拒绝实验并冻结抑制计时；区块卸载仍消耗计时，服务器停止不消耗。
- 烧灼固体自然组织留下 `charred_scar`；可移除覆膜只清为空气，不在建筑旁留下实体障碍。焦痕加入绝对保护／永久回退标签，即使残留来源记录也不自动恢复；玩家仍可手动挖除。烧灼不关闭 Rift、不推进终局、不启动区域 rollback。抑制结束后活跃 Rift 可再次扩张，玩家仍可选择保留或抵抗。
- 不处理无来源组织、玩家后放内容、BlockEntity、Rift／Organ Core、受保护建筑主体；遵守已取消或 DENY 的交互事件、旁观／冒险限制和原版出生点保护。失败的组织烧灼不扣耐久，也不继续触发普通点火。正常非潜行点火仍为原版行为。
- 新焦痕方块具有掉落、物品模型及「生组织 + 熔炉燃料 → 焦痕」烧炼配方；烧炼／放置焦痕不提供世界抑制，以免形成无来源的免费抑制。当前模型复用原版玄武岩纹理，不能视为正式美术验收。
- White Sanctuary 默认群系为 Frozen Ocean、Deep Frozen Ocean、Ice Spikes、Frozen Peaks；不默认加入 Snowy Plains。通过 `#meatscape:white_sanctuary_biomes` 扩展，不使用纬度或是否下雪的泛化判断。
- 采样契约：Overworld 每 chunk 在固定 Y=64 的 4×4 quart-biome 网格取 16 个样本，每个样本代表对应的 4×4 水平列，规则作用于整列高度。抽象上限为 `100 - floor(85 × 极寒样本数 / 16)`：全暖 100、半冷 58、全冷 15；局部冻结则按对应列判断，避免混合 chunk 的暖侧也被全部冻结。固定高度是本阶段明确的近似，不是地表高度扫描或三维气候模拟。
- 冷列阻止正向转换及跨边界附着生长，现有组织保持原位、原恢复来源不变；已有恢复调度仍可有限恢复非永久内容。全冷 chunk 的 Rift 累积最多每 200 服务端 tick 增加 1 点，且不能超过 15；节拍使用与字段更新一致的服务端时钟，重启重新对齐。不生成巨型 Frozen Scar／White Wall 地标，也不批量改写既有组织外观。
- 未加载区域只处理抽象值。已观察的相关 chunk 保存 16 个 biome ID，不保存 Holder／Chunk 引用；卸载前刷新已记录样本。没有历史样本时查询生成器 BiomeSource，不强制加载或生成地形；加载后实际 chunk 样本优先并再次钳制。标签成员不写死到存档，数据包重新定义标签后下一次查询即按新规则判断；未知 biome ID 按非极寒处理。管理员改写既有群系但尚无样本的卸载区块，首次加载时校正。
- Heart Pump 冷列配方为 8 生组织 → 1 胶原，暖列仍为 2 → 1；这里以材料产率 25% 表达初期低效率，不新增机器计时器或改变注册 ID。输入不足不消耗；本阶段没有新增 Living Architecture 组件，其营养与冻结规则留给 7.4。
- 世界 schema v6 → v7 增加可选 ThermalProfiles 和 Suppression；旧存档不虚构抑制历史。网络仍使用既有权威 Coherence DTO（协议 2），不发送全量 biome／抑制索引。`/meatscape inspect` 显示上限、当前列冻结状态和剩余抑制 tick。
- 实际操作手感、极寒工业代价与正式焦痕／冻结美术留待 Alpha／Beta；本阶段自动测试和客户端加载不能替代真人评估。Nether 自然烧灼地貌与 Burning Wound 仍属 Phase 8。

## 7.4 资源与 Living Architecture

- Git 基线（2026-09-09）：从已合并 PR #10 的 main `ec86be9` 开始实现。

- [x] 从现有组织／胶原链扩展一个可用建筑组件与必要资源，记录输入、输出、成本和作用范围。
- [x] 营养依赖、冻结效率、保存／卸载和自动测试；核心不依赖 Create／IE。
- [x] 保留现有注册 ID；新增内容具备配方、掉落和可加载模型。
- [x] 最终 JDK 17 全量回归、客户端资源加载和普通专服启停。
- [ ] Alpha／Beta 人工验收：建筑手感、爆炸强度平衡、营养成本、红石读数可读性与正式美术。

### 7.4 实现边界与默认参数

- 最小组件为 `regenerative_membrane`（再生真皮膜墙），是玩家主动建造的 Living Architecture，不参与自然扩散。完整膜墙承受一次爆炸后设置持久化 `wounded=true`；尚未修复又被爆炸命中则按普通方块破坏，避免形成无营养的永久防爆墙。玩家正常挖掘仍可移除。每块膜墙独立保存最多 4 份营养，比较器以 0／3／7／11／15 输出储量。
- 营养材料为 `nutrient_paste`：2 生组织 + 1 骨粉合成 2 份。膜墙配方为 6 胶原 + 2 营养膏合成 4 块。配方及喂养使用 `#forge:collagen`、`#forge:nutrient_pastes`，允许数据包提供材料等价物；核心没有 Create／IE 类型或必需依赖。
- 潜行与否不影响喂养：手持营养膏右击向单块膜墙加入 1 份，容量已满时不消耗。受伤且储有营养时，在已加载、未全局暂停、非 White Sanctuary 冻结列中累计 200 个服务端 tick；完成后恰好消耗 1 份并恢复外观。无营养、pause 与极寒均冻结进度而非清零或消耗。
- 区块卸载时没有全局任务、票或强引用，愈合不会离线推进；BlockEntity 的 `DataVersion=1`、`Nutrition`、`HealProgress` 在重载后精确续跑，非法 NBT 被钳制。它不需要世界 schema v8，也不改变网络协议；外观完全由同步的原版 BlockState 表达。
- 膜墙作为 BlockEntity／Living component 同时位于绝对保护与永久回退标签，不会被正向演化、烧灼或 rollback 误改；这不妨碍玩家主动挖除。没有营养网络、跨块共享库存、方块扫描或每块随机 tick；血管运输等网络组件留给后续独立闭环。
- 当前完整／受伤模型分别复用已有真皮土／异化石贴图，营养膏复用原版岩浆膏图标；这是可加载的占位表现，不标记为正式美术完成。

### 7.4 自动验证记录（2026-09-09）

- 首次服务端启动暴露 BlockEntity DeferredRegister 被重复挂载，Forge 注册表拒绝启动；删除重复注册后重新执行，不能将 Gradle 外层曾错误显示的成功记作 GameTest 通过。
- JDK 17 `clean build runGameTestServer`：59 个既有 JUnit 与 25/25 required GameTest 通过。新增测试覆盖容量／材料消耗、比较器满值、真实爆炸第一次受伤／第二次破坏、pause 和极寒休眠、73 tick 保存后续跑、单次营养消耗、恶意 NBT 钳制、演化／回退保护以及玩家主动拆除。资源 JSON 全量解析及 `git diff --check` 通过；最终 Core JAR 约 225 KiB。
- 客户端完成 Mod、BlockEntity 注册、纹理图集和声音引擎初始化，无 Meatscape 资源缺失记录；随后手动终止开发实例。仍有既知 flite／原版声音／shader sampler／开发 Realms 提示，不计作本 Mod 错误。该检查不等于入世界模型和建筑体验验收。日志：`run/phase7-4-client.log`。
- 最终代码的普通专服加载 schema v7、DORMANT 并进入 `Done`；测试放置 wounded 膜墙后，`data get block` 在数据包 `reload` 前后均返回 `DataVersion=1`、Nutrition=0、HealProgress=0。7 个配方正常加载，`stop` 后全维度保存且 Gradle 正常退出。日志：`run/phase7-4-server.log`。

## 7.5 Overworld 生态

- Git 基线（2026-09-09）：从已合并 PR #11 的 main `07476f3` 开始实现。

- [x] 完善已有 Grazer／Immune 的栖息地与招牌行为，逐个验证；未批量新增生物。
- [x] 受预算和局部密度约束的生成，难度／暂停／卸载行为明确。
- [x] 正式资产与玩法代码分开记录验收；占位贴图未标作正式美术完成。
- [ ] Alpha／Beta 人工验收：两种生物的正式模型／贴图／动画、生成频率、Grazer 摄食可读性、Immune 驱赶压力与多人体验。

### 7.5 实现边界与默认参数

- 两种既有实体只在 Overworld、世界阶段至少为 `BLEEDING`、附近区块已加载且候选列不属于 White Sanctuary 时由服务端生态循环尝试生成；不会为寻找栖息地加载或生成区块。Biome 与基底分别通过 `#meatscape:maw_grazer_habitats`、`#meatscape:immune_organism_habitats`、`#meatscape:grazer_food`、`#meatscape:immune_habitat` 扩展。默认两个 biome tag 接受 Overworld；Grazer 基底为 Nutrient Mound／Dermal Soil，Immune 基底为 Vascular Mat／Ossified Stone。
- 每 200 个服务端 tick 执行一次，每个维度周期最多 4 次候选尝试；旁观玩家不提供候选。候选距玩家水平轴向 12–24 格，且同一周期不重复采样同一 chunk。32 格局部范围内 Grazer 上限 3、Immune 上限 1；Grazer 从 40 Coherence 开始出现，Immune 从 60 开始出现。
- 和平难度不生成 Immune，已有 Immune 清除攻击目标但不被强制删除；其他难度下它只攻击 12 格内非创造、非旁观玩家。自然生成的 Immune 以生成点为 16 格限制中心，表达局部排异而不是理解整座基地或维护全局免疫网络。
- Grazer 保留原版游荡、恐慌、繁殖和胶原引诱，并增加 8 格内寻找 `#meatscape:grazer_food` 的摄食行为；连续进食 40 tick 恢复 4 点生命并进入 600 tick 冷却，不删除或改写栖息地方块。全局 pause 冻结新生成、摄食／冷却与 Immune 攻击目标；普通游荡与物理仍由 Vanilla 管理。
- 生态循环不保存 Level、Chunk、Player 或 Entity 引用，也不创建 chunk ticket。区块卸载后不执行生成或离线生态模拟；实体本身继续使用 Vanilla 区块序列化／卸载生命周期。本阶段不改变世界 schema 或网络协议。
- Grazer 继续复用原版牛模型／贴图，Immune 继续复用原版僵尸模型／贴图，均只是可运行占位表现。客户端检查不构成正式模型、动画、轮廓、恐怖感或生成平衡验收。

### 7.5 自动验证记录（2026-09-09）

- JDK 17 `clean build runGameTestServer` 最终通过：62 个 JUnit、27/27 required Forge GameTest；Core JAR 约 234 KiB。新增纯策略测试覆盖维度、阶段、Coherence、极寒、pause、难度、密度与固定尝试预算，并审计运行事件类不持有跨 tick 世界／实体引用。
- GameTest 使用真实数据包标签和实体注册验证 Grazer 在有效基底生成并恰好停在局部上限；分别验证 Grazer 摄食恢复／冷却、pause 冻结，以及 Immune 在 pause 时清除目标。第一次测试暴露以测试结构 heightmap 定位无法稳定指向手工基底，随后把生产 heightmap 入口与确定性精确位置测试入口分离；最终全量回归通过。
- 全部资源 JSON 解析和 `git diff --check` 通过。构建依赖继续使用持久化 `~/.gradle`，原始验证日志保存在忽略目录 `run/phase7-5-verification.log`。
- 客户端完成实体 renderer、纹理图集与声音引擎初始化后手动结束；复测同时修复 7.4 再生膜墙占位模型错误引用不存在的 Meatscape 贴图，之后无 Meatscape 缺失纹理记录。原版音效／shader sampler／Realms 开发提示不计作本 Mod 错误。
- 普通专服进入 `Done`，加载 schema v7；数据包 `reload` 后 7 个配方与标签正常重载，`stop` 后全维度保存并正常退出。自动测试和主菜单加载未替代正式资产与真人生态体验验收。

## 7.6 知识驱动研究

2026-09-10 规划复核：以下已完成项限于观察数据与条件函数；代码尚未把研究条件接入玩家可见记录。新增 [7.6-R 收尾任务](ROADMAP_REMAINING.md)，完成前不宣称完整研究体验已交付。

- Git 基线（2026-09-10）：从已合并 PR #12 的 main `7ebed67` 开始实现。

- [x] 独立观察状态支持先发现、后研究的乱序流程；不做新的完整 Quest UI。
- [x] Rift／烧灼／极寒／工业观察形成最小研究链，保留和清除均可继续。
- [x] 自动化验证：持久化格式、未知字段恢复、Clone 数据复制与独立玩家隔离，以及全量既有 GameTest 回归。
- [~] 真实玩家验证待进行：死亡／重连／数据包重载后的实际存档流程、真实多人知识隔离与观察反馈；当前环境无法执行，不得视为已通过。

## 7.7 长时间验证

2026-09-10：PR #13、#14 已合并至 main `2ef4017`。进一步拆为 [7.7-A 遥测修整、7.7-B 无人长测、7.7-C 真人验收](ROADMAP_REMAINING.md)。下列原规程保留为混合玩家场景；没有真人不阻止执行无人专服可覆盖的部分。低频瞬时 CSV 本身不能证明所有 tick 都未超预算。

- Git 基线（2026-09-10）：从 7.6 Draft PR #13 的提交 `30ccbf7` 开始；合并后以 main 的对应 merge commit 复核。

- [x] 可复用遥测基础：显式开启 `diagnostics.soakTelemetryEnabled=true` 后，专服默认每 1200 tick 将有界窗口 CSV 追加至该存档的 `data/meatscape-soak.csv`；默认关闭。2026-09-23 字段与后台扫描、轮转语义见 [Phase 7 技术债记录](PHASE_7_TECH_DEBT.md)，旧版瞬时字段不再是当前格式。
- [~] 真实独立专服数小时 soak 场景待执行：活跃／休眠 Rift、生态和工业并存，暂停、重载、重启与回退交替。当前没有可连接的真实观察者，不能伪称生态与多人路径已验证。
- [~] 原始遥测记录待产生：模拟 tick 压测和 GameTest 回归不代替真实数小时运行。

### 7.7 执行规程

1. 使用新的、可保留的专服存档，启用遥测并保留 `data/meatscape-soak.csv`、服务器日志和存档目录大小；不得将日志或依赖写入系统临时目录。
2. 由 OP 在已加载区域准备一个 active Rift，并保留一个未激活／休眠 Rift；放置 Heart Pump 与已喂养的受伤再生膜墙。至少一名真实观察者保持在生态测试区域，才可覆盖生态循环。
3. 连续运行至少 3 小时。每 30 分钟记录 `/meatscape debug stats`、`/meatscape rollback status` 和 CSV；依次执行一次 pause/resume、一次数据包 `reload`、一次正常 stop/restart，并在已加载的安全转换区域运行一次有界 rollback。
4. 验收时检查 CSV 中 MSPT、堆和存档字节数是否存在持续无界增长；检查队列与每 tick 处理量未超预算；重启后确认 Rift、膜墙、工业和 rollback 状态可继续。保留原始记录，异常必须附带复现步骤，不以压缩 tick 测试替代。
- [ ] Core Alpha 发布前汇总技术缺口和人工验收项；The Maw、三终局实现和正式 Shader 不属于本 Phase。
