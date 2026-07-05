package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.guild.Guild;
import com.guild.utils.VersionCompat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * 公会管理 GUI
 * <p>
 * 3行(27格)界面，提供重命名、改标签、解散操作。
 * 仅会长和管理员可访问。
 */
public class GuildManageGUI {

    public static void openGUI(GuildPlugin plugin, Player player, Guild guild) {
        Inventory inv = Bukkit.createInventory(new SimpleGuildGUIHolder("manage"), 27,
                GuiBuilder.color("&6&l管理公会: &e" + guild.getName()));

        // 边框
        GuiBuilder.drawBorder(inv, 3, GuiBuilder.DECORATION_MATERIAL);

        // 重命名公会
        inv.setItem(11, GuiBuilder.createItem(
                VersionCompat.getNameTagMaterial(),
                GuiBuilder.COLOR_SECONDARY + "重命名公会",
                GuiBuilder.COLOR_INFO + "修改公会的显示名称",
                "",
                GuiBuilder.COLOR_INFO + "点击后在聊天栏输入新名称",
                GuiBuilder.COLOR_INFO + "1分钟内有效，输入 C 取消"));

        // 更改标签
        inv.setItem(13, GuiBuilder.createItem(
                VersionCompat.getAnvilMaterial(),
                GuiBuilder.COLOR_SECONDARY + "更改标签",
                GuiBuilder.COLOR_INFO + "修改公会的简称标签",
                "",
                GuiBuilder.COLOR_INFO + "点击后在聊天栏输入新标签",
                GuiBuilder.COLOR_INFO + "1分钟内有效，输入 C 取消"));

        // 解散公会（危险操作）
        inv.setItem(15, GuiBuilder.createItem(
                VersionCompat.getBarrierMaterial(),
                GuiBuilder.COLOR_DANGER + "解散公会",
                GuiBuilder.COLOR_INFO + "永久删除此公会",
                "",
                GuiBuilder.COLOR_DANGER + "警告: 此操作不可撤销!",
                GuiBuilder.COLOR_DANGER + "需要二次确认"));

        // 返回按钮
        inv.setItem(22, GuiBuilder.backButton());

        player.openInventory(inv);
    }
}
