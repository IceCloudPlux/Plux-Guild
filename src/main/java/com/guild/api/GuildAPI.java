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

public final class GuildAPI implements GuildAPIProvider
{
    private static volatile GuildAPI instance;

    private final GuildPlugin plugin;

    private final Map<Class<? extends org.bukkit.event.Event>, Listener> registeredListeners = new ConcurrentHashMap<>();

    private GuildAPI(GuildPlugin plugin)
    {
        this.plugin = Objects.requireNonNull(plugin);
    }

    public static void init(GuildPlugin plugin)
    {
        instance = new GuildAPI(plugin);
    }

    public static GuildAPI getInstance()
    {
        return instance;
    }

    public static boolean isApiReady()
    {
        return instance != null && instance.plugin != null;
    }

    public static GuildAPI requireInstance() throws IllegalStateException
    {
        if (!isApiReady())
        {
            throw new IllegalStateException("GuildAPI 尚未初始化！请确保在 onEnable 之后调用。" + " 可通过 GuildAPI.isApiReady() 检查。");
        }
        return instance;
    }

@Override
public boolean isReady()
    {
        return instance != null && plugin != null;
    }

@Override
public GuildPlugin getPlugin()
    {
        return plugin;
    }

@Override
public GuildData getGuild(String name)
    {
        Guild guild = plugin.getGuildManager().getGuild(name);
        return guild != null ? new GuildData(guild) : null;
    }

@Override
public GuildData getGuildByTag(String tag)
    {
        if (tag == null || tag.isEmpty()) return null;
        String lowerTag = tag.toLowerCase();
        return plugin.getGuildManager().getGuilds().values().stream() .filter(g -> g.getTag() != null && g.getTag().equalsIgnoreCase(lowerTag)) .findFirst() .map(GuildData::new) .orElse( null);
    }

@Override
public List<GuildData> searchGuilds(String keyword)
    {
        if (keyword == null || keyword.trim().isEmpty())
        {
            return Collections.emptyList();
        }
        String kw = keyword.trim().toLowerCase();
        return plugin.getGuildManager().getGuilds().values().stream() .map(GuildData::new) .filter(g -> g.getName().toLowerCase().contains(kw) || (g.getTag() != null && g.getTag().toLowerCase().contains(kw))) .sorted((a, b) ->
        {
            boolean aExact = a.getName().equalsIgnoreCase(kw);
            boolean bExact = b.getName().equalsIgnoreCase(kw);
            if (aExact != bExact) return aExact ? -1 : 1;
            return Integer.compare(b.getLevel(), a.getLevel());
        }) .collect(Collectors.toList());
    }

@Override
public GuildData getPlayerGuild(UUID playerUuid)
    {
        Guild guild = plugin.getGuildManager().getPlayerGuild(playerUuid);
        return guild != null ? new GuildData(guild) : null;
    }

    public String getPlayerGuildName(UUID playerUuid)
    {
        GuildData g = getPlayerGuild(playerUuid);
        return g != null ? g.getName() : null;
    }

@Override
public boolean isInGuild(UUID playerUuid)
    {
        return plugin.getGuildManager().isInGuild(playerUuid);
    }

@Override
public Map<String, GuildData> getAllGuilds()
    {
        Map<String, GuildData> result = new LinkedHashMap<>();
        plugin.getGuildManager().getGuilds().forEach((name, guild) -> result.put(name, new GuildData(guild)));
        return Collections.unmodifiableMap(result);
    }

    public List<GuildData> getAllGuildList()
    {
        return plugin.getGuildManager().getGuilds().values().stream() .map(GuildData::new) .collect(Collectors.toList());
    }

@Override
public List<GuildData> getTopGuilds(int limit)
    {
        if (limit <= 0) return Collections.emptyList();
        return plugin.getGuildManager().getGuilds().values().stream() .map(GuildData::new) .sorted(Comparator.comparingInt(GuildData::getLevel).reversed()) .limit(limit) .collect(Collectors.toList());
    }

@Override
public String getMemberRole(UUID playerUuid)
    {
        Guild guild = plugin.getGuildManager().getPlayerGuild(playerUuid);
        if (guild == null) return null;
        GuildMember member = guild.getMember(playerUuid);
        return member != null ? member.getRole().name() : null;
    }

@Override
public List<UUID> getOnlineMembers(String guildName)
    {
        Guild guild = plugin.getGuildManager().getGuild(guildName);
        if (guild == null) return Collections.emptyList();
        return guild.getMembers().keySet().stream() .filter(uuid ->
        {
            Player p = Bukkit.getPlayer(uuid);
            return p != null && p.isOnline();
        }) .collect(Collectors.toList());
    }

@Override
public int getOnlineCount(String guildName)
    {
        Guild guild = plugin.getGuildManager().getGuild(guildName);
        if (guild == null) return 0;
        return (int) guild.getMembers().keySet().stream() .filter(uuid ->
        {
            Player p = Bukkit.getPlayer(uuid);
            return p != null && p.isOnline();
        }) .count();
    }

    public GuildData createGuild(String name, Player owner)
    {
        GuildCreateEvent event = new GuildCreateEvent(owner, name, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;
        Guild guild = plugin.getGuildManager().createGuild(event.getGuildName(), owner);
        if (guild != null && event.getGuildTag() != null && !event.getGuildTag().isEmpty())
        {
            guild.setTag(event.getGuildTag());
            plugin.getDatabaseManager().saveGuild(guild);
        }
        return guild != null ? new GuildData(guild) : null;
    }

    public GuildData createGuild(String name, String tag, Player owner)
    {
        GuildCreateEvent event = new GuildCreateEvent(owner, name, tag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;
        Guild guild = plugin.getGuildManager().createGuild(event.getGuildName(), owner);
        if (guild != null)
        {
            String finalTag = event.getGuildTag() != null ? event.getGuildTag() : tag;
            guild.setTag(finalTag);
            plugin.getDatabaseManager().saveGuild(guild);
        }
        return guild != null ? new GuildData(guild) : null;
    }

    public boolean disbandGuild(String name, OfflinePlayer disbander)
    {
        boolean result = plugin.getGuildManager().disbandGuild(name, disbander.getUniqueId());
        if (result)
        {
            Bukkit.getPluginManager().callEvent( new GuildDisbandEvent(name, disbander));
        }
        return result;
    }

    public boolean joinGuild(Player player, String guildName)
    {
        GuildJoinEvent event = new GuildJoinEvent(player, guildName);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        return plugin.getGuildManager().joinGuild(guildName, player);
    }

    public boolean leaveGuild(Player player)
    {
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) return false;
        GuildLeaveEvent event = new GuildLeaveEvent(player, guild.getName());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        return plugin.getGuildManager().leaveGuild(player.getUniqueId());
    }

    public boolean kickMember(String guildName, UUID targetUuid, OfflinePlayer kicker)
    {
        GuildKickEvent event = new GuildKickEvent(guildName, targetUuid, kicker);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        return plugin.getGuildManager().kickMember(guildName, targetUuid, kicker != null ? kicker.getUniqueId() : null);
    }

    public boolean promoteMember(String guildName, UUID targetUuid, UUID operatorUuid)
    {
        return plugin.getGuildManager().promoteMember(guildName, targetUuid, operatorUuid);
    }

    public boolean demoteMember(String guildName, UUID targetUuid, UUID operatorUuid)
    {
        return plugin.getGuildManager().demoteMember(guildName, targetUuid, operatorUuid);
    }

    public boolean transferOwnership(String guildName, UUID newOwner, UUID currentOwner)
    {
        return plugin.getGuildManager().transferOwnership(guildName, newOwner, currentOwner);
    }

    public boolean sendInvite(String guildName, UUID inviter, UUID target, String targetName)
    {
        return plugin.getGuildManager().sendInvite(guildName, inviter, target, targetName);
    }

    public boolean acceptInvite(UUID playerUuid)
    {
        return plugin.getGuildManager().acceptInvite(playerUuid);
    }

    public boolean declineInvite(UUID playerUuid)
    {
        return plugin.getGuildManager().declineInvite(playerUuid);
    }

    public GuildInviteInfo getInvite(UUID playerUuid)
    {
        GuildManager.GuildInvite invite = plugin.getGuildManager().getInvite(playerUuid);
        if (invite == null) return null;
        return new GuildInviteInfo( invite.getGuildName(), invite.getInviterUuid(), Bukkit.getOfflinePlayer(invite.getInviterUuid()).getName(), invite.getTargetUuid(), invite.getTargetName(), invite.getInviteTime() );
    }

@Override
public void addExperience(UUID playerUuid, long amount)
    {
        plugin.getGuildManager().addExperience(playerUuid, amount);
    }

    public boolean upgradeGuild(String guildName, UUID playerUuid)
    {
        Guild guild = plugin.getGuildManager().getGuild(guildName);
        int oldLevel = guild != null ? guild.getLevel() : 0;
        boolean success = plugin.getGuildManager().upgradeGuild(guildName, playerUuid);
        if (success && guild != null)
        {
            Bukkit.getPluginManager().callEvent( new GuildLevelUpEvent(guildName, oldLevel, guild.getLevel()));
        }
        return success;
    }

@Override
public boolean depositToBank(String guildName, UUID playerUuid, long amount)
    {
        return plugin.getGuildManager().depositToBank(guildName, playerUuid, amount);
    }

@Override
public boolean withdrawFromBank(String guildName, UUID playerUuid, long amount)
    {
        return plugin.getGuildManager().withdrawFromBank(guildName, playerUuid, amount);
    }

    public long getBankBalance(String guildName)
    {
        return plugin.getGuildManager().getGuildBalance(guildName);
    }

@Override
public CompletableFuture<GuildData> createGuildAsync(String name, Player owner)
    {
        return CompletableFuture.supplyAsync(() -> createGuild(name, owner), task -> Bukkit.getScheduler().runTask(plugin, task));
    }

@Override
public CompletableFuture<GuildData> createGuildAsync(String name, String tag, Player owner)
    {
        return CompletableFuture.supplyAsync(() -> createGuild(name, tag, owner), task -> Bukkit.getScheduler().runTask(plugin, task));
    }

@Override
public CompletableFuture<Boolean> disbandGuildAsync(String name)
    {
        return CompletableFuture.supplyAsync(() -> disbandGuild(name, null), task -> Bukkit.getScheduler().runTask(plugin, task));
    }

@Override
public CompletableFuture<Boolean> joinGuildAsync(Player player, String guildName)
    {
        return CompletableFuture.supplyAsync(() -> joinGuild(player, guildName), task -> Bukkit.getScheduler().runTask(plugin, task));
    }

@Override
public CompletableFuture<Boolean> leaveGuildAsync(Player player)
    {
        return CompletableFuture.supplyAsync(() -> leaveGuild(player), task -> Bukkit.getScheduler().runTask(plugin, task));
    }

@Override
public CompletableFuture<Boolean> kickMemberAsync(String guildName, UUID target, UUID kicker)
    {
        return CompletableFuture.supplyAsync(() -> kickMember(guildName, target, kicker != null ? Bukkit.getOfflinePlayer(kicker) : null), task -> Bukkit.getScheduler().runTask(plugin, task));
    }

@Override
public CompletableFuture<Boolean> upgradeGuildAsync(String guildName, UUID playerUuid)
    {
        return CompletableFuture.supplyAsync(() -> upgradeGuild(guildName, playerUuid), task -> Bukkit.getScheduler().runTask(plugin, task));
    }

@Override
public void openMainGUI(Player player)
    {
        GuildGUI.openGUI(plugin, player);
    }

    public void openBankGUI(Player player, GuildData guild)
    {
        if (guild != null)
        {
            com.guild.gui.GuildBankGUI.openBankGUI(plugin, player, guild.getInternalGuild());
        }}

        public void openShopGUI(Player player, GuildData guild)
        {
            if (guild != null)
            {
                com.guild.gui.GuildShopGUI.openShopGUI(plugin, player, guild.getInternalGuild());
            }}

            public void registerListener(Listener listener)
            {
                Bukkit.getPluginManager().registerEvents(listener, plugin);
            }

            public void unregisterAllListeners()
            {
                registeredListeners.clear();
            }

@Override
public String getMessage(String key)
            {
                return plugin.getMessage(key);
            }

            public String getMessage(String key, String... replacements)
            {
                return plugin.getMessage(key, replacements);
            }

            public void reloadConfig()
            {
                plugin.reloadConfig();
                plugin.getGUIConfig().reloadConfig();
            }

            public int getTotalGuildCount()
            {
                return plugin.getGuildManager().getGuilds().size();
            }

            public int getTotalMemberCount()
            {
                Set<UUID> allMembers = new HashSet<>();
                plugin.getGuildManager().getGuilds().values() .forEach(g -> allMembers.addAll(g.getMembers().keySet()));
                return allMembers.size();
            }

            public double getAverageLevel()
            {
                Map<String, Guild> guilds = plugin.getGuildManager().getGuilds();
                if (guilds.isEmpty()) return 0.0;
                return guilds.values().stream() .mapToInt(Guild::getLevel) .average() .orElse(0.0);
            }}