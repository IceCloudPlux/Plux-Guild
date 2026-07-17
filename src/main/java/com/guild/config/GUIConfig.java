package com.guild.config;

import com.guild.GuildPlugin;
import com.guild.utils.VersionCompat;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class GUIConfig {
    private final GuildPlugin plugin;
    private FileConfiguration config;

    public GUIConfig(GuildPlugin guildPlugin) {
        this.plugin = guildPlugin;
        guildPlugin.saveDefaultConfig();
        this.reloadConfig();
    }

    public void reloadConfig() {
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
    }

    public String getMainTitle() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.title.main", "&6\u516c\u4f1a\u7cfb\u7edf"));
    }

    public String getNoGuildTitle() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.title.no_guild", "&6\u516c\u4f1a\u7cfb\u7edf"));
    }

    public String getAllGuildsTitle() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.title.all_guilds", "&6\u6240\u6709\u516c\u4f1a"));
    }

    public String getMemberTitle(String string) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.title.member", "&6\u516c\u4f1a: &e%name%").replace("%name%", string));
    }

    public String getOfficerTitle(String string) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.title.officer", "&6\u516c\u4f1a\u7ba1\u7406: &e%name%").replace("%name%", string));
    }

    public String getOwnerTitle(String string) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.title.owner", "&6\u516c\u4f1a\u4f1a\u957f: &e%name%").replace("%name%", string));
    }

    public int getMainSize() {
        return this.config.getInt("gui.size.main", 54);
    }

    public int getAllGuildsSize() {
        return this.config.getInt("gui.size.all_guilds", 54);
    }

    private Material getMaterialSafe(String string, String ... stringArray) {
        String string2 = this.config.getString(string);
        if (string2 != null) {
            try {
                return Material.valueOf((String)string2);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        for (String string3 : stringArray) {
            try {
                return Material.valueOf((String)string3);
            }
            catch (IllegalArgumentException illegalArgumentException) {
            }
        }
        return Material.STONE;
    }

    public Material getNoGuildBarrierMaterial() {
        return this.getMaterialSafe("gui.items.no_guild.barrier.material", "BARRIER", "BEDROCK");
    }

    public String getNoGuildBarrierName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.no_guild.barrier.name", "&c\u4f60\u8fd8\u6ca1\u6709\u516c\u4f1a"));
    }

    public List<String> getNoGuildBarrierLore() {
        return this.config.getStringList("gui.items.no_guild.barrier.lore");
    }

    public Material getCreateMaterial() {
        return this.getMaterialSafe("gui.items.no_guild.create.material", "DIAMOND", "EMERALD");
    }

    public String getCreateName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.no_guild.create.name", "&a\u521b\u5efa\u516c\u4f1a"));
    }

    public List<String> getCreateLore() {
        return this.config.getStringList("gui.items.no_guild.create.lore");
    }

    public Material getViewAllMaterial() {
        return this.getMaterialSafe("gui.items.no_guild.view_all.material", "BOOK", "PAPER");
    }

    public String getViewAllName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.no_guild.view_all.name", "&e\u67e5\u770b\u6240\u6709\u516c\u4f1a"));
    }

    public List<String> getViewAllLore() {
        return this.config.getStringList("gui.items.no_guild.view_all.lore");
    }

    public Material getGuildItemMaterial() {
        return this.getMaterialSafe("gui.items.all_guilds.guild_item.material", "DIAMOND_BLOCK", "EMERALD_BLOCK");
    }

    public List<String> getGuildItemLore() {
        return this.config.getStringList("gui.items.all_guilds.guild_item.lore");
    }

    public Material getBackMaterial() {
        return this.getMaterialSafe("gui.items.all_guilds.back.material", "ARROW", "STICK");
    }

    public String getBackName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.all_guilds.back.name", "&c\u8fd4\u56de"));
    }

    public List<String> getBackLore() {
        return this.config.getStringList("gui.items.all_guilds.back.lore");
    }

    public Material getInfoMaterial() {
        return this.getMaterialSafe("gui.items.member.info.material", "PAPER", "BOOK");
    }

    public String getInfoName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.member.info.name", "&e\u516c\u4f1a\u4fe1\u606f"));
    }

    public List<String> getInfoLore() {
        return this.config.getStringList("gui.items.member.info.lore");
    }

    public Material getMembersMaterial() {
        return VersionCompat.getPlayerHeadMaterial();
    }

    public String getMembersName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.member.members.name", "&e\u516c\u4f1a\u6210\u5458"));
    }

    public List<String> getMembersLore() {
        return this.config.getStringList("gui.items.member.members.lore");
    }

    public Material getSettingsMaterial() {
        return this.getMaterialSafe("gui.items.member.settings.material", "REDSTONE", "REDSTONE_BLOCK");
    }

    public String getSettingsName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.member.settings.name", "&e\u4e2a\u4eba\u8bbe\u7f6e"));
    }

    public List<String> getSettingsLore() {
        return this.config.getStringList("gui.items.member.settings.lore");
    }

    public Material getLeaveMaterial() {
        return this.getMaterialSafe("gui.items.member.leave.material", "BARRIER", "BEDROCK");
    }

    public String getLeaveName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.member.leave.name", "&c\u79bb\u5f00\u516c\u4f1a"));
    }

    public List<String> getLeaveLore() {
        return this.config.getStringList("gui.items.member.leave.lore");
    }

    public Material getInviteToggleMaterial() {
        return this.getMaterialSafe("gui.items.member.invite_toggle.material", "LEVER", "STICK");
    }

    public String getInviteToggleName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.member.invite_toggle.name", "&e\u516c\u4f1a\u9080\u8bf7"));
    }

    public List<String> getInviteToggleLore() {
        return this.config.getStringList("gui.items.member.invite_toggle.lore");
    }

    public Material getNotifyToggleMaterial() {
        return this.getMaterialSafe("gui.items.member.notify_toggle.material", "NOTE_BLOCK", "JUKEBOX");
    }

    public String getNotifyToggleName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.member.notify_toggle.name", "&e\u4e0a\u4e0b\u7ebf\u901a\u77e5"));
    }

    public List<String> getNotifyToggleLore() {
        return this.config.getStringList("gui.items.member.notify_toggle.lore");
    }

    public Material getManageMaterial() {
        return this.getMaterialSafe("gui.items.officer.manage.material", "COMMAND_BLOCK", "COMMAND", "BEDROCK");
    }

    public String getManageName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.officer.manage.name", "&c\u7ba1\u7406\u516c\u4f1a"));
    }

    public List<String> getManageLore() {
        return this.config.getStringList("gui.items.officer.manage.lore");
    }

    public String getOwnerColor() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.colors.owner", "&c"));
    }

    public String getOfficerColor() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.colors.officer", "&6"));
    }

    public String getMemberColor() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.colors.member", "&a"));
    }

    public String getOnlineColor() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.colors.online", "&a"));
    }

    public String getOfflineColor() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.colors.offline", "&c"));
    }

    public String getBankTitle(String string) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.title.bank", "&6\u516c\u4f1a\u94f6\u884c: &e%name%").replace("%name%", string));
    }

    public String getBankBalanceName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.bank.balance.name", "&e\u5f53\u524d\u4f59\u989d"));
    }

    public String getBankBalanceLore(long l) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.bank.balance.lore", "&f\u4f59\u989d: &a%balance%").replace("%balance%", String.valueOf(l)));
    }

    public String getBankDepositName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.bank.deposit.name", "&a\u5b58\u5165\u8d44\u91d1"));
    }

    public String getBankDepositLore() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.bank.deposit.lore", "&f\u70b9\u51fb\u5b58\u5165\u8d44\u91d1\u5230\u516c\u4f1a\u94f6\u884c"));
    }

    public String getBankWithdrawName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.bank.withdraw.name", "&c\u53d6\u51fa\u8d44\u91d1"));
    }

    public String getBankWithdrawLore() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.bank.withdraw.lore", "&f\u70b9\u51fb\u4ece\u516c\u4f1a\u94f6\u884c\u53d6\u51fa\u8d44\u91d1"));
    }

    public String getBankName() {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)this.config.getString("gui.items.member.bank.name", "&e\u516c\u4f1a\u94f6\u884c"));
    }

    public List<String> getBankLore() {
        return this.config.getStringList("gui.items.member.bank.lore");
    }

    public int getNoGuildBarrierSlot() {
        return this.config.getInt("gui.items.no_guild.barrier.slot", 22);
    }

    public int getCreateSlot() {
        return this.config.getInt("gui.items.no_guild.create.slot", 11);
    }

    public int getViewAllSlot() {
        return this.config.getInt("gui.items.no_guild.view_all.slot", 15);
    }

    public int getBackSlot() {
        return this.config.getInt("gui.items.all_guilds.back.slot", 49);
    }

    public int getInfoSlot() {
        return this.config.getInt("gui.items.member.info.slot", 10);
    }

    public int getMembersSlot() {
        return this.config.getInt("gui.items.member.members.slot", 13);
    }

    public int getSettingsSlot() {
        return this.config.getInt("gui.items.member.settings.slot", 16);
    }

    public int getLeaveSlot() {
        return this.config.getInt("gui.items.member.leave.slot", 31);
    }

    public int getInviteToggleSlot() {
        return this.config.getInt("gui.items.member.invite_toggle.slot", 28);
    }

    public int getNotifyToggleSlot() {
        return this.config.getInt("gui.items.member.notify_toggle.slot", 34);
    }

    public int getManageSlot() {
        return this.config.getInt("gui.items.officer.manage.slot", 37);
    }

    public int getUpgradeSlot() {
        return this.config.getInt("gui.items.owner.upgrade.slot", 19);
    }

    public int getBuyExpSlot() {
        return this.config.getInt("gui.items.owner.buy_exp.slot", 25);
    }

    public int getBankSlot() {
        return this.config.getInt("gui.items.member.bank.slot", 22);
    }

    public int getBankBalanceSlot() {
        return this.config.getInt("gui.items.bank.balance.slot", 13);
    }

    public int getBankDepositSlot() {
        return this.config.getInt("gui.items.bank.deposit.slot", 20);
    }

    public int getBankWithdrawSlot() {
        return this.config.getInt("gui.items.bank.withdraw.slot", 24);
    }

    public int getBankBackSlot() {
        return this.config.getInt("gui.items.bank.back.slot", 40);
    }
}

