package com.example.homes.util;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

/**
 * プレイヤーヘッドを、すでにテクスチャを持っているプロフィールだけで組み立てる。
 * UUID だけの未完成プロフィールを頭に付けると Paper が Mojang へ問い合わせ、
 * /vhome のように人数分並ぶと 429 の警告が連発する。
 */
public final class PlayerHeads {

    private static final ConcurrentMap<UUID, PlayerProfile> TEXTURE_CACHE = new ConcurrentHashMap<>();

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
        applyProfile(meta, resolveTexturedProfile(uuid, name));
    }

    public static void applySkin(SkullMeta meta, PlayerProfile profile) {
        if (meta == null || profile == null) {
            return;
        }
        if (hasTextures(profile)) {
            remember(profile);
            applyProfile(meta, copy(profile));
            return;
        }
        UUID uuid = uuidOf(profile);
        if (uuid != null) {
            applySkin(meta, uuid, nameOf(profile));
        }
    }

    public static void remember(Player player) {
        if (player == null) {
            return;
        }
        try {
            remember(player.getPlayerProfile());
        } catch (RuntimeException ignored) {
        }
    }

    public static void remember(PlayerProfile profile) {
        if (!hasTextures(profile)) {
            return;
        }
        UUID uuid = uuidOf(profile);
        if (uuid == null) {
            return;
        }
        PlayerProfile copy = copy(profile);
        if (copy != null) {
            TEXTURE_CACHE.put(uuid, copy);
        }
    }

    public static void clearCache() {
        TEXTURE_CACHE.clear();
    }

    private static PlayerProfile resolveTexturedProfile(UUID uuid, String name) {
        PlayerProfile cached = TEXTURE_CACHE.get(uuid);
        if (hasTextures(cached)) {
            return copy(cached);
        }

        PlayerProfile live = liveProfile(uuid);
        if (hasTextures(live)) {
            remember(live);
            return copy(live);
        }

        PlayerProfile fromCache = fromServerCache(uuid, name);
        if (hasTextures(fromCache)) {
            remember(fromCache);
            return copy(fromCache);
        }
        return null;
    }

    private static PlayerProfile liveProfile(UUID uuid) {
        try {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && Bukkit.getServer().isOwnedByCurrentRegion(online)) {
                return online.getPlayerProfile();
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private static PlayerProfile fromServerCache(UUID uuid, String name) {
        try {
            String profileName = (name == null || name.isBlank()) ? "Unknown" : name;
            com.destroystokyo.paper.profile.PlayerProfile paper;
            try {
                paper = Bukkit.createProfile(uuid, profileName);
            } catch (RuntimeException ignored) {
                paper = Bukkit.createProfile(uuid);
            }
            if (paper.hasTextures()) {
                return paper;
            }
            if (paper.completeFromCache() && paper.hasTextures()) {
                return paper;
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private static boolean hasTextures(PlayerProfile profile) {
        if (profile == null) {
            return false;
        }
        try {
            if (profile instanceof com.destroystokyo.paper.profile.PlayerProfile paper) {
                return paper.hasTextures();
            }
            return !profile.getTextures().isEmpty();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static UUID uuidOf(PlayerProfile profile) {
        if (profile instanceof com.destroystokyo.paper.profile.PlayerProfile paper && paper.getId() != null) {
            return paper.getId();
        }
        try {
            return profile.getUniqueId();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String nameOf(PlayerProfile profile) {
        try {
            return profile.getName();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void applyProfile(SkullMeta meta, PlayerProfile profile) {
        if (meta == null || profile == null) {
            return;
        }
        try {
            if (profile instanceof com.destroystokyo.paper.profile.PlayerProfile paper) {
                meta.setPlayerProfile(paper);
            } else {
                meta.setOwnerProfile(profile);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static PlayerProfile copy(PlayerProfile profile) {
        if (profile == null) {
            return null;
        }
        try {
            return profile.clone();
        } catch (RuntimeException ignored) {
            return profile;
        }
    }
}
