package net.cannasmp.shardsplaceholder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CannaSMPShardsPlaceholderPlugin extends JavaPlugin {
    private CannaSMPShardsExpansion expansion;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI is not installed. Shards placeholder disabled.");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("Skript") == null) {
            getLogger().warning("Skript is not installed. Shards placeholder disabled.");
            return;
        }

        expansion = new CannaSMPShardsExpansion(this);
        expansion.register();
        getLogger().info("Registered PlaceholderAPI placeholder %cannasmp_shards%.");
    }

    @Override
    public void onDisable() {
        if (expansion != null && expansion.isRegistered()) {
            expansion.unregister();
        }
    }
}
