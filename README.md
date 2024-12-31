# BellCommand - 命令物品插件

## 简介
BellCommand 是一个功能强大的 Minecraft 插件，允许服务器管理员创建可以执行命令的自定义物品。玩家可以通过左键或右键点击这些物品来触发预设的命令。  
灵感来源于配置杜蕾斯菜单时发现没有物品打开打开的方法，在minebbs上找到了名为右键菜单的插件，在评论区看到作者称ai制作的，但是ai没有提供源码，所以作者自己写了一个。于是下载插件并运行，发现无法加载，于是开发了此插件。
## 特性
- 支持自定义物品外观和名称
- 可为每个物品设置不同的左键和右键命令
- 完善的权限控制系统
- 命令冷却时间设置
- 多语言支持
- 调试模式
- 实时重载配置

## 命令
- `/clock [物品ID]` - 获取命令物品
- `/clockremove [物品ID]` - 移除命令物品
- `/bellcommand:reload` - 重载插件配置

## 权限
- `bellcommand.clock` - 允许使用 `/clock` 命令
- `bellcommand.reload` - 允许重载插件配置
- `bellcommand.item.*` - 允许使用所有命令物品
- `bellcommand.item.<物品ID>` - 允许使用特定命令物品

## 配置文件

```yaml
# 插件设置
debug: false # 调试模式，设置为true时会输出更多信息
language: "zh_CN" # 语言设置 (支持: zh_CN, en_US, zh_TW, ja_JP)

# 更新源设置
update-source:
   enabled: true # 是否启用更新检查
   source: "github" # 更新源类型 (支持: github, gitee, custom)
   check-interval: 86400 # 检查间隔（秒），默认24小时
   custom-url: "" # 自定义更新源URL，仅在 source 为 custom 时生效
   github:
      owner: "ning-g-mo" # GitHub 仓库所有者
      repo: "BellCommand" # GitHub 仓库名称
   gitee:
      owner: "ning-g-mo" # Gitee 仓库所有者
      repo: "BellCommand" # Gitee 仓库名称

# 命令物品设置
items:
   clock: # 物品ID，用于配置中引用
      item-id: CLOCK # 物品类型
      name: "&6时钟命令物品" # 显示名称
      lore: # 物品说明
         - "&7右键点击执行命令"
         - "&7左键点击执行命令"
      commands:
         right-click: # 右键命令列表
            - "say 这是右键命令1"
            - "me 正在使用命令物品"
         left-click: # 左键命令列表
            - "say 这是左键命令1"
            - "me 使用了左键命令"
      permission: "bellcommand.item.clock" # 使用权限
      cooldown: 5 # 命令冷却时间(秒)

   compass: # 另一个命令物品示例
      item-id: COMPASS
      name: "&a指南针命令物品"
      lore:
         - "&7右键传送到出生点"
         - "&7左键打开传送菜单"
      commands:
         right-click:
            - "spawn"
         left-click:
            - "tpmenu"
      permission: "bellcommand.item.compass"
      cooldown: 10

# 物品默认设置
default-settings:
   cooldown: 5 # 默认冷却时间(秒)
   permission-required: true # 是否需要权限

# 配置版本号，用于后续升级时的配置迁移
config-version: 1

```

## 安装
1. 下载最新版本的 BellCommand.jar
2. 将文件放入服务器的 plugins 文件夹
3. 重启服务器或重载插件
4. 编辑 config.yml 配置文件
5. 使用 `/bellcommand:reload` 重载配置

## 注意事项
- 命令无需添加斜杠 `/`
- 确保玩家具有执行相应命令的权限
- 调试模式会产生大量日志，建议仅在排查问题时启用
- 物品ID不要包含特殊字符
- 确保配置文件格式正确

## 常见问题
1. 物品无法使用
   - 检查玩家是否有相应权限
   - 确认物品是否在冷却中
   - 查看控制台是否有错误信息

2. 命令执行失败
   - 确保命令格式正确
   - 检查玩家是否有执行该命令的权限
   - 启用调试模式查看详细信息

3. 配置重载失败
   - 检查配置文件格式是否正确
   - 确保所有物品ID和命令格式正确
   - 查看控制台错误信息

## 支持
如果遇到问题或需要帮助，可以：
1. 查看控制台日志
2. 启用调试模式获取更多信息
3. 检查配置文件格式
4. 联系插件作者获取支持

## 特别鸣谢
- [cursor](https://www.cursor.com/) - 主力提供了插件的源代码，解决了部分问题。
- [IDEa](https://www.jetbrains.com/zh-cn/idea/) - IDEa提供了插件的构建环境，解决了部分问题。
- [leafMC](https://github.com/Winds-Studio/Leaf) - leafMC提供了插件的生产环境，解决了部分问题。