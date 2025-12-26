# BellCommand - 命令物品插件

[English Version](README_EN.md)

## 版本说明
当前版本：1.4.0-beta.1

这是一个重大的 Beta 版本，包含以下特性：
- ✅ **配置系统重构**: 采用模块化多文件夹架构，支持实时热重载（WatchService）和智能自动迁移。
- ✅ **次数性物品系统**: 支持固定数量、概率触发、随机区间等多种消耗模式。
- ✅ **增强的线程安全**: 全面引入读写锁（ReentrantReadWriteLock），确保高并发下的配置稳定性。
- ✅ **自动化发布流程**: 深度集成 GitHub Actions 与 Modrinth API 自动发布。
- ✅ 完整的命令物品系统
- ✅ 中文语言支持
- ✅ 基岩版玩家支持（需要 Floodgate）
- ✅ 动作类型独立冷却功能
- ✅ 自定义更新源支持（GitHub、Gitee、自定义API）

## 简介
BellCommand 是一个功能强大的 Minecraft 插件，允许服务器管理员创建可以执行命令的自定义物品。玩家可以通过左键或右键点击这些物品来触发预设的命令。

## 特性
- **模块化配置管理**: 支持多文件夹存储物品配置（如 `Default_config/`），物品定义与主配置解耦。
- **实时热重载**: 基于 `WatchService` 监听文件变动，修改配置即刻生效，无需手动重载。
- **次数性消耗系统**: 支持物品按固定次数、概率或随机区间消耗，灵活控制物品寿命。
- **线程安全保障**: 核心配置读取引入 `ReentrantReadWriteLock` 读写锁，确保高并发环境下的绝对稳定。
- **多维交互支持**: 支持左键、右键、Shift+左/右键独立触发不同命令。
- **基岩版深度适配**: 针对 Floodgate 玩家提供专用交互逻辑和命令配置。
- **精细权限控制**: 完善的权限系统，支持全局权限及针对单个物品的专属权限。
- **独立冷却机制**: 不同触发动作（如左键/右键）可拥有独立的冷却计时，互不干扰。
- **国际化多语言**: 完善的 `LanguageManager` 系统，支持系统日志与提示信息的实时切换。
- **自动更新提醒**: 集成 GitHub、Gitee 及自定义 API 的更新检测机制。

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
