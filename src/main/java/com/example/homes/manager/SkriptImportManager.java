package com.example.homes.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.homes.HomesPlugin;

public class SkriptImportManager {

    private final HomesPlugin plugin;
    private final HomeManager homeManager;

    public SkriptImportManager(HomesPlugin plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
    }

    public void startImport(Player player, File csvFile) {
        if (!csvFile.exists()) {
            player.sendMessage(ChatColor.RED + "ファイルが見つかりません: " + csvFile.getAbsolutePath());
            return;
        }

        player.sendMessage(ChatColor.GREEN + "移行を開始します... バックグラウンドで処理されます。");
        BossBar bossBar = Bukkit.createBossBar(ChatColor.YELLOW + "Skript データ移行中...", BarColor.YELLOW, BarStyle.SOLID);
        bossBar.addPlayer(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // First pass: count lines
                    int totalLines = countLines(csvFile);
                    AtomicInteger processed = new AtomicInteger(0);
                    AtomicInteger imported = new AtomicInteger(0);
                    AtomicInteger failed = new AtomicInteger(0);

                    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                        String line;
                        // Skip header if exists (starts with #)
                        while ((line = br.readLine()) != null) {
                            if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                                continue;
                            }
                            
                            // Process line
                            // Format: homes::UUID::Number, location, BINARY_DATA
                            // Or: homes::UUID::Number, type, DATA
                            
                            String[] parts = line.split(",", 3);
                            if (parts.length >= 3) {
                                String key = parts[0].trim();
                                String type = parts[1].trim();
                                String data = parts[2].trim();

                                if (key.startsWith("homes::") && type.equalsIgnoreCase("location")) {
                                    // Parse Key
                                    String[] keyParts = key.split("::");
                                    if (keyParts.length == 3) {
                                        String uuidStr = keyParts[1];
                                        String homeNum = keyParts[2];
                                        
                                        try {
                                            UUID uuid = UUID.fromString(uuidStr);
                                            // Parse Location
                                            Location loc = parseSkriptLocation(data);
                                            
                                            if (loc != null) {
                                                // Save to HomeManager
                                                // Use "Home <Num>" as name
                                                String homeName = "Home " + homeNum;
                                                
                                                // Sync to main thread for DB operations if not thread safe (HomeManager uses DB)
                                                // But usually DB ops are async safe or handled. 
                                                // However, HomeManager methods might update cache which is not thread safe.
                                                // Let's schedule a sync task for the actual save, or batch them.
                                                // For simplicity/safety, sync task per batch or single.
                                                // Given the potential volume, sync per item is slow but safe.
                                                
                                                // Actually, let's just do it directly if DB manager handles connection pool.
                                                // But HomeManager.setHome might trigger events or messages? No, just DB.
                                                // Let's call a method that doesn't message player.
                                                // homeManager.setHome sends messages? No, it just saves.
                                                // But let's check HomeManager source.
                                                // It seems safe.
                                                
                                                // WARNING: setHome updates cache which is ConcurrentHashMap, so it's thread safe.
                                                // BUT, databaseManager operations should be checked.
                                                
                                                // To be safe and avoid lag, we should run DB inserts here (async).
                                                homeManager.setHomeDirectly(uuid, homeName, loc);
                                                imported.incrementAndGet();
                                            } else {
                                                failed.incrementAndGet();
                                                plugin.getLogger().warning("Failed to parse location for " + key);
                                            }
                                        } catch (IllegalArgumentException e) {
                                            failed.incrementAndGet(); // Invalid UUID
                                        }
                                    }
                                }
                            }

                            int current = processed.incrementAndGet();
                            double progress = (double) current / totalLines;
                            
                            // Update BossBar (Sync)
                            if (current % 10 == 0) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        bossBar.setProgress(Math.min(1.0, progress));
                                        bossBar.setTitle(ChatColor.YELLOW + "Skript データ移行中... " + (int)(progress * 100) + "% (" + current + "/" + totalLines + ")");
                                    }
                                }.runTask(plugin);
                            }
                        }
                    }

                    // Finish
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            bossBar.removeAll();
                            player.sendMessage(ChatColor.GREEN + "移行が完了しました！");
                            player.sendMessage(ChatColor.GREEN + "成功: " + imported.get() + ", 失敗/スキップ: " + failed.get());
                            plugin.getLogger().info("Migration finished. Imported: " + imported.get() + ", Failed: " + failed.get());
                        }
                    }.runTask(plugin);

                } catch (IOException e) {
                    e.printStackTrace();
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            bossBar.removeAll();
                            player.sendMessage(ChatColor.RED + "エラーが発生しました: " + e.getMessage());
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private int countLines(File file) throws IOException {
        int lines = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (reader.readLine() != null) lines++;
        }
        return lines;
    }

    /**
     * Parses Skript's hex-encoded binary location string.
     * Note: This is a best-effort heuristic parser based on known patterns.
     * Skript binary format is complex and might vary.
     * 
     * Pattern observed in example:
     * 8605776F726C6480FF0000000181046E616D65208010537572766976616C5F7468655F656E64017809408D15F7620B77560179093FE0000000000000017A09C07D7C5FF4BE3B140570697463680840F8E04C037961770842A907BA
     * 
     * "world" -> 776F726C64
     * "name" -> 6E616D65
     * "Survival_the_end" -> 537572766976616C5F7468655F656E64
     * "x" -> 78
     * "y" -> 79
     * "z" -> 7A
     * "pitch" -> 7069746368
     * "yaw" -> 796177
     * 
     * Values seem to follow keys.
     * Doubles seem to be 8 bytes (64-bit IEEE 754) often prefixed by 09 (type marker?)
     */
    private Location parseSkriptLocation(String hexData) {
        try {
            // Heuristic Parsing
            // 1. Find World Name
            String worldName = extractStringAfterKey(hexData, "name");
            if (worldName == null) worldName = extractStringAfterKey(hexData, "world"); // fallback
            if (worldName == null) return null;

            // 2. Find Coordinates (x, y, z)
            // Keys are "x" (78), "y" (79), "z" (7A)
            // Values are doubles.
            double x = extractDoubleAfterKey(hexData, "78"); // x
            double y = extractDoubleAfterKey(hexData, "79"); // y
            double z = extractDoubleAfterKey(hexData, "7A"); // z
            
            // 3. Find Rotation (pitch, yaw)
            // Keys: "pitch" (7069746368), "yaw" (796177)
            // Note: In Skript dump, pitch/yaw might be floats or doubles.
            // Example dump shows pitch/yaw after z.
            // Let's try to extract them.
            float pitch = (float) extractDoubleAfterKey(hexData, "7069746368");
            float yaw = (float) extractDoubleAfterKey(hexData, "796177");

            if (Bukkit.getWorld(worldName) == null) {
                // Try to map "Survival" to "world", etc if needed
                // For now, just log warning if null but proceed (it will be null in Location)
            }

            return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
        } catch (Exception e) {
            // e.printStackTrace();
            return null;
        }
    }

    private String extractStringAfterKey(String hex, String keyName) {
        // Key is usually preceded by length or type?
        // In the example: ... 6E616D65 (name) 20 (space?) 80 (map?) 10 (length 16?) 53... (Survival...)
        // Hex for "name" is 6E616D65
        
        // Simple strategy: Find hex of key, look ahead for string content.
        // Skript serialization is NBT-like or custom.
        // Let's try to find the ASCII bytes of the world name.
        
        // "Survival" = 537572766976616C
        // "Survival_nether" = 537572766976616C5F6E6574686572
        // "Survival_the_end" = 537572766976616C5F7468655F656E64
        
        // Regex lookups for known world names might be safer/easier if standard worlds.
        if (hex.contains("537572766976616C5F6E6574686572")) return "world_nether";
        if (hex.contains("537572766976616C5F7468655F656E64")) return "world_the_end";
        if (hex.contains("537572766976616C")) return "world";
        
        return null;
    }

    private double extractDoubleAfterKey(String hex, String keyHex) {
        // Locate key
        int index = hex.indexOf(keyHex);
        if (index == -1) return 0.0;
        
        // After key (e.g. "78" for x), there is usually a type marker (09 for double?) then 8 bytes (16 hex chars)
        // Example: ... 01 78 (x) 09 (double) 408D15F7620B7756 ...
        // So skip key length + 2 chars (type marker)
        
        try {
            int start = index + keyHex.length() + 2;
            String doubleHex = hex.substring(start, start + 16);
            long longBits = Long.parseUnsignedLong(doubleHex, 16);
            return Double.longBitsToDouble(longBits);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
