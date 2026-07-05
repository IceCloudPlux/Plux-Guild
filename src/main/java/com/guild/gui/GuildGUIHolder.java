package com.guild.gui;

import org.bukkit.inventory.InventoryHolder;

/**
 * 公会 GUI 标识接口
 * <p>
 * 所有公会 GUI 的 Inventory 持有者都实现此接口，
 * 使 InventoryListener 能通过 instanceof 高效识别公会 GUI，
 * 无需遍历所有在线玩家获取标题（原方案 O(n) → O(1)）。
 */
public interface GuildGUIHolder extends InventoryHolder {

    /**
     * 获取 GUI 类型标识
     *
     * @return GUI 类型字符串，用于区分不同界面
     */
    String getGuiType();
}
