package net.cannasmp.statsapi.listeners;

import net.cannasmp.statsapi.managers.SnapshotManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerActivityListener implements Listener {
    private final SnapshotManager snapshots;

    public PlayerActivityListener(SnapshotManager snapshots) {
        this.snapshots = snapshots;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        snapshots.refresh();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        snapshots.refresh();
    }
}
