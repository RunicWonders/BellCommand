package cn.ningmo.bellcommand;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import cn.ningmo.bellcommand.utils.ColorUtils;

public class LanguageManager {
    private final BellCommand plugin;
    private FileConfiguration langConfig;
    
    public LanguageManager(BellCommand plugin) {
        this.plugin = plugin;
        loadLanguage();
    }
    
    public void loadLanguage() {
        // 确保语言文件目录存在
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        // 保存默认语言文件
        File langFile = new File(langDir, "zh_CN.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/zh_CN.yml", false);
        }
        
        // 加载语言文件
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        // 检查默认语言文件中的键是否完整
        InputStream defaultLangStream = plugin.getResource("lang/zh_CN.yml");
        if (defaultLangStream != null) {
            FileConfiguration defaultLang = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultLangStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defaultLang);
        }
    }
    
    public String getMessage(String path) {
        return getMessage(path, new HashMap<>());
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = langConfig.getString(path);
        if (message == null) {
            plugin.getLogger().warning("找不到语言键: " + path);
            return "§c[错误] 找不到语言键: " + path;
        }
        
        // 替换占位符
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        
        // 根据输出目标处理颜色
        if (path.startsWith("messages.debug.")) {
            // 调试信息输出到控制台
            return ColorUtils.translateConsoleColors(message);
        } else {
            // 玩家消息
            return ColorUtils.translatePlayerColors(message);
        }
    }
    
    public String getConsoleMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        return ColorUtils.translateConsoleColors(message);
    }
    
    public String getPlayerMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        return ColorUtils.translatePlayerColors(message);
    }
    
    public void reload() {
        loadLanguage();
    }
    
    public void validateMessages() {
        // 检查必需的消息键
        String[] requiredKeys = {
            "messages.plugin.enable",
            "messages.plugin.disable",
            "messages.command.player-only",
            "messages.command.no-permission",
            "messages.command.reload-success",
            "messages.command.reload-failed",
            "messages.command.item-list",
            "messages.command.item-list-entry",
            "messages.command.invalid-item",
            "messages.command.item-exists",
            "messages.command.item-given",
            "messages.command.item-removed",
            "messages.command.no-item-found",
            "messages.command.cooldown",
            "messages.error.no-permission-use",
            "messages.error.reload-error"
        };
        
        for (String key : requiredKeys) {
            if (!langConfig.contains(key)) {
                plugin.getLogger().warning("语言文件缺少必需的消息键: " + key);
            }
        }
    }
} 