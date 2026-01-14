package com.example.homes.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.example.homes.HomesPlugin;
import com.example.homes.manager.EconomyManager;
import com.example.homes.manager.HomeManager;
import com.example.homes.manager.InputListener;
import com.example.homes.manager.SoundManager;
import com.example.homes.manager.TeleportManager;

public class HomeGUI implements Listener {

    private final HomesPlugin plugin;
    private final HomeManager homeManager;
    private final TeleportManager teleportManager;
    private final SoundManager soundManager;
    private final EconomyManager economyManager;
    private InputListener inputListener;
    private final int GUI_SIZE = 27;
    private final Set<UUID> deleteModePlayers = new HashSet<>();
    private final Set<UUID> publicModePlayers = new HashSet<>();

    public HomeGUI(HomesPlugin plugin, HomeManager homeManager, TeleportManager teleportManager, SoundManager soundManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.teleportManager = teleportManager;
        this.soundManager = soundManager;
        this.economyManager = economyManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setInputListener(InputListener inputListener) {
        this.inputListener = inputListener;
    }

    public void open(Player player) {
        open(player, player);
    }
    
    public void open(Player viewer, Player target) {
        // Floodgate check removed
        
        boolean isOwner = viewer.getUniqueId().equals(target.getUniqueId());
        boolean isAdmin = viewer.hasPermission("homes.admin") && !isOwner;
        
        boolean deleteMode = deleteModePlayers.contains(viewer.getUniqueId());
        boolean publicMode = publicModePlayers.contains(viewer.getUniqueId());
        
        String titleKey = "gui.title";
        if (deleteMode) titleKey = "gui.delete-mode-title";
        else if (publicMode) titleKey = "gui.public-mode-title";
        
        String defaultTitle = "ホーム一覧";
        if (deleteMode) defaultTitle = "&c削除モード (クリックで削除)";
        else if (publicMode) defaultTitle = "&b公開設定モード (クリックで切替)";
        
        if (!isOwner) {
            defaultTitle = target.getName() + "のホーム";
            titleKey = "gui.title-other"; // Custom key if wanted, fallback to defaultTitle
        }
        
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(titleKey, defaultTitle));
        // Ensure title is unique enough or handle in listener
        
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, title);

        // Slot 0: Create Home Button (Top Left) - Only for Owner
        if (isOwner) {
            ItemStack createItem = new ItemStack(Material.ANVIL);
            ItemMeta createMeta = createItem.getItemMeta();
            if (createMeta != null) {
                String name = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.create-button.name", "&aホームを作成する"));
                createMeta.setDisplayName(name);
                List<String> lore = new ArrayList<>();
                for (String line : plugin.getConfig().getStringList("gui.create-button.lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                
                // Show limit info
                int current = homeManager.getHomes(target).size();
                int max = homeManager.getMaxHomes(target);
                lore.add(ChatColor.YELLOW + "現在の作成数: " + current + " / " + max);
                
                // Show cost
                if (economyManager != null && economyManager.hasEconomy()) {
                     double cost = plugin.getConfig().getDouble("economy.cost.set-home", 0);
                     if (cost > 0) {
                         lore.add(ChatColor.GOLD + "費用: " + economyManager.format(cost));
                     }
                }
                
                createMeta.setLore(lore);
                createItem.setItemMeta(createMeta);
            }
            inv.setItem(0, createItem);
        }

        // Slot 8: Delete Mode Button (Top Right) - Only for Owner or Admin
        if (isOwner || isAdmin) {
            ItemStack deleteItem;
            if (deleteMode) {
                deleteItem = new ItemStack(Material.TNT);
            } else {
                deleteItem = new ItemStack(Material.BARRIER);
            }
            ItemMeta deleteMeta = deleteItem.getItemMeta();
            if (deleteMeta != null) {
                String nameKey = deleteMode ? "gui.delete-button.name-on" : "gui.delete-button.name-off";
                String defaultName = deleteMode ? "&c削除モード: ON" : "&a削除モード: OFF";
                deleteMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(nameKey, defaultName)));
                
                List<String> lore = new ArrayList<>();
                String loreKey = deleteMode ? "gui.delete-button.lore-on" : "gui.delete-button.lore-off";
                for (String line : plugin.getConfig().getStringList(loreKey)) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                deleteMeta.setLore(lore);
                deleteItem.setItemMeta(deleteMeta);
            }
            inv.setItem(8, deleteItem);
        }
        
        // Slot 7: Public Mode Button (Next to Delete) - Only for Owner
        if (isOwner) {
            ItemStack publicItem;
            if (publicMode) {
                publicItem = new ItemStack(Material.ENDER_EYE);
            } else {
                publicItem = new ItemStack(Material.ENDER_PEARL);
            }
            ItemMeta publicMeta = publicItem.getItemMeta();
            if (publicMeta != null) {
                String nameKey = publicMode ? "gui.public-button.name-on" : "gui.public-button.name-off";
                String defaultName = publicMode ? "&b公開設定モード: ON" : "&a公開設定モード: OFF";
                publicMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(nameKey, defaultName)));
                
                List<String> lore = new ArrayList<>();
                String loreKey = publicMode ? "gui.public-button.lore-on" : "gui.public-button.lore-off";
                // Default lore if config missing
                List<String> defaultLore = new ArrayList<>();
                if (publicMode) defaultLore.add("&7クリックしてモードを終了");
                else defaultLore.add("&7クリックして公開設定モードに切替");
                
                List<String> configLore = plugin.getConfig().getStringList(loreKey);
                if (configLore.isEmpty()) configLore = defaultLore;
                
                for (String line : configLore) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                publicMeta.setLore(lore);
                publicItem.setItemMeta(publicMeta);
            }
            inv.setItem(7, publicItem);
        }
        
        // Load Homes of Target
        Map<String, Location> homes = homeManager.getHomes(target);
        List<String> visibleHomes = getVisibleHomes(viewer, target, homes);
        
        // Fill slots starting from 9 (Row 2 first slot) to 26 (End of Row 3)
        // Available slots: 9 to 26 = 18 slots
        int startSlot = 9;

        // Get world icons config
        String defaultIcon = plugin.getConfig().getString("gui.home-icon.default-material", "RED_BED");
        Material defaultMat = Material.getMaterial(defaultIcon);
        if (defaultMat == null) defaultMat = Material.RED_BED;
        
        ConfigurationSection worldIcons = plugin.getConfig().getConfigurationSection("gui.home-icon.world-icons");

        for (int i = 0; i < visibleHomes.size(); i++) {
            if (startSlot + i >= GUI_SIZE) break; 

            String homeName = visibleHomes.get(i);
            Location loc = homes.get(homeName);
            boolean isPublic = homeManager.isPublic(target.getUniqueId(), homeName);
            
            // Determine icon material based on world
            Material iconMat = defaultMat;
            if (worldIcons != null && loc.getWorld() != null) {
                String worldName = loc.getWorld().getName();
                String matName = worldIcons.getString(worldName);
                if (matName != null) {
                    Material m = Material.getMaterial(matName);
                    if (m != null) iconMat = m;
                }
            }

            ItemStack item = new ItemStack(iconMat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String nameTmpl = plugin.getConfig().getString("gui.home-icon.name", "&6{name}");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', nameTmpl.replace("{name}", homeName)));
                
                List<String> lore = new ArrayList<>();
                
                // Add base lore
                for (String line : plugin.getConfig().getStringList("gui.home-icon.lore")) {
                    String l = line.replace("{world}", loc.getWorld().getName())
                                   .replace("{x}", String.valueOf(loc.getBlockX()))
                                   .replace("{y}", String.valueOf(loc.getBlockY()))
                                   .replace("{z}", String.valueOf(loc.getBlockZ()));
                    lore.add(ChatColor.translateAlternateColorCodes('&', l));
                }
                
                // Public Status
                if (isPublic) {
                    lore.add(ChatColor.GREEN + "公開中");
                } else {
                    lore.add(ChatColor.RED + "非公開");
                }
                
                // Add action specific lore
                List<String> actionLore = new ArrayList<>(); // Initialize empty
                if (deleteMode) {
                    actionLore = plugin.getConfig().getStringList("gui.home-icon.lore-delete");
                } else if (publicMode) {
                     actionLore.add(ChatColor.YELLOW + "クリックして公開/非公開を切り替え");
                } else {
                    actionLore = plugin.getConfig().getStringList("gui.home-icon.lore-teleport");
                    // Show cost if not owner and cost enabled
                    if (economyManager != null && economyManager.hasEconomy()) {
                         double cost = plugin.getConfig().getDouble("economy.cost.teleport", 0);
                         if (cost > 0) {
                             lore.add(ChatColor.GOLD + "テレポート費用: " + economyManager.format(cost));
                         }
                    }
                }
                
                for (String line : actionLore) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            
            inv.setItem(startSlot + i, item);
        }

        viewer.openInventory(inv);
    }
    
    private List<String> getVisibleHomes(Player viewer, Player target, Map<String, Location> homes) {
        List<String> visibleHomes = new ArrayList<>();
        boolean isOwner = viewer.getUniqueId().equals(target.getUniqueId());
        boolean isAdmin = viewer.hasPermission("homes.admin") && !isOwner;
        
        for (String name : homes.keySet()) {
            boolean isPublic = homeManager.isPublic(target.getUniqueId(), name);
            if (isOwner || isAdmin || isPublic) {
                visibleHomes.add(name);
            }
        }
        return visibleHomes;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Check both titles (Normal and Delete Mode) - AND match flexible titles if possible
        String title = event.getView().getTitle();
        // Simple check
        if (!title.contains("ホーム") && !title.contains("削除") && !title.contains("公開")) {
             return;
        }
        // Better: Check if title equals config strings
        String normalTitle = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.title", "ホーム一覧"));
        String deleteTitle = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.delete-mode-title", "&c削除モード (クリックで削除)"));
        String publicTitle = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.public-mode-title", "&b公開設定モード (クリックで切替)"));
        
        // Allow other titles for Admin view (e.g. "User's Homes")
        boolean isMyGui = title.equals(normalTitle) || title.equals(deleteTitle) || title.equals(publicTitle) || title.contains("のホーム"); 
        
        if (!isMyGui) return;

        event.setCancelled(true); // Prevent taking items

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        Player viewer = (Player) event.getWhoClicked();
        
        // Determine target (owner of homes)
        // If title is default, target is viewer. If title is "X's Home", target is X.
        Player target = viewer;
        if (title.contains("のホーム")) {
            String targetName = ChatColor.stripColor(title).replace("のホーム", "");
            target = Bukkit.getPlayer(targetName); // Warning: this relies on title not changing format
        }
        
        if (target == null) target = viewer; // Fallback
        
        boolean isOwner = viewer.getUniqueId().equals(target.getUniqueId());
        boolean isAdmin = viewer.hasPermission("homes.admin") && !isOwner;
        
        int slot = event.getSlot();

        // Create Home Button at Slot 0
        if (slot == 0 && isOwner) {
            if (inputListener != null) {
                inputListener.startCreation(viewer);
            }
            return;
        }

        // Delete Mode Button at Slot 8
        if (slot == 8) {
            if (deleteModePlayers.contains(viewer.getUniqueId())) {
                deleteModePlayers.remove(viewer.getUniqueId());
                soundManager.play(viewer, "gui-click");
            } else {
                deleteModePlayers.add(viewer.getUniqueId());
                // Disable other modes
                publicModePlayers.remove(viewer.getUniqueId());
                soundManager.play(viewer, "gui-click");
            }
            open(viewer, target); 
            return;
        }

        // Public Mode Button at Slot 7
        if (slot == 7 && isOwner) {
            if (publicModePlayers.contains(viewer.getUniqueId())) {
                publicModePlayers.remove(viewer.getUniqueId());
                soundManager.play(viewer, "gui-click");
            } else {
                publicModePlayers.add(viewer.getUniqueId());
                // Disable other modes
                deleteModePlayers.remove(viewer.getUniqueId());
                soundManager.play(viewer, "gui-click");
            }
            open(viewer, target); 
            return;
        }

        // Home Items (Slot 9-26)
        if (slot >= 9 && slot <= 26) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                
                Map<String, Location> homes = homeManager.getHomes(target);
                List<String> visibleHomes = getVisibleHomes(viewer, target, homes);
                
                int index = slot - 9;
                if (index < visibleHomes.size()) {
                    String homeName = visibleHomes.get(index);
                    
                    if (deleteModePlayers.contains(viewer.getUniqueId())) {
                        // Delete Mode Logic (existing)
                         new ConfirmGUI(plugin, homeManager, this, homeName, soundManager, target.getUniqueId()).open(viewer);
                         soundManager.play(viewer, "gui-click");
                    } else if (publicModePlayers.contains(viewer.getUniqueId()) && isOwner) {
                        // Public Mode Logic
                        boolean isPublic = homeManager.isPublic(target.getUniqueId(), homeName);
                        boolean newState = !isPublic;
                        
                        // Economy check for making public (only when turning ON)
                        if (newState && economyManager != null && economyManager.hasEconomy()) {
                             double cost = plugin.getConfig().getDouble("economy.cost.make-public", 0);
                             if (cost > 0 && !economyManager.hasMoney(viewer.getName(), cost)) {
                                viewer.sendMessage(plugin.getMessage("insufficient-funds").replace("{cost}", economyManager.format(cost)));
                                return;
                            }
                            if (cost > 0) {
                                economyManager.withdraw(viewer.getName(), cost);
                                viewer.sendMessage(plugin.getMessage("payment-success").replace("{cost}", economyManager.format(cost)));
                            }
                        }
                        
                        homeManager.setPublic(target.getUniqueId(), homeName, newState);
                        soundManager.play(viewer, "gui-click");
                        
                        // Re-open to update icon
                        open(viewer, target);
                    } else {
                        // Teleport Logic (existing)
                        // Economy Check for TP
                         if (economyManager != null && economyManager.hasEconomy()) {
                             double cost = plugin.getConfig().getDouble("economy.cost.teleport", 0);
                             if (cost > 0 && !economyManager.hasMoney(viewer.getName(), cost)) {
                                viewer.sendMessage(plugin.getMessage("insufficient-funds").replace("{cost}", economyManager.format(cost)));
                                return;
                            }
                            if (cost > 0) {
                                economyManager.withdraw(viewer.getName(), cost);
                                viewer.sendMessage(plugin.getMessage("payment-success").replace("{cost}", economyManager.format(cost)));
                            }
                        }
                        
                        viewer.closeInventory();
                        Location loc = homeManager.getHome(target.getUniqueId(), homeName);
                        teleportManager.teleport(viewer, loc);
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getReason() != InventoryCloseEvent.Reason.OPEN_NEW) {
            deleteModePlayers.remove(event.getPlayer().getUniqueId());
            publicModePlayers.remove(event.getPlayer().getUniqueId());
        }
    }
}
