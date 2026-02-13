package com.example.homes.manager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.example.homes.HomesPlugin;

public class TpaManager {

    private final HomesPlugin plugin;
    
    // Request: Receiver UUID -> Sender UUID (One pending request at a time per player)
    // Or better: Map<Receiver, Map<Sender, RequestType>> but simple TPA usually allows one or stack.
    // Let's allow multiple requests but separate them by sender.
    // Map<ReceiverUUID, Map<SenderUUID, RequestType>>
    private final Map<UUID, Map<UUID, TpaRequest>> requests = new HashMap<>();
    
    // Last Locations for /back: UUID -> Location
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    
    // TPA Toggle (Ignore list or global toggle)
    private final Set<UUID> tpaDisabled = new HashSet<>();
    private final Map<UUID, Set<UUID>> ignoredPlayers = new HashMap<>();

    public enum RequestType {
        TPA, // Sender wants to tp to Receiver
        TPAHERE // Sender wants Receiver to tp to Sender
    }

    public static class TpaRequest {
        public final UUID sender;
        public final RequestType type;
        public final long timestamp;

        public TpaRequest(UUID sender, RequestType type) {
            this.sender = sender;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public TpaManager(HomesPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player sender, Player receiver, RequestType type) {
        if (tpaDisabled.contains(receiver.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + receiver.getName() + " はテレポートリクエストを受け付けていません。");
            return;
        }
        if (isIgnored(receiver.getUniqueId(), sender.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + receiver.getName() + " はあなたのリクエストを無視しています。");
            return;
        }

        // Auto accept check (if implemented, user said /tpauto exists)
        // If receiver has auto-accept enabled... (Not storing this yet, assume command logic handles or add field)
        // For now standard request flow.

        requests.computeIfAbsent(receiver.getUniqueId(), k -> new HashMap<>()).put(sender.getUniqueId(), new TpaRequest(sender.getUniqueId(), type));

        sender.sendMessage(ChatColor.GREEN + receiver.getName() + " にテレポートリクエストを送信しました。");
        
        String typeMsg = type == RequestType.TPA ? "テレポート" : "カモン(呼び出し)";
        receiver.sendMessage(ChatColor.GREEN + sender.getName() + " から" + typeMsg + "リクエストが届きました。");
        receiver.sendMessage(ChatColor.YELLOW + "/tpaccept " + ChatColor.WHITE + "で承認、" + ChatColor.YELLOW + "/tpdeny " + ChatColor.WHITE + "で拒否します。");
        
        // Expire after 60 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (requests.containsKey(receiver.getUniqueId()) && requests.get(receiver.getUniqueId()).containsKey(sender.getUniqueId())) {
                    requests.get(receiver.getUniqueId()).remove(sender.getUniqueId());
                    if (sender.isOnline()) sender.sendMessage(ChatColor.YELLOW + receiver.getName() + " へのリクエストが期限切れになりました。");
                    if (receiver.isOnline()) receiver.sendMessage(ChatColor.YELLOW + sender.getName() + " からのリクエストが期限切れになりました。");
                }
            }
        }.runTaskLater(plugin, 20 * 60);
    }

    public void acceptRequest(Player receiver) {
        // Accept last request if multiple? Or require name if multiple?
        // Simple implementation: Accept most recent or ANY if only one.
        // User spec: /tpaccept - Accepts the request.
        
        if (!requests.containsKey(receiver.getUniqueId()) || requests.get(receiver.getUniqueId()).isEmpty()) {
            receiver.sendMessage(ChatColor.RED + "保留中のリクエストはありません。");
            return;
        }

        // Get the most recent request (Map doesn't guarantee order, need to iterate or track last)
        // For simplicity, pick first found.
        UUID senderUuid = requests.get(receiver.getUniqueId()).keySet().iterator().next();
        acceptRequest(receiver, senderUuid);
    }
    
    public void acceptRequest(Player receiver, UUID senderUuid) {
        if (!requests.containsKey(receiver.getUniqueId()) || !requests.get(receiver.getUniqueId()).containsKey(senderUuid)) {
            receiver.sendMessage(ChatColor.RED + "そのプレイヤーからのリクエストはありません。");
            return;
        }

        TpaRequest req = requests.get(receiver.getUniqueId()).remove(senderUuid);
        Player sender = Bukkit.getPlayer(senderUuid);

        if (sender == null || !sender.isOnline()) {
            receiver.sendMessage(ChatColor.RED + "プレイヤーが見つかりません。");
            return;
        }

        if (req.type == RequestType.TPA) {
            // Sender -> Receiver
            saveLastLocation(sender);
            sender.teleport(receiver);
            sender.sendMessage(ChatColor.GREEN + "リクエストが承認されました。");
            receiver.sendMessage(ChatColor.GREEN + sender.getName() + " のリクエストを承認しました。");
        } else {
            // Receiver -> Sender (TPAHERE)
            saveLastLocation(receiver);
            receiver.teleport(sender);
            sender.sendMessage(ChatColor.GREEN + receiver.getName() + " があなたのリクエストを承認しました。");
            receiver.sendMessage(ChatColor.GREEN + "カモンリクエストを承認しました。");
        }
    }

    public void denyRequest(Player receiver) {
        if (!requests.containsKey(receiver.getUniqueId()) || requests.get(receiver.getUniqueId()).isEmpty()) {
            receiver.sendMessage(ChatColor.RED + "保留中のリクエストはありません。");
            return;
        }
        
        // Deny all or one? Usually one.
        UUID senderUuid = requests.get(receiver.getUniqueId()).keySet().iterator().next();
        requests.get(receiver.getUniqueId()).remove(senderUuid);
        
        receiver.sendMessage(ChatColor.YELLOW + "リクエストを拒否しました。");
        Player sender = Bukkit.getPlayer(senderUuid);
        if (sender != null) {
            sender.sendMessage(ChatColor.RED + receiver.getName() + " にリクエストを拒否されました。");
        }
    }

    public void cancelRequest(Player sender, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "プレイヤーが見つかりません。");
            return;
        }
        
        if (requests.containsKey(target.getUniqueId()) && requests.get(target.getUniqueId()).containsKey(sender.getUniqueId())) {
            requests.get(target.getUniqueId()).remove(sender.getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "リクエストをキャンセルしました。");
        } else {
            sender.sendMessage(ChatColor.RED + "そのプレイヤーへのリクエストはありません。");
        }
    }

    public void saveLastLocation(Player player) {
        lastLocations.put(player.getUniqueId(), player.getLocation());
    }

    public void teleportBack(Player player) {
        if (lastLocations.containsKey(player.getUniqueId())) {
            Location loc = lastLocations.get(player.getUniqueId());
            // Save current location as new back location before tp? Usually /back allows toggle back and forth or stack.
            // Simple: Swap.
            Location current = player.getLocation();
            player.teleport(loc);
            lastLocations.put(player.getUniqueId(), current);
            player.sendMessage(ChatColor.GREEN + "前の場所に戻りました。");
        } else {
            player.sendMessage(ChatColor.RED + "戻る場所がありません。");
        }
    }

    public void toggleTpa(Player player) {
        if (tpaDisabled.contains(player.getUniqueId())) {
            tpaDisabled.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "テレポートリクエストを受け付けるようにしました。");
        } else {
            tpaDisabled.add(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "テレポートリクエストを拒否するようにしました。");
        }
    }

    public void ignorePlayer(Player player, String targetName) {
        // Implementation for ignore list
        Player target = Bukkit.getPlayer(targetName); // Or offline logic
        if (target == null) return;
        
        ignoredPlayers.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (ignoredPlayers.get(player.getUniqueId()).contains(target.getUniqueId())) {
            ignoredPlayers.get(player.getUniqueId()).remove(target.getUniqueId());
            player.sendMessage(ChatColor.GREEN + target.getName() + " の無視を解除しました。");
        } else {
            ignoredPlayers.get(player.getUniqueId()).add(target.getUniqueId());
            player.sendMessage(ChatColor.RED + target.getName() + " を無視リストに追加しました。");
        }
    }
    
    public boolean isIgnored(UUID receiver, UUID sender) {
        return ignoredPlayers.containsKey(receiver) && ignoredPlayers.get(receiver).contains(sender);
    }
}
