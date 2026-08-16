package com.guild.config;
import com.guild.GuildPlugin;

public class FeatureConfig extends SimpleFileConfig
{
    public FeatureConfig(GuildPlugin plugin)
    {
        super(plugin, "features.yml");
    }

    public boolean isBankEnabled() { return config.getBoolean("bank", true); }
    public boolean isChatEnabled() { return config.getBoolean("chat", true); }
    public boolean isExperienceEnabled() { return config.getBoolean("experience", true); }
    public boolean isLevelEnabled() { return config.getBoolean("level", true); }
    public boolean isMotdEnabled() { return config.getBoolean("motd", true); }
    public boolean isTagEnabled() { return config.getBoolean("tag", true); }
    public boolean isNicknameEnabled() { return config.getBoolean("nickname", true); }
    public boolean isNotificationEnabled() { return config.getBoolean("notification", true); }
    public boolean isGuiEnabled() { return config.getBoolean("gui", true); }
    public boolean isShopEnabled() { return config.getBoolean("shop", true); }
    public boolean isAutoSaveEnabled() { return config.getBoolean("auto-save", true); }
    public boolean isAutoDisbandEnabled() { return config.getBoolean("auto-disband", false); }
    public boolean isDailyResetEnabled() { return config.getBoolean("daily-reset", true); }
    public boolean isCombatTagEnabled() { return config.getBoolean("combat-tag", false); }
    public boolean isCooldownBarEnabled() { return config.getBoolean("cooldown-bar", true); }
}
