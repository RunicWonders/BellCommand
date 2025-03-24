package cn.ningmo.bellcommand.item;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import cn.ningmo.bellcommand.utils.ColorUtils;

public class CommandItem {
    private final String id;
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final String permission;
    private final double cooldown;
    private final Map<String, List<CommandEntry>> commands;

    public static class CommandEntry {
        private final String command;
        private final boolean asConsole;
        private final double delay; // 延迟执行时间（秒）

        public CommandEntry(String command, boolean asConsole, double delay) {
            this.command = command;
            this.asConsole = asConsole;
            this.delay = delay;
        }

        public String getCommand() {
            return command;
        }

        public boolean isAsConsole() {
            return asConsole;
        }
        
        public double getDelay() {
            return delay;
        }
    }

    public CommandItem(String id, ConfigurationSection config) {
        this.id = id;
        this.material = Material.valueOf(config.getString("item-id", "CLOCK").toUpperCase());
        this.name = ColorUtils.translateColors(config.getString("name", "命令物品"));
        
        // 处理物品说明
        List<String> rawLore = config.getStringList("lore");
        if (rawLore.isEmpty()) {
            rawLore = new ArrayList<>();
            rawLore.add("&7右键点击使用");
        }
        this.lore = ColorUtils.translateColors(rawLore);
        
        this.permission = config.getString("permission", "");
        this.cooldown = config.getDouble("cooldown", 0.0);
        this.commands = new HashMap<>();
        
        // 加载命令
        ConfigurationSection commandsSection = config.getConfigurationSection("commands");
        if (commandsSection != null) {
            for (String type : commandsSection.getKeys(false)) {
                List<CommandEntry> typeCommands = new ArrayList<>();
                ConfigurationSection typeSection = commandsSection.getConfigurationSection(type);
                
                if (typeSection != null) {
                    for (String key : typeSection.getKeys(false)) {
                        ConfigurationSection cmdSection = typeSection.getConfigurationSection(key);
                        if (cmdSection != null) {
                            String cmd = cmdSection.getString("command");
                            boolean asConsole = cmdSection.getBoolean("as-console", false);
                            double delay = cmdSection.getDouble("delay", 0.0); // 获取延迟时间，默认为0秒
                            if (cmd != null && !cmd.isEmpty()) {
                                typeCommands.add(new CommandEntry(cmd, asConsole, delay));
                            }
                        }
                    }
                }
                
                if (!typeCommands.isEmpty()) {
                    commands.put(type, typeCommands);
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public double getCooldown() {
        return cooldown;
    }

    public List<CommandEntry> getCommands(String type) {
        return commands.getOrDefault(type, new ArrayList<>());
    }

    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        return meta.getDisplayName().equals(name);
    }
}