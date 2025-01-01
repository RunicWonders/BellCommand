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

public class ItemClickListener implements Listener {
    private final BellCommand plugin;
    private final CommandItemManager itemManager;
    private final boolean hasFloodgate;

    public ItemClickListener(BellCommand plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        this.hasFloodgate = plugin.getServer().getPluginManager().getPlugin("floodgate") != null;
        if (hasFloodgate && plugin.isDebugEnabled()) {
            plugin.getLogger().info("已检测到 Floodgate，将启用基岩版支持");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        CommandItem commandItem = itemManager.getCommandItem(item);
        if (commandItem == null) {
            return;
        }

        // 检查权限
        if (!player.hasPermission(commandItem.getPermission())) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.error.no-permission"));
            return;
        }

        // 检查冷却
        if (!itemManager.checkCooldown(player, commandItem)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(itemManager.getRemainingCooldown(player, commandItem)));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.error.cooldown", placeholders));
            return;
        }

        // 确定命令类型
        String commandType = determineCommandType(event, player);
        
        // 执行命令
        itemManager.executeCommands(player, commandItem, commandType);
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