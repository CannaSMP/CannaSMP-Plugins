package net.cannasmp.afkworld;

import ch.njol.skript.variables.Variables;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CannaSMPAfkWorldPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, Location> pendingAfkTeleports = new ConcurrentHashMap<>();
    private AfkWorldExpansion expansion;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            expansion = new AfkWorldExpansion(this);
            expansion.register();
            getLogger().info("Registered PlaceholderAPI placeholder %cannasmp_afk_tag%.");
        } else {
            getLogger().warning("PlaceholderAPI was not found. TAB AFK tag placeholder disabled.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /afk.");
            return true;
        }

        startAfkTeleport(player);
        return true;
    }

    @Override
    public void onDisable() {
        if (expansion != null && expansion.isRegistered()) {
            expansion.unregister();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAfkCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage().trim().toLowerCase(Locale.ROOT);
        if (!raw.equals("/afk") && !raw.equals("/essentials:afk")) {
            return;
        }

        event.setCancelled(true);
        startAfkTeleport(event.getPlayer());
    }

    private void startAfkTeleport(Player player) {
        pendingAfkTeleports.put(player.getUniqueId(), player.getLocation().clone());
        player.sendMessage("\u00a78[\u00a7aAFK\u00a78] \u00a77Teleporting to AFK in \u00a7f5 seconds\u00a77. Do not move.");
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Location start = pendingAfkTeleports.remove(player.getUniqueId());
            if (start == null) {
                return;
            }
            ensureInAfkWorld(player);
        }, 100L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location start = pendingAfkTeleports.get(event.getPlayer().getUniqueId());
        if (start == null || event.getTo() == null) {
            return;
        }

        Location to = event.getTo();
        if (!start.getWorld().equals(to.getWorld())
                || start.getBlockX() != to.getBlockX()
                || start.getBlockY() != to.getBlockY()
                || start.getBlockZ() != to.getBlockZ()) {
            pendingAfkTeleports.remove(event.getPlayer().getUniqueId());
            event.getPlayer().sendMessage("\u00a78[\u00a7aAFK\u00a78] \u00a7cTeleport cancelled because you moved.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingAfkTeleports.remove(event.getPlayer().getUniqueId());
    }

    boolean isAfkWorld(World world) {
        return world != null && world.getName().toLowerCase(Locale.ROOT).equals("afk");
    }

    private void ensureInAfkWorld(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        World afkWorld = Bukkit.getWorld("afk");
        if (afkWorld == null) {
            player.sendMessage("\u00a78[\u00a7aAFK\u00a78] \u00a7cThe AFK world is not loaded.");
            return;
        }

        Location afkSpawn = new Location(afkWorld, 0.50, -54.00, 0.50, 180.00f, 0.00f);
        player.teleport(afkSpawn);
    }

    private static final class AfkWorldExpansion extends PlaceholderExpansion {
        private final CannaSMPAfkWorldPlugin plugin;
        private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.US);

        private AfkWorldExpansion(CannaSMPAfkWorldPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getIdentifier() {
            return "cannasmp";
        }

        @Override
        public String getAuthor() {
            return "ItzOnlyFisher/Codex";
        }

        @Override
        public String getVersion() {
            return plugin.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onPlaceholderRequest(Player player, String params) {
            return resolve(player, params);
        }

        @Override
        public String onRequest(OfflinePlayer player, String params) {
            return resolve(player, params);
        }

        private String resolve(OfflinePlayer player, String params) {
            if (player == null || params == null) {
                return "";
            }
            String key = params.toLowerCase(Locale.ROOT);
            if (key.equals("afk_tag")) {
                if (player instanceof Player onlinePlayer) {
                    return plugin.isAfkWorld(onlinePlayer.getWorld()) ? " &8[&eAFK&8]" : "";
                }
                return "";
            }
            if (key.equals("afk")) {
                if (player instanceof Player onlinePlayer) {
                    return plugin.isAfkWorld(onlinePlayer.getWorld()) ? "yes" : "no";
                }
                return "no";
            }
            if (key.equals("shards") || key.equals("afk_shards") || key.equals("shards_formatted")) {
                long shards = getShards(player);
                return key.equals("shards_formatted") ? numberFormat.format(shards) : Long.toString(shards);
            }
            return null;
        }

        private long getShards(OfflinePlayer player) {
            Object value = Variables.getVariable("shards.balance." + player.getUniqueId(), null, false);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String string) {
                try {
                    return Long.parseLong(string.replace(",", "").trim());
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
            return 0;
        }
    }
}
