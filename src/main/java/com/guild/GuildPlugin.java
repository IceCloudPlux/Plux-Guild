package com.guild;

import com.guild.api.GuildAPI;
import com.guild.cache.PlayerNameCache;
import com.guild.commands.GuildChatCommand;
import com.guild.commands.GuildCommand;
import com.guild.commands.GuildGUICommand;
import com.guild.config.CurrencyConfig;
import com.guild.config.FeatureConfig;
import com.guild.config.GUIConfig;
import com.guild.currency.GuildCurrency;
import com.guild.database.DatabaseManager;
import com.guild.guild.GuildManager;
import com.guild.listeners.ChatInputListener;
import com.guild.listeners.InventoryListener;
import com.guild.listeners.PlayerListener;
import com.guild.papi.GuildPlaceholderExpansion;
import com.guild.utils.VersionCompat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class GuildPlugin extends JavaPlugin {

    private static GuildPlugin instance;
    private DatabaseManager databaseManager;
    private GuildManager guildManager;
    private GUIConfig guiConfig;
    private FeatureConfig featureConfig;
    private CurrencyConfig currencyConfig;
    private GuildCurrency guildCurrency;
    private final Map<String, String> messages = new HashMap<>();
    private String currentLanguage;
    private PlayerNameCache playerNameCache;
    private ChatInputListener chatInputListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadLanguage();
        saveLanguageFiles();
        guildManager = new GuildManager(this);
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        playerNameCache = new PlayerNameCache(this);
        guiConfig = new GUIConfig(this);
        featureConfig = new FeatureConfig(this);
        currencyConfig = new CurrencyConfig(this);
        guildCurrency = new GuildCurrency(this);
        registerCommands();
        registerListeners();
        registerPlaceholderAPI();
        GuildAPI.init(this); // 初始化公开 API
        printEnableMessage();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        printDisableMessage();
    }

    private void loadLanguage() {
        currentLanguage = getConfig().getString("language", "zh_cn");
        messages.clear();
        messages.putAll(loadLanguageFile(currentLanguage));
        getLogger().info("Loaded language file: " + currentLanguage + ".yml");
        getLogger().info("Default language set to: " + currentLanguage);
    }

    private Map<String, String> loadLanguageFile(String lang) {
        Map<String, String> result = new HashMap<>();
        String path = "lang/" + lang + ".yml";
        File file = new File(getDataFolder(), path);

        try (InputStream resourceStream = getResource(path)) {
            if (resourceStream == null) {
                getLogger().warning("Could not load " + lang + ".yml, using default messages");
                return getDefaultMessages();
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                Files.copy(resourceStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int colonIndex = line.indexOf(':');
                    if (colonIndex <= 0) continue;
                    String key = line.substring(0, colonIndex).trim();
                    String value = line.substring(colonIndex + 1).trim();
                    if (!key.isEmpty() && !value.isEmpty()) {
                        if ((value.startsWith("\"") && value.endsWith("\"")) || 
                            (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        result.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Could not load language file: " + lang + ".yml", e);
        }
        return result;
    }

    private Map<String, String> getDefaultMessages() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("plugin.enabling", "Enabling Guild");
        defaults.put("plugin.enabled", "GuildPlugin has been enabled!");
        defaults.put("plugin.disabling", "Disabling Guild");
        defaults.put("plugin.disabled", "GuildPlugin has been disabled!");
        defaults.put("plugin.author", "By GuildPlugin");
        defaults.put("plugin.version", "v" + getDescription().getVersion());
        return defaults;
    }

    private void saveLanguageFiles() {
        for (String lang : new String[]{"en_US", "zh_cn", "zh_tw"}) {
            String path = "lang/" + lang + ".yml";
            try (InputStream inputStream = getResource(path)) {
                if (inputStream == null) continue;
                File file = new File(getDataFolder(), path);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                if (!file.exists()) {
                    Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                getLogger().warning("Could not save language file: " + lang + ".yml");
            }
        }
    }

    private void printEnableMessage() {
        String version = getDescription().getVersion();
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "========================================");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "  \u2588\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2588\u2588\u2588\u2588\u2557");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "  \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557 \u2588\u2588\u2554\u2550\u2550\u2550\u255d \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557 \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "  \u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2588\u2557   \u2588\u2588\u2588\u2588\u2588\u2554\u255d \u2588\u2588\u2551   \u2588\u2588\u2551");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "  \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557 \u2588\u2588\u2554\u2550\u2550\u255d   \u2588\u2588\u2554\u2550\u2550\u2550\u255d  \u2588\u2588\u2551   \u2588\u2588\u2551");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "  \u2588\u2588\u2551  \u2588\u2588\u2551 \u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2551      \u255a\u2588\u2588\u2588\u2588\u2588\u2554\u255d");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "  \u255a\u2550\u255d  \u255a\u2550\u255d \u255a\u2550\u2550\u2550\u2550\u2550\u255d \u255a\u2550\u255d       \u255a\u2550\u2550\u2550\u2550\u2550\u255d");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "========================================");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "  " + ChatColor.BOLD + "GUILD " + messages.getOrDefault("plugin.version", "v" + version));
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "  " + messages.getOrDefault("plugin.enabling", "Enabling Guild"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "========================================");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "  " + messages.getOrDefault("plugin.enabled", "GuildPlugin has been enabled!"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "  " + messages.getOrDefault("plugin.author", "By GuildPlugin"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "  Loaded " + guildManager.getGuilds().size() + " guilds");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "  Server Type: " + VersionCompat.getServerType());
        Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "  Minecraft Version: " + VersionCompat.getMcVersion());
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "========================================");
        Bukkit.getConsoleSender().sendMessage("");
    }

    private void printDisableMessage() {
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "========================================");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "  " + messages.getOrDefault("plugin.disabling", "Disabling Guild"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "  " + messages.getOrDefault("plugin.disabled", "GuildPlugin has been disabled!"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "========================================");
        Bukkit.getConsoleSender().sendMessage("");
    }

    private void registerCommands() {
        getCommand("guild").setExecutor((CommandExecutor) new GuildCommand(this));
        getCommand("guildchat").setExecutor((CommandExecutor) new GuildChatCommand(this));
        getCommand("guildgui").setExecutor((CommandExecutor) new GuildGUICommand(this));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents((Listener) new PlayerListener(this), this);
        chatInputListener = new ChatInputListener(this);
        InventoryListener inventoryListener = new InventoryListener(this);
        inventoryListener.setChatInputListener(chatInputListener);
        Bukkit.getPluginManager().registerEvents((Listener) inventoryListener, this);
        Bukkit.getPluginManager().registerEvents((Listener) chatInputListener, this);
    }

    private void registerPlaceholderAPI() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            new GuildPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI integration enabled");
        } catch (ClassNotFoundException e) {
            getLogger().info("PlaceholderAPI not found, placeholder integration disabled");
        }
    }

    // ========== Public API ==========

    public static GuildPlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }

    public ChatInputListener getChatInputListener() {
        return chatInputListener;
    }

    public GUIConfig getGUIConfig() {
        return guiConfig;
    }

    public FeatureConfig getFeatureConfig() {
        return featureConfig;
    }

    public CurrencyConfig getCurrencyConfig() {
        return currencyConfig;
    }

    public GuildCurrency getGuildCurrency() {
        return guildCurrency;
    }

    public PlayerNameCache getPlayerNameCache() {
        return playerNameCache;
    }

    /**
     * 获取语言文件中的消息，按 key -> messages.key -> guild.key 顺序查找
     */
    public String getMessage(String key) {
        String msg = messages.get(key);
        if (msg == null) {
            msg = messages.get("messages." + key);
        }
        if (msg == null) {
            msg = messages.get("guild." + key);
        }
        if (msg != null) {
            msg = ChatColor.translateAlternateColorCodes('&', msg);
        }
        return msg != null ? msg : key;
    }

    /**
     * 获取消息并替换占位符（奇数参数为占位符，偶数参数为替换值）
     */
    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }
}
