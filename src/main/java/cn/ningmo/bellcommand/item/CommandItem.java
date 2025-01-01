package cn.ningmo.bellcommand.item;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandItem {
    private final String id;
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final List<CommandEntry> leftClickCommands;
    private final List<CommandEntry> rightClickCommands;
    private final String permission;
    private final int cooldown;

    public static class CommandEntry {
        private final String command;
        private final boolean asConsole;

        public CommandEntry(String command, boolean asConsole) {
            this.command = command;
            this.asConsole = asConsole;
        }

        public String getCommand() {
            return command;
        }

        public boolean isAsConsole() {
            return asConsole;
        }
    }

    public CommandItem(String id, ConfigurationSection config) {
        this.id = id;
        this.material = Material.valueOf(config.getString("item-id", "CLOCK").toUpperCase());
        this.name = ChatColor.translateAlternateColorCodes('&', config.getString("name", "Command Item"));
        this.lore = config.getStringList("lore").stream()
            .map(line -> ChatColor.translateAlternateColorCodes('&', line))
            .collect(Collectors.toList());
        this.permission = config.getString("permission", "");
        this.cooldown = config.getInt("cooldown", 0);

        this.leftClickCommands = loadCommands(config, "commands.left-click");
        this.rightClickCommands = loadCommands(config, "commands.right-click");
    }

    private List<CommandEntry> loadCommands(ConfigurationSection config, String path) {
        List<CommandEntry> commands = new ArrayList<>();
        if (config.contains(path)) {
            if (config.isList(path)) {
                // 支持旧格式
                config.getStringList(path).forEach(cmd -> 
                    commands.add(new CommandEntry(cmd, false)));
            } else if (config.isConfigurationSection(path)) {
                // 新格式
                ConfigurationSection cmdSection = config.getConfigurationSection(path);
                for (String key : cmdSection.getKeys(false)) {
                    if (cmdSection.isConfigurationSection(key)) {
                        ConfigurationSection entry = cmdSection.getConfigurationSection(key);
                        String command = entry.getString("command");
                        boolean asConsole = entry.getBoolean("as-console", false);
                        commands.add(new CommandEntry(command, asConsole));
                    }
                }
            }
        }
        return commands;
    }

    public void executeCommands(Player player, boolean isRightClick) {
        List<CommandEntry> commands = isRightClick ? rightClickCommands : leftClickCommands;
        for (CommandEntry entry : commands) {
            if (entry.asConsole) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                    entry.command.replace("%player%", player.getName()));
            } else {
                player.performCommand(entry.command);
            }
        }
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
        if (item == null || item.getType() != material) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().equals(name);
    }

    // Getters
    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public String getName() { return name; }
    public List<String> getLore() { return new ArrayList<>(lore); }
    public List<CommandEntry> getRightClickCommands() { return new ArrayList<>(rightClickCommands); }
    public List<CommandEntry> getLeftClickCommands() { return new ArrayList<>(leftClickCommands); }
    public String getPermission() { return permission; }
    public int getCooldown() { return cooldown; }

    public boolean isValid() {
        if (id == null || id.isEmpty()) return false;
        if (name == null || name.isEmpty()) return false;
        if (material == null) return false;
        return true;
    }
} 