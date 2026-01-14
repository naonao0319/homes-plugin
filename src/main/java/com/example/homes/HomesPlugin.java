package com.example.homes;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.example.homes.gui.HomeGUI;
import com.example.homes.manager.DataListener;
import com.example.homes.manager.EconomyManager;
import com.example.homes.manager.HomeManager;
import com.example.homes.manager.HomeTabCompleter;
import com.example.homes.manager.InputListener;
import com.example.homes.manager.SoundManager;
import com.example.homes.manager.TeleportManager;

public class HomesPlugin extends JavaPlugin {

    private HomeManager homeManager;
    private TeleportManager teleportManager;
    private HomeGUI homeGUI;
    private InputListener inputListener;
    private SoundManager soundManager;
    private EconomyManager economyManager;
    @SuppressWarnings("unused")
    private DataListener dataListener;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize Managers
        this.soundManager = new SoundManager(this);
        this.homeManager = new HomeManager(this);
        this.teleportManager = new TeleportManager(this, soundManager);
        this.inputListener = new InputListener(this, homeManager, soundManager);
        this.homeGUI = new HomeGUI(this, homeManager, teleportManager, soundManager, economyManager);
        this.dataListener = new DataListener(this, homeManager);
        
        // Link GUI and Input Listener
        this.homeGUI.setInputListener(inputListener);
        this.inputListener.setHomeGUI(homeGUI);

        // Register TabCompleter
        HomeTabCompleter tabCompleter = new HomeTabCompleter(homeManager);
        getCommand("home").setTabCompleter(tabCompleter);
        getCommand("homes").setTabCompleter(tabCompleter);
        getCommand("sethome").setTabCompleter(tabCompleter);
        getCommand("delhome").setTabCompleter(tabCompleter);

        getLogger().info("HomesPlugin が有効になりました！");
    }

    @Override
    public void onDisable() {
        if (homeManager != null) {
            homeManager.close();
        }
        getLogger().info("HomesPlugin が無効になりました！");
    }

    public String getMessage(String key) {
        String msg = getConfig().getString("messages." + key);
        if (msg == null) return "Message not found: " + key;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // /homes reload
        if (command.getName().equalsIgnoreCase("homes") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("homes.reload") && !sender.isOp()) {
                sender.sendMessage(getMessage("no-permission"));
                return true;
            }
            reloadConfig();
            homeManager.reload();
            sender.sendMessage(getMessage("reload-success"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("only-player"));
            return true;
        }

        Player player = (Player) sender;

        // /sethome <name>
        if (command.getName().equalsIgnoreCase("sethome")) {
            if (args.length == 0) {
                player.sendMessage(getMessage("usage-sethome"));
                return true;
            }

            String homeName = args[0];
            if (homeManager.hasHome(player, homeName)) {
                player.sendMessage(getMessage("home-exists"));
                return true;
            }

            // Check economy for creating home
            if (economyManager != null && economyManager.hasEconomy()) {
                double cost = getConfig().getDouble("economy.cost.set-home", 0);
                if (cost > 0 && !economyManager.hasMoney(player.getName(), cost)) {
                    player.sendMessage(getMessage("insufficient-funds").replace("{cost}", economyManager.format(cost)));
                    return true;
                }
                if (cost > 0) {
                    economyManager.withdraw(player.getName(), cost);
                    player.sendMessage(getMessage("payment-success").replace("{cost}", economyManager.format(cost)));
                }
            }

            homeManager.setHome(player, homeName, player.getLocation());
            player.sendMessage(getMessage("home-set").replace("{name}", homeName));
            // soundManager.play(player, "home-created"); // Optional
            return true;
        }

        // /delhome <name>
        if (command.getName().equalsIgnoreCase("delhome")) {
            if (args.length == 0) {
                player.sendMessage(getMessage("usage-delhome"));
                return true;
            }

            String homeName = args[0];
            if (!homeManager.hasHome(player, homeName)) {
                player.sendMessage(getMessage("home-not-found").replace("{name}", homeName));
                return true;
            }

            homeManager.deleteHome(player, homeName);
            player.sendMessage(getMessage("home-deleted").replace("{name}", homeName));
            soundManager.play(player, "delete-success");
            return true;
        }

        // /home <name> - Teleport directly
        if (command.getName().equalsIgnoreCase("home")) {
            
            if (args.length == 0) {
                player.sendMessage(getMessage("usage-home"));
                player.sendMessage(getMessage("use-gui-info"));
                return true;
            }

            String homeName = args[0];
            
            // Check own home first
            if (homeManager.hasHome(player, homeName)) {
                // Teleport cost
                 if (economyManager != null && economyManager.hasEconomy()) {
                     double cost = getConfig().getDouble("economy.cost.teleport", 0);
                     if (cost > 0 && !economyManager.hasMoney(player.getName(), cost)) {
                        player.sendMessage(getMessage("insufficient-funds").replace("{cost}", economyManager.format(cost)));
                        return true;
                    }
                    if (cost > 0) {
                        economyManager.withdraw(player.getName(), cost);
                        player.sendMessage(getMessage("payment-success").replace("{cost}", economyManager.format(cost)));
                    }
                }
                
                Location loc = homeManager.getHome(player, homeName);
                if (loc != null) {
                    teleportManager.teleport(player, loc);
                }
                return true;
            } 
            
            if (homeName.contains(":")) {
                String[] parts = homeName.split(":");
                if (parts.length > 1) {
                    org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(parts[0]);
                    String targetHome = parts[1];
                    
                    // Check if public or has permission
                    boolean isPublic = homeManager.isPublic(target.getUniqueId(), targetHome);
                    boolean isAdmin = player.hasPermission("homes.admin");
                    
                    if (isPublic || isAdmin) {
                        Location loc = homeManager.getHome(target.getUniqueId(), targetHome);
                        if (loc != null) {
                            teleportManager.teleport(player, loc);
                            return true;
                        }
                    } else {
                        player.sendMessage(getMessage("home-not-found").replace("{name}", homeName));
                        return true;
                    }
                }
            }

            player.sendMessage(getMessage("home-not-found").replace("{name}", homeName));
            player.sendMessage(getMessage("use-gui-info"));
            return true;
        }

        // /homes [list] [player]
        if (command.getName().equalsIgnoreCase("homes")) {
            
            // /homes list
            if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
                Map<String, Location> homes = homeManager.getHomes(player);
                if (homes.isEmpty()) {
                    player.sendMessage(getMessage("no-homes"));
                } else {
                    player.sendMessage(ChatColor.GOLD + "=== " + getConfig().getString("gui.title", "Home List") + " ===");
                    for (String name : homes.keySet()) {
                        Location loc = homes.get(name);
                        player.sendMessage(ChatColor.YELLOW + "- " + name + ChatColor.GRAY + " (" + 
                                loc.getWorld().getName() + ": " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
                    }
                }
                return true;
            }
            
            // /homes <player> (View other's homes)
            if (args.length > 0 && !args[0].equalsIgnoreCase("reload")) {
                // Allow viewing other players (GUI filters public/private)
                
                String targetName = args[0];
                Player target = Bukkit.getPlayer(targetName);
                if (target == null) {
                    sender.sendMessage(getMessage("player-not-found"));
                    return true;
                }
                
                // If viewing self, just use standard open
                if (target.getUniqueId().equals(player.getUniqueId())) {
                    homeGUI.open(player);
                    return true;
                }
                
                // Message based on permission
                if (sender.hasPermission("homes.admin")) {
                    sender.sendMessage(getMessage("admin-view").replace("{player}", target.getName()));
                } else {
                    sender.sendMessage(ChatColor.GREEN + target.getName() + "の公開ホームを表示します。");
                }
                
                homeGUI.open(player, target);
                return true;
            }

            homeGUI.open(player);
            return true;
        }

        return false;
    }
}
