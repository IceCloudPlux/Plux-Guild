package com.guild.api;

import com.guild.guild.Guild;
import com.guild.guild.GuildMember;
import com.guild.guild.GuildRole;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 公会数据只读视图
 * <p>
 * 封装内部 {@link Guild} 对象，对外提供安全的只读访问。
 * 所有返回值都是防御性拷贝或不可修改视图，防止外部修改内部状态。
 *
 * @since 3.1.0
 */
public final class GuildData {

    private final Guild guild;

    public GuildData(Guild guild) {
        this.guild = Objects.requireNonNull(guild, "guild cannot be null");
    }

    // ========== 基础属性 ==========

    /** 公会名称 */
    public String getName() { return guild.getName(); }

    /** 公会标签 */
    public String getTag() { return guild.getTag(); }

    /** 标签颜色代码（如 &6） */
    public String getTagColor() { return guild.getTagColor(); }

    /** 带颜色的标签显示文本 */
    public String getDisplayTag() { return guild.getTagColor() + guild.getTag(); }

    /** 会长 UUID */
    public UUID getOwnerUuid() { return guild.getOwner(); }

    /** 会长名称（可能为 null 如果从未上线） */
    public String getOwnerName() {
        OfflinePlayer op = Bukkit.getOfflinePlayer(guild.getOwner());
        return op != null ? op.getName() : "未知";
    }

    /** 等级 (0-100) */
    public int getLevel() { return guild.getLevel(); }

    /** 当前经验值 */
    public long getExperience() { return guild.getExperience(); }

    /** 升级所需经验 */
    public long getRequiredExperience() { return guild.getRequiredExperience(); }

    /** 今日累计经验 */
    public long getDailyExperience() { return guild.getDailyExperience(); }

    /** 公会公告 */
    public String getMotd() { return guild.getMotd(); }

    /** 是否公开公会（可被搜索/申请） */
    public boolean isPublicGuild() { return guild.isPublicGuild(); }

    /** 创建时间戳（毫秒） */
    public long getCreatedTime() { return guild.getCreatedTime(); }

    // ========== 成员信息 ==========

    /** 当前成员数量 */
    public int getMemberCount() { return guild.getMembers().size(); }

    /** 最大成员数（基于等级） */
    public int getMaxMembers() { return guild.getMaxMembers(); }

    /** 是否可以添加新成员 */
    public boolean canAddMember() { return guild.canAddMember(); }

    /** 成员容量使用率 (0.0 ~ 1.0+) */
    public double getCapacityUsage() {
        return (double) guild.getMembers().size() / guild.getMaxMembers();
    }

    /**
     * 获取所有成员 UUID 的不可修改副本
     */
    public Set<UUID> getMemberUuids() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(guild.getMembers().keySet()));
    }

    /**
     * 获取在线成员 UUID 列表
     */
    public List<UUID> getOnlineMembers() {
        return guild.getMembers().keySet().stream()
                .filter(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    return p != null && p.isOnline();
                })
                .collect(Collectors.toList());
    }

    /** 在线成员数 */
    public int getOnlineCount() {
        return (int) guild.getMembers().keySet().stream()
                .filter(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    return p != null && p.isOnline();
                })
                .count();
    }

    // ========== 银行 ==========

    /** 银行余额 */
    public long getBankBalance() { return guild.getBank().getBalance(); }

    // ========== 权限检查 ==========

    /** 指定玩家是否拥有某权限 */
    public boolean hasPermission(UUID playerUuid, String permissionKey) {
        return guild.hasPermission(playerUuid, permissionKey);
    }

    /** 指定玩家是否是会长 */
    public boolean isOwner(UUID playerUuid) {
        return guild.getOwner().equals(playerUuid);
    }

    /** 指定玩家是否是管理员或会长 */
    public boolean isOfficerOrAbove(UUID playerUuid) {
        GuildMember member = guild.getMember(playerUuid);
        if (member == null) return false;
        GuildRole role = member.getRole();
        return role.name().equals("OWNER") || role.name().equals("OFFICER");
    }

    // ========== 内部对象访问（高级用法）==========

    /**
     * 获取底层 Guild 对象（仅供高级操作，请谨慎使用）
     *
     * @return 内部 Guild 实例
     */
    public Guild getInternalGuild() {
        return guild;
    }

    // ========== Object 方法 ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GuildData)) return false;
        return guild.getName().equalsIgnoreCase(((GuildData) o).guild.getName());
    }

    @Override
    public int hashCode() {
        return guild.getName().toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return String.format("GuildData{name=%s, tag=%s, level=%d, members=%d/%d}",
                getName(), getTag(), getLevel(), getMemberCount(), getMaxMembers());
    }
}
