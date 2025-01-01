package cn.ningmo.bellcommand.utils;

import org.bukkit.ChatColor;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    /**
     * 转换颜色代码（支持 & 和 § 以及 RGB 十六进制颜色）
     * @param text 要转换的文本
     * @return 转换后的文本
     */
    public static String translateColors(String text) {
        if (text == null) {
            return "";
        }

        // 转换十六进制颜色代码
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String group = matcher.group();
            matcher.appendReplacement(buffer, ChatColor.valueOf(group) + "");
        }
        matcher.appendTail(buffer);
        text = buffer.toString();

        // 转换传统颜色代码
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * 转换列表中所有文本的颜色代码
     * @param list 要转换的文本列表
     * @return 转换后的文本列表
     */
    public static List<String> translateColors(List<String> list) {
        if (list == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (String text : list) {
            result.add(translateColors(text));
        }
        return result;
    }

    /**
     * 转换控制台颜色代码（仅支持 & 和 §）
     * @param text 要转换的文本
     * @return 转换后的文本
     */
    public static String translateConsoleColors(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * 移除所有颜色代码
     * @param text 要处理的文本
     * @return 移除颜色代码后的文本
     */
    public static String stripColors(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(text);
    }
} 