# ADR 0003：End Wormhole 自然入口契约

状态：已接受（2026-09-13）

## 决定

- 注册名固定为 `meatscape:end_wormhole`。它仅由 End 世界生成的稀有 feature 放置，且只接受正在生成的 Outer End chunk；中心岛半径一千格以内一律拒绝。不会在运行时扫描、补种或改写旧 End。
- feature 只在原生 End Stone 表面放置一个非 BlockEntity 入口。入口没有 BlockItem，不能通过正常配方、创造物品栏或自动物流部署；它不替换容器、结构、玩家方块或未知旧地形。
- Wormhole 是稳定的自然探索入口：不以击杀末影龙、任务、全局阶段或 Shader 为前置，也不加入 Ancient Anchor、End 真相或 Compatibility 内容。第一版只允许玩家穿越，不建立物品、实体或自动物流的跨维语义。
- 每个 Wormhole 首次成功使用时，由 `MawTransitService` 建立独立、持久的 GatewayId 链接；同一入口永远复用同一链接。目标加载、有界安全落点、失败语义、冷却和 ticket `try/finally` 释放完全复用 Maw Gateway 契约。
- 落点保持同 X/Z，可在既有安全搜索范围使用地表高度；不采用随机远距散布、最近入口搜索或坐标比例。成功通过记录 `observed_wormhole` 和 `observed_maw`，但两者都不是使用前置条件。

## 验证

- 自动验证：注册／数据包加载、入口没有 BlockItem、观察状态持久化，以及既有 Maw 安全传送路径。
- 运行验证：新 Outer End 区块发现、未杀龙进入、单人进入／返回、存档重启、目标缺失、边界坐标和普通专服。多人、实际稀有度、美术可读性与传送手感保留 `[~]`。

## 8.11 延伸（2026-09-23）

8.4 的“仅放置一个入口”是当时的交付范围。8.11 允许在同一新生成区块的安全空位添加局部 Ancient Anchor 标记，细则见 [ADR 0010](0010-end-revelation-local-anchor-contract.md)。入口 ID、无 BlockItem、开放进入与既有存档链接语义不变。
