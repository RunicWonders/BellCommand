# 命令系统
## 基础命令

- /bc - 显示帮助信息
- /bc reload - 重载插件
- /bc give <玩家> <物品ID> [数量] - 给予命令物品
- /bc list - 列出所有可用的命令物品

## 权限
- bellcommand.reload - 允许重载插件
- bellcommand.give - 允许给予命令物品
- bellcommand.list - 允许查看物品列表

## 命令物品配置
从版本 1.4.0 开始，命令物品定义已移至独立的文件夹（默认为 `Default_config/`）中的 `commands.yml` 文件。

### 基础结构
```yaml
items:
  example_item:
    item-id: CLOCK
    name: "&6示例物品"
    lore:
      - "&7右键点击使用"
    permission: "example.use"
    cooldown: 10
    # 自动给予配置
    auto-give:
      join: true
      first-join: true
      respawn: true
    # 自动清理配置
    auto-cleanup:
      enabled: true
      delay: 30
    commands:
      right-click:
        1:
          command: "say 你好，%player%!"
          as-console: false
```

### 次数性物品 (Consumable)
从版本 1.4.0-alpha-3 开始，支持配置物品的使用次数和消耗逻辑。

```yaml
    consumable:
      enabled: true
      mode: "COUNT"      # 消耗模式: COUNT, PROBABILITY, RANGE, PROBABILITY_RANGE
      amount: 1         # 消耗数量 (适用于 COUNT 和 PROBABILITY 模式)
      probability: 0.5  # 消耗概率 (适用于 PROBABILITY 和 PROBABILITY_RANGE 模式)
      min-amount: 1     # 最小消耗数量 (适用于 RANGE 和 PROBABILITY_RANGE 模式)
      max-amount: 3     # 最大消耗数量 (适用于 RANGE 和 PROBABILITY_RANGE 模式)
```

#### 模式详解：
- **COUNT**: 每次执行命令固定消耗 `amount` 个物品。
- **PROBABILITY**: 每次执行命令有 `probability` 的概率消耗 `amount` 个物品。
- **RANGE**: 每次执行命令随机消耗 `min-amount` 到 `max-amount` 之间的物品数量。
- **PROBABILITY_RANGE**: 每次执行命令有 `probability` 的概率，随机消耗 `min-amount` 到 `max-amount` 之间的物品数量。

### 命令类型
- right-click - 右键点击
- left-click - 左键点击
- shift-right-click - Shift+右键
- shift-left-click - Shift+左键
- bedrock-right-click - 基岩版右键
- bedrock-left-click - 基岩版左键
- bedrock-shift-right-click - 基岩版 Shift+右键
- bedrock-shift-left-click - 基岩版 Shift+左键

### 延迟执行命令
从版本1.3.0开始，BellCommand支持延迟执行命令功能。您可以通过添加`delay`参数来设置命令的延迟执行时间（秒）。

从版本1.3.1开始，BellCommand支持动作类型独立冷却功能，不同交互方式（如左键/右键）可以拥有各自独立的冷却。

```yaml
items:
  teleport_item:
    item-id: ENDER_PEARL
    name: "&b传送魔珠"
    lore:
      - "&7右键点击: &f3秒后传送到主城"
    permission: "bellcommand.item.teleport"
    cooldown: 10
    commands:
      right-click:
        1:
          command: "title %player% title {\"text\":\"准备传送\",\"color\":\"yellow\"}"
          as-console: true
        2:
          command: "title %player% subtitle {\"text\":\"3秒后传送到主城\",\"color\":\"gold\"}"
          as-console: true
        3:
          command: "spawn"
          as-console: false
          delay: 3.0  # 延迟3秒执行
```

延迟执行命令特性：
- 支持小数点，可以精确到毫秒级别
- 可以在同一个动作中混合使用即时命令和延迟命令
- 延迟命令在玩家离线时不会执行
- 服务器重启会取消所有未执行的延迟命令