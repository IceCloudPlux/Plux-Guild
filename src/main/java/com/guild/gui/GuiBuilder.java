package com.guild.gui;

import com.guild.utils.VersionCompat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * GUI 构建工具类
 * <p>
 * 提供统一的物品创建、边框填充、配色方案等功能，
 * 确保所有 GUI 界面风格一致且美观。
 */
public final class GuiBuilder {

    private GuiBuilder() {}

    // ========== 配色方案 ==========

    /** 主色调 - 金色（标题/高亮） */
    public static final String COLOR_PRIMARY = "&6";
    /** 次要色调 - 黄色（描述/次要信息） */
    public static final String COLOR_SECONDARY = "&e";
    /** 成功色 - 绿色（确认/正向操作） */
    public static final String COLOR_SUCCESS = "&a";
    /** 危险色 - 红色（警告/破坏性操作） */
    public static final String COLOR_DANGER = "&c";
    /** 信息色 - 灰色（说明文字） */
    public static final String COLOR_INFO = "&7";
    /** 特殊色 - 青色（特殊功能） */
    public static final String COLOR_SPECIAL = "&b";
    /** 传奇色 - 暗紫（高级内容） */
    public static final String COLOR_LEGENDARY = "&5";
    /** 强调色 - 黄色（操作提示） */
    public static final String COLOR_YELLOW = "&e";

    // ========== 边框材质 ==========

    /** 深灰染色玻璃板（边框填充） */
    public static final Material BORDER_MATERIAL = VersionCompat.getGrayStainedGlassPaneMaterial();
    /** 浅蓝染色玻璃板（装饰边框） */
    public static final Material DECORATION_MATERIAL = VersionCompat.getLightBlueStainedGlassPaneMaterial();
    /** 金色染色玻璃板（高级装饰） */
    public static final Material GOLD_BORDER_MATERIAL = VersionCompat.getGoldStainedGlassPaneMaterial();
    /** 紫色染色玻璃板（传奇装饰） */
    public static final Material PURPLE_BORDER_MATERIAL = VersionCompat.getPurpleStainedGlassPaneMaterial();

    // ========== 物品创建 ==========

    /**
     * 创建带名称和描述的物品
     *
     * @param material 物品类型
     * @param name     显示名称（支持 & 颜色代码）
     * @param lore     描述行（支持 & 颜色代码）
     * @return 配置好的 ItemStack
     */
    public static ItemStack createItem(Material material, String name, String... lore) {
        return createItem(material, name, Arrays.asList(lore));
    }

    /**
     * 创建带名称和描述的物品
     *
     * @param material 物品类型
     * @param name     显示名称
     * @param lore     描述列表
     * @return 配置好的 ItemStack
     */
    public static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(color(name));

        if (lore != null && !lore.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(color(line));
            }
            meta.setLore(coloredLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    /** 创建仅带名称的物品（无描述） */
    public static ItemStack createSimpleItem(Material material, String name) {
        return createItem(material, name, Collections.emptyList());
    }

    // ========== 边框填充 ==========

    /**
     * 用灰色玻璃板填充空槽位（全屏填充）
     *
     * @param inventory 目标背包
     * @param size      背包总大小
     */
    public static void fillEmpty(org.bukkit.inventory.Inventory inventory, int size) {
        ItemStack filler = createSimpleItem(BORDER_MATERIAL, " ");
        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    /**
     * 填充指定范围的槽位
     *
     * @param inventory 目标背包
     * @param start     起始槽位
     * @param end       结束槽位（包含）
     * @param material  填充材质
     */
    public static void fillRange(org.bukkit.inventory.Inventory inventory,
                                 int start, int end, Material material) {
        ItemStack filler = createSimpleItem(material, " ");
        for (int i = start; i <= end; i++) {
            if (i >= 0 && i < inventory.getSize()) {
                inventory.setItem(i, filler);
            }
        }
    }

    /**
     * 绘制矩形边框（适用于 9 列布局）
     * 填充第一行、最后一行、第一列、最后一列
     *
     * @param inventory 目标背包
     * @param rows      行数
     * @param material  边框材质
     */
    public static void drawBorder(org.bukkit.inventory.Inventory inventory,
                                  int rows, Material material) {
        int size = rows * 9;
        ItemStack border = createSimpleItem(material, " ");

        // 第一行和最后一行
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);                    // 顶行
            inventory.setItem(size - 9 + i, border);         // 底行
        }
        // 左右列
        for (int r = 1; r < rows - 1; r++) {
            inventory.setItem(r * 9, border);                 // 左列
            inventory.setItem(r * 9 + 8, border);             // 右列
        }
    }

    /**
     * 绘制分隔线（某一行全部填充）
     *
     * @param inventory 目标背包
     * @param row       行号（从 0 开始）
     * @param material  材质
     */
    public static void drawRow(org.bukkit.inventory.Inventory inventory,
                               int row, Material material) {
        ItemStack line = createSimpleItem(material, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(row * 9 + i, line);
        }
    }

    // ========== 快捷物品 ==========

    /** 返回按钮 */
    public static ItemStack backButton(String... extraLore) {
        List<String> lore = new ArrayList<>();
        lore.add(COLOR_INFO + "点击返回上一级界面");
        for (String s : extraLore) lore.add(s);
        return createItem(VersionCompat.getArrowMaterial(),
                COLOR_DANGER + "返回", lore);
    }

    /** 确认按钮（绿色） */
    public static ItemStack confirmButton(String name, String... lore) {
        return createItem(VersionCompat.getLimeStainedGlassPaneMaterial(),
                COLOR_SUCCESS + name, lore);
    }

    /** 取消按钮（红色） */
    public static ItemStack cancelButton(String name, String... lore) {
        return createItem(VersionCompat.getRedStainedGlassPaneMaterial(),
                COLOR_DANGER + name, lore);
    }

    /** 信息按钮（蓝色） */
    public static ItemStack infoButton(String name, String... lore) {
        return createItem(VersionCompat.getPaperMaterial(),
                COLOR_SPECIAL + name, lore);
    }

    /** 成员列表按钮（玩家头颅） */
    public static ItemStack memberListButton(String... lore) {
        List<String> l = new ArrayList<>();
        l.add(COLOR_INFO + "查看公会成员列表");
        for (String s : lore) l.add(s);
        return createItem(VersionCompat.getPlayerHeadMaterial(),
                COLOR_SECONDARY + "成员列表", l);
    }

    /** 银行按钮 */
    public static ItemStack bankButton(String... lore) {
        List<String> l = new ArrayList<>();
        l.add(COLOR_INFO + "管理公会银行资金");
        for (String s : lore) l.add(s);
        return createItem(VersionCompat.getGoldIngotMaterial(),
                COLOR_PRIMARY + "公会银行", l);
    }

    /** 商店按钮 */
    public static ItemStack shopButton(int level) {
        return createItem(VersionCompat.getChestMaterial(),
                COLOR_PRIMARY + "公会商店",
                COLOR_INFO + "等级专属商店",
                COLOR_INFO + "当前等级: " + COLOR_SECONDARY + level,
                COLOR_YELLOW + "点击查看商品");
    }

    /** 设置按钮 */
    public static ItemStack settingsButton() {
        return createItem(VersionCompat.getRedstoneMaterial(),
                COLOR_SECONDARY + "个人设置",
                COLOR_INFO + "修改你的个人偏好");
    }

    /** 离开按钮（危险操作） */
    public static ItemStack leaveButton() {
        return createItem(VersionCompat.getBarrierMaterial(),
                COLOR_DANGER + "离开公会",
                COLOR_INFO + "退出当前公会",
                COLOR_DANGER + "此操作不可撤销");
    }

    /** 升级按钮 */
    public static ItemStack upgradeButton(int level, String costStr) {
        return createItem(VersionCompat.getEmeraldMaterial(),
                COLOR_SUCCESS + "升级公会",
                COLOR_INFO + "当前等级: " + COLOR_SECONDARY + level,
                COLOR_INFO + "升级所需: " + costStr,
                COLOR_YELLOW + "点击升级公会");
    }

    /** 购买经验按钮 */
    public static ItemStack buyExpButton(String costStr, int expAmount) {
        return createItem(VersionCompat.getExperienceBottleMaterial(),
                COLOR_SPECIAL + "购买经验",
                COLOR_INFO + "获得: " + COLOR_SECONDARY + expAmount + " 经验",
                COLOR_INFO + "所需: " + costStr,
                COLOR_YELLOW + "点击购买经验");
    }

    /** 管理按钮（管理员+会长可见） */
    public static ItemStack manageButton() {
        return createItem(VersionCompat.getCommandBlockMaterial(),
                COLOR_DANGER + "管理公会",
                COLOR_INFO + "重命名/改标签/解散");
    }

    /** 创建公会按钮 */
    public static ItemStack createGuildButton() {
        return createItem(VersionCompat.getDiamondMaterial(),
                COLOR_SUCCESS + "创建公会",
                COLOR_INFO + "建立属于你自己的公会",
                COLOR_YELLOW + "点击开始创建");
    }

    /** 查看所有公会按钮 */
    public static ItemStack viewAllGuildsButton() {
        return createItem(VersionCompat.getBookMaterial(),
                COLOR_SECONDARY + "查看所有公会",
                COLOR_INFO + "浏览服务器上的所有公会",
                COLOR_YELLOW + "点击浏览");
    }

    /** 无公会提示物品 */
    public static ItemStack noGuildBarrier() {
        return createItem(VersionCompat.getBarrierMaterial(),
                COLOR_DANGER + "你还没有公会",
                COLOR_INFO + "选择下方选项开始游戏");
    }

    // ========== 工具方法 ==========

    /** 将 & 颜色代码转换为 Minecraft 颜色字符 */
    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text != null ? text : "");
    }

    /** 将 & 颜色代码转换并去除末尾空白 */
    public static String colorTrim(String text) {
        return color(text).trim();
    }

    /**
     * 绘制双层边框（外边框 + 内边框）
     */
    public static void drawDoubleBorder(org.bukkit.inventory.Inventory inventory,
                                        int rows, Material outer, Material inner) {
        int size = rows * 9;
        ItemStack outerBorder = createSimpleItem(outer, " ");
        ItemStack innerBorder = createSimpleItem(inner, " ");

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, outerBorder);
            inventory.setItem(size - 9 + i, outerBorder);
            if (rows > 2) {
                inventory.setItem(9 + i, innerBorder);
                inventory.setItem(size - 18 + i, innerBorder);
            }
        }
        
        for (int r = 1; r < rows - 1; r++) {
            inventory.setItem(r * 9, outerBorder);
            inventory.setItem(r * 9 + 8, outerBorder);
            if (rows > 2 && r > 1 && r < rows - 2) {
                inventory.setItem(r * 9 + 1, innerBorder);
                inventory.setItem(r * 9 + 7, innerBorder);
            }
        }
    }

    /**
     * 绘制渐变分隔线（两种颜色交替）
     */
    public static void drawGradientLine(org.bukkit.inventory.Inventory inventory,
                                        int row, Material material1, Material material2) {
        ItemStack item1 = createSimpleItem(material1, " ");
        ItemStack item2 = createSimpleItem(material2, " ");
        
        for (int i = 0; i < 9; i++) {
            inventory.setItem(row * 9 + i, (i % 2 == 0) ? item1 : item2);
        }
    }

    /** 会员等级按钮 */
    public static ItemStack memberLevelButton(String name, int level, int maxLevel, String... lore) {
        List<String> l = new ArrayList<>();
        l.add(COLOR_INFO + "等级: " + COLOR_SECONDARY + level + "/" + maxLevel);
        for (String s : lore) l.add(s);
        
        Material material;
        if (level >= maxLevel) {
            material = VersionCompat.getNetherStarMaterial();
        } else if (level >= maxLevel * 0.7) {
            material = VersionCompat.getDiamondMaterial();
        } else if (level >= maxLevel * 0.4) {
            material = VersionCompat.getGoldIngotMaterial();
        } else {
            material = VersionCompat.getIronIngotMaterial();
        }
        
        return createItem(material, COLOR_PRIMARY + name, l);
    }

    /** 贡献按钮 */
    public static ItemStack contributionButton(long contribution) {
        return createItem(VersionCompat.getEmeraldMaterial(),
                COLOR_SUCCESS + "贡献值",
                COLOR_INFO + "当前贡献: " + COLOR_SECONDARY + contribution,
                COLOR_YELLOW + "参与公会活动获得贡献");
    }

    /** 公会 MOTD 按钮 */
    public static ItemStack motdButton(String motd) {
        return createItem(VersionCompat.getPaperMaterial(),
                COLOR_SPECIAL + "公会公告",
                COLOR_INFO + (motd != null && !motd.isEmpty() ? motd : "暂无公告"));
    }

    /** 经验进度条按钮 */
    public static ItemStack expProgressButton(long current, long required, int level) {
        double percentage = (double) current / required * 100;
        String progress = "";
        for (int i = 0; i < 10; i++) {
            progress += (i * 10 < percentage) ? "\u2588" : "\u2591";
        }
        
        return createItem(VersionCompat.getExperienceBottleMaterial(),
                COLOR_PRIMARY + "经验进度",
                COLOR_INFO + "等级: " + COLOR_SECONDARY + level,
                COLOR_YELLOW + progress,
                COLOR_INFO + current + " / " + required);
    }
}
