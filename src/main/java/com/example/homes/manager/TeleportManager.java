package com.example.homes.manager;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.homes.HomesPlugin;
import com.example.homes.manager.SoundManager;
import com.example.homes.manager.TpaManager;

public class TeleportManager {

    private final HomesPlugin plugin;
    private final SoundManager soundManager;
    private final TpaManager tpaManager;

    public TeleportManager(HomesPlugin plugin, SoundManager soundManager, TpaManager tpaManager) {
        this.plugin = plugin;
        this.soundManager = soundManager;
        this.tpaManager = tpaManager;
    }

    public void teleport(Player player, Location target) {
        teleport(player, (Object) target);
    }
    
    public void teleport(Player player, Player target) {
        teleport(player, (Object) target);
    }

    private void teleport(Player player, Object target) {
        // Save current location to back before teleporting
        if (tpaManager != null) {
            tpaManager.saveLastLocation(player);
        }
        
        // 5 seconds delay fixed (or config)
        int delay = 5; 
        
        if (delay <= 0) {
            doTeleport(player, target);
            player.sendMessage(plugin.getMessage("teleport-success"));
            soundManager.play(player, "teleport-success");
            return;
        }
        
        player.sendMessage(ChatColor.YELLOW + String.valueOf(delay) + "秒後にテレポートします。動かないでください。");
        
        Location initialLoc = player.getLocation();
        
        new BukkitRunnable() {
            int timeLeft = delay;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                // Check movement
                if (player.getLocation().distance(initialLoc) > 0.1) {
                    player.sendMessage(plugin.getMessage("teleport-cancelled"));
                    soundManager.play(player, "teleport-fail");
                    this.cancel();
                    return;
                }
                
                if (timeLeft <= 0) {
                    doTeleport(player, target);
                    player.sendMessage(plugin.getMessage("teleport-success"));
                    soundManager.play(player, "teleport-success");
                    this.cancel();
                } else {
                    player.sendTitle(ChatColor.GREEN + String.valueOf(timeLeft), "", 0, 20, 0);
                    soundManager.play(player, "teleport-count");
                    timeLeft--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void doTeleport(Player player, Object target) {
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            if (targetPlayer.isOnline()) {
                player.teleport(targetPlayer.getLocation());
                playTeleportEffect(player);
            } else {
                player.sendMessage(ChatColor.RED + "テレポート先が見つかりません。");
            }
        } else if (target instanceof Location) {
            player.teleport((Location) target);
            playTeleportEffect(player);
        }
    }

    private void playTeleportEffect(Player player) {
        // Sound and Particles on arrival
        Location loc = player.getLocation();
        // Increase count and spread for better visibility
        player.getWorld().spawnParticle(Particle.PORTAL, loc.add(0, 1, 0), 100, 0.5, 1, 0.5);
        player.getWorld().spawnParticle(Particle.END_ROD, loc, 50, 0.5, 1, 0.5); // Add End Rod for visibility
        player.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
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
