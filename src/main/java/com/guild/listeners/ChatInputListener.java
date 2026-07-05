package com.guild.listeners;

import com.guild.GuildPlugin;
import com.guild.gui.GuildBankGUI;
import com.guild.gui.GuildGUI;
import com.guild.gui.GuildManageGUI;
import com.guild.guild.Guild;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatInputListener implements Listener {

    private final GuildPlugin plugin;
    private final Map<UUID, PendingChatAction> pendingActions = new ConcurrentHashMap<>();

    public ChatInputListener(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerPendingAction(UUID playerUuid, String actionType, Object context) {
        pendingActions.put(playerUuid, new PendingChatAction(actionType, context));
    }

    public void removePendingAction(UUID playerUuid) {
        pendingActions.remove(playerUuid);
    }

    public boolean hasPendingAction(UUID playerUuid) {
        return pendingActions.containsKey(playerUuid);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingChatAction action = pendingActions.remove(player.getUniqueId());
        if (action == null) return; // 无待处理操作，不拦截

        event.setCancelled(true);
        String input = event.getMessage().trim();

        switch (action.getActionType()) {
            case "guild_name":
                handleGuildNameCreation(player, input);
                break;
            case "guild_tag":
                handleGuildTagSetting(player, input, action.getContext());
                break;
            case "guild_motd":
                handleGuildMotdSetting(player, input);
                break;
            case "bank_deposit":
                handleBankDeposit(player, input);
                break;
            case "bank_withdraw":
                handleBankWithdraw(player, input);
                break;
            default:
                break;
        }
    }

    private void handleGuildNameCreation(Player player, String input) {
        int minLen = plugin.getConfig().getInt("guild.min-name-length", 3);
        int maxLen = plugin.getConfig().getInt("guild.max-name-length", 16);
        
        if (input.length() < minLen || input.length() > maxLen) {
            player.sendMessage(plugin.getMessage("guild.name-length-invalid")
                    .replace("%min%", String.valueOf(minLen))
                    .replace("%max%", String.valueOf(maxLen)));
            return;
        }
        if (!input.matches("[a-zA-Z0-9\u4e00-\u9fa5_]+")) {
            player.sendMessage(plugin.getMessage("guild.name-chars-invalid"));
            return;
        }
        if (plugin.getGuildManager().getGuild(input) != null) {
            player.sendMessage(plugin.getMessage("guild.already-exists"));
            return;
        }
        plugin.getGuildManager().createGuild(input, player);
        player.sendMessage(plugin.getMessage("guild.created").replace("%name%", input));
        final Player finalPlayer = player;
        Bukkit.getScheduler().runTask(plugin, () -> GuildGUI.openGUI(plugin, finalPlayer));
    }

    private void handleGuildTagSetting(Player player, String input, Object context) {
        if (input.length() < 1 || input.length() > 6) {
            player.sendMessage(plugin.getMessage("guild.tag-length-invalid"));
            return;
        }
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "settings")) {
            player.sendMessage(plugin.getMessage("guild.no-permission"));
            return;
        }
        guild.setTag(input);
        plugin.getDatabaseManager().saveGuild(guild);
        player.sendMessage(plugin.getMessage("guild.tag-updated").replace("%tag%", input));
        final Player finalPlayer = player;
        final Guild finalGuild = guild;
        Bukkit.getScheduler().runTask(plugin, () -> GuildManageGUI.openGUI(plugin, finalPlayer, finalGuild));
    }

    private void handleGuildMotdSetting(Player player, String input) {
        if (input.length() > 100) {
            player.sendMessage(plugin.getMessage("guild.motd-too-long"));
            return;
        }
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "motd")) {
            player.sendMessage(plugin.getMessage("guild.no-permission"));
            return;
        }
        guild.setMotd(input);
        plugin.getDatabaseManager().saveGuild(guild);
        player.sendMessage(plugin.getMessage("guild.motd-updated"));
        final Player finalPlayer = player;
        final Guild finalGuild = guild;
        Bukkit.getScheduler().runTask(plugin, () -> GuildManageGUI.openGUI(plugin, finalPlayer, finalGuild));
    }

    private void handleBankDeposit(Player player, String input) {
        long amount = parsePositiveLong(player, input);
        if (amount <= 0L) return;

        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getMessage("guild.not-in-guild"));
            return;
        }
        if (plugin.getGuildManager().depositToBank(guild.getName(), player.getUniqueId(), amount)) {
            player.sendMessage(plugin.getMessage("bank.deposit-success")
                    .replace("%amount%", String.valueOf(amount)));
        } else {
            player.sendMessage(plugin.getMessage("bank.deposit-failed"));
        }
        final Player finalPlayer = player;
        final Guild finalGuild = guild;
        Bukkit.getScheduler().runTask(plugin, () -> GuildBankGUI.openBankGUI(plugin, finalPlayer, finalGuild));
    }

    private void handleBankWithdraw(Player player, String input) {
        long amount = parsePositiveLong(player, input);
        if (amount <= 0L) return;

        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getMessage("guild.not-in-guild"));
            return;
        }
        if (plugin.getGuildManager().withdrawFromBank(guild.getName(), player.getUniqueId(), amount)) {
            player.sendMessage(plugin.getMessage("bank.withdraw-success")
                    .replace("%amount%", String.valueOf(amount)));
        } else {
            player.sendMessage(plugin.getMessage("bank.withdraw-failed"));
        }
        final Player finalPlayer = player;
        final Guild finalGuild = guild;
        Bukkit.getScheduler().runTask(plugin, () -> GuildBankGUI.openBankGUI(plugin, finalPlayer, finalGuild));
    }

    /**
     * 解析正整数输入（存取款共用验证逻辑）
     *
     * @param player 目标玩家
     * @param input  玩家输入的文本
     * @return 解析成功的正整数，<= 0 表示解析失败（已发送错误消息）
     */
    private long parsePositiveLong(Player player, String input) {
        long amount;
        try {
            amount = Long.parseLong(input);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("bank.invalid-amount"));
            return -1L;
        }
        if (amount <= 0L) {
            player.sendMessage(plugin.getMessage("bank.amount-positive"));
            return -1L;
        }
        return amount;
    }

    public static class PendingChatAction {
        private final String actionType;
        private final Object context;

        public PendingChatAction(String actionType, Object context) {
            this.actionType = actionType;
            this.context = context;
        }

        public String getActionType() { return actionType; }
        public Object getContext() { return context; }
    }
}
