package com.guild.guild;

import java.util.HashMap;
import java.util.Map;

public class GuildBank {
    private long balance = 0L;
    private Map<String, Map<Long, Long>> depositHistory = new HashMap<String, Map<Long, Long>>();
    private Map<String, Map<Long, Long>> withdrawHistory = new HashMap<String, Map<Long, Long>>();

    public long getBalance() {
        return this.balance;
    }

    public void setBalance(long l) {
        this.balance = l;
    }

    public boolean deposit(long l) {
        if (l <= 0L) {
            return false;
        }
        this.balance += l;
        return true;
    }

    public boolean withdraw(long l) {
        if (l <= 0L) {
            return false;
        }
        if (this.balance < l) {
            return false;
        }
        this.balance -= l;
        return true;
    }

    public void addDepositRecord(String string2, long l) {
        this.depositHistory.computeIfAbsent(string2, string -> new HashMap()).put(System.currentTimeMillis(), l);
    }

    public void addWithdrawRecord(String string2, long l) {
        this.withdrawHistory.computeIfAbsent(string2, string -> new HashMap()).put(System.currentTimeMillis(), l);
    }

    public Map<String, Map<Long, Long>> getDepositHistory() {
        HashMap<String, Map<Long, Long>> hashMap = new HashMap<String, Map<Long, Long>>();
        this.depositHistory.forEach((string, map2) -> {
            Map cfr_ignored_0 = hashMap.put((String)string, new HashMap(map2));
        });
        return hashMap;
    }

    public Map<String, Map<Long, Long>> getWithdrawHistory() {
        HashMap<String, Map<Long, Long>> hashMap = new HashMap<String, Map<Long, Long>>();
        this.withdrawHistory.forEach((string, map2) -> {
            Map cfr_ignored_0 = hashMap.put((String)string, new HashMap(map2));
        });
        return hashMap;
    }

    public void clearHistory() {
        this.depositHistory.clear();
        this.withdrawHistory.clear();
    }
}

