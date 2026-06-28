package net.cannasmp.shardleaderboard;

import ch.njol.skript.variables.Variables;
import ch.njol.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CannaSMPShardLeaderboardPlugin extends JavaPlugin {
    private static final String TAG = "cannasmp_shard_lb";
    private static final String WORLD_NAME = "afk";
    private static final double X = 0.50;
    private static final double Y = -53.00;
    private static final double Z = 6.00;
    private static final double LINE_SPACING = 0.28;

    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.US);

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTaskLater(this, this::updateLeaderboard, 60L);
        Bukkit.getScheduler().runTaskTimer(this, this::updateLeaderboard, 20L * 60L, 20L * 60L);
    }

    @Override
    public void onDisable() {
        clearDisplays();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        updateLeaderboard();
        sender.sendMessage(ChatColor.GREEN + "Shard leaderboard refreshed.");
        return true;
    }

    private void updateLeaderboard() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            getLogger().warning("World '" + WORLD_NAME + "' is not loaded; shard leaderboard skipped.");
            return;
        }

        clearDisplays(world);
        List<Entry> entries = topEntries();
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GREEN + "" + ChatColor.BOLD + "CannaSMP Shards");
        lines.add(ChatColor.DARK_GRAY + "----------------");
        lines.add(ChatColor.GRAY + "Top 10 AFK earners");

        for (int i = 0; i < 10; i++) {
            if (i < entries.size()) {
                Entry entry = entries.get(i);
                lines.add(rankColor(i + 1) + "#" + (i + 1) + " "
                        + ChatColor.WHITE + entry.name()
                        + ChatColor.GRAY + " - "
                        + ChatColor.GREEN + numberFormat.format(entry.shards()));
            } else {
                lines.add(ChatColor.DARK_GRAY + "#" + (i + 1) + " Empty");
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            Location location = new Location(world, X, Y - (i * LINE_SPACING), Z, 180.0f, 0.0f);
            TextDisplay display = (TextDisplay) world.spawnEntity(location, EntityType.TEXT_DISPLAY);
            display.addScoreboardTag(TAG);
            display.setPersistent(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setText(lines.get(i));
        }
    }

    private List<Entry> topEntries() {
        List<Entry> entries = new ArrayList<>();
        Iterator<Pair<String, Object>> iterator = Variables.getVariableIterator("shards.balance", false, null);
        while (iterator != null && iterator.hasNext()) {
            Pair<String, Object> pair = iterator.next();
            String uuidText = pair.getFirst();
            long shards = toLong(pair.getSecond());
            if (shards <= 0 || uuidText == null || uuidText.isBlank()) {
                continue;
            }

            try {
                UUID uuid = UUID.fromString(uuidText);
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                String name = player.getName() != null ? player.getName() : uuidText.substring(0, 8);
                entries.add(new Entry(name, shards));
            } catch (IllegalArgumentException ignored) {
                entries.add(new Entry(uuidText, shards));
            }
        }
        entries.sort(Comparator.comparingLong(Entry::shards).reversed().thenComparing(Entry::name));
        return entries.size() > 10 ? new ArrayList<>(entries.subList(0, 10)) : entries;
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string.replace(",", "").trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private ChatColor rankColor(int rank) {
        return switch (rank) {
            case 1 -> ChatColor.GOLD;
            case 2 -> ChatColor.GRAY;
            case 3 -> ChatColor.YELLOW;
            default -> ChatColor.GREEN;
        };
    }

    private void clearDisplays() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world != null) {
            clearDisplays(world);
        }
    }

    private void clearDisplays(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains(TAG)) {
                entity.remove();
            }
        }
    }

    private record Entry(String name, long shards) {
    }
}
