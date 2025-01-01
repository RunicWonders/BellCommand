package cn.ningmo.bellcommand;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import cn.ningmo.bellcommand.item.CommandItem;
import cn.ningmo.bellcommand.utils.PerformanceMonitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemClickListener implements Listener {
    private final BellCommand plugin;

    public ItemClickListener(BellCommand plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        CommandItem cmdItem = plugin.getItemManager().getItem(item);
        if (cmdItem == null) {
            return;
        }

        // 取消事件，防止方块放置等
        event.setCancelled(true);

        try {
            // 检查冷却
            if (plugin.getItemManager().isOnCooldown(player, cmdItem)) {
                int remaining = plugin.getItemManager().getRemainingCooldown(player, cmdItem);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("seconds", String.valueOf(remaining));
                player.sendMessage(plugin.getLanguageManager().getMessage(
                    "messages.command.cooldown", placeholders));
                return;
            }
            
            // 检查权限
            if (!plugin.getItemManager().canUseItem(player, cmdItem)) {
                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().info("§c[调试] 玩家没有使用权限");
                }
                player.sendMessage(plugin.getLanguageManager().getMessage(
                    "messages.error.no-permission-use"));
                return;
            }

            // 执行命令
            boolean isRightClick = (event.getAction() == Action.RIGHT_CLICK_AIR 
                || event.getAction() == Action.RIGHT_CLICK_BLOCK);
            
            List<CommandItem.CommandEntry> commands = isRightClick ?
                cmdItem.getRightClickCommands() : cmdItem.getLeftClickCommands();

            if (commands.isEmpty()) {
                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().info("§c[调试] 命令列表为空");
                }
                return;
            }

            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info("§a[调试] 准备执行命令:");
                for (CommandItem.CommandEntry cmd : commands) {
                    plugin.getLogger().info("§a[调试] - " + cmd.getCommand());
                }
            }

            // 更新冷却（在执行命令前更新，防止命令执行失败导致冷却不生效）
            plugin.getItemManager().updateCooldown(player, cmdItem);

            // 执行命令
            PerformanceMonitor.startTiming("command_execution");
            for (CommandItem.CommandEntry command : commands) {
                try {
                    if (plugin.isDebugEnabled()) {
                        plugin.getLogger().info("§a[调试] 正在执行命令: " + command.getCommand());
                    }

                    boolean success;
                    if (command.isAsConsole()) {
                        success = plugin.getServer().dispatchCommand(
                            plugin.getServer().getConsoleSender(), 
                            command.getCommand().replace("%player%", player.getName())
                        );
                    } else {
                        success = plugin.getServer().dispatchCommand(
                            player, 
                            command.getCommand()
                        );
                    }

                    if (plugin.isDebugEnabled()) {
                        plugin.getLogger().info("§a[调试] 命令执行" + (success ? "成功" : "失败"));
                    }
                } catch (Exception e) {
                    if (plugin.isDebugEnabled()) {
                        plugin.getLogger().warning("§c[调试] 执行命令时出错: " + command.getCommand());
                        e.printStackTrace();
                    }
                }
            }
            long executionTime = PerformanceMonitor.endTiming("command_execution");
            PerformanceMonitor.logTiming("command_execution", executionTime, 50000000L, plugin, "命令执行时间过长");
            
        } catch (Exception e) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().warning("§c[调试] 处理物品交互时出错");
                e.printStackTrace();
            }
        }
    }
} 