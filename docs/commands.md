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
在 config.yml 中配置命令物品：

```yaml
items:
  example_item:
    item-id: CLOCK
    name: "&6示例物品"
    lore:
      - "&7右键点击使用"
    permission: "example.use"
    cooldown: 10
    commands:
      right-click:
        1:
          command: "say 你好，%player%!"
          as-console: false
      shift-right-click:
        1:
          command: "gamemode creative"
          as-console: true
```

### 命令类型
- right-click - 右键点击
- left-click - 左键点击
- shift-right-click - Shift+右键
- shift-left-click - Shift+左键
- bedrock-right-click - 基岩版右键
- bedrock-left-click - 基岩版左键

### 延迟执行命令
从版本1.3.0开始，BellCommand支持延迟执行命令功能。您可以通过添加`delay`参数来设置命令的延迟执行时间（秒）。

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