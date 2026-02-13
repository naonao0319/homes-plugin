package com.example.homes.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class UpdateChecker implements Listener {

    private final JavaPlugin plugin;
    private final String githubRepo; // e.g. "naonao0319/homes-plugin"
    private String latestVersion;

    public UpdateChecker(JavaPlugin plugin, String githubRepo) {
        this.plugin = plugin;
        this.githubRepo = githubRepo;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // GitHub API for latest release
                URL url = new URL("https://api.github.com/repos/" + githubRepo + "/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "HomesPlugin-UpdateChecker"); // GitHub requires User-Agent

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
                    reader.close();
                    
                    // Simple JSON parsing to find "tag_name"
                    String json = response.toString();
                    String tagKey = "\"tag_name\":";
                    int tagIndex = json.indexOf(tagKey);
                    if (tagIndex != -1) {
                        int start = json.indexOf("\"", tagIndex + tagKey.length()) + 1;
                        int end = json.indexOf("\"", start);
                        String version = json.substring(start, end);
                        
                        // Strip 'v' prefix if present (e.g., v1.5.2 -> 1.5.2)
                        if (version.startsWith("v")) {
                            version = version.substring(1);
                        }
                        
                        this.latestVersion = version;
                        
                        String currentVersion = plugin.getDescription().getVersion();
                        if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                            plugin.getLogger().info("新しいバージョンが見つかりました: " + latestVersion + " (現在: " + currentVersion + ")");
                            plugin.getLogger().info("ダウンロード: https://github.com/" + githubRepo + "/releases");
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("アップデートの確認に失敗しました: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() && latestVersion != null) {
            String currentVersion = plugin.getDescription().getVersion();
            if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                player.sendMessage(ChatColor.GOLD + "[HomesPlugin] " + ChatColor.GREEN + "新しいバージョンが利用可能です！");
                player.sendMessage(ChatColor.YELLOW + "現在: " + currentVersion + ChatColor.GRAY + " -> " + ChatColor.AQUA + latestVersion);
                player.sendMessage(ChatColor.YELLOW + "ダウンロード: " + ChatColor.UNDERLINE + "https://github.com/" + githubRepo + "/releases");
            }
        }
    }
}
