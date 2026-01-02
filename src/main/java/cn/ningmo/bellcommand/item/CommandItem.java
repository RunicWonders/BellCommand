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
    private final AutoGiveConfig autoGive;
    private final AutoCleanupConfig autoCleanup;
    private final ConsumableConfig consumable;

    public static class ConsumableConfig {
        private final boolean enabled;
        private final String mode; // COUNT, PROBABILITY, RANGE, PROBABILITY_RANGE
        private final int amount;
        private final double probability;
        private final int minAmount;
        private final int maxAmount;

        public ConsumableConfig(ConfigurationSection config) {
            if (config == null) {
                this.enabled = false;
                this.mode = "COUNT";
                this.amount = 1;
                this.probability = 1.0;
                this.minAmount = 1;
                this.maxAmount = 1;
            } else {
                this.enabled = config.getBoolean("enabled", false);
                this.mode = config.getString("mode", "COUNT").toUpperCase();
                this.amount = config.getInt("amount", 1);
                this.probability = config.getDouble("probability", 1.0);
                this.minAmount = config.getInt("min-amount", 1);
                this.maxAmount = config.getInt("max-amount", 1);
            }
        }

        public boolean isEnabled() { return enabled; }
        public String getMode() { return mode; }
        public int getAmount() { return amount; }
        public double getProbability() { return probability; }
        public int getMinAmount() { return minAmount; }
        public int getMaxAmount() { return maxAmount; }
    }

    public static class AutoGiveConfig {
        private final boolean join;
        private final boolean firstJoin;
        private final boolean respawn;

        public AutoGiveConfig(ConfigurationSection config) {
            if (config == null) {
                this.join = false;
                this.firstJoin = false;
                this.respawn = false;
            } else {
                this.join = config.getBoolean("join", false);
                this.firstJoin = config.getBoolean("first-join", false);
                this.respawn = config.getBoolean("respawn", false);
            }
        }

        public boolean isJoin() { return join; }
        public boolean isFirstJoin() { return firstJoin; }
        public boolean isRespawn() { return respawn; }
    }

    public static class AutoCleanupConfig {
        private final boolean enabled;
        private final int delay;

        public AutoCleanupConfig(ConfigurationSection config) {
            if (config == null) {
                this.enabled = false;
                this.delay = 30;
            } else {
                this.enabled = config.getBoolean("enabled", false);
                this.delay = config.getInt("delay", 30);
            }
        }

        public boolean isEnabled() { return enabled; }
        public int getDelay() { return delay; }
    }

    public static class CommandEntry {
        private final String command;
        private final boolean asConsole;
        private final boolean asOp;
        private final double delay; // 延迟执行时间（秒）

        public CommandEntry(String command, boolean asConsole, boolean asOp, double delay) {
            this.command = command;
            this.asConsole = asConsole;
            this.asOp = asOp;
            this.delay = delay;
        }

        public String getCommand() {
            return command;
        }

        public boolean isAsConsole() {
            return asConsole;
        }
        
        public boolean isAsOp() {
            return asOp;
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
        this.autoGive = new AutoGiveConfig(config.getConfigurationSection("auto-give"));
        this.autoCleanup = new AutoCleanupConfig(config.getConfigurationSection("auto-cleanup"));
        this.consumable = new ConsumableConfig(config.getConfigurationSection("consumable"));
        
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
                            boolean asOp = cmdSection.getBoolean("as-op", false);
                            double delay = cmdSection.getDouble("delay", 0.0); // 获取延迟时间，默认为0秒
                            if (cmd != null && !cmd.isEmpty()) {
                                typeCommands.add(new CommandEntry(cmd, asConsole, asOp, delay));
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
        List<CommandEntry> result = commands.getOrDefault(type, new ArrayList<>());
        
        // 如果是基岩版特定的点击类型且没有配置，则回退到普通点击类型
        if (result.isEmpty() && type.startsWith("bedrock-")) {
            String fallbackType = type.replace("bedrock-", "");
            result = commands.getOrDefault(fallbackType, new ArrayList<>());
        }
        
        return result;
    }

    public AutoGiveConfig getAutoGive() {
        return autoGive;
    }

    public AutoCleanupConfig getAutoCleanup() {
        return autoCleanup;
    }

    public ConsumableConfig getConsumable() {
        return consumable;
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