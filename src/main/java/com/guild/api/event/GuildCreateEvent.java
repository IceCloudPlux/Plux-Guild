package com.guild.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 公会创建事件
 * <p>
 * 当玩家通过命令或 GUI 创建新公会时触发。
 * 可通过 setCancelled(true) 阻止创建。
 */
public class GuildCreateEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player creator;
    private String guildName;
    private String guildTag;
    private boolean cancelled;

    public GuildCreateEvent(Player creator, String guildName, String guildTag) {
        this.creator = creator;
        this.guildName = guildName;
        this.guildTag = guildTag != null ? guildTag : "";
        this.cancelled = false;
    }

    /** 获取创建者 */
    public Player getCreator() { return creator; }

    /** 获取/设置公会名称（允许修改） */
    public String getGuildName() { return guildName; }
    public void setGuildName(String name) { this.guildName = name; }

    /** 获取/设置公会标签（允许修改） */
    public String getGuildTag() { return guildTag; }
    public void setGuildTag(String tag) { this.guildTag = tag; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
