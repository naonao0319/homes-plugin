package com.example.homes.manager;

import com.example.homes.HomesPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final HomesPlugin plugin;
    private Economy economy = null;

    public EconomyManager(HomesPlugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public boolean hasMoney(String playerName, double amount) {
        if (!hasEconomy()) return true; // Free if no economy
        return economy.has(playerName, amount);
    }

    public void withdraw(String playerName, double amount) {
        if (!hasEconomy()) return;
        economy.withdrawPlayer(playerName, amount);
    }
    
    public String format(double amount) {
        if (!hasEconomy()) return String.valueOf(amount);
        return economy.format(amount);
    }
}
