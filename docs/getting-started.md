# 入门指南

## 安装

1. 从以下渠道下载最新版本的 BellCommand：
   - [GitHub Releases](https://github.com/ning-g-mo/BellCommand/releases)
   - [Gitee Releases](https://gitee.com/ning-g-mo/BellCommand/releases)

2. 将下载的 jar 文件放入服务器的 plugins 文件夹

3. 重启服务器

## 基础配置

首次运行后，插件会在 `plugins/BellCommand` 目录下生成以下文件：
```yaml
config.yml # 主配置文件
lang/ # 语言文件目录
messages.yml # 默认语言文件
```


### 配置文件结构

```yaml
# 配置版本，请勿修改
config-version: 2
# 调试模式
debug: false
# 自动给予设置
auto-give:
enabled: true
join: true # 玩家加入时给予
respawn: true # 玩家重生时给予
items: # 要给予的物品ID列表
"example_item"
# 自动清理设置
auto-cleanup:
enabled: true
delay: 30 # 物品掉落后多少秒清理
# 更新检查设置
update-source:
enabled: true
source: "github" # 可选：github, gitee, custom
check-interval: 86400
reminder-interval: 14400
```
