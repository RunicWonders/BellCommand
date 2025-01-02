# BellCommand - 命令物品插件

## 版本说明
当前版本：1.2.0-beta.1

这是一个 beta 版本，包含以下特性和已知问题：
- ✅ 完整的命令物品系统
- ✅ 多语言支持（简体中文、繁体中文、英文、日文、法文）
- ✅ 基岩版玩家支持（需要 Floodgate）
- ✅ 自动给予和清理功能
- ⚠️ 已知的性能问题，将在后续版本优化
- ⚠️ 建议在测试环境中充分测试后再用于生产环境

## 简介
BellCommand 是一个功能强大的 Minecraft 插件，允许服务器管理员创建可以执行命令的自定义物品。玩家可以通过左键或右键点击这些物品来触发预设的命令。

灵感来源于配置杜蕾斯菜单时发现没有物品打开菜单的方法，在 MineBBS 上找到了名为右键菜单的插件，在评论区看到作者称 AI 制作的，于是下载插件并运行，发现无法加载，于是开发了此插件。

## 特性
- 支持自定义物品外观和名称
- 可为每个物品设置不同的左键和右键命令
- 完善的权限控制系统
- 命令冷却时间设置
- 多语言支持（简体中文、繁体中文、英文、日文）
- 调试模式
- 实时重载配置
- 自动更新检查（支持 GitHub、Gitee 和自定义源）

## 命令
- `/clock [物品ID]` - 获取命令物品
- `/clockremove [物品ID]` - 移除命令物品
- `/bellcommand reload` - 重载插件配置

## 权限
- `bellcommand.clock` - 允许使用 `/clock` 命令
- `bellcommand.clockremove` - 允许使用 `/clockremove` 命令
- `bellcommand.reload` - 允许重载插件配置
- `bellcommand.item.*` - 允许使用所有命令物品
- `bellcommand.item.<物品ID>` - 允许使用特定命令物品

## 安装
1. 下载最新版本的 BellCommand.jar
2. 将文件放入服务器的 plugins 文件夹
3. 重启服务器或重载插件
4. 编辑 plugins/BellCommand/config.yml 配置文件
5. 使用 `/bellcommand reload` 重载配置

## 配置说明
### 基础配置
```yaml
# 配置文件版本
config-version: 1

# 调试模式
debug: false

# 语言设置
language: zh_CN  # 可选: zh_CN, en_US, zh_TW, ja_JP
```

### 更新检查配置
支持三种更新源：
1. GitHub（默认）
```yaml
update-source:
  source: github
  github:
    owner: ning-g-mo
    repo: BellCommand
```

2. Gitee
```yaml
update-source:
  source: gitee
  gitee:
    owner: your-username
    repo: your-repo
```

3. 自定义源
```yaml
update-source:
  source: custom
  custom:
    check-url: "https://your-api.com/version"
    verify-ssl: true  # 使用自签名证书时设为 false
    headers:
      Authorization: "your-token"
```

### 物品配置示例
```yaml
items:
  home:
    item-id: CLOCK
    name: "&6菜单"
    lore:
      - "&7左键点击: &f打开菜单"
      - "&7右键点击: &f打开菜单"
    permission: "bellcommand.item.home"
    cooldown: 3
    commands:
      left-click:
        1:
          command: "cd"
          as-console: false
      right-click:
        1:
          command: "cd"
          as-console: false
```

## 高级功能
1. 多命令执行
   - 可以为同一个动作配置多个按顺序执行的命令
   - 支持控制台和玩家身份执行命令

2. 变量支持
   - `%player%` - 玩家名称

3. 冷却系统
   - 可为每个物品单独设置冷却时间
   - 支持精确到秒的冷却控制

4. 调试模式
   - 提供详细的操作日志
   - 帮助排查配置问题

## 常见问题
1. 物品无法使用
   - 检查玩家是否有对应权限
   - 确认物品 ID 配置正确
   - 查看控制台是否有错误信息

2. 命令执行失败
   - 确认命令语法正确
   - 检查执行身份（玩家/控制台）是否合适
   - 验证服务器是否安装了命令所需插件

3. 更新检查失败
   - 检查网络连接
   - 确认更新源配置正确
   - 如使用自定义源，验证 API 返回格式

## 支持与反馈
- GitHub Issues: [问题反馈](https://github.com/ning-g-mo/BellCommand/issues)
- MineBBS: [插件发布页](https://www.minebbs.com/)


## 特别鸣谢
- [cursor](https://www.cursor.com/) - 主力提供了插件的源代码，解决了部分问题。
- [IDEa](https://www.jetbrains.com/zh-cn/idea/) - IDEa提供了插件的构建环境，解决了部分问题。
- [leafMC](https://github.com/Winds-Studio/Leaf) - leafMC提供了插件的生产环境，解决了部分问题。