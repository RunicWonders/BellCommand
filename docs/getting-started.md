# 入门指南

## 安装

1. 从以下渠道下载最新版本的 BellCommand：
   - [GitHub Releases](https://github.com/ning-g-mo/BellCommand/releases)
   - [Gitee Releases](https://gitee.com/ning-g-mo/BellCommand/releases)

2. 将下载的 jar 文件放入服务器的 plugins 文件夹

3. 重启服务器

## 基础配置

首次运行后，插件会在 `plugins/BellCommand` 目录下生成以下结构：
```text
BellCommand/
├── config.yml              # 全局设置（语言、更新、调试等）
├── lang/                   # 语言文件夹
│   └── messages_zh_CN.yml  # 默认中文语言包
└── Default_config/         # 默认物品配置文件夹
    └── commands.yml        # 默认物品定义文件
```

### 全局配置 (config.yml)

```yaml
# 配置版本
config-version: 3

# 调试模式
debug: false

# 语言设置
language: "zh_CN"

# 更新检查设置
update-source:
  enabled: true
  source: "github"
  github:
    owner: "ning-g-mo"
    repo: "BellCommand"
```

### 物品配置 (Default_config/commands.yml)

在 v1.4.0 中，原本属于全局的 `auto-give` 和 `auto-cleanup` 已分拆到各物品配置中：

```yaml
items:
  example_item:
    item-id: "STICK"
    name: "&b示例命令棒"
    lore:
      - "&7左键点击执行命令"
    # 自动给予设置 (针对此物品)
    auto-give:
      join: true
      first-join: true
      respawn: true
    # 自动清理设置 (针对此物品)
    auto-cleanup:
      enabled: true
      delay: 30
    # 命令设置
    commands:
      left-click:
        1:
          command: "say 你好！"
          as-console: false
```
