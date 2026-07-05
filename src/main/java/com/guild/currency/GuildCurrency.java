package com.guild.currency;

import com.guild.GuildPlugin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 公会货币管理器，统一处理 Vault 经济和 PlayerPoints 积分系统
 *
 * 优化点：
 * - 全部使用反射调用 Vault / PlayerPoints，无硬编译依赖
 * - 缓存反射 Method 对象，避免每次调用都进行方法查找
 * - 统一 withdraw/deposit/getBalance 接口，简化上层调用
 */
public class GuildCurrency {

    private final GuildPlugin plugin;

    /** Vault 反射缓存 */
    private Object vaultEconomy;
    private Method vaultHasMethod;
    private Method vaultWithdrawMethod;
    private Method vaultDepositMethod;
    private Method vaultGetBalanceMethod;
    private boolean vaultInitialized = false;
    private boolean vaultAvailable = false;

    /** PlayerPoints 反射缓存 */
    private boolean playerPointsEnabled = false;
    private static Method ppLookUpMethod;
    private static Method ppGetMethod;
    private static Method ppSetMethod;
    private static Method ppGiveMethod;
    private static Method ppTakeMethod;
    private static boolean ppMethodsInitialized = false;
    /** 缓存的 PlayerPoints API 实例（避免每次调用都查询 ServiceManager） */
    private static Object cachedPlayerPointsApi;

    public GuildCurrency(GuildPlugin plugin) {
        this.plugin = plugin;
        initVault();
        initPlayerPoints();
    }

    // ========== 初始化 ==========

    @SuppressWarnings("unchecked")
    private synchronized void initVault() {
        if (vaultInitialized) return;
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                vaultAvailable = false;
                vaultInitialized = true;
                return;
            }
            // 通过反射获取 Economy 接口和 provider
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Class<?> serviceManagerClass = Class.forName("net.milkbowl.vault.Vault");
            Object vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault");

            Method getServicesManager = serviceManagerClass.getMethod("getServicesManager");
            Object servicesManager = getServicesManager.invoke(vaultPlugin);

            Method getRegistration = servicesManager.getClass()
                    .getMethod("getRegistration", Class.class);
            Object registration = getRegistration.invoke(servicesManager, economyClass);
            if (registration == null) {
                vaultAvailable = false;
                vaultInitialized = true;
                return;
            }
            Method getProvider = registration.getClass().getMethod("getProvider");
            vaultEconomy = getProvider.invoke(registration);

            // 缓存 Economy 方法
            Class<?> ecoClass = vaultEconomy.getClass();
            vaultHasMethod = ecoClass.getMethod("has", OfflinePlayer.class, double.class);
            vaultWithdrawMethod = ecoClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            vaultDepositMethod = ecoClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            vaultGetBalanceMethod = ecoClass.getMethod("getBalance", OfflinePlayer.class);

            vaultAvailable = true;
        } catch (ClassNotFoundException e) {
            // Vault 不在 classpath 中，静默忽略
            vaultAvailable = false;
        } catch (Exception e) {
            vaultAvailable = false;
        }
        vaultInitialized = true;
    }

    private synchronized void initPlayerPoints() {
        if (ppMethodsInitialized) return;
        try {
            Class<?> ppApiClass = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
            Object ppApi = Bukkit.getServicesManager()
                    .getRegistration(ppApiClass).getProvider();

            // 缓存 API 实例（避免后续每次调用都查 ServiceManager）
            cachedPlayerPointsApi = ppApi;

            ppLookUpMethod = ppApiClass.getMethod("lookUpUUID", UUID.class);
            ppGetMethod = ppApiClass.getMethod("get", UUID.class);
            ppSetMethod = ppApiClass.getMethod("set", UUID.class, int.class);
            ppGiveMethod = ppApiClass.getMethod("give", UUID.class, int.class);
            ppTakeMethod = ppApiClass.getMethod("take", UUID.class, int.class);

            playerPointsEnabled = true;
        } catch (Exception e) {
            playerPointsEnabled = false;
        }
        ppMethodsInitialized = true;
    }

    // ========== 核心接口 ==========

    public boolean withdraw(UUID playerUuid, long amount, CurrencyType type) {
        switch (type) {
            case VAULT:
                return withdrawFromVault(playerUuid, amount);
            case PLAYER_POINTS:
                return takeFromPlayerPoints(playerUuid, (int) amount);
            case GUILD_COIN:
                return plugin.getGuildManager().withdrawPlayerGuildCurrency(playerUuid, amount);
            default:
                return false;
        }
    }

    public boolean deposit(UUID playerUuid, long amount, CurrencyType type) {
        switch (type) {
            case VAULT:
                return depositToVault(playerUuid, amount);
            case PLAYER_POINTS:
                return givePlayerPoints(playerUuid, (int) amount);
            case GUILD_COIN:
                return plugin.getGuildManager().depositPlayerGuildCurrency(playerUuid, amount);
            default:
                return false;
        }
    }

    public long getBalance(UUID playerUuid, CurrencyType type) {
        switch (type) {
            case VAULT:
                return (long) getVaultBalance(playerUuid);
            case PLAYER_POINTS:
                return getPlayerPointsBalance(playerUuid);
            case GUILD_COIN:
                return plugin.getGuildManager().getPlayerGuildCurrency(playerUuid);
            default:
                return 0L;
        }
    }

    // ========== Vault 实现（纯反射）==========

    private boolean withdrawFromVault(UUID playerUuid, long amount) {
        if (!vaultAvailable || vaultEconomy == null || vaultHasMethod == null) return false;
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
            Boolean has = (Boolean) vaultHasMethod.invoke(vaultEconomy, offlinePlayer, (double) amount);
            if (!has.booleanValue()) return false;
            Object result = vaultWithdrawMethod.invoke(vaultEconomy, offlinePlayer, (double) amount);
            // EconomyResponse.transactionSuccess()
            return (Boolean) result.getClass().getMethod("transactionSuccess").invoke(result);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean depositToVault(UUID playerUuid, long amount) {
        if (!vaultAvailable || vaultEconomy == null || vaultDepositMethod == null) return false;
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
            Object result = vaultDepositMethod.invoke(vaultEconomy, offlinePlayer, (double) amount);
            return (Boolean) result.getClass().getMethod("transactionSuccess").invoke(result);
        } catch (Exception e) {
            return false;
        }
    }

    private double getVaultBalance(UUID playerUuid) {
        if (!vaultAvailable || vaultEconomy == null || vaultGetBalanceMethod == null) return 0D;
        try {
            return (Double) vaultGetBalanceMethod.invoke(vaultEconomy,
                    Bukkit.getOfflinePlayer(playerUuid));
        } catch (Exception e) {
            return 0D;
        }
    }

    // ========== PlayerPoints 实现（带缓存） ==========

    private boolean givePlayerPoints(UUID playerUuid, int amount) {
        if (!playerPointsEnabled || ppGiveMethod == null) return false;
        try {
            Object ppApi = getPlayerPointsApi();
            ppGiveMethod.invoke(ppApi, playerUuid, amount);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean takeFromPlayerPoints(UUID playerUuid, int amount) {
        if (!playerPointsEnabled || ppTakeMethod == null) return false;
        try {
            Object ppApi = getPlayerPointsApi();
            int current = (Integer) ppGetMethod.invoke(ppApi, playerUuid);
            if (current < amount) return false;
            ppTakeMethod.invoke(ppApi, playerUuid, amount);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private long getPlayerPointsBalance(UUID playerUuid) {
        if (!playerPointsEnabled || ppGetMethod == null) return 0L;
        try {
            Object ppApi = getPlayerPointsApi();
            return (Integer) ppGetMethod.invoke(ppApi, playerUuid);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 获取缓存的 PlayerPoints API 实例
     * 优化：使用启动时缓存的实例，避免每次调用都查询 ServiceManager（反射开销）
     */
    private Object getPlayerPointsApi() throws Exception {
        if (cachedPlayerPointsApi == null) {
            throw new IllegalStateException("PlayerPoints API not initialized");
        }
        return cachedPlayerPointsApi;
    }

    // ========== 状态查询 ==========

    public boolean isVaultAvailable() { return vaultAvailable; }

    public boolean isPlayerPointsAvailable() { return playerPointsEnabled; }

    // ========== 格式化 ==========

    public String formatAmount(long amount, CurrencyType type) {
        switch (type) {
            case VAULT:
                return String.format("%,.0f 金币", (double) amount);
            case PLAYER_POINTS:
                return amount + " 点数";
            case GUILD_COIN:
                return amount + " 公会币";
            default:
                return String.valueOf(amount);
        }
    }

    // ========== 枚举 ==========

    public enum CurrencyType {
        VAULT,
        PLAYER_POINTS,
        GUILD_COIN
    }
}
