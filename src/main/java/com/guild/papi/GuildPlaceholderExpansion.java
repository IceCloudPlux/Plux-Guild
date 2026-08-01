package com.guild.papi;
import com.guild.GuildPlugin;

public class GuildPlaceholderExpansion
{
    private final GuildPlugin plugin;

    public GuildPlaceholderExpansion(GuildPlugin guildPlugin)
    {
        this.plugin = guildPlugin;
    }

    public boolean register()
    {
        try
        {
            plugin.getLogger().info("Attempting to register PAPI expansion...");
            try
            {
                Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
                plugin.getLogger().info("Found PAPI v2 (PlaceholderExpansion class)");
                return registerV2();
            }
            catch (ClassNotFoundException e1)
            {
                try
                {
                    Class.forName("me.clip.placeholderapi.external.EZPlaceholderHook");
                    plugin.getLogger().info("Found PAPI v1 (EZPlaceholderHook class)");
                    return registerV1();
                }
                catch (ClassNotFoundException e2)
                {
                    plugin.getLogger().info("PlaceholderAPI not found, placeholder integration disabled");
                    return false;
                }
            }
        }
        catch (Throwable e)
        {
            plugin.getLogger().severe("PAPI registration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean registerV2()
    {
        try
        {
            GuildPAPIExpansion expansion = new GuildPAPIExpansion(plugin);
            boolean result = expansion.register();
            if (result)
            {
                plugin.getLogger().info("PAPI v2 expansion registered (identifier: " + expansion.getIdentifier() + ", version: " + expansion.getVersion() + ")");
                return true;
            }
            else
            {
                plugin.getLogger().warning("PAPI v2 register() returned false, trying alternative...");
                return registerV2Alternative();
            }
        }
        catch (NoClassDefFoundError e)
        {
            plugin.getLogger().severe("PAPI v2 class def error: " + e.getMessage());
            return false;
        }
        catch (Throwable e)
        {
            plugin.getLogger().severe("PAPI v2 registration error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean registerV2Alternative()
    {
        try
        {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Class<?> expansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            java.lang.reflect.Method registerExpansion = papiClass.getMethod("registerExpansion", expansionClass);
            GuildPAPIExpansion expansion = new GuildPAPIExpansion(plugin);
            registerExpansion.invoke(null, expansion);
            plugin.getLogger().info("PAPI v2 expansion registered via alternative method");
            return true;
        }
        catch (Throwable e)
        {
            plugin.getLogger().severe("PAPI v2 alternative registration failed: " + e.getMessage());
            return false;
        }
    }

    private boolean registerV1()
    {
        try
        {
            Class<?> hookClass = Class.forName("me.clip.placeholderapi.external.EZPlaceholderHook");
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Constructor<?> ctor = hookClass.getDeclaredConstructor(String.class, String.class);
            ctor.setAccessible(true);
            Object hook = ctor.newInstance("guild", "ya_xzer21145");
            java.lang.reflect.Method onHook = hookClass.getDeclaredMethod("onPlaceholderHook", org.bukkit.entity.Player.class, String.class);
            onHook.setAccessible(true);
            try
            {
                java.lang.reflect.Method registerMethod = papiClass.getMethod("registerPlaceholderHook", String.class, hookClass);
                registerMethod.invoke(null, "guild", hook);
            }
            catch (NoSuchMethodException e)
            {
                try
                {
                    java.lang.reflect.Method registerMethod = papiClass.getMethod("registerPlaceholderHook",
                        org.bukkit.plugin.Plugin.class, String.class, hookClass);
                    registerMethod.invoke(null, plugin, "guild", hook);
                }
                catch (NoSuchMethodException ex)
                {
                    plugin.getLogger().severe("Could not find PAPI v1 register method");
                    return false;
                }
            }
            plugin.getLogger().info("PAPI v1 expansion registered (note: V1 has limited placeholder support)");
            return true;
        }
        catch (Throwable e)
        {
            plugin.getLogger().severe("PAPI v1 registration failed: " + e.getMessage());
            return false;
        }
    }
}
