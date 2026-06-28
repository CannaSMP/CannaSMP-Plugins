package net.cannasmp.statsapi.events;

import net.cannasmp.statsapi.models.StatsSnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class SnapshotRefreshEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final StatsSnapshot snapshot;

    public SnapshotRefreshEvent(StatsSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public StatsSnapshot snapshot() {
        return snapshot;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
