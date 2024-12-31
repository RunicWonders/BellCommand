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
    private final Map<Language, FileConfiguration> languageConfigs;
    private Language currentLanguage;
    
    public LanguageManager(BellCommand plugin) {
        this.plugin = plugin;
        this.languageConfigs = new HashMap<>();
        loadLanguages();
    }
    
    private void loadLanguages() {
        // 从配置文件获取当前语言设置
        String langCode = plugin.getConfig().getString("language", "zh_CN");
        currentLanguage = Language.fromCode(langCode);
        
        // 确保语言文件目录存在
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        // 加载所有支持的语言
        for (Language lang : Language.values()) {
            loadLanguage(lang, langDir);
        }
    }
    
    private void loadLanguage(Language lang, File langDir) {
        String fileName = lang.getCode() + ".yml";
        File langFile = new File(langDir, fileName);
        
        // 如果语言文件不存在，保存默认文件
        if (!langFile.exists()) {
            try {
                plugin.saveResource("lang/" + fileName, false);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "无法保存语言文件: " + fileName, e);
                return;
            }
        }
        
        // 加载语言文件
        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        // 检查默认语言文件中的键是否完整
        InputStream defaultLangStream = plugin.getResource("lang/" + fileName);
        if (defaultLangStream != null) {
            FileConfiguration defaultLang = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultLangStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defaultLang);
        }
        
        languageConfigs.put(lang, langConfig);
    }
    
    public String getMessage(String path) {
        return getMessage(path, new HashMap<>());
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        // 获取当前语言的配置
        FileConfiguration langConfig = languageConfigs.get(currentLanguage);
        if (langConfig == null) {
            return "Missing language configuration: " + currentLanguage.getCode();
        }
        
        // 获取消息
        String message = langConfig.getString(path);
        if (message == null) {
            // 如果当前语言没有这个消息，尝试从默认语言获取
            if (currentLanguage != Language.ZH_CN) {
                FileConfiguration defaultLangConfig = languageConfigs.get(Language.ZH_CN);
                if (defaultLangConfig != null) {
                    message = defaultLangConfig.getString(path);
                }
            }
            
            if (message == null) {
                return "Missing message: " + path;
            }
        }
        
        // 替换占位符
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return message;
    }
    
    public void setLanguage(Language language) {
        if (languageConfigs.containsKey(language)) {
            currentLanguage = language;
            // 保存语言设置到配置文件
            plugin.getConfig().set("language", language.getCode());
            plugin.saveConfig();
        }
    }
    
    public Language getCurrentLanguage() {
        return currentLanguage;
    }
    
    public void reload() {
        loadLanguages();
    }
    
    public boolean isLanguageAvailable(Language language) {
        return languageConfigs.containsKey(language);
    }
    
    public Map<Language, String> getAvailableLanguages() {
        Map<Language, String> available = new HashMap<>();
        for (Language lang : languageConfigs.keySet()) {
            available.put(lang, lang.getDisplayName());
        }
        return available;
    }
} 