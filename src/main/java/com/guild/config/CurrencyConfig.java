package com.guild.config;

import com.guild.GuildPlugin;
import com.guild.currency.GuildCurrency;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 货币配置读取器
 *
 * 对应 config.yml 中 currency.* 段
 */
public class CurrencyConfig {
    private final GuildPlugin plugin;
    private FileConfiguration config;

    public CurrencyConfig(GuildPlugin guildPlugin) {
        this.plugin = guildPlugin;
        this.config = guildPlugin.getConfig();
    }

    public GuildCurrency.CurrencyType getCurrencyType() {
        String type = config.getString("currency.type", "GUILD_COIN");
        try {
            return GuildCurrency.CurrencyType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GuildCurrency.CurrencyType.GUILD_COIN;
        }
    }

    /** 升级公会所需货币 */
    public long getLevelUpCost() {
        return config.getLong("currency.level-up-cost", 100000L);
    }

    /** 购买经验所需货币 */
    public long getExperienceCost() {
        return config.getLong("currency.experience-cost", 5000L);
    }

    /** 购买经验获得的经验值 */
    public int getExperienceAmount() {
        return config.getInt("currency.experience-amount", 50);
    }

    // ========== 显示名称（用于消息提示）==========

    public String getGuildCurrencyName() {
        return config.getString("currency.display-names.guild_coin", "公会币");
    }

    public String getVaultCurrencyName() {
        return config.getString("currency.display-names.vault", "金币");
    }

    public String getPlayerPointsCurrencyName() {
        return config.getString("currency.display-names.playerpoints", "积分");
    }

    /** 银行是否启用（从 features 段读取，不再在 currency 段重复定义） */
    public boolean isBankEnabled() {
        return config.getBoolean("features.bank-enabled", true);
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
}
