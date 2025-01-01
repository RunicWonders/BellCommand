package cn.ningmo.bellcommand.utils;

import org.bukkit.ChatColor;
import java.util.regex.Pattern;

public class ColorUtils {
    private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9a-fk-orA-FK-OR]");
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final String RESET_CODE = "\u001B[0m";
    
    /**
     * 转换颜色代码为控制台可读的格式
     * @param message 原始消息
     * @return 处理后的消息
     */
    public static String translateConsoleColors(String message) {
        if (message == null) return "";
        
        // 将 & 转换为 §
        String result = ChatColor.translateAlternateColorCodes('&', message);
        
        // 如果消息中没有颜色代码，直接返回
        if (!result.contains("§")) {
            return result;
        }
        
        if (IS_WINDOWS) {
            // Windows系统使用简单的颜色代码
            StringBuilder coloredMessage = new StringBuilder();
            boolean hasColor = false;
            
            for (int i = 0; i < result.length(); i++) {
                if (result.charAt(i) == '§' && i + 1 < result.length()) {
                    char code = result.charAt(i + 1);
                    String colorCode = getWindowsColorCode(code);
                    if (!colorCode.isEmpty()) {
                        coloredMessage.append(colorCode);
                        hasColor = true;
                    }
                    i++; // 跳过颜色代码
                } else {
                    coloredMessage.append(result.charAt(i));
                }
            }
            
            // 如果添加了颜色，确保在消息末尾重置
            if (hasColor) {
                coloredMessage.append(RESET_CODE);
            }
            
            return coloredMessage.toString();
        } else {
            // 非Windows系统直接返回，但确保末尾有重置代码
            return result.endsWith("§r") ? result : result + "§r";
        }
    }
    
    /**
     * 获取Windows控制台颜色代码
     */
    private static String getWindowsColorCode(char code) {
        switch (Character.toLowerCase(code)) {
            case '0': return "\u001B[30m"; // 黑色
            case '1': return "\u001B[34m"; // 深蓝色
            case '2': return "\u001B[32m"; // 深绿色
            case '3': return "\u001B[36m"; // 湖蓝色
            case '4': return "\u001B[31m"; // 深红色
            case '5': return "\u001B[35m"; // 紫色
            case '6': return "\u001B[33m"; // 金色
            case '7': return "\u001B[37m"; // 灰色
            case '8': return "\u001B[90m"; // 深灰色
            case '9': return "\u001B[94m"; // 蓝色
            case 'a': return "\u001B[92m"; // 绿色
            case 'b': return "\u001B[96m"; // 天蓝色
            case 'c': return "\u001B[91m"; // 红色
            case 'd': return "\u001B[95m"; // 粉红色
            case 'e': return "\u001B[93m"; // 黄色
            case 'f': return "\u001B[97m"; // 白色
            case 'r': return RESET_CODE;   // 重置
            case 'l': return "\u001B[1m";  // 粗体
            case 'n': return "\u001B[4m";  // 下划线
            case 'o': return "\u001B[3m";  // 斜体
            default: return "";
        }
    }
    
    /**
     * 移除所有颜色代码
     * @param message 原始消息
     * @return 无颜色代码的消息
     */
    public static String stripColors(String message) {
        if (message == null) return "";
        return COLOR_PATTERN.matcher(message).replaceAll("");
    }
    
    /**
     * 转换颜色代码为玩家可读的格式
     * @param message 原始消息
     * @return 处理后的消息
     */
    public static String translatePlayerColors(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 检查消息是否包含颜色代码
     * @param message 要检查的消息
     * @return 是否包含颜色代码
     */
    public static boolean hasColorCodes(String message) {
        if (message == null) return false;
        return message.contains("§") || message.contains("&");
    }
} 