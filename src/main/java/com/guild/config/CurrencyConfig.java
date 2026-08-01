package com.guild.config;
import com.guild.GuildPlugin;
import com.guild.currency.GuildCurrency;
import org.bukkit.configuration.file.FileConfiguration;

public class CurrencyConfig
{
    private final GuildPlugin plugin;
    private FileConfiguration config;

    public CurrencyConfig(GuildPlugin guildPlugin)
    {
        this.plugin = guildPlugin;
        this.config = guildPlugin.getConfig();
    }

    public GuildCurrency.CurrencyType getCurrencyType()
    {
        String type = config.getString("currency.type", "GUILD_COIN");
        try
        {
            return GuildCurrency.CurrencyType.valueOf(type.toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            return GuildCurrency.CurrencyType.GUILD_COIN;
        }
    }

    public long getLevelUpCost()
    {
        return config.getLong("currency.level-up-cost", 100000L);
    }

    public long getLevelUpCost(int currentLevel)
    {
        long baseCost = config.getLong("currency.level-up-cost", 100000L);
        double multiplier = 1.0 + currentLevel * 0.08;
        return (long) (baseCost * multiplier);
    }

    public long getExperienceCost()
    {
        return config.getLong("currency.experience-cost", 5000L);
    }

    public int getExperienceAmount()
    {
        return config.getInt("currency.experience-amount", 50);
    }

    public String getGuildCurrencyName()
    {
        return config.getString("currency.display-names.guild_coin", "公会币");
    }

    public String getVaultCurrencyName()
    {
        return config.getString("currency.display-names.vault", "金币");
    }

    public String getPlayerPointsCurrencyName()
    {
        return config.getString("currency.display-names.playerpoints", "积分");
    }

    public boolean isBankEnabled()
    {
        return config.getBoolean("features.bank-enabled", true);
    }

    public void reload()
    {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
}
