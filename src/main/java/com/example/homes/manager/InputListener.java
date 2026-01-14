package com.example.homes.manager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.example.homes.HomesPlugin;
import com.example.homes.gui.HomeGUI;

public class InputListener implements Listener {

    private final HomesPlugin plugin;
    private final HomeManager homeManager;
    private final SoundManager soundManager;
    private final Set<UUID> creatingHome = new HashSet<>();
    private HomeGUI homeGUI; 

    public InputListener(HomesPlugin plugin, HomeManager homeManager, SoundManager soundManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.soundManager = soundManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setHomeGUI(HomeGUI homeGUI) {
        this.homeGUI = homeGUI;
    }

    public void startCreation(Player player) {
        if (!homeManager.canSetHome(player)) {
            player.sendMessage(plugin.getMessage("max-homes-reached").replace("{max}", String.valueOf(homeManager.getMaxHomes(player))));
            return;
        }
        
        creatingHome.add(player.getUniqueId());
        player.sendMessage(plugin.getMessage("enter-name"));
        player.sendMessage(plugin.getMessage("cancel-info"));
        player.closeInventory();
        soundManager.play(player, "gui-click"); // Or some start sound
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!creatingHome.contains(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();

        if (message.equalsIgnoreCase("cancel")) {
            creatingHome.remove(player.getUniqueId());
            player.sendMessage(plugin.getMessage("creation-cancelled"));
            soundManager.play(player, "gui-click");
            return;
        }

        if (homeManager.hasHome(player, message)) {
            player.sendMessage(plugin.getMessage("home-exists"));
            soundManager.play(player, "teleport-fail");
            return;
        }

        // Run on main thread to safely modify world/data
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Check limit again just in case
            if (!homeManager.canSetHome(player)) {
                player.sendMessage(plugin.getMessage("max-homes-reached").replace("{max}", String.valueOf(homeManager.getMaxHomes(player))));
                creatingHome.remove(player.getUniqueId());
                return;
            }

            homeManager.setHome(player, message, player.getLocation());
            player.sendMessage(plugin.getMessage("home-created").replace("{name}", message));
            soundManager.play(player, "teleport-success"); // Or home-created sound
            creatingHome.remove(player.getUniqueId());
            
            // Optionally reopen GUI
            if (homeGUI != null) {
                homeGUI.open(player);
            }
        });
    }
}
