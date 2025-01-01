package cn.ningmo.bellcommand.item;

import cn.ningmo.bellcommand.BellCommand;
import cn.ningmo.bellcommand.utils.ColorUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandItemManager {
    private final BellCommand plugin;
    private final Map<String, CommandItem> items;
    private final Map<String, Map<String, Long>> cooldowns; // playerId -> (itemId -> lastUseTime)
    private BukkitTask cleanupTask;

    public CommandItemManager(BellCommand plugin) {
        this.plugin = plugin;
        this.items = new HashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
        loadItems();
        startCleanupTask();
    }

    private void loadItems() {
        items.clear();
        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("items");
        if (itemsSection == null) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().warning(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.debug.config.no-items")
                ));
            }
            return;
        }

        if (plugin.isDebugEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("count", String.valueOf(itemsSection.getKeys(false).size()));
            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                plugin.getLanguageManager().getMessage("messages.debug.config.items-loaded", placeholders)
            ));
        }

        for (String id : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(id);
            if (itemSection != null) {
                try {
                    CommandItem item = new CommandItem(id, itemSection);
                    items.put(id, item);
                    
                    if (plugin.isDebugEnabled()) {
                        plugin.getLogger().info("已加载物品: " + id + " (" + item.getName() + ")");
                    }
                } catch (Exception e) {
                    if (plugin.isDebugEnabled()) {
                        plugin.getLogger().warning("加载物品 " + id + " 时出错: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }

        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info("命令物品加载完成，共 " + items.size() + " 个物品");
        }
    }

    public CommandItem getItem(String id) {
        return items.get(id);
    }

    public CommandItem getItem(ItemStack item) {
        if (item == null) return null;
        return items.values().stream()
            .filter(cmdItem -> cmdItem.matches(item))
            .findFirst()
            .orElse(null);
    }

    public Collection<CommandItem> getAllItems() {
        return Collections.unmodifiableCollection(items.values());
    }

    public boolean canUseItem(Player player, CommandItem item) {
        // 如果玩家有全部权限，直接返回true
        if (player.hasPermission("bellcommand.*")) {
            return true;
        }
        
        // 如果玩家有所有物品的使用权限，直接返回true
        if (player.hasPermission("bellcommand.item.*")) {
            return true;
        }
        
        // 检查基础命令权限
        if (!player.hasPermission("bellcommand.clock")) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info("玩家 " + player.getName() + " 缺少基础命令权限: bellcommand.clock");
            }
            return false;
        }
        
        // 检查特定物品权限
        if (!item.getPermission().isEmpty()) {
            boolean hasPermission = player.hasPermission(item.getPermission());
            if (plugin.isDebugEnabled() && !hasPermission) {
                plugin.getLogger().info("玩家 " + player.getName() + " 缺少物品权限: " + item.getPermission());
            }
            return hasPermission;
        }
        
        return true;
    }

    public boolean isOnCooldown(Player player, CommandItem item) {
        if (item.getCooldown() <= 0) return false;

        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(
            player.getUniqueId().toString(), k -> new ConcurrentHashMap<>());
        
        Long lastUse = playerCooldowns.get(item.getId());
        if (lastUse == null) return false;

        long cooldownEnd = lastUse + (item.getCooldown() * 1000L);
        return System.currentTimeMillis() < cooldownEnd;
    }

    public int getRemainingCooldown(Player player, CommandItem item) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId().toString());
        if (playerCooldowns == null) return 0;

        Long lastUse = playerCooldowns.get(item.getId());
        if (lastUse == null) return 0;

        long cooldownEnd = lastUse + (item.getCooldown() * 1000L);
        long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
        return remaining > 0 ? (int) remaining : 0;
    }

    public void updateCooldown(Player player, CommandItem item) {
        if (item.getCooldown() <= 0) return;
        
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(
            player.getUniqueId().toString(), k -> new ConcurrentHashMap<>());
        
        playerCooldowns.put(item.getId(), System.currentTimeMillis());
    }

    public void reload() {
        loadItems();
        startCleanupTask();
    }

    public void disable() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        cleanupCooldowns();
        items.clear();
        cooldowns.clear();
    }

    public void cleanupCooldowns() {
        long now = System.currentTimeMillis();
        cooldowns.forEach((playerId, playerCooldowns) -> {
            playerCooldowns.entrySet().removeIf(entry -> {
                CommandItem item = items.get(entry.getKey());
                if (item == null) return true;
                
                long cooldownEnd = entry.getValue() + (item.getCooldown() * 1000L);
                return now >= cooldownEnd;
            });
            
            // 如果玩家没有任何冷却中的物品，移除该玩家的记录
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(playerId);
            }
        });
    }
    
    private void startCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        
        // 每5分钟清理一次过期的冷却时间
        cleanupTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, 
            this::cleanupCooldowns, 6000L, 6000L);
    }

    public void clearCache() {
        items.clear();
        cooldowns.clear();
    }
} 