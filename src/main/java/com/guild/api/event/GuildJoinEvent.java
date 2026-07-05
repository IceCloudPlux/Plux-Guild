package com.guild.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 公会加入事件
 * <p>
 * 当玩家加入公会时触发（通过邀请/申请/命令）。
 * 可通过 setCancelled(true) 阻止加入。
 */
public class GuildJoinEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String guildName;
    private boolean cancelled;

    public GuildJoinEvent(Player player, String guildName) {
        this.player = player;
        this.guildName = guildName;
        this.cancelled = false;
    }

    public Player getPlayer() { return player; }
    public String getGuildName() { return guildName; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
