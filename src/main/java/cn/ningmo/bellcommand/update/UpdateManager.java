package cn.ningmo.bellcommand.update;

import cn.ningmo.bellcommand.BellCommand;
import cn.ningmo.bellcommand.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class UpdateManager {
    private final BellCommand plugin;
    private boolean enabled;
    private long checkInterval;
    private long lastCheck;
    private final Object lock = new Object();
    private BukkitTask reminderTask;
    private UpdateSource updateSource;

    public UpdateManager(BellCommand plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("update-source.enabled", true);
        this.checkInterval = plugin.getConfig().getLong("update-source.check-interval", 86400) * 1000L;
        this.lastCheck = 0;
        initUpdateSource();
    }

    private void initUpdateSource() {
        try {
            String sourceType = plugin.getConfig().getString("update-source.source", "github");
            switch (sourceType.toLowerCase()) {
                case "github":
                    updateSource = new GitHubUpdateSource(plugin);
                    break;
                case "gitee":
                    updateSource = new GiteeUpdateSource(plugin);
                    break;
                case "custom":
                    updateSource = new CustomUpdateSource(plugin);
                    break;
                default:
                    plugin.getLogger().warning("无效的更新源类型: " + sourceType);
                    enabled = false;
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("初始化更新源时发生错误: " + e.getMessage());
            if (plugin.isDebugEnabled()) {
                e.printStackTrace();
            }
            enabled = false;
        }
    }

    public void checkForUpdates() {
        if (!enabled || updateSource == null) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.plugin.update.source-disabled")
                ));
            }
            return;
        }

        synchronized (lock) {
            long now = System.currentTimeMillis();
            if (now - lastCheck < checkInterval) {
                return;
            }
            lastCheck = now;

            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.plugin.update.checking")
                ));
            }

            try {
                UpdateInfo updateInfo = updateSource.checkUpdate();
                if (updateInfo != null) {
                    if (updateInfo.isUpdateAvailable()) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("current", updateInfo.getCurrentVersion());
                        placeholders.put("latest", updateInfo.getLatestVersion());
                        plugin.getLogger().info(ColorUtils.translateConsoleColors(
                            plugin.getLanguageManager().getMessage("messages.plugin.update.available", placeholders)
                        ));
                        
                        placeholders.clear();
                        placeholders.put("url", updateInfo.getDownloadUrl());
                        plugin.getLogger().info(ColorUtils.translateConsoleColors(
                            plugin.getLanguageManager().getMessage("messages.plugin.update.download-url", placeholders)
                        ));
                        
                        scheduleUpdateReminder(updateInfo);
                    } else {
                        if (plugin.isDebugEnabled()) {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("version", updateInfo.getCurrentVersion());
                            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                                plugin.getLanguageManager().getMessage("messages.plugin.update.up-to-date", placeholders)
                            ));
                        }
                    }
                }
            } catch (Exception e) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("error", e.getMessage());
                plugin.getLogger().warning(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.plugin.update.check-failed", placeholders)
                ));
                if (plugin.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void scheduleUpdateReminder(UpdateInfo updateInfo) {
        if (reminderTask != null) {
            reminderTask.cancel();
        }
        
        long reminderInterval = plugin.getConfig().getLong("update-source.reminder-interval", 14400) * 20L;
        reminderTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("current", updateInfo.getCurrentVersion());
            placeholders.put("latest", updateInfo.getLatestVersion());
            plugin.getLogger().info(plugin.getLanguageManager()
                .getMessage("messages.plugin.update.reminder", placeholders));
        }, reminderInterval, reminderInterval);
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("update-source.enabled", true);
        checkInterval = plugin.getConfig().getLong("update-source.check-interval", 86400) * 1000L;
        lastCheck = 0;
        
        if (reminderTask != null) {
            reminderTask.cancel();
            reminderTask = null;
        }
        
        initUpdateSource();
    }

    public void disable() {
        if (reminderTask != null) {
            reminderTask.cancel();
            reminderTask = null;
        }
        updateSource = null;
    }
} 