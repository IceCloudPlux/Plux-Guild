package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.guild.Guild;
import com.guild.utils.VersionCompat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 公会银行 GUI
 * <p>
 * 5行(45格)界面，提供余额查看、存入、取出功能。
 */
public class GuildBankGUI {

    public static void openBankGUI(GuildPlugin plugin, Player player, Guild guild) {
        Inventory inv = Bukkit.createInventory(new SimpleGuildGUIHolder("bank"), 45,
                GuiBuilder.color("&6&l公会银行: &e" + guild.getName()));

        // 边框
        GuiBuilder.drawBorder(inv, 5, GuiBuilder.DECORATION_MATERIAL);

        long balance = guild.getBank().getBalance();

        // 中央：余额显示（大号展示）
        inv.setItem(13, GuiBuilder.createItem(
                VersionCompat.getGoldIngotMaterial(),
                GuiBuilder.COLOR_PRIMARY + "当前余额",
                GuiBuilder.COLOR_SECONDARY + "" + balance + " &7公会币",
                "",
                GuiBuilder.COLOR_INFO + "左键存入 | 右键取出"));

        // 存入按钮
        inv.setItem(20, GuiBuilder.createItem(
                VersionCompat.getEmeraldMaterial(),
                GuiBuilder.COLOR_SUCCESS + "存入资金",
                GuiBuilder.COLOR_INFO + "点击后输入金额",
                GuiBuilder.COLOR_INFO + "从你的账户转入公会"));

        // 取出按钮
        inv.setItem(24, GuiBuilder.createItem(
                VersionCompat.getRedstoneMaterial(),
                GuiBuilder.COLOR_DANGER + "取出资金",
                GuiBuilder.COLOR_INFO + "点击后输入金额",
                GuiBuilder.COLOR_INFO + "从公会银行转出",
                "",
                GuiBuilder.COLOR_DANGER + "需要管理员权限"));

        // 返回按钮
        inv.setItem(40, GuiBuilder.backButton());

        // 填充空位
        GuiBuilder.fillEmpty(inv, 45);

        player.openInventory(inv);
    }
}
