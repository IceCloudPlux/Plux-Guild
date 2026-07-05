package com.guild.gui;

import org.bukkit.inventory.Inventory;

/**
 * 公会 GUI 持有者基类
 * <p>
 * 所有公会 GUI 共用此持有者，通过 guiType 区分界面类型。
 */
public final class SimpleGuildGUIHolder implements GuildGUIHolder {

    private final String guiType;

    public SimpleGuildGUIHolder(String guiType) {
        this.guiType = guiType;
    }

    @Override
    public String getGuiType() {
        return guiType;
    }

    @Override
    public Inventory getInventory() {
        return null; // Bukkit 内部管理
    }
}
