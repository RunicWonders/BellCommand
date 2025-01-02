package cn.ningmo.bellcommand.utils;

import org.bukkit.plugin.Plugin;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import cn.ningmo.bellcommand.BellCommand;

public class PerformanceMonitor {
    private static final Map<String, Long> timings = new ConcurrentHashMap<>();
    
    public static void startTiming(String key) {
        timings.put(key, System.nanoTime());
    }
    
    public static long endTiming(String key) {
        Long start = timings.remove(key);
        if (start == null) return 0;
        return System.nanoTime() - start;
    }
    
    public static void logTiming(String key, long time, long threshold, Plugin plugin, String logMessage) {
        if (time > threshold && plugin instanceof BellCommand) {
            BellCommand bellCommand = (BellCommand) plugin;
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("message", logMessage);
            placeholders.put("time", String.format("%.2f", time/1_000_000.0));
            plugin.getLogger().warning(ColorUtils.translateConsoleColors(
                bellCommand.getLanguageManager().getMessage("messages.debug.performance.slow-operation", placeholders)
            ));
        }
    }
    
    public static void clearTimings() {
        timings.clear();
    }
    
    private PerformanceMonitor() {
        // 私有构造函数防止实例化
    }
}