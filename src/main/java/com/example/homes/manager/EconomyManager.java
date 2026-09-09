package com.example.homes.manager;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.example.homes.HomesPlugin;

import net.milkbowl.vault.economy.Economy;

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
        if (!plugin.getConfig().getBoolean("economy.enabled", true)) {
            return false;
        }
        return economy != null;
    }

    public boolean hasMoney(OfflinePlayer player, double amount) {
        if (!hasEconomy()) return true; // Free if no economy
        if (player == null) return true;
        return economy.has(player, amount);
    }

    public void withdraw(OfflinePlayer player, double amount) {
        if (!hasEconomy()) return;
        if (player == null) return;
        economy.withdrawPlayer(player, amount);
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (!hasEconomy()) return;
        if (player == null) return;
        economy.depositPlayer(player, amount);
    }
    
    public String format(double amount) {
        if (!hasEconomy()) return String.valueOf(amount);
        return economy.format(amount);
    }

    /**
     * config の economy.cost.&lt;costKey&gt; に設定された費用。
     * visit-public が未設定なら teleport 代をそのまま使う（なければ 10）。
     */
    public double getCost(String costKey) {
        if (costKey == null) return 0;
        if ("visit-public".equals(costKey) && !plugin.getConfig().isSet("economy.cost.visit-public")) {
            return plugin.getConfig().getDouble("economy.cost.teleport", 10.0);
        }
        double fallback = "visit-public".equals(costKey) ? 10.0 : 0.0;
        return plugin.getConfig().getDouble("economy.cost." + costKey, fallback);
    }

    /**
     * {@link #charge(Player, String)} が実際に徴収する額。無料（経済無効・bypass・費用 0 以下）なら 0。
     */
    public double chargedAmount(Player player, String costKey) {
        if (!hasEconomy()) return 0;
        if (player != null && player.hasPermission("homes.bypass.economy")) return 0;
        double cost = getCost(costKey);
        return cost > 0 ? cost : 0;
    }

    /**
     * config の economy.cost.&lt;costKey&gt; に設定された費用を徴収する。
     * 経済が無効・費用 0 以下なら何もせず成功扱い。
     * 残高不足のときはメッセージを送って false、徴収できたら支払いメッセージを送って true を返す。
     */
    public boolean charge(Player player, String costKey) {
        if (!hasEconomy()) return true;
        // homes.bypass.economy 保持者 (既定で OP) は利用料金が無料
        if (player.hasPermission("homes.bypass.economy")) return true;
        double cost = getCost(costKey);
        if (cost <= 0) return true;
        if (!hasMoney(player, cost)) {
            player.sendMessage(plugin.msg("insufficient-funds", "cost", format(cost)));
            return false;
        }
        withdraw(player, cost);
        player.sendMessage(plugin.msg("payment-success", "cost", format(cost)));
        return true;
    }

    /**
     * {@link #charge(Player, String)} で徴収した費用を払い戻す。
     * 徴収条件 (経済有効・homes.bypass.economy なし・費用 &gt; 0) と対称で、
     * 実際に徴収していないケース (無料) では何もしない。
     */
    public void refund(Player player, String costKey) {
        if (costKey == null) return;
        if (!hasEconomy()) return;
        if (player.hasPermission("homes.bypass.economy")) return;
        double cost = getCost(costKey);
        if (cost <= 0) return;
        deposit(player, cost);
        player.sendMessage(plugin.msg("refund-success", "cost", format(cost)));
    }
}
