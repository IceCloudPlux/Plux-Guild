package com.guild.papi;

import com.guild.GuildPlugin;
import com.guild.guild.Guild;
import com.guild.guild.GuildMember;
import com.guild.guild.GuildRole;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.bukkit.entity.Player;

public class GuildPlaceholderExpansion {
    private final GuildPlugin plugin;

    public GuildPlaceholderExpansion(GuildPlugin guildPlugin) {
        this.plugin = guildPlugin;
    }

    public boolean register() {
        try {
            Class<?> clazz = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            Object object = this.createExpansion(clazz);
            Method method = clazz.getDeclaredMethod("register", new Class[0]);
            method.invoke(object, new Object[0]);
            return true;
        }
        catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private Object createExpansion(Class<?> clazz) throws Exception {
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, (object, method, objectArray) -> {
            String string = method.getName();
            if (string.equals("getIdentifier")) {
                return "guild";
            }
            if (string.equals("getAuthor")) {
                return "ya_xzer21145";
            }
            if (string.equals("getVersion")) {
                return "2.0.1";
            }
            if (string.equals("persist")) {
                return true;
            }
            if (string.equals("onPlaceholderRequest") && objectArray.length == 2) {
                Player player = (Player)objectArray[0];
                String string2 = (String)objectArray[1];
                return this.handlePlaceholderRequest(player, string2);
            }
            return method.getDefaultValue();
        });
    }

    private String handlePlaceholderRequest(Player player, String string) {
        if (player == null) {
            return null;
        }
        UUID uUID = player.getUniqueId();
        Guild guild = this.plugin.getGuildManager().getPlayerGuild(uUID);
        if (string.equalsIgnoreCase("name")) {
            return guild != null ? guild.getName() : "";
        }
        if (string.equalsIgnoreCase("tag")) {
            return guild != null ? guild.getTag() : "";
        }
        if (string.equalsIgnoreCase("tag_color")) {
            return guild != null ? guild.getTagColor() : "";
        }
        if (string.equalsIgnoreCase("level")) {
            return guild != null ? String.valueOf(guild.getLevel()) : "0";
        }
        if (string.equalsIgnoreCase("experience")) {
            return guild != null ? String.valueOf(guild.getExperience()) : "0";
        }
        if (string.equalsIgnoreCase("required_experience")) {
            return guild != null ? String.valueOf(guild.getRequiredExperience()) : "0";
        }
        if (string.equalsIgnoreCase("owner")) {
            if (guild == null) {
                return "";
            }
            return this.plugin.getServer().getOfflinePlayer(guild.getOwner()).getName();
        }
        if (string.equalsIgnoreCase("member_count")) {
            return guild != null ? String.valueOf(guild.getMembers().size()) : "0";
        }
        if (string.equalsIgnoreCase("motd")) {
            return guild != null ? guild.getMotd() : "";
        }
        if (string.equalsIgnoreCase("role")) {
            if (guild == null) {
                return "";
            }
            GuildMember guildMember = guild.getMember(uUID);
            return guildMember != null ? guildMember.getRole().getDisplayName() : "";
        }
        if (string.equalsIgnoreCase("role_level")) {
            if (guild == null) {
                return "0";
            }
            GuildMember guildMember = guild.getMember(uUID);
            return guildMember != null ? String.valueOf(guildMember.getRole().getLevel()) : "0";
        }
        if (string.equalsIgnoreCase("contribution")) {
            if (guild == null) {
                return "0";
            }
            GuildMember guildMember = guild.getMember(uUID);
            return guildMember != null ? String.valueOf(guildMember.getTotalContribution()) : "0";
        }
        if (string.equalsIgnoreCase("daily_contribution")) {
            if (guild == null) {
                return "0";
            }
            GuildMember guildMember = guild.getMember(uUID);
            return guildMember != null ? String.valueOf(guildMember.getDailyContribution()) : "0";
        }
        if (string.equalsIgnoreCase("joined_time")) {
            if (guild == null) {
                return "0";
            }
            GuildMember guildMember = guild.getMember(uUID);
            return guildMember != null ? String.valueOf(guildMember.getJoinedTime()) : "0";
        }
        if (string.equalsIgnoreCase("is_owner")) {
            if (guild == null) {
                return "false";
            }
            return String.valueOf(guild.getOwner().equals(uUID));
        }
        if (string.equalsIgnoreCase("is_officer")) {
            if (guild == null) {
                return "false";
            }
            GuildMember guildMember = guild.getMember(uUID);
            return guildMember != null ? String.valueOf(guildMember.getRole() == GuildRole.OFFICER) : "false";
        }
        if (string.equalsIgnoreCase("is_member")) {
            return String.valueOf(guild != null);
        }
        if (string.equalsIgnoreCase("bank_balance")) {
            return guild != null ? String.valueOf(guild.getBank().getBalance()) : "0";
        }
        if (string.equalsIgnoreCase("currency")) {
            return String.valueOf(this.plugin.getGuildManager().getPlayerGuildCurrency(uUID));
        }
        if (string.equalsIgnoreCase("has_guild")) {
            return String.valueOf(guild != null);
        }
        return null;
    }
}

