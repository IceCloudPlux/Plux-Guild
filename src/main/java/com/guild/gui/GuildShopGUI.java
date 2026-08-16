package com.guild.gui;
import com.guild.GuildPlugin;
import com.guild.config.ShopConfig;
import com.guild.guild.Guild;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

public class GuildShopGUI
{
    public static void openShopGUI(GuildPlugin plugin, Player player, Guild guild)
    {
        ShopConfig shopConfig = plugin.getShopConfig();
        if (shopConfig == null || !shopConfig.isEnabled())
        {
            player.sendMessage(ChatColor.RED + "公会商店未启用");
            return;
        }
        int level = guild.getLevel();
        int rows = shopConfig.getRows();
        String title = ChatColor.translateAlternateColorCodes('&',
            shopConfig.getTitle().replace("%level%", String.valueOf(level)));
        Inventory inv = Bukkit.createInventory(new SimpleGuildGUIHolder("shop"), rows * 9, title);
        GuiBuilder.drawBorder(inv, rows, GuiBuilder.DECORATION_MATERIAL);
        Material lockedMaterial = GuiBuilder.BORDER_MATERIAL;
        for (ShopConfig.ShopItemEntry item : shopConfig.getItems())
        {
            int slot = item.getSlot();
            if (slot < 0 || slot >= rows * 9) continue;
            if (slot % 9 == 0 || slot % 9 == 8) continue;
            if (level >= item.getRequiredLevel())
            {
                inv.setItem(slot, createShopItem(item, level));
            }
            else if (shopConfig.isShowLockedItems())
            {
                inv.setItem(slot, createLockedItem(shopConfig, item));
            }
        }
        inv.setItem(rows * 9 - 5, GuiBuilder.backButton());
        player.openInventory(inv);
    }

    private static ItemStack createShopItem(ShopConfig.ShopItemEntry item, int guildLevel)
    {
        ItemStack stack = new ItemStack(item.getMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null)
        {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getName()));
            List<String> lore = new ArrayList<>();
            if (item.getLore() != null)
            {
                for (String line : item.getLore())
                {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack createLockedItem(ShopConfig shopConfig, ShopConfig.ShopItemEntry item)
    {
        Material lockedMat = item.getMaterial();
        ItemStack stack = new ItemStack(lockedMat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null)
        {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                shopConfig.getLockedDisplayName().replace("%level%", String.valueOf(item.getRequiredLevel()))));
            List<String> lore = new ArrayList<>();
            if (shopConfig.getLockedLore() != null)
            {
                for (String line : shopConfig.getLockedLore())
                {
                    lore.add(ChatColor.translateAlternateColorCodes('&',
                        line.replace("%level%", String.valueOf(item.getRequiredLevel()))));
                }
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
