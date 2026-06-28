package net.cannasmp.statsapi.managers;

import net.cannasmp.statsapi.config.ApiConfig;
import net.cannasmp.statsapi.events.SnapshotRefreshEvent;
import net.cannasmp.statsapi.models.StatsSnapshot;
import net.cannasmp.statsapi.services.PlayerDataService;
import net.cannasmp.statsapi.services.PluginDataService;
import net.cannasmp.statsapi.services.ServerDataService;
import net.cannasmp.statsapi.services.SystemMetricsService;
import net.cannasmp.statsapi.storage.WebsiteExportService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public final class SnapshotManager {
    private final JavaPlugin plugin;
    private final ApiConfig config;
    private final IntegrationManager integrations;
    private final WebsiteExportService websiteExportService;
    private final AtomicReference<StatsSnapshot> snapshot = new AtomicReference<>(StatsSnapshot.empty());
    private final ServerDataService serverDataService = new ServerDataService(Instant.now());
    private final SystemMetricsService systemMetricsService = new SystemMetricsService();
    private final PluginDataService pluginDataService = new PluginDataService();
    private BukkitTask task;

    public SnapshotManager(JavaPlugin plugin, ApiConfig config, IntegrationManager integrations, WebsiteExportService websiteExportService) {
        this.plugin = plugin;
        this.config = config;
        this.integrations = integrations;
        this.websiteExportService = websiteExportService;
    }

    public void start() {
        refresh();
        long periodTicks = Math.max(20L, config.refreshSeconds() * 20L);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, periodTicks, periodTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public StatsSnapshot current() {
        return snapshot.get();
    }

    public void refresh() {
        try {
            integrations.refresh();
            PlayerDataService playerDataService = new PlayerDataService(integrations);
            List<Map<String, Object>> players = Bukkit.getOnlinePlayers().stream()
                    .map((Player player) -> playerDataService.collect(player))
                    .toList();
            StatsSnapshot next = new StatsSnapshot(
                    Instant.now(),
                    serverDataService.collect(config.publicHost()),
                    systemMetricsService.collect(),
                    players,
                    pluginDataService.collect(),
                    integrations.statusMap()
            );
            snapshot.set(next);
            Bukkit.getPluginManager().callEvent(new SnapshotRefreshEvent(next));
            websiteExportService.export(next);
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to refresh API snapshot", ex);
        }
    }
}
