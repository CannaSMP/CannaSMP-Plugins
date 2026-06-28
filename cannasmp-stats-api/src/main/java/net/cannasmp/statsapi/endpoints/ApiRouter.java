package net.cannasmp.statsapi.endpoints;

import net.cannasmp.statsapi.config.ApiConfig;
import net.cannasmp.statsapi.managers.IntegrationManager;
import net.cannasmp.statsapi.managers.SnapshotManager;
import net.cannasmp.statsapi.models.StatsSnapshot;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiRouter {
    private final JavaPlugin plugin;
    private final ApiConfig config;
    private final SnapshotManager snapshots;
    private final IntegrationManager integrations;

    public ApiRouter(JavaPlugin plugin, ApiConfig config, SnapshotManager snapshots, IntegrationManager integrations) {
        this.plugin = plugin;
        this.config = config;
        this.snapshots = snapshots;
        this.integrations = integrations;
    }

    public RouteResult route(String path, boolean authenticated) {
        StatsSnapshot snapshot = snapshots.current();
        return switch (ApiConfig.normalize(path)) {
            case "/health", "/api/v1/health" -> ok(Map.of("ok", true, "plugin", plugin.getDescription().getVersion()));
            case "/api/v1/version" -> ok(version());
            case "/api/v1/status" -> ok(snapshot.status());
            case "/api/v1/public", "/api/v1/website" -> ok(snapshot.publicWebsiteSnapshot());
            case "/api/v1/server" -> ok(Map.of("server", snapshot.server()));
            case "/api/v1/system" -> ok(Map.of("system", snapshot.system()));
            case "/api/v1/players" -> ok(snapshot.players(authenticated || config.publicPlayerList(), authenticated && config.exposePlayerDetails()));
            case "/api/v1/plugins" -> ok(Map.of("plugins", snapshot.plugins()));
            case "/api/v1/integrations" -> ok(Map.of("integrations", integrations.statusMap()));
            case "/api/v1" -> ok(snapshot.apiEnvelope("v1"));
            default -> playerRoute(path, snapshot);
        };
    }

    private RouteResult playerRoute(String path, StatsSnapshot snapshot) {
        String normalized = ApiConfig.normalize(path);
        if (normalized.startsWith("/api/v1/player/")) {
            String lookup = normalized.substring("/api/v1/player/".length());
            return ok(snapshot.player(lookup));
        }
        return new RouteResult(404, Map.of("ok", false, "error", Map.of("code", "not_found", "message", "Endpoint not found.")));
    }

    private RouteResult ok(Object body) {
        return new RouteResult(200, body);
    }

    private Map<String, Object> version() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ok", true);
        map.put("name", plugin.getDescription().getName());
        map.put("pluginVersion", plugin.getDescription().getVersion());
        map.put("apiVersion", "v1");
        map.put("paperApi", plugin.getDescription().getAPIVersion());
        return map;
    }

    public record RouteResult(int status, Object body) {
    }
}
