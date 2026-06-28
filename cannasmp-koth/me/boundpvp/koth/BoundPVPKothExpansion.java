/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 */
package me.boundpvp.koth;

import me.boundpvp.koth.BoundPVPKothPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class BoundPVPKothExpansion
extends PlaceholderExpansion {
    private final BoundPVPKothPlugin plugin;

    BoundPVPKothExpansion(BoundPVPKothPlugin boundPVPKothPlugin) {
        this.plugin = boundPVPKothPlugin;
    }

    public String getIdentifier() {
        return "boundpvpkoth";
    }

    public String getAuthor() {
        return "BoundPVP";
    }

    public String getVersion() {
        return "3.0.0";
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(OfflinePlayer offlinePlayer, String string) {
        Player player = offlinePlayer == null ? null : offlinePlayer.getPlayer();
        return switch (string.toLowerCase()) {
            case "running" -> {
                if (this.plugin.isRunning()) {
                    yield "true";
                }
                yield "false";
            }
            case "in_world" -> {
                if (player != null && this.plugin.isPlayerInKothWorld(player)) {
                    yield "true";
                }
                yield "false";
            }
            case "controller" -> this.plugin.getControllerName();
            case "status" -> this.plugin.getStatus();
            case "time" -> this.plugin.getCapturedSeconds() + "/" + this.plugin.getCaptureSeconds() + "s";
            case "captured" -> String.valueOf(this.plugin.getCapturedSeconds());
            case "required" -> String.valueOf(this.plugin.getCaptureSeconds());
            case "players" -> String.valueOf(this.plugin.getPlayersInZoneCount());
            default -> null;
        };
    }
}

