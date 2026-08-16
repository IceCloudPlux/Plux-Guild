package com.guild.config;
import com.guild.GuildPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

public abstract class SimpleFileConfig
{
    protected final GuildPlugin plugin;
    protected FileConfiguration config;
    protected File file;
    private final String fileName;

    public SimpleFileConfig(GuildPlugin plugin, String fileName)
    {
        this.plugin = plugin;
        this.fileName = fileName;
        load();
    }

    public void load()
    {
        file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists())
        {
            plugin.saveResource(fileName, false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        onLoad();
    }

    protected void onLoad() {}

    public void reload()
    {
        load();
    }

    public FileConfiguration getConfig() { return config; }
}
