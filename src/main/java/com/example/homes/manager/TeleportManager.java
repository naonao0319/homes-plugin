package com.example.homes.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.example.homes.HomesPlugin;

public class TeleportManager implements Listener {

    private final HomesPlugin plugin;
    private final SoundManager soundManager;
    private final TpaManager tpaManager;
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();

    public TeleportManager(HomesPlugin plugin, SoundManager soundManager, TpaManager tpaManager) {
        this.plugin = plugin;
        this.soundManager = soundManager;
        this.tpaManager = tpaManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void teleport(Player player, Location target) {
        // Save current location to back before teleporting
        if (tpaManager != null) {
            tpaManager.saveLastLocation(player);
        }
        if (pendingTeleports.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("teleport-already-in-progress"));
            return;
        }

        int seconds = plugin.getConfig().getInt("settings.teleport-delay", 5);
        player.sendMessage(plugin.getMessage("teleport-start").replace("{seconds}", String.valueOf(seconds)));

        BukkitTask task = new BukkitRunnable() {
            int timeLeft = seconds;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    player.teleport(target);
                    player.sendMessage(plugin.getMessage("teleport-success"));
                    soundManager.play(player, "teleport-success");
                    pendingTeleports.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                // Show countdown title/actionbar
                String title = ChatColor.YELLOW + String.valueOf(timeLeft);
                String subtitle = ChatColor.GRAY + "テレポートまで...";
                player.sendTitle(title, subtitle, 0, 25, 5);
                soundManager.play(player, "teleport-count", 1f, 2f);

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        pendingTeleports.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (pendingTeleports.containsKey(player.getUniqueId())) {
            // Check if actually moved block
            if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()
                    || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                
                cancelTeleport(player);
                player.sendMessage(plugin.getMessage("teleport-cancelled"));
                player.sendTitle("", "", 0, 0, 0); // Clear title
                soundManager.play(player, "teleport-fail");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelTeleport(event.getPlayer());
    }

    private void cancelTeleport(Player player) {
        if (pendingTeleports.containsKey(player.getUniqueId())) {
            pendingTeleports.get(player.getUniqueId()).cancel();
            pendingTeleports.remove(player.getUniqueId());
        }
    }
}
