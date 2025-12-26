package cn.ningmo.bellcommand.item;

import cn.ningmo.bellcommand.BellCommand;
import cn.ningmo.bellcommand.utils.ColorUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Material;
import org.bukkit.Statistic;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令物品管理器类
 * <p>
 * 负责加载、管理命令物品及其冷却时间
 * 
 * @version 1.3.1 新增动作类型独立冷却功能，不同交互方式（左键/右键等）分别记录冷却时间
 * @author NingMo
 */
public class CommandItemManager {
    private final BellCommand plugin;
    private final Map<String, CommandItem> items;
    // 全局冷却 playerId -> (itemId -> lastUseTick)
    private final Map<String, Map<String, Integer>> cooldowns; 
    // 按动作类型冷却 playerId -> (itemId_actionType -> lastUseTick)
    private final Map<String, Map<String, Integer>> actionCooldowns;
    private BukkitTask cleanupTask;

    public CommandItemManager(BellCommand plugin) {
        this.plugin = plugin;
        this.items = new HashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
        this.actionCooldowns = new ConcurrentHashMap<>();
        loadItems();
        startCleanupTask();
    }

    private void startCleanupTask() {
        // 每5分钟清理一次过期的冷却数据
        cleanupTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
            plugin,
            this::cleanupCooldowns,
            6000L, // 5分钟 = 300秒 = 6000tick
            6000L
        );
    }

    public void disable() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        items.clear();
        cooldowns.clear();
        actionCooldowns.clear();
    }

    public void reload() {
        items.clear();
        loadItems();
    }

    private void loadItems() {
        items.clear();
        
        // 获取所有配置文件
        Map<String, FileConfiguration> itemConfigs = 
            plugin.getConfigurationManager().getItemConfigs();
        
        int totalItems = 0;
        
        for (Map.Entry<String, FileConfiguration> entry : itemConfigs.entrySet()) {
            String configName = entry.getKey();
            FileConfiguration config = entry.getValue();
            
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            if (itemsSection == null) {
                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().info(ColorUtils.translateConsoleColors(
                        "&e[调试] 配置文件 " + configName + " 中没有物品配置"
                    ));
                }
                continue;
            }

            for (String id : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(id);
                if (itemSection != null) {
                    try {
                        CommandItem item = new CommandItem(id, itemSection);
                        items.put(id, item);
                        totalItems++;
                        
                        if (plugin.isDebugEnabled()) {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("id", id);
                            placeholders.put("name", item.getName());
                            placeholders.put("type", itemSection.getString("item-id", "CLOCK"));
                            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                                plugin.getLanguageManager().getMessage("messages.debug.config.item-entry", placeholders)
                            ));
                        }
                    } catch (Exception e) {
                        if (plugin.isDebugEnabled()) {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("id", id);
                            placeholders.put("error", e.getMessage());
                            plugin.getLogger().warning(ColorUtils.translateConsoleColors(
                                plugin.getLanguageManager().getMessage("messages.error.invalid-config", placeholders)
                            ));
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        if (plugin.isDebugEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("count", String.valueOf(totalItems));
            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                plugin.getLanguageManager().getMessage("messages.debug.config.items-loaded", placeholders)
            ));
        }
    }

    public CommandItem getItem(String id) {
        return items.get(id);
    }

    public Collection<CommandItem> getAllItems() {
        return items.values();
    }

    public void executeCommands(Player player, CommandItem item, String type) {
        List<CommandItem.CommandEntry> commands = item.getCommands(type);
        if (commands == null || commands.isEmpty()) {
            return;
        }
        
        // 设置该动作类型的冷却
        String playerId = player.getUniqueId().toString();
        String actionKey = item.getId() + "_" + type;
        Map<String, Integer> playerActionCooldowns = actionCooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        playerActionCooldowns.put(actionKey, player.getStatistic(Statistic.PLAY_ONE_MINUTE));
        
        // 设置全局冷却（保持原有功能）
        Map<String, Integer> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        playerCooldowns.put(item.getId(), player.getStatistic(Statistic.PLAY_ONE_MINUTE));

        for (CommandItem.CommandEntry command : commands) {
            final String processedCommand = command.getCommand()
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString());

            // 检查是否需要延迟执行
            if (command.getDelay() > 0) {
                // 计算延迟的tick数（1秒 = 20tick）
                long delayTicks = (long) (command.getDelay() * 20);
                
                // 使用BukkitScheduler延迟执行命令
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    try {
                        boolean success;
                        if (command.isAsConsole()) {
                            success = plugin.getServer().dispatchCommand(
                                plugin.getServer().getConsoleSender(),
                                processedCommand
                            );
                        } else {
                            // 检查玩家是否仍在线
                            if (!player.isOnline()) {
                                if (plugin.isDebugEnabled()) {
                                    plugin.getLogger().info(ColorUtils.translateConsoleColors(
                                        plugin.getLanguageManager().getMessage("messages.debug.command.player-offline")
                                    ));
                                }
                                return;
                            }
                            success = plugin.getServer().dispatchCommand(
                                player,
                                processedCommand
                            );
                        }

                        if (plugin.isDebugEnabled()) {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("command", processedCommand);
                            placeholders.put("result", success ? "success" : "failed");
                            placeholders.put("delay", String.valueOf(command.getDelay()));
                            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                                plugin.getLanguageManager().getMessage("messages.debug.command.delayed-command-result", placeholders)
                            ));
                        }
                    } catch (Exception e) {
                        if (plugin.isDebugEnabled()) {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("command", processedCommand);
                            placeholders.put("error", e.getMessage());
                            plugin.getLogger().warning(ColorUtils.translateConsoleColors(
                                plugin.getLanguageManager().getMessage("messages.error.command-error", placeholders)
                            ));
                            e.printStackTrace();
                        }
                    }
                }, delayTicks);
            } else {
                // 立即执行命令
                try {
                    boolean success;
                    if (command.isAsConsole()) {
                        success = plugin.getServer().dispatchCommand(
                            plugin.getServer().getConsoleSender(),
                            processedCommand
                        );
                    } else {
                        success = plugin.getServer().dispatchCommand(
                            player,
                            processedCommand
                        );
                    }

                    if (plugin.isDebugEnabled()) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("command", processedCommand);
                        placeholders.put("result", success ? "success" : "failed");
                        plugin.getLogger().info(ColorUtils.translateConsoleColors(
                            plugin.getLanguageManager().getMessage("messages.debug.command.command-result", placeholders)
                        ));
                    }
                } catch (Exception e) {
                    if (plugin.isDebugEnabled()) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("command", processedCommand != null ? processedCommand : "unknown");
                        placeholders.put("error", e.getMessage());
                        plugin.getLogger().warning(ColorUtils.translateConsoleColors(
                            plugin.getLanguageManager().getMessage("messages.error.command-error", placeholders)
                        ));
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public CommandItem getCommandItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }

        for (CommandItem item : items.values()) {
            if (item.matches(itemStack)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 检查物品的全局冷却状态
     * @param player 玩家
     * @param item 命令物品
     * @return 是否可以使用
     */
    public boolean checkCooldown(Player player, CommandItem item) {
        if (item.getCooldown() <= 0) {
            return true;
        }

        String playerId = player.getUniqueId().toString();
        Map<String, Integer> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        Integer lastUseTick = playerCooldowns.get(item.getId());

        if (lastUseTick == null) {
            playerCooldowns.put(item.getId(), player.getStatistic(Statistic.PLAY_ONE_MINUTE));
            return true;
        }

        int currentTick = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        int ticksPassed = currentTick - lastUseTick;
        int cooldownTicks = (int)(item.getCooldown() * 20);
        if (ticksPassed >= cooldownTicks) {
            playerCooldowns.put(item.getId(), currentTick);
            return true;
        }

        return false;
    }

    /**
     * 检查物品特定动作类型的冷却状态
     * @param player 玩家
     * @param item 命令物品
     * @param actionType 动作类型
     * @return 是否可以使用
     */
    public boolean checkActionCooldown(Player player, CommandItem item, String actionType) {
        if (item.getCooldown() <= 0) {
            return true;
        }

        String playerId = player.getUniqueId().toString();
        String actionKey = item.getId() + "_" + actionType;
        Map<String, Integer> playerActionCooldowns = actionCooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        Integer lastUseTick = playerActionCooldowns.get(actionKey);

        if (lastUseTick == null) {
            return true;
        }

        int currentTick = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        int ticksPassed = currentTick - lastUseTick;
        int cooldownTicks = (int)(item.getCooldown() * 20);
        if (ticksPassed >= cooldownTicks) {
            return true;
        }

        if (plugin.isDebugEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("item", item.getId());
            placeholders.put("action", actionType);
            placeholders.put("last_use", String.valueOf(lastUseTick));
            placeholders.put("current", String.valueOf(currentTick));
            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                plugin.getLanguageManager().getMessage("messages.debug.cooldown.check-action", placeholders)
            ));
        }

        return false;
    }

    /**
     * 获取物品的全局剩余冷却时间
     */
    public double getRemainingCooldown(Player player, CommandItem item) {
        String playerId = player.getUniqueId().toString();
        Map<String, Integer> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return 0;
        }

        Integer lastUseTick = playerCooldowns.get(item.getId());
        if (lastUseTick == null) {
            return 0;
        }

        int currentTick = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        int ticksPassed = currentTick - lastUseTick;
        int cooldownTicks = (int)(item.getCooldown() * 20);
        int remainingTicks = cooldownTicks - ticksPassed;

        return remainingTicks > 0 ? Math.round(remainingTicks / 20.0 * 10.0) / 10.0 : 0;
    }

    /**
     * 获取物品特定动作类型的剩余冷却时间
     */
    public double getRemainingActionCooldown(Player player, CommandItem item, String actionType) {
        String playerId = player.getUniqueId().toString();
        Map<String, Integer> playerActionCooldowns = actionCooldowns.get(playerId);
        if (playerActionCooldowns == null) {
            return 0;
        }

        String actionKey = item.getId() + "_" + actionType;
        Integer lastUseTick = playerActionCooldowns.get(actionKey);
        if (lastUseTick == null) {
            return 0;
        }

        int currentTick = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        int ticksPassed = currentTick - lastUseTick;
        int cooldownTicks = (int)(item.getCooldown() * 20);
        int remainingTicks = cooldownTicks - ticksPassed;

        return remainingTicks > 0 ? Math.round(remainingTicks / 20.0 * 10.0) / 10.0 : 0;
    }

    private void cleanupCooldowns() {
        // 清理全局冷却
        cooldowns.forEach((playerId, playerCooldowns) -> {
            playerCooldowns.entrySet().removeIf(entry -> {
                CommandItem item = getItem(entry.getKey());
                if (item == null) {
                    return true;
                }
                Player player = plugin.getServer().getPlayer(UUID.fromString(playerId));
                if (player == null || !player.isOnline()) {
                    return true;
                }
                int ticksPassed = player.getStatistic(Statistic.PLAY_ONE_MINUTE) - entry.getValue();
                return ticksPassed >= item.getCooldown() * 20;
            });
        });
        cooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // 清理动作类型冷却
        actionCooldowns.forEach((playerId, playerActionCooldowns) -> {
            playerActionCooldowns.entrySet().removeIf(entry -> {
                String[] parts = entry.getKey().split("_", 2);
                if (parts.length < 2) {
                    return true; // 无效的格式，清理掉
                }
                
                String itemId = parts[0];
                CommandItem item = getItem(itemId);
                if (item == null) {
                    return true;
                }
                
                Player player = plugin.getServer().getPlayer(UUID.fromString(playerId));
                if (player == null || !player.isOnline()) {
                    return true;
                }
                
                int ticksPassed = player.getStatistic(Statistic.PLAY_ONE_MINUTE) - entry.getValue();
                return ticksPassed >= item.getCooldown() * 20;
            });
        });
        actionCooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public void resetCooldowns(Player player) {
        String playerId = player.getUniqueId().toString();
        cooldowns.remove(playerId);
        actionCooldowns.remove(playerId);
    }

    public void resetCooldown(Player player, CommandItem item) {
        String playerId = player.getUniqueId().toString();
        
        // 重置全局冷却
        Map<String, Integer> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(item.getId());
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(playerId);
            }
        }
        
        // 重置所有动作冷却
        Map<String, Integer> playerActionCooldowns = actionCooldowns.get(playerId);
        if (playerActionCooldowns != null) {
            // 移除所有以物品ID开头的动作冷却
            playerActionCooldowns.entrySet().removeIf(entry -> entry.getKey().startsWith(item.getId() + "_"));
            if (playerActionCooldowns.isEmpty()) {
                actionCooldowns.remove(playerId);
            }
        }
        
        if (plugin.isDebugEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("item", item.getId());
            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                plugin.getLanguageManager().getMessage("messages.debug.cooldown.reset", placeholders)
            ));
        }
    }
    
    /**
     * 重置特定动作类型的冷却
     */
    public void resetActionCooldown(Player player, CommandItem item, String actionType) {
        String playerId = player.getUniqueId().toString();
        Map<String, Integer> playerActionCooldowns = actionCooldowns.get(playerId);
        if (playerActionCooldowns != null) {
            String actionKey = item.getId() + "_" + actionType;
            playerActionCooldowns.remove(actionKey);
            if (playerActionCooldowns.isEmpty()) {
                actionCooldowns.remove(playerId);
            }
        }
    }
}