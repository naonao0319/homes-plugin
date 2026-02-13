package com.example.homes.manager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.example.homes.HomesPlugin;

public class DeathListener implements Listener {

    private final HomesPlugin plugin;
    private final TpaManager tpaManager;

    public DeathListener(HomesPlugin plugin, TpaManager tpaManager) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (plugin.getConfig().getBoolean("settings.back.save-death-location", true)) {
            // Save death location to /back
            tpaManager.saveLastLocation(event.getEntity());
            // Optional: send message?
            // event.getEntity().sendMessage(ChatColor.YELLOW + "死亡地点が /back に保存されました。");
        }
    }
}
