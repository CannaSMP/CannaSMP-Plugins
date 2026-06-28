/*
 * Decompiled with CFR 0.152.
 */
package me.boundpvp.koth;

import java.util.List;
import me.boundpvp.koth.BoundPVPKothPlugin;
import org.bukkit.configuration.file.FileConfiguration;

final class KothConfig {
    final String prefix;
    final String world;
    final String warpCommand;
    final int minX;
    final int maxX;
    final int minY;
    final int maxY;
    final int minZ;
    final int maxZ;
    final int captureSeconds;
    final List<String> rewardCommands;
    final boolean autoStartEnabled;
    final int autoStartIntervalSeconds;
    final int autoStartDelaySeconds;
    final List<String> adminGroups;

    private KothConfig(String string, String string2, String string3, int n, int n2, int n3, int n4, int n5, int n6, int n7, List<String> list, boolean bl, int n8, int n9, List<String> list2) {
        this.prefix = string;
        this.world = string2;
        this.warpCommand = string3;
        this.minX = Math.min(n, n2);
        this.maxX = Math.max(n, n2);
        this.minY = Math.min(n3, n4);
        this.maxY = Math.max(n3, n4);
        this.minZ = Math.min(n5, n6);
        this.maxZ = Math.max(n5, n6);
        this.captureSeconds = n7;
        this.rewardCommands = list;
        this.autoStartEnabled = bl;
        this.autoStartIntervalSeconds = n8;
        this.autoStartDelaySeconds = n9;
        this.adminGroups = list2;
    }

    static KothConfig from(BoundPVPKothPlugin boundPVPKothPlugin) {
        FileConfiguration fileConfiguration = boundPVPKothPlugin.getConfig();
        return new KothConfig(fileConfiguration.getString("prefix", "&8[&c&lKOTH&8]&7"), fileConfiguration.getString("world", "Koth"), fileConfiguration.getString("warp-command", "warp koth"), fileConfiguration.getInt("zone.x1", 90), fileConfiguration.getInt("zone.x2", 96), fileConfiguration.getInt("zone.y1", -54), fileConfiguration.getInt("zone.y2", -50), fileConfiguration.getInt("zone.z1", 169), fileConfiguration.getInt("zone.z2", 175), fileConfiguration.getInt("capture-seconds", 180), fileConfiguration.getStringList("reward-commands"), fileConfiguration.getBoolean("auto-start.enabled", true), fileConfiguration.getInt("auto-start.interval-seconds", 7200), fileConfiguration.getInt("auto-start.start-delay-seconds", 7200), fileConfiguration.getStringList("admin-groups"));
    }
}

