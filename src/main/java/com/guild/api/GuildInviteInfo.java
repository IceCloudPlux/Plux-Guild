package com.guild.api;

import java.util.UUID;

/**
 * 公会邀请信息（只读视图）
 * <p>
 * 封装内部邀请数据，对外提供安全的只读访问。
 *
 * @since 3.1.0
 */
public final class GuildInviteInfo {

    private final String guildName;
    private final UUID inviterUuid;
    private final String inviterName;
    private final UUID targetUuid;
    private final String targetName;
    private final long inviteTime;

    public GuildInviteInfo(String guildName, UUID inviterUuid, String inviterName,
                           UUID targetUuid, String targetName, long inviteTime) {
        this.guildName = guildName;
        this.inviterUuid = inviterUuid;
        this.inviterName = inviterName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.inviteTime = inviteTime;
    }

    /** 被邀请的公会名称 */
    public String getGuildName() { return guildName; }

    /** 邀请者 UUID */
    public UUID getInviterUuid() { return inviterUuid; }

    /** 邀请者名称 */
    public String getInviterName() { return inviterName; }

    /** 被邀请者 UUID */
    public UUID getTargetUuid() { return targetUuid; }

    /** 被邀请者名称 */
    public String getTargetName() { return targetName; }

    /** 邀请时间戳（毫秒） */
    public long getInviteTime() { return inviteTime; }

    /** 邀请已存在多久（秒） */
    public long getAgeSeconds() {
        return (System.currentTimeMillis() - inviteTime) / 1000L;
    }

    @Override
    public String toString() {
        return String.format("GuildInviteInfo{guild=%s, from=%s, to=%s}",
                guildName, inviterName, targetName);
    }
}
