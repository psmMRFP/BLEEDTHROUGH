# ADR 0010：End Revelation 与局部 Ancient Anchor

状态：已接受（2026-09-23）

- Ancient Anchor 是天然 End Wormhole 附近的局部观测标记，不承担维持整个维度网络的职责。现有 Wormhole 的进入规则不变；末影龙死亡、研究和 Anchor 均不是传送前置条件。
- 新生成的 Outer End Wormhole 在同一生成区块寻找 End Stone 表面的空位，若场地足够，放置一个朝向虫洞的 Anchor 与四块 End Stone Bricks 标记。先检查全部位置，再写入；不跨区块、不覆盖非空气／非 End Stone 基础、不扫描或补种旧 End 区块。布局不适合时保留虫洞，不强行放置设施。重复生成时已占用的虫洞位置阻止再次放置。
- Anchor 无 BlockItem。右键只检查朝向前方两格且已加载的虫洞，并报告坐标或缺失；它不持有目标引用或全局索引。成功读取才记录观察。
- End Stone 与 Chorus 样本通过数据包 Item Tag 判定；Anchor 通过成功交互判定。三项观察独立、可乱序，完成后写入版本化的玩家知识状态。服务器每 40 tick 同步一次游戏内进度页，旧存档中已有的观察可恢复显示；首次观察和完成有中英文本反馈。研究不是入口硬锁。
- 本阶段不声明黑洞天空、Enderman 新 AI、完整 Anchor Network 或终局机制已经实现；它们仍是后续内容。

## 验证与限制

- Java 17 单元测试覆盖乱序、旧知识和重复完成；专服 GameTest 检查局部布局、占用保护、无 BlockItem、标签和进度数据包加载。
- 真实新 Outer End 发现率、玩家读取、客户端可读性、旧 End 世界重启与多人观察仍需实际验证。
