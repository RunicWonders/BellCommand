package cn.ningmo.bellcommand;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import cn.ningmo.bellcommand.utils.ColorUtils;
import java.io.File;
import java.io.IOException;
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
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");

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
                plugin.getLogger().warning("找不到语言文件: messages.yml，将使用默认语言");
                // 如果找不到语言文件，创建一个空的配置
                langConfig = new YamlConfiguration();
                return;
            }
        }

        try (InputStreamReader reader = new InputStreamReader(new java.io.FileInputStream(langFile), StandardCharsets.UTF_8)) {
            langConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().warning("加载语言文件失败: " + e.getMessage());
            langConfig = YamlConfiguration.loadConfiguration(langFile); // 回退到默认加载
        }

        // 加载默认语言文件作为后备
        InputStream defLangStream = plugin.getResource("lang/messages.yml");
        if (defLangStream != null) {
            YamlConfiguration defLangConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defLangStream, StandardCharsets.UTF_8)
            );
            langConfig.setDefaults(defLangConfig);
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
} 