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
            plugin.getLogger().info(plugin.getLanguageManager().getMessage("messages.plugin.floodgate-detected"));
        } catch (ClassNotFoundException e) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info(plugin.getLanguageManager().getMessage("messages.debug.floodgate.not-detected"));
            }
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
        
        // 检查是否是基岩版玩家
        if (hasFloodgate && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            if (plugin.isDebugEnabled()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                plugin.getLogger().info(plugin.getLanguageManager().getMessage(
                    "messages.plugin.bedrock-player-detected",
                    placeholders
                ));
            }
        }
        
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
        String commandType = determineCommandType(event, player);

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

    private String determineCommandType(PlayerInteractEvent event, Player player) {
        boolean isLeftClick = event.getAction() == Action.LEFT_CLICK_AIR || 
                            event.getAction() == Action.LEFT_CLICK_BLOCK;
        boolean isShifting = player.isSneaking();
        boolean isBedrockPlayer = hasFloodgate && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());

        if (isBedrockPlayer) {
            if (isShifting) {
                return isLeftClick ? "bedrock-shift-left-click" : "bedrock-shift-right-click";
            } else {
                return isLeftClick ? "bedrock-left-click" : "bedrock-right-click";
            }
        } else {
            if (isShifting) {
                return isLeftClick ? "shift-left-click" : "shift-right-click";
            } else {
                return isLeftClick ? "left-click" : "right-click";
            }
        }
    }
} 