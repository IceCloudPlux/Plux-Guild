package com.guild.guild;
import com.guild.GuildPlugin;
import com.guild.currency.GuildCurrency;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

public class GuildManager
{
    private final GuildPlugin plugin;
    private final Map<String, Guild> guilds = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerGuilds = new ConcurrentHashMap<>();
    private final Map<String, List<GuildRequest>> requests = new ConcurrentHashMap<>();
    private final Map<UUID, GuildInvite> invites = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSettings> playerSettings = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerGuildCurrency = new ConcurrentHashMap<>();
    /** 每日银行限额追踪: uuid -> [当天epochDay, 已存, 已取] */
    private final Map<UUID, long[]> dailyBankUsage = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerNameCache = new ConcurrentHashMap<>();
    private final LongAdder totalCacheHits = new LongAdder();
    private final LongAdder totalCacheMisses = new LongAdder();
    private volatile long lastCacheCleanup = System.currentTimeMillis();
    private static final long CACHE_CLEANUP_INTERVAL = 300000L;
    private static final long INVITE_EXPIRE_TIME = 600000L;
    private static final long REQUEST_EXPIRE_TIME = 600000L;
    private final Queue<Guild> pendingSaves = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean saveRunning = new AtomicBoolean(false);

    public GuildManager(GuildPlugin plugin)
    {
        this.plugin = plugin;
        startSaveQueue();
        startInviteCleanup();
    }

    private String getPlayerNameCached(UUID uuid)
    {
        String cached = playerNameCache.get(uuid);
        if (cached != null)
        {
            totalCacheHits.increment();
            return cached;
        }
        totalCacheMisses.increment();
        String name = plugin.getServer().getOfflinePlayer(uuid).getName();
        if (name != null)
        {
            playerNameCache.put(uuid, name);
        }
        cleanupCacheIfNeeded();
        return name != null ? name : "未知";
    }

    private void cleanupCacheIfNeeded()
    {
        long now = System.currentTimeMillis();
        if (now - lastCacheCleanup > CACHE_CLEANUP_INTERVAL)
        {
            lastCacheCleanup = now;
            playerNameCache.clear();
        }
    }

    private void startSaveQueue()
    {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () ->
        {
            if (saveRunning.compareAndSet(false, true))
            {
                try
                {
                    Guild guild;
                    while ((guild = pendingSaves.poll()) != null)
                    {
                        plugin.getDatabaseManager().saveGuild(guild);
                    }
                }
                finally
                {
                    saveRunning.set(false);
                }
            }
        }, 100L, 200L);
    }

    public void scheduleSavePublic(Guild guild) { scheduleSave(guild); }

    private void scheduleSave(Guild guild)
    {
        pendingSaves.offer(guild);
    }

    private void startInviteCleanup()
    {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () ->
        {
            long now = System.currentTimeMillis();
            invites.entrySet().removeIf(entry -> now - entry.getValue().getInviteTime() > INVITE_EXPIRE_TIME);
            // 同步清理过期的入会申请，防止幽灵申请长期堆积
            for (List<GuildRequest> requestList : requests.values())
            {
                requestList.removeIf(r -> now - r.getRequestTime() > REQUEST_EXPIRE_TIME);
            }
            requests.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }, 60000L, 60000L);
    }

    public Guild createGuild(String name, Player player)
    {
        if (!player.hasPermission("guild.create"))
        {
            player.sendMessage(ChatColor.RED + "你没有创建公会的权限");
            return null;
        }
        String key = name.toLowerCase();
        if (guilds.containsKey(key)) return null;
        if (playerGuilds.containsKey(player.getUniqueId())) return null;
        boolean requiresMoney = plugin.getCurrencyConfig().isCreateRequiresMoney();
        long createCost = plugin.getCurrencyConfig().getCreateCost();
        if (requiresMoney && createCost > 0)
        {
            GuildCurrency.CurrencyType currencyType = plugin.getCurrencyConfig().getCurrencyType();
            if (!plugin.getGuildCurrency().withdraw(player.getUniqueId(), createCost, currencyType))
            {
                String currencyName = plugin.getGuildCurrency().formatAmount(createCost, currencyType);
                player.sendMessage(ChatColor.RED + "创建公会需要 " + currencyName);
                return null;
            }
        }
        Guild guild = new Guild(name, player.getUniqueId(), plugin);
        guilds.put(key, guild);
        playerGuilds.put(player.getUniqueId(), key);
        scheduleSave(guild);
        return guild;
    }

    /**
     * 重命名公会：保留全部成员、等级、经验、银行与权限数据，
     * 仅迁移内存索引与数据库记录（替代旧版"解散后重建"的危险实现）
     */
    public boolean renameGuild(String oldName, String newName, UUID operatorUuid)
    {
        String oldKey = oldName.toLowerCase();
        Guild guild = guilds.get(oldKey);
        if (guild == null) return false;
        if (!guild.getOwner().equals(operatorUuid)) return false;
        String newKey = newName.toLowerCase();
        if (guilds.containsKey(newKey)) return false;
        guilds.remove(oldKey);
        guild.setName(newName);
        guilds.put(newKey, guild);
        for (UUID memberUuid : guild.getMembers().keySet())
        {
            playerGuilds.put(memberUuid, newKey);
        }
        // 迁移数据库记录（旧名称的成员/权限/银行记录全部转移）
        plugin.getDatabaseManager().migrateGuildName(oldName, newName);
        scheduleSave(guild);
        return true;
    }

    public boolean disbandGuild(String name, UUID playerUuid)
    {
        String key = name.toLowerCase();
        Guild guild = guilds.get(key);
        if (guild == null) return false;
        if (!guild.getOwner().equals(playerUuid)) return false;
        guilds.remove(key);
        for (UUID memberUuid : guild.getMembers().keySet())
        {
            playerGuilds.remove(memberUuid);
        }
        requests.remove(key);
        plugin.getDatabaseManager().deleteGuild(name);
        return true;
    }

    public Guild getGuild(String name)
    {
        return guilds.get(name.toLowerCase());
    }

    public Guild getPlayerGuild(UUID playerUuid)
    {
        String guildName = playerGuilds.get(playerUuid);
        return guildName != null ? guilds.get(guildName) : null;
    }

    public boolean isInGuild(UUID playerUuid)
    {
        return playerGuilds.containsKey(playerUuid);
    }

    public void removePlayerFromGuild(UUID playerUuid)
    {
        playerGuilds.remove(playerUuid);
    }

    public boolean joinGuild(String name, Player player)
    {
        Guild guild = guilds.get(name.toLowerCase());
        if (guild == null || !guild.canAddMember()) return false;
        guild.addMember(player.getUniqueId(), GuildRole.MEMBER);
        playerGuilds.put(player.getUniqueId(), name.toLowerCase());
        scheduleSave(guild);
        return true;
    }

    public boolean leaveGuild(UUID playerUuid)
    {
        String guildName = playerGuilds.get(playerUuid);
        if (guildName == null) return false;
        Guild guild = guilds.get(guildName);
        if (guild == null || guild.getOwner().equals(playerUuid)) return false;
        guild.removeMember(playerUuid);
        playerGuilds.remove(playerUuid);
        scheduleSave(guild);
        return true;
    }

    public boolean kickMember(String guildName, UUID targetUuid, UUID operatorUuid)
    {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.hasPermission(operatorUuid, "kick")) return false;
        guild.removeMember(targetUuid);
        playerGuilds.remove(targetUuid);
        scheduleSave(guild);
        return true;
    }

    public boolean promoteMember(String guildName, UUID targetUuid, UUID operatorUuid)
    {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.hasPermission(operatorUuid, "promote")) return false;
        GuildMember member = guild.getMember(targetUuid);
        if (member == null) return false;
        member.setRole(member.getRole().promote());
        scheduleSave(guild);
        return true;
    }

    public boolean demoteMember(String guildName, UUID targetUuid, UUID operatorUuid)
    {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.hasPermission(operatorUuid, "demote")) return false;
        GuildMember member = guild.getMember(targetUuid);
        if (member == null) return false;
        member.setRole(member.getRole().demote());
        scheduleSave(guild);
        return true;
    }

    public boolean transferOwnership(String guildName, UUID targetUuid, UUID ownerUuid)
    {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.getOwner().equals(ownerUuid)) return false;
        GuildMember targetMember = guild.getMember(targetUuid);
        if (targetMember == null) return false;
        GuildMember ownerMember = guild.getMember(ownerUuid);
        if (ownerMember != null) ownerMember.setRole(GuildRole.OFFICER);
        targetMember.setRole(GuildRole.OWNER);
        guild.setOwner(targetUuid);
        scheduleSave(guild);
        return true;
    }

    public void addRequest(String guildName, UUID playerUuid, String playerName)
    {
        List<GuildRequest> requestList = requests.computeIfAbsent(guildName.toLowerCase(), k -> new CopyOnWriteArrayList<>());
        // 防止同一玩家重复提交申请
        for (GuildRequest existing : requestList)
        {
            if (existing.getPlayerUuid().equals(playerUuid)) return;
        }
        requestList.add(new GuildRequest(playerUuid, playerName));
    }

    public List<GuildRequest> getRequests(String guildName)
    {
        return requests.getOrDefault(guildName.toLowerCase(), Collections.emptyList());
    }

    public boolean acceptRequest(String guildName, UUID playerUuid)
    {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.canAddMember()) return false;
        List<GuildRequest> requestList = requests.get(guildName.toLowerCase());
        if (requestList == null) return false;
        GuildRequest foundRequest = null;
        Iterator<GuildRequest> iter = requestList.iterator();
        while (iter.hasNext())
        {
            GuildRequest r = iter.next();
            if (r.getPlayerUuid().equals(playerUuid))
            {
                foundRequest = r;
                iter.remove();
                break;
            }
        }
        if (foundRequest == null) return false;
        guild.addMember(playerUuid, GuildRole.MEMBER);
        playerGuilds.put(playerUuid, guildName.toLowerCase());
        scheduleSave(guild);
        return true;
    }

    public boolean sendInvite(String guildName, UUID inviterUuid, UUID targetUuid, String targetName)
    {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.hasPermission(inviterUuid, "invite")) return false;
        if (invites.containsKey(targetUuid)) return false;
        invites.put(targetUuid, new GuildInvite(guildName.toLowerCase(), inviterUuid, targetUuid, targetName));
        return true;
    }

    public boolean acceptInvite(UUID playerUuid)
    {
        GuildInvite invite = invites.remove(playerUuid);
        if (invite == null) return false;
        long now = System.currentTimeMillis();
        if (now - invite.getInviteTime() > INVITE_EXPIRE_TIME) return false;
        Guild guild = guilds.get(invite.getGuildName());
        if (guild == null || !guild.canAddMember()) return false;
        guild.addMember(playerUuid, GuildRole.MEMBER);
        playerGuilds.put(playerUuid, invite.getGuildName());
        scheduleSave(guild);
        return true;
    }

    public boolean declineInvite(UUID playerUuid)
    {
        return invites.remove(playerUuid) != null;
    }

    public GuildInvite getInvite(UUID playerUuid)
    {
        return invites.get(playerUuid);
    }

    public Map<UUID, GuildInvite> getInvites()
    {
        return invites;
    }

    public void addExperience(UUID playerUuid, long amount)
    {
        if (amount <= 0) return;
        String guildName = playerGuilds.get(playerUuid);
        if (guildName == null) return;
        Guild guild = guilds.get(guildName);
        if (guild == null) return;
        long finalAmount = amount;
        // 周末/节假日经验加成（experience.yml）
        double multiplier = plugin.getExperienceConfig().getWeekendMultiplier();
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if ((dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) && multiplier > 1.0)
        {
            finalAmount = (long) (finalAmount * multiplier);
        }
        double holidayMultiplier = plugin.getExperienceConfig().getHolidayMultiplier();
        if (holidayMultiplier != 1.0)
        {
            finalAmount = (long) (finalAmount * holidayMultiplier);
        }
        if (finalAmount <= 0) return;
        GuildMember member = guild.getMember(playerUuid);
        // 每日经验/贡献上限（0 = 无限制，experience.yml）
        long expLimit = plugin.getExperienceConfig().getDailyExpLimit();
        if (expLimit > 0 && guild.getDailyExperience() + finalAmount > expLimit)
        {
            finalAmount = Math.max(0L, expLimit - guild.getDailyExperience());
        }
        if (member != null)
        {
            long contributionLimit = plugin.getExperienceConfig().getDailyContributionLimit();
            long contribution = finalAmount;
            if (contributionLimit > 0 && member.getDailyContribution() + contribution > contributionLimit)
            {
                contribution = Math.max(0L, contributionLimit - member.getDailyContribution());
            }
            member.addContribution(contribution);
        }
        if (finalAmount <= 0) return;
        guild.addExperience(finalAmount);
        scheduleSave(guild);
    }

    public long getGuildBalance(String guildName)
    {
        Guild guild = guilds.get(guildName.toLowerCase());
        return guild != null ? guild.getBank().getBalance() : 0L;
    }

    public boolean depositToBank(String guildName, UUID playerUuid, long amount)
    {
        if (amount <= 0L) return false;
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.isMember(playerUuid)) return false;
        com.guild.config.BankConfig bankConfig = plugin.getBankConfig();
        // 单次存款上限
        long maxDeposit = bankConfig.getMaxDeposit();
        if (maxDeposit > 0 && amount > maxDeposit) return false;
        // 每日存款限额
        long dailyLimit = bankConfig.getDailyDepositLimit();
        if (dailyLimit > 0)
        {
            long[] usage = dailyBankUsage.computeIfAbsent(playerUuid, k -> new long[]
            { java.time.LocalDate.now().toEpochDay(), 0L, 0L });
            if (usage[0] != java.time.LocalDate.now().toEpochDay())
            {
                usage[0] = java.time.LocalDate.now().toEpochDay();
                usage[1] = 0L;
                usage[2] = 0L;
            }
            if (usage[1] + amount > dailyLimit) return false;
        }
        // 银行余额上限
        long maxBalance = bankConfig.getMaxBalance();
        if (maxBalance > 0 && guild.getBank().getBalance() + amount > maxBalance) return false;
        // 先从玩家钱包扣款（按 currency.yml 配置的货币类型）
        GuildCurrency.CurrencyType currencyType = plugin.getCurrencyConfig().getCurrencyType();
        if (!plugin.getGuildCurrency().withdraw(playerUuid, amount, currencyType)) return false;
        // 存款税率：扣除后实际入账
        double tax = bankConfig.getDepositTax();
        long net = amount;
        if (tax > 0)
        {
            net = (long) (amount * (1.0 - Math.min(tax, 1.0)));
        }
        if (net <= 0)
        {
            // 税后为0，退还扣款
            plugin.getGuildCurrency().deposit(playerUuid, amount, currencyType);
            return false;
        }
        if (guild.getBank().deposit(net))
        {
            guild.getBank().addDepositRecord(getPlayerNameCached(playerUuid), net);
            if (dailyLimit > 0)
            {
                dailyBankUsage.get(playerUuid)[1] += amount;
            }
            scheduleSave(guild);
            return true;
        }
        // 入账失败则退还扣款
        plugin.getGuildCurrency().deposit(playerUuid, amount, currencyType);
        return false;
    }

    public boolean withdrawFromBank(String guildName, UUID playerUuid, long amount)
    {
        if (amount <= 0L) return false;
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.isMember(playerUuid) || !guild.hasPermission(playerUuid, "withdraw")) return false;
        com.guild.config.BankConfig bankConfig = plugin.getBankConfig();
        // 单次取款上限
        long maxWithdraw = bankConfig.getMaxWithdraw();
        if (maxWithdraw > 0 && amount > maxWithdraw) return false;
        // 每日取款限额
        long dailyLimit = bankConfig.getDailyWithdrawLimit();
        if (dailyLimit > 0)
        {
            long[] usage = dailyBankUsage.computeIfAbsent(playerUuid, k -> new long[]
            { java.time.LocalDate.now().toEpochDay(), 0L, 0L });
            if (usage[0] != java.time.LocalDate.now().toEpochDay())
            {
                usage[0] = java.time.LocalDate.now().toEpochDay();
                usage[1] = 0L;
                usage[2] = 0L;
            }
            if (usage[2] + amount > dailyLimit) return false;
        }
        // 最低保留余额
        long minBalance = bankConfig.getMinBalance();
        if (guild.getBank().getBalance() - amount < minBalance) return false;
        // 取款税率：扣除后实际到账
        double tax = bankConfig.getWithdrawTax();
        long net = amount;
        if (tax > 0)
        {
            net = (long) (amount * (1.0 - Math.min(tax, 1.0)));
        }
        if (net <= 0) return false;
        if (guild.getBank().withdraw(amount))
        {
            guild.getBank().addWithdrawRecord(getPlayerNameCached(playerUuid), amount);
            // 将资金发放到玩家钱包（按 currency.yml 配置的货币类型）
            plugin.getGuildCurrency().deposit(playerUuid, net, plugin.getCurrencyConfig().getCurrencyType());
            if (dailyLimit > 0)
            {
                dailyBankUsage.get(playerUuid)[2] += amount;
            }
            scheduleSave(guild);
            return true;
        }
        return false;
    }

    public long getPlayerGuildCurrency(UUID playerUuid)
    {
        return playerGuildCurrency.getOrDefault(playerUuid, 0L);
    }

    public boolean depositPlayerGuildCurrency(UUID playerUuid, long amount)
    {
        if (amount <= 0L) return false;
        long maxBalance = plugin.getCurrencyConfig().getMaxBalance();
        long result = playerGuildCurrency.merge(playerUuid, amount, Long::sum);
        // 公会币上限（0 = 无限制，currency.yml）
        if (maxBalance > 0)
        {
            if (result > maxBalance)
            {
                playerGuildCurrency.put(playerUuid, maxBalance);
                result = maxBalance;
            }
        }
        savePlayerCurrencyAsync(playerUuid);
        return true;
    }

    public boolean withdrawPlayerGuildCurrency(UUID playerUuid, long amount)
    {
        if (amount <= 0L) return false;
        while (true)
        {
            Long current = playerGuildCurrency.get(playerUuid);
            long cur = current != null ? current : 0L;
            if (cur < amount) return false;
            if (playerGuildCurrency.replace(playerUuid, cur, cur - amount)) break;
        }
        savePlayerCurrencyAsync(playerUuid);
        return true;
    }

    public boolean setPlayerGuildCurrency(UUID playerUuid, long amount)
    {
        if (amount < 0L) return false;
        playerGuildCurrency.put(playerUuid, amount);
        savePlayerCurrencyAsync(playerUuid);
        return true;
    }

    /** 异步持久化玩家公会币余额（修复旧版重启后余额丢失问题） */
    private void savePlayerCurrencyAsync(UUID playerUuid)
    {
        long balance = playerGuildCurrency.getOrDefault(playerUuid, 0L);
        try
        {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                plugin.getDatabaseManager().savePlayerCurrency(playerUuid, balance));
        }
        catch (IllegalStateException ignored)
        {
            // 插件已禁用时跳过异步调度
        }
    }

    /** 启动时从数据库加载所有玩家的公会币余额 */
    public void loadPlayerCurrencyBalances(Map<UUID, Long> balances)
    {
        playerGuildCurrency.putAll(balances);
    }

    /**
     * 确保玩家在数据库中有公会币账户行（用于发放 initial-balance 初始余额）。
     * 仅在玩家首次加入服务器时异步调用一次。
     */
    public void ensurePlayerCurrencyRow(UUID playerUuid)
    {
        if (playerGuildCurrency.containsKey(playerUuid)) return;
        try
        {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
            {
                long initial = plugin.getCurrencyConfig().getInitialBalance();
                long balance = plugin.getDatabaseManager().ensurePlayerCurrency(playerUuid, initial);
                playerGuildCurrency.put(playerUuid, balance);
            });
        }
        catch (IllegalStateException ignored)
        {
        }
    }

    public boolean upgradeGuild(String guildName, UUID playerUuid)
    {
        Guild guild = guilds.get(guildName.toLowerCase());
        // 最高等级改为配置驱动（guild.yml max-level）
        int maxLevel = plugin.getGuildConfig().getMaxLevel();
        if (guild == null || !guild.getOwner().equals(playerUuid) || guild.getLevel() >= maxLevel) return false;
        GuildCurrency.CurrencyType currencyType = plugin.getCurrencyConfig().getCurrencyType();
        long cost = plugin.getCurrencyConfig().getLevelUpCost(guild.getLevel());
        if (!plugin.getGuildCurrency().withdraw(playerUuid, cost, currencyType)) return false;
        guild.setLevel(guild.getLevel() + 1);
        scheduleSave(guild);
        return true;
    }

    public long getUpgradeCost(String guildName)
    {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null) return -1L;
        return plugin.getCurrencyConfig().getLevelUpCost(guild.getLevel());
    }

    public boolean addExperienceWithCurrency(String guildName, UUID playerUuid)
    {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || !guild.isMember(playerUuid)) return false;
        GuildCurrency.CurrencyType currencyType = plugin.getCurrencyConfig().getCurrencyType();
        long cost = plugin.getCurrencyConfig().getExperienceCost();
        int expAmount = plugin.getCurrencyConfig().getExperienceAmount();
        if (!plugin.getGuildCurrency().withdraw(playerUuid, cost, currencyType)) return false;
        guild.addExperience(expAmount);
        GuildMember member = guild.getMember(playerUuid);
        if (member != null)
        {
            member.addContribution(expAmount);
        }
        scheduleSave(guild);
        return true;
    }

    public PlayerSettings getPlayerSettings(UUID playerUuid)
    {
        return playerSettings.computeIfAbsent(playerUuid, PlayerSettings::new);
    }

    public boolean togglePlayerInvites(UUID playerUuid)
    {
        PlayerSettings settings = getPlayerSettings(playerUuid);
        settings.toggleInvites();
        return settings.isAllowInvites();
    }

    public boolean togglePlayerNotify(UUID playerUuid)
    {
        PlayerSettings settings = getPlayerSettings(playerUuid);
        settings.toggleNotify();
        return settings.isNotifyOnlineStatus();
    }

    public Map<String, Guild> getGuilds()
    {
        return Collections.unmodifiableMap(guilds);
    }

    public void addGuild(Guild guild)
    {
        guilds.put(guild.getName().toLowerCase(), guild);
        for (UUID memberUuid : guild.getMembers().keySet())
        {
            playerGuilds.put(memberUuid, guild.getName().toLowerCase());
        }
    }

    public Map<UUID, String> getPlayerGuilds()
    {
        return Collections.unmodifiableMap(playerGuilds);
    }

    public void clearAllDailyData()
    {
        for (Guild guild : guilds.values())
        {
            guild.setDailyExperience(0L);
            for (GuildMember member : guild.getMembers().values())
            {
                member.setDailyContribution(0L);
            }
            scheduleSave(guild);
        }
        dailyBankUsage.clear();
    }

    public static class GuildRequest
    {
        private final UUID playerUuid;
        private final String playerName;
        private final long requestTime;

        public GuildRequest(UUID playerUuid, String playerName)
        {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.requestTime = System.currentTimeMillis();
        }

        public UUID getPlayerUuid() { return playerUuid; }
        public String getPlayerName() { return playerName; }
        public long getRequestTime() { return requestTime; }
    }

    public static class GuildInvite
    {
        private final String guildName;
        private final UUID inviterUuid;
        private final UUID targetUuid;
        private final String targetName;
        private final long inviteTime;

        public GuildInvite(String guildName, UUID inviterUuid, UUID targetUuid, String targetName)
        {
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
