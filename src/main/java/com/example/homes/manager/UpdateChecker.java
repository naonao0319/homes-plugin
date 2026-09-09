package com.example.homes.manager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.example.homes.HomesPlugin;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class UpdateChecker implements Listener {

    private static final String GITHUB_REPO = "paper0319/homes-plugin";
    private static final String API_URL =
            "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private static final String RELEASES_PAGE =
            "https://github.com/" + GITHUB_REPO + "/releases";

    private static final LegacyComponentSerializer LEGACY_AMPERSAND =
            LegacyComponentSerializer.legacyAmpersand();

    private final HomesPlugin plugin;
    private final String currentVersion;

    private volatile String latestVersion;
    private volatile String latestReleaseUrl;
    private volatile boolean updateAvailable;

    public UpdateChecker(HomesPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    /** Kicks off an async fetch. Safe to call once during onEnable(). */
    public void checkAsync() {
        plugin.getFoliaScheduler().runAsync(this::doCheck);
    }

    private void doCheck() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "paper0319/homes-plugin (" + currentVersion + ")")
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 404) {
                plugin.getLogger().info("Update check: no GitHub release published yet");
                return;
            }
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                plugin.getLogger().warning("Update check: HTTP " + resp.statusCode());
                return;
            }

            GitHubRelease fetched = parseLatestRelease(resp.body());
            if (fetched == null) {
                plugin.getLogger().info("Update check: no published GitHub release");
                return;
            }

            int cmp;
            try {
                cmp = compareSemver(currentVersion, fetched.version());
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Update check: cannot compare versions '"
                        + currentVersion + "' vs '" + fetched.version() + "': " + ex.getMessage());
                return;
            }

            if (cmp < 0) {
                this.latestVersion = fetched.version();
                this.latestReleaseUrl = fetched.htmlUrl() != null ? fetched.htmlUrl() : RELEASES_PAGE;
                this.updateAvailable = true;
                plugin.getLogger().info("Update available: " + currentVersion + " -> " + fetched.version());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Update check failed: " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
        }
    }

    /**
     * GitHub {@code /releases/latest} JSON からバージョン番号を取り出す。
     * {@code tag_name} の先頭 {@code v} は取り除く。draft / prerelease は無視する。
     */
    static String parseLatestVersionNumber(String json) {
        GitHubRelease release = parseLatestRelease(json);
        return release == null ? null : release.version();
    }

    static GitHubRelease parseLatestRelease(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return null;
            JsonObject obj = root.getAsJsonObject();
            if (isTrue(obj.get("draft")) || isTrue(obj.get("prerelease"))) {
                return null;
            }
            JsonElement tagName = obj.get("tag_name");
            if (tagName == null || tagName.isJsonNull()) return null;
            String version = stripTagPrefix(tagName.getAsString());
            if (version == null || version.isBlank()) return null;
            String htmlUrl = null;
            JsonElement url = obj.get("html_url");
            if (url != null && !url.isJsonNull()) {
                htmlUrl = url.getAsString();
            }
            return new GitHubRelease(version, htmlUrl);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean isTrue(JsonElement element) {
        return element != null && !element.isJsonNull() && element.getAsBoolean();
    }

    static String stripTagPrefix(String tag) {
        if (tag == null) return null;
        String trimmed = tag.trim();
        if (trimmed.length() >= 2 && (trimmed.charAt(0) == 'v' || trimmed.charAt(0) == 'V')
                && Character.isDigit(trimmed.charAt(1))) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    /**
     * Compare two semver-like strings segment by segment as integers.
     * Suffixes like "-SNAPSHOT" are stripped before comparison.
     * Missing trailing segments are treated as 0.
     * @return negative if a&lt;b, 0 if equal, positive if a&gt;b
     * @throws NumberFormatException if a segment is not an integer
     */
    static int compareSemver(String a, String b) {
        String[] as = stripSuffix(a).split("\\.");
        String[] bs = stripSuffix(b).split("\\.");
        int n = Math.max(as.length, bs.length);
        for (int i = 0; i < n; i++) {
            int ai = i < as.length ? Integer.parseInt(as[i]) : 0;
            int bi = i < bs.length ? Integer.parseInt(bs[i]) : 0;
            if (ai != bi) return Integer.compare(ai, bi);
        }
        return 0;
    }

    private static String stripSuffix(String v) {
        int dash = v.indexOf('-');
        return dash < 0 ? v : v.substring(0, dash);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!updateAvailable) return;
        Player player = event.getPlayer();
        if (!player.isOp()) return;

        player.sendMessage(colorize(plugin.getLanguageManager().getString(
                "update-available-header", "&6[homes] &a更新があります！")));
        player.sendMessage(colorize(plugin.getLanguageManager().getString(
                "update-available-current", "&7現在のバージョン &f{current}")
                .replace("{current}", currentVersion)));
        player.sendMessage(colorize(plugin.getLanguageManager().getString(
                "update-available-latest", "&7新しいバージョン &e{latest}")
                .replace("{latest}", latestVersion)));

        String url = latestReleaseUrl != null ? latestReleaseUrl : RELEASES_PAGE;
        Component link = colorize(plugin.getLanguageManager().getString(
                "update-available-link", "&b【GitHubで表示】"))
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(colorize(plugin.getLanguageManager().getString(
                        "update-available-link-hover", "&7GitHubで開く"))));
        player.sendMessage(link);
    }

    private Component colorize(String text) {
        if (text == null) return Component.empty();
        return LEGACY_AMPERSAND.deserialize(text);
    }

    record GitHubRelease(String version, String htmlUrl) {
    }
}
