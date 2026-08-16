package com.guild.config;
import com.guild.GuildPlugin;

public class BankConfig extends SimpleFileConfig
{
    public BankConfig(GuildPlugin plugin)
    {
        super(plugin, "bank.yml");
    }

    public long getMaxDeposit() { return config.getLong("max-deposit", 0L); }
    public long getMaxWithdraw() { return config.getLong("max-withdraw", 0L); }
    public long getMaxBalance() { return config.getLong("max-balance", 0L); }
    public long getMinBalance() { return config.getLong("min-balance", 0L); }
    public double getDepositTax() { return config.getDouble("deposit-tax", 0.0); }
    public double getWithdrawTax() { return config.getDouble("withdraw-tax", 0.0); }
    public String getWithdrawPermission() { return config.getString("withdraw-permission", "OFFICER"); }
    public String getDepositPermission() { return config.getString("deposit-permission", "MEMBER"); }
    public long getDailyDepositLimit() { return config.getLong("daily-deposit-limit", 0L); }
    public long getDailyWithdrawLimit() { return config.getLong("daily-withdraw-limit", 0L); }
    public boolean isInterestEnabled() { return config.getBoolean("interest.enabled", false); }
    public double getInterestRate() { return config.getDouble("interest.rate", 0.01); }
    public String getInterestTime() { return config.getString("interest.time", "04:00"); }
    public long getMaxInterest() { return config.getLong("interest.max-interest", 0L); }
    public int getMaxHistoryRecords() { return config.getInt("max-history-records", 50); }
    public boolean isLogLargeTransactions() { return config.getBoolean("log-large-transactions", true); }
    public long getLargeTransactionThreshold() { return config.getLong("large-transaction-threshold", 50000L); }
}
