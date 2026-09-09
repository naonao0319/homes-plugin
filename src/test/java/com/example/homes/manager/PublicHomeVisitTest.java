package com.example.homes.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import com.example.homes.HomesPlugin;
import com.example.homes.gui.holder.PublicHomeGuiHolder;
import com.example.homes.testutil.FakeEconomy;

import net.milkbowl.vault.economy.Economy;

class PublicHomeVisitTest {

    private ServerMock server;
    private HomesPlugin plugin;
    private FakeEconomy economy;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        Plugin vault = MockBukkit.createMockPlugin("Vault");
        economy = new FakeEconomy();
        server.getServicesManager().register(Economy.class, economy, vault, ServicePriority.Highest);

        plugin = MockBukkit.load(HomesPlugin.class);
        plugin.getConfig().set("economy.enabled", true);
        plugin.getConfig().set("economy.cost.teleport", 50.0);
        plugin.getConfig().set("economy.cost.visit-public", 10.0);
        plugin.getConfig().set("economy.cost.set-home", 0.0);
        plugin.getConfig().set("settings.teleport.delay", 0);
        plugin.getConfig().set("settings.teleport.confirm-unsafe", false);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Location prepareSafeHome(World world, int x, int z) {
        world.getBlockAt(x, 63, z).setType(Material.STONE);
        return new Location(world, x + 0.5, 64.0, z + 0.5);
    }

    private void awaitUntil(BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 5000;
        do {
            if (condition.getAsBoolean()) {
                return;
            }
            server.getScheduler().waitAsyncTasksFinished();
            server.getScheduler().performTicks(1);
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        } while (System.currentTimeMillis() < deadline);
    }

    private static List<String> drain(PlayerMock player) {
        List<String> messages = new ArrayList<>();
        String message;
        while ((message = player.nextMessage()) != null) {
            messages.add(message);
        }
        return messages;
    }

    private static boolean containsEarned(String message) {
        return message.contains("公開ホーム") || message.toLowerCase().contains("public home");
    }

    private void awaitPublicHome(UUID ownerId, String homeName) {
        java.util.concurrent.atomic.AtomicBoolean found = new java.util.concurrent.atomic.AtomicBoolean(false);
        awaitUntil(() -> {
            if (found.get()) {
                return true;
            }
            plugin.getHomeManager().fetchAllPublicHomes(list -> {
                if (list.stream().anyMatch(view ->
                        ownerId.equals(view.ownerUuid()) && homeName.equals(view.homeName()))) {
                    found.set(true);
                }
            });
            server.getScheduler().waitAsyncTasksFinished();
            server.getScheduler().performTicks(2);
            return found.get();
        });
        assertTrue(found.get(), "公開ホームが DB に保存されていること: " + homeName);
    }

    @Test
    void visitingPublicHomePaysOnlineOwnerTheVisitFee() {
        World world = server.addSimpleWorld("world");
        Location home = prepareSafeHome(world, 10, 10);

        PlayerMock owner = server.addPlayer("Owner");
        PlayerMock visitor = server.addPlayer("Visitor");
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performTicks(2);

        plugin.getHomeManager().setHomeDirectly(owner.getUniqueId(), "shop", home);
        plugin.getHomeManager().setPublic(owner.getUniqueId(), "shop", true);
        awaitPublicHome(owner.getUniqueId(), "shop");

        economy.setBalance(owner, 0);
        economy.setBalance(visitor, 100);
        drain(owner);
        drain(visitor);

        assertTrue(plugin.getPublicHomeVisitService().visit(visitor, owner.getUniqueId(), "shop"));
        awaitUntil(() -> economy.getBalance(owner) == 10 && economy.getBalance(visitor) == 90);

        assertEquals(90, economy.getBalance(visitor), 0.001);
        assertEquals(10, economy.getBalance(owner), 0.001);
        assertTrue(drain(owner).stream().anyMatch(PublicHomeVisitTest::containsEarned),
                "オンラインの持ち主に入金通知が出る");
    }

    @Test
    void visitingOwnPublicHomeDoesNotPaySelf() {
        World world = server.addSimpleWorld("world");
        Location home = prepareSafeHome(world, 8, 8);

        PlayerMock owner = server.addPlayer("Owner");
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performTicks(2);

        plugin.getHomeManager().setHomeDirectly(owner.getUniqueId(), "shop", home);
        plugin.getHomeManager().setPublic(owner.getUniqueId(), "shop", true);
        economy.setBalance(owner, 100);
        drain(owner);

        assertTrue(plugin.getPublicHomeVisitService().visit(owner, owner.getUniqueId(), "shop"));
        awaitUntil(() -> economy.getBalance(owner) == 50);

        assertEquals(50, economy.getBalance(owner), 0.001, "自分のホームは teleport 代のみで自己入金しない");
        assertFalse(drain(owner).stream().anyMatch(PublicHomeVisitTest::containsEarned));
    }

    @Test
    void offlineOwnerIsPaidAndNotifiedOnJoin() {
        World world = server.addSimpleWorld("world");
        Location home = prepareSafeHome(world, 12, 12);

        UUID ownerId = UUID.randomUUID();
        PlayerMock owner = new PlayerMock(server, "Owner", ownerId);
        server.addPlayer(owner);
        PlayerMock visitor = server.addPlayer("Visitor");
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performTicks(2);

        plugin.getHomeManager().setHomeDirectly(ownerId, "shop", home);
        plugin.getHomeManager().setPublic(ownerId, "shop", true);
        awaitPublicHome(ownerId, "shop");

        economy.setBalance(owner, 0);
        economy.setBalance(visitor, 100);
        owner.disconnect();
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performTicks(2);

        assertTrue(plugin.getPublicHomeVisitService().visit(visitor, ownerId, "shop"));
        awaitUntil(() -> economy.getBalance(visitor) == 90);
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performTicks(5);

        assertEquals(90, economy.getBalance(visitor), 0.001);
        assertEquals(0, economy.getBalance(owner), 0.001, "オフライン中はまだ入金しない");

        PlayerMock rejoined = new PlayerMock(server, "Owner", ownerId);
        server.addPlayer(rejoined);
        awaitUntil(() -> economy.getBalance(rejoined) == 10);

        assertEquals(10, economy.getBalance(rejoined), 0.001);
        assertTrue(drain(rejoined).stream().anyMatch(PublicHomeVisitTest::containsEarned),
                "ログイン時にオフライン中の収益がチャットに出る");
    }

    @Test
    void vhomeWithoutArgsOpensPublicHomeBrowser() {
        World world = server.addSimpleWorld("world");
        Location home = prepareSafeHome(world, 4, 4);

        PlayerMock owner = server.addPlayer("Owner");
        PlayerMock visitor = server.addPlayer("Visitor");
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performTicks(2);

        plugin.getHomeManager().setHomeDirectly(owner.getUniqueId(), "shop", home);
        plugin.getHomeManager().setPublic(owner.getUniqueId(), "shop", true);
        awaitPublicHome(owner.getUniqueId(), "shop");

        visitor.performCommand("vhome");
        awaitUntil(() -> {
            Inventory top = visitor.getOpenInventory().getTopInventory();
            return top != null && top.getHolder() instanceof PublicHomeGuiHolder;
        });

        Inventory top = visitor.getOpenInventory().getTopInventory();
        assertNotNull(top, "公開ホーム GUI が開いていること");
        assertInstanceOf(PublicHomeGuiHolder.class, top.getHolder());
        PublicHomeGuiHolder holder = (PublicHomeGuiHolder) top.getHolder();
        assertEquals(1, holder.getHomes().size());
        assertEquals("shop", holder.getHomes().get(0).homeName());
        assertEquals(owner.getUniqueId(), holder.getHomes().get(0).ownerUuid());
    }
}
