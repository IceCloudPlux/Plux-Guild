package com.guild.gui;
import com.guild.GuildPlugin;
import com.guild.guild.Guild;
import com.guild.utils.VersionCompat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GuildShopGUI
{
    private static final ShopItem[] SHOP_ITEMS =
    {
        new ShopItem(1, VersionCompat.getGoldIngotMaterial(), "&e初级资源包", new String[]
        {
            " &7等级要求: 1", "&7价格: 1000 公会币", "&a点击购买"
        }, " shop:level1:resource_pack"), new ShopItem(5, VersionCompat.getDiamondMaterial(), "&b中级工具包", new String[]
        {
            " &7等级要求: 5", "&7价格: 5000 公会币", "&a点击购买"
        }, " shop:level5:tool_kit"), new ShopItem(10, VersionCompat.getEmeraldMaterial(), "&a高级装备包", new String[]
        {
            " &7等级要求: 10", "&7价格: 10000 公会币", "&a点击购买"
        }, " shop:level10:armor_set"), new ShopItem(20, VersionCompat.getExperienceBottleMaterial(), "&5经验卷轴", new String[]
        {
            " &7等级要求: 20", "&7价格: 20000 公会币", "&a点击购买"
        }, " shop:level20:exp_scroll"), new ShopItem(30, VersionCompat.getGoldenHelmetMaterial(), "&6传说头盔", new String[]
        {
            " &7等级要求: 30", "&7价格: 50000 公会币", "&a点击购买"
        }, " shop:level30:legendary_helmet"), new ShopItem(50, VersionCompat.getCommandBlockMaterial(), "&c公会技能书", new String[]
        {
            " &7等级要求: 50", "&7价格: 100000 公会币", "&a点击购买"
        }, " shop:level50:guild_skill"),
    };

    public static void openShopGUI(GuildPlugin plugin, Player player, Guild guild)
    {
        int level = guild.getLevel();
        Inventory inv = Bukkit.createInventory( new SimpleGuildGUIHolder("shop"), 54, GuiBuilder.color("&6&l公会商店 - 等级 " + level));
        GuiBuilder.drawBorder(inv, 6, GuiBuilder.DECORATION_MATERIAL);
        int slot = 10;
        for (ShopItem item : SHOP_ITEMS)
        {
            if (slot >= 44) break;
            if (slot % 9 == 8)
            {
                slot += 2;
            }
            if (slot >= 44) break;
            if (level >= item.requiredLevel)
            {
                inv.setItem(slot, GuiBuilder.createItem( item.material, item.name, item.lore));
            }
            else
            {
                inv.setItem(slot, GuiBuilder.createItem( VersionCompat.getGrayStainedGlassPaneMaterial(), GuiBuilder.COLOR_INFO + "[锁定]", GuiBuilder.COLOR_DANGER + "需要等级: " + item.requiredLevel, "", GuiBuilder.COLOR_INFO + "公会达到 Lv." + item.requiredLevel + " 后解锁"));
            }
            slot++;
            if (slot % 9 == 8) slot++;
        }
        inv.setItem(49, GuiBuilder.backButton());
        player.openInventory(inv);
    }

    public static ShopItem findShopItem(String itemName)
    {
        String stripped = ChatColor.stripColor(GuiBuilder.color(itemName));
        for (ShopItem item : SHOP_ITEMS)
        {
            if (ChatColor.stripColor(GuiBuilder.color(item.name)).equals(stripped))
            {
                return item;
            }}

            return null;
        }

        public static class ShopItem
        {
            final int requiredLevel;

            final Material material;

            final String name;

            final String[] lore;

            final String actionId;
            ShopItem(int requiredLevel, Material material, String name, String[] lore, String actionId)
            {
                this.requiredLevel = requiredLevel;
                this.material = material;
                this.name = name;
                this.lore = lore;
                this.actionId = actionId;
            }

            public int getRequiredLevel()
            {
                return requiredLevel;
            }

            public Material getMaterial()
            {
                return material;
            }

            public String getName()
            {
                return name;
            }

            public String[] getLore()
            {
                return lore;
            }

            public String getActionId()
            {
                return actionId;
            }}

        }