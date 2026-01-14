package com.example.homes.manager;

import com.example.homes.HomesPlugin;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {

    private final HomesPlugin plugin;

    public SoundManager(HomesPlugin plugin) {
        this.plugin = plugin;
    }

    public void play(Player player, String key) {
        play(player, key, 1f, 1f);
    }

    public void play(Player player, String key, float volume, float pitch) {
        String soundName = plugin.getConfig().getString("sounds." + key);
        if (soundName == null || soundName.equalsIgnoreCase("NONE")) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name in config: " + soundName);
        }
    }
    
    public void playAtLocation(Location loc, String key, float volume, float pitch) {
        String soundName = plugin.getConfig().getString("sounds." + key);
        if (soundName == null || soundName.equalsIgnoreCase("NONE")) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            if (loc.getWorld() != null) {
                loc.getWorld().playSound(loc, sound, volume, pitch);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name in config: " + soundName);
        }
    }
}
