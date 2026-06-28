package net.cannasmp.tools;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class CannaSMPToolsPlugin extends JavaPlugin implements Listener {
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private final Random random = new Random();
    private final HttpClient http = HttpClient.newHttpClient();
    private final Map<String, String> emojis = new LinkedHashMap<>();
    private final List<Pattern> blockedPatterns = new ArrayList<>();
    private File jokesFile;
    private FileConfiguration jokes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadRuntimeConfig();
        loadJokes();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("clearchat")) {
            return clearChat(sender);
        }
        if (name.equals("canntools")) {
            if (!sender.hasPermission("cannasmp.tools.staff")) {
                sender.sendMessage(color("&cNo permission."));
                return true;
            }
            reloadConfig();
            loadRuntimeConfig();
            loadJokes();
            sender.sendMessage(color("&aCannaSMP tools reloaded."));
            return true;
        }
        if (name.equals("jokeadd")) {
            return staffAddJoke(sender, join(args, 0));
        }
        if (name.equals("jokeremove")) {
            return staffRemoveJoke(sender, args);
        }
        if (name.equals("jokelist")) {
            return staffListJokes(sender);
        }
        if (name.equals("jokepending")) {
            return staffPendingJokes(sender);
        }
        if (name.equals("joke")) {
            return handleJoke(sender, args);
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        String text = plain.serialize(event.message());
        if (isBlocked(text) && !event.getPlayer().hasPermission(getConfig().getString("chat-filter.bypass-permission", "cannasmp.tools.staff"))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(color(getConfig().getString("chat-filter.blocked-message", "&cKeep chat clean.")));
            warnStaff(event.getPlayer().getName(), text);
            return;
        }
        String replaced = replaceEmojis(text);
        if (!replaced.equals(text)) {
            event.message(Component.text(replaced));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSign(SignChangeEvent event) {
        for (int i = 0; i < 4; i++) {
            Component line = event.line(i);
            if (line == null) {
                continue;
            }
            String text = plain.serialize(line);
            if (isBlocked(text) && !event.getPlayer().hasPermission(getConfig().getString("chat-filter.bypass-permission", "cannasmp.tools.staff"))) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(color("&cCannaSMP &8> &cThat sign has blocked words."));
                return;
            }
            String replaced = replaceEmojis(text);
            if (!replaced.equals(text)) {
                event.line(i, Component.text(replaced));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        String rename = event.getInventory().getRenameText();
        if (rename == null || rename.isBlank()) {
            return;
        }
        String replaced = replaceEmojis(rename);
        if (isBlocked(replaced)) {
            event.setResult(new ItemStack(Material.AIR));
            return;
        }
        if (replaced.equals(rename) || event.getResult() == null) {
            return;
        }
        ItemStack result = event.getResult().clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.displayName(Component.text(replaced));
        result.setItemMeta(meta);
        event.setResult(result);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!getConfig().getBoolean("chat-filter.filter-commands", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission(getConfig().getString("chat-filter.bypass-permission", "cannasmp.tools.staff"))) {
            return;
        }
        String message = event.getMessage();
        String lower = message.toLowerCase(Locale.ROOT);
        boolean shouldCheck = false;
        for (String prefix : getConfig().getStringList("chat-filter.checked-command-prefixes")) {
            String normalized = prefix.toLowerCase(Locale.ROOT).trim();
            if (normalized.isBlank()) {
                continue;
            }
            if (lower.equals(normalized) || lower.startsWith(normalized + " ")) {
                shouldCheck = true;
                break;
            }
        }
        if (!shouldCheck) {
            return;
        }
        if (isBlocked(message)) {
            event.setCancelled(true);
            player.sendMessage(color(getConfig().getString("chat-filter.command-message", "&cYou cannot use blocked words in commands.")));
            warnStaff(player.getName(), message);
        }
    }

    private boolean clearChat(CommandSender sender) {
        if (!sender.hasPermission("cannasmp.tools.clearchat")) {
            sender.sendMessage(color("&cNo permission."));
            return true;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("cannasmp.tools.staff")) {
                player.sendMessage(color("&8[&aCannaSMP&8] &7Chat was cleared by &f" + sender.getName() + "&7."));
                continue;
            }
            for (int i = 0; i < 120; i++) {
                player.sendMessage(Component.empty());
            }
            player.sendMessage(color("&aCannaSMP &8> &7Chat was cleared."));
        }
        Bukkit.getConsoleSender().sendMessage("CannaSMP chat cleared by " + sender.getName());
        return true;
    }

    private boolean handleJoke(CommandSender sender, String[] args) {
        if (args.length == 0) {
            List<Map<?, ?>> approved = jokes.getMapList("approved");
            if (approved.isEmpty()) {
                sender.sendMessage(color("&cCannaSMP &8> &7No jokes approved yet. Submit one with &f/joke submit <joke>&7."));
                return true;
            }
            Map<?, ?> joke = approved.get(random.nextInt(approved.size()));
            sender.sendMessage(color("&aCannaSMP Joke &8> &f" + String.valueOf(joke.get("text"))));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("submit")) {
            String text = join(args, 1);
            int min = getConfig().getInt("jokes.min-length", 5);
            int max = getConfig().getInt("jokes.max-length", 180);
            if (text.length() < min || text.length() > max) {
                sender.sendMessage(color("&cCannaSMP &8> &7Jokes must be &f" + min + "-" + max + " &7characters."));
                return true;
            }
            if (isBlocked(text)) {
                sender.sendMessage(color("&cCannaSMP &8> &cThat joke has blocked words."));
                return true;
            }
            int id = nextJokeId();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("text", text);
            row.put("submitter", sender.getName());
            row.put("uuid", sender instanceof Player p ? p.getUniqueId().toString() : "console");
            row.put("submitted-at", Instant.now().toString());
            List<Map<?, ?>> pending = jokes.getMapList("pending");
            pending.add(row);
            jokes.set("pending", pending);
            saveJokes();
            sender.sendMessage(color("&aCannaSMP &8> &7Joke submitted for staff review. ID: &f" + id));
            notifyJokeWebhook(id, sender.getName(), text);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("cannasmp.tools.staff")) {
                    player.sendMessage(color("&aCannaSMP Joke &8> &f" + sender.getName() + " &7submitted joke &f#" + id + "&7. Use &f/joke approve " + id));
                }
            }
            return true;
        }
        if (!sender.hasPermission("cannasmp.tools.staff")) {
            sender.sendMessage(color("&cUsage: /joke or /joke submit <joke>"));
            return true;
        }
        if (sub.equals("pending")) {
            return staffPendingJokes(sender);
        }
        if (sub.equals("add")) {
            return staffAddJoke(sender, join(args, 1));
        }
        if (sub.equals("remove")) {
            return staffRemoveJoke(sender, args.length >= 2 ? new String[]{args[1]} : new String[0]);
        }
        if (sub.equals("list")) {
            return staffListJokes(sender);
        }
        if (sub.equals("approve") || sub.equals("deny")) {
            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /joke " + sub + " <id>"));
                return true;
            }
            int id;
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(color("&cInvalid joke id."));
                return true;
            }
            Map<?, ?> row = removePending(id);
            if (row == null) {
                sender.sendMessage(color("&cNo pending joke with that id."));
                return true;
            }
            if (sub.equals("approve")) {
                List<Map<?, ?>> approved = jokes.getMapList("approved");
                approved.add(row);
                jokes.set("approved", approved);
                Bukkit.broadcast(color("&aCannaSMP Joke &8> &7A new joke was approved. Use &f/joke &7to see one."));
            }
            saveJokes();
            sender.sendMessage(color("&aJoke #" + id + " " + (sub.equals("approve") ? "approved" : "denied") + "."));
            return true;
        }
        if (sub.equals("reload")) {
            reloadConfig();
            loadRuntimeConfig();
            loadJokes();
            sender.sendMessage(color("&aCannaSMP tools reloaded."));
            return true;
        }
        sender.sendMessage(color("&cUsage: /joke [submit|pending|approve|deny|reload]"));
        return true;
    }

    private boolean staffAddJoke(CommandSender sender, String text) {
        if (!sender.hasPermission("cannasmp.tools.staff")) {
            sender.sendMessage(color("&cNo permission."));
            return true;
        }
        int min = getConfig().getInt("jokes.min-length", 5);
        int max = getConfig().getInt("jokes.max-length", 180);
        if (text == null || text.length() < min || text.length() > max) {
            sender.sendMessage(color("&cUsage: /jokeadd <joke> &7(" + min + "-" + max + " chars)"));
            return true;
        }
        int id = nextJokeId();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("text", text);
        row.put("submitter", sender.getName());
        row.put("uuid", sender instanceof Player p ? p.getUniqueId().toString() : "console");
        row.put("submitted-at", Instant.now().toString());
        List<Map<?, ?>> approved = jokes.getMapList("approved");
        approved.add(row);
        jokes.set("approved", approved);
        saveJokes();
        sender.sendMessage(color("&aAdded joke #" + id + "."));
        return true;
    }

    private boolean staffRemoveJoke(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cannasmp.tools.staff")) {
            sender.sendMessage(color("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /jokeremove <id>"));
            return true;
        }
        int id;
        try {
            id = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(color("&cInvalid joke id."));
            return true;
        }
        List<Map<?, ?>> approved = jokes.getMapList("approved");
        for (int i = 0; i < approved.size(); i++) {
            if (asInt(approved.get(i).get("id")) == id) {
                approved.remove(i);
                jokes.set("approved", approved);
                saveJokes();
                sender.sendMessage(color("&aRemoved joke #" + id + "."));
                return true;
            }
        }
        sender.sendMessage(color("&cNo approved joke with that id."));
        return true;
    }

    private boolean staffListJokes(CommandSender sender) {
        if (!sender.hasPermission("cannasmp.tools.staff")) {
            sender.sendMessage(color("&cNo permission."));
            return true;
        }
        List<Map<?, ?>> approved = jokes.getMapList("approved");
        sender.sendMessage(color("&aApproved jokes: &f" + approved.size()));
        for (Map<?, ?> row : approved) {
            sender.sendMessage(color("&7#" + row.get("id") + " &f" + row.get("submitter") + "&8: &7" + row.get("text")));
        }
        return true;
    }

    private boolean staffPendingJokes(CommandSender sender) {
        if (!sender.hasPermission("cannasmp.tools.staff")) {
            sender.sendMessage(color("&cNo permission."));
            return true;
        }
        List<Map<?, ?>> pending = jokes.getMapList("pending");
        sender.sendMessage(color("&aPending jokes: &f" + pending.size()));
        for (Map<?, ?> row : pending) {
            sender.sendMessage(color("&7#" + row.get("id") + " &f" + row.get("submitter") + "&8: &7" + row.get("text")));
        }
        return true;
    }

    private void loadRuntimeConfig() {
        emojis.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("emojis");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                emojis.put(key, section.getString(key, ""));
            }
        }
        blockedPatterns.clear();
        for (String word : getConfig().getStringList("chat-filter.blocked-words")) {
            if (word == null || word.isBlank()) {
                continue;
            }
            blockedPatterns.add(Pattern.compile("(?i)(^|[^a-z0-9])" + Pattern.quote(word) + "([^a-z0-9]|$)"));
        }
    }

    private void loadJokes() {
        jokesFile = new File(getDataFolder(), "jokes.yml");
        jokes = YamlConfiguration.loadConfiguration(jokesFile);
        if (!jokes.isList("approved")) {
            List<Map<String, Object>> defaults = new ArrayList<>();
            int id = 1;
            for (String text : getConfig().getStringList("jokes.defaults")) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", id++);
                row.put("text", text);
                row.put("submitter", "CannaSMP");
                row.put("uuid", "server");
                row.put("submitted-at", Instant.now().toString());
                defaults.add(row);
            }
            jokes.set("approved", defaults);
        }
        if (!jokes.isList("pending")) {
            jokes.set("pending", new ArrayList<>());
        }
        saveJokes();
    }

    private void saveJokes() {
        try {
            jokes.save(jokesFile);
        } catch (IOException ex) {
            getLogger().warning("Could not save jokes.yml: " + ex.getMessage());
        }
    }

    private boolean isBlocked(String text) {
        if (!getConfig().getBoolean("chat-filter.enabled", true) || text == null) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT)
                .replace('1', 'i')
                .replace('!', 'i')
                .replace('3', 'e')
                .replace('4', 'a')
                .replace('@', 'a')
                .replace('0', 'o')
                .replace('$', 's')
                .replace('5', 's')
                .replace('7', 't');
        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(normalized).find()) {
                return true;
            }
        }
        return false;
    }

    private String replaceEmojis(String text) {
        String out = text == null ? "" : text;
        for (Map.Entry<String, String> entry : emojis.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue());
        }
        return out;
    }

    private int nextJokeId() {
        int max = 0;
        for (Map<?, ?> row : jokes.getMapList("approved")) {
            max = Math.max(max, asInt(row.get("id")));
        }
        for (Map<?, ?> row : jokes.getMapList("pending")) {
            max = Math.max(max, asInt(row.get("id")));
        }
        return max + 1;
    }

    private int asInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }

    private Map<?, ?> removePending(int id) {
        List<Map<?, ?>> pending = jokes.getMapList("pending");
        for (int i = 0; i < pending.size(); i++) {
            if (asInt(pending.get(i).get("id")) == id) {
                Map<?, ?> row = pending.remove(i);
                jokes.set("pending", pending);
                return row;
            }
        }
        return null;
    }

    private void notifyJokeWebhook(int id, String submitter, String text) {
        String webhook = getConfig().getString("webhook.joke-submissions", "");
        if (webhook == null || webhook.isBlank()) {
            return;
        }
        String username = getConfig().getString("webhook.username", "CannaSMP Joke Box");
        String avatar = getConfig().getString("webhook.avatar-url", "");
        String payload = "{"
                + "\"username\":\"" + json(username) + "\","
                + (avatar == null || avatar.isBlank() ? "" : "\"avatar_url\":\"" + json(avatar) + "\",")
                + "\"embeds\":[{\"title\":\"New Joke Submission\",\"color\":2262366,"
                + "\"description\":\"" + json(text) + "\","
                + "\"fields\":[{\"name\":\"ID\",\"value\":\"#" + id + "\",\"inline\":true},{\"name\":\"Submitter\",\"value\":\"" + json(submitter) + "\",\"inline\":true},{\"name\":\"Commands\",\"value\":\"/joke approve " + id + "\\n/joke deny " + id + "\"}],"
                + "\"footer\":{\"text\":\"CannaSMP Joke Box\"}}]}";
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(webhook))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();
                http.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ex) {
                getLogger().warning("Joke webhook failed: " + ex.getMessage());
            }
        });
    }

    private void warnStaff(String player, String text) {
        Bukkit.getScheduler().runTask(this, () -> {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("cannasmp.tools.staff")) {
                    staff.sendMessage(color("&cFilter &8> &f" + player + " &7tried: &f" + text));
                }
            }
        });
    }

    private String join(String[] args, int start) {
        if (args.length <= start) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString().trim();
    }

    private String json(String input) {
        return (input == null ? "" : input)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private Component color(String text) {
        return legacy.deserialize(text == null ? "" : text);
    }
}
