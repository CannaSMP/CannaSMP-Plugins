package net.cannasmp.statsapi.storage;

import net.cannasmp.statsapi.config.ApiConfig;
import net.cannasmp.statsapi.models.StatsSnapshot;
import net.cannasmp.statsapi.utils.JsonUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class WebsiteExportService implements AutoCloseable {
    private final JavaPlugin plugin;
    private final ApiConfig config;
    private final ExecutorService executor;
    private final HttpClient client;

    public WebsiteExportService(JavaPlugin plugin, ApiConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "CannaSMP-WebsiteExport");
            thread.setDaemon(true);
            return thread;
        });
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public void export(StatsSnapshot snapshot) {
        if (!config.websiteExportEnabled()) {
            return;
        }
        Map<String, Object> publicSnapshot = new LinkedHashMap<>();
        publicSnapshot.put("ok", true);
        publicSnapshot.put("snapshotTime", snapshot.createdAt().toString());
        publicSnapshot.put("server", snapshot.server());
        publicSnapshot.put("system", snapshot.system());
        publicSnapshot.put("players", snapshot.players().stream().map(player -> {
            Map<String, Object> compact = new LinkedHashMap<>();
            compact.put("username", player.get("username"));
            compact.put("uuid", player.get("uuid"));
            compact.put("ping", player.get("ping"));
            compact.put("rank", player.get("rank"));
            compact.put("world", player.get("world"));
            compact.put("afk", player.get("afk"));
            compact.put("balance", player.get("balance"));
            compact.put("playtimeSeconds", player.get("playtimeSeconds"));
            compact.put("level", player.get("level"));
            return compact;
        }).toList());
        publicSnapshot.put("integrations", snapshot.integrations());
        String json = JsonUtil.stringify(publicSnapshot);
        executor.execute(() -> {
            writeLocal(json);
            publishToGitHub(json);
        });
    }

    private void writeLocal(String json) {
        try {
            Path path = plugin.getDataFolder().toPath().resolve(config.websiteExportFile()).normalize();
            Files.createDirectories(path.getParent());
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to write website export JSON", ex);
        }
    }

    private void publishToGitHub(String json) {
        if (!config.githubPublishEnabled() || config.githubToken() == null || config.githubToken().isBlank()) {
            return;
        }
        try {
            String encodedPath = config.githubPath().replace(" ", "%20");
            URI uri = URI.create("https://api.github.com/repos/" + config.githubOwner() + "/" + config.githubRepo() + "/contents/" + encodedPath);
            String sha = currentSha(uri);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Update live CannaSMP server status");
            body.put("branch", config.githubBranch());
            body.put("content", Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));
            if (sha != null) {
                body.put("sha", sha);
            }
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + config.githubToken())
                    .PUT(HttpRequest.BodyPublishers.ofString(JsonUtil.stringify(body)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                plugin.getLogger().warning("GitHub website export failed with HTTP " + response.statusCode());
            }
        } catch (IOException | InterruptedException | RuntimeException ex) {
            Thread.currentThread().interrupt();
            plugin.getLogger().log(Level.WARNING, "Failed to publish website export to GitHub", ex);
        }
    }

    private String currentSha(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + config.githubToken())
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            return null;
        }
        String body = response.body();
        int key = body.indexOf("\"sha\"");
        if (key < 0) {
            return null;
        }
        int firstQuote = body.indexOf('"', body.indexOf(':', key) + 1);
        int secondQuote = body.indexOf('"', firstQuote + 1);
        return firstQuote >= 0 && secondQuote > firstQuote ? body.substring(firstQuote + 1, secondQuote) : null;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
