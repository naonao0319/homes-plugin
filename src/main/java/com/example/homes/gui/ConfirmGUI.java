package com.example.homes.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.example.homes.HomesPlugin;
import com.example.homes.manager.HomeManager;
import com.example.homes.manager.SoundManager;
import java.util.UUID;

public class ConfirmGUI implements Listener {

    private final HomesPlugin plugin;
    private final HomeManager homeManager;
    private final HomeGUI homeGUI;
    private final String targetHome;
    private final UUID targetUUID;
    private final SoundManager soundManager;

    public ConfirmGUI(HomesPlugin plugin, HomeManager homeManager, HomeGUI homeGUI, String targetHome, SoundManager soundManager, UUID targetUUID) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.homeGUI = homeGUI;
        this.targetHome = targetHome;
        this.soundManager = soundManager;
        this.targetUUID = targetUUID;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public ConfirmGUI(HomesPlugin plugin, HomeManager homeManager, HomeGUI homeGUI, String targetHome, SoundManager soundManager) {
        this(plugin, homeManager, homeGUI, targetHome, soundManager, null);
    }

    public void open(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.confirm-delete.title", "&c本当に削除しますか？"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Yes Button (Slot 11)
        ItemStack yesItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta yesMeta = yesItem.getItemMeta();
        if (yesMeta != null) {
            yesMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.confirm-delete.yes-button.name", "&aはい、削除します")));
            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("gui.confirm-delete.yes-button.lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            yesMeta.setLore(lore);
            yesItem.setItemMeta(yesMeta);
        }
        inv.setItem(11, yesItem);

        // No Button (Slot 15)
        ItemStack noItem = new ItemStack(Material.RED_WOOL);
        ItemMeta noMeta = noItem.getItemMeta();
        if (noMeta != null) {
            noMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.confirm-delete.no-button.name", "&cいいえ、キャンセルします")));
            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("gui.confirm-delete.no-button.lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            noMeta.setLore(lore);
            noItem.setItemMeta(noMeta);
        }
        inv.setItem(15, noItem);

        // Info Item (Slot 13)
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.YELLOW + "削除対象: " + ChatColor.GOLD + targetHome);
            infoItem.setItemMeta(infoMeta);
        }
        inv.setItem(13, infoItem);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.confirm-delete.title", "&c本当に削除しますか？"));
        if (!event.getView().getTitle().equals(title)) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Yes (Delete)
        if (slot == 11) {
            UUID uuid = targetUUID != null ? targetUUID : player.getUniqueId();
            homeManager.deleteHome(uuid, targetHome);
            player.sendMessage(plugin.getMessage("home-deleted").replace("{name}", targetHome));
            soundManager.play(player, "delete-success");
            
            // Unregister listener and return to HomeGUI
            InventoryClickEvent.getHandlerList().unregister(this);
            // Re-open target's GUI
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
                homeGUI.open(player, target);
            } else {
                homeGUI.open(player); // Fallback
            }
        }

        // No (Cancel)
        if (slot == 15) {
            soundManager.play(player, "gui-click");
            
            // Unregister listener and return to HomeGUI
            InventoryClickEvent.getHandlerList().unregister(this);
             // Re-open target's GUI
            UUID uuid = targetUUID != null ? targetUUID : player.getUniqueId();
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
                homeGUI.open(player, target);
            } else {
                homeGUI.open(player); // Fallback
            }
        }
    }
}
