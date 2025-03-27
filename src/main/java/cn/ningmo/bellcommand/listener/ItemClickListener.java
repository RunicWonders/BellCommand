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

/**
 * 物品点击监听器
 * <p>
 * 处理玩家与命令物品的交互事件
 * 
 * @version 1.3.1 增加详细的动作类型冷却提示
 * @author NingMo
 */
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
            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                plugin.getLanguageManager().getMessage("messages.plugin.floodgate-detected")
            ));
        } else {
            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                plugin.getLanguageManager().getMessage("messages.plugin.floodgate-not-detected")
            ));
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                plugin.getLanguageManager().getMessage(
                    hasFloodgate ? "messages.debug.floodgate.detected" : "messages.debug.floodgate.not-detected"
                )
            ));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 检查是否是基岩版玩家
        if (hasFloodgate && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            if (plugin.isDebugEnabled()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.plugin.bedrock-player-detected", placeholders)
                ));
            }
        }

        // 检查物品是否为空
        if (item == null) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.debug.interact.no-item")
                ));
            }
            return;
        }

        // 获取命令物品
        CommandItem commandItem = itemManager.getCommandItem(item);
        if (commandItem == null) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.debug.interact.not-command-item")
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

        // 确定命令类型
        String commandType = determineCommandType(event, player);
        
        // 检查动作类型冷却
        if (!itemManager.checkActionCooldown(player, commandItem, commandType)) {
            if (plugin.isDebugEnabled()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                placeholders.put("item", commandItem.getId());
                placeholders.put("type", commandType);
                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.debug.command.action-cooldown-active", placeholders)
                ));
            }
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.format("%.1f", itemManager.getRemainingActionCooldown(player, commandItem, commandType)));
            placeholders.put("action", getActionDisplayName(commandType));
            player.sendMessage(ColorUtils.translateColors(
                plugin.getLanguageManager().getMessage("messages.command.action-cooldown", placeholders)
            ));
            player.sendMessage(ColorUtils.translateColors(
                plugin.getLanguageManager().getMessage("messages.command.other-actions-available")
            ));
            return;
        }
        
        // 全局冷却仍然保留，作为整体限制（全冷却模式）
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
            placeholders.put("time", String.format("%.1f", itemManager.getRemainingCooldown(player, commandItem)));
            player.sendMessage(ColorUtils.translateColors(
                plugin.getLanguageManager().getMessage("messages.command.cooldown", placeholders)
            ));
            return;
        }

        if (plugin.isDebugEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("type", commandType);
            placeholders.put("item", commandItem.getId());
            placeholders.put("action", event.getAction().toString());
            placeholders.put("sneaking", String.valueOf(player.isSneaking()));
            placeholders.put("is_bedrock", String.valueOf(hasFloodgate && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())));
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

    private String getActionDisplayName(String commandType) {
        // 获取当前语言环境下的动作名称翻译
        String actionKey = "action." + commandType.replace("-", "_");
        String translatedAction = plugin.getLanguageManager().getMessage("messages." + actionKey);
        
        // 如果获取到的是错误消息(表示没有对应的翻译)，则使用默认的英文显示
        if (translatedAction.startsWith("§c缺少语言键")) {
            switch (commandType) {
                case "left-click":
                    return "左键点击 (Left Click)";
                case "right-click":
                    return "右键点击 (Right Click)";
                case "shift-left-click":
                    return "蹲下左键 (Shift+Left Click)";
                case "shift-right-click": 
                    return "蹲下右键 (Shift+Right Click)";
                case "bedrock-left-click":
                    return "基岩版左键 (Bedrock Left)";
                case "bedrock-right-click":
                    return "基岩版右键 (Bedrock Right)";
                case "bedrock-shift-left-click":
                    return "基岩版蹲下左键 (Bedrock Shift+Left)";
                case "bedrock-shift-right-click":
                    return "基岩版蹲下右键 (Bedrock Shift+Right)";
                default:
                    return commandType;
            }
        }
        
        return translatedAction;
    }
} 