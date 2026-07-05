package com.guild.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全版本兼容工具类（1.7.10 ~ 26.1+）
 *
 * 设计原则（参照工业级插件兼容规范）：
 * 1. 基线兼容：以 1.8 Bukkit API 为基线，核心逻辑仅使用全版本通用接口
 * 2. 版本隔离：Material/NMS 差异化逻辑全部封装在此类，核心代码面向此工具类编程
 * 3. 动态适配：启动时自动识别服务端版本，无专属适配时自动降级
 * 4. 反射缓存：所有反射 Method 对象缓存到 Map，避免重复查找
 */
public final class VersionCompat {

    // ==================== 版本信息（启动时一次性初始化）====================

    /** NMS 版本号，如 v1_8_R3、v1_26_R1 */
    private static final String NMS_VERSION;

    /** MC 原版版本号，如 1.8.8、1.26.1 */
    private static final String MC_VERSION;

    /** MC 主版本号，如 8、26 */
    private static final int MAJOR_VERSION;

    /** 是否为 1.13+（扁平化材质系统）*/
    private static final boolean MODERN_MATERIALS;

    static {
        // 初始化 NMS 版本（全版本通用，通过 Server 类的包名获取）
        String nmsVer = "";
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            nmsVer = pkg.substring(pkg.lastIndexOf(".") + 1);
        } catch (Exception ignored) {}
        NMS_VERSION = nmsVer;

        // 初始化 MC 原版版本（全版本通用，解析 getVersion 输出）
        String mcVer = "";
        try {
            String ver = Bukkit.getVersion();
            int start = ver.indexOf("(MC: ");
            if (start >= 0) {
                start += 5;
                int end = ver.indexOf(')', start);
                mcVer = ver.substring(start, end);
            }
        } catch (Exception ignored) {}
        MC_VERSION = mcVer;

        // 解析主版本号
        int major = 8;
        try {
            String[] parts = MC_VERSION.split("\\.");
            major = Integer.parseInt(parts[1]);
        } catch (Exception ignored) {}
        MAJOR_VERSION = major;

        // 检测是否为扁平化材质（1.13+）
        boolean modern = false;
        try { Material.valueOf("OAK_LOG"); modern = true; }
        catch (IllegalArgumentException | NoSuchFieldError ignored) {}
        MODERN_MATERIALS = modern;
    }

    // ==================== 反射方法缓存 ====================

    /** 缓存所有反射查找的 Method 对象 */
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取并缓存方法引用（线程安全）
     * @param clazz 目标类
     * @param name 方法名
     * @param paramTypes 参数类型
     * @return 缓存的方法对象，不存在则返回 null
     */
    public static Method getCachedMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        String key = clazz.getName() + "." + name + Arrays.toString(paramTypes);
        return METHOD_CACHE.computeIfAbsent(key, k -> {
            try {
                Method m = clazz.getMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                return null; // 缓存 null 表示该方法不存在
            }
        });
    }

    /**
     * 安全调用已缓存的方法
     * @param method 方法对象
     * @param target 调用目标（static 方法传 null）
     * @param args 参数列表
     * @return 调用结果，失败返回 null
     */
    public static Object invokeCached(Method method, Object target, Object... args) {
        if (method == null) return null;
        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Guild] 反射调用失败: " + method.getName());
            return null;
        }
    }

    /**
     * 获取 NMS 类（全版本通用）
     * @param className NMS 简短类名（不含包名前缀）
     * @return NMS Class 对象
     */
    public static Class<?> getNmsClass(String className) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + NMS_VERSION + "." + className);
    }

    // ==================== 版本查询 API ====================

    /** 获取 NMS 版本字符串，如 v1_26_R1 */
    public static String getNmsVersion() { return NMS_VERSION; }

    /** 获取 MC 原版版本字符串，如 1.26.1 */
    public static String getMcVersion() { return MC_VERSION; }

    /** 获取 MC 主版本号，如 26 */
    public static int getMajorVersion() { return MAJOR_VERSION; }

    /** 是否为 1.13+ 扁平化材质版本 */
    public static boolean isModernVersion() { return MODERN_MATERIALS; }

    /** 是否为 1.17+（需要 Java 17+ 的版本）*/
    public static boolean isSeventeenPlus() { return MAJOR_VERSION >= 17; }

    /** 是否为 1.20.5+（组件系统版本）*/
    public static boolean isTwentyPlusFive() { return MAJOR_VERSION >= 20 && MC_VERSION.compareTo("1.20.5") >= 0; }

    /** 获取服务端类型名称 */
    public static String getServerType() {
        try { return Bukkit.getServer().getName(); }
        catch (Exception e) { return "Unknown"; }
    }

    // ==================== InventoryView 标题获取（兼容旧版）====================

    public static String getInventoryTitleFromView(InventoryView view) {
        if (view == null) return "";
        // 优先直接调用（大多数版本支持）
        try { return view.getTitle(); }
        catch (Throwable ignored) {}

        // 降级：反射调用
        Method titleMethod = getCachedMethod(InventoryView.class, "getTitle");
        if (titleMethod != null) {
            Object result = invokeCached(titleMethod, view);
            if (result instanceof String) return (String) result;
        }
        return "";
    }

    // ==================== Material 工厂方法（全版本兼容）====================
    //
    // 1.13+ 使用扁平化名称（如 PLAYER_HEAD）
    // 1.12- 使用旧版名称 + data 值（如 SKULL_ITEM:3）
    // 所有方法在首次调用时确定版本并返回正确的 Material

    public static Material getBarrierMaterial()          { return safeMaterial("BARRIER"); }
    public static Material getArrowMaterial()             { return safeMaterial("ARROW"); }
    public static Material getDiamondMaterial()           { return safeMaterial("DIAMOND"); }
    public static Material getBookMaterial()              { return safeMaterial("BOOK"); }
    public static Material getPaperMaterial()             { return safeMaterial("PAPER"); }
    public static Material getPlayerHeadMaterial()        { return legacyMaterial("PLAYER_HEAD", "SKULL_ITEM", (short)3); }
    public static Material getRedstoneMaterial()          { return safeMaterial("REDSTONE"); }
    public static Material getGoldIngotMaterial()         { return safeMaterial("GOLD_INGOT"); }
    public static Material getChestMaterial()             { return safeMaterial("CHEST"); }
    public static Material getLeverMaterial()             { return safeMaterial("LEVER"); }
    public static Material getNoteBlockMaterial()         { return safeMaterial("NOTE_BLOCK"); }
    public static Material getNameTagMaterial()           { return safeMaterial("NAME_TAG"); }
    public static Material getAnvilMaterial()             { return safeMaterial("ANVIL"); }
    public static Material getCommandBlockMaterial()      { return safeMaterial("COMMAND_BLOCK"); }
    public static Material getEmeraldMaterial()           { return safeMaterial("EMERALD"); }
    public static Material getExperienceBottleMaterial()  { return safeMaterial("EXPERIENCE_BOTTLE"); }
    public static Material getGoldenHelmetMaterial()      { return safeMaterial("GOLDEN_HELMET"); }
    public static Material getIronHelmetMaterial()        { return safeMaterial("IRON_HELMET"); }
    public static Material getGrayStainedGlassPaneMaterial(){ return legacyMaterial("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", (short)8); }
    public static Material getLightBlueStainedGlassPaneMaterial(){ return legacyMaterial("LIGHT_BLUE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", (short)3); }
    public static Material getDiamondBlockMaterial()      { return safeMaterial("DIAMOND_BLOCK"); }
    public static Material getLimeStainedGlassPaneMaterial(){ return legacyMaterial("LIME_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", (short)5); }
    public static Material getRedStainedGlassPaneMaterial(){ return legacyMaterial("RED_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", (short)14); }
    public static Material getGoldStainedGlassPaneMaterial(){ return legacyMaterial("YELLOW_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", (short)4); }
    public static Material getPurpleStainedGlassPaneMaterial(){ return legacyMaterial("PURPLE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", (short)10); }
    public static Material getNetherStarMaterial(){ return safeMaterial("NETHER_STAR"); }
    public static Material getIronIngotMaterial(){ return safeMaterial("IRON_INGOT"); }

    /**
     * 安全获取扁平化版本的 Material（1.13+ 名称），失败回退 STONE
     */
    private static Material safeMaterial(String modernName) {
        try { return Material.valueOf(modernName); }
        catch (IllegalArgumentException | NoSuchFieldError e) { return Material.STONE; }
    }

    /**
     * 兼容新旧版本的 Material 获取
     * @param modernName  1.13+ 名称
     * @param legacyName  1.8-1.12 名称
     * @param legacyData  旧版 data 值
     */
    @SuppressWarnings("deprecation")
    private static Material legacyMaterial(String modernName, String legacyName, short legacyData) {
        if (MODERN_MATERIALS) {
            try { return Material.valueOf(modernName); }
            catch (IllegalArgumentException | NoSuchFieldError ignored) {}
        }
        try { return Material.valueOf(legacyName); }
        catch (IllegalArgumentException | NoSuchFieldError ignored) {}
        return Material.STONE;
    }

    /**
     * 通用 Material 获取入口（供外部动态使用）
     */
    public static Material getMaterial(String modernName) {
        return safeMaterial(modernName);
    }

    // ==================== ItemStack 工厂方法 ====================

    /**
     * 创建带名称和描述的物品
     */
    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                meta.setLore(Arrays.stream(lore)
                        .map(l -> ChatColor.translateAlternateColorCodes('&', l))
                        .collect(java.util.stream.Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 创建玩家头颅物品（全版本兼容）
     * 1.13+: Material.PLAYER_HEAD + SkullMeta.setOwningPlayer()
     * 1.12-: Material.SKULL_ITEM:3 + SkullMeta.setOwner() 或 setOwningPlayer()
     */
    @SuppressWarnings("deprecation")
    public static ItemStack createPlayerHead(UUID playerUuid, String name, String... lore) {
        ItemStack head;
        if (MODERN_MATERIALS) {
            head = new ItemStack(Material.PLAYER_HEAD, 1);
        } else {
            head = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
        }
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta) {
            // 缓存 OfflinePlayer 引用，避免重复查询（原代码调用两次 getOfflinePlayer）
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
            try {
                ((SkullMeta) meta).setOwningPlayer(offlinePlayer);
            } catch (NoSuchMethodError e) {
                // 1.12- 旧 API：使用 setOwner
                try {
                    String playerName = offlinePlayer.getName();
                    if (playerName != null) ((SkullMeta) meta).setOwner(playerName);
                } catch (Exception ignored) {}
            }
        }
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                meta.setLore(Arrays.stream(lore)
                        .map(l -> ChatColor.translateAlternateColorCodes('&', l))
                        .collect(java.util.stream.Collectors.toList()));
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    // ==================== 消息发送辅助 ====================

    /**
     * 发送带颜色码转换的消息（& -> §）
     */
    public static void sendColoredMessage(Player player, String message) {
        if (player == null || message == null) return;
        try {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        } catch (Exception e) {
            player.sendMessage(message.replace("&", "\u00a7"));
        }
    }

    /**
     * 发送标题消息（1.8+ 支持，旧版本降级为聊天消息）
     */
    public static void sendTitle(Player player, String title, String subtitle,
                                  int fadeIn, int stay, int fadeOut) {
        if (player == null) return;
        try {
            player.sendTitle(
                    ChatColor.translateAlternateColorCodes('&', title),
                    ChatColor.translateAlternateColorCodes('&', subtitle),
                    fadeIn, stay, fadeOut);
        } catch (NoSuchMethodError e) {
            sendColoredMessage(player, title + " " + subtitle);
        }
    }

    // ==================== JSON 可点击消息（供 MessageUtils 调用）====================

    private static Method SEND_RAW_METHOD;

    /**
     * 发送可点击 JSON 消息
     * 通过反射调用 Player.sendRawMessage(String json)，低版本自动降级为普通消息
     */
    public static void sendClickableMessage(Player player, String text, String hoverText,
                                             String clickCommand, String action) {
        if (player == null) return;
        try {
            // 构建 JSON 文本组件
            String escapedText = escapeJson(text);
            String escapedHover = escapeJson(hoverText);
            String escapedCmd = escapeJson(clickCommand);
            String json = "[{\"text\":\"" + colorize(escapedText)
                    + "\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"" + colorize(escapedHover) + "\"}"
                    + ",\"clickEvent\":{\"action\":\"" + action + "\",\"value\":\"" + escapedCmd + "\"}}]";

            // 缓存并调用 sendRawMessage
            if (SEND_RAW_METHOD == null) {
                SEND_RAW_METHOD = getCachedMethod(Player.class, "sendRawMessage", String.class);
            }
            if (SEND_RAW_METHOD != null) {
                SEND_RAW_METHOD.invoke(player, json);
                return;
            }
        } catch (Exception ignored) {}
        // 降级：普通消息
        sendColoredMessage(player, text + " -> " + clickCommand);
    }

    public static void sendAcceptDeclineMessage(Player player, String text,
                                                  String acceptCmd, String declineCmd) {
        sendClickableMessage(player, text + " [接受]", "点击接受", acceptCmd, "run_command");
        sendClickableMessage(player, "[拒绝]", "点击拒绝", declineCmd, "run_command");
    }

    public static void sendAcceptMessage(Player player, String text, String acceptCmd) {
        sendClickableMessage(player, text, "点击接受", acceptCmd, "run_command");
    }

    // ==================== 内部工具 ====================

    /** 转义 JSON 特殊字符 */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    /** 将 & 颜色码转为 Minecraft 颜色码 */
    private static String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
