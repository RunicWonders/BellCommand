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

public class AutoGiveListener implements Listener {
    private final BellCommand plugin;
    private final CommandItemManager itemManager;

    public AutoGiveListener(BellCommand plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("auto-give.enabled") || 
            !plugin.getConfig().getBoolean("auto-give.join")) {
            return;
        }

        Player player = event.getPlayer();
        giveItems(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean("auto-give.enabled") || 
            !plugin.getConfig().getBoolean("auto-give.respawn")) {
            return;
        }

        Player player = event.getPlayer();
        // 延迟给予物品以确保玩家完全重生
        new BukkitRunnable() {
            @Override
            public void run() {
                giveItems(player);
            }
        }.runTaskLater(plugin, 20L); // 1秒后给予物品
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!plugin.getConfig().getBoolean("auto-cleanup.enabled")) {
            return;
        }

        ItemStack droppedItem = event.getItemDrop().getItemStack();
        CommandItem commandItem = itemManager.getCommandItem(droppedItem);
        
        if (commandItem != null) {
            int delay = plugin.getConfig().getInt("auto-cleanup.delay", 30) * 20; // 转换为tick
            new BukkitRunnable() {
                @Override
                public void run() {
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
            }.runTaskLater(plugin, delay);
        }
    }

    private void giveItems(Player player) {
        List<String> itemIds = plugin.getConfig().getStringList("auto-give.items");
        for (String itemId : itemIds) {
            CommandItem item = itemManager.getItem(itemId);
            if (item != null) {
                ItemStack itemStack = item.createItemStack();
                player.getInventory().addItem(itemStack);
                
                if (plugin.isDebugEnabled()) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("player", player.getName());
                    placeholders.put("item", item.getName());
                    plugin.getLogger().info(plugin.getLanguageManager().getMessage("messages.auto-give", placeholders));
                }
            }
        }
    }
} 