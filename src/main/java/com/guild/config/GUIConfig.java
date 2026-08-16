package com.guild.config;
import com.guild.GuildPlugin;
import com.guild.utils.VersionCompat;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public class GUIConfig extends SimpleFileConfig
{
    public GUIConfig(GuildPlugin plugin)
    {
        super(plugin, "gui.yml");
    }

    public void reloadConfig()
    {
        reload();
    }

    public String getMainTitle()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("title.main", "&6公会系统"));
    }

    public String getNoGuildTitle()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("title.no_guild", "&6公会系统"));
    }

    public String getAllGuildsTitle()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("title.all_guilds", "&6所有公会"));
    }

    public String getMemberTitle(String name)
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("title.member", "&6公会: &e%name%").replace("%name%", name));
    }

    public String getOfficerTitle(String name)
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("title.officer", "&6公会管理: &e%name%").replace("%name%", name));
    }

    public String getOwnerTitle(String name)
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("title.owner", "&6公会会长: &e%name%").replace("%name%", name));
    }

    public int getMainSize()
    {
        return config.getInt("size.main", 54);
    }

    public int getAllGuildsSize()
    {
        return config.getInt("size.all_guilds", 54);
    }

    private Material getMaterialSafe(String path, String... fallbacks)
    {
        String materialName = config.getString(path);
        if (materialName != null)
        {
            try
            {
                return Material.valueOf(materialName);
            }
            catch (IllegalArgumentException ignored)
            {
            }
        }
        for (String fallback : fallbacks)
        {
            try
            {
                return Material.valueOf(fallback);
            }
            catch (IllegalArgumentException ignored)
            {
            }
        }
        return Material.STONE;
    }

    public Material getNoGuildBarrierMaterial()
    {
        return getMaterialSafe("items.no_guild.barrier.material", "BARRIER", "BEDROCK");
    }

    public String getNoGuildBarrierName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.no_guild.barrier.name", "&c你还没有公会"));
    }

    public List<String> getNoGuildBarrierLore()
    {
        return config.getStringList("items.no_guild.barrier.lore");
    }

    public Material getCreateMaterial()
    {
        return getMaterialSafe("items.no_guild.create.material", "DIAMOND", "EMERALD");
    }

    public String getCreateName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.no_guild.create.name", "&a创建公会"));
    }

    public List<String> getCreateLore()
    {
        return config.getStringList("items.no_guild.create.lore");
    }

    public Material getViewAllMaterial()
    {
        return getMaterialSafe("items.no_guild.view_all.material", "BOOK", "PAPER");
    }

    public String getViewAllName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.no_guild.view_all.name", "&e查看所有公会"));
    }

    public List<String> getViewAllLore()
    {
        return config.getStringList("items.no_guild.view_all.lore");
    }

    public Material getGuildItemMaterial()
    {
        return getMaterialSafe("items.all_guilds.guild_item.material", "DIAMOND_BLOCK", "EMERALD_BLOCK");
    }

    public List<String> getGuildItemLore()
    {
        return config.getStringList("items.all_guilds.guild_item.lore");
    }

    public Material getBackMaterial()
    {
        return getMaterialSafe("items.all_guilds.back.material", "ARROW", "STICK");
    }

    public String getBackName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.all_guilds.back.name", "&c返回"));
    }

    public List<String> getBackLore()
    {
        return config.getStringList("items.all_guilds.back.lore");
    }

    public Material getInfoMaterial()
    {
        return getMaterialSafe("items.member.info.material", "PAPER", "BOOK");
    }

    public String getInfoName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.member.info.name", "&e公会信息"));
    }

    public List<String> getInfoLore()
    {
        return config.getStringList("items.member.info.lore");
    }

    public Material getMembersMaterial()
    {
        return VersionCompat.getPlayerHeadMaterial();
    }

    public String getMembersName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.member.members.name", "&e公会成员"));
    }

    public List<String> getMembersLore()
    {
        return config.getStringList("items.member.members.lore");
    }

    public Material getSettingsMaterial()
    {
        return getMaterialSafe("items.member.settings.material", "REDSTONE", "REDSTONE_BLOCK");
    }

    public String getSettingsName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.member.settings.name", "&e个人设置"));
    }

    public List<String> getSettingsLore()
    {
        return config.getStringList("items.member.settings.lore");
    }

    public Material getLeaveMaterial()
    {
        return getMaterialSafe("items.member.leave.material", "BARRIER", "BEDROCK");
    }

    public String getLeaveName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.member.leave.name", "&c离开公会"));
    }

    public List<String> getLeaveLore()
    {
        return config.getStringList("items.member.leave.lore");
    }

    public Material getInviteToggleMaterial()
    {
        return getMaterialSafe("items.member.invite_toggle.material", "LEVER", "STICK");
    }

    public String getInviteToggleName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.member.invite_toggle.name", "&e公会邀请"));
    }

    public List<String> getInviteToggleLore()
    {
        return config.getStringList("items.member.invite_toggle.lore");
    }

    public Material getNotifyToggleMaterial()
    {
        return getMaterialSafe("items.member.notify_toggle.material", "NOTE_BLOCK", "JUKEBOX");
    }

    public String getNotifyToggleName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.member.notify_toggle.name", "&e上下线通知"));
    }

    public List<String> getNotifyToggleLore()
    {
        return config.getStringList("items.member.notify_toggle.lore");
    }

    public Material getManageMaterial()
    {
        return getMaterialSafe("items.officer.manage.material", "COMMAND_BLOCK", "COMMAND", "BEDROCK");
    }

    public String getManageName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.officer.manage.name", "&c管理公会"));
    }

    public List<String> getManageLore()
    {
        return config.getStringList("items.officer.manage.lore");
    }

    public String getOwnerColor()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("colors.owner", "&c"));
    }

    public String getOfficerColor()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("colors.officer", "&6"));
    }

    public String getMemberColor()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("colors.member", "&a"));
    }

    public String getOnlineColor()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("colors.online", "&a"));
    }

    public String getOfflineColor()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("colors.offline", "&c"));
    }

    public String getBankTitle(String name)
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("title.bank", "&6公会银行: &e%name%").replace("%name%", name));
    }

    public String getBankBalanceName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.bank.balance.name", "&e当前余额"));
    }

    public String getBankBalanceLore(long balance)
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.bank.balance.lore", "&f余额: &a%balance%").replace("%balance%", String.valueOf(balance)));
    }

    public String getBankDepositName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.bank.deposit.name", "&a存入资金"));
    }

    public String getBankDepositLore()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.bank.deposit.lore", "&f点击存入资金到公会银行"));
    }

    public String getBankWithdrawName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.bank.withdraw.name", "&c取出资金"));
    }

    public String getBankWithdrawLore()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.bank.withdraw.lore", "&f点击从公会银行取出资金"));
    }

    public String getBankName()
    {
        return ChatColor.translateAlternateColorCodes('&', config.getString("items.member.bank.name", "&e公会银行"));
    }

    public List<String> getBankLore()
    {
        return config.getStringList("items.member.bank.lore");
    }

    public int getNoGuildBarrierSlot()
    {
        return config.getInt("items.no_guild.barrier.slot", 22);
    }

    public int getCreateSlot()
    {
        return config.getInt("items.no_guild.create.slot", 11);
    }

    public int getViewAllSlot()
    {
        return config.getInt("items.no_guild.view_all.slot", 15);
    }

    public int getBackSlot()
    {
        return config.getInt("items.all_guilds.back.slot", 49);
    }

    public int getInfoSlot()
    {
        return config.getInt("items.member.info.slot", 10);
    }

    public int getMembersSlot()
    {
        return config.getInt("items.member.members.slot", 13);
    }

    public int getSettingsSlot()
    {
        return config.getInt("items.member.settings.slot", 16);
    }

    public int getLeaveSlot()
    {
        return config.getInt("items.member.leave.slot", 31);
    }

    public int getInviteToggleSlot()
    {
        return config.getInt("items.member.invite_toggle.slot", 28);
    }

    public int getNotifyToggleSlot()
    {
        return config.getInt("items.member.notify_toggle.slot", 34);
    }

    public int getManageSlot()
    {
        return config.getInt("items.officer.manage.slot", 37);
    }

    public int getUpgradeSlot()
    {
        return config.getInt("items.owner.upgrade.slot", 19);
    }

    public int getBuyExpSlot()
    {
        return config.getInt("items.owner.buy_exp.slot", 25);
    }

    public int getBankSlot()
    {
        return config.getInt("items.member.bank.slot", 22);
    }

    public int getBankBalanceSlot()
    {
        return config.getInt("items.bank.balance.slot", 13);
    }

    public int getBankDepositSlot()
    {
        return config.getInt("items.bank.deposit.slot", 20);
    }

    public int getBankWithdrawSlot()
    {
        return config.getInt("items.bank.withdraw.slot", 24);
    }

    public int getBankBackSlot()
    {
        return config.getInt("items.bank.back.slot", 40);
    }
}
