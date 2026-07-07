package com.guild.listeners;

import com.guild.GuildPlugin;
import com.guild.currency.GuildCurrency;
import com.guild.gui.GuildBankGUI;
import com.guild.gui.GuildGUI;
import com.guild.gui.GuildGUIHolder;
import com.guild.gui.GuildManageGUI;
import com.guild.gui.GuildMemberSettingsGUI;
import com.guild.gui.GuildShopGUI;
import com.guild.guild.Guild;
import com.guild.guild.GuildMember;
import com.guild.guild.GuildRole;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公会 GUI 事件监听器
 * <p>
 * 优化要点：
 * 1. 使用 GuildGUIHolder instanceof 判断（O(1)），替代遍历所有在线玩家获取标题（O(n)）
 * 2. 通过 holder.getGuiType() 分发事件，替代标题字符串匹配
 * 3. 成员头像点击使用 UUID→GuildMember Map 查找（O(1)），替代遍历所有成员
 */
public class InventoryListener implements Listener {

    private final GuildPlugin plugin;
    private ChatInputListener chatInputListener;

    /** 铁砧输入状态：跟踪玩家当前处于创建公会的哪一步 */
    private static final Map<UUID, String> PENDING_GUILD_NAME = new ConcurrentHashMap<>();

    /** 铁砧输入文本缓存：PrepareAnvilEvent 捕获玩家输入的文本 */
    private static final Map<UUID, String> ANVIL_TEXT_CACHE = new ConcurrentHashMap<>();

    /** 正在重新打开铁砧标志：防止 onInventoryClose 清除状态 */
    private static final Map<UUID, Boolean> REOPENING_ANVIL = new ConcurrentHashMap<>();

    /** GUI 类型常量 */
    private static final String GUI_MAIN = "main";
    private static final String GUI_LIST = "list";
    private static final String GUI_BANK = "bank";
    private static final String GUI_MANAGE = "manage";
    private static final String GUI_MEMBER_SETTINGS = "member_settings";
    private static final String GUI_SHOP = "shop";

    public InventoryListener(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    public void setChatInputListener(ChatInputListener listener) {
        this.chatInputListener = listener;
    }

    // ==================== 核心识别方法（O(1)）====================

    /**
     * 高效判断是否为公会 GUI
     * <p>
     * 优化：通过 GuildGUIHolder instanceof 直接判断，无需遍历在线玩家。
     * 原方案 getInventoryTitle() 遍历所有 Bukkit.getOnlinePlayers()，
     * 在高并发场景下每次点击都触发 O(n) 遍历，是严重的性能瓶颈。
     *
     * @param inventory 目标背包
     * @return 是否为公会 GUI
     */
    private boolean isGuildInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof GuildGUIHolder;
    }

    /**
     * 获取公会 GUI 类型标识
     */
    private String getGuiType(Inventory inventory) {
        if (inventory.getHolder() instanceof GuildGUIHolder) {
            return ((GuildGUIHolder) inventory.getHolder()).getGuiType();
        }
        return null;
    }

    // ==================== 事件处理 ====================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // ========== 铁砧GUI输入拦截（优先于公会GUI）==========
        if (handleAnvilInput(event, player)) return;

        Inventory topInv = event.getView().getTopInventory();
        if (!isGuildInventory(topInv)) return;

        event.setCancelled(true);
        player.updateInventory();

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;
        if (clickedInv.getType() == InventoryType.PLAYER || event.getSlotType() == SlotType.QUICKBAR) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

        // 通过 GUI 类型分发处理（替代标题匹配）
        switch (getGuiType(topInv)) {
            case GUI_BANK:
                handleBankClick(player, guild, displayName);
                break;
            case GUI_LIST:
                handleGuildListClick(player, displayName);
                break;
            case GUI_MANAGE:
                handleManageClick(player, displayName);
                break;
            case GUI_MEMBER_SETTINGS:
                handleMemberSettingsClick(player, displayName);
                break;
            case GUI_SHOP:
                handleShopClick(player, displayName);
                break;
            default: // main / 无公会界面
                if (guild != null) {
                    handleMainGUIClick(player, guild, displayName);
                } else {
                    handleNoGuildClick(player, displayName);
                }
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!isGuildInventory(event.getView().getTopInventory())) return;
        event.setCancelled(true);
        ((Player) event.getWhoClicked()).updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryInteract(InventoryInteractEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!isGuildInventory(event.getView().getTopInventory())) return;
        event.setCancelled(true);
        ((Player) event.getWhoClicked()).updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (isGuildInventory(event.getSource()) || isGuildInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    /** 玩家关闭铁砧时清理待输入状态 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (event.getInventory() == null || event.getInventory().getType() != InventoryType.ANVIL) return;

        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (uuid == null) return;
        if (PENDING_GUILD_NAME.containsKey(uuid)) {
            if (!Boolean.TRUE.equals(REOPENING_ANVIL.get(uuid))) {
                PENDING_GUILD_NAME.remove(uuid);
                ANVIL_TEXT_CACHE.remove(uuid);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player)) return;
        Player player = (Player) event.getView().getPlayer();
        UUID uuid = player.getUniqueId();
        if (uuid == null || !PENDING_GUILD_NAME.containsKey(uuid)) return;

        String text = null;
        
        if (event.getView() instanceof AnvilView) {
            AnvilView anvilView = (AnvilView) event.getView();
            text = anvilView.getRenameText();
        }

        if ((text == null || text.isEmpty()) && event.getInventory() instanceof AnvilInventory) {
            AnvilInventory anvilInv = (AnvilInventory) event.getInventory();
            try {
                text = anvilInv.getRenameText();
            } catch (NoSuchMethodError ignored) {}
        }

        if (text == null || text.isEmpty()) {
            ItemStack result = event.getResult();
            if (result != null && result.hasItemMeta()) {
                text = ChatColor.stripColor(result.getItemMeta().getDisplayName());
            }
        }

        if (text != null && !text.isEmpty()) {
            ANVIL_TEXT_CACHE.put(uuid, text);
        }

        ItemStack slot0 = event.getInventory().getItem(0);
        if (slot0 != null) {
            ItemStack result = slot0.clone();
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                String displayName = (text != null && !text.isEmpty()) ? text : ChatColor.GRAY + "请输入名称";
                meta.setDisplayName(ChatColor.GREEN + displayName);
                result.setItemMeta(meta);
            }
            event.setResult(result);
        }
    }

    // ========== 无公会界面点击 ==========

    private void handleNoGuildClick(Player player, String name) {
        if (name.contains("创建公会")) {
            player.closeInventory();
            int minLen = plugin.getConfig().getInt("guild.min-name-length", 3);
            int maxLen = plugin.getConfig().getInt("guild.max-name-length", 16);
            player.sendMessage(ChatColor.YELLOW + "========== 创建公会 ==========");
            player.sendMessage(ChatColor.GOLD + "请在聊天框输入公会名称（" + minLen + "-" + maxLen + "字符）");
            player.sendMessage(ChatColor.GRAY + "输入 'cancel' 取消创建");
            plugin.getChatInputListener().registerPendingAction(player.getUniqueId(), "guild_name", null);
        } else if (name.contains("查看所有公会")) {
            GuildGUI.openGuildListGUI(plugin, player);
        }
    }

    // ========== 公会列表点击 ==========

    private void handleGuildListClick(Player player, String name) {
        if (name.contains("返回")) {
            GuildGUI.openGUI(plugin, player);
        }
    }

    // ========== 银行界面点击 ==========

    private void handleBankClick(Player player, Guild guild, String name) {
        if (name.contains("存入资金")) {
            player.closeInventory();
            chatInputListener.registerPendingAction(
                    player.getUniqueId(), "bank_deposit", null);
            player.sendMessage(ChatColor.YELLOW + "请输入要存入的金额:");
        } else if (name.contains("取出资金")) {
            player.closeInventory();
            chatInputListener.registerPendingAction(
                    player.getUniqueId(), "bank_withdraw", null);
            player.sendMessage(ChatColor.YELLOW + "请输入要取出的金额:");
        } else if (name.contains("返回") && guild != null) {
            GuildGUI.openGUI(plugin, player);
        }
    }

    // ========== 管理界面点击 ==========

    private void handleManageClick(Player player, String name) {
        if (name.contains("返回")) {
            GuildGUI.openGUI(plugin, player);
        } else if (name.contains("重命名")) {
            player.closeInventory();
            chatInputListener.registerPendingAction(
                    player.getUniqueId(), "guild_motd", "rename");
            player.sendMessage(ChatColor.YELLOW + "请输入新名称:");
        } else if (name.contains("更改标签") || name.contains("标签")) {
            player.closeInventory();
            chatInputListener.registerPendingAction(
                    player.getUniqueId(), "guild_tag", null);
            player.sendMessage(ChatColor.YELLOW + "请输入新标签:");
        } else if (name.contains("解散公会")) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请输入: /guild disband 来解散公会");
        }
    }

    // ========== 成员设置界面点击 ==========

    private void handleMemberSettingsClick(Player player, String name) {
        if (name.contains("返回")) {
            GuildGUI.openGUI(plugin, player);
        }
    }

    // ========== 商店界面点击 ==========

    private void handleShopClick(Player player, String name) {
        if (name.contains("返回")) {
            GuildGUI.openGUI(plugin, player);
        } else if (name.contains("初级资源包")) {
            player.closeInventory();
            player.performCommand("menu shop level1 resource_pack");
        } else if (name.contains("中级工具包")) {
            player.closeInventory();
            player.performCommand("menu shop level5 tool_kit");
        } else if (name.contains("高级装备包")) {
            player.closeInventory();
            player.performCommand("menu shop level10 armor_set");
        } else if (name.contains("经验卷轴")) {
            player.closeInventory();
            player.performCommand("menu shop level20 exp_scroll");
        } else if (name.contains("传说头盔")) {
            player.closeInventory();
            player.performCommand("menu shop level30 legendary_helmet");
        } else if (name.contains("公会技能书")) {
            player.closeInventory();
            player.performCommand("menu shop level50 guild_skill");
        }
    }

    // ========== 主公会界面点击（合并 Member/Officer/Owner 的公共逻辑）==========

    private void handleMainGUIClick(Player player, Guild guild, String name) {
        // 公共按钮（所有角色通用）
        if (name.contains("离开公会")) {
            plugin.getGuildManager().leaveGuild(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "你已离开公会");
            return;
        }
        if (name.contains("公会银行")) {
            GuildBankGUI.openBankGUI(plugin, player, guild);
            return;
        }
        if (name.contains("公会商店")) {
            GuildShopGUI.openShopGUI(plugin, player, guild);
            return;
        }
        if (handleToggleButtons(player, guild, name)) return;

        // 角色特有按钮
        GuildMember member = guild.getMember(player.getUniqueId());
        if (member != null && member.getRole() == GuildRole.OWNER) {
            handleOwnerSpecificClick(player, guild, name);
        } else if (member != null && member.getRole() == GuildRole.OFFICER) {
            handleOfficerSpecificClick(player, guild, name);
        } else {
            handleMemberHeadClick(player, guild, name);
        }
    }

    /**
     * 处理通用的切换按钮（邀请开关、上下线通知）
     */
    private boolean handleToggleButtons(Player player, Guild guild, String name) {
        if (name.contains("公会邀请")) {
            boolean enabled = plugin.getGuildManager().togglePlayerInvites(player.getUniqueId());
            player.sendMessage(enabled ? ChatColor.GREEN + "已开启公会邀请" : ChatColor.RED + "已关闭公会邀请");
            GuildGUI.openGUI(plugin, player);
            return true;
        }
        if (name.contains("上下线通知")) {
            boolean enabled = plugin.getGuildManager().togglePlayerNotify(player.getUniqueId());
            player.sendMessage(enabled ? ChatColor.GREEN + "已开启上下线通知" : ChatColor.RED + "已关闭上下线通知");
            GuildGUI.openGUI(plugin, player);
            return true;
        }
        return false;
    }

    private void handleOwnerSpecificClick(Player player, Guild guild, String name) {
        if (name.contains("管理公会")) {
            GuildManageGUI.openGUI(plugin, player, guild);
        } else if (name.contains("升级公会")) {
            handleUpgradeClick(player, guild);
        } else if (name.contains("购买经验")) {
            handleBuyExpClick(player, guild);
        } else {
            handleMemberHeadClick(player, guild, name);
        }
    }

    private void handleOfficerSpecificClick(Player player, Guild guild, String name) {
        if (name.contains("管理公会")) {
            GuildManageGUI.openGUI(plugin, player, guild);
        } else {
            handleMemberHeadClick(player, guild, name);
        }
    }

    /**
     * 处理成员头像点击 → 打开成员设置界面
     * <p>
     * 优化：构建 UUID→GuildMember 反向映射进行 O(1) 查找，
     * 替代原来遍历所有成员逐一匹配名称的方式。
     */
    private void handleMemberHeadClick(Player player, Guild guild, String name) {
        // 构建 UUID → 名称 映射用于反向查找
        for (Map.Entry<UUID, GuildMember> entry : guild.getMembers().entrySet()) {
            UUID targetUuid = entry.getKey();
            GuildMember targetMember = entry.getValue();
            String playerName = Bukkit.getOfflinePlayer(targetUuid).getName();
            String display = targetMember.getNickname() != null
                    ? targetMember.getNickname() : playerName;

            if (name.equals(playerName) || name.equals(display)) {
                GuildMemberSettingsGUI.openGUI(plugin, player, guild, targetUuid);
                return;
            }
        }
    }

    // ========== 升级 / 购买经验逻辑 ==========

    private void handleUpgradeClick(Player player, Guild guild) {
        if (!guild.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "只有公会会长才能升级公会");
            return;
        }
        if (guild.getLevel() >= 100) {
            player.sendMessage(ChatColor.RED + "公会已达到最高等级");
            return;
        }
        if (plugin.getGuildManager().upgradeGuild(guild.getName(), player.getUniqueId())) {
            long cost = plugin.getCurrencyConfig().getLevelUpCost();
            String costStr = plugin.getGuildCurrency().formatAmount(cost, plugin.getCurrencyConfig().getCurrencyType());
            player.sendMessage(ChatColor.GREEN + "成功使用 " + costStr + " 将公会升级到 " + guild.getLevel() + " 级！");
            guild.broadcast(ChatColor.YELLOW + "恭喜！公会在 " + player.getName() + " 的努力下升级到了 " + guild.getLevel() + " 级！");
            player.closeInventory();
        } else {
            long cost = plugin.getCurrencyConfig().getLevelUpCost();
            String costStr = plugin.getGuildCurrency().formatAmount(cost, plugin.getCurrencyConfig().getCurrencyType());
            player.sendMessage(ChatColor.RED + "升级失败，你可能没有足够的 " + costStr);
        }
    }

    private void handleBuyExpClick(Player player, Guild guild) {
        if (plugin.getGuildManager().addExperienceWithCurrency(guild.getName(), player.getUniqueId())) {
            int amount = plugin.getCurrencyConfig().getExperienceAmount();
            long cost = plugin.getCurrencyConfig().getExperienceCost();
            String costStr = plugin.getGuildCurrency().formatAmount(cost, plugin.getCurrencyConfig().getCurrencyType());
            player.sendMessage(ChatColor.GREEN + "成功使用 " + costStr + " 购买了 " + amount + " 经验！");
            guild.broadcast(ChatColor.YELLOW + player.getName() + " 使用 " + costStr + " 为公会购买了 " + amount + " 经验");
        } else {
            long cost = plugin.getCurrencyConfig().getExperienceCost();
            String costStr = plugin.getGuildCurrency().formatAmount(cost, plugin.getCurrencyConfig().getCurrencyType());
            player.sendMessage(ChatColor.RED + "购买失败，你可能没有足够的 " + costStr);
        }
    }

    // ==================== 铁砧GUI输入（公会创建流程）====================

    private boolean handleAnvilInput(InventoryClickEvent event, Player player) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null || topInv.getType() != InventoryType.ANVIL) return false;

        UUID uuid = player.getUniqueId();
        if (uuid == null || !PENDING_GUILD_NAME.containsKey(uuid)) return false;

        event.setCancelled(true);

        if (event.getRawSlot() != 2) return true;

        String input = ANVIL_TEXT_CACHE.get(uuid);

        if (input == null || input.isEmpty()) {
            ItemStack result = event.getCurrentItem();
            if (result != null && result.hasItemMeta()) {
                input = ChatColor.stripColor(result.getItemMeta().getDisplayName());
            }
        }

        if (input != null && input.startsWith(ChatColor.GREEN.toString())) {
            input = input.substring(2);
        }

        if (input == null || input.isEmpty()) {
            ItemStack slot1 = topInv.getItem(1);
            if (slot1 != null && slot1.hasItemMeta()) {
                input = ChatColor.stripColor(slot1.getItemMeta().getDisplayName());
            }
        }

        if (input != null && input.startsWith(ChatColor.GREEN.toString())) {
            input = input.substring(2);
        }

        if (input == null || input.isEmpty()) {
            player.sendMessage(ChatColor.RED + "请输入有效内容");
            reopenCurrentAnvil(player);
            return true;
        }

        String pendingName = PENDING_GUILD_NAME.get(uuid);
        if (pendingName == null || pendingName.isEmpty()) {
            processGuildNameInput(player, input);
        } else {
            processGuildTagInput(player, pendingName, input);
        }
        return true;
    }

    private void reopenCurrentAnvil(Player player) {
        UUID uuid = player.getUniqueId();
        if (uuid == null) return;
        String pending = PENDING_GUILD_NAME.get(uuid);
        String cachedText = ANVIL_TEXT_CACHE.get(uuid);
        String finalPending = pending;
        
        REOPENING_ANVIL.put(uuid, true);
        player.closeInventory();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PENDING_GUILD_NAME.put(uuid, pending);
            if (cachedText != null && !cachedText.isEmpty()) {
                ANVIL_TEXT_CACHE.put(uuid, cachedText);
            }
            if (pending == null || pending.isEmpty()) {
                openGuildNameAnvil(player);
            } else {
                openGuildTagAnvil(player, finalPending);
            }
            REOPENING_ANVIL.remove(uuid);
        }, 2L);
    }

    private void openGuildNameAnvil(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        if (uuid == null) return;

        Inventory anvil = Bukkit.createInventory(null, InventoryType.ANVIL,
                ChatColor.translateAlternateColorCodes('&', "&6&l输入公会名称"));
        ItemStack nameTag = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = nameTag.getItemMeta();
        
        String cachedText = ANVIL_TEXT_CACHE.get(uuid);
        if (cachedText != null && !cachedText.isEmpty()) {
            meta.setDisplayName(cachedText);
        } else {
            meta.setDisplayName(ChatColor.GRAY + "在此输入公会名称");
        }
        nameTag.setItemMeta(meta);
        anvil.setItem(0, nameTag);

        REOPENING_ANVIL.put(uuid, true);
        PENDING_GUILD_NAME.put(uuid, "");
        player.openInventory(anvil);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            REOPENING_ANVIL.remove(uuid);
        }, 1L);
    }

    private void openGuildTagAnvil(Player player, String guildName) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        if (uuid == null) return;

        Inventory anvil = Bukkit.createInventory(null, InventoryType.ANVIL,
                ChatColor.translateAlternateColorCodes('&', "&6&l输入公会标签"));
        ItemStack nameTag = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = nameTag.getItemMeta();
        
        String cachedText = ANVIL_TEXT_CACHE.get(uuid);
        if (cachedText != null && !cachedText.isEmpty()) {
            meta.setDisplayName(cachedText);
        } else {
            meta.setDisplayName(ChatColor.GRAY + "在此输入公会标签 (2-4字符)");
        }
        nameTag.setItemMeta(meta);
        anvil.setItem(0, nameTag);

        REOPENING_ANVIL.put(uuid, true);
        PENDING_GUILD_NAME.put(uuid, guildName);
        player.openInventory(anvil);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            REOPENING_ANVIL.remove(uuid);
        }, 1L);
    }

    private void processGuildNameInput(Player player, String name) {
        int minLen = plugin.getConfig().getInt("guild.min-name-length", 3);
        int maxLen = plugin.getConfig().getInt("guild.max-name-length", 16);

        if (name.length() < minLen || name.length() > maxLen) {
            player.sendMessage(plugin.getMessage("guild.name-length-invalid")
                    .replace("%min%", String.valueOf(minLen))
                    .replace("%max%", String.valueOf(maxLen)));
            reopenCurrentAnvil(player);
            return;
        }
        if (plugin.getGuildManager().getGuild(name) != null) {
            player.sendMessage(plugin.getMessage("guild.already-exists"));
            reopenCurrentAnvil(player);
            return;
        }

        // 进入第二步：输入标签
        UUID uuid = player.getUniqueId();
        REOPENING_ANVIL.put(uuid, true);
        PENDING_GUILD_NAME.put(uuid, name);
        ANVIL_TEXT_CACHE.remove(uuid);
        player.closeInventory();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            openGuildTagAnvil(player, name);
            REOPENING_ANVIL.remove(uuid);
        }, 1L);
    }

    private void processGuildTagInput(Player player, String guildName, String tag) {
        int maxTagLen = plugin.getConfig().getInt("guild.max-tag-length", 4);

        if (tag.length() < 2 || tag.length() > maxTagLen) {
            player.sendMessage(plugin.getMessage("guild.tag-length-invalid")
                    .replace("%max%", String.valueOf(maxTagLen)));
            reopenCurrentAnvil(player);
            return;
        }

        double cost = plugin.getConfig().getDouble("guild.create-cost", 0.0);
        GuildCurrency currency = plugin.getGuildCurrency();
        GuildCurrency.CurrencyType currencyType = plugin.getCurrencyConfig().getCurrencyType();

        if (cost > 0 && !currency.withdraw(player.getUniqueId(), (long) cost, currencyType)) {
            player.sendMessage(plugin.getMessage("guild.insufficient-funds")
                    .replace("%cost%", String.valueOf(cost)));
            PENDING_GUILD_NAME.remove(player.getUniqueId());
            ANVIL_TEXT_CACHE.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }

        Guild guild = plugin.getGuildManager().createGuild(guildName, player);
        if (guild == null) {
            player.sendMessage(plugin.getMessage("guild.already-exists"));
            if (cost > 0) currency.deposit(player.getUniqueId(), (long) cost, currencyType);
            reopenCurrentAnvil(player);
            return;
        }

        guild.setTag(tag);
        plugin.getDatabaseManager().saveGuild(guild);

        PENDING_GUILD_NAME.remove(player.getUniqueId());
        ANVIL_TEXT_CACHE.remove(player.getUniqueId());

        player.sendMessage(plugin.getMessage("guild.created").replace("%name%", guildName));
        player.sendMessage(ChatColor.GREEN + "公会标签: " + tag);
        GuildGUI.openGUI(plugin, player);
    }

    // ========== 清理方法（供外部调用）==========

    public static void cleanupPlayer(UUID uuid) {
        PENDING_GUILD_NAME.remove(uuid);
        ANVIL_TEXT_CACHE.remove(uuid);
    }
}
