package com.example.homes.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.homes.HomesPlugin;
import com.example.homes.database.DatabaseManager;

public class HomeManager {

    private final HomesPlugin plugin;
    private DatabaseManager databaseManager;
    
    // Cache: UUID -> (HomeName -> Location)
    private final Map<UUID, Map<String, Location>> homeCache = new ConcurrentHashMap<>();
    
    // Cache: UUID -> (HomeName -> Boolean) - Public Status
    private final Map<UUID, Map<String, Boolean>> publicCache = new ConcurrentHashMap<>();

    public HomeManager(HomesPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        this.databaseManager = new DatabaseManager(plugin);
    }

    public void close() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    // Load data asynchronously
    public void loadHomes(UUID uuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Map<String, Location> homes = databaseManager.getHomes(uuid);
                homeCache.put(uuid, homes);
                
                // Bulk fetch public status to avoid N+1 queries
                Map<String, Boolean> publicStatus = databaseManager.getHomePublicStatus(uuid);
                publicCache.put(uuid, publicStatus);
            }
        }.runTaskAsynchronously(plugin);
    }

    // Unload data (Save not needed as we save on write, just clear cache)
    public void unloadHomes(UUID uuid) {
        homeCache.remove(uuid);
        publicCache.remove(uuid);
    }
    
    // Async set home
    public void setHome(Player player, String name, Location loc) {
        setHomeDirectly(player.getUniqueId(), name, loc);
    }
    
    public void setHomeDirectly(UUID uuid, String name, Location loc) {
        // Update cache immediately for responsiveness
        homeCache.computeIfAbsent(uuid, k -> new HashMap<>()).put(name, loc);
        
        // Save to DB asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                databaseManager.setHome(uuid, name, loc, false); // Default false
            }
        }.runTaskAsynchronously(plugin);
    }
    
    public void setPublic(UUID uuid, String name, boolean isPublic) {
        publicCache.computeIfAbsent(uuid, k -> new HashMap<>()).put(name, isPublic);
        new BukkitRunnable() {
            @Override
            public void run() {
                databaseManager.updatePublic(uuid, name, isPublic);
            }
        }.runTaskAsynchronously(plugin);
    }
    
    public boolean isPublic(UUID uuid, String name) {
        if (publicCache.containsKey(uuid) && publicCache.get(uuid).containsKey(name)) {
            return publicCache.get(uuid).get(name);
        }
        return databaseManager.isPublic(uuid, name);
    }
    
    // Async delete home
    public void deleteHome(UUID uuid, String name) {
        // Update cache immediately
        if (homeCache.containsKey(uuid)) {
            homeCache.get(uuid).remove(name);
        }
        if (publicCache.containsKey(uuid)) {
            publicCache.get(uuid).remove(name);
        }
        
        // Delete from DB asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                databaseManager.deleteHome(uuid, name);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void deleteHome(Player player, String name) {
        deleteHome(player.getUniqueId(), name);
    }

    // Get from cache (Fast)
    public Location getHome(Player player, String name) {
        return getHome(player.getUniqueId(), name);
    }
    
    public Location getHome(UUID uuid, String name) {
        if (homeCache.containsKey(uuid)) {
            return homeCache.get(uuid).get(name);
        }
        // Fallback to DB if not cached (e.g. startup/reload issues), but sync call warning
        return databaseManager.getHome(uuid, name);
    }

    // Get from cache (Fast)
    public Map<String, Location> getHomes(Player player) {
        return getHomes(player.getUniqueId());
    }
    
    public Map<String, Location> getHomes(UUID uuid) {
        if (homeCache.containsKey(uuid)) {
            return new HashMap<>(homeCache.get(uuid));
        }
        // Fallback
        return databaseManager.getHomes(uuid);
    }

    public boolean hasHome(Player player, String name) {
        return getHomes(player).containsKey(name);
    }

    public int getMaxHomes(Player player) {
        // Check permissions from 100 down to 1
        // Note: OPs usually have all permissions, so they might match homes.limit.100 if we check loop first.
        // So we should check specific OP config first if we want to control OP limit separately.
        
        if (player.isOp()) {
            return plugin.getConfig().getInt("settings.op-home-limit", 100);
        }

        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("homes.limit." + i)) {
                return i;
            }
        }
        // Fallback to config default
        return plugin.getConfig().getInt("settings.default-home-limit", 1);
    }

    public boolean canSetHome(Player player) {
        int current = getHomes(player).size();
        int max = getMaxHomes(player);
        return current < max;
    }
    
    // For reload command
    public void reload() {
        // Reload DB config if needed, or just clear cache and reload online players
        // Re-init DB connection might be complex, assuming config change requires restart for DB
        // But for other settings, we can just clear cache and re-fetch for online players
        homeCache.clear();
        publicCache.clear();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            loadHomes(p.getUniqueId());
        }
    }
    
    // Get list of names of players who have at least one public home (or any home? vhome checks public anyway)
    // To be efficient, this should probably come from DB query or cache.
    // For now, let's just return online players + cached offline players?
    // Or query DB for "SELECT DISTINCT uuid FROM homes WHERE is_public = true"
    // Let's implement a method in DatabaseManager to get players with public homes.
    public List<String> getPlayersWithPublicHomes() {
        return databaseManager.getPlayersWithPublicHomes();
    }
}
