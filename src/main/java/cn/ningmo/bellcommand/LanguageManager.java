package cn.ningmo.bellcommand;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import cn.ningmo.bellcommand.utils.ColorUtils;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;

public class LanguageManager {
    private final BellCommand plugin;
    private FileConfiguration langConfig;
    private String language;

    public LanguageManager(BellCommand plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    private void loadLanguage() {
        // 获取配置文件中设置的语言
        language = plugin.getConfig().getString("language", "zh_CN");
        
        // 保存默认语言文件
        saveDefaultLanguageFiles();
        
        // 加载语言文件
        File langFile = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("找不到语言文件: " + language + ".yml，将使用默认语言");
            language = "zh_CN";
            langFile = new File(plugin.getDataFolder(), "lang/zh_CN.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // 加载默认语言文件作为后备
        InputStream defLangStream = plugin.getResource("lang/" + language + ".yml");
        if (defLangStream != null) {
            YamlConfiguration defLangConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defLangStream, StandardCharsets.UTF_8)
            );
            langConfig.setDefaults(defLangConfig);
        }
    }

    private void saveDefaultLanguageFiles() {
        String[] languages = {"zh_CN", "en_US", "zh_TW"};
        for (String lang : languages) {
            File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
            if (!langFile.exists()) {
                plugin.saveResource("lang/" + lang + ".yml", false);
            }
        }
    }

    public String getMessage(String path) {
        String message = langConfig.getString(path);
        if (message == null) {
            return "§c缺少语言键: " + path;
        }
        return ColorUtils.translateColors(message);
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return ColorUtils.translateColors(message);
    }

    public void reload() {
        loadLanguage();
    }

    public String getLanguage() {
        return language;
    }
} 