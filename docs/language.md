# 多语言支持
## 语言文件
语言文件位于 `plugins/BellCommand/lang/messages.yml`。你可以根据需要修改或添加新的语言文件。

## 消息格式

```yaml
messages:
  plugin:
    version: "&a当前版本：&f%version%"
    debug-enabled: "&e调试模式已启用"
    floodgate-detected: "&a已检测到 Floodgate，基岩版支持已启用"
    
  command:
    help: "&6/bc help &7- 显示帮助信息"
    reload-success: "&a插件重载成功"
    give-success: "&a已将 &f%item% &ax%amount% &a给予 &f%player%"
    
  error:
    no-permission: "&c你没有权限执行此命令"
    player-not-found: "&c找不到玩家：%player%"
    item-not-found: "&c找不到物品：%item%"
```

## 占位符

- %player% - 玩家名称
- %item% - 物品名称
- %amount% - 物品数量
- %version% - 插件版本
- %time% - 剩余时间