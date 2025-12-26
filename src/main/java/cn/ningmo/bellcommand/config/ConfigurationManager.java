package cn.ningmo.bellcommand.config;

import cn.ningmo.bellcommand.BellCommand;
import cn.ningmo.bellcommand.utils.ColorUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 配置管理器
 * 负责主配置文件和物品管理器配置的加载、保存、迁移及监听
 */
public class ConfigurationManager {
    private final BellCommand plugin;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, FileConfiguration> itemConfigs = new ConcurrentHashMap<>();
    private FileConfiguration mainConfig;
    private WatchService watchService;
    private final Map<WatchKey, Path> watchKeys = new HashMap<>();
    private boolean isReloading = false;

    public ConfigurationManager(BellCommand plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化配置系统
     */
    public void init() {
        lock.writeLock().lock();
        try {
            isReloading = true;
            loadMainConfig();
            migrateOldConfig();
            loadItemConfigs();
            setupWatchService();
            isReloading = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 加载主配置文件 config.yml
     */
    private void loadMainConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
    }

    /**
     * 加载所有物品配置文件夹
     */
    private void loadItemConfigs() {
        itemConfigs.clear();
        File dataFolder = plugin.getDataFolder();
        File[] dirs = dataFolder.listFiles(f -> f.isDirectory() && f.getName().endsWith("_config"));
        
        if (dirs == null || dirs.length == 0) {
            // 如果没有任何配置文件夹，创建一个默认的
            createDefaultItemConfig();
            dirs = dataFolder.listFiles(f -> f.isDirectory() && f.getName().endsWith("_config"));
        }

        if (dirs != null) {
            for (File dir : dirs) {
                File commandsFile = new File(dir, "commands.yml");
                if (commandsFile.exists()) {
                    try {
                        itemConfigs.put(dir.getName(), YamlConfiguration.loadConfiguration(commandsFile));
                    } catch (Exception e) {
                        plugin.getLogger().severe(ColorUtils.translateConsoleColors(
                            plugin.getLanguageManager().getMessage("messages.plugin.config.load-failed", 
                            Map.of("file", commandsFile.getPath(), "error", e.getMessage()))
                        ));
                    }
                }
            }
        }
    }

    /**
     * 创建默认物品配置文件
     */
    private void createDefaultItemConfig() {
        File defaultDir = new File(plugin.getDataFolder(), "Default_config");
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }
        File commandsFile = new File(defaultDir, "commands.yml");
        if (!commandsFile.exists()) {
            try {
                // 如果主配置中还有 items (虽然应该已经迁移了)，则不创建空文件
                if (!mainConfig.contains("items")) {
                    commandsFile.createNewFile();
                    YamlConfiguration config = new YamlConfiguration();
                    config.set("items", new HashMap<>());
                    config.save(commandsFile);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("创建默认配置文件失败: " + e.getMessage());
            }
        }
    }

    /**
     * 迁移旧版配置到新架构
     */
    private void migrateOldConfig() {
        boolean hasItems = mainConfig.contains("items");
        boolean hasAutoGive = mainConfig.contains("auto-give");
        boolean hasAutoCleanup = mainConfig.contains("auto-cleanup");

        if (hasItems || hasAutoGive || hasAutoCleanup) {
            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                plugin.getLanguageManager().getMessage("messages.plugin.config.migration-start")
            ));

            // 1. 备份旧文件
            backupFile(new File(plugin.getDataFolder(), "config.yml"));

            // 2. 获取旧设置
            ConfigurationSection itemsSection = mainConfig.getConfigurationSection("items");
            ConfigurationSection globalAutoGive = mainConfig.getConfigurationSection("auto-give");
            ConfigurationSection globalAutoCleanup = mainConfig.getConfigurationSection("auto-cleanup");

            // 3. 创建新目录和文件
            File targetDir = new File(plugin.getDataFolder(), "Legacy_config");
            if (!targetDir.exists()) targetDir.mkdirs();
            
            File commandsFile = new File(targetDir, "commands.yml");
            YamlConfiguration commandsConfig = new YamlConfiguration();
            
            if (itemsSection != null) {
                // 如果有旧物品，将全局设置应用到每个物品上
                for (String key : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                    if (itemSection != null) {
                        // 迁移 auto-give
                        if (globalAutoGive != null && !itemSection.contains("auto-give")) {
                            itemSection.set("auto-give", globalAutoGive);
                        }
                        // 迁移 auto-cleanup
                        if (globalAutoCleanup != null && !itemSection.contains("auto-cleanup")) {
                            itemSection.set("auto-cleanup", globalAutoCleanup);
                        }
                    }
                }
                commandsConfig.set("items", itemsSection);
            }
            
            try {
                commandsConfig.save(commandsFile);
                
                // 4. 更新主配置，移除已迁移的项
                mainConfig.set("items", null);
                mainConfig.set("auto-give", null);
                mainConfig.set("auto-cleanup", null);
                plugin.saveConfig();
                
                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.plugin.config.migration-success")
                ));
            } catch (IOException e) {
                plugin.getLogger().severe(ColorUtils.translateConsoleColors(
                    plugin.getLanguageManager().getMessage("messages.plugin.config.migration-failed", 
                    Map.of("error", e.getMessage()))
                ));
            }
        }
    }

    /**
     * 备份文件
     */
    public void backupFile(File file) {
        if (!file.exists()) return;
        
        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) backupDir.mkdirs();
        
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backupFile = new File(backupDir, file.getName() + "." + timestamp + ".bak");
        
        try {
            Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info(ColorUtils.translateConsoleColors(
                plugin.getLanguageManager().getMessage("messages.plugin.config.backup-created", 
                Map.of("file", backupFile.getName()))
            ));
        } catch (IOException e) {
            plugin.getLogger().warning("备份文件失败: " + file.getName() + " - " + e.getMessage());
        }
    }

    /**
     * 设置文件变更监听
     */
    private void setupWatchService() {
        try {
            stopWatchService();
            watchService = FileSystems.getDefault().newWatchService();
            
            // 监听数据根目录 (针对 config.yml)
            registerWatcher(plugin.getDataFolder().toPath());
            
            // 监听所有配置文件夹 (针对 commands.yml)
            File[] dirs = plugin.getDataFolder().listFiles(f -> f.isDirectory() && f.getName().endsWith("_config"));
            if (dirs != null) {
                for (File dir : dirs) {
                    registerWatcher(dir.toPath());
                }
            }

            Thread watchThread = new Thread(this::watchLoop, "BellCommand-ConfigWatcher");
            watchThread.setDaemon(true);
            watchThread.start();
        } catch (IOException e) {
            plugin.getLogger().warning("无法初始化文件监听服务: " + e.getMessage());
        }
    }

    private void registerWatcher(Path path) throws IOException {
        WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        watchKeys.put(key, path);
    }

    public void stopWatchService() {
        try {
            if (watchService != null) {
                watchService.close();
                watchService = null;
            }
            watchKeys.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void watchLoop() {
        try {
            while (watchService != null) {
                WatchKey key = watchService.take();
                Path dir = watchKeys.get(key);
                if (dir == null) continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                    
                    Path name = (Path) event.context();
                    String fileName = name.toString();
                    
                    if (fileName.equals("config.yml") || fileName.equals("commands.yml")) {
                        // 延迟一小段时间，防止文件被占用
                        Thread.sleep(100);
                        if (!isReloading) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                plugin.getLogger().info(ColorUtils.translateConsoleColors(
                                    plugin.getLanguageManager().getMessage("messages.plugin.config.hot-reload", 
                                    Map.of("file", fileName))
                                ));
                                plugin.getItemManager().reload();
                            });
                        }
                    }
                }
                
                if (!key.reset()) {
                    watchKeys.remove(key);
                    if (watchKeys.isEmpty()) break;
                }
            }
        } catch (InterruptedException | ClosedWatchServiceException e) {
            // 正常停止
        } catch (Exception e) {
            if (watchService != null) {
                plugin.getLogger().warning("配置监听线程异常: " + e.getMessage());
            }
        }
    }

    /**
     * 获取主配置（线程安全）
     */
    public FileConfiguration getMainConfig() {
        lock.readLock().lock();
        try {
            return mainConfig;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取所有物品配置（线程安全）
     */
    public Map<String, FileConfiguration> getItemConfigs() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableMap(new HashMap<>(itemConfigs));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 保存主配置（线程安全）
     */
    public void saveMainConfig() {
        lock.writeLock().lock();
        try {
            plugin.saveConfig();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 保存特定的物品配置（线程安全）
     * @param configName 配置文件夹名称 (如 "Default_config")
     * @param config 配置对象
     */
    public void saveItemConfig(String configName, FileConfiguration config) {
        lock.writeLock().lock();
        try {
            File dir = new File(plugin.getDataFolder(), configName);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "commands.yml");
            try {
                config.save(file);
                itemConfigs.put(configName, config);
            } catch (IOException e) {
                plugin.getLogger().severe("无法保存配置文件 " + file.getPath() + ": " + e.getMessage());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 热重载所有配置
     */
    public void reload() {
        init();
    }
}
