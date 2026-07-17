package com.guild.papi;

import com.guild.GuildPlugin;
import java.lang.reflect.Method;

public class GuildPlaceholderExpansion {
    private final GuildPlugin plugin;

    public GuildPlaceholderExpansion(GuildPlugin guildPlugin) {
        this.plugin = guildPlugin;
    }

    public boolean register() {
        try {
            plugin.getLogger().info("Attempting to register PAPI expansion...");
            
            try {
                Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
                plugin.getLogger().info("Found PAPI v2");
                return registerV2();
            } catch (ClassNotFoundException e1) {
                plugin.getLogger().info("PAPI v2 not found, trying v1...");
                try {
                    Class.forName("me.clip.placeholderapi.external.EZPlaceholderHook");
                    plugin.getLogger().info("Found PAPI v1");
                    return registerV1();
                } catch (ClassNotFoundException e2) {
                    plugin.getLogger().info("PlaceholderAPI not found");
                    return false;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("PAPI registration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean registerV2() {
        try {
            GuildPAPIExpansion expansion = new GuildPAPIExpansion(plugin);
            boolean result = expansion.register();
            if (result) {
                plugin.getLogger().info("PAPI v2 expansion registered successfully");
                return true;
            } else {
                plugin.getLogger().warning("PAPI v2 register() returned false");
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("PAPI v2 registration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean registerV1() {
        try {
            Class<?> hookClass = Class.forName("me.clip.placeholderapi.external.EZPlaceholderHook");
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            
            Method registerMethod = null;
            try {
                registerMethod = papiClass.getMethod("registerPlaceholderHook", String.class, hookClass);
            } catch (NoSuchMethodException e) {
                try {
                    registerMethod = papiClass.getMethod("registerPlaceholderHook", org.bukkit.plugin.Plugin.class, String.class, hookClass);
                } catch (NoSuchMethodException ex) {
                    plugin.getLogger().severe("Could not find PAPI v1 register method");
                    return false;
                }
            }
            
            Object result = null;
            try {
                Object hook = hookClass.getConstructor(String.class, String.class).newInstance("guild", "ya_xzer21145");
                result = registerMethod.invoke(null, "guild", hook);
            } catch (Exception e) {
                try {
                    Object hook = hookClass.getConstructor(String.class, String.class).newInstance("guild", "ya_xzer21145");
                    result = registerMethod.invoke(null, plugin, "guild", hook);
                } catch (Exception ex) {
                    plugin.getLogger().severe("PAPI v1 registration failed: " + ex.getMessage());
                    return false;
                }
            }
            
            plugin.getLogger().info("PAPI v1 expansion registered");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("PAPI v1 registration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
