package com.guild.guild;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Guild {

    private String name;
    private String tag;
    private String tagColor;
    private UUID owner;
    private int level;
    private long experience;
    private long dailyExperience;
    private String motd;
    private boolean publicGuild;
    private long createdTime;
    private final Map<UUID, GuildMember> members = new ConcurrentHashMap<>();
    private final Map<String, GuildPermission> permissions = new ConcurrentHashMap<>();
    private GuildBank bank;

    public Guild(String name, UUID owner) {
        this.name = name;
        this.tag = name.substring(0, Math.min(4, name.length()));
        this.tagColor = "&f";
        this.owner = owner;
        this.level = 0;
        this.experience = 0L;
        this.dailyExperience = 0L;
        this.motd = "";
        this.publicGuild = true;
        this.createdTime = System.currentTimeMillis();
        GuildMember ownerMember = new GuildMember(owner, GuildRole.OWNER);
        members.put(owner, ownerMember);
        initializeDefaultPermissions();
        this.bank = new GuildBank();
    }

    private void initializeDefaultPermissions() {
        permissions.put("invite", GuildPermission.OWNER);
        permissions.put("kick", GuildPermission.OFFICER);
        permissions.put("promote", GuildPermission.OFFICER);
        permissions.put("demote", GuildPermission.OFFICER);
        permissions.put("chat", GuildPermission.MEMBER);
        permissions.put("motd", GuildPermission.OFFICER);
        permissions.put("settings", GuildPermission.OFFICER);
        permissions.put("disband", GuildPermission.OWNER);
        permissions.put("withdraw", GuildPermission.OFFICER);
    }

    // ========== 基础属性 ==========

    public int getMaxMembers() {
        return 25 + level * 5;
    }

    public boolean canAddMember() {
        return members.size() < getMaxMembers();
    }

    // ========== 成员管理 ==========

    public void addMember(UUID uuid, GuildRole role) {
        members.put(uuid, new GuildMember(uuid, role));
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public GuildMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    // ========== 经验与升级 ==========

    public void addExperience(long amount) {
        experience += amount;
        dailyExperience += amount;
        checkLevelUp();
    }

    private void checkLevelUp() {
        long required = getRequiredExperience();
        while (experience >= required && level < 100) {
            experience -= required;
            ++level;
            required = getRequiredExperience(); // 重新计算下一级所需经验
        }
    }

    public long getRequiredExperience() {
        return (long) (500.0 * level * (1 + level * 0.02));
    }

    // ========== 权限检查 ==========

    public boolean hasPermission(UUID playerUuid, String permissionKey) {
        GuildMember member = members.get(playerUuid);
        if (member == null) return false;
        GuildPermission perm = permissions.get(permissionKey);
        if (perm == null) return false;
        return member.getRole().getLevel() >= perm.getLevel();
    }

    // ========== 广播消息 ==========

    public void broadcast(String message) {
        for (UUID memberUuid : members.keySet()) {
            Player onlinePlayer = Bukkit.getPlayer(memberUuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.sendMessage(message);
            }
        }
    }

    public void broadcastToOfficers(String message) {
        for (GuildMember member : members.values()) {
            if (member.getRole() != GuildRole.OFFICER && member.getRole() != GuildRole.OWNER) continue;
            Player onlinePlayer = Bukkit.getPlayer(member.getUuid());
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.sendMessage(message);
            }
        }
    }

    // ========== Getter / Setter ==========

    public String getName() { return name; }
    public void setName(String name) { this.name = sanitizeInput(name); }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = sanitizeInput(tag); }

    public String getTagColor() { return tagColor; }
    public void setTagColor(String tagColor) { this.tagColor = tagColor; }

    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public long getExperience() { return experience; }
    public void setExperience(long experience) { this.experience = experience; }

    public long getDailyExperience() { return dailyExperience; }
    public void setDailyExperience(long dailyExperience) { this.dailyExperience = dailyExperience; }

    public String getMotd() { return motd; }
    public void setMotd(String motd) { this.motd = motd; }

    public boolean isPublicGuild() { return publicGuild; }
    public void setPublicGuild(boolean publicGuild) { this.publicGuild = publicGuild; }

    public long getCreatedTime() { return createdTime; }
    public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }

    public Map<UUID, GuildMember> getMembers() { return members; }
    public Map<String, GuildPermission> getPermissions() { return permissions; }
    public GuildBank getBank() { return bank; }
    public void setBank(GuildBank bank) { this.bank = bank; }

    /**
     * 净化用户输入，移除可能被滥用的颜色代码
     */
    private static String sanitizeInput(String input) {
        if (input == null) return "";
        // 移除 & 字符开头的颜色代码（防止颜色注入）
        return input.replace("&", "").replace("\u00a7", "");
    }
}
