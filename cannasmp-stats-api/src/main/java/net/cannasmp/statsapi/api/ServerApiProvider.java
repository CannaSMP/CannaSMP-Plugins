package net.cannasmp.statsapi.api;

import net.cannasmp.statsapi.models.StatsSnapshot;

public interface ServerApiProvider {
    StatsSnapshot currentSnapshot();
}
