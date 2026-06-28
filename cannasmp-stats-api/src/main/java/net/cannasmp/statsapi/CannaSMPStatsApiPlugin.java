package net.cannasmp.statsapi;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class CannaSMPStatsApiPlugin extends JavaPlugin {
    private HttpServer httpServer;
    private ExecutorService executor;
    private BukkitTask snapshotTask;
    private volatile StatsSnapshot snapshot = StatsSnapshot.empty();
    private final Instant startedAt = Instant.now();
    private final Map<String, RateBucket> rateBuckets = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocal();
        startSnapshotTask();
        startHttpServer();
    }

    @Override
    public void onDisable() {
        stopHttpServer();
        if (snapshotTask != null) {
            snapshotTask.cancel();
            snapshotTask = null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("CannaSMPStatsAPI: running on " + bindHost() + ":" + port());
            sender.sendMessage("Cached players: " + snapshot.onlinePlayers + "/" + snapshot.maxPlayers);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            reloadLocal();
            startSnapshotTask();
            restartHttpServer();
            sender.sendMessage("CannaSMPStatsAPI reloaded.");
            return true;
        }
        if (args[0].equalsIgnoreCase("key")) {
            sender.sendMessage("API key is " + (apiKeyConfigured() ? "configured." : "NOT configured. Edit config.yml."));
            return true;
        }
        sender.sendMessage("Usage: /statsapi <reload|status|key>");
        return true;
    }

    private void reloadLocal() {
        int refresh = Math.max(1, getConfig().getInt("stats.refresh-seconds", 5));
        if (getConfig().getString("security.api-key", "").equals("CHANGE_ME_TO_A_LONG_RANDOM_SECRET")) {
            getLogger().warning("security.api-key is still the default placeholder. Protected endpoints will reject requests until changed.");
        }
        getLogger().info("Stats snapshots refresh every " + refresh + "s.");
    }

    private void startSnapshotTask() {
        if (snapshotTask != null) {
            snapshotTask.cancel();
        }
        long periodTicks = Math.max(20L, getConfig().getLong("stats.refresh-seconds", 5) * 20L);
        snapshot = createSnapshot();
        snapshotTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                snapshot = createSnapshot();
            } catch (RuntimeException ex) {
                getLogger().log(Level.WARNING, "Failed to refresh stats snapshot", ex);
            }
        }, periodTicks, periodTicks);
    }

    private void startHttpServer() {
        try {
            String host = bindHost();
            int port = port();
            int workers = Math.max(1, Math.min(16, getConfig().getInt("server.worker-threads", 4)));
            httpServer = HttpServer.create(new InetSocketAddress(host, port), 32);
            executor = Executors.newFixedThreadPool(workers, runnable -> {
                Thread thread = new Thread(runnable, "CannaSMPStatsAPI-HTTP");
                thread.setDaemon(true);
                return thread;
            });
            httpServer.setExecutor(executor);
            httpServer.createContext("/", new ApiHandler());
            httpServer.start();
            getLogger().info("HTTP stats API listening on " + host + ":" + port);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not start HTTP stats API on " + bindHost() + ":" + port(), ex);
        }
    }

    private void restartHttpServer() {
        stopHttpServer();
        startHttpServer();
    }

    private void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(2);
            httpServer = null;
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
            executor = null;
        }
    }

    private String bindHost() {
        return getConfig().getString("server.bind-host", "0.0.0.0");
    }

    private int port() {
        return Math.max(1, Math.min(65535, getConfig().getInt("server.port", 25886)));
    }

    private boolean apiKeyConfigured() {
        String key = getConfig().getString("security.api-key", "");
        return key != null && !key.isBlank() && !key.equals("CHANGE_ME_TO_A_LONG_RANDOM_SECRET");
    }

    private StatsSnapshot createSnapshot() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        List<Map<String, Object>> playerList = new ArrayList<>();
        for (Player player : players) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", player.getName());
            map.put("uuid", player.getUniqueId().toString());
            map.put("world", player.getWorld().getName());
            map.put("ping", player.getPing());
            map.put("kills", player.getStatistic(Statistic.PLAYER_KILLS));
            map.put("deaths", player.getStatistic(Statistic.DEATHS));
            map.put("playtimeTicks", player.getStatistic(Statistic.PLAY_ONE_MINUTE));
            map.put("playtimeSeconds", player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L);
            map.put("balance", getVaultBalance(player));
            map.put("rank", getLuckPermsRank(player));
            map.put("shards", getShardBalance(player.getUniqueId()));
            playerList.add(map);
        }

        List<Map<String, Object>> worlds = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", world.getName());
            map.put("environment", world.getEnvironment().name().toLowerCase(Locale.ROOT));
            map.put("players", world.getPlayers().size());
            worlds.add(map);
        }

        Map<String, Object> leaderboards = new LinkedHashMap<>();
        leaderboards.put("kills", leaderboard(playerList, "kills"));
        leaderboards.put("deaths", leaderboard(playerList, "deaths"));
        leaderboards.put("playtime", leaderboard(playerList, "playtimeSeconds"));
        leaderboards.put("balance", leaderboard(playerList, "balance"));
        leaderboards.put("shards", leaderboard(playerList, "shards"));

        double[] tps = Bukkit.getTPS();
        double[] mspt = readMspt();
        return new StatsSnapshot(
                Instant.now(),
                Bukkit.getOnlinePlayers().size(),
                Bukkit.getMaxPlayers(),
                Bukkit.getVersion(),
                Bukkit.getMinecraftVersion(),
                getDescription().getVersion(),
                getConfig().getString("server.public-host", "cannasmp.smpserver.net"),
                uptimeSeconds(),
                tps,
                mspt,
                playerList,
                worlds,
                leaderboards
        );
    }

    private List<Map<String, Object>> leaderboard(List<Map<String, Object>> players, String field) {
        int size = Math.max(1, getConfig().getInt("stats.leaderboard-size", 10));
        return players.stream()
                .filter(player -> player.get(field) instanceof Number)
                .sorted(Comparator.comparingDouble((Map<String, Object> player) -> ((Number) player.get(field)).doubleValue()).reversed())
                .limit(size)
                .map(player -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("name", player.get("name"));
                    entry.put("uuid", player.get("uuid"));
                    entry.put("value", player.get(field));
                    return entry;
                })
                .toList();
    }

    private long uptimeSeconds() {
        return Duration.between(startedAt, Instant.now()).toSeconds();
    }

    private double[] readMspt() {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getAverageTickTime");
            Object value = method.invoke(Bukkit.getServer());
            if (value instanceof Number number) {
                return new double[]{number.doubleValue(), 0D, 0D};
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return new double[]{0D, 0D, 0D};
    }

    private Double getVaultBalance(Player player) {
        if (!getConfig().getBoolean("optional-data.vault-economy", true) || !Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return null;
        }
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(economyClass);
            if (rsp == null) {
                return null;
            }
            Object economy = rsp.getProvider();
            Method method = economyClass.getMethod("getBalance", org.bukkit.OfflinePlayer.class);
            Object value = method.invoke(economy, player);
            return value instanceof Number number ? number.doubleValue() : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private String getLuckPermsRank(Player player) {
        if (!getConfig().getBoolean("optional-data.luckperms-groups", true) || !Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            return null;
        }
        try {
            Class<?> luckPermsProvider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = luckPermsProvider.getMethod("get").invoke(null);
            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, player.getUniqueId());
            if (user == null) {
                return null;
            }
            Object primaryGroup = user.getClass().getMethod("getPrimaryGroup").invoke(user);
            return Objects.toString(primaryGroup, null);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private Long getShardBalance(UUID uuid) {
        if (!getConfig().getBoolean("optional-data.shards.enabled", true)) {
            return null;
        }
        String path = getConfig().getString("optional-data.shards.file", "");
        if (path == null || path.isBlank()) {
            return null;
        }
        File file = new File(getServer().getWorldContainer(), path);
        if (!file.isFile()) {
            file = new File(path);
        }
        if (!file.isFile()) {
            return null;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.isLong("players." + uuid)) {
            return yaml.getLong("players." + uuid);
        }
        if (yaml.isLong(uuid.toString())) {
            return yaml.getLong(uuid.toString());
        }
        return null;
    }

    private final class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = normalizePath(exchange.getRequestURI().getPath());
            try {
                addCors(exchange);
                if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                    send(exchange, 204, "");
                    return;
                }
                if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    sendJson(exchange, 405, error("method_not_allowed", "Only GET is supported."));
                    return;
                }
                if (isRateLimited(exchange)) {
                    sendJson(exchange, 429, error("rate_limited", "Too many requests. Try again soon."));
                    return;
                }
                boolean authenticated = isAuthenticated(exchange);
                if (requiresAuth(path) && !authenticated) {
                    sendJson(exchange, 401, error("unauthorized", "Missing or invalid API key."));
                    return;
                }
                route(exchange, path, authenticated);
            } catch (RuntimeException ex) {
                getLogger().log(Level.WARNING, "API request failed for " + path, ex);
                sendJson(exchange, 500, error("internal_error", "Request failed."));
            }
        }

        private void route(HttpExchange exchange, String path, boolean authenticated) throws IOException {
            StatsSnapshot current = snapshot;
            switch (path) {
                case "/health" -> sendJson(exchange, 200, Map.of("ok", true, "plugin", getDescription().getVersion()));
                case "/api/v1/status" -> sendJson(exchange, 200, current.statusMap());
                case "/api/v1/players" -> sendJson(exchange, 200, current.playersMap(authenticated || getConfig().getBoolean("stats.public-player-list", false), authenticated && getConfig().getBoolean("stats.expose-player-details", true)));
                case "/api/v1/worlds" -> sendJson(exchange, 200, Map.of("worlds", current.worlds));
                case "/api/v1/performance" -> sendJson(exchange, 200, current.performanceMap());
                case "/api/v1/leaderboards" -> sendJson(exchange, 200, Map.of("leaderboards", current.leaderboards));
                case "/api/v1/meta" -> sendJson(exchange, 200, current.metaMap());
                default -> {
                    if (path.startsWith("/api/v1/player/")) {
                        String lookup = path.substring("/api/v1/player/".length());
                        sendJson(exchange, 200, current.playerLookup(lookup));
                    } else {
                        sendJson(exchange, 404, error("not_found", "Endpoint not found."));
                    }
                }
            }
        }
    }

    private boolean requiresAuth(String path) {
        if (!getConfig().getBoolean("security.require-auth-by-default", true)) {
            return false;
        }
        List<String> publicEndpoints = getConfig().getStringList("security.public-endpoints");
        return publicEndpoints.stream().noneMatch(endpoint -> normalizePath(endpoint).equals(path));
    }

    private boolean isAuthenticated(HttpExchange exchange) {
        if (!apiKeyConfigured()) {
            return false;
        }
        String expected = getConfig().getString("security.api-key", "");
        Headers headers = exchange.getRequestHeaders();
        String authorization = headers.getFirst("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return constantEquals(expected, authorization.substring("Bearer ".length()).trim());
        }
        String apiKey = headers.getFirst("X-API-Key");
        return apiKey != null && constantEquals(expected, apiKey.trim());
    }

    private boolean constantEquals(String expected, String actual) {
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        int diff = a.length ^ b.length;
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            diff |= (i < a.length ? a[i] : 0) ^ (i < b.length ? b[i] : 0);
        }
        return diff == 0;
    }

    private boolean isRateLimited(HttpExchange exchange) {
        if (!getConfig().getBoolean("rate-limit.enabled", true)) {
            return false;
        }
        int limit = Math.max(1, getConfig().getInt("rate-limit.requests-per-minute", 120));
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        RateBucket bucket = rateBuckets.computeIfAbsent(ip, ignored -> new RateBucket(limit));
        return !bucket.tryConsume(limit);
    }

    private void addCors(HttpExchange exchange) {
        if (!getConfig().getBoolean("cors.enabled", true)) {
            return;
        }
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        List<String> allowed = getConfig().getStringList("cors.allowed-origins");
        if (origin != null && allowed.contains(origin)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().set("Vary", "Origin");
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", String.join(", ", getConfig().getStringList("cors.allowed-methods")));
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, X-API-Key, Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "600");
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        send(exchange, status, Json.stringify(body));
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private Map<String, Object> error(String code, String message) {
        return Map.of("ok", false, "error", Map.of("code", code, "message", message));
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static final class RateBucket {
        private long windowStarted = System.currentTimeMillis();
        private int used;

        RateBucket(int ignored) {
        }

        synchronized boolean tryConsume(int limit) {
            long now = System.currentTimeMillis();
            if (now - windowStarted >= 60_000L) {
                windowStarted = now;
                used = 0;
            }
            if (used >= limit) {
                return false;
            }
            used++;
            return true;
        }
    }

    private record StatsSnapshot(
            Instant createdAt,
            int onlinePlayers,
            int maxPlayers,
            String bukkitVersion,
            String minecraftVersion,
            String pluginVersion,
            String publicHost,
            long uptimeSeconds,
            double[] tps,
            double[] mspt,
            List<Map<String, Object>> players,
            List<Map<String, Object>> worlds,
            Map<String, Object> leaderboards
    ) {
        static StatsSnapshot empty() {
            return new StatsSnapshot(Instant.EPOCH, 0, 0, "unknown", "unknown", "unknown", "unknown", 0L, new double[]{0, 0, 0}, new double[]{0, 0, 0}, List.of(), List.of(), Map.of());
        }

        Map<String, Object> statusMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("online", true);
            map.put("publicHost", publicHost);
            map.put("onlinePlayers", onlinePlayers);
            map.put("maxPlayers", maxPlayers);
            map.put("minecraftVersion", minecraftVersion);
            map.put("uptimeSeconds", uptimeSeconds);
            map.put("snapshotTime", createdAt.toString());
            map.put("tps", tpsMap());
            return map;
        }

        Map<String, Object> playersMap(boolean includeList, boolean includeDetails) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("onlinePlayers", onlinePlayers);
            map.put("maxPlayers", maxPlayers);
            if (includeList) {
                map.put("players", includeDetails ? players : players.stream().map(player -> Map.of("name", player.get("name"))).toList());
            }
            return map;
        }

        Map<String, Object> performanceMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("tps", tpsMap());
            map.put("mspt", msptMap());
            map.put("uptimeSeconds", uptimeSeconds);
            map.put("snapshotTime", createdAt.toString());
            return map;
        }

        Map<String, Object> metaMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "CannaSMPStatsAPI");
            map.put("pluginVersion", pluginVersion);
            map.put("minecraftVersion", minecraftVersion);
            map.put("bukkitVersion", bukkitVersion);
            map.put("apiVersion", "v1");
            map.put("readOnly", true);
            return map;
        }

        Map<String, Object> playerLookup(String lookup) {
            String normalized = lookup.toLowerCase(Locale.ROOT);
            return players.stream()
                    .filter(player -> Objects.toString(player.get("uuid"), "").equalsIgnoreCase(lookup) || Objects.toString(player.get("name"), "").toLowerCase(Locale.ROOT).equals(normalized))
                    .findFirst()
                    .map(player -> Map.of("found", true, "player", player))
                    .orElseGet(() -> Map.of("found", false));
        }

        private Map<String, Object> tpsMap() {
            return Map.of("oneMinute", round(tps.length > 0 ? tps[0] : 0), "fiveMinute", round(tps.length > 1 ? tps[1] : 0), "fifteenMinute", round(tps.length > 2 ? tps[2] : 0));
        }

        private Map<String, Object> msptMap() {
            return Map.of("average", round(mspt.length > 0 ? mspt[0] : 0), "min", round(mspt.length > 1 ? mspt[1] : 0), "max", round(mspt.length > 2 ? mspt[2] : 0));
        }

        private double round(double value) {
            return Math.round(value * 100.0) / 100.0;
        }
    }

    private static final class Json {
        private Json() {
        }

        static String stringify(Object value) {
            StringBuilder builder = new StringBuilder();
            write(builder, value);
            return builder.toString();
        }

        private static void write(StringBuilder builder, Object value) {
            if (value == null) {
                builder.append("null");
            } else if (value instanceof String string) {
                builder.append('"').append(escape(string)).append('"');
            } else if (value instanceof Number || value instanceof Boolean) {
                builder.append(value);
            } else if (value instanceof Map<?, ?> map) {
                builder.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) builder.append(',');
                    first = false;
                    write(builder, Objects.toString(entry.getKey()));
                    builder.append(':');
                    write(builder, entry.getValue());
                }
                builder.append('}');
            } else if (value instanceof Iterable<?> iterable) {
                builder.append('[');
                boolean first = true;
                for (Object item : iterable) {
                    if (!first) builder.append(',');
                    first = false;
                    write(builder, item);
                }
                builder.append(']');
            } else if (value.getClass().isArray()) {
                builder.append('[');
                int length = java.lang.reflect.Array.getLength(value);
                for (int i = 0; i < length; i++) {
                    if (i > 0) builder.append(',');
                    write(builder, java.lang.reflect.Array.get(value, i));
                }
                builder.append(']');
            } else {
                write(builder, Objects.toString(value));
            }
        }

        private static String escape(String value) {
            StringBuilder escaped = new StringBuilder(value.length() + 16);
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"' -> escaped.append("\\\"");
                    case '\\' -> escaped.append("\\\\");
                    case '\b' -> escaped.append("\\b");
                    case '\f' -> escaped.append("\\f");
                    case '\n' -> escaped.append("\\n");
                    case '\r' -> escaped.append("\\r");
                    case '\t' -> escaped.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            escaped.append(String.format("\\u%04x", (int) c));
                        } else {
                            escaped.append(c);
                        }
                    }
                }
            }
            return escaped.toString();
        }
    }
}
