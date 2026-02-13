package com.example.homes.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class HomeTabCompleter implements TabCompleter {

    private final HomeManager homeManager;

    public HomeTabCompleter(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String cmdName = command.getName().toLowerCase();
            
            // /home <name> or /delhome <name> - suggest existing homes
            if (cmdName.equals("home") || cmdName.equals("delhome")) {
                Map<String, ?> homes = homeManager.getHomes(player);
                completions.addAll(homes.keySet());
            }
            
            // /homes [list]
            if (cmdName.equals("homes")) {
                completions.add("list");
                // Removed player completions for /homes
            }
            
            // /vhome <player>
            if (cmdName.equals("vhome")) {
                for (Player p : player.getServer().getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                     if (op.getName() != null && !completions.contains(op.getName())) {
                         completions.add(op.getName());
                     }
                }
            }
            
            // TPA Commands
            if (cmdName.equals("tpa") || cmdName.equals("tpahere") || cmdName.equals("tpcancel") || cmdName.equals("tpaignore")) {
                for (Player p : player.getServer().getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                     if (op.getName() != null && !completions.contains(op.getName())) {
                         completions.add(op.getName());
                     }
                }
            }
            
            // /sethome <name> - no suggestions usually, maybe "home"
            if (cmdName.equals("sethome")) {
                // No specific suggestions for new name
            }

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
