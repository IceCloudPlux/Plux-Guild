package com.guild.config;

import com.guild.GuildPlugin;

public class FeatureConfig {
    private final GuildPlugin plugin;

    public FeatureConfig(GuildPlugin guildPlugin) {
        this.plugin = guildPlugin;
    }

    public boolean isBankEnabled() {
        return this.plugin.getConfig().getBoolean("features.bank-enabled", true);
    }

    public boolean isChatEnabled() {
        return this.plugin.getConfig().getBoolean("features.chat-enabled", true);
    }

    public boolean isExperienceEnabled() {
        return this.plugin.getConfig().getBoolean("features.experience-enabled", true);
    }

    public boolean isLevelEnabled() {
        return this.plugin.getConfig().getBoolean("features.level-enabled", true);
    }

    public boolean isMotdEnabled() {
        return this.plugin.getConfig().getBoolean("features.motd-enabled", true);
    }

    public boolean isTagEnabled() {
        return this.plugin.getConfig().getBoolean("features.tag-enabled", true);
    }

    public boolean isNicknameEnabled() {
        return this.plugin.getConfig().getBoolean("features.nickname-enabled", true);
    }

    public boolean isNotificationEnabled() {
        return this.plugin.getConfig().getBoolean("features.notification-enabled", true);
    }

    public boolean isGuiEnabled() {
        return this.plugin.getConfig().getBoolean("features.gui-enabled", true);
    }
}

