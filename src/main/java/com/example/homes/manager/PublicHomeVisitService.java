package com.example.homes.manager;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.example.homes.HomesPlugin;

/**
 * 公開ホーム（または他人のホーム）へのテレポートと訪問料の徴収・分配を行う。
 */
public class PublicHomeVisitService {

    private final HomesPlugin plugin;
    private final HomeManager homeManager;
    private final EconomyManager economyManager;
    private final TeleportManager teleportManager;

    public PublicHomeVisitService(
            HomesPlugin plugin,
            HomeManager homeManager,
            EconomyManager economyManager,
            TeleportManager teleportManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.economyManager = economyManager;
        this.teleportManager = teleportManager;
    }

    /**
     * 指定ホームへテレポートを開始する。
     * @return テレポート開始（または非同期ロード開始）したら true。残高不足なら false（GUI を開いたままにする）。
     */
    public boolean visit(Player visitor, UUID ownerUuid, String homeName) {
        if (visitor == null || ownerUuid == null || homeName == null) {
            return false;
        }

        if (!homeManager.isLoaded(ownerUuid)) {
            visitor.sendMessage(plugin.msg("loading-homes"));
            homeManager.ensureLoaded(ownerUuid).thenRun(() ->
                    plugin.getFoliaScheduler().runEntity(visitor, () -> visit(visitor, ownerUuid, homeName)));
            return true;
        }

        Location loc = homeManager.getHome(ownerUuid, homeName);
        if (loc == null) {
            visitor.sendMessage(plugin.msg("home-not-found", "name", homeName));
            return false;
        }

        boolean ownHome = visitor.getUniqueId().equals(ownerUuid);
        boolean publicHome = homeManager.isPublic(ownerUuid, homeName);
        if (!ownHome && publicHome) {
            if (!economyManager.charge(visitor, "visit-public")) {
                return false;
            }
            double payout = economyManager.chargedAmount(visitor, "visit-public");
            UUID payoutOwner = payout > 0 ? ownerUuid : null;
            teleportManager.teleport(
                    visitor, loc, TeleportPayment.visitPublic("visit-public", payoutOwner, payout));
            return true;
        }

        if (!economyManager.charge(visitor, "teleport")) {
            return false;
        }
        teleportManager.teleport(visitor, loc, TeleportPayment.refundable("teleport"));
        return true;
    }
}
