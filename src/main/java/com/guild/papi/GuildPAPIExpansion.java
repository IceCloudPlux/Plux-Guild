package com.guild.papi;

import com.guild.GuildPlugin;
import com.guild.guild.Guild;
import com.guild.guild.GuildMember;
import com.guild.guild.GuildRole;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class GuildPAPIExpansion extends PlaceholderExpansion {
    
    private final GuildPlugin plugin;

    public GuildPAPIExpansion(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "guild";
    }

    @Override
    public String getAuthor() {
        return "ya_xzer21145";
    }

    @Override
    public String getVersion() {
        return "3.0.3";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";
        UUID uuid = player.getUniqueId();
        if (uuid == null) return "";
        
        Guild guild = plugin.getGuildManager().getPlayerGuild(uuid);
        
        switch (params.toLowerCase()) {
            case "name":
                return guild != null ? guild.getName() : "";
            case "tag":
                return guild != null ? guild.getTag() : "";
            case "tag_color":
                return guild != null ? guild.getTagColor() : "";
            case "level":
                return guild != null ? String.valueOf(guild.getLevel()) : "0";
            case "experience":
                return guild != null ? String.valueOf(guild.getExperience()) : "0";
            case "required_experience":
                return guild != null ? String.valueOf(guild.getRequiredExperience()) : "0";
            case "owner":
                if (guild == null || guild.getOwner() == null) return "";
                String ownerName = plugin.getServer().getOfflinePlayer(guild.getOwner()).getName();
                return ownerName != null ? ownerName : "";
            case "member_count":
                return guild != null ? String.valueOf(guild.getMembers().size()) : "0";
            case "motd":
                return guild != null ? guild.getMotd() : "";
            case "role":
                if (guild == null) return "";
                GuildMember member = guild.getMember(uuid);
                return member != null && member.getRole() != null ? member.getRole().getDisplayName() : "";
            case "role_level":
                if (guild == null) return "0";
                GuildMember m = guild.getMember(uuid);
                return m != null && m.getRole() != null ? String.valueOf(m.getRole().getLevel()) : "0";
            case "contribution":
                if (guild == null) return "0";
                GuildMember cm = guild.getMember(uuid);
                return cm != null ? String.valueOf(cm.getTotalContribution()) : "0";
            case "daily_contribution":
                if (guild == null) return "0";
                GuildMember dm = guild.getMember(uuid);
                return dm != null ? String.valueOf(dm.getDailyContribution()) : "0";
            case "joined_time":
                if (guild == null) return "0";
                GuildMember jm = guild.getMember(uuid);
                return jm != null ? String.valueOf(jm.getJoinedTime()) : "0";
            case "is_owner":
                return guild != null && guild.getOwner() != null && guild.getOwner().equals(uuid) ? "true" : "false";
            case "is_officer":
                if (guild == null) return "false";
                GuildMember om = guild.getMember(uuid);
                return om != null && om.getRole() == GuildRole.OFFICER ? "true" : "false";
            case "is_member":
                return guild != null ? "true" : "false";
            case "bank_balance":
                return guild != null && guild.getBank() != null ? String.valueOf(guild.getBank().getBalance()) : "0";
            case "currency":
                return String.valueOf(plugin.getGuildManager().getPlayerGuildCurrency(uuid));
            case "has_guild":
                return guild != null ? "true" : "false";
            default:
                return "";
        }
    }
}
