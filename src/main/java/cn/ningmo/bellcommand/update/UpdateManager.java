package cn.ningmo.bellcommand.update;

import cn.ningmo.bellcommand.BellCommand;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class UpdateManager {
    private final BellCommand plugin;
    private final UpdateSource updateSource;
    private boolean enabled;
    private long lastCheck;
    private long checkInterval;
    private final Object lock = new Object();

    public UpdateManager(BellCommand plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("update-source.enabled", true);
        this.checkInterval = plugin.getConfig().getLong("update-source.check-interval", 86400) * 1000L;
        this.updateSource = initUpdateSource();
    }

    private UpdateSource initUpdateSource() {
        if (!enabled) return null;

        String sourceType = plugin.getConfig().getString("update-source.source", "github");
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("update-source." + sourceType);

        if (plugin.isDebugEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("source", sourceType);
            plugin.getLogger().info(plugin.getLanguageManager()
                .getMessage("messages.debug.update.source-init", placeholders));
        }

        switch (sourceType.toLowerCase()) {
            case "github":
                return new GitHubUpdateSource(plugin, config);
            case "gitee":
                return new GiteeUpdateSource(plugin, config);
            case "custom":
                return new CustomUpdateSource(plugin, config);
            default:
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("source", sourceType);
                plugin.getLogger().warning(plugin.getLanguageManager()
                    .getMessage("messages.plugin.update.invalid-source", placeholders));
                return null;
        }
    }

    public void checkForUpdates() {
        if (!enabled || updateSource == null) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info(plugin.getLanguageManager()
                    .getMessage("messages.plugin.update.source-disabled"));
            }
            return;
        }

        synchronized (lock) {
            long now = System.currentTimeMillis();
            if (now - lastCheck < checkInterval) {
                return;
            }
            lastCheck = now;
        }

        plugin.getLogger().info(plugin.getLanguageManager()
            .getMessage("messages.plugin.update.checking"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().info(plugin.getLanguageManager()
                        .getMessage("messages.debug.update.check-start"));
                }

                UpdateInfo updateInfo = updateSource.checkUpdate();
                if (updateInfo != null && updateInfo.isUpdateAvailable()) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("current", plugin.getDescription().getVersion());
                    placeholders.put("latest", updateInfo.getLatestVersion());
                    plugin.getLogger().info(plugin.getLanguageManager()
                        .getMessage("messages.plugin.update.available", placeholders));
                } else {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("version", plugin.getDescription().getVersion());
                    plugin.getLogger().info(plugin.getLanguageManager()
                        .getMessage("messages.plugin.update.up-to-date", placeholders));
                }
            } catch (Exception e) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("error", e.getMessage());
                plugin.getLogger().warning(plugin.getLanguageManager()
                    .getMessage("messages.plugin.update.check-failed", placeholders));
                if (plugin.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("update-source.enabled", true);
        checkInterval = plugin.getConfig().getLong("update-source.check-interval", 86400) * 1000L;
        lastCheck = 0;
    }
} 