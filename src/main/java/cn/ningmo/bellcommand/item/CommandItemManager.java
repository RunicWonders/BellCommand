package cn.ningmo.bellcommand.item;

import cn.ningmo.bellcommand.BellCommand;
import cn.ningmo.bellcommand.utils.ColorUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Material;
import org.bukkit.Statistic;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandItemManager {
    private final BellCommand plugin;
    private final Map<String, CommandItem> items;
    private final Map<String, Map<String, Integer>> cooldowns; // playerId -> (itemId -> lastUseTick)
    private BukkitTask cleanupTask;

    public CommandItemManager(BellCommand plugin) {
        this.plugin = plugin;
        this.items = new HashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
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
    }

    public void reload() {
        items.clear();
        loadItems();
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

        if (plugin.isDebugEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("count", String.valueOf(items.size()));
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

        for (CommandItem.CommandEntry command : commands) {
            String processedCommand = null;
            try {
                processedCommand = command.getCommand()
                    .replace("%player%", player.getName())
                    .replace("%uuid%", player.getUniqueId().toString());

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

    private void cleanupCooldowns() {
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
    }

    public void resetCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId().toString());
    }

    public void resetCooldown(Player player, CommandItem item) {
        Map<String, Integer> playerCooldowns = cooldowns.get(player.getUniqueId().toString());
        if (playerCooldowns != null) {
            playerCooldowns.remove(item.getId());
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(player.getUniqueId().toString());
            }
        }
    }
} 