# Phase 8 技术验收与待测项

更新：2026-09-23。范围按 [路线表](ROADMAP_REMAINING.md) 的 8.0–8.12 最小交付，不把远期群系、生物、黑洞视觉或终局视为本阶段已完成。

## 技术闭环

| 环节 | 已有实现与自动证据 | 尚需真实验证 |
|---|---|---|
| 往返远征 | Maw 维度、安全落点和持久链接；Nether Burning Wound 与 Outer End Wormhole 复用传送服务；既有 JUnit／GameTest 回归 | 真人双向往返、边界与目标缺失、死亡／重连、多人和 ticket 长期释放 |
| 补给与居住 | Nutrient Mound、Raw／Prepared Tissue、Nutrient Paste、Dermal Panel、再生膜墙；专服场景测试 | 资源密度、压力、建筑手感和正式美术 |
| 工业与控制 | Hematic 来源／输送／执行、Enzyme Vat、Neural 传感／传输／执行；专服测试含实际收获组织→Heart Pump 胶原→Enzyme Vat 营养膏→适应效果的一条连续链 | 大规模网络、长时间卸载／重载、平衡和材质 |
| 适应与生态 | 独立 Compatibility、Maw Adaptation、Immune、Stoneblight 和 Vascular Canopy；自动测试 | 真人可读性、生态密度、多人状态与实际遭遇 |
| 研究与 End | Wormhole 观察、End Stone／Chorus／Ancient Anchor 独立观察及可见进度；旧知识加载、数据包与布局测试 | 自然发现率、旧 End 世界重启、多人知识隔离与文本体验 |

## 存档与迁移

- 世界数据通过既有 schema v8 迁移测试；旧世界不扫描补种 End 设施。
- Hematic、Enzyme Vat、Compatibility 现在在保存时写入 `DataVersion=1`；旧无版本标量仍按原键读取并夹紧。再生膜墙及玩家知识已有版本字段。新增 Anchor／Neural 状态由原版 BlockState 保存，不新增自定义 NBT。
- 本次单元测试验证旧 Compatibility 与知识数据；专服 GameTest 验证旧 Hematic／Vat 标量及夹紧。真实长期存档升级仍需备份后的实机验证。

## 发布门

- 自动验证：Java 17 `test build runGameTestServer` 通过，77 项 JUnit、39/39 项 Core-only 专服 GameTest；日志保存在工作区 `run/phase8-completion-gametest.log`（该目录不提交）。
- 客户端启动烟测：`runClient` 已进入资源重载、纹理图集与声音引擎初始化，日志未见 Meatscape 模型／贴图缺失；限时 90 秒后退出。实际打开存档、交互与视觉检查尚未完成。
- 至少双客户端同步、代表硬件性能、至少三小时带负载专服长测、正式资产与真人体验：待执行。不能将本报告解释为 Phase 8 或发布验收全部通过。
- Phase 7 的真人与长测发布门仍独立存在；Phase 8 技术实现不会自动关闭这些待办。
