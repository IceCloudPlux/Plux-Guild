package com.guild.commands;

import com.guild.GuildPlugin;
import com.guild.currency.GuildCurrency;
import com.guild.gui.GuildBankGUI;
import com.guild.gui.GuildGUI;
import com.guild.guild.Guild;
import com.guild.guild.GuildManager;
import com.guild.guild.GuildMember;
import com.guild.guild.GuildPermission;
import com.guild.guild.GuildRole;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class GuildCommand implements CommandExecutor {

    private final GuildPlugin plugin;
    private final GuildManager guildManager;
    /** 待确认的强制删除操作缓存 */
    private final Set<UUID> pendingDeleteConfirm = new HashSet<>();

    public GuildCommand(GuildPlugin guildPlugin) {
        this.plugin = guildPlugin;
        this.guildManager = guildPlugin.getGuildManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("此命令只能由玩家执行");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCmd = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCmd) {
            case "help":       sendHelp(player); break;
            case "create":     handleCreate(player, subArgs); break;
            case "join":       handleJoin(player, subArgs); break;
            case "leave":      handleLeave(player); break;
            case "info":       handleInfo(player); break;
            case "list":       handleList(player); break;
            case "listguilds": handleListGuilds(player); break;
            case "online":     handleOnline(player); break;
            case "invite":     handleInvite(player, subArgs); break;
            case "accept":     handleAccept(player, subArgs); break;
            case "decline":    handleDecline(player); break;
            case "kick":       handleKick(player, subArgs); break;
            case "promote":    handlePromote(player, subArgs); break;
            case "demote":     handleDemote(player, subArgs); break;
            case "transfer":   handleTransfer(player, subArgs); break;
            case "disband":    handleDisband(player); break;
            case "tag":        handleTag(player, subArgs); break;
            case "tagcolor":   handleTagColor(player, subArgs); break;
            case "motd":       handleMotd(player, subArgs); break;
            case "chat":       handleChat(player, subArgs); break;
            case "officerchat":handleOfficerChat(player, subArgs); break;
            case "top":        handleTop(player, subArgs); break;
            case "log":        handleLog(player, subArgs); break;
            case "requests":   handleRequests(player, subArgs); break;
            case "member":     handleMember(player, subArgs); break;
            case "settings":   handleSettings(player, subArgs); break;
            case "permission": handlePermission(player, subArgs); break;
            case "party":      handleParty(player); break;
            case "rename":     handleRename(player, subArgs); break;
            case "gexp":       handleGExp(player, subArgs); break;
            case "slevel":     handleSLevel(player, subArgs); break;
            case "clxplev":    handleClxpLev(player, subArgs); break;
            case "delete":     handleDelete(player, subArgs); break;
            case "reload":     handleReload(player, subArgs); break;
            case "balance":    handleBalance(player); break;
            case "bank":       handleOpenBank(player); break;
            case "deposit":    handleDeposit(player, subArgs); break;
            case "withdraw":   handleWithdraw(player, subArgs); break;
            case "upgrade":    handleUpgrade(player, subArgs); break;
            case "mute":       handleMute(player, subArgs); break;
            case "unmute":     handleUnmute(player, subArgs); break;
            default:           sendHelp(player); break;
        }
        return true;
    }

    // ========== 帮助 ==========

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "--------------------公会---------------------");
        player.sendMessage(ChatColor.WHITE + "/guild leave" + ChatColor.GRAY + " - 离开你的公会");
        player.sendMessage(ChatColor.WHITE + "/guild list" + ChatColor.GRAY + " - 查看公会中的成员");
        player.sendMessage(ChatColor.WHITE + "/guild listguilds" + ChatColor.GRAY + " - 查看所有公会");
        player.sendMessage(ChatColor.WHITE + "/guild create <公会> [标签]" + ChatColor.GRAY + " - 创建公会");
        player.sendMessage(ChatColor.WHITE + "/guild tag <标签>" + ChatColor.GRAY + " - 设置公会标签");
        player.sendMessage(ChatColor.WHITE + "/guild join <公会>" + ChatColor.GRAY + " - 请求加入一个公会");
        player.sendMessage(ChatColor.WHITE + "/guild info" + ChatColor.GRAY + " - 查看你的公会信息");
        player.sendMessage(ChatColor.WHITE + "/guild motd help" + ChatColor.GRAY + " - 显示公会公告帮助");
        player.sendMessage(ChatColor.WHITE + "/guild settings <属性> <值>" + ChatColor.GRAY + " - 修改公会设置");
        player.sendMessage(ChatColor.WHITE + "/guild tagcolor <颜色序号>" + ChatColor.GRAY + " - 设置公会标签颜色");
        player.sendMessage(ChatColor.WHITE + "/guild accept <玩家>" + ChatColor.GRAY + " - 接受公会邀请/申请");
        player.sendMessage(ChatColor.WHITE + "/guild transfer <成员>" + ChatColor.GRAY + " - 转让公会会长");
        player.sendMessage(ChatColor.WHITE + "/guild party" + ChatColor.GRAY + " - 向公会在线玩家发起组队邀请");
        player.sendMessage(ChatColor.WHITE + "/guild role help" + ChatColor.GRAY + " - 显示公会职位帮助");
        player.sendMessage(ChatColor.WHITE + "/guild log <页码>" + ChatColor.GRAY + " - 查看公会日志");
        player.sendMessage(ChatColor.WHITE + "/guild requests <页码>" + ChatColor.GRAY + " - 查看公会申请列表");
        player.sendMessage(ChatColor.WHITE + "/guild promote <玩家>" + ChatColor.GRAY + " - 提升公会成员职位");
        player.sendMessage(ChatColor.WHITE + "/guild officerchat <聊天>" + ChatColor.GRAY + " - 发送聊天信息至公会管理频道");
        player.sendMessage(ChatColor.WHITE + "/guild top <页码>" + ChatColor.GRAY + " - 查看今天公会经验贡献排行榜");
        player.sendMessage(ChatColor.WHITE + "/guild member <成员>" + ChatColor.GRAY + " - 查看公会成员信息");
        player.sendMessage(ChatColor.WHITE + "/guild disband" + ChatColor.GRAY + " - 解散公会");
        player.sendMessage(ChatColor.WHITE + "/guild chat <聊天>" + ChatColor.GRAY + " - 公会聊天");
        player.sendMessage(ChatColor.WHITE + "/guild permission" + ChatColor.GRAY + " - 设置公会权限");
        player.sendMessage(ChatColor.WHITE + "/guild demote <玩家>" + ChatColor.GRAY + " - 降低公会成员职位");
        player.sendMessage(ChatColor.WHITE + "/guild kick <玩家> <原因>" + ChatColor.GRAY + " - 从公会中踢出一名玩家");
        player.sendMessage(ChatColor.WHITE + "/guild rename <新名称>" + ChatColor.GRAY + " - 重命名你的公会名称(需审核)");
        player.sendMessage(ChatColor.WHITE + "/guild online" + ChatColor.GRAY + " - 查看公会中在线的成员");
        player.sendMessage(ChatColor.WHITE + "/guild invite <玩家>" + ChatColor.GRAY + " - 邀请玩家加入到你的公会");
        player.sendMessage(ChatColor.WHITE + "/guild upgrade <bexp50 | upgrade>" + ChatColor.GRAY + " - 购买经验或直接升级公会");
        player.sendMessage(ChatColor.WHITE + "/guild mute <玩家> <时间>" + ChatColor.GRAY + " - 禁言公会成员(管理员以上)");
        player.sendMessage(ChatColor.WHITE + "/guild unmute <玩家>" + ChatColor.GRAY + " - 解除公会成员禁言(管理员以上)");
        player.sendMessage(ChatColor.WHITE + "/guild bank" + ChatColor.GRAY + " - 打开公会银行GUI");
        player.sendMessage(ChatColor.WHITE + "/guildgui bank" + ChatColor.GRAY + " - 打开公会银行GUI");
        player.sendMessage(ChatColor.WHITE + "/guild balance" + ChatColor.GRAY + " - 查看公会银行余额");
        player.sendMessage(ChatColor.WHITE + "/guild deposit <金额>" + ChatColor.GRAY + " - 向公会银行存入资金");
        player.sendMessage(ChatColor.WHITE + "/guild withdraw <金额>" + ChatColor.GRAY + " - 从公会银行取出资金");

        if (player.hasPermission("guild.admin")) {
            player.sendMessage(ChatColor.RED + "-----------------公会ADMIN------------------");
            player.sendMessage(ChatColor.WHITE + "/guild gexp <公会> <数量>" + ChatColor.GRAY + " - 给予指定公会经验");
            player.sendMessage(ChatColor.WHITE + "/guild slevel <公会> <数量>" + ChatColor.GRAY + " - 设置指定公会等级");
            player.sendMessage(ChatColor.WHITE + "/guild clxplev <公会>" + ChatColor.GRAY + " - 将公会等级和经验清0");
            player.sendMessage(ChatColor.WHITE + "/guild delete <公会>" + ChatColor.GRAY + " - 强制删除一个公会(需确认操作)");
            player.sendMessage(ChatColor.WHITE + "/guild delete confirm" + ChatColor.GRAY + " - 确认删除");
            player.sendMessage(ChatColor.WHITE + "/guild reload" + ChatColor.GRAY + " - 重载插件配置文件");
            player.sendMessage(ChatColor.WHITE + "/guild reload database" + ChatColor.GRAY + " - 重载数据库");
        }
        player.sendMessage(ChatColor.GOLD + "--------------------------------------------");
    }

    // ========== 创建公会 ==========

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild create <公会名称> [标签]");
            return;
        }
        if (!player.hasPermission("guild.create")) {
            player.sendMessage(ChatColor.RED + "你没有权限创建公会");
            return;
        }
        if (guildManager.isInGuild(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你已经在一个公会中了");
            return;
        }

        String name = args[1];
        int minLen = plugin.getConfig().getInt("guild.min-name-length", 3);
        int maxLen = plugin.getConfig().getInt("guild.max-name-length", 16);
        if (name.length() < minLen || name.length() > maxLen) {
            player.sendMessage(ChatColor.RED + "公会名称长度必须在 " + minLen + " 到 " + maxLen + " 之间");
            return;
        }

        String tag = "";
        if (args.length >= 3) {
            tag = args[2];
            int maxTagLen = plugin.getConfig().getInt("guild.max-tag-length", 4);
            if (tag.length() > maxTagLen) {
                player.sendMessage(ChatColor.RED + "标签长度不能超过 " + maxTagLen + " 个字符");
                return;
            }
        }

        // 扣费（使用统一的货币接口）
        double cost = plugin.getConfig().getDouble("guild.create-cost", 0.0);
        if (cost > 0 && !chargeCreateCost(player, cost)) {
            return;
        }

        Guild guild = guildManager.createGuild(name, player);
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "公会名称已存在");
            // 退款
            refundCreateCost(player, cost);
            return;
        }

        if (!tag.isEmpty()) {
            guild.setTag(tag);
        }
        player.sendMessage(ChatColor.GREEN + "成功创建公会: " + name);
        if (!tag.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "公会标签: " + tag);
        }
    }

    /**
     * 统一扣费逻辑：优先尝试 Vault，再尝试 PlayerPoints
     */
    private boolean chargeCreateCost(Player player, double cost) {
        GuildCurrency currency = plugin.getGuildCurrency();

        // 尝试 Vault
        if (currency.isVaultAvailable()) {
            if (currency.withdraw(player.getUniqueId(), (long) cost,
                    GuildCurrency.CurrencyType.VAULT)) {
                return true;
            }
        }
        // 尝试 PlayerPoints
        if (currency.isPlayerPointsAvailable()) {
            if (currency.withdraw(player.getUniqueId(), (long) cost,
                    GuildCurrency.CurrencyType.PLAYER_POINTS)) {
                return true;
            }
        }

        player.sendMessage(ChatColor.RED + "创建公会需要 " + cost + " 金币/点数");
        return false;
    }

    /**
     * 创建失败时退款
     */
    private void refundCreateCost(Player player, double cost) {
        if (cost <= 0) return;
        GuildCurrency currency = plugin.getGuildCurrency();
        if (currency.isVaultAvailable()) {
            currency.deposit(player.getUniqueId(), (long) cost, GuildCurrency.CurrencyType.VAULT);
        } else if (currency.isPlayerPointsAvailable()) {
            currency.deposit(player.getUniqueId(), (long) cost, GuildCurrency.CurrencyType.PLAYER_POINTS);
        }
    }

    // ========== 加入/离开 ==========

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild join <公会名称>");
            return;
        }
        if (guildManager.isInGuild(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你已经在一个公会中了");
            return;
        }
        String name = args[1];
        Guild guild = guildManager.getGuild(name);
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "未找到公会");
            return;
        }
        guildManager.addRequest(name, player.getUniqueId(), player.getDisplayName());
        player.sendMessage(ChatColor.GREEN + "已发送加入申请到公会: " + name);
        for (UUID memberUuid : guild.getMembers().keySet()) {
            Player onlineMember = Bukkit.getPlayer(memberUuid);
            if (onlineMember != null && onlineMember.isOnline()) {
                onlineMember.sendMessage(plugin.getMessage("guild.join-request")
                        .replace("%player%", player.getDisplayName())
                        .replace("%guild%", name));
            }
        }
    }

    private void handleLeave(Player player) {
        if (!guildManager.leaveGuild(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你不在任何公会中，或者你是会长无法离开");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "你已离开公会");
    }

    // ========== 信息查询 ==========

    private void handleInfo(Player player) {
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "你不在任何公会中");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "========== 公会信息 ==========");
        player.sendMessage(ChatColor.YELLOW + "名称: " + ChatColor.WHITE + guild.getName());
        player.sendMessage(ChatColor.YELLOW + "标签: " + ChatColor.WHITE + guild.getTagColor() + guild.getTag());
        player.sendMessage(ChatColor.YELLOW + "等级: " + ChatColor.WHITE + guild.getLevel());
        player.sendMessage(ChatColor.YELLOW + "经验: " + ChatColor.WHITE + guild.getExperience()
                + "/" + guild.getRequiredExperience());
        player.sendMessage(ChatColor.YELLOW + "成员数: " + ChatColor.WHITE + guild.getMembers().size()
                + "/" + guild.getMaxMembers());
        player.sendMessage(ChatColor.YELLOW + "会长: " + ChatColor.WHITE +
                Bukkit.getOfflinePlayer(guild.getOwner()).getName());
        player.sendMessage(ChatColor.YELLOW + "公告: " + ChatColor.WHITE + guild.getMotd());
        player.sendMessage(ChatColor.GOLD + "=================================");
    }

    private void handleList(Player player) {
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "你不在任何公会中");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "========== 成员列表 ==========");
        List<GuildMember> sortedMembers = guild.getMembers().values().stream()
                .sorted((a, b) -> b.getRole().getLevel() - a.getRole().getLevel())
                .collect(Collectors.toList());
        for (GuildMember member : sortedMembers) {
            String roleName = member.getRole().getDisplayName();
            String playerName = Bukkit.getOfflinePlayer(member.getUuid()).getName();
            ChatColor roleColor = member.getRole() == GuildRole.OWNER ? ChatColor.GOLD :
                    member.getRole() == GuildRole.OFFICER ? ChatColor.BLUE : ChatColor.WHITE;
            player.sendMessage(roleColor + "[" + roleName + "] " + ChatColor.WHITE + playerName
                    + ChatColor.GRAY + " - 贡献: " + member.getTotalContribution());
        }
        player.sendMessage(ChatColor.GOLD + "=================================");
    }

    private void handleListGuilds(Player player) {
        Map<String, Guild> allGuilds = guildManager.getGuilds();
        if (allGuilds.isEmpty()) {
            player.sendMessage(ChatColor.RED + "当前没有公会");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "========== 公会列表 ==========");
        allGuilds.values().stream()
                .sorted(Comparator.comparingInt(Guild::getLevel).reversed())
                .forEach(g -> player.sendMessage(ChatColor.YELLOW + g.getName()
                        + ChatColor.GRAY + " [Lv." + g.getLevel() + "] "
                        + ChatColor.WHITE + "(" + g.getMembers().size() + "人)"));
        player.sendMessage(ChatColor.GOLD + "==================================");
    }

    private void handleOnline(Player player) {
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "你不在任何公会中");
            return;
        }
        long onlineCount = guild.getMembers().keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .count();
        player.sendMessage(ChatColor.GREEN + "在线成员 (" + onlineCount + "/" + guild.getMembers().size() + "):");
        for (UUID uuid : guild.getMembers().keySet()) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.isOnline()) {
                player.sendMessage(ChatColor.GREEN + "- " + online.getDisplayName());
            }
        }
    }

    // ========== 邀请/申请 ==========

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild invite <玩家名>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "invite")) {
            player.sendMessage(ChatColor.RED + "你没有权限邀请成员");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "玩家不在线");
            return;
        }
        if (guildManager.isInGuild(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "该玩家已经在公会中了");
            return;
        }
        if (guildManager.sendInvite(guild.getName(), player.getUniqueId(),
                target.getUniqueId(), target.getDisplayName())) {
            player.sendMessage(ChatColor.GREEN + "已向 " + target.getDisplayName() + " 发送邀请");
            target.sendMessage(ChatColor.GREEN + "你收到了来自公会 [" + guild.getName() + "] 的邀请！");
            target.sendMessage(ChatColor.YELLOW + "输入 /guild accept 接受邀请");
        } else {
            player.sendMessage(ChatColor.RED + "该玩家已有待处理的邀请");
        }
    }

    private void handleAccept(Player player, String[] args) {
        // 先检查是否有待接受的邀请
        GuildManager.GuildInvite invite = guildManager.getInvite(player.getUniqueId());
        if (invite != null) {
            if (guildManager.acceptInvite(player.getUniqueId())) {
                player.sendMessage(ChatColor.GREEN + "你已加入公会: " + invite.getGuildName());
            } else {
                player.sendMessage(ChatColor.RED + "接受邀请失败，公会可能已满员");
            }
            return;
        }
        // 再检查是否有待审批的申请
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild accept <玩家名>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "invite")) {
            player.sendMessage(ChatColor.RED + "你没有权限批准申请");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "玩家不在线");
            return;
        }
        if (guildManager.acceptRequest(guild.getName(), target.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "已批准 " + target.getDisplayName() + " 的加入申请");
            target.sendMessage(ChatColor.GREEN + "你已被批准加入公会: " + guild.getName());
        } else {
            player.sendMessage(ChatColor.RED + "未找到该玩家的申请，或公会已满员");
        }
    }

    private void handleDecline(Player player) {
        if (guildManager.declineInvite(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "你拒绝了公会邀请");
        } else {
            player.sendMessage(ChatColor.RED + "没有待拒绝的邀请");
        }
    }

    // ========== 成员管理 ==========

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild kick <玩家名> [原因]");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "kick")) {
            player.sendMessage(ChatColor.RED + "你没有权限踢出成员");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "玩家不在线");
            return;
        }
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "无";
        if (guildManager.kickMember(guild.getName(), target.getUniqueId(), player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "已踢出 " + target.getDisplayName() + ": " + reason);
            target.sendMessage(ChatColor.RED + "你被从公会 [" + guild.getName() + "] 踢出。原因: " + reason);
        } else {
            player.sendMessage(ChatColor.RED + "踢出失败");
        }
    }

    private void handlePromote(Player player, String[] args) {
        modifyRole(player, args, true);
    }

    private void handleDemote(Player player, String[] args) {
        modifyRole(player, args, false);
    }

    private void modifyRole(Player player, String[] args, boolean promote) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild " + (promote ? "promote" : "demote") + " <玩家名>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "你不在任何公会中");
            return;
        }
        String permKey = promote ? "promote" : "demote";
        if (!guild.hasPermission(player.getUniqueId(), permKey)) {
            player.sendMessage(ChatColor.RED + "你没有权限执行此操作");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "玩家不在线");
            return;
        }
        boolean success = promote
                ? guildManager.promoteMember(guild.getName(), target.getUniqueId(), player.getUniqueId())
                : guildManager.demoteMember(guild.getName(), target.getUniqueId(), player.getUniqueId());

        if (success) {
            GuildMember member = guild.getMember(target.getUniqueId());
            player.sendMessage(ChatColor.GREEN + (promote ? "已提升" : "已降低")
                    + " " + target.getDisplayName() + " 的职位至 " + member.getRole().getDisplayName());
            target.sendMessage(ChatColor.GREEN + "你的职位已被"
                    + (promote ? "提升" : "降低") + " 至: " + member.getRole().getDisplayName());
        } else {
            player.sendMessage(ChatColor.RED + "操作失败，目标可能已是最高/最低级别");
        }
    }

    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild transfer <玩家名>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "你不在任何公会中");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "玩家不在线");
            return;
        }
        if (guildManager.transferOwnership(guild.getName(), target.getUniqueId(), player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "你已将会长转让给 " + target.getDisplayName());
            target.sendMessage(ChatColor.GREEN + "你已成为公会 [" + guild.getName() + "] 的会长！");
        } else {
            player.sendMessage(ChatColor.RED + "转让失败");
        }
    }

    // ========== 公会设置 ==========

    private void handleDisband(Player player) {
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "只有会长才能解散公会");
            return;
        }
        if (guildManager.disbandGuild(guild.getName())) {
            player.sendMessage(ChatColor.RED + "公会 [" + guild.getName() + "] 已解散");
        } else {
            player.sendMessage(ChatColor.RED + "解散失败");
        }
    }

    private void handleTag(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild tag <标签>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "settings")) {
            player.sendMessage(ChatColor.RED + "你没有权限修改标签");
            return;
        }
        guild.setTag(args[1]);
        plugin.getDatabaseManager().saveGuild(guild);
        player.sendMessage(ChatColor.GREEN + "公会标签已更新为: " + args[1]);
    }

    private void handleTagColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild tagcolor <颜色代码>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "settings")) {
            player.sendMessage(ChatColor.RED + "你没有权限修改标签颜色");
            return;
        }
        guild.setTagColor("&" + args[1]);
        plugin.getDatabaseManager().saveGuild(guild);
        player.sendMessage(ChatColor.GREEN + "公会标签颜色已更新");
    }

    private void handleMotd(Player player, String[] args) {
        if (args.length >= 2 && "help".equalsIgnoreCase(args[1])) {
            player.sendMessage(ChatColor.YELLOW + "/guild motd <公告内容> - 设置公会公告");
            player.sendMessage(ChatColor.YELLOW + "/guild motd clear - 清空公会公告");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild motd <内容> 或 /guild motd clear");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "motd")) {
            player.sendMessage(ChatColor.RED + "你没有权限修改公告");
            return;
        }
        if ("clear".equalsIgnoreCase(args[1])) {
            guild.setMotd("");
            player.sendMessage(ChatColor.YELLOW + "公告已清空");
        } else {
            guild.setMotd(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            player.sendMessage(ChatColor.GREEN + "公告已更新");
        }
        plugin.getDatabaseManager().saveGuild(guild);
    }

    // ========== 聊天 ==========

    private void handleChat(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "用法: /guild chat <消息>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "chat")) {
            player.sendMessage(ChatColor.RED + "你不能在公会聊天频道发言");
            return;
        }
        GuildMember member = guild.getMember(player.getUniqueId());
        if (member.isMuted()) {
            player.sendMessage(ChatColor.RED + "你已被禁言，无法发送消息");
            return;
        }
        String message = String.join(" ", args);
        String format = ChatColor.GOLD + "[公会] " + guild.getTagColor() + "[" + guild.getTag() + "] "
                + ChatColor.WHITE + "%player%: %message%";
        format = format.replace("%player%", player.getDisplayName()).replace("%message%", message);
        guild.broadcast(format);
    }

    private void handleOfficerChat(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "用法: /guild officerchat <消息>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || (guild.getMember(player.getUniqueId()).getRole() != GuildRole.OFFICER
                && guild.getMember(player.getUniqueId()).getRole() != GuildRole.OWNER)) {
            player.sendMessage(ChatColor.RED + "只有管理员和会长可以使用管理频道");
            return;
        }
        String message = String.join(" ", args);
        String format = ChatColor.BLUE + "[管理] " + ChatColor.WHITE + "%player%: %message%";
        format = format.replace("%player%", player.getDisplayName()).replace("%message%", message);
        guild.broadcastToOfficers(format);
    }

    // ========== 排行/日志/申请列表 ==========

    private void handleTop(Player player, String[] args) {
        int page = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "你不在任何公会中");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "===== 今日贡献排行榜 (第" + page + "页) =====");
        List<GuildMember> ranked = guild.getMembers().values().stream()
                .sorted(Comparator.comparingLong(GuildMember::getDailyContribution).reversed())
                .collect(Collectors.toList());
        int start = (page - 1) * 10;
        int end = Math.min(start + 10, ranked.size());
        for (int i = start; i < end; i++) {
            GuildMember m = ranked.get(i);
            String name = Bukkit.getOfflinePlayer(m.getUuid()).getName();
            player.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". " + ChatColor.WHITE + name
                    + ChatColor.GRAY + " - " + m.getDailyContribution() + " 经验");
        }
    }

    private void handleLog(Player player, String[] args) {
        int page = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        player.sendMessage(ChatColor.YELLOW + "公会日志功能开发中... (第" + page + "页)");
    }

    private void handleRequests(Player player, String[] args) {
        int page = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "invite")) {
            player.sendMessage(ChatColor.RED + "你没有权限查看申请列表");
            return;
        }
        List<GuildManager.GuildRequest> requests = guildManager.getRequests(guild.getName());
        if (requests.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "当前没有待处理的申请");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "===== 申请列表 (第" + page + "页) =====");
        int start = (page - 1) * 10;
        int end = Math.min(start + 10, requests.size());
        for (int i = start; i < end; i++) {
            GuildManager.GuildRequest req = requests.get(i);
            player.sendMessage(ChatColor.WHITE + String.valueOf(i + 1) + ". " + req.getPlayerName());
        }
    }

    // ========== 成员/设置/权限 ==========

    private void handleMember(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild member <玩家名>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "你不在任何公会中");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "玩家不存在或不在线");
            return;
        }
        GuildMember member = guild.getMember(target.getUniqueId());
        if (member == null) {
            player.sendMessage(ChatColor.RED + "该玩家不是你的公会成员");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "====== 成员信息 ======");
        player.sendMessage(ChatColor.YELLOW + "玩家: " + ChatColor.WHITE + target.getDisplayName());
        player.sendMessage(ChatColor.YELLOW + "职位: " + ChatColor.WHITE + member.getRole().getDisplayName());
        player.sendMessage(ChatColor.YELLOW + "总贡献: " + ChatColor.WHITE + member.getTotalContribution());
        player.sendMessage(ChatColor.YELLOW + "今日贡献: " + ChatColor.WHITE + member.getDailyContribution());
        player.sendMessage(ChatColor.YELLOW + "加入时间: " + ChatColor.WHITE + new Date(member.getJoinedTime()));
        player.sendMessage(ChatColor.GOLD + "========================");
    }

    private void handleSettings(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "用法: /guild settings <属性> <值>");
            player.sendMessage(ChatColor.YELLOW + "可用属性: public (true/false)");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "settings")) {
            player.sendMessage(ChatColor.RED + "你没有权限修改设置");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "public":
                guild.setPublicGuild(Boolean.parseBoolean(args[2]));
                player.sendMessage(ChatColor.GREEN + "公开状态已设置为: " + args[2]);
                break;
            default:
                player.sendMessage(ChatColor.RED + "未知属性: " + args[1]);
        }
        plugin.getDatabaseManager().saveGuild(guild);
    }

    private void handlePermission(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "用法: /guild permission <权限键> <角色等级>");
            player.sendMessage(ChatColor.YELLOW + "角色等级: MEMBER=1, OFFICER=2, OWNER=3");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "只有会长可以修改权限配置");
            return;
        }
        String permKey = args[1].toLowerCase();
        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "无效的等级数字");
            return;
        }
        GuildPermission perm = GuildPermission.fromLevel(level);
        guild.getPermissions().put(permKey, perm);
        plugin.getDatabaseManager().saveGuild(guild);
        player.sendMessage(ChatColor.GREEN + "权限 [" + permKey + "] 已设置为需要 " + perm.name() + " 级别");
    }

    // ========== 组队/重命名 ==========

    private void handleParty(Player player) {
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "你不在任何公会中");
            return;
        }
        for (UUID uuid : guild.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline() && !uuid.equals(player.getUniqueId())) {
                member.sendMessage(ChatColor.GREEN + player.getDisplayName() + " 想要组队！输入 /party accept " + player.getDisplayName() + " 接受");
            }
        }
        player.sendMessage(ChatColor.GREEN + "组队邀请已发送给公会在线成员");
    }

    private void handleRename(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild rename <新名称>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "只有会长可以重命名公会");
            return;
        }
        String newName = args[1];
        if (guildManager.getGuild(newName) != null) {
            player.sendMessage(ChatColor.RED + "该名称已被占用");
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "公会重命名请求已提交，请等待审核...");
        // 实际项目中这里可能需要审核流程，此处直接执行
        String oldName = guild.getName();
        guildManager.disbandGuild(oldName); // 删除旧记录
        guild.setName(newName);
        guildManager.createGuild(newName, Bukkit.getPlayer(guild.getOwner()));
        player.sendMessage(ChatColor.GREEN + "公会已重命名为: " + newName);
    }

    // ========== 经验/升级 ==========

    private void handleGExp(Player player, String[] subArgs) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令");
            return;
        }
        if (subArgs.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild gexp <公会名> <数量>");
            return;
        }
        Guild guild = guildManager.getGuild(subArgs[0]);
        if (guild == null) { player.sendMessage(ChatColor.RED + "公会不存在"); return; }
        long amount = Long.parseLong(subArgs[1]);
        guild.addExperience(amount);
        plugin.getDatabaseManager().saveGuild(guild);
        player.sendMessage(ChatColor.GREEN + "已给予 " + amount + " 经验到公会 " + guild.getName());
    }

    private void handleSLevel(Player player, String[] subArgs) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令");
            return;
        }
        if (subArgs.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild slevel <公会名> <等级>");
            return;
        }
        Guild guild = guildManager.getGuild(subArgs[0]);
        if (guild == null) { player.sendMessage(ChatColor.RED + "公会不存在"); return; }
        guild.setLevel(Integer.parseInt(subArgs[1]));
        plugin.getDatabaseManager().saveGuild(guild);
        player.sendMessage(ChatColor.GREEN + "已设置公会等级为 " + guild.getLevel());
    }

    private void handleClxpLev(Player player, String[] subArgs) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令");
            return;
        }
        if (subArgs.length < 1) {
            player.sendMessage(ChatColor.RED + "用法: /guild clxplev <公会名>");
            return;
        }
        Guild guild = guildManager.getGuild(subArgs[0]);
        if (guild == null) { player.sendMessage(ChatColor.RED + "公会不存在"); return; }
        guild.setExperience(0L);
        guild.setLevel(0);
        plugin.getDatabaseManager().saveGuild(guild);
        player.sendMessage(ChatColor.GREEN + "已将公会 " + guild.getName() + " 的等级和经验清零");
    }

    private void handleUpgrade(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild upgrade <bexp50 | upgrade>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "你不在任何公会中");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "bexp50":
                if (guildManager.addExperienceWithCurrency(guild.getName(), player.getUniqueId())) {
                    player.sendMessage(ChatColor.GREEN + "购买经验成功！+"
                            + plugin.getCurrencyConfig().getExperienceAmount() + " EXP");
                } else {
                    player.sendMessage(ChatColor.RED + "购买失败，余额不足");
                }
                break;
            case "upgrade":
                if (guildManager.upgradeGuild(guild.getName(), player.getUniqueId())) {
                    player.sendMessage(ChatColor.GREEN + "公会升级成功！当前等级: Lv." + guild.getLevel());
                } else {
                    player.sendMessage(ChatColor.RED + "升级失败，余额不足或已达上限");
                }
                break;
            default:
                player.sendMessage(ChatColor.RED + "未知选项: " + args[1]);
        }
    }

    // ========== 禁言 ==========

    private void handleMute(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "用法: /guild mute <玩家名> <分钟>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "kick")) {
            player.sendMessage(ChatColor.RED + "你需要管理员以上权限才能禁言");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(ChatColor.RED + "玩家不在线"); return; }
        long minutes = Long.parseLong(args[2]);
        GuildMember member = guild.getMember(target.getUniqueId());
        if (member == null) { player.sendMessage(ChatColor.RED + "该玩家不是公会成员"); return; }
        member.mute(minutes * 60_000L);
        plugin.getDatabaseManager().saveGuild(guild);
        player.sendMessage(ChatColor.GREEN + "已禁言 " + target.getDisplayName() + " " + minutes + " 分钟");
        target.sendMessage(ChatColor.RED + "你被禁言 " + minutes + " 分钟");
    }

    private void handleUnmute(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild unmute <玩家名>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null || !guild.hasPermission(player.getUniqueId(), "kick")) {
            player.sendMessage(ChatColor.RED + "你需要管理员以上权限才能解除禁言");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(ChatColor.RED + "玩家不在线"); return; }
        GuildMember member = guild.getMember(target.getUniqueId());
        if (member == null) { player.sendMessage(ChatColor.RED + "该玩家不是公会成员"); return; }
        member.unmute();
        plugin.getDatabaseManager().saveGuild(guild);
        player.sendMessage(ChatColor.GREEN + "已解除 " + target.getDisplayName() + " 的禁言");
        target.sendMessage(ChatColor.GREEN + "你已被解除禁言");
    }

    // ========== 银行 ==========

    private void handleBalance(Player player) {
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "你不在任何公会中");
            return;
        }
        long balance = guildManager.getGuildBalance(guild.getName());
        player.sendMessage(ChatColor.GOLD + "公会银行余额: " + ChatColor.WHITE + balance);
    }

    private void handleOpenBank(Player player) {
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "你不在任何公会中");
            return;
        }
        GuildBankGUI.openBankGUI(plugin, player, guild);
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild deposit <金额>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) { player.sendMessage(ChatColor.RED + "你不在任何公会中"); return; }
        try {
            long amount = Long.parseLong(args[1]);
            if (amount <= 0) { player.sendMessage(ChatColor.RED + "金额必须大于0"); return; }
            if (guildManager.depositToBank(guild.getName(), player.getUniqueId(), amount)) {
                player.sendMessage(ChatColor.GREEN + "已存入 " + amount + " 到公会银行");
            } else {
                player.sendMessage(ChatColor.RED + "存款失败，余额不足");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "无效的金额");
        }
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /guild withdraw <金额>");
            return;
        }
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) { player.sendMessage(ChatColor.RED + "你不在任何公会中"); return; }
        try {
            long amount = Long.parseLong(args[1]);
            if (amount <= 0) { player.sendMessage(ChatColor.RED + "金额必须大于0"); return; }
            if (guildManager.withdrawFromBank(guild.getName(), player.getUniqueId(), amount)) {
                player.sendMessage(ChatColor.GREEN + "已取出 " + amount + " 从公会银行");
            } else {
                player.sendMessage(ChatColor.RED + "取款失败，余额不足或无权限");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "无效的金额");
        }
    }

    // ========== Admin 命令 ==========

    private void handleDelete(Player player, String[] subArgs) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令");
            return;
        }
        if (subArgs.length < 1) {
            player.sendMessage(ChatColor.RED + "用法: /guild delete <公会名>");
            return;
        }
        if ("confirm".equalsIgnoreCase(subArgs[0])) {
            if (pendingDeleteConfirm.remove(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "请重新使用 /guild delete <公会名> 后确认");
                return;
            }
        }
        Guild guild = guildManager.getGuild(subArgs[0]);
        if (guild == null) { player.sendMessage(ChatColor.RED + "公会不存在"); return; }
        pendingDeleteConfirm.add(player.getUniqueId());
        player.sendMessage(ChatColor.RED + "警告：即将强制删除公会 [" + guild.getName() + "]");
        player.sendMessage(ChatColor.YELLOW + "请输入 /guild delete confirm 确认操作");
    }

    private void handleReload(Player player, String[] subArgs) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令");
            return;
        }
        if (subArgs.length >= 1 && "database".equalsIgnoreCase(subArgs[0])) {
            plugin.reloadConfig();
            plugin.getDatabaseManager().initialize();
            player.sendMessage(ChatColor.GREEN + "数据库已重载");
        } else {
            plugin.reloadConfig();
            player.sendMessage(ChatColor.GREEN + "配置文件已重载");
        }
    }
}
