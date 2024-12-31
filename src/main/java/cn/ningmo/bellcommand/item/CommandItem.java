package cn.ningmo.bellcommand.item;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandItem {
    private final String id;
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final List<String> rightClickCommands;
    private final List<String> leftClickCommands;
    private final String permission;
    private final int cooldown;

    private List<String> validateCommands(List<String> commands) {
        if (commands == null) {
            return new ArrayList<>();
        }
        
        return commands.stream()
            .map(String::trim)
            .filter(cmd -> !cmd.isEmpty())
            .map(cmd -> cmd.startsWith("/") ? cmd.substring(1) : cmd)
            .filter(this::isCommandSafe)
            .collect(Collectors.toList());
    }

    private boolean isCommandSafe(String command) {
        String lcCmd = command.toLowerCase();
        String[] dangerousCommands = {
            "op", "deop", "stop", "reload", "restart",
            "bukkit:", "minecraft:", "spigot:", "paper:",
            "${", "$(", "eval", "rl", "ban", "kick"
        };
        
        for (String dangerous : dangerousCommands) {
            if (lcCmd.startsWith(dangerous)) {
                return false;
            }
        }
        
        return true;
    }

    public CommandItem(String id, ConfigurationSection config) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("物品ID不能为空");
        }
        this.id = id.toLowerCase();
        
        String itemId = config.getString("item-id", "CLOCK");
        this.material = Material.matchMaterial(itemId);
        if (this.material == null) {
            throw new IllegalArgumentException("无效的物品类型: " + itemId);
        }
        
        String rawName = config.getString("name");
        if (rawName == null || rawName.isEmpty()) {
            throw new IllegalArgumentException("物品名称不能为空");
        }
        this.name = ChatColor.translateAlternateColorCodes('&', rawName);
        
        this.lore = config.getStringList("lore").stream()
            .map(line -> ChatColor.translateAlternateColorCodes('&', line))
            .collect(Collectors.toList());
        this.rightClickCommands = validateCommands(config.getStringList("commands.right-click"));
        this.leftClickCommands = validateCommands(config.getStringList("commands.left-click"));
        this.permission = config.getString("permission", "");
        this.cooldown = Math.max(0, config.getInt("cooldown", 0));
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
    public List<String> getRightClickCommands() { return new ArrayList<>(rightClickCommands); }
    public List<String> getLeftClickCommands() { return new ArrayList<>(leftClickCommands); }
    public String getPermission() { return permission; }
    public int getCooldown() { return cooldown; }

    public boolean isValid() {
        if (id == null || id.isEmpty()) return false;
        if (name == null || name.isEmpty()) return false;
        if (material == null) return false;
        return true;
    }
} 