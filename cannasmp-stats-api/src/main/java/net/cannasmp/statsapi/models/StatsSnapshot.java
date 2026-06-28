package net.cannasmp.statsapi.models;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record StatsSnapshot(
        Instant createdAt,
        Map<String, Object> server,
        Map<String, Object> system,
        List<Map<String, Object>> players,
        List<Map<String, Object>> plugins,
        Map<String, Object> integrations
) {
    public static StatsSnapshot empty() {
        return new StatsSnapshot(
                Instant.EPOCH,
                Map.of("online", false, "currentPlayers", 0, "maxPlayers", 0),
                Map.of(),
                List.of(),
                List.of(),
                Map.of()
        );
    }

    public Map<String, Object> apiEnvelope(String apiVersion) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ok", true);
        map.put("apiVersion", apiVersion);
        map.put("snapshotTime", createdAt.toString());
        map.put("server", server);
        map.put("system", system);
        map.put("players", players);
        map.put("plugins", plugins);
        map.put("integrations", integrations);
        return map;
    }

    public Map<String, Object> status() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ok", true);
        map.put("online", server.getOrDefault("online", true));
        map.put("snapshotTime", createdAt.toString());
        map.put("currentPlayers", server.getOrDefault("currentPlayers", 0));
        map.put("maxPlayers", server.getOrDefault("maxPlayers", 0));
        map.put("tps", server.get("tps"));
        map.put("mspt", server.get("mspt"));
        map.put("ram", system.get("ram"));
        map.put("cpu", system.get("cpu"));
        return map;
    }

    public Map<String, Object> players(boolean includeList, boolean includeDetails) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("onlinePlayers", server.getOrDefault("currentPlayers", 0));
        map.put("maxPlayers", server.getOrDefault("maxPlayers", 0));
        if (includeList) {
            map.put("players", includeDetails ? players : players.stream().map(player -> {
                Map<String, Object> compact = new LinkedHashMap<>();
                compact.put("username", player.get("username"));
                compact.put("uuid", player.get("uuid"));
                compact.put("rank", player.get("rank"));
                return compact;
            }).toList());
        }
        return map;
    }

    public Map<String, Object> player(String lookup) {
        String normalized = Objects.toString(lookup, "").toLowerCase();
        return players.stream()
                .filter(player -> Objects.toString(player.get("uuid"), "").equalsIgnoreCase(lookup)
                        || Objects.toString(player.get("username"), "").toLowerCase().equals(normalized))
                .findFirst()
                .<Map<String, Object>>map(player -> Map.of("found", true, "player", player))
                .orElseGet(() -> Map.of("found", false));
    }
}
