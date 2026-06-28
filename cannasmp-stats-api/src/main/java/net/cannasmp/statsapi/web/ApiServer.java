package net.cannasmp.statsapi.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.cannasmp.statsapi.authentication.ApiKeyAuthenticator;
import net.cannasmp.statsapi.authentication.IpWhitelist;
import net.cannasmp.statsapi.authentication.RateLimiter;
import net.cannasmp.statsapi.config.ApiConfig;
import net.cannasmp.statsapi.endpoints.ApiRouter;
import net.cannasmp.statsapi.managers.IntegrationManager;
import net.cannasmp.statsapi.managers.SnapshotManager;
import net.cannasmp.statsapi.utils.JsonUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class ApiServer {
    private final JavaPlugin plugin;
    private final ApiConfig config;
    private final ApiKeyAuthenticator authenticator;
    private final IpWhitelist whitelist;
    private final RateLimiter rateLimiter;
    private final ApiRouter router;
    private HttpServer server;
    private ExecutorService executor;

    public ApiServer(JavaPlugin plugin, ApiConfig config, SnapshotManager snapshots, IntegrationManager integrations,
                     ApiKeyAuthenticator authenticator, IpWhitelist whitelist, RateLimiter rateLimiter) {
        this.plugin = plugin;
        this.config = config;
        this.authenticator = authenticator;
        this.whitelist = whitelist;
        this.rateLimiter = rateLimiter;
        this.router = new ApiRouter(plugin, config, snapshots, integrations);
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(config.bindHost(), config.port()), 64);
            executor = Executors.newFixedThreadPool(config.workerThreads(), runnable -> {
                Thread thread = new Thread(runnable, "CannaSMP-ServerAPI-HTTP");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.createContext("/", this::handle);
            server.start();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not start API server on " + config.bindHost() + ":" + config.port(), ex);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(2);
            server = null;
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

    private void handle(HttpExchange exchange) throws IOException {
        String path = ApiConfig.normalize(exchange.getRequestURI().getPath());
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
            if (!whitelist.isAllowed(exchange)) {
                sendJson(exchange, 403, error("forbidden", "This IP address is not allowed."));
                return;
            }
            if (rateLimiter.isLimited(exchange)) {
                sendJson(exchange, 429, error("rate_limited", "Too many requests. Try again soon."));
                return;
            }
            boolean authenticated = authenticator.isAuthenticated(exchange);
            if (authenticator.requiresAuthentication(path) && !authenticated) {
                sendJson(exchange, 401, error("unauthorized", "Missing or invalid API key."));
                return;
            }
            ApiRouter.RouteResult result = router.route(path, authenticated);
            sendJson(exchange, result.status(), result.body());
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "API request failed for " + path, ex);
            sendJson(exchange, 500, error("internal_error", "Request failed."));
        }
    }

    private void addCors(HttpExchange exchange) {
        if (!config.corsEnabled()) {
            return;
        }
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null && (config.corsAllowedOrigins().contains("*") || config.corsAllowedOrigins().contains(origin))) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().set("Vary", "Origin");
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, X-API-Key, Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "600");
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        send(exchange, status, JsonUtil.stringify(body));
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
}
