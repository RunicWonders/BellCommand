# BellCommand 文档

BellCommand 是一个功能强大的 Minecraft 命令物品插件，支持自定义命令物品、自动给予、多语言等功能。

## 特性

- 📂 **模块化配置**: 支持多文件夹存储物品配置，结构清晰。
- 🔄 **实时热重载**: 基于 `WatchService` 实现免命令配置自动同步。
- 💎 **次数性物品**: 支持固定次数、概率触发、随机区间等多种消耗模式。
- 🎮 **多维交互**: 支持左键、右键、Shift+点击触发不同命令。
- 🌐 **多语言支持**: 完善的国际化方案。
- 🔒 **线程安全**: 引入读写锁保障高并发下的稳定性。
- 📱 **基岩版适配**: 针对 Floodgate 玩家的专项优化。

## 核心系统

- [📂 配置系统](config-system.md)
- [💎 次数性物品](consumables.md)
- [🌐 多语言支持](language.md)
- [🔄 更新与监控](update.md)
- [⌨️ 命令与权限](commands.md)
- [📜 更新日志](v1.4.0-beta.1_Changelog.md)

## 快速开始

1. 下载最新版本的 BellCommand (v1.4.0-beta.1)
2. 将插件放入服务器的 plugins 文件夹
3. 重启服务器
4. 编辑 `plugins/BellCommand/Default_config/commands.yml` 自定义你的首个物品

## 系统要求

- Java 21 或更高版本
- Spigot/Paper/Purpur 1.13 - 1.21
- 可选：Floodgate (用于基岩版支持)