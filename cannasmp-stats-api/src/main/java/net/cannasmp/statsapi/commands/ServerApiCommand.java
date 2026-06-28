package net.cannasmp.statsapi.commands;

import net.cannasmp.statsapi.CannaSMPStatsApiPlugin;
import net.cannasmp.statsapi.models.StatsSnapshot;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class ServerApiCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("reload", "status", "api", "version", "key");
    private final CannaSMPStatsApiPlugin plugin;

    public ServerApiCommand(CannaSMPStatsApiPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cannasmp.serverapi.admin")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage("CannaSMP ServerAPI reloaded.");
            }
            case "status" -> sendStatus(sender);
            case "api" -> sender.sendMessage("API base URL: http://" + plugin.apiConfig().publicHost() + ":" + plugin.apiConfig().port() + "/api/v1");
            case "version" -> sender.sendMessage("CannaSMP ServerAPI v" + plugin.getDescription().getVersion());
            case "key" -> sender.sendMessage(plugin.apiConfig().apiKeyConfigured() ? "API key is configured." : "API key is NOT configured.");
            default -> sender.sendMessage("Usage: /" + label + " <reload|status|api|version|key>");
        }
        return true;
    }

    private void sendStatus(CommandSender sender) {
        StatsSnapshot snapshot = plugin.snapshotManager().current();
        sender.sendMessage("CannaSMP ServerAPI: running on " + plugin.apiConfig().bindHost() + ":" + plugin.apiConfig().port());
        sender.sendMessage("Players: " + snapshot.server().get("currentPlayers") + "/" + snapshot.server().get("maxPlayers"));
        sender.sendMessage("Snapshot: " + snapshot.createdAt());
        sender.sendMessage("Integrations: " + snapshot.integrations());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
