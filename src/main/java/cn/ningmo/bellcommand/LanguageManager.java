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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageManager {
    private final BellCommand plugin;
    private FileConfiguration langConfig;
    private String language;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");

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
        String[] languages = {"zh_CN", "en_US", "zh_TW", "ja_JP", "fr_FR"};
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
        String message = langConfig.getString(path);
        if (message == null) {
            return "§c缺少语言键: " + path;
        }

        // 先处理占位符，再处理颜色代码
        if (placeholders != null) {
            StringBuffer result = new StringBuffer();
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
            
            while (matcher.find()) {
                String placeholder = matcher.group(1);
                String replacement = placeholders.getOrDefault(placeholder, matcher.group());
                // 转义特殊字符
                replacement = Matcher.quoteReplacement(replacement);
                matcher.appendReplacement(result, replacement);
            }
            matcher.appendTail(result);
            message = result.toString();
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