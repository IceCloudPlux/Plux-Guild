package com.guild.database;

import com.guild.GuildPlugin;
import com.guild.guild.Guild;
import com.guild.guild.GuildMember;
import com.guild.guild.GuildPermission;
import com.guild.guild.GuildRole;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final GuildPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String dbPath = dataFolder.getAbsolutePath() + "/guilds.db";
            String jdbcUrl = "jdbc:sqlite:" + dbPath;

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setPoolName("GuildPool");
            config.setMaximumPoolSize(3);
            config.setMinimumIdle(1);
            config.setIdleTimeout(30000);
            config.setMaxLifetime(180000);
            config.setConnectionTimeout(5000);

            // SQLite-specific optimizations
            config.addDataSourceProperty("journal_mode", "WAL");
            config.addDataSourceProperty("synchronous", "NORMAL");
            config.addDataSourceProperty("cache_size", "-2000");

            this.dataSource = new HikariDataSource(config);

            createTables();
            loadGuilds();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS guilds (" +
                    "name TEXT PRIMARY KEY," +
                    "tag TEXT," +
                    "tag_color TEXT," +
                    "owner TEXT," +
                    "level INTEGER DEFAULT 0," +
                    "experience INTEGER DEFAULT 0," +
                    "daily_experience INTEGER DEFAULT 0," +
                    "motd TEXT DEFAULT ''," +
                    "public_guild INTEGER DEFAULT 1," +
                    "created_time INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS guild_members (" +
                    "guild_name TEXT," +
                    "uuid TEXT," +
                    "role TEXT," +
                    "joined_time INTEGER," +
                    "total_contribution INTEGER DEFAULT 0," +
                    "daily_contribution INTEGER DEFAULT 0," +
                    "muted INTEGER DEFAULT 0," +
                    "muted_until INTEGER DEFAULT 0," +
                    "nickname TEXT," +
                    "PRIMARY KEY (guild_name, uuid)," +
                    "FOREIGN KEY (guild_name) REFERENCES guilds(name))");
            stmt.execute("CREATE TABLE IF NOT EXISTS guild_permissions (" +
                    "guild_name TEXT," +
                    "permission TEXT," +
                    "level INTEGER," +
                    "PRIMARY KEY (guild_name, permission)," +
                    "FOREIGN KEY (guild_name) REFERENCES guilds(name))");
            stmt.execute("CREATE TABLE IF NOT EXISTS player_settings (" +
                    "uuid TEXT PRIMARY KEY," +
                    "guild_invites INTEGER DEFAULT 1," +
                    "join_notifications INTEGER DEFAULT 1)");
            stmt.execute("CREATE TABLE IF NOT EXISTS guild_banks (" +
                    "guild_name TEXT PRIMARY KEY," +
                    "balance INTEGER DEFAULT 0)");
        }
    }

    public void loadGuilds() {
        try (Connection conn = getConnection()) {
            Map<String, Guild> guilds = plugin.getGuildManager().getGuilds();
            Map<UUID, String> playerGuilds = plugin.getGuildManager().getPlayerGuilds();

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM guilds");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String name = rs.getString("name");
                    String tag = rs.getString("tag");
                    String tagColor = rs.getString("tag_color");
                    UUID owner = UUID.fromString(rs.getString("owner"));
                    int level = rs.getInt("level");
                    long experience = rs.getLong("experience");
                    long dailyExperience = rs.getLong("daily_experience");
                    String motd = rs.getString("motd");
                    boolean publicGuild = rs.getBoolean("public_guild");
                    long createdTime = rs.getLong("created_time");

                    Guild guild = new Guild(name, owner);
                    guild.setTag(tag);
                    guild.setTagColor(tagColor);
                    guild.setLevel(level);
                    guild.setExperience(experience);
                    guild.setDailyExperience(dailyExperience);
                    guild.setMotd(motd);
                    guild.setPublicGuild(publicGuild);
                    guild.setCreatedTime(createdTime);

                    loadGuildMembers(conn, guild);
                    loadGuildPermissions(conn, guild);
                    loadGuildBank(conn, guild);

                    guilds.put(name.toLowerCase(), guild);
                }
            }

            // Rebuild player-guild mapping
            for (Guild guild : guilds.values()) {
                for (UUID memberUuid : guild.getMembers().keySet()) {
                    playerGuilds.put(memberUuid, guild.getName().toLowerCase());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load guilds", e);
        }
    }

    private void loadGuildMembers(Connection conn, Guild guild) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM guild_members WHERE guild_name = ?")) {
            ps.setString(1, guild.getName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    GuildRole role = GuildRole.valueOf(rs.getString("role"));
                    long totalContribution = rs.getLong("total_contribution");
                    long dailyContribution = rs.getLong("daily_contribution");
                    boolean muted = rs.getBoolean("muted");
                    long mutedUntil = rs.getLong("muted_until");
                    String nickname = rs.getString("nickname");

                    GuildMember member = new GuildMember(uuid, role);
                    member.setTotalContribution(totalContribution);
                    member.setDailyContribution(dailyContribution);
                    if (muted) {
                        long remainingMute = mutedUntil - System.currentTimeMillis();
                        if (remainingMute > 0) {
                            member.mute(remainingMute);
                        }
                    }
                    if (nickname != null && !nickname.isEmpty()) {
                        member.setNickname(nickname);
                    }
                    guild.getMembers().put(uuid, member);
                }
            }
        }
    }

    private void loadGuildPermissions(Connection conn, Guild guild) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM guild_permissions WHERE guild_name = ?")) {
            ps.setString(1, guild.getName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String permission = rs.getString("permission");
                    int level = rs.getInt("level");
                    GuildPermission perm;
                    switch (level) {
                        case 1:
                            perm = GuildPermission.MEMBER;
                            break;
                        case 2:
                            perm = GuildPermission.OFFICER;
                            break;
                        default:
                            perm = GuildPermission.OWNER;
                            break;
                    }
                    guild.getPermissions().put(permission, perm);
                }
            }
        }
    }

    private void loadGuildBank(Connection conn, Guild guild) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM guild_banks WHERE guild_name = ?")) {
            ps.setString(1, guild.getName());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long balance = rs.getLong("balance");
                    guild.getBank().setBalance(balance);
                }
            }
        }
    }

    /**
     * 保存公会数据（使用事务确保原子性）
     * <p>
     * 优化：成员数据使用 INSERT OR REPLACE（upsert）替代 DELETE + 全量重新插入，
     * 减少数据库 I/O 开销，尤其在成员数量较多时效果显著。
     */
    public void saveGuild(Guild guild) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Save guild main data
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR REPLACE INTO guilds (name, tag, tag_color, owner, level, experience, daily_experience, motd, public_guild, created_time) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, guild.getName());
                    ps.setString(2, guild.getTag());
                    ps.setString(3, guild.getTagColor());
                    ps.setString(4, guild.getOwner().toString());
                    ps.setInt(5, guild.getLevel());
                    ps.setLong(6, guild.getExperience());
                    ps.setLong(7, guild.getDailyExperience());
                    ps.setString(8, guild.getMotd());
                    ps.setBoolean(9, guild.isPublicGuild());
                    ps.setLong(10, guild.getCreatedTime());
                    ps.executeUpdate();
                }

                // Upsert members (INSERT OR REPLACE per member, no bulk delete needed)
                try (PreparedStatement insertPs = conn.prepareStatement(
                        "INSERT OR REPLACE INTO guild_members (guild_name, uuid, role, joined_time, total_contribution, daily_contribution, muted, muted_until, nickname) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    for (GuildMember member : guild.getMembers().values()) {
                        insertPs.setString(1, guild.getName());
                        insertPs.setString(2, member.getUuid().toString());
                        insertPs.setString(3, member.getRole().name());
                        insertPs.setLong(4, member.getJoinedTime());
                        insertPs.setLong(5, member.getTotalContribution());
                        insertPs.setLong(6, member.getDailyContribution());
                        insertPs.setBoolean(7, member.isMuted());
                        insertPs.setLong(8, member.isMuted() ?
                                System.currentTimeMillis() + 86400000L : 0L);
                        insertPs.setString(9, member.getNickname());
                        insertPs.addBatch();
                    }
                    insertPs.executeBatch();
                }

                // Save bank balance
                try (PreparedStatement bankPs = conn.prepareStatement(
                        "INSERT OR REPLACE INTO guild_banks (guild_name, balance) VALUES (?, ?)")) {
                    bankPs.setString(1, guild.getName());
                    bankPs.setLong(2, guild.getBank().getBalance());
                    bankPs.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save guild: " + guild.getName(), e);
        }
    }

    /**
     * 删除公会及其关联数据（成员、权限、银行）
     */
    public void deleteGuild(String guildName) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM guild_members WHERE guild_name = ?")) {
                    ps.setString(1, guildName);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM guild_permissions WHERE guild_name = ?")) {
                    ps.setString(1, guildName);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM guild_banks WHERE guild_name = ?")) {
                    ps.setString(1, guildName);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM guilds WHERE name = ?")) {
                    ps.setString(1, guildName);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete guild: " + guildName, e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
