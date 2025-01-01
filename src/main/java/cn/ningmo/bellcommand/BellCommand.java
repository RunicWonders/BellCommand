package cn.ningmo.bellcommand;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import cn.ningmo.bellcommand.item.CommandItem;
import java.util.HashMap;
import java.util.Map;
import cn.ningmo.bellcommand.item.CommandItemManager;
import cn.ningmo.bellcommand.update.UpdateManager;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.bukkit.Bukkit;
import java.util.stream.Collectors;

public class BellCommand extends JavaPlugin {

    private FileConfiguration config;
    private boolean debug;
    private LanguageManager languageManager;
    private CommandItemManager itemManager;
    private UpdateManager updateManager;

    @Override
    public void onEnable() {
        if (!checkServerVersion()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // 初始化语言管理器
        languageManager = new LanguageManager(this);
        
        // 加载配置文件
        loadConfiguration();
        
        // 初始化物品管理器
        itemManager = new CommandItemManager(this);
        
        // 注册命令和监听器
        if (getCommand("clock") != null) {
            getCommand("clock").setExecutor(this::onCommand);
        }
        if (getCommand("clockremove") != null) {
            getCommand("clockremove").setExecutor(this::onRemoveCommand);
        }
        if (getCommand("bellcommand") != null) {
            getCommand("bellcommand").setExecutor(this::onBellCommand);
        }
        getServer().getPluginManager().registerEvents(new ItemClickListener(this), this);
        
        // 输出启动信息
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("version", getDescription().getVersion());
        getLogger().info(languageManager.getMessage("messages.plugin.enable", placeholders));
        
        if (debug) {
            getLogger().info(languageManager.getMessage("messages.plugin.debug-enabled"));
        }
        
        if (getServer().getName().toLowerCase().contains("nukkit") || 
            getServer().getName().toLowerCase().contains("bedrock")) {
            getLogger().info(languageManager.getMessage("messages.plugin.bedrock-detected"));
        }
        
        // 初始化更新管理器
        updateManager = new UpdateManager(this);
        updateManager.checkForUpdates();
    }

    @Override
    public void onDisable() {
        // 先保存日志消息
        String disableMessage = languageManager != null ? 
            languageManager.getMessage("messages.plugin.disable") : 
            "BellCommand 插件已关闭!";
        
        // 清理资源
        if (itemManager != null) {
            itemManager.cleanupCooldowns();
        }
        
        // 清空缓存
        itemManager = null;
        config = null;
        languageManager = null;
        
        // 最后输出日志
        getLogger().info(disableMessage);
        
        updateManager = null;
    }

    private void loadConfiguration() {
        try {
            // 备份旧配置
            if (getConfig().contains("config-version")) {
                File configFile = new File(getDataFolder(), "config.yml");
                File backupFile = new File(getDataFolder(), "config.backup.yml");
                if (configFile.exists()) {
                    Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            
            saveDefaultConfig();
            reloadConfig();
            config = getConfig();
            debug = config.getBoolean("debug", false);
            
            // 添加调试信息
            if (debug) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("count", String.valueOf(config.getConfigurationSection("items").getKeys(false).size()));
                getLogger().info(languageManager.getMessage("messages.debug.config.items-loaded", placeholders));
                
                // 输出每个物品的信息
                for (String id : config.getConfigurationSection("items").getKeys(false)) {
                    ConfigurationSection itemSection = config.getConfigurationSection("items." + id);
                    placeholders.clear();
                    placeholders.put("id", id);
                    placeholders.put("name", itemSection.getString("name"));
                    placeholders.put("type", itemSection.getString("item-id"));
                    getLogger().info(languageManager.getMessage("messages.debug.config.item-entry", placeholders));
                }
            }
            
            // 验证配置完整性
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            if (itemsSection == null) {
                getLogger().warning("配置文件中缺少items部分，将使用默认配置");
                saveResource("config.yml", true);
                reloadConfig();
                config = getConfig();
            }
            
            // 验证每个物品的配置
            if (itemsSection != null) {
                for (String id : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(id);
                    if (itemSection == null) continue;
                    
                    String itemId = itemSection.getString("item-id");
                    if (Material.matchMaterial(itemId) == null) {
                        getLogger().warning("无效的物品ID: " + itemId + " (在物品 " + id + " 中)");
                    }
                }
            }
            
            // 验证配置版本
            int configVersion = config.getInt("config-version", 0);
            if (configVersion < 1) {
                getLogger().warning("配置文件版本过低，将使用新版本配置");
                saveResource("config.yml", true);
                reloadConfig();
                config = getConfig();
            }
            
        } catch (Exception e) {
            getLogger().severe("加载配置文件时发生错误: " + e.getMessage());
            if (debug) {
                e.printStackTrace();
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getMessage("messages.command.player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("clock")) {
            if (args.length == 0) {
                // 显示可用物品列表
                player.sendMessage(languageManager.getMessage("messages.command.item-list"));
                for (CommandItem item : itemManager.getAllItems()) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("id", item.getId());
                    placeholders.put("name", item.getName());
                    player.sendMessage(languageManager.getMessage("messages.command.item-list-entry", placeholders));
                }
                player.sendMessage(languageManager.getMessage("messages.command.item-list-end"));
                return true;
            }

            String itemId = args[0].toLowerCase();
            CommandItem item = itemManager.getItem(itemId);
            
            if (item == null) {
                if (isDebugEnabled()) {
                    getLogger().warning("找不到物品ID: " + itemId);
                    getLogger().warning("可用物品列表: " + String.join(", ", itemManager.getAllItems().stream().map(CommandItem::getId).collect(Collectors.toList())));
                }
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("id", itemId);
                player.sendMessage(languageManager.getMessage("messages.command.invalid-item", placeholders));
                return true;
            }

            // 检查权限
            if (!itemManager.canUseItem(player, item)) {
                if (isDebugEnabled()) {
                    getLogger().warning("玩家 " + player.getName() + " 权限检查失败");
                    getLogger().warning("- bellcommand.*: " + player.hasPermission("bellcommand.*"));
                    getLogger().warning("- bellcommand.clock: " + player.hasPermission("bellcommand.clock"));
                    getLogger().warning("- bellcommand.item.*: " + player.hasPermission("bellcommand.item.*"));
                    if (!item.getPermission().isEmpty()) {
                        getLogger().warning("- " + item.getPermission() + ": " + 
                            player.hasPermission(item.getPermission()));
                    }
                }
                player.sendMessage(languageManager.getMessage("messages.error.no-permission-use"));
                return true;
            }

            // 检查是否已有该物品
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && item.matches(invItem)) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("item", item.getName());
                    player.sendMessage(languageManager.getMessage("messages.command.item-exists", placeholders));
                    return true;
                }
            }

            // 给予物品
            player.getInventory().addItem(item.createItemStack());
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("item", item.getName());
            player.sendMessage(languageManager.getMessage("messages.command.item-given", placeholders));

            if (debug) {
                placeholders.put("player", player.getName());
                getLogger().info(languageManager.getMessage("messages.debug.command.item-given", placeholders));
            }
        }
        return true;
    }

    public boolean onReloadCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("reload")) {
            return false;
        }
        
        if (!sender.hasPermission("bellcommand.reload")) {
            sender.sendMessage(languageManager.getMessage("messages.command.no-permission"));
            return true;
        }

        try {
            reloadConfig();
            loadConfiguration();
            // 重新加载语言文件
            languageManager.reload();
            // 重新加载物品管理器
            itemManager.reload();
            
            sender.sendMessage(languageManager.getMessage("messages.command.reload-success"));
            
            if (debug) {
                sender.sendMessage(languageManager.getMessage("messages.command.reload-debug"));
            }
        } catch (Exception e) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("error", e.getMessage());
            sender.sendMessage(languageManager.getMessage("messages.command.reload-failed"));
            getLogger().severe(languageManager.getMessage("messages.error.reload-error", placeholders));
            if (debug) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public boolean onRemoveCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getMessage("messages.command.player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            // 移除所有命令物品
            boolean removed = false;
            for (ItemStack item : player.getInventory().getContents()) {
                CommandItem cmdItem = itemManager.getItem(item);
                if (cmdItem != null) {
                    item.setAmount(0);
                    removed = true;
                    
                    if (debug) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("player", player.getName());
                        placeholders.put("item", cmdItem.getName());
                        getLogger().info(languageManager.getMessage("messages.debug.command.item-removed", placeholders));
                    }
                }
            }

            if (removed) {
                player.sendMessage(languageManager.getMessage("messages.command.item-removed"));
            } else {
                player.sendMessage(languageManager.getMessage("messages.command.no-item-found"));
            }
            return true;
        }

        // 移除指定物品
        String itemId = args[0].toLowerCase();
        CommandItem item = itemManager.getItem(itemId);
        
        if (item == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", itemId);
            player.sendMessage(languageManager.getMessage("messages.command.invalid-item", placeholders));
            return true;
        }

        boolean removed = false;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && item.matches(invItem)) {
                invItem.setAmount(0);
                removed = true;
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("item", item.getName());
                player.sendMessage(languageManager.getMessage("messages.command.item-removed", placeholders));
                
                if (debug) {
                    placeholders.put("player", player.getName());
                    getLogger().info(languageManager.getMessage("messages.debug.command.item-removed", placeholders));
                }
                break;
            }
        }

        if (!removed) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("item", item.getName());
            player.sendMessage(languageManager.getMessage("messages.command.no-item-found", placeholders));
        }

        return true;
    }

    public boolean onBellCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("bellcommand")) {
            return false;
        }
        
        if (args.length == 0) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("version", getDescription().getVersion());
            sender.sendMessage(languageManager.getMessage("messages.plugin.version", placeholders));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bellcommand.reload")) {
                sender.sendMessage(languageManager.getMessage("messages.command.no-permission"));
                return true;
            }

            try {
                reloadConfig();
                loadConfiguration();
                languageManager.reload();
                itemManager.reload();
                
                sender.sendMessage(languageManager.getMessage("messages.command.reload-success"));
                
                if (debug) {
                    sender.sendMessage(languageManager.getMessage("messages.command.reload-debug"));
                }
            } catch (Exception e) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("error", e.getMessage());
                sender.sendMessage(languageManager.getMessage("messages.command.reload-failed"));
                getLogger().severe(languageManager.getMessage("messages.error.reload-error", placeholders));
                if (debug) {
                    e.printStackTrace();
                }
            }
            updateManager.reload();
            return true;
        }
        
        return false;
    }

    // 用于其他类访问调试模式状态
    public boolean isDebugEnabled() {
        return debug;
    }

    // 获取语言管理器的方法
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public CommandItemManager getItemManager() {
        return itemManager;
    }

    private boolean checkServerVersion() {
        String version = Bukkit.getBukkitVersion();
        if (version.contains("1.8") || version.contains("1.7")) {
            getLogger().warning("不支持的服务器版本: " + version);
            getLogger().warning("请使用 1.13 或更高版本");
            return false;
        }
        return true;
    }
}
