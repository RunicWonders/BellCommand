# 更新系统
## 更新源配置
BellCommand 支持多个更新源：

**github**
```yaml
update-source:
  source: "github"
  github:
    owner: "ning-g-mo"
    repo: "BellCommand"
```

**gitee**
```yaml
update-source:
  source: "gitee"
  gitee:
    owner: "ning-g-mo"
    repo: "BellCommand"
```

**自定义更新源**
```yaml
update-source:
  source: "custom"
  custom:
    check-url: "https://api.example.com/version"
    verify-ssl: true
    headers:
      Authorization: "Bearer token" 
```

## 更新检查
- check-interval: 检查间隔（秒）
- reminder-interval: 提醒间隔（秒）
更新检查会返回以下信息：
- 当前版本
- 最新版本
- 下载链接
- 更新说明（仅限自定义更新源）

# 性能优化
## 性能监控
BellCommand 内置了性能监控系统，可以追踪各种操作的执行时间。
**监控项目**
- 命令执行时间
- 更新检查耗时
- 物品给予性能
- 配置加载时间
- 调试输出
启用调试模式后，可以看到详细的性能信息：
```yaml
debug: true
```
性能日志示例：性能日志示例：
```yaml
[BellCommand] 操作耗时过长: 命令执行 (23.45 ms)
[BellCommand] 操作耗时过长: 更新检查 (1254.67 ms)
```
## 性能优化建议
1. 合理设置更新检查间隔
2. 避免过多的自动给予物品
3. 使用异步处理耗时操作
4. 及时清理掉落的命令物品