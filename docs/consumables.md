# 次数性物品系统

BellCommand v1.4.0 引入了精细的物品消耗逻辑，允许你为命令物品设置使用次数或触发概率。

## 配置项

消耗逻辑在物品定义的 `consumable` 节点下配置：

```yaml
items:
  limited_sword:
    item-id: "DIAMOND_SWORD"
    consumable:
      enabled: true          # 是否开启消耗系统
      mode: "COUNT"          # 消耗模式
      amount: 1              # 消耗数量 (适用于 COUNT 和 PROBABILITY 模式)
      probability: 0.5       # 消耗概率 (适用于 PROBABILITY 和 PROBABILITY_RANGE 模式)
      min-amount: 1          # 最小消耗数量 (适用于 RANGE 和 PROBABILITY_RANGE 模式)
      max-amount: 3          # 最大消耗数量 (适用于 RANGE 和 PROBABILITY_RANGE 模式)
```

## 消耗模式 (Modes)

### 1. COUNT (固定消耗)
每次执行成功命令后，固定消耗指定数量的物品。
- `amount`: 每次消耗的数量。
- 示例：`amount: 1` 表示每点一次少一个。

### 2. PROBABILITY (概率消耗)
每次执行成功后，有一定概率消耗指定数量的物品。
- `probability`: 触发概率 (0.0 到 1.0)。
- `amount`: 触发后的消耗数量。
- 示例：`probability: 0.5`, `amount: 1` 表示 50% 的几率扣除 1 个物品。

### 3. RANGE (随机区间消耗)
每次执行成功后，随机消耗 `[min-amount, max-amount]` 范围内的数量。
- `min-amount`: 最小消耗量。
- `max-amount`: 最大消耗量。

### 4. PROBABILITY_RANGE (概率触发 + 随机区间)
先判定是否触发消耗，如果触发，再随机扣除指定范围的数量。
- `probability`: 触发概率 (0.0 到 1.0)。
- `min-amount`: 触发后的最小消耗量。
- `max-amount`: 触发后的最大消耗量。

## 注意事项

- **堆叠支持**: 消耗逻辑会自动处理物品堆叠。如果当前格子的物品扣完了，该堆叠会消失。
- **命令成功触发**: 只有在动作触发且命令逻辑被执行后（通过权限和冷却检查）才会进行消耗判定。
- **冷却独立**: 消耗逻辑与冷却系统（Cooldown）是独立的，你可以设置一个有 5 秒冷却且 20% 概率消耗的强力卷轴。
