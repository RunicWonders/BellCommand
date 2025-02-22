package cn.ningmo.bellcommand.utils;

import org.bukkit.ChatColor;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Color;

public class ColorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    /**
     * 转换颜色代码（支持 & 和 §）
     * @param text 要转换的文本
     * @return 转换后的文本
     */
    public static String translateColors(String text) {
        if (text == null) {
            return "";
        }

        // 1.13 不支持十六进制颜色，将其转换为最接近的传统颜色
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexColor = matcher.group();
            ChatColor nearestColor = getNearestChatColor(hexColor);
            matcher.appendReplacement(buffer, "&" + nearestColor.getChar());
        }
        matcher.appendTail(buffer);
        text = buffer.toString();

        // 转换传统颜色代码
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * 转换列表中所有文本的颜色代码
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
     * 转换控制台颜色代码
     */
    public static String translateConsoleColors(String text) {
        if (text == null) {
            return "";
        }
        
        // 转换十六进制颜色代码为近似的传统颜色代码
        Matcher matcher = HEX_PATTERN.matcher(text);
        while (matcher.find()) {
            String hexColor = matcher.group();
            ChatColor nearestColor = getNearestChatColor(hexColor);
            text = text.replace(hexColor, "&" + nearestColor.getChar());
        }
        
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * 移除所有颜色代码
     */
    public static String stripColors(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(text);
    }

    /**
     * 获取最接近的传统颜色代码
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
                Color javaColor = getColorFromChatColor(color);
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
     * 从ChatColor获取java.awt.Color
     */
    private static Color getColorFromChatColor(ChatColor chatColor) {
        switch (chatColor) {
            case BLACK: return new Color(0x000000);
            case DARK_BLUE: return new Color(0x0000AA);
            case DARK_GREEN: return new Color(0x00AA00);
            case DARK_AQUA: return new Color(0x00AAAA);
            case DARK_RED: return new Color(0xAA0000);
            case DARK_PURPLE: return new Color(0xAA00AA);
            case GOLD: return new Color(0xFFAA00);
            case GRAY: return new Color(0xAAAAAA);
            case DARK_GRAY: return new Color(0x555555);
            case BLUE: return new Color(0x5555FF);
            case GREEN: return new Color(0x55FF55);
            case AQUA: return new Color(0x55FFFF);
            case RED: return new Color(0xFF5555);
            case LIGHT_PURPLE: return new Color(0xFF55FF);
            case YELLOW: return new Color(0xFFFF55);
            case WHITE: return new Color(0xFFFFFF);
            default: return new Color(0xFFFFFF);
        }
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