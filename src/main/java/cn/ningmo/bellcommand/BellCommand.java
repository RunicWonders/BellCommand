package cn.ningmo.bellcommand;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import cn.ningmo.bellcommand.item.CommandItem;
import cn.ningmo.bellcommand.item.CommandItemManager;
import cn.ningmo.bellcommand.update.UpdateManager;
import cn.ningmo.bellcommand.listener.ItemClickListener;
import cn.ningmo.bellcommand.listener.AutoGiveListener;
import cn.ningmo.bellcommand.utils.ColorUtils;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class BellCommand extends JavaPlugin {
    private static BellCommand instance;
    private boolean debug;
    private LanguageManager languageManager;
    private CommandItemManager itemManager;
    private UpdateManager updateManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        try {
            // 初始化语言管理器
            languageManager = new LanguageManager(this);
            
            // 检查配置文件版本
            if (!checkConfigVersion()) {
                getLogger().severe(languageManager.getMessage("messages.plugin.config.version-mismatch"));
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // 初始化物品管理器
            itemManager = new CommandItemManager(this);
            
            // 注册命令和监听器
            getCommand("bc").setExecutor(this);
            getServer().getPluginManager().registerEvents(new ItemClickListener(this), this);
            getServer().getPluginManager().registerEvents(new AutoGiveListener(this), this);
            
            // 初始化更新管理器
            updateManager = new UpdateManager(this);
            updateManager.checkForUpdates();
            
            // 输出启动信息
            logStartupInfo();
            
        } catch (Exception e) {
            getLogger().severe("插件启动失败: " + e.getMessage());
            if (debug) {
                e.printStackTrace();
            }
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (itemManager != null) {
            itemManager.disable();
        }
        
        if (languageManager != null) {
            getLogger().info(ColorUtils.translateConsoleColors(
                languageManager.getMessage("messages.plugin.disable")
            ));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(languageManager.getMessage("messages.command.help"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("bellcommand.reload")) {
                    sender.sendMessage(languageManager.getMessage("messages.error.no-permission"));
                    return true;
                }
                reloadPlugin();
                sender.sendMessage(languageManager.getMessage("messages.command.reload-success"));
                return true;

            case "give":
                if (!sender.hasPermission("bellcommand.give")) {
                    sender.sendMessage(languageManager.getMessage("messages.error.no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(languageManager.getMessage("messages.command.give-usage"));
                    return true;
                }
                handleGiveCommand(sender, args);
                return true;

            case "list":
                if (!sender.hasPermission("bellcommand.list")) {
                    sender.sendMessage(languageManager.getMessage("messages.error.no-permission"));
                    return true;
                }
                handleListCommand(sender);
                return true;

            default:
                sender.sendMessage(languageManager.getMessage("messages.command.help"));
                return true;
        }
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        String playerName = args[1];
        String itemId = args[2];
        int amount = args.length > 3 ? Math.max(1, Integer.parseInt(args[3])) : 1;

        Player target = getServer().getPlayer(playerName);
        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", playerName);
            sender.sendMessage(languageManager.getMessage("messages.error.player-not-found", placeholders));
            return;
        }

        CommandItem item = itemManager.getItem(itemId);
        if (item == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("item", itemId);
            sender.sendMessage(languageManager.getMessage("messages.error.item-not-found", placeholders));
            return;
        }

        ItemStack itemStack = item.createItemStack();
        itemStack.setAmount(amount);
        target.getInventory().addItem(itemStack);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("item", item.getName());
        placeholders.put("amount", String.valueOf(amount));
        sender.sendMessage(languageManager.getMessage("messages.command.give-success", placeholders));
    }

    private void handleListCommand(CommandSender sender) {
        List<CommandItem> items = new ArrayList<>(itemManager.getAllItems());
        if (items.isEmpty()) {
            sender.sendMessage(languageManager.getMessage("messages.command.list-empty"));
            return;
        }

        sender.sendMessage(languageManager.getMessage("messages.command.list-header"));
        for (CommandItem item : items) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", item.getId());
            placeholders.put("name", item.getName());
            sender.sendMessage(languageManager.getMessage("messages.command.list-item", placeholders));
        }
    }

    private void reloadPlugin() {
        reloadConfig();
        if (itemManager != null) {
            itemManager.reload();
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
    }

    private boolean checkConfigVersion() {
        int currentVersion = getConfig().getInt("config-version", 0);
        int requiredVersion = 2;
        
        if (currentVersion == 0) {
            return false;
        }
        
        if (currentVersion != requiredVersion) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("current", String.valueOf(currentVersion));
            placeholders.put("required", String.valueOf(requiredVersion));
            getLogger().warning(languageManager.getMessage(
                currentVersion < requiredVersion ? 
                "messages.plugin.config.version-too-low" : 
                "messages.plugin.config.version-too-high", 
                placeholders
            ));
            return false;
        }
        
        return true;
    }

    public static BellCommand getInstance() {
        return instance;
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public CommandItemManager getItemManager() {
        return itemManager;
    }
}
