package cn.ningmo.bellcommand.language;

import cn.ningmo.bellcommand.BellCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class LanguageManager {
    private final BellCommand plugin;
    private FileConfiguration langConfig;
    
    public LanguageManager(BellCommand plugin) {
        this.plugin = plugin;
        loadLanguage();
    }
    
    private void loadLanguage() {
        // 确保语言文件目录存在
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        // 加载语言文件
        File langFile = new File(langDir, "messages.yml");
        if (!langFile.exists()) {
            try {
                plugin.saveResource("lang/messages.yml", false);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "无法保存语言文件: messages.yml", e);
                return;
            }
        }
        
        // 加载语言文件
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        // 检查默认语言文件中的键是否完整
        InputStream defaultLangStream = plugin.getResource("lang/messages.yml");
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
        // 获取消息
        String message = langConfig.getString(path);
        if (message == null) {
            return "Missing message: " + path;
        }
        
        // 替换占位符
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        
        return message;
    }
    
    public void reload() {
        loadLanguage();
    }
}