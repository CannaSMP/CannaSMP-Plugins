package net.cannasmp.statsapi.services;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ServerDataService {
    private final Instant startedAt;

    public ServerDataService(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Map<String, Object> collect(String publicHost) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("online", true);
        map.put("publicHost", publicHost);
        map.put("version", Bukkit.getVersion());
        map.put("minecraftVersion", Bukkit.getMinecraftVersion());
        map.put("motd", Bukkit.getMotd());
        map.put("currentPlayers", Bukkit.getOnlinePlayers().size());
        map.put("maxPlayers", Bukkit.getMaxPlayers());
        map.put("uptimeSeconds", Duration.between(startedAt, Instant.now()).toSeconds());
        map.put("tps", tps());
        map.put("mspt", mspt());
        map.put("tickUsage", tickUsage());
        map.put("worlds", Bukkit.getWorlds().stream().map(this::world).toList());
        map.put("restartTime", null);
        return map;
    }

    private Map<String, Object> world(World world) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", world.getName());
        map.put("environment", world.getEnvironment().name());
        map.put("players", world.getPlayers().size());
        map.put("time", world.getTime());
        map.put("fullTime", world.getFullTime());
        map.put("weather", weather(world));
        map.put("loadedChunks", world.getLoadedChunks().length);
        return map;
    }

    private String weather(World world) {
        if (world.isThundering()) {
            return "THUNDER";
        }
        if (world.hasStorm()) {
            return "RAIN";
        }
        return "CLEAR";
    }

    private Map<String, Object> tps() {
        double[] tps = Bukkit.getTPS();
        return Map.of(
                "oneMinute", round(value(tps, 0)),
                "fiveMinute", round(value(tps, 1)),
                "fifteenMinute", round(value(tps, 2))
        );
    }

    private Map<String, Object> mspt() {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getTickTimes");
            Object value = method.invoke(Bukkit.getServer());
            if (value instanceof long[] times && times.length > 0) {
                List<Double> values = Arrays.stream(times).mapToDouble(ns -> ns / 1_000_000D).boxed().toList();
                double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
                double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0D);
                double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0D);
                return Map.of("average", round(average), "min", round(min), "max", round(max));
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getAverageTickTime");
            Object value = method.invoke(Bukkit.getServer());
            if (value instanceof Number number) {
                return Map.of("average", round(number.doubleValue()), "min", 0D, "max", 0D);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return Map.of("average", 0D, "min", 0D, "max", 0D);
    }

    private Map<String, Object> tickUsage() {
        Object average = mspt().get("average");
        double mspt = average instanceof Number number ? number.doubleValue() : 0D;
        return Map.of("percentOfTickBudget", round((mspt / 50D) * 100D));
    }

    private double value(double[] values, int index) {
        return values.length > index ? values[index] : 0D;
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }
}
