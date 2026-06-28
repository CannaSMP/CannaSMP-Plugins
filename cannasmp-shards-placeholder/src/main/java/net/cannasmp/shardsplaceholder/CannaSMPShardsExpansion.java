package net.cannasmp.shardsplaceholder;

import ch.njol.skript.variables.Variables;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.text.NumberFormat;
import java.util.Locale;

public final class CannaSMPShardsExpansion extends PlaceholderExpansion {
    private final CannaSMPShardsPlaceholderPlugin plugin;
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.US);

    CannaSMPShardsExpansion(CannaSMPShardsPlaceholderPlugin plugin) {
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
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getUniqueId() == null) {
            return "0";
        }

        String key = params.toLowerCase(Locale.ROOT);
        if (!key.equals("shards") && !key.equals("shards_formatted") && !key.equals("afk_shards")) {
            return null;
        }

        long shards = getShards(player);
        if (key.equals("shards_formatted")) {
            return numberFormat.format(shards);
        }
        return Long.toString(shards);
    }

    private long getShards(OfflinePlayer player) {
        Object value = Variables.getVariable("shards.balance." + player.getUniqueId(), null, false);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
