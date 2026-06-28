package net.cannasmp.statsapi.services;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PluginDataService {
    public List<Map<String, Object>> collect() {
        return Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .sorted(Comparator.comparing(Plugin::getName, String.CASE_INSENSITIVE_ORDER))
                .map(plugin -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", plugin.getName());
                    map.put("version", plugin.getDescription().getVersion());
                    map.put("enabled", plugin.isEnabled());
                    map.put("authors", plugin.getDescription().getAuthors());
                    return map;
                })
                .toList();
    }
}
