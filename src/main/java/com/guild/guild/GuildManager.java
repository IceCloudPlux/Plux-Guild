package com.guild.guild;

import com.guild.GuildPlugin;
import com.guild.currency.GuildCurrency;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

public class GuildManager {

    private final GuildPlugin plugin;
    private final Map<String, Guild> guilds = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerGuilds = new ConcurrentHashMap<>();
    private final Map<String, List<GuildRequest>> requests = new ConcurrentHashMap<>();
    private final Map<UUID, GuildInvite> invites = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSettings> playerSettings = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerGuildCurrency = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerNameCache = new ConcurrentHashMap<>();
    private final LongAdder totalCacheHits = new LongAdder();
    private final LongAdder totalCacheMisses = new LongAdder();
    private volatile long lastCacheCleanup = System.currentTimeMillis();
    private static final long CACHE_CLEANUP_INTERVAL = 300000L;
    private static final long INVITE_EXPIRE_TIME = 600000L;

    public GuildManager(GuildPlugin plugin) {
        this.plugin = plugin;
        startSaveQueue();
        startInviteCleanup();
    }

    private String getPlayerNameCached(UUID uuid) {
        String cached = playerNameCache.get(uuid);
        if (cached != null) {
            totalCacheHits.increment();
            return cached;
        }
        
        totalCacheMisses.increment();
        String name = plugin.getServer().getOfflinePlayer(uuid).getName();
        if (name != null) {
            playerNameCache.put(uuid, name);
        }
        
        cleanupCacheIfNeeded();
        return name != null ? name : "未知";
    }

    private void cleanupCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheCleanup > CACHE_CLEANUP_INTERVAL) {
            lastCacheCleanup = now;
            playerNameCache.clear();
        }
    }

    private final Queue<Guild> pendingSaves = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean saveRunning = new AtomicBoolean(false);

    private void startSaveQueue() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (saveRunning.compareAndSet(false, true)) {
                try {
                    Guild guild;
                    while ((guild = pendingSaves.poll()) != null) {
                        plugin.getDatabaseManager().saveGuild(guild);
                    }
                } finally {
                    saveRunning.set(false);
                }
            }
        }, 100L, 200L);
    }

    private void scheduleSave(Guild guild) {
        pendingSaves.offer(guild);
    }

    private void startInviteCleanup() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            invites.entrySet().removeIf(entry -> 
                    now - entry.getValue().getInviteTime() > INVITE_EXPIRE_TIME);
        }, 60000L, 60000L);
    }

    // ========== 公会 CRUD ==========

    public Guild createGuild(String name, Player player) {
        String key = name.toLowerCase();
        if (guilds.containsKey(key)) {
            return null;
        }
        Guild guild = new Guild(name, player.getUniqueId());
        guilds.put(key, guild);
        playerGuilds.put(player.getUniqueId(), key);
        scheduleSave(guild);
        return guild;
    }

    public boolean disbandGuild(String name) {
        String key = name.toLowerCase();
        Guild guild = guilds.remove(key);
        if (guild == null) return false;
        for (UUID memberUuid : guild.getMembers().keySet()) {
            playerGuilds.remove(memberUuid);
        }
        plugin.getDatabaseManager().deleteGuild(name);
        return true;
    }

    public Guild getGuild(String name) {
        return guilds.get(name.toLowerCase());
    }

    public Guild getPlayerGuild(UUID playerUuid) {
        String guildName = playerGuilds.get(playerUuid);
        return guildName != null ? guilds.get(guildName) : null;
    }

    public boolean isInGuild(UUID playerUuid) {
        return playerGuilds.containsKey(playerUuid);
    }

    // ========== 成员管理 ==========

    public boolean joinGuild(String name, Player player) {
        Guild guild = guilds.get(name.toLowerCase());
        if (guild == null || !guild.canAddMember()) return false;
        guild.addMember(player.getUniqueId(), GuildRole.MEMBER);
        playerGuilds.put(player.getUniqueId(), name.toLowerCase());
        scheduleSave(guild);
        return true;
    }

    public boolean leaveGuild(UUID playerUuid) {
        String guildName = playerGuilds.get(playerUuid);
        if (guildName == null) return false;
        Guild guild = guilds.get(guildName);
        if (guild == null || guild.getOwner().equals(playerUuid)) return false;
        guild.removeMember(playerUuid);
        playerGuilds.remove(playerUuid);
        scheduleSave(guild);
        return true;
    }

    public boolean kickMember(String guildName, UUID targetUuid, UUID operatorUuid) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.hasPermission(operatorUuid, "kick")) return false;
        guild.removeMember(targetUuid);
        playerGuilds.remove(targetUuid);
        scheduleSave(guild);
        return true;
    }

    public boolean promoteMember(String guildName, UUID targetUuid, UUID operatorUuid) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.hasPermission(operatorUuid, "promote")) return false;
        GuildMember member = guild.getMember(targetUuid);
        if (member == null) return false;
        member.setRole(member.getRole().promote());
        scheduleSave(guild);
        return true;
    }

    public boolean demoteMember(String guildName, UUID targetUuid, UUID operatorUuid) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.hasPermission(operatorUuid, "demote")) return false;
        GuildMember member = guild.getMember(targetUuid);
        if (member == null) return false;
        member.setRole(member.getRole().demote());
        scheduleSave(guild);
        return true;
    }

    public boolean transferOwnership(String guildName, UUID targetUuid, UUID ownerUuid) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.getOwner().equals(ownerUuid)) return false;
        GuildMember targetMember = guild.getMember(targetUuid);
        if (targetMember == null) return false;
        GuildMember ownerMember = guild.getMember(ownerUuid);
        ownerMember.setRole(GuildRole.OFFICER);
        targetMember.setRole(GuildRole.OWNER);
        guild.setOwner(targetUuid);
        scheduleSave(guild);
        return true;
    }

    // ========== 申请/邀请系统 ==========

    public void addRequest(String guildName, UUID playerUuid, String playerName) {
        requests.computeIfAbsent(guildName.toLowerCase(), k -> new ArrayList<>())
                .add(new GuildRequest(playerUuid, playerName));
    }

    public List<GuildRequest> getRequests(String guildName) {
        return requests.getOrDefault(guildName.toLowerCase(), Collections.emptyList());
    }

    public boolean acceptRequest(String guildName, UUID playerUuid) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.canAddMember()) return false;
        List<GuildRequest> requestList = requests.get(guildName.toLowerCase());
        if (requestList == null) return false;
        
        GuildRequest foundRequest = null;
        for (GuildRequest r : requestList) {
            if (r.getPlayerUuid().equals(playerUuid)) {
                foundRequest = r;
                break;
            }
        }
        
        if (foundRequest == null) return false;
        requestList.remove(foundRequest);
        guild.addMember(playerUuid, GuildRole.MEMBER);
        playerGuilds.put(playerUuid, guildName.toLowerCase());
        scheduleSave(guild);
        return true;
    }

    public boolean sendInvite(String guildName, UUID inviterUuid, UUID targetUuid, String targetName) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.hasPermission(inviterUuid, "invite")) return false;
        if (invites.containsKey(targetUuid)) return false; // 已有邀请
        invites.put(targetUuid, new GuildInvite(guildName.toLowerCase(), inviterUuid, targetUuid, targetName));
        return true;
    }

    public boolean acceptInvite(UUID playerUuid) {
        GuildInvite invite = invites.remove(playerUuid);
        if (invite == null) return false;
        
        long now = System.currentTimeMillis();
        if (now - invite.getInviteTime() > INVITE_EXPIRE_TIME) {
            return false;
        }
        
        Guild guild = guilds.get(invite.getGuildName());
        if (guild == null || !guild.canAddMember()) return false;
        guild.addMember(playerUuid, GuildRole.MEMBER);
        playerGuilds.put(playerUuid, invite.getGuildName());
        scheduleSave(guild);
        return true;
    }

    public boolean declineInvite(UUID playerUuid) {
        return invites.remove(playerUuid) != null;
    }

    public GuildInvite getInvite(UUID playerUuid) {
        return invites.get(playerUuid);
    }

    public Map<UUID, GuildInvite> getInvites() {
        return invites;
    }

    // ========== 经验系统 ==========

    public void addExperience(UUID playerUuid, long amount) {
        String guildName = playerGuilds.get(playerUuid);
        if (guildName == null) return;
        Guild guild = guilds.get(guildName);
        if (guild == null) return;
        guild.addExperience(amount);
        GuildMember member = guild.getMember(playerUuid);
        if (member != null) {
            member.addContribution(amount);
        }
        scheduleSave(guild);
    }

    // ========== 银行系统 ==========

    public long getGuildBalance(String guildName) {
        Guild guild = guilds.get(guildName.toLowerCase());
        return guild != null ? guild.getBank().getBalance() : 0L;
    }

    public boolean depositToBank(String guildName, UUID playerUuid, long amount) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.isMember(playerUuid)) return false;
        if (guild.getBank().deposit(amount)) {
            guild.getBank().addDepositRecord(getPlayerNameCached(playerUuid), amount);
            scheduleSave(guild);
            return true;
        }
        return false;
    }

    public boolean withdrawFromBank(String guildName, UUID playerUuid, long amount) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.isMember(playerUuid) || !guild.hasPermission(playerUuid, "withdraw"))
            return false;
        if (guild.getBank().withdraw(amount)) {
            guild.getBank().addWithdrawRecord(getPlayerNameCached(playerUuid), amount);
            scheduleSave(guild);
            return true;
        }
        return false;
    }

    // ========== 公会货币 ==========

    public long getPlayerGuildCurrency(UUID playerUuid) {
        return playerGuildCurrency.getOrDefault(playerUuid, 0L);
    }

    public boolean depositPlayerGuildCurrency(UUID playerUuid, long amount) {
        if (amount <= 0L) return false;
        playerGuildCurrency.merge(playerUuid, amount, Long::sum);
        return true;
    }

    public boolean withdrawPlayerGuildCurrency(UUID playerUuid, long amount) {
        long current = playerGuildCurrency.getOrDefault(playerUuid, 0L);
        if (current < amount || amount <= 0L) return false;
        playerGuildCurrency.put(playerUuid, current - amount);
        return true;
    }

    public boolean setPlayerGuildCurrency(UUID playerUuid, long amount) {
        if (amount < 0L) return false;
        playerGuildCurrency.put(playerUuid, amount);
        return true;
    }

    // ========== 升级 / 购买经验 ==========

    public boolean upgradeGuild(String guildName, UUID playerUuid) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.getOwner().equals(playerUuid) || guild.getLevel() >= 100)
            return false;
        GuildCurrency.CurrencyType currencyType = plugin.getCurrencyConfig().getCurrencyType();
        long cost = plugin.getCurrencyConfig().getLevelUpCost();
        if (!plugin.getGuildCurrency().withdraw(playerUuid, cost, currencyType)) return false;
        guild.setLevel(guild.getLevel() + 1);
        scheduleSave(guild);
        return true;
    }

    public boolean addExperienceWithCurrency(String guildName, UUID playerUuid) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.isMember(playerUuid)) return false;
        GuildCurrency.CurrencyType currencyType = plugin.getCurrencyConfig().getCurrencyType();
        long cost = plugin.getCurrencyConfig().getExperienceCost();
        int expAmount = plugin.getCurrencyConfig().getExperienceAmount();
        if (!plugin.getGuildCurrency().withdraw(playerUuid, cost, currencyType)) return false;
        guild.addExperience(expAmount);
        scheduleSave(guild);
        return true;
    }

    // ========== 玩家设置 ==========

    public PlayerSettings getPlayerSettings(UUID playerUuid) {
        return playerSettings.computeIfAbsent(playerUuid, PlayerSettings::new);
    }

    public boolean togglePlayerInvites(UUID playerUuid) {
        PlayerSettings settings = getPlayerSettings(playerUuid);
        settings.toggleInvites();
        return settings.isAllowInvites();
    }

    public boolean togglePlayerNotify(UUID playerUuid) {
        PlayerSettings settings = getPlayerSettings(playerUuid);
        settings.toggleNotify();
        return settings.isNotifyOnlineStatus();
    }

    // ========== 数据访问 ==========

    /**
     * 获取所有公会的只读视图
     * <p>
     * 优化：返回不可修改视图，避免每次调用都创建新的 HashMap 拷贝。
     * 原方案 new HashMap<>(guilds) 在高频调用场景下产生大量短命对象，增加 GC 压力。
     *
     * @return 公会映射的不可修改视图
     */
    public Map<String, Guild> getGuilds() {
        return Collections.unmodifiableMap(guilds);
    }

    /**
     * 获取玩家→公会名称映射的只读视图
     */
    public Map<UUID, String> getPlayerGuilds() {
        return Collections.unmodifiableMap(playerGuilds);
    }

    // ========== 内部类 ==========

    public static class GuildRequest {
        private final UUID playerUuid;
        private final String playerName;
        private final long requestTime;

        public GuildRequest(UUID playerUuid, String playerName) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.requestTime = System.currentTimeMillis();
        }

        public UUID getPlayerUuid() { return playerUuid; }
        public String getPlayerName() { return playerName; }
        public long getRequestTime() { return requestTime; }
    }

    public static class GuildInvite {
        private final String guildName;
        private final UUID inviterUuid;
        private final UUID targetUuid;
        private final String targetName;
        private final long inviteTime;

        public GuildInvite(String guildName, UUID inviterUuid, UUID targetUuid, String targetName) {
            this.guildName = guildName;
            this.inviterUuid = inviterUuid;
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.inviteTime = System.currentTimeMillis();
        }

        public String getGuildName() { return guildName; }
        public UUID getInviterUuid() { return inviterUuid; }
        public UUID getTargetUuid() { return targetUuid; }
        public String getTargetName() { return targetName; }
        public long getInviteTime() { return inviteTime; }
    }
}
