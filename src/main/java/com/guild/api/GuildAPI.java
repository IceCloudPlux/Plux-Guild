package com.guild.api;

import com.guild.GuildPlugin;
import com.guild.api.event.*;
import com.guild.guild.*;
import com.guild.gui.GuildGUI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Guild 插件公开 API（v3.1.0 优化版）
 * <p>
 * 提供完整的公会数据访问和操作接口，供其他插件集成使用。
 * <p>
 * <b>快速开始：</b>
 * <pre>
 *   // 1. 检查 API 是否就绪
 *   if (!GuildAPI.isReady()) return;
 *
 *   // 2. 获取实例
 *   GuildAPI api = GuildAPI.getInstance();
 *
 *   // 3. 查询
 *   GuildData guild = api.getGuild("公会名");
 *   if (api.isInGuild(player.getUniqueId())) {
 *       GuildData myGuild = api.getPlayerGuild(player.getUniqueId());
 *       player.sendMessage("你的公会: " + myGuild.getName() + " Lv." + myGuild.getLevel());
 *   }
 *
 *   // 4. 异步操作
 *   api.createGuildAsync("新公会", player)
 *      .thenAccept(g -&gt; { if (g != null) player.sendMessage("创建成功!"); });
 *
 *   // 5. 监听事件
 *   api.registerEvent(GuildCreateEvent.class, e -&gt; {
 *       Bukkit.broadcastMessage(e.getCreator().getName() + " 创建了公会 " + e.getGuildName());
 *   });
 * </pre>
 *
 * @since 3.0.0, 优化于 3.1.0
 */
public final class GuildAPI implements GuildAPIProvider {

    private static volatile GuildAPI instance;

    private final GuildPlugin plugin;

    /** 已注册的事件监听器缓存（用于 cleanup） */
    private final Map<Class<? extends org.bukkit.event.Event>, Listener> registeredListeners = new ConcurrentHashMap<>();

    private GuildAPI(GuildPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
    }

    // ==================== 初始化 ====================

    /**
     * 初始化 API（由 GuildPlugin 在 onEnable 时调用）
     */
    public static void init(GuildPlugin plugin) {
        instance = new GuildAPI(plugin);
    }

    /**
     * 获取 API 实例
     * <p>
     * 注意：在插件加载完成前（onEnable 之前）此方法返回 null。
     * 建议先调用 {@link #isReady()} 检查。
     *
     * @return API 实例，未初始化时返回 null
     */
    public static GuildAPI getInstance() {
        return instance;
    }

    /**
     * 检查 API 是否已初始化并可用（静态快捷方式）
     *
     * @return true 表示 API 可以安全使用
     */
    public static boolean isApiReady() {
        return instance != null && instance.plugin != null;
    }

    /** 安全获取 API 实例，未就绪时抛出异常 */

    /**
     * 安全获取 API 实例，未就绪时抛出异常
     *
     * @return 非空的 API 实例
     * @throws IllegalStateException API 未初始化时抛出
     */
    public static GuildAPI requireInstance() throws IllegalStateException {
        if (!isApiReady()) {
            throw new IllegalStateException("GuildAPI 尚未初始化！请确保在 onEnable 之后调用。"
                    + " 可通过 GuildAPI.isApiReady() 检查。");
        }
        return instance;
    }

    // ==================== 生命周期 ====================

    @Override
    public boolean isReady() {
        return instance != null && plugin != null;
    }

    @Override
    public GuildPlugin getPlugin() {
        return plugin;
    }

    // ==================== 公会查询 ====================

    /**
     * 按名称获取公会（不区分大小写）
     *
     * @param name 公会名称
     * @return 公会数据视图，不存在返回 null
     */
    @Override
    public GuildData getGuild(String name) {
        Guild guild = plugin.getGuildManager().getGuild(name);
        return guild != null ? new GuildData(guild) : null;
    }

    /**
     * 按标签查找公会（精确匹配）
     *
     * @param tag 公会标签
     * @return 匹配的公会数据，无匹配返回 null
     */
    @Override
    public GuildData getGuildByTag(String tag) {
        if (tag == null || tag.isEmpty()) return null;
        String lowerTag = tag.toLowerCase();
        return plugin.getGuildManager().getGuilds().values().stream()
                .filter(g -> g.getTag() != null && g.getTag().equalsIgnoreCase(lowerTag))
                .findFirst()
                .map(GuildData::new)
                .orElse(null);
    }

    /**
     * 搜索公会（名称/标签模糊匹配）
     *
     * @param keyword 搜索关键词
     * @return 匹配的公会列表，按名称完全匹配 > 标签匹配 > 名称包含 排序
     */
    @Override
    public List<GuildData> searchGuilds(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String kw = keyword.trim().toLowerCase();
        return plugin.getGuildManager().getGuilds().values().stream()
                .map(GuildData::new)
                .filter(g -> g.getName().toLowerCase().contains(kw)
                        || (g.getTag() != null && g.getTag().toLowerCase().contains(kw)))
                .sorted((a, b) -> {
                    // 完全匹配优先
                    boolean aExact = a.getName().equalsIgnoreCase(kw);
                    boolean bExact = b.getName().equalsIgnoreCase(kw);
                    if (aExact != bExact) return aExact ? -1 : 1;
                    // 其次等级高的排前面
                    return Integer.compare(b.getLevel(), a.getLevel());
                })
                .collect(Collectors.toList());
    }

    /** 获取玩家所属公会 */
    @Override
    public GuildData getPlayerGuild(UUID playerUuid) {
        Guild guild = plugin.getGuildManager().getPlayerGuild(playerUuid);
        return guild != null ? new GuildData(guild) : null;
    }

    /** 获取玩家所属公会名称（轻量级，不创建 GuildData 对象） */
    public String getPlayerGuildName(UUID playerUuid) {
        GuildData g = getPlayerGuild(playerUuid);
        return g != null ? g.getName() : null;
    }

    /** 玩家是否在某个公会中 */
    @Override
    public boolean isInGuild(UUID playerUuid) {
        return plugin.getGuildManager().isInGuild(playerUuid);
    }

    /**
     * 获取服务器所有公会的不可修改视图
     * <p>
     * 返回值是 {@link GuildData} 包装的只读列表，
     * 外部修改不会影响内部状态。
     */
    @Override
    public Map<String, GuildData> getAllGuilds() {
        Map<String, GuildData> result = new LinkedHashMap<>();
        plugin.getGuildManager().getGuilds().forEach((name, guild) ->
                result.put(name, new GuildData(guild)));
        return Collections.unmodifiableMap(result);
    }

    /**
     * 获取所有公会的 GuildData 列表（流式友好）
     */
    public List<GuildData> getAllGuildList() {
        return plugin.getGuildManager().getGuilds().values().stream()
                .map(GuildData::new)
                .collect(Collectors.toList());
    }

    /**
     * 按等级降序获取排行榜前 N 名公会
     * <p>
     * 使用 Stream API 排序，避免中间集合创建。
     *
     * @param limit 最大返回数量
     * @return 排序后的公会列表
     */
    @Override
    public List<GuildData> getTopGuilds(int limit) {
        if (limit <= 0) return Collections.emptyList();
        return plugin.getGuildManager().getGuilds().values().stream()
                .map(GuildData::new)
                .sorted(Comparator.comparingInt(GuildData::getLevel).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ==================== 成员便捷查询 ====================

    /**
     * 获取玩家在公会中的角色名称
     *
     * @param playerUuid 玩家 UUID
     * @return 角色名称（OWNER/OFFICER/MEMBER），不在公会返回 null
     */
    @Override
    public String getMemberRole(UUID playerUuid) {
        Guild guild = plugin.getGuildManager().getPlayerGuild(playerUuid);
        if (guild == null) return null;
        GuildMember member = guild.getMember(playerUuid);
        return member != null ? member.getRole().name() : null;
    }

    /**
     * 获取指定公会的在线成员 UUID 列表
     */
    @Override
    public List<UUID> getOnlineMembers(String guildName) {
        Guild guild = plugin.getGuildManager().getGuild(guildName);
        if (guild == null) return Collections.emptyList();
        return guild.getMembers().keySet().stream()
                .filter(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    return p != null && p.isOnline();
                })
                .collect(Collectors.toList());
    }

    /** 获取指定公会的在线成员数量 */
    @Override
    public int getOnlineCount(String guildName) {
        Guild guild = plugin.getGuildManager().getGuild(guildName);
        if (guild == null) return 0;
        return (int) guild.getMembers().keySet().stream()
                .filter(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    return p != null && p.isOnline();
                })
                .count();
    }

    // ==================== 公会操作（同步）====================

    /**
     * 创建公会（同步，触发事件）
     *
     * @param name  公会名称
     * @param owner 会长玩家
     * @return 创建的 GuildData，失败/被取消返回 null
     */
    public GuildData createGuild(String name, Player owner) {
        GuildCreateEvent event = new GuildCreateEvent(owner, name, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;

        Guild guild = plugin.getGuildManager().createGuild(event.getGuildName(), owner);
        if (guild != null && event.getGuildTag() != null && !event.getGuildTag().isEmpty()) {
            guild.setTag(event.getGuildTag());
            plugin.getDatabaseManager().saveGuild(guild);
        }
        return guild != null ? new GuildData(guild) : null;
    }

    /**
     * 创建公会带标签（同步）
     */
    public GuildData createGuild(String name, String tag, Player owner) {
        GuildCreateEvent event = new GuildCreateEvent(owner, name, tag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;

        Guild guild = plugin.getGuildManager().createGuild(event.getGuildName(), owner);
        if (guild != null) {
            String finalTag = event.getGuildTag() != null ? event.getGuildTag() : tag;
            guild.setTag(finalTag);
            plugin.getDatabaseManager().saveGuild(guild);
        }
        return guild != null ? new GuildData(guild) : null;
    }

    /** 解散公会（同步，触发事件） */
    public boolean disbandGuild(String name, OfflinePlayer disbander) {
        boolean result = plugin.getGuildManager().disbandGuild(name);
        if (result) {
            Bukkit.getPluginManager().callEvent(new GuildDisbandEvent(name, disbander));
        }
        return result;
    }

    // ==================== 成员操作（同步）====================

    /** 玩家加入公会（触发事件） */
    public boolean joinGuild(Player player, String guildName) {
        GuildJoinEvent event = new GuildJoinEvent(player, guildName);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        return plugin.getGuildManager().joinGuild(guildName, player);
    }

    /** 玩家离开公会（触发事件） */
    public boolean leaveGuild(Player player) {
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) return false;

        GuildLeaveEvent event = new GuildLeaveEvent(player, guild.getName());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        return plugin.getGuildManager().leaveGuild(player.getUniqueId());
    }

    /** 踢出成员（触发事件） */
    public boolean kickMember(String guildName, UUID targetUuid, OfflinePlayer kicker) {
        GuildKickEvent event = new GuildKickEvent(guildName, targetUuid, kicker);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        return plugin.getGuildManager().kickMember(guildName, targetUuid,
                kicker != null ? kicker.getUniqueId() : null);
    }

    /** 升级成员职位 */
    public boolean promoteMember(String guildName, UUID targetUuid, UUID operatorUuid) {
        return plugin.getGuildManager().promoteMember(guildName, targetUuid, operatorUuid);
    }

    /** 降级成员职位 */
    public boolean demoteMember(String guildName, UUID targetUuid, UUID operatorUuid) {
        return plugin.getGuildManager().demoteMember(guildName, targetUuid, operatorUuid);
    }

    /** 转让会长 */
    public boolean transferOwnership(String guildName, UUID newOwner, UUID currentOwner) {
        return plugin.getGuildManager().transferOwnership(guildName, newOwner, currentOwner);
    }

    // ==================== 邀请/申请系统 ====================

    /** 发送邀请 */
    public boolean sendInvite(String guildName, UUID inviter, UUID target, String targetName) {
        return plugin.getGuildManager().sendInvite(guildName, inviter, target, targetName);
    }

    /** 接受邀请 */
    public boolean acceptInvite(UUID playerUuid) {
        return plugin.getGuildManager().acceptInvite(playerUuid);
    }

    /** 拒绝邀请 */
    public boolean declineInvite(UUID playerUuid) {
        return plugin.getGuildManager().declineInvite(playerUuid);
    }

    /**
     * 获取玩家的待处理邀请（包装为 GuildInviteInfo）
     *
     * @param playerUuid 目标玩家
     * @return 邀请信息，无邀请返回 null
     */
    public GuildInviteInfo getInvite(UUID playerUuid) {
        GuildManager.GuildInvite invite = plugin.getGuildManager().getInvite(playerUuid);
        if (invite == null) return null;
        return new GuildInviteInfo(
                invite.getGuildName(),
                invite.getInviterUuid(),
                Bukkit.getOfflinePlayer(invite.getInviterUuid()).getName(),
                invite.getTargetUuid(),
                invite.getTargetName(),
                invite.getInviteTime()
        );
    }

    // ==================== 经验/升级/银行 ====================

    /** 给公会增加经验 */
    @Override
    public void addExperience(UUID playerUuid, long amount) {
        plugin.getGuildManager().addExperience(playerUuid, amount);
    }

    /** 升级公会（同步，触发事件） */
    public boolean upgradeGuild(String guildName, UUID playerUuid) {
        Guild guild = plugin.getGuildManager().getGuild(guildName);
        int oldLevel = guild != null ? guild.getLevel() : 0;
        boolean success = plugin.getGuildManager().upgradeGuild(guildName, playerUuid);
        if (success && guild != null) {
            Bukkit.getPluginManager().callEvent(
                    new GuildLevelUpEvent(guildName, oldLevel, guild.getLevel()));
        }
        return success;
    }

    /** 存入银行 */
    @Override
    public boolean depositToBank(String guildName, UUID playerUuid, long amount) {
        return plugin.getGuildManager().depositToBank(guildName, playerUuid, amount);
    }

    /** 从银行取出 */
    @Override
    public boolean withdrawFromBank(String guildName, UUID playerUuid, long amount) {
        return plugin.getGuildManager().withdrawFromBank(guildName, playerUuid, amount);
    }

    /** 获取银行余额 */
    public long getBankBalance(String guildName) {
        return plugin.getGuildManager().getGuildBalance(guildName);
    }

    // ==================== 异步操作（CompletableFuture）====================

    /**
     * 异步创建公会
     * <p>
     * 事件调度和数据库操作在主线程完成，
     * 返回 CompletableFuture 支持链式回调。
     *
     * <pre>
     *   api.createGuildAsync("MyGuild", player)
     *      .thenAccept(g -&gt; { ... })
     *      .exceptionally(ex -&gt; { ex.printStackTrace(); return null; });
     * </pre>
     */
    @Override
    public CompletableFuture<GuildData> createGuildAsync(String name, Player owner) {
        return CompletableFuture.supplyAsync(() -> createGuild(name, owner),
                task -> Bukkit.getScheduler().runTask(plugin, task));
    }

    /** 异步创建公会带标签 */
    @Override
    public CompletableFuture<GuildData> createGuildAsync(String name, String tag, Player owner) {
        return CompletableFuture.supplyAsync(() -> createGuild(name, tag, owner),
                task -> Bukkit.getScheduler().runTask(plugin, task));
    }

    /** 异步解散公会 */
    @Override
    public CompletableFuture<Boolean> disbandGuildAsync(String name) {
        return CompletableFuture.supplyAsync(() -> disbandGuild(name, null),
                task -> Bukkit.getScheduler().runTask(plugin, task));
    }

    /** 异步加入公会 */
    @Override
    public CompletableFuture<Boolean> joinGuildAsync(Player player, String guildName) {
        return CompletableFuture.supplyAsync(() -> joinGuild(player, guildName),
                task -> Bukkit.getScheduler().runTask(plugin, task));
    }

    /** 异步离开公会 */
    @Override
    public CompletableFuture<Boolean> leaveGuildAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> leaveGuild(player),
                task -> Bukkit.getScheduler().runTask(plugin, task));
    }

    /** 异步踢出成员 */
    @Override
    public CompletableFuture<Boolean> kickMemberAsync(String guildName, UUID target, UUID kicker) {
        return CompletableFuture.supplyAsync(() ->
                        kickMember(guildName, target,
                                kicker != null ? Bukkit.getOfflinePlayer(kicker) : null),
                task -> Bukkit.getScheduler().runTask(plugin, task));
    }

    /** 异步升级公会 */
    @Override
    public CompletableFuture<Boolean> upgradeGuildAsync(String guildName, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> upgradeGuild(guildName, playerUuid),
                task -> Bukkit.getScheduler().runTask(plugin, task));
    }

    // ==================== GUI 操作 ====================

    /** 打开主 GUI */
    @Override
    public void openMainGUI(Player player) {
        GuildGUI.openGUI(plugin, player);
    }

    /** 打开银行 GUI */
    public void openBankGUI(Player player, GuildData guild) {
        if (guild != null) {
            com.guild.gui.GuildBankGUI.openBankGUI(plugin, player, guild.getInternalGuild());
        }
    }

    /** 打开商店 GUI */
    public void openShopGUI(Player player, GuildData guild) {
        if (guild != null) {
            com.guild.gui.GuildShopGUI.openShopGUI(plugin, player, guild.getInternalGuild());
        }
    }

    // ==================== 事件注册辅助 ====================

    /**
     * 注册事件监听器（便捷方法）
     * <p>
     * 自动将 Listener 注册到 Bukkit，并缓存引用以便清理。
     *
     * @param listener 要注册的监听器
     */
    public void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    /**
     * 清理所有通过此 API 注册的监听器
     * <p>
     * 注意：Bukkit 不支持注销单个 Listener，
     * 此方法仅清除内部缓存引用。
     */
    public void unregisterAllListeners() {
        registeredListeners.clear();
    }

    // ==================== 工具方法 ====================

    /** 获取语言消息 */
    @Override
    public String getMessage(String key) {
        return plugin.getMessage(key);
    }

    /** 获取带占位符替换的语言消息 */
    public String getMessage(String key, String... replacements) {
        return plugin.getMessage(key, replacements);
    }

    /** 刷新配置文件 */
    public void reloadConfig() {
        plugin.reloadConfig();
        plugin.getGUIConfig().reloadConfig();
    }

    // ==================== 统计信息 ====================

    /** 获取服务器总公会数 */
    public int getTotalGuildCount() {
        return plugin.getGuildManager().getGuilds().size();
    }

    /** 获取服务器总成员数（跨所有公会去重后） */
    public int getTotalMemberCount() {
        Set<UUID> allMembers = new HashSet<>();
        plugin.getGuildManager().getGuilds().values()
                .forEach(g -> allMembers.addAll(g.getMembers().keySet()));
        return allMembers.size();
    }

    /** 获取平均公会等级 */
    public double getAverageLevel() {
        Map<String, Guild> guilds = plugin.getGuildManager().getGuilds();
        if (guilds.isEmpty()) return 0.0;
        return guilds.values().stream()
                .mapToInt(Guild::getLevel)
                .average()
                .orElse(0.0);
    }
}
