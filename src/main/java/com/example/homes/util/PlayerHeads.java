package com.example.homes.util;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

/**
 * プレイヤーヘッドを Mojang Skin API (PlayerProfile) で組み立てる。
 * 同一リージョンにいるオンラインプレイヤーはライブプロファイルを使い、
 * Geyser 等で差し込まれたスキンも拾う。それ以外は UUID から Skin API で解決する。
 */
public final class PlayerHeads {

    private PlayerHeads() {
    }

    public static ItemStack of(UUID uuid, String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (!(head.getItemMeta() instanceof SkullMeta meta) || uuid == null) {
            return head;
        }
        applySkin(meta, uuid, name);
        head.setItemMeta(meta);
        return head;
    }

    public static void applySkin(SkullMeta meta, UUID uuid, String name) {
        if (meta == null || uuid == null) {
            return;
        }
        try {
            PlayerProfile profile = profileOf(uuid, name);
            if (profile != null) {
                meta.setOwnerProfile(profile);
                return;
            }
        } catch (RuntimeException ignored) {
        }
        try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        } catch (RuntimeException ignored) {
        }
    }

    public static PlayerProfile profileOf(UUID uuid, String name) {
        try {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && Bukkit.getServer().isOwnedByCurrentRegion(online)) {
                return online.getPlayerProfile();
            }
        } catch (RuntimeException ignored) {
        }
        String profileName = (name == null || name.isBlank()) ? "Unknown" : name;
        try {
            return Bukkit.createPlayerProfile(uuid, profileName);
        } catch (RuntimeException ignored) {
            try {
                return Bukkit.createPlayerProfile(uuid);
            } catch (RuntimeException ignoredAgain) {
                return null;
            }
        }
    }
}
