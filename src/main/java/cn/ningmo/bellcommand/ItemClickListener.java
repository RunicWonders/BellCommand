package cn.ningmo.bellcommand;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import cn.ningmo.bellcommand.item.CommandItem;
import cn.ningmo.bellcommand.item.CommandItemManager;
import java.util.HashMap;
import java.util.Map;

public class ItemClickListener implements Listener {
    private final BellCommand plugin;
    private final CommandItemManager itemManager;
    private final boolean hasFloodgate;

    public ItemClickListener(BellCommand plugin, CommandItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        
        // 检测 Floodgate
        boolean detected = false;
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            detected = true;
            plugin.getLogger().info(plugin.getLanguageManager().getMessage("messages.debug.floodgate.detected"));
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info(plugin.getLanguageManager().getMessage("messages.debug.floodgate.not-detected"));
        }
        this.hasFloodgate = detected;
        
        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info(plugin.getLanguageManager().getMessage("messages.debug.listener.initialized"));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info(String.format(
                plugin.getLanguageManager().getMessage("messages.debug.interaction.detected"),
                player.getName()
            ));
        }

        // 检查物品是否为空
        if (item == null) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info(plugin.getLanguageManager().getMessage("messages.debug.interaction.no-item"));
            }
            return;
        }

        // 获取命令物品
        CommandItem commandItem = itemManager.getCommandItem(item);
        if (commandItem == null) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info(plugin.getLanguageManager().getMessage("messages.debug.interaction.not-command-item"));
            }
            return;
        }

        // 检查权限
        if (!commandItem.getPermission().isEmpty() && !player.hasPermission(commandItem.getPermission())) {
            if (plugin.isDebugEnabled()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                placeholders.put("permission", commandItem.getPermission());
                plugin.getLogger().info(plugin.getLanguageManager().getMessage(
                    "messages.debug.permission.denied", 
                    placeholders
                ));
            }
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.error.no-permission-use"));
            return;
        }

        // 检查冷却
        if (!itemManager.checkCooldown(player, commandItem)) {
            if (plugin.isDebugEnabled()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                placeholders.put("item", commandItem.getId());
                plugin.getLogger().info(plugin.getLanguageManager().getMessage(
                    "messages.debug.cooldown.active", 
                    placeholders
                ));
            }
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(itemManager.getRemainingCooldown(player, commandItem)));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.command.cooldown", placeholders));
            return;
        }

        // 确定命令类型
        String commandType;
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (player.isSneaking()) {
                commandType = "shift-left-click";
            } else {
                commandType = hasFloodgate && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()) 
                    ? "bedrock-left-click" 
                    : "left-click";
            }
        } else {
            if (player.isSneaking()) {
                commandType = "shift-right-click";
            } else {
                commandType = hasFloodgate && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())
                    ? "bedrock-right-click"
                    : "right-click";
            }
        }

        if (plugin.isDebugEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("type", commandType);
            placeholders.put("item", commandItem.getId());
            plugin.getLogger().info(plugin.getLanguageManager().getMessage(
                "messages.debug.command.executing", 
                placeholders
            ));
        }

        // 执行命令
        itemManager.executeCommands(player, commandItem, commandType);
        event.setCancelled(true);
    }
} 