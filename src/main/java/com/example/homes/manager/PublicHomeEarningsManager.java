package com.example.homes.manager;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import com.example.homes.HomesPlugin;
import com.example.homes.database.HomeRepository;

/**
 * 公開ホーム訪問料を持ち主へ渡す。オンラインなら即入金、オフラインなら DB に積み立てて
 * 次回ログイン時に入金＋チャット通知する。
 */
public class PublicHomeEarningsManager {

    private final HomesPlugin plugin;
    private final HomeRepository repository;

    public PublicHomeEarningsManager(HomesPlugin plugin, HomeRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void credit(UUID ownerUuid, double amount) {
        if (ownerUuid == null || amount <= 0) {
            return;
        }

        Player owner = plugin.getServer().getPlayer(ownerUuid);
        if (owner != null && owner.isOnline()) {
            boolean scheduled = plugin.getFoliaScheduler().runEntity(owner, () -> {
                if (!owner.isOnline()) {
                    persistPending(ownerUuid, amount);
                    return;
                }
                depositAndNotify(owner, amount, "public-home-earned");
            });
            if (!scheduled) {
                persistPending(ownerUuid, amount);
            }
            return;
        }

        persistPending(ownerUuid, amount);
    }

    public void settleOnJoin(Player player) {
        if (player == null || !plugin.isEnabled()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        plugin.getFoliaScheduler().runAsync(() -> {
            if (!plugin.isEnabled()) {
                return;
            }
            double amount;
            try {
                amount = repository.takePendingEarnings(uuid);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load pending public home earnings", e);
                return;
            }
            if (amount <= 0) {
                return;
            }
            plugin.getFoliaScheduler().runEntity(player, () -> {
                if (!player.isOnline()) {
                    persistPending(uuid, amount);
                    return;
                }
                depositAndNotify(player, amount, "public-home-earned-offline");
            }, () -> persistPending(uuid, amount));
        });
    }

    private void depositAndNotify(Player owner, double amount, String messageKey) {
        EconomyManager economy = plugin.getEconomyManager();
        economy.deposit(owner, amount);
        owner.sendMessage(plugin.msg(messageKey, "amount", economy.format(amount)));
    }

    private void persistPending(UUID ownerUuid, double amount) {
        plugin.getFoliaScheduler().runAsync(() -> {
            try {
                repository.addPendingEarnings(ownerUuid, amount);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to store pending public home earnings for " + ownerUuid, e);
            }
        });
    }
}
