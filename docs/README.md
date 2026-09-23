# BLEEDTHROUGH 设计文档

本目录是总体设计的工程化拆分版。原始 DOCX 仍是完整创意来源；Markdown 文档负责明确实现边界、模块归属和阶段验收。若两者冲突，以本目录中标记为“冻结”的近期工程决策为准，并通过设计决策记录修订，而不是静默偏离。

## 文档地图

- [01 — 产品愿景与设计支柱](design/01-product-vision.md)
- [02 — 世界、剧情与玩家进程](design/02-world-and-progression.md)
- [03 — Maw Coherence 与存档安全](design/03-coherence-and-safety.md)
- [04 — 内容、生态与生物工业](design/04-content-and-gameplay.md)
- [05 — 技术架构与模块边界](design/05-technical-architecture.md)
- [06 — 外部集成、资产与性能](design/06-integrations-assets-performance.md)
- [实施计划](IMPLEMENTATION_PLAN.md)
- [Phase 7 Core Alpha 分项计划](PHASE_7_CORE_ALPHA.md)
- [后续路线：Phase 7 收尾与 Phase 8–10](ROADMAP_REMAINING.md)
- [Phase 8 技术验收与待测项](PHASE_8_ACCEPTANCE.md)
- [ADR 0001 — The Maw 维度契约](decisions/0001-the-maw-dimension-contract.md)
- [ADR 0010 — End Revelation 与局部 Ancient Anchor](decisions/0010-end-revelation-local-anchor-contract.md)
- [原始总体设计 DOCX](../../BLEEDTHROUGH%20%E2%80%94%20Minecraft%20%E6%95%B4%E5%90%88%E5%8C%85%E6%80%BB%E4%BD%93%E8%AE%BE%E8%AE%A1.docx)

## 当前冻结基线

- Minecraft 1.20.1
- Forge 47.4.22
- Java 17
- 单一 `Meatscape Core` 发布 JAR，代码内部强模块化
- Create、Immersive Engineering、TaCZ 等保持可选，不得成为核心存档依赖
- 唯一运行时区块指标为 `Maw Coherence`
- 禁止逐方块随机感染；已有区块由有预算的演化调度器处理
- Phase 8 规划内的最小系统已实现，真实玩家、长测与正式资产验收见 Phase 8 报告；完整 The Maw、Boss、正式 Shader 和深度兼容仍属后续内容

## 维护规则

1. 总体愿景与当前实现范围分开维护。
2. 新功能必须归属某个阶段，并有明确验收标准。
3. 世界状态、存档格式或安全契约的变更必须记录迁移方案。
4. 未通过技术验证门前，不扩充大型群系、生物、科技树或终局内容。
5. 原始 DOCX 保留，不在拆分过程中删除或覆盖。
