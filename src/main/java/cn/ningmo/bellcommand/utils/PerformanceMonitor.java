package cn.ningmo.bellcommand.utils;

import org.bukkit.plugin.Plugin;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

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
        if (time > threshold) {
            plugin.getLogger().warning(String.format("[性能监控] %s: %.2fms", logMessage, time/1_000_000.0));
        }
    }
    
    public static void clearTimings() {
        timings.clear();
    }
    
    private PerformanceMonitor() {
        // 私有构造函数防止实例化
    }
}