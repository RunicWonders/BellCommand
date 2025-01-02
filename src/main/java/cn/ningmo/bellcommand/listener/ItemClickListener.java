package cn.ningmo.bellcommand.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import cn.ningmo.bellcommand.BellCommand;
import cn.ningmo.bellcommand.item.CommandItem;
import cn.ningmo.bellcommand.item.CommandItemManager;
import org.bukkit.event.block.Action;
import org.geysermc.floodgate.api.FloodgateApi;
import java.util.Map;
import java.util.HashMap;
import org.bukkit.event.EventPriority;
import cn.ningmo.bellcommand.utils.ColorUtils;

public class ItemClickListener implements Listener {
    private final BellCommand plugin;
    private final CommandItemManager itemManager;
    private final boolean hasFloodgate;

    public ItemClickListener(BellCommand plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        this.hasFloodgate = plugin.getServer().getPluginManager().getPlugin("floodgate") != null;
        
        // 输出 Floodgate 检测结果
        if (hasFloodgate) {
            plugin.getLogger().info(plugin.getLanguageManager().getMessage("messages.plugin.floodgate-detected"));
        } else {
            plugin.getLogger().info(plugin.getLanguageManager().getMessage("messages.plugin.floodgate-not-detected"));
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info(plugin.getLanguageManager().getMessage(
                hasFloodgate ? "messages.debug.floodgate.detected" : "messages.debug.floodgate.not-detected"
            ));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.debug.interaction.no-item")
                ));
            }
            return;
        }

        CommandItem commandItem = itemManager.getCommandItem(item);
        if (commandItem == null) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.debug.interaction.not-command-item")
                ));
            }
            return;
        }

        // 检查权限
        if (!commandItem.getPermission().isEmpty() && !player.hasPermission(commandItem.getPermission())) {
            if (plugin.isDebugEnabled()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                placeholders.put("permission", commandItem.getPermission());
                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.debug.permission.denied", placeholders)
                ));
            }
            player.sendMessage(ColorUtils.translateColors(
                plugin.getLanguageManager().getMessage("messages.error.no-permission-use")
            ));
            return;
        }

        // 检查冷却
        if (!itemManager.checkCooldown(player, commandItem)) {
            if (plugin.isDebugEnabled()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                placeholders.put("item", commandItem.getId());
                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.debug.cooldown.active", placeholders)
                ));
            }
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(itemManager.getRemainingCooldown(player, commandItem)));
            player.sendMessage(ColorUtils.translateColors(
                plugin.getLanguageManager().getMessage("messages.command.cooldown", placeholders)
            ));
            return;
        }

        // 确定命令类型
        String commandType = determineCommandType(event, player);
        
        if (plugin.isDebugEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("type", commandType);
            placeholders.put("item", commandItem.getId());
            placeholders.put("action", event.getAction().toString());
            placeholders.put("sneaking", String.valueOf(player.isSneaking()));
            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                plugin.getLanguageManager().getMessage("messages.debug.interact.event-start", placeholders)
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