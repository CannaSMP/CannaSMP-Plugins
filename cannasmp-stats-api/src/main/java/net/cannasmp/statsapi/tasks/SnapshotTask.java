package net.cannasmp.statsapi.tasks;

import net.cannasmp.statsapi.managers.SnapshotManager;

public final class SnapshotTask implements Runnable {
    private final SnapshotManager snapshots;

    public SnapshotTask(SnapshotManager snapshots) {
        this.snapshots = snapshots;
    }

    @Override
    public void run() {
        snapshots.refresh();
    }
}
