package com.example.homes.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;

import com.example.homes.HomesPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {

    private final HomesPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(HomesPlugin plugin) {
        this.plugin = plugin;
        setupDataSource();
        createTable();
    }

    private void setupDataSource() {
        HikariConfig config = new HikariConfig();
        String type = plugin.getConfig().getString("database.type", "h2").toLowerCase();

        if (type.equals("mariadb") || type.equals("mysql")) {
            String host = plugin.getConfig().getString("database.host", "localhost");
            String port = plugin.getConfig().getString("database.port", "3306");
            String db = plugin.getConfig().getString("database.name", "minecraft");
            String user = plugin.getConfig().getString("database.user", "root");
            String pass = plugin.getConfig().getString("database.password", "");
            
            config.setJdbcUrl("jdbc:" + type + "://" + host + ":" + port + "/" + db);
            config.setUsername(user);
            config.setPassword(pass);
            
            // Optimization for MySQL/MariaDB
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
        } else {
            // H2 (Default)
            String path = plugin.getDataFolder().getAbsolutePath() + "/homes";
            config.setJdbcUrl("jdbc:h2:" + path + ";MODE=MySQL");
            config.setDriverClassName("org.h2.Driver");
        }

        // Connection Pool Tuning
        config.setPoolName("HomesPlugin-Pool");
        config.setMaximumPoolSize(10); // Adjust based on server load, 10 is usually enough for this plugin
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000); // 30 seconds
        config.setMaxLifetime(1800000); // 30 minutes
        config.setConnectionTimeout(10000); // 10 seconds

        dataSource = new HikariDataSource(config);
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_homes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "home_name VARCHAR(64) NOT NULL," +
                "world_name VARCHAR(64) NOT NULL," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL," +
                "pitch FLOAT NOT NULL," +
                "is_public BOOLEAN NOT NULL DEFAULT FALSE," +
                "UNIQUE (player_uuid, home_name)" +
                ");";
        
        // Use createIndex for faster lookups
        String indexSql = "CREATE INDEX IF NOT EXISTS idx_player_uuid ON player_homes(player_uuid);";

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(indexSql)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addColumnIfNotExists(String columnName, String columnDef) {
        try (Connection conn = dataSource.getConnection()) {
            boolean columnExists = false;
            // Check if column exists using MetaData is most reliable across DB types
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "PLAYER_HOMES", null)) {
                while (rs.next()) {
                    if (columnName.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                        columnExists = true;
                        break;
                    }
                }
            }
            
            // If checking "PLAYER_HOMES" (uppercase) failed, try lowercase "player_homes" just in case
            if (!columnExists) {
                 try (ResultSet rs = conn.getMetaData().getColumns(null, null, "player_homes", null)) {
                    while (rs.next()) {
                        if (columnName.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                            columnExists = true;
                            break;
                        }
                    }
                }
            }

            if (!columnExists) {
                String sql = "ALTER TABLE player_homes ADD COLUMN " + columnName + " " + columnDef;
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public void setHome(UUID uuid, String name, Location loc, boolean isPublic) {
        String sql = "INSERT INTO player_homes (player_uuid, home_name, world_name, x, y, z, yaw, pitch, is_public) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE world_name=?, x=?, y=?, z=?, yaw=?, pitch=?, is_public=?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.setString(3, loc.getWorld().getName());
            stmt.setDouble(4, loc.getX());
            stmt.setDouble(5, loc.getY());
            stmt.setDouble(6, loc.getZ());
            stmt.setFloat(7, loc.getYaw());
            stmt.setFloat(8, loc.getPitch());
            stmt.setBoolean(9, isPublic);
            
            stmt.setString(10, loc.getWorld().getName());
            stmt.setDouble(11, loc.getX());
            stmt.setDouble(12, loc.getY());
            stmt.setDouble(13, loc.getZ());
            stmt.setFloat(14, loc.getYaw());
            stmt.setFloat(15, loc.getPitch());
            stmt.setBoolean(16, isPublic);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setHome(UUID uuid, String name, Location loc) {
        setHome(uuid, name, loc, false); // Default not public
    }
    
    public void updatePublic(UUID uuid, String name, boolean isPublic) {
        String sql = "UPDATE player_homes SET is_public = ? WHERE player_uuid = ? AND home_name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, isPublic);
            stmt.setString(2, uuid.toString());
            stmt.setString(3, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean isPublic(UUID uuid, String name) {
        String sql = "SELECT is_public FROM player_homes WHERE player_uuid = ? AND home_name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_public");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void deleteHome(UUID uuid, String name) {
        String sql = "DELETE FROM player_homes WHERE player_uuid = ? AND home_name = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Location getHome(UUID uuid, String name) {
        String sql = "SELECT * FROM player_homes WHERE player_uuid = ? AND home_name = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Location(
                            plugin.getServer().getWorld(rs.getString("world_name")),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, Boolean> getHomePublicStatus(UUID uuid) {
        Map<String, Boolean> status = new HashMap<>();
        String sql = "SELECT home_name, is_public FROM player_homes WHERE player_uuid = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    status.put(rs.getString("home_name"), rs.getBoolean("is_public"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return status;
    }

    public Map<String, Location> getHomes(UUID uuid) {
        Map<String, Location> homes = new HashMap<>();
        String sql = "SELECT * FROM player_homes WHERE player_uuid = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Location loc = new Location(
                            plugin.getServer().getWorld(rs.getString("world_name")),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                    homes.put(rs.getString("home_name"), loc);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return homes;
    }
}
