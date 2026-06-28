package net.cannasmp.statsapi;

import net.cannasmp.statsapi.authentication.ApiKeyAuthenticator;
import net.cannasmp.statsapi.authentication.IpWhitelist;
import net.cannasmp.statsapi.authentication.RateLimiter;
import net.cannasmp.statsapi.commands.ServerApiCommand;
import net.cannasmp.statsapi.config.ApiConfig;
import net.cannasmp.statsapi.listeners.PlayerActivityListener;
import net.cannasmp.statsapi.managers.IntegrationManager;
import net.cannasmp.statsapi.managers.SnapshotManager;
import net.cannasmp.statsapi.storage.WebsiteExportService;
import net.cannasmp.statsapi.web.ApiServer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class CannaSMPStatsApiPlugin extends JavaPlugin {
    private ApiConfig apiConfig;
    private IntegrationManager integrationManager;
    private SnapshotManager snapshotManager;
    private WebsiteExportService websiteExportService;
    private ApiServer apiServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startServices();
        registerCommand();
    }

    @Override
    public void onDisable() {
        stopServices();
    }

    public void reloadPlugin() {
        reloadConfig();
        stopServices();
        startServices();
    }

    public ApiConfig apiConfig() {
        return apiConfig;
    }

    public SnapshotManager snapshotManager() {
        return snapshotManager;
    }

    public IntegrationManager integrationManager() {
        return integrationManager;
    }

    public ApiServer apiServer() {
        return apiServer;
    }

    private void startServices() {
        apiConfig = ApiConfig.from(this);
        integrationManager = new IntegrationManager(this, apiConfig);
        integrationManager.refresh();

        websiteExportService = new WebsiteExportService(this, apiConfig);
        snapshotManager = new SnapshotManager(this, apiConfig, integrationManager, websiteExportService);
        snapshotManager.start();
        getServer().getPluginManager().registerEvents(new PlayerActivityListener(snapshotManager), this);

        ApiKeyAuthenticator authenticator = new ApiKeyAuthenticator(apiConfig);
        IpWhitelist whitelist = new IpWhitelist(apiConfig);
        RateLimiter rateLimiter = new RateLimiter(apiConfig);
        apiServer = new ApiServer(this, apiConfig, snapshotManager, integrationManager, authenticator, whitelist, rateLimiter);
        apiServer.start();

        getLogger().info("CannaSMP ServerAPI enabled on " + apiConfig.bindHost() + ":" + apiConfig.port());
    }

    private void stopServices() {
        if (apiServer != null) {
            apiServer.stop();
            apiServer = null;
        }
        if (snapshotManager != null) {
            snapshotManager.stop();
            snapshotManager = null;
        }
        if (websiteExportService != null) {
            try {
                websiteExportService.close();
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, "Failed to close website export service", ex);
            }
            websiteExportService = null;
        }
    }

    private void registerCommand() {
        ServerApiCommand executor = new ServerApiCommand(this);
        PluginCommand serverApi = getCommand("serverapi");
        if (serverApi != null) {
            serverApi.setExecutor(executor);
            serverApi.setTabCompleter(executor);
        }
        PluginCommand statsApi = getCommand("statsapi");
        if (statsApi != null) {
            statsApi.setExecutor(executor);
            statsApi.setTabCompleter(executor);
        }
    }
}
