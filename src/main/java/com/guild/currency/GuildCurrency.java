package com.guild.currency;
import com.guild.GuildPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.lang.reflect.Method;
import java.util.UUID;

public class GuildCurrency
{
    private final GuildPlugin plugin;
    private Object vaultEconomy;
    private Method vaultHasMethod;
    private Method vaultWithdrawMethod;
    private Method vaultDepositMethod;
    private Method vaultGetBalanceMethod;
    private boolean vaultInitialized = false;
    private boolean vaultAvailable = false;
    private boolean playerPointsEnabled = false;
    private static Method ppLookUpMethod;
    private static Method ppGetMethod;
    private static Method ppSetMethod;
    private static Method ppGiveMethod;
    private static Method ppTakeMethod;
    private static boolean ppMethodsInitialized = false;
    private static Object cachedPlayerPointsApi;

    public GuildCurrency(GuildPlugin plugin)
    {
        this.plugin = plugin;
        initVault();
        initPlayerPoints();
    }

    @SuppressWarnings("unchecked")
    private synchronized void initVault()
    {
        if (vaultInitialized) return;
        try
        {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null)
            {
                vaultAvailable = false;
                vaultInitialized = true;
                return;
            }
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Object registration = Bukkit.getServicesManager().getRegistration(economyClass);
            if (registration == null)
            {
                vaultAvailable = false;
                vaultInitialized = true;
                return;
            }
            Method getProvider = registration.getClass().getMethod("getProvider");
            vaultEconomy = getProvider.invoke(registration);
            if (vaultEconomy == null)
            {
                vaultAvailable = false;
                vaultInitialized = true;
                return;
            }
            Class<?> ecoClass = vaultEconomy.getClass();
            vaultHasMethod = ecoClass.getMethod("has", OfflinePlayer.class, double.class);
            vaultWithdrawMethod = ecoClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            vaultDepositMethod = ecoClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            vaultGetBalanceMethod = ecoClass.getMethod("getBalance", OfflinePlayer.class);
            vaultAvailable = true;
            plugin.getLogger().info("Vault economy initialized successfully");
        }
        catch (ClassNotFoundException e)
        {
            vaultAvailable = false;
        }
        catch (Exception e)
        {
            vaultAvailable = false;
            plugin.getLogger().warning("Vault initialization failed: " + e.getMessage());
        }
        vaultInitialized = true;
    }

    private synchronized void initPlayerPoints()
    {
        if (ppMethodsInitialized) return;
        try
        {
            Class<?> ppApiClass = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
            Object ppApi = Bukkit.getServicesManager().getRegistration(ppApiClass).getProvider();
            cachedPlayerPointsApi = ppApi;
            ppLookUpMethod = ppApiClass.getMethod("lookUpUUID", UUID.class);
            ppGetMethod = ppApiClass.getMethod("get", UUID.class);
            ppSetMethod = ppApiClass.getMethod("set", UUID.class, int.class);
            ppGiveMethod = ppApiClass.getMethod("give", UUID.class, int.class);
            ppTakeMethod = ppApiClass.getMethod("take", UUID.class, int.class);
            playerPointsEnabled = true;
            plugin.getLogger().info("PlayerPoints initialized successfully");
        }
        catch (Exception e)
        {
            playerPointsEnabled = false;
        }
        ppMethodsInitialized = true;
    }

    public boolean withdraw(UUID playerUuid, long amount, CurrencyType type)
    {
        if (amount <= 0) return false;
        switch (type)
        {
            case VAULT: return withdrawFromVault(playerUuid, amount);
            case PLAYER_POINTS: return takeFromPlayerPoints(playerUuid, (int) amount);
            case GUILD_COIN: return plugin.getGuildManager().withdrawPlayerGuildCurrency(playerUuid, amount);
            default: return false;
        }
    }

    public boolean deposit(UUID playerUuid, long amount, CurrencyType type)
    {
        if (amount <= 0) return false;
        switch (type)
        {
            case VAULT: return depositToVault(playerUuid, amount);
            case PLAYER_POINTS: return givePlayerPoints(playerUuid, (int) amount);
            case GUILD_COIN: return plugin.getGuildManager().depositPlayerGuildCurrency(playerUuid, amount);
            default: return false;
        }
    }

    public long getBalance(UUID playerUuid, CurrencyType type)
    {
        switch (type)
        {
            case VAULT: return (long) getVaultBalance(playerUuid);
            case PLAYER_POINTS: return getPlayerPointsBalance(playerUuid);
            case GUILD_COIN: return plugin.getGuildManager().getPlayerGuildCurrency(playerUuid);
            default: return 0L;
        }
    }

    private boolean withdrawFromVault(UUID playerUuid, long amount)
    {
        if (!vaultAvailable || vaultEconomy == null || vaultHasMethod == null) return false;
        try
        {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
            Boolean has = (Boolean) vaultHasMethod.invoke(vaultEconomy, offlinePlayer, (double) amount);
            if (has == null || !has.booleanValue()) return false;
            Object result = vaultWithdrawMethod.invoke(vaultEconomy, offlinePlayer, (double) amount);
            if (result != null)
            {
                return (Boolean) result.getClass().getMethod("transactionSuccess").invoke(result);
            }
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private boolean depositToVault(UUID playerUuid, long amount)
    {
        if (!vaultAvailable || vaultEconomy == null || vaultDepositMethod == null) return false;
        try
        {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
            Object result = vaultDepositMethod.invoke(vaultEconomy, offlinePlayer, (double) amount);
            if (result != null)
            {
                return (Boolean) result.getClass().getMethod("transactionSuccess").invoke(result);
            }
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private double getVaultBalance(UUID playerUuid)
    {
        if (!vaultAvailable || vaultEconomy == null || vaultGetBalanceMethod == null) return 0D;
        try
        {
            return (Double) vaultGetBalanceMethod.invoke(vaultEconomy, Bukkit.getOfflinePlayer(playerUuid));
        }
        catch (Exception e)
        {
            return 0D;
        }
    }

    private boolean givePlayerPoints(UUID playerUuid, int amount)
    {
        if (!playerPointsEnabled || ppGiveMethod == null) return false;
        try
        {
            Object ppApi = getPlayerPointsApi();
            ppGiveMethod.invoke(ppApi, playerUuid, amount);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private boolean takeFromPlayerPoints(UUID playerUuid, int amount)
    {
        if (!playerPointsEnabled || ppTakeMethod == null) return false;
        try
        {
            Object ppApi = getPlayerPointsApi();
            int current = (Integer) ppGetMethod.invoke(ppApi, playerUuid);
            if (current < amount) return false;
            ppTakeMethod.invoke(ppApi, playerUuid, amount);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private long getPlayerPointsBalance(UUID playerUuid)
    {
        if (!playerPointsEnabled || ppGetMethod == null) return 0L;
        try
        {
            Object ppApi = getPlayerPointsApi();
            return (Integer) ppGetMethod.invoke(ppApi, playerUuid);
        }
        catch (Exception e)
        {
            return 0L;
        }
    }

    private Object getPlayerPointsApi() throws Exception
    {
        if (cachedPlayerPointsApi == null)
        {
            throw new IllegalStateException("PlayerPoints API not initialized");
        }
        return cachedPlayerPointsApi;
    }

    public boolean isVaultAvailable()
    {
        return vaultAvailable;
    }

    public boolean isPlayerPointsAvailable()
    {
        return playerPointsEnabled;
    }

    public String formatAmount(long amount, CurrencyType type)
    {
        switch (type)
        {
            case VAULT: return String.format("%,.0f 金币", (double) amount);
            case PLAYER_POINTS: return amount + " 点数";
            case GUILD_COIN: return amount + " 公会币";
            default: return String.valueOf(amount);
        }
    }

    public enum CurrencyType
    {
        VAULT, PLAYER_POINTS, GUILD_COIN
    }
}
