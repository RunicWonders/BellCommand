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