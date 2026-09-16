# ADR 0008：Compatibility 与局部 Stoneblight 契约

状态：已接受（2026-09-16）

- Compatibility 是每名玩家独立保存的 `0..100` 数值能力。死亡／重生仅复制同一名玩家的值；维度切换、背包和其他玩家不会共享或重置它。
- Maw Adaptation 仍是可逆的短期适应途径：它存在时，玩家每 200 tick 在 The Maw 获得至多一点 Compatibility；离开 The Maw 时既有规则移除该效果和 Maw Pressure。Compatibility 不作为入口、资源或任务硬门。
- Stoneblight 只在玩家于 The Maw 放置 Stone、Dirt 或 Oak Planks 时，在其正上方的空位生成一格 Dermal Film。它不替换放置物、不读取远方区块、不传播、不保存第二套世界感染状态。
- 现有 Immune Organism 的限制范围和暂停行为继续承担局部 Immune Response；8.9 不将其扩展为基地识别、全局仇恨或机器破坏。

## 验证

- 自动验证 Compatibility 上限、序列化和玩家间隔离，以及普通专服 Capability 加载。
- 实际食用适应物、死亡／重连／维度切换、Stoneblight 可读性、Immune 遭遇强度和多人体验保留 `[~]`。
