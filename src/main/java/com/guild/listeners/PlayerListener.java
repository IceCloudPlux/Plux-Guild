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

public class PlayerListener
implements Listener {
    private final GuildPlugin plugin;

    public PlayerListener(GuildPlugin guildPlugin) {
        this.plugin = guildPlugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();
        Guild guild = this.plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild != null) {
            for (GuildMember guildMember : guild.getMembers().values()) {
                Player player2;
                PlayerSettings playerSettings;
                if (guildMember.getUuid().equals(player.getUniqueId()) || !(playerSettings = this.plugin.getGuildManager().getPlayerSettings(guildMember.getUuid())).isNotifyOnlineStatus() || (player2 = Bukkit.getPlayer((UUID)guildMember.getUuid())) == null || !player2.isOnline()) continue;
                player2.sendMessage(ChatColor.YELLOW + player.getName() + " \u4e0a\u7ebf\u4e86");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        Player player = playerQuitEvent.getPlayer();
        Guild guild = this.plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild != null) {
            for (GuildMember guildMember : guild.getMembers().values()) {
                Player player2;
                PlayerSettings playerSettings;
                if (guildMember.getUuid().equals(player.getUniqueId()) || !(playerSettings = this.plugin.getGuildManager().getPlayerSettings(guildMember.getUuid())).isNotifyOnlineStatus() || (player2 = Bukkit.getPlayer((UUID)guildMember.getUuid())) == null || !player2.isOnline()) continue;
                player2.sendMessage(ChatColor.YELLOW + player.getName() + " \u4e0b\u7ebf\u4e86");
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent playerDeathEvent) {
        Player player = playerDeathEvent.getEntity();
        Player player2 = player.getKiller();
        if (player2 != null && player2 != player) {
            long l = this.plugin.getConfig().getLong("experience.player-kill", 50L);
            this.plugin.getGuildManager().addExperience(player2.getUniqueId(), l);
        }
    }
}

