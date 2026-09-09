package com.example.homes.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

class PlayerHeadsTest {

    private static final String TEXTURE_VALUE = "e3RleHR1cmVzfQ==";

    @BeforeEach
    void setUp() {
        PlayerHeads.clearCache();
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        PlayerHeads.clearCache();
    }

    @Test
    void doesNotAttachIncompleteProfilesThatWouldTriggerMojangLookups() {
        UUID missing = UUID.fromString("d53e17f8-20f9-4f52-bcbf-c3e6d7e8f91b");

        ItemStack head = PlayerHeads.of(missing, "OfflinePlayer");

        assertEquals(Material.PLAYER_HEAD, head.getType());
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertNotNull(meta);
        assertNull(meta.getOwnerProfile(),
                "incomplete profile must not be set (Paper would fetch Mojang and 429)");
        assertFalse(meta.hasOwner());
    }

    @Test
    void reusesCachedTexturedProfileForOfflineOwners() {
        UUID id = UUID.fromString("5b05acbb-66f8-4398-87da-f868a54dfb8a");
        PlayerProfile profile = Bukkit.createProfile(id, "Cached");
        profile.setProperty(new ProfileProperty("textures", TEXTURE_VALUE));
        PlayerHeads.remember(profile);

        ItemStack head = PlayerHeads.of(id, "Cached");

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertNotNull(meta);
        assertTrue(meta.hasOwner());
        assertTrue(meta.getPlayerProfile().hasTextures());
        assertEquals(id, meta.getPlayerProfile().getId());
    }

    @Test
    void duplicateOwnersShareTheCachedProfileInsteadOfNewLookups() {
        UUID id = UUID.fromString("56f34019-a928-4c03-8091-733364d1bd00");
        PlayerProfile profile = Bukkit.createProfile(id, "Owner");
        profile.setProperty(new ProfileProperty("textures", TEXTURE_VALUE));
        PlayerHeads.remember(profile);

        ItemStack first = PlayerHeads.of(id, "Owner");
        ItemStack second = PlayerHeads.of(id, "Owner");

        assertTrue(((SkullMeta) first.getItemMeta()).getPlayerProfile().hasTextures());
        assertTrue(((SkullMeta) second.getItemMeta()).getPlayerProfile().hasTextures());
    }
}
