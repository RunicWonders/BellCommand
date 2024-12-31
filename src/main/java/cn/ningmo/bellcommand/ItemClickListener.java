package cn.ningmo.bellcommand;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        CommandItem cmdItem = null;
        
        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info("§e[调试] 玩家 " + player.getName() + " 触发了交互事件");
            plugin.getLogger().info("§e[调试] 交互类型: " + event.getAction().name());
            if (event.getItem() != null) {
                plugin.getLogger().info("§e[调试] 手持物品: " + event.getItem().getType().name());
            }
        }
        
        // 检查玩家状态
        if (!player.isOnline() || player.isDead()) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info("§c[调试] 玩家状态无效: " + (player.isDead() ? "已死亡" : "离线"));
            }
            return;
        }
        
        // 使用同步锁防止并发问题
        synchronized (player.getUniqueId().toString().intern()) {
            try {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    if (plugin.isDebugEnabled()) {
                        plugin.getLogger().info("§c[调试] 手持物品为空");
                    }
                    return;
                }
                
                cmdItem = plugin.getItemManager().getItem(item);
                if (cmdItem == null) {
                    if (plugin.isDebugEnabled()) {
                        plugin.getLogger().info("§c[调试] 不是命令物品");
                    }
                    return;
                }

                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().info("§a[调试] 找到命令物品: " + cmdItem.getId());
                }

                event.setCancelled(true);
                
                // 防止命令快速点击
                if (plugin.getItemManager().isOnCooldown(player, cmdItem)) {
                    int remaining = plugin.getItemManager().getRemainingCooldown(player, cmdItem);
                    if (plugin.isDebugEnabled()) {
                        plugin.getLogger().info("§c[调试] 物品正在冷却中: 还剩 " + remaining + " 秒");
                    }
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
                
                List<String> commands = isRightClick ? 
                    cmdItem.getRightClickCommands() : cmdItem.getLeftClickCommands();

                if (commands.isEmpty()) {
                    if (plugin.isDebugEnabled()) {
                        plugin.getLogger().info("§c[调试] 命令列表为空");
                    }
                    return;
                }

                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().info("§a[调试] 准备执行命令:");
                    for (String cmd : commands) {
                        plugin.getLogger().info("§a[调试] - " + cmd);
                    }
                }

                // 更新冷却（在执行命令前更新，防止命令执行失败导致冷却不生效）
                plugin.getItemManager().updateCooldown(player, cmdItem);

                // 执行命令
                PerformanceMonitor.startTiming("command_execution");
                for (String command : commands) {
                    try {
                        if (plugin.isDebugEnabled()) {
                            plugin.getLogger().info("§a[调试] 正在执行命令: " + command);
                        }
                        boolean success = plugin.getServer().dispatchCommand(player, command);
                        if (plugin.isDebugEnabled()) {
                            plugin.getLogger().info("§a[调试] 命令执行" + (success ? "成功" : "失败"));
                        }
                    } catch (Exception e) {
                        if (plugin.isDebugEnabled()) {
                            plugin.getLogger().warning("§c[调试] 执行命令时出错: " + command);
                            e.printStackTrace();
                        }
                    }
                }
                long executionTime = PerformanceMonitor.endTiming("command_execution");
                PerformanceMonitor.logTiming("command_execution", executionTime, 50000000L, plugin, "命令执行时间过长");
                
            } catch (Exception e) {
                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().severe("§c[调试] 处理物品交互事件时出错");
                    e.printStackTrace();
                }
            }
        }
        
        if (cmdItem != null && plugin.isDebugEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", cmdItem.getId());
            placeholders.put("name", cmdItem.getName());
            plugin.getLogger().info(plugin.getLanguageManager().getMessage("messages.debug.interact.item-id", placeholders));
            plugin.getLogger().info(plugin.getLanguageManager().getMessage("messages.debug.interact.item-name", placeholders));
        }
    }
} 