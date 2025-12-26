package cn.ningmo.bellcommand.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import cn.ningmo.bellcommand.BellCommand;
import cn.ningmo.bellcommand.item.CommandItem;
import cn.ningmo.bellcommand.item.CommandItemManager;
import cn.ningmo.bellcommand.utils.ColorUtils;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;

public class AutoGiveListener implements Listener {
    private final BellCommand plugin;
    private final CommandItemManager itemManager;

    public AutoGiveListener(BellCommand plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean isFirstJoin = !player.hasPlayedBefore();
        
        // 遍历所有物品，检查自动给予设置
        for (CommandItem item : itemManager.getAllItems()) {
            CommandItem.AutoGiveConfig config = item.getAutoGive();
            
            boolean shouldGive = false;
            if (config.isFirstJoin() && isFirstJoin) {
                shouldGive = true;
            } else if (config.isJoin()) {
                shouldGive = true;
            }

            if (shouldGive) {
                giveItemIfNotExists(player, item);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // 延迟给予物品以确保玩家完全重生
        new BukkitRunnable() {
            @Override
            public void run() {
                for (CommandItem item : itemManager.getAllItems()) {
                    if (item.getAutoGive().isRespawn()) {
                        giveItemIfNotExists(player, item);
                    }
                }
            }
        }.runTaskLater(plugin, 20L); // 1秒后给予物品
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        CommandItem commandItem = itemManager.getCommandItem(droppedItem);
        
        if (commandItem != null && commandItem.getAutoCleanup().isEnabled()) {
            int delay = commandItem.getAutoCleanup().getDelay() * 20; // 转换为tick
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (event.getItemDrop().isValid()) {
                        event.getItemDrop().remove();
                        if (plugin.isDebugEnabled()) {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("player", event.getPlayer().getName());
                            placeholders.put("item", commandItem.getId());
                            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                                plugin.getLanguageManager().getMessage("messages.debug.item.removed", placeholders)
                            ));
                        }
                    }
                }
            }.runTaskLater(plugin, delay);
        }
    }

    private void giveItemIfNotExists(Player player, CommandItem item) {
        // 检查玩家背包中是否已有这个物品
        boolean hasItem = false;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && item.matches(invItem)) {
                hasItem = true;
                break;
            }
        }

        // 只有当玩家没有这个物品时才给予
        if (!hasItem) {
            ItemStack itemStack = item.createItemStack();
            // 检查背包是否已满
            if (player.getInventory().firstEmpty() == -1) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("item", item.getName());
                player.sendMessage(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.inventory-full", placeholders)
                ));
                return;
            }
            
            player.getInventory().addItem(itemStack);

            if (plugin.isDebugEnabled()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                placeholders.put("item", item.getName());
                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.debug.item.given", placeholders)
                ));
            }
        }
    }
}
