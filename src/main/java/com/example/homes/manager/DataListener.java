package com.example.homes.manager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DataListener implements Listener {

    private final HomeManager homeManager;
    private final PublicHomeEarningsManager earningsManager;

    public DataListener(HomeManager homeManager, PublicHomeEarningsManager earningsManager) {
        this.homeManager = homeManager;
        this.earningsManager = earningsManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        homeManager.loadHomes(event.getPlayer().getUniqueId());
        if (earningsManager != null) {
            earningsManager.settleOnJoin(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        homeManager.unloadHomes(event.getPlayer().getUniqueId());
    }
}
