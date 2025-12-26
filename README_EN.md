# BellCommand - Command Item Plugin

[中文版](README.md)

## Version Info
Current Version: 1.4.0-beta.1

This is a major Beta release with the following features:
- ✅ **Config System Overhaul**: Modular multi-folder architecture with real-time hot-reloading (WatchService) and smart auto-migration.
- ✅ **Consumable Item System**: Support for fixed count, probability-based, and random range consumption modes.
- ✅ **Enhanced Thread Safety**: Full implementation of ReentrantReadWriteLock for configuration stability under high concurrency.
- ✅ **Automated Release Pipeline**: Deep integration with GitHub Actions and Modrinth API for automatic publishing.
- ✅ Complete command item system
- ✅ Language support
- ✅ Bedrock player support (requires Floodgate)
- ✅ Action-specific cooldown system
- ✅ Custom update source support (GitHub, Gitee, Custom API)

## Introduction
BellCommand is a powerful Minecraft plugin that allows server administrators to create custom items that can execute commands. Players can trigger preset commands by left-clicking or right-clicking these items.

## Features
- **Modular Config Management**: Support for multiple folders (e.g., `Default_config/`) to store item configurations, decoupling item definitions from the main config.
- **Real-time Hot-Reloading**: Powered by `WatchService` to monitor file changes; modifications take effect instantly without manual reloading.
- **Consumable Item System**: Support for item consumption based on fixed counts, probabilities, or random ranges to flexibly control item lifespan.
- **Enhanced Thread Safety**: Implementation of `ReentrantReadWriteLock` for core configuration access, ensuring absolute stability under high concurrency.
- **Multi-Dimensional Interaction**: Independent command triggers for Left-Click, Right-Click, and Shift+Left/Right-Click.
- **Deep Bedrock Adaptation**: Dedicated interaction logic and command configurations for Floodgate (Bedrock) players.
- **Granular Permission Control**: Comprehensive permission system supporting global and item-specific permissions.
- **Independent Cooldowns**: Different action types (e.g., Left/Right click) can have their own independent cooldown timers.
- **Internationalization (i18n)**: Robust `LanguageManager` system for real-time switching of system logs and messages.
- **Automatic Update Alerts**: Integrated update detection for GitHub, Gitee, and custom APIs.

## Commands
- `/bc give <player> <itemID> [amount]` - Give command items
- `/bc list` - View available command items
- `/bc reload` - Reload plugin configuration

## Permissions
- `bellcommand.command` - Allow using basic commands
- `bellcommand.reload` - Allow configuration reload
- `bellcommand.give` - Allow giving items
- `bellcommand.list` - Allow viewing item list
- `bellcommand.use` - Allow using command items
- `bellcommand.item.<itemID>` - Allow using specific items

## Installation
1. Download the latest BellCommand.jar
2. Place the file in your server's plugins folder
3. Restart server or reload plugin
4. Edit configuration file
5. Use `/bc reload` to reload configuration

## Support & Feedback
- GitHub Issues: [Issue Tracker](https://github.com/ning-g-mo/BellCommand/issues)
- MineBBS: [Plugin Page](https://www.minebbs.com/)

## Special Thanks
- [cursor](https://www.cursor.com/)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- [leafMC](https://github.com/Winds-Studio/Leaf) 
