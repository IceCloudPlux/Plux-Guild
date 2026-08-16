package com.guild.config;
import com.guild.GuildPlugin;
import java.util.List;

public class ExperienceConfig extends SimpleFileConfig
{
    public ExperienceConfig(GuildPlugin plugin)
    {
        super(plugin, "experience.yml");
    }

    public int getMonsterKillExp() { return config.getInt("events.monster-kill", 10); }
    public int getBlockBreakExp() { return config.getInt("events.block-break", 5); }
    public int getBlockPlaceExp() { return config.getInt("events.block-place", 3); }
    public int getPlayerKillExp() { return config.getInt("events.player-kill", 50); }
    public int getFishingExp() { return config.getInt("events.fishing", 8); }
    public int getBreedingExp() { return config.getInt("events.breeding", 15); }
    public int getTradingExp() { return config.getInt("events.trading", 20); }
    public int getEnchantingExp() { return config.getInt("events.enchanting", 12); }
    public int getCraftingExp() { return config.getInt("events.crafting", 5); }
    public int getBrewingExp() { return config.getInt("events.brewing", 6); }
    public int getSmeltingExp() { return config.getInt("events.smelting", 4); }
    public int getTamingExp() { return config.getInt("events.taming", 10); }
    public int getAdvancementExp() { return config.getInt("events.advancement", 30); }
    public long getDailyExpLimit() { return config.getLong("daily-exp-limit", 0L); }
    public long getDailyContributionLimit() { return config.getLong("daily-contribution-limit", 0L); }
    public double getWeekendMultiplier() { return config.getDouble("weekend-multiplier", 1.0); }
    public double getHolidayMultiplier() { return config.getDouble("holiday-multiplier", 1.0); }
    public long getLevelFormulaBase() { return config.getLong("level-formula.base", 500L); }
    public double getLevelFormulaGrowthRate() { return config.getDouble("level-formula.growth-rate", 0.02); }
    public long getLevelFormulaMaxRequired() { return config.getLong("level-formula.max-required", 0L); }
    public List<String> getNoExpBlocks() { return config.getStringList("no-exp-blocks"); }
    public List<String> getNoExpMobs() { return config.getStringList("no-exp-mobs"); }
    public boolean isCumulativeContribution() { return config.getBoolean("cumulative-contribution", true); }
    public boolean isWeeklyRewardEnabled() { return config.getBoolean("weekly-reward.enabled", false); }
    public long getWeeklyRewardTop1() { return config.getLong("weekly-reward.top-1", 1000L); }
    public long getWeeklyRewardTop2() { return config.getLong("weekly-reward.top-2", 500L); }
    public long getWeeklyRewardTop3() { return config.getLong("weekly-reward.top-3", 200L); }
}
