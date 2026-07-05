package com.guild.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 公会 API 提供者接口
 * <p>
 * 定义公会系统的核心操作契约，支持依赖注入和 Mock 测试。
 * 通过 {@link GuildAPI#getInstance()} 获取默认实现。
 *
 * @since 3.1.0
 */
public interface GuildAPIProvider {

    // ========== 生命周期 ==========

    /** API 是否已初始化并可用（默认实现） */
    default boolean isReady() {
        return false;
    }

    /** 获取插件实例 */
    Plugin getPlugin();

    // ========== 公会查询 ==========

    /**
     * 按名称获取公会（不区分大小写）
     *
     * @param name 公会名称
     * @return 公会对象，不存在返回 null
     */
    GuildData getGuild(String name);

    /**
     * 按标签获取公会（模糊匹配）
     *
     * @param tag 公会标签
     * @return 匹配的公会，无匹配返回 null
     */
    GuildData getGuildByTag(String tag);

    /**
     * 搜索公会（名称/标签模糊匹配）
     *
     * @param keyword 关键词
     * @return 匹配的公会列表，按相关度排序
     */
    List<GuildData> searchGuilds(String keyword);

    /** 获取玩家所属公会 */
    GuildData getPlayerGuild(UUID playerUuid);

    /** 玩家是否在某个公会中 */
    boolean isInGuild(UUID playerUuid);

    /** 获取服务器所有公会（不可修改视图） */
    Map<String, GuildData> getAllGuilds();

    /** 按等级降序获取排行榜前 N 名公会 */
    List<GuildData> getTopGuilds(int limit);

    // ========== 成员查询 ==========

    /** 获取玩家在公会中的角色 */
    String getMemberRole(UUID playerUuid);

    /** 获取公会的在线成员列表 */
    List<UUID> getOnlineMembers(String guildName);

    /** 获取公会的在线成员数量 */
    int getOnlineCount(String guildName);

    // ========== 公会操作 ==========

    /** 创建公会（触发事件） */
    CompletableFuture<GuildData> createGuildAsync(String name, Player owner);

    /** 创建公会带标签（触发事件） */
    CompletableFuture<GuildData> createGuildAsync(String name, String tag, Player owner);

    /** 解散公会（触发事件） */
    CompletableFuture<Boolean> disbandGuildAsync(String name);

    // ========== 成员操作 ==========

    /** 玩家加入公会（触发事件） */
    CompletableFuture<Boolean> joinGuildAsync(Player player, String guildName);

    /** 玩家离开公会（触发事件） */
    CompletableFuture<Boolean> leaveGuildAsync(Player player);

    /** 踢出成员（触发事件） */
    CompletableFuture<Boolean> kickMemberAsync(String guildName, UUID target, UUID kicker);

    // ========== 经验/银行 ==========

    /** 给公会增加经验 */
    void addExperience(UUID playerUuid, long amount);

    /** 升级公会（触发事件） */
    CompletableFuture<Boolean> upgradeGuildAsync(String guildName, UUID playerUuid);

    /** 存入银行 */
    boolean depositToBank(String guildName, UUID playerUuid, long amount);

    /** 从银行取出 */
    boolean withdrawFromBank(String guildName, UUID playerUuid, long amount);

    // ========== GUI ==========

    /** 打开主 GUI */
    void openMainGUI(Player player);

    // ========== 消息 ==========

    /** 获取语言消息 */
    String getMessage(String key);
}
