package com.example.homes.manager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DataListener implements Listener {

    private final HomeManager homeManager;

    public DataListener(JavaPlugin plugin, HomeManager homeManager) {
        this.homeManager = homeManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        homeManager.loadHomes(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        homeManager.unloadHomes(event.getPlayer().getUniqueId());
    }
}
