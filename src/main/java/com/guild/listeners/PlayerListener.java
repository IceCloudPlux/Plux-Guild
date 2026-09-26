package com.guild.listeners;
import com.guild.GuildPlugin;
import com.guild.guild.Guild;
import com.guild.guild.GuildMember;
import com.guild.guild.PlayerSettings;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener
{
    private final GuildPlugin plugin;

    public PlayerListener(GuildPlugin guildPlugin)
    {
        this.plugin = guildPlugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        // 异步确保公会币账户行存在（首次加入发放 initial-balance 初始余额）
        plugin.getGuildManager().ensurePlayerCurrencyRow(player.getUniqueId());
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) return;
        for (GuildMember member : guild.getMembers().values())
        {
            if (member.getUuid().equals(player.getUniqueId())) continue;
            PlayerSettings settings = plugin.getGuildManager().getPlayerSettings(member.getUuid());
            if (!settings.isNotifyOnlineStatus()) continue;
            Player online = Bukkit.getPlayer(member.getUuid());
            if (online != null && online.isOnline())
            {
                online.sendMessage(ChatColor.YELLOW + player.getName() + " 上线了");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) return;
        for (GuildMember member : guild.getMembers().values())
        {
            if (member.getUuid().equals(player.getUniqueId())) continue;
            PlayerSettings settings = plugin.getGuildManager().getPlayerSettings(member.getUuid());
            if (!settings.isNotifyOnlineStatus()) continue;
            Player online = Bukkit.getPlayer(member.getUuid());
            if (online != null && online.isOnline())
            {
                online.sendMessage(ChatColor.YELLOW + player.getName() + " 下线了");
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)
    {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && killer != victim)
        {
            long exp = plugin.getExperienceConfig().getPlayerKillExp();
            plugin.getGuildManager().addExperience(killer.getUniqueId(), exp);
        }
    }
}
