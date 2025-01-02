package cn.ningmo.bellcommand.utils;

import org.bukkit.ChatColor;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");

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
            String hexColor = matcher.group();
            String bukkitColor = hexColor.replace("#", "§x§" + String.join("§", hexColor.substring(1).split("")));
            matcher.appendReplacement(buffer, bukkitColor);
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
        String result = text;
        
        // 转换十六进制颜色代码为近似的传统颜色代码
        Matcher matcher = HEX_PATTERN.matcher(result);
        while (matcher.find()) {
            String hexColor = matcher.group();
            ChatColor nearestColor = getNearestChatColor(hexColor);
            result = result.replace(hexColor, "&" + nearestColor.getChar());
        }
        
        return ChatColor.translateAlternateColorCodes('&', result);
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

    /**
     * 获取最接近的传统颜色代码
     * @param hexColor 十六进制颜色代码
     * @return 最接近的ChatColor
     */
    private static ChatColor getNearestChatColor(String hexColor) {
        // 移除#号
        hexColor = hexColor.replace("#", "");
        
        // 解析RGB值
        int r = Integer.parseInt(hexColor.substring(0, 2), 16);
        int g = Integer.parseInt(hexColor.substring(2, 4), 16);
        int b = Integer.parseInt(hexColor.substring(4, 6), 16);
        
        ChatColor nearestColor = ChatColor.WHITE;
        double minDistance = Double.MAX_VALUE;
        
        // 遍历所有传统颜色找到最接近的
        for (ChatColor color : ChatColor.values()) {
            if (color.isColor()) {
                java.awt.Color javaColor = color.asBungee().getColor();
                double distance = getColorDistance(r, g, b, 
                    javaColor.getRed(), javaColor.getGreen(), javaColor.getBlue());
                
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestColor = color;
                }
            }
        }
        
        return nearestColor;
    }

    /**
     * 计算两个颜色之间的距离
     */
    private static double getColorDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
        double rmean = (r1 + r2) / 2.0;
        int r = r1 - r2;
        int g = g1 - g2;
        int b = b1 - b2;
        double weightR = 2 + rmean / 256.0;
        double weightG = 4.0;
        double weightB = 2 + (255 - rmean) / 256.0;
        return Math.sqrt(weightR * r * r + weightG * g * g + weightB * b * b);
    }
} 