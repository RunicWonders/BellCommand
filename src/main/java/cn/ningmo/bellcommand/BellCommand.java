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
import cn.ningmo.bellcommand.utils.ColorUtils;
import java.io.IOException;
import java.util.List;

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
        
        try {
            // 初始化语言管理器
            languageManager = new LanguageManager(this);
            
            // 加载配置文件
            loadConfiguration();
            
            // 初始化物品管理器
            itemManager = new CommandItemManager(this);
            
            // 注册命令和监听器
            registerCommands();
            getServer().getPluginManager().registerEvents(new ItemClickListener(this), this);
            
            // 输出启动信息
            logStartupInfo();
            
            // 初始化更新管理器
            updateManager = new UpdateManager(this);
            updateManager.checkForUpdates();
            
        } catch (Exception e) {
            getLogger().severe("插件启动失败: " + e.getMessage());
            if (debug) {
                e.printStackTrace();
            }
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerCommands() {
        if (getCommand("clock") != null) {
            getCommand("clock").setExecutor(this::onCommand);
        }
        if (getCommand("clockremove") != null) {
            getCommand("clockremove").setExecutor(this::onRemoveCommand);
        }
        if (getCommand("bellcommand") != null) {
            getCommand("bellcommand").setExecutor(this::onBellCommand);
        }
    }

    private void logStartupInfo() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("version", getDescription().getVersion());
        getLogger().info(ColorUtils.translateConsoleColors(
            languageManager.getMessage("messages.plugin.enable", placeholders)
        ));
        
        if (debug) {
            getLogger().info(ColorUtils.translateConsoleColors(
                languageManager.getMessage("messages.plugin.debug-enabled")
            ));
        }
        
        if (getServer().getName().toLowerCase().contains("nukkit") || 
            getServer().getName().toLowerCase().contains("bedrock")) {
            getLogger().info(languageManager.getMessage("messages.plugin.bedrock-detected"));
        }
    }

    @Override
    public void onDisable() {
        String disableMessage = languageManager != null ? 
            languageManager.getMessage("messages.plugin.disable") : 
            "BellCommand 插件已关闭!";
        
        cleanup();
        
        getLogger().info(ColorUtils.translateConsoleColors(disableMessage));
    }

    private void cleanup() {
        if (itemManager != null) {
            itemManager.disable();
            itemManager = null;
        }
        
        if (updateManager != null) {
            updateManager = null;
        }
        
        config = null;
        languageManager = null;
    }

    private void loadConfiguration() {
        try {
            backupConfig();
            initializeConfig();
            validateConfig();
            loadDebugMode();
            validateItems();
        } catch (Exception e) {
            handleConfigError(e);
        }
    }

    private void backupConfig() throws IOException {
        if (getConfig().contains("config-version")) {
            File configFile = new File(getDataFolder(), "config.yml");
            File backupFile = new File(getDataFolder(), "config.backup.yml");
            if (configFile.exists()) {
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                if (debug) {
                    getLogger().info("配置文件已备份到: config.backup.yml");
                }
            }
        }
    }

    private void initializeConfig() {
        saveDefaultConfig();
        reloadConfig();
        config = getConfig();
    }

    private void validateConfig() {
        int configVersion = config.getInt("config-version", 0);
        if (configVersion < 1) {
            getLogger().warning("配置文件版本过低，将使用新版本配置");
            saveResource("config.yml", true);
            reloadConfig();
            config = getConfig();
        }
        
        // 验证必需的配置节点
        String[] requiredSections = {"items", "update-source", "language"};
        for (String section : requiredSections) {
            if (!config.contains(section)) {
                getLogger().warning("配置文件缺少必需的节点: " + section);
                getLogger().warning("将使用默认配置");
                saveResource("config.yml", true);
                reloadConfig();
                config = getConfig();
                break;
            }
        }
    }

    private void loadDebugMode() {
        debug = config.getBoolean("debug", false);
        if (debug) {
            getLogger().info("调试模式已启用");
        }
    }

    private void validateItems() {
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            getLogger().warning("配置文件中缺少items部分，将使用默认配置");
            return;
        }

        if (debug) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("count", String.valueOf(itemsSection.getKeys(false).size()));
            getLogger().info(languageManager.getMessage("messages.debug.config.items-loaded", placeholders));
        }

        for (String id : itemsSection.getKeys(false)) {
            validateItem(id, itemsSection.getConfigurationSection(id));
        }
    }

    private void validateItem(String id, ConfigurationSection itemSection) {
        if (itemSection == null) {
            getLogger().warning("物品 " + id + " 的配置无效");
            return;
        }

        // 验证必需的物品属性
        String[] requiredFields = {"item-id", "name"};
        for (String field : requiredFields) {
            if (!itemSection.contains(field)) {
                getLogger().warning("物品 " + id + " 缺少必需的属性: " + field);
                continue;
            }
        }

        // 验证物品类型
        String itemId = itemSection.getString("item-id");
        if (Material.matchMaterial(itemId) == null) {
            getLogger().warning("无效的物品ID: " + itemId + " (在物品 " + id + " 中)");
        }

        // 验证命令配置
        validateItemCommands(id, itemSection);

        if (debug) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", id);
            placeholders.put("name", itemSection.getString("name"));
            placeholders.put("type", itemId);
            getLogger().info(languageManager.getMessage("messages.debug.config.item-entry", placeholders));
        }
    }

    private void validateItemCommands(String id, ConfigurationSection itemSection) {
        ConfigurationSection commands = itemSection.getConfigurationSection("commands");
        if (commands == null) {
            getLogger().warning("物品 " + id + " 没有配置命令");
            return;
        }

        validateCommandSection(id, commands, "left-click");
        validateCommandSection(id, commands, "right-click");
    }

    private void validateCommandSection(String id, ConfigurationSection commands, String clickType) {
        if (!commands.contains(clickType)) {
            if (debug) {
                getLogger().info("物品 " + id + " 没有配置 " + clickType + " 命令");
            }
            return;
        }

        if (commands.isList(clickType)) {
            List<String> cmdList = commands.getStringList(clickType);
            if (cmdList.isEmpty()) {
                getLogger().warning("物品 " + id + " 的 " + clickType + " 命令列表为空");
            }
        } else if (commands.isConfigurationSection(clickType)) {
            ConfigurationSection cmdSection = commands.getConfigurationSection(clickType);
            for (String key : cmdSection.getKeys(false)) {
                if (!cmdSection.isConfigurationSection(key)) {
                    getLogger().warning("物品 " + id + " 的 " + clickType + " 命令 " + key + " 配置格式无效");
                }
            }
        } else {
            getLogger().warning("物品 " + id + " 的 " + clickType + " 命令配置格式无效");
        }
    }

    private void handleConfigError(Exception e) {
        getLogger().severe("加载配置文件时发生错误: " + e.getMessage());
        if (debug) {
            e.printStackTrace();
        }
        
        // 尝试加载默认配置
        getLogger().warning("尝试加载默认配置...");
        saveResource("config.yml", true);
        reloadConfig();
        config = getConfig();
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

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(languageManager.getMessage("messages.command.help"));
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

        sender.sendMessage(languageManager.getMessage("messages.command.unknown"));
        return true;
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
