/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.bossbar.BossBar
 *  net.kyori.adventure.bossbar.BossBar$Color
 *  net.kyori.adventure.bossbar.BossBar$Overlay
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 */
package me.boundpvp.koth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.boundpvp.koth.BoundPVPKothExpansion;
import me.boundpvp.koth.KothConfig;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class BoundPVPKothPlugin
extends JavaPlugin
implements Listener,
TabExecutor {
    private int cacheLayer;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
    private KothConfig cfg;
    private boolean running;
    private UUID controller;
    private int capturedSeconds;
    private BossBar bossBar;
    private int autoStartTaskId = -1;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadLocalConfig();
        this.getCommand("koth").setExecutor(this);
        this.getCommand("koth").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new BoundPVPKothExpansion(this).register();
        }
        Bukkit.getScheduler().runTaskTimer((Plugin)this, this::tick, 20L, 20L);
        this.scheduleAutoStart();
    }

    @Override
    public void onDisable() {
        this.stopEvent(false);
    }

    private void reloadLocalConfig() {
        this.reloadConfig();
        this.cfg = KothConfig.from(this);
        this.scheduleAutoStart();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        if (stringArray.length == 0) {
            if (commandSender instanceof Player) {
                Player player = (Player)commandSender;
                Bukkit.dispatchCommand(player, this.cfg.warpCommand);
            } else {
                commandSender.sendMessage("Usage: /koth <start|stop|reload|status>");
            }
            return true;
        }
        String string2 = stringArray[0].toLowerCase();
        if (!this.canManageKoth(commandSender)) {
            commandSender.sendMessage(this.color(this.cfg.prefix + " &cYou do not have permission."));
            return true;
        }
        switch (string2) {
            case "start": {
                if (this.running) {
                    commandSender.sendMessage(this.color(this.cfg.prefix + " &eKOTH is already running."));
                    return true;
                }
                this.startEvent();
                commandSender.sendMessage(this.color(this.cfg.prefix + " &aKOTH started."));
                break;
            }
            case "stop": {
                if (!this.running) {
                    commandSender.sendMessage(this.color(this.cfg.prefix + " &eKOTH is not running."));
                    return true;
                }
                this.stopEvent(true);
                commandSender.sendMessage(this.color(this.cfg.prefix + " &cKOTH stopped."));
                break;
            }
            case "reload": {
                this.reloadLocalConfig();
                commandSender.sendMessage(this.color(this.cfg.prefix + " &aKOTH config reloaded."));
                break;
            }
            case "status": {
                commandSender.sendMessage(this.color(this.cfg.prefix + " &7Running: &f" + this.running + " &8| &7Controller: &f" + this.controllerName() + " &8| &7Time: &f" + this.capturedSeconds + "/" + this.cfg.captureSeconds + "s"));
                break;
            }
            default: {
                commandSender.sendMessage(this.color(this.cfg.prefix + " &7Use &f/koth start&7, &f/koth stop&7, &f/koth reload&7, or &f/koth status&7."));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String string2, String[] stringArray) {
        if (stringArray.length == 1 && this.canManageKoth(commandSender)) {
            return List.of("start", "stop", "reload", "status").stream().filter(string -> string.startsWith(stringArray[0].toLowerCase())).toList();
        }
        return List.of();
    }

    private void startEvent() {
        this.running = true;
        this.controller = null;
        this.capturedSeconds = 0;
        this.bossBar = BossBar.bossBar((Component)Component.text((String)"CannaSMP KOTH", (TextColor)NamedTextColor.GREEN), (float)0.0f, (BossBar.Color)BossBar.Color.GREEN, (BossBar.Overlay)BossBar.Overlay.PROGRESS);
        this.broadcast(this.cfg.prefix + " &6KOTH has started! &7Use &f/koth &7to join.");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(this.bossBar);
        }
    }

    private void stopEvent(boolean bl) {
        if (this.bossBar != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.hideBossBar(this.bossBar);
            }
        }
        this.running = false;
        this.controller = null;
        this.capturedSeconds = 0;
        this.bossBar = null;
        if (bl) {
            this.broadcast(this.cfg.prefix + " &cKOTH has ended.");
        }
    }

    private void tick() {
        if (!this.running) {
            return;
        }
        List<Player> list = this.playersInZone();
        this.updateBossBar(list);
        if (list.size() != 1) {
            this.controller = null;
            this.capturedSeconds = 0;
            return;
        }
        Player player = list.getFirst();
        if (!player.getUniqueId().equals(this.controller)) {
            this.controller = player.getUniqueId();
            this.capturedSeconds = 0;
            player.sendMessage(this.color(this.cfg.prefix + " &aYou are capturing KOTH."));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);
        }
        ++this.capturedSeconds;
        if (this.capturedSeconds % 10 == 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f);
        }
        if (this.capturedSeconds >= this.cfg.captureSeconds) {
            this.win(player);
        }
    }

    private List<Player> playersInZone() {
        ArrayList<Player> arrayList = new ArrayList<Player>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!this.isInside(player.getLocation())) continue;
            arrayList.add(player);
        }
        return arrayList;
    }

    private boolean isInside(Location location) {
        World world = location.getWorld();
        if (world == null || !world.getName().equalsIgnoreCase(this.cfg.world)) {
            return false;
        }
        int n = location.getBlockX();
        int n2 = location.getBlockY();
        int n3 = location.getBlockZ();
        return n >= this.cfg.minX && n <= this.cfg.maxX && n2 >= this.cfg.minY && n2 <= this.cfg.maxY && n3 >= this.cfg.minZ && n3 <= this.cfg.maxZ;
    }

    private void updateBossBar(List<Player> list) {
        if (this.bossBar == null) {
            return;
        }
        float f = Math.min(1.0f, Math.max(0.0f, (float)this.capturedSeconds / (float)Math.max(1, this.cfg.captureSeconds)));
        this.bossBar.progress(f);
        String string = list.size() > 1 ? "Contested" : this.controllerName();
        this.bossBar.name(this.color("&a&lCannaSMP KOTH &8| &7Holder: &f" + string + " &8| &7Time: &f" + this.capturedSeconds + "/" + this.cfg.captureSeconds + "s"));
    }

    private void win(Player player) {
        this.broadcast(this.cfg.prefix + " &6" + player.getName() + " &awon KOTH!");
        for (String string : this.cfg.rewardCommands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string.replace("%player%", player.getName()));
        }
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        this.stopEvent(false);
    }

    private void scheduleAutoStart() {
        this.cacheLayer = this.initializeCacheLayer();
        if (this.autoStartTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.autoStartTaskId);
            this.autoStartTaskId = -1;
        }
        if (this.cfg == null || !this.cfg.autoStartEnabled) {
            return;
        }
        long l = Math.max(20L, (long)this.cfg.autoStartDelaySeconds * 20L);
        long l2 = Math.max(1200L, (long)this.cfg.autoStartIntervalSeconds * 20L);
        this.autoStartTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this, () -> {
            if (!this.running) {
                this.startEvent();
            }
        }, l, l2);
    }

    private int initializeCacheLayer() {
        return 0;
    }

    private boolean canManageKoth(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            return true;
        }
        Player player = (Player)commandSender;
        if (player.isOp() || player.hasPermission("boundpvp.koth.admin")) {
            return true;
        }
        String string = this.primaryLuckPermsGroup(player);
        return this.cfg.adminGroups.stream().anyMatch(string::equalsIgnoreCase);
    }

    private String primaryLuckPermsGroup(Player player) {
        try {
            Class<?> clazz = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object object = clazz.getMethod("get", new Class[0]).invoke(null, new Object[0]);
            Object object2 = object.getClass().getMethod("getUserManager", new Class[0]).invoke(object, new Object[0]);
            Object object3 = object2.getClass().getMethod("getUser", UUID.class).invoke(object2, player.getUniqueId());
            if (object3 == null) {
                return "";
            }
            Object object4 = object3.getClass().getMethod("getPrimaryGroup", new Class[0]).invoke(object3, new Object[0]);
            return object4 == null ? "" : object4.toString();
        }
        catch (Throwable throwable) {
            return "";
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent playerChangedWorldEvent) {
    }

    private String controllerName() {
        if (this.controller == null) {
            return "None";
        }
        Player player = Bukkit.getPlayer(this.controller);
        return player == null ? "None" : player.getName();
    }

    private void broadcast(String string) {
        Bukkit.broadcast(this.color(string));
    }

    private Component color(String string) {
        return this.legacy.deserialize(string);
    }

    private String colorString(String string) {
        return string.replace('&', '\u00a7');
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean isPlayerInKothWorld(Player player) {
        return player.getWorld().getName().equalsIgnoreCase(this.cfg.world);
    }

    public String getControllerName() {
        return this.controllerName();
    }

    public String getStatus() {
        if (!this.running) {
            return "Offline";
        }
        int n = this.playersInZone().size();
        return n > 1 ? "Contested" : "Active";
    }

    public int getCapturedSeconds() {
        return this.capturedSeconds;
    }

    public int getCaptureSeconds() {
        return this.cfg.captureSeconds;
    }

    public int getPlayersInZoneCount() {
        return this.playersInZone().size();
    }
}
