# 配置系统

BellCommand v1.4.0 引入了全新的模块化配置系统，旨在提供更高的灵活性、安全性和易用性。

## 1. 模块化架构

插件配置分为两个层次：

- **全局配置 (`config.yml`)**: 控制插件的基础行为，如语言、更新源、调试模式等。
- **物品配置 (`*_config/*.yml`)**: 定义具体的命令物品。插件会自动加载所有以 `_config` 结尾的文件夹中的 `.yml` 文件。

### 目录结构示例
```text
plugins/BellCommand/
├── config.yml
├── Default_config/
│   └── commands.yml
└── RPG_Items_config/    # 自定义文件夹，只要以 _config 结尾即可
    ├── weapons.yml
    └── tools.yml
```

## 2. 实时热重载 (WatchService)

不再需要频繁输入 `/bc reload`！BellCommand 采用了 Java NIO 的 `WatchService` 技术。

- **自动同步**: 当你在磁盘上修改、新增或删除配置文件时，插件会实时感知并自动重载受影响的配置。
- **零停机**: 重载过程在异步线程中进行，不会导致游戏卡顿。
- **安全保障**: 内部使用 `ReentrantReadWriteLock` 确保在重载期间玩家使用物品时的配置访问安全。

## 3. 智能迁移

如果你是从旧版本 (v1.3.x) 升级而来：
- 插件会自动检测旧版 `config.yml`。
- 它会将原有的全局 `auto-give` 和 `auto-cleanup` 设置应用到所有被迁移的物品中。
- 旧配置文件会被备份为 `config.yml.bak`。

## 4. 读写锁技术

为了应对高并发场景（如大型服务器中多名玩家同时频繁使用物品），我们引入了 **读写锁 (ReentrantReadWriteLock)**：
- **读操作并发**: 多名玩家可以同时读取配置触发命令。
- **写操作排他**: 仅在配置重载（热重载）时短暂锁定，确保玩家不会读到“破碎”或不完整的配置数据。
