package com.guild.guild;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class GuildBank
{
    private final AtomicLong balance = new AtomicLong(0L);
    private final Map<String, Map<Long, Long>> depositHistory = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, Long>> withdrawHistory = new ConcurrentHashMap<>();

    public long getBalance()
    {
        return balance.get();
    }

    public void setBalance(long amount)
    {
        balance.set(amount);
    }

    public boolean deposit(long amount)
    {
        if (amount <= 0L) return false;
        balance.addAndGet(amount);
        return true;
    }

    public boolean withdraw(long amount)
    {
        if (amount <= 0L) return false;
        while (true)
        {
            long current = balance.get();
            if (current < amount) return false;
            if (balance.compareAndSet(current, current - amount)) return true;
        }
    }

    public void addDepositRecord(String playerName, long amount)
    {
        depositHistory.computeIfAbsent(playerName, k -> new ConcurrentHashMap<>())
            .put(System.currentTimeMillis(), amount);
    }

    public void addWithdrawRecord(String playerName, long amount)
    {
        withdrawHistory.computeIfAbsent(playerName, k -> new ConcurrentHashMap<>())
            .put(System.currentTimeMillis(), amount);
    }

    public Map<String, Map<Long, Long>> getDepositHistory()
    {
        Map<String, Map<Long, Long>> result = new ConcurrentHashMap<>();
        depositHistory.forEach((name, records) ->
            result.put(name, new ConcurrentHashMap<>(records)));
        return result;
    }

    public Map<String, Map<Long, Long>> getWithdrawHistory()
    {
        Map<String, Map<Long, Long>> result = new ConcurrentHashMap<>();
        withdrawHistory.forEach((name, records) ->
            result.put(name, new ConcurrentHashMap<>(records)));
        return result;
    }

    public void clearHistory()
    {
        depositHistory.clear();
        withdrawHistory.clear();
    }
}
