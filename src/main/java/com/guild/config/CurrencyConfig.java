package com.guild.config;
import com.guild.GuildPlugin;
import com.guild.currency.GuildCurrency;

public class CurrencyConfig extends SimpleFileConfig
{
    public CurrencyConfig(GuildPlugin plugin)
    {
        super(plugin, "currency.yml");
    }

    public GuildCurrency.CurrencyType getCurrencyType()
    {
        String type = config.getString("type", "GUILD_COIN");
        try { return GuildCurrency.CurrencyType.valueOf(type.toUpperCase()); }
        catch (IllegalArgumentException e) { return GuildCurrency.CurrencyType.GUILD_COIN; }
    }

    public long getLevelUpCost() { return config.getLong("level-up-cost", 100000L); }

    public long getLevelUpCost(int currentLevel)
    {
        long baseCost = config.getLong("level-up-cost", 100000L);
        double rate = config.getDouble("upgrade-formula.growth-rate", 0.08);
        long max = config.getLong("upgrade-formula.max-cost", 0L);
        long cost = (long) (baseCost * (1.0 + currentLevel * rate));
        if (max > 0 && cost > max) cost = max;
        return cost;
    }

    public long getExperienceCost() { return config.getLong("experience-cost", 5000L); }
    public int getExperienceAmount() { return config.getInt("experience-amount", 50); }
    public String getGuildCurrencyName() { return config.getString("display-names.guild_coin", "公会币"); }
    public String getVaultCurrencyName() { return config.getString("display-names.vault", "金币"); }
    public String getPlayerPointsCurrencyName() { return config.getString("display-names.playerpoints", "积分"); }
    public long getInitialBalance() { return config.getLong("initial-balance", 0L); }
    public long getMaxBalance() { return config.getLong("max-balance", 0L); }
    public long getCreateCost() { return config.getLong("create.cost", 0L); }
    public boolean isCreateRequiresMoney() { return config.getBoolean("create.requires-money", true); }
    public boolean isThousandsSeparator() { return config.getBoolean("format.thousands-separator", true); }
    public int getDecimalPlaces() { return config.getInt("format.decimal-places", 0); }
}
