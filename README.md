# BellCommand - 命令物品插件

[English Version](README_EN.md)

## 版本说明
当前版本：1.2.0-beta.1

这是一个 beta 版本，包含以下特性和已知问题：
- ✅ 完整的命令物品系统
- ✅ 中文语言支持
- ✅ 基岩版玩家支持（需要 Floodgate）
- ✅ 自动给予和清理功能
- ⚠️ 已知的性能问题，将在后续版本优化
- ⚠️ 建议在测试环境中充分测试后再用于生产环境

## 简介
BellCommand 是一个功能强大的 Minecraft 插件，允许服务器管理员创建可以执行命令的自定义物品。玩家可以通过左键或右键点击这些物品来触发预设的命令。

## 特性
- 支持自定义物品和名称
- 可为每个物品设置不同的左键和右键命令
- 完善的权限控制系统
- 命令冷却时间设置
- 多语言支持
- 调试模式
- 自动更新检查

## 命令
- `/bc give <玩家> <物品ID> [数量]` - 给予命令物品
- `/bc list` - 查看可用的命令物品
- `/bc reload` - 重载插件配置

## 权限
- `bellcommand.command` - 允许使用基础命令
- `bellcommand.reload` - 允许重载配置
- `bellcommand.give` - 允许给予物品
- `bellcommand.list` - 允许查看物品列表
- `bellcommand.use` - 允许使用命令物品
- `bellcommand.item.<物品ID>` - 允许使用特定物品

## 安装
1. 下载最新版本的 BellCommand.jar
2. 将文件放入服务器的 plugins 文件夹
3. 重启服务器或重载插件
4. 编辑配置文件
5. 使用 `/bc reload` 重载配置

## 支持与反馈
- GitHub Issues: [问题反馈](https://github.com/ning-g-mo/BellCommand/issues)

## 特别鸣谢
- [cursor](https://www.cursor.com/)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- [leafMC](https://github.com/Winds-Studio/Leaf)
