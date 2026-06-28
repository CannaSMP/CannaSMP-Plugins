package me.cannasmp.discordrank;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class CannaSmpRankCommandPlugin extends JavaPlugin {
    private static final String PERMISSION = "cannasmp.discordbridge.admin";
    private static final Set<String> STAFF_RANKERS = new HashSet<>(Arrays.asList(
            "xxayeceexx",
            "theguywhogodded",
            "itzonlyfisher"
    ));
    private static final Map<String, String> STAFF_RANK_ALIASES = staffRankAliases();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 3 && args[0].equalsIgnoreCase("perms") && isStaffRank(args[2])) {
            return applyStaffRank(sender, args[1], normalizeStaffRank(args[2]), false);
        }

        if (args.length == 2 && isStaffRank(args[1])) {
            return applyStaffRank(sender, args[0], normalizeStaffRank(args[1]), true);
        }

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(color("&cYou do not have permission."));
            return true;
        }

        if (args.length == 0) {
            help(sender);
            return true;
        }

        try {
            String sub = args[0].toLowerCase();
            if (sub.equals("create")) {
                if (args.length != 3) {
                    sender.sendMessage(color("&cUsage: /rank create <luckpermsrankname> <discord role id>"));
                    return true;
                }
                String rank = cleanRank(args[1]);
                String roleId = cleanRoleId(args[2]);
                String text = readConfig();
                text = setMinecraftRole(text, rank, roleId);
                text = setDiscordRank(text, roleId, rank);
                writeConfig(text);
                reloadBridge();
                sender.sendMessage(color("&aMapped LuckPerms rank &f" + rank + " &ato Discord role &f" + roleId + "&a."));
                return true;
            }

            if (sub.equals("remove")) {
                if (args.length != 2) {
                    sender.sendMessage(color("&cUsage: /rank remove <luckpermsrankname>"));
                    return true;
                }
                String rank = cleanRank(args[1]);
                String text = readConfig();
                Map<String, String> mc = minecraftRoleMap(text);
                String roleId = mc.remove(rank);
                text = replaceMinecraftRoleMap(text, mc);
                if (roleId != null) {
                    Map<String, String> dc = discordRankMap(text);
                    dc.remove(roleId);
                    text = replaceDiscordRankMap(text, dc);
                }
                writeConfig(text);
                reloadBridge();
                sender.sendMessage(color("&aRemoved Discord mapping for rank &f" + rank + "&a."));
                return true;
            }

            if (sub.equals("removeid")) {
                if (args.length != 2) {
                    sender.sendMessage(color("&cUsage: /rank removeid <discord role id>"));
                    return true;
                }
                String roleId = cleanRoleId(args[1]);
                String text = readConfig();
                Map<String, String> dc = discordRankMap(text);
                String rank = dc.remove(roleId);
                text = replaceDiscordRankMap(text, dc);
                if (rank != null) {
                    Map<String, String> mc = minecraftRoleMap(text);
                    mc.remove(rank);
                    text = replaceMinecraftRoleMap(text, mc);
                }
                writeConfig(text);
                reloadBridge();
                sender.sendMessage(color("&aRemoved Discord role id mapping &f" + roleId + "&a."));
                return true;
            }

            if (sub.equals("list")) {
                Map<String, String> mc = minecraftRoleMap(readConfig());
                sender.sendMessage(color("&aCannaSMP Discord rank mappings:"));
                if (mc.isEmpty()) {
                    sender.sendMessage(color("&7None yet."));
                } else {
                    mc.forEach((rank, role) -> sender.sendMessage(color("&7- &f" + rank + " &8-> &f" + role)));
                }
                return true;
            }

            if (sub.equals("enable") || sub.equals("disable")) {
                String text = readConfig().replaceFirst("(?m)^enabled:\\s*(true|false)\\s*$", "enabled: " + sub.equals("enable"));
                writeConfig(text);
                reloadBridge();
                sender.sendMessage(color("&aDiscord bridge " + (sub.equals("enable") ? "enabled" : "disabled") + "."));
                return true;
            }
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(color("&c" + ex.getMessage()));
            return true;
        } catch (Exception ex) {
            sender.sendMessage(color("&cCould not update Discord bridge config. Check console."));
            ex.printStackTrace();
            return true;
        }

        help(sender);
        return true;
    }

    private boolean applyStaffRank(CommandSender sender, String target, String rank, boolean setGroup) {
        if (!isTrustedRanker(sender)) {
            sender.sendMessage(color("&cOnly xxayeceexx, theguywhogodded, itzonlyfisher, or console can use this."));
            return true;
        }
        if (!target.matches("[A-Za-z0-9_]{3,16}")) {
            sender.sendMessage(color("&cUse a valid Minecraft username."));
            return true;
        }

        if (setGroup) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target + " parent set " + rank);
        }
        for (String permission : permissionsFor(rank)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target + " permission set " + permission + " true");
        }
        if (setGroup) {
            sender.sendMessage(color("&aApplied &f" + rank + " &adisplay group and per-player permissions to &f" + target + "&a."));
        } else {
            sender.sendMessage(color("&aApplied &f" + rank + " &aper-player permissions to &f" + target + " &awithout changing their prefix/group."));
        }
        return true;
    }

    private boolean isTrustedRanker(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || STAFF_RANKERS.contains(sender.getName().toLowerCase());
    }

    private void help(CommandSender sender) {
        sender.sendMessage(color("&2&m                                                "));
        sender.sendMessage(color("&a&lCannaSMP Rank Tools"));
        sender.sendMessage(color("&a/rank <player> <founder|manager|developer|admin|moderator|helper|builder>"));
        sender.sendMessage(color("&a/rank perms <player> <rank> &7- perms only, no prefix"));
        sender.sendMessage(color("&a/rank create <luckpermsrankname> <discord role id>"));
        sender.sendMessage(color("&a/rank remove <luckpermsrankname>"));
        sender.sendMessage(color("&a/rank removeid <discord role id>"));
        sender.sendMessage(color("&a/rank list"));
        sender.sendMessage(color("&a/rank enable &7or &a/rank disable"));
        sender.sendMessage(color("&2&m                                                "));
    }

    private String readConfig() throws IOException {
        Path path = configPath();
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Missing plugins/CannaSMPDiscordBridge/config.yml.");
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private void writeConfig(String text) throws IOException {
        Files.writeString(configPath(), text, StandardCharsets.UTF_8);
    }

    private Path configPath() {
        return getDataFolder().toPath().getParent().resolve("CannaSMPDiscordBridge").resolve("config.yml");
    }

    private void reloadBridge() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discordbridge reload");
    }

    private static boolean isStaffRank(String rank) {
        return STAFF_RANK_ALIASES.containsKey(rank.toLowerCase());
    }

    private static String normalizeStaffRank(String rank) {
        return STAFF_RANK_ALIASES.get(rank.toLowerCase());
    }

    private static Map<String, String> staffRankAliases() {
        Map<String, String> ranks = new LinkedHashMap<>();
        ranks.put("founder", "founder");
        ranks.put("owner", "founder");
        ranks.put("manager", "manager");
        ranks.put("developer", "developer");
        ranks.put("dev", "developer");
        ranks.put("admin", "admin");
        ranks.put("administrator", "admin");
        ranks.put("moderator", "moderator");
        ranks.put("mod", "moderator");
        ranks.put("helper", "helper");
        ranks.put("builder", "builder");
        return Collections.unmodifiableMap(ranks);
    }

    private static String[] permissionsFor(String rank) {
        if (rank.equals("helper")) {
            return new String[] {
                    "essentials.helpop.receive",
                    "essentials.socialspy",
                    "essentials.invsee",
                    "essentials.kick",
                    "libertybans.commands.warn",
                    "libertybans.commands.mute"
            };
        }
        if (rank.equals("builder")) {
            return new String[] {
                    "essentials.fly",
                    "essentials.gamemode",
                    "worldedit.*",
                    "worldguard.region.wand",
                    "worldguard.region.define",
                    "worldguard.region.redefine",
                    "worldguard.region.select.*"
            };
        }
        if (rank.equals("moderator")) {
            return new String[] {
                    "essentials.helpop.receive",
                    "essentials.socialspy",
                    "essentials.invsee",
                    "essentials.enderchest.others",
                    "essentials.kick",
                    "essentials.vanish",
                    "essentials.tp",
                    "essentials.tphere",
                    "libertybans.commands.warn",
                    "libertybans.commands.mute",
                    "libertybans.commands.tempmute",
                    "libertybans.commands.kick",
                    "libertybans.commands.ban",
                    "libertybans.commands.tempban",
                    "libertybans.commands.unban",
                    "libertybans.commands.history"
            };
        }
        if (rank.equals("developer")) {
            return new String[] {
                    "essentials.helpop.receive",
                    "essentials.socialspy",
                    "essentials.invsee",
                    "essentials.vanish",
                    "essentials.vanish.see",
                    "essentials.tp",
                    "essentials.tphere",
                    "essentials.tpo",
                    "essentials.tpohere",
                    "essentials.gamemode",
                    "essentials.fly",
                    "essentials.item",
                    "essentials.give",
                    "worldedit.*",
                    "worldguard.*",
                    "multiverse.*",
                    "citizens.*",
                    "decentholograms.*",
                    "crazycrates.*",
                    "skript.*",
                    "tab.admin",
                    "cannasmp.discordbridge.admin"
            };
        }
        if (rank.equals("manager")) {
            return new String[] {
                    "essentials.helpop.receive",
                    "essentials.socialspy",
                    "essentials.invsee",
                    "essentials.enderchest.others",
                    "essentials.kick",
                    "essentials.vanish",
                    "essentials.vanish.see",
                    "essentials.tp",
                    "essentials.tphere",
                    "essentials.tpo",
                    "essentials.tpohere",
                    "essentials.fly",
                    "essentials.heal",
                    "essentials.feed",
                    "essentials.broadcast",
                    "crazycrates.*",
                    "libertybans.commands.warn",
                    "libertybans.commands.mute",
                    "libertybans.commands.tempmute",
                    "libertybans.commands.kick",
                    "libertybans.commands.ban",
                    "libertybans.commands.tempban",
                    "libertybans.commands.unban",
                    "libertybans.commands.history",
                    "libertybans.commands.list",
                    "cannasmp.discordbridge.admin"
            };
        }
        if (rank.equals("founder")) {
            return new String[] {
                    "*"
            };
        }
        return new String[] {
                "essentials.helpop.receive",
                "essentials.socialspy",
                "essentials.invsee",
                "essentials.enderchest.others",
                "essentials.kick",
                "essentials.vanish",
                "essentials.vanish.see",
                "essentials.tp",
                "essentials.tphere",
                "essentials.tpo",
                "essentials.tpohere",
                "essentials.gamemode",
                "essentials.gamemode.others",
                "essentials.fly",
                "essentials.fly.others",
                "essentials.heal",
                "essentials.feed",
                "essentials.item",
                "essentials.give",
                "essentials.eco",
                "essentials.weather",
                "essentials.time",
                "essentials.broadcast",
                "worldedit.*",
                "worldguard.*",
                "multiverse.*",
                "citizens.*",
                "decentholograms.*",
                "crazycrates.*",
                "libertybans.commands.warn",
                "libertybans.commands.mute",
                "libertybans.commands.tempmute",
                "libertybans.commands.kick",
                "libertybans.commands.ban",
                "libertybans.commands.tempban",
                "libertybans.commands.unban",
                "libertybans.commands.history",
                "libertybans.commands.list",
                "cannasmp.discordbridge.admin"
        };
    }

    private static String cleanRank(String rank) {
        String value = rank.toLowerCase();
        if (!value.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Rank can only use letters, numbers, dot, dash, and underscore.");
        }
        return value;
    }

    private static String cleanRoleId(String roleId) {
        if (!roleId.matches("[0-9]{10,25}")) {
            throw new IllegalArgumentException("Discord role id must be numbers only.");
        }
        return roleId;
    }

    private static String setMinecraftRole(String text, String rank, String roleId) {
        Map<String, String> map = minecraftRoleMap(text);
        map.put(rank, roleId);
        return replaceMinecraftRoleMap(text, map);
    }

    private static String setDiscordRank(String text, String roleId, String rank) {
        Map<String, String> map = discordRankMap(text);
        map.put(roleId, rank);
        return replaceDiscordRankMap(text, map);
    }

    private static Map<String, String> minecraftRoleMap(String text) {
        Matcher matcher = minecraftBlock().matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not find minecraft-rank-to-discord-role.roles.");
        }
        return readMap(matcher.group(2));
    }

    private static String replaceMinecraftRoleMap(String text, Map<String, String> map) {
        Matcher matcher = minecraftBlock().matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not find minecraft-rank-to-discord-role.roles.");
        }
        return text.substring(0, matcher.start()) + matcher.group(1) + renderMap(map) + text.substring(matcher.end());
    }

    private static Map<String, String> discordRankMap(String text) {
        Matcher matcher = discordBlock().matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not find discord-role-to-minecraft-rank.roles.");
        }
        return readMap(matcher.group(2));
    }

    private static String replaceDiscordRankMap(String text, Map<String, String> map) {
        Matcher matcher = discordBlock().matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not find discord-role-to-minecraft-rank.roles.");
        }
        String rendered = map.isEmpty() ? " {}\n" : "\n" + renderMap(map);
        return text.substring(0, matcher.start()) + matcher.group(1) + rendered + text.substring(matcher.end());
    }

    private static Pattern minecraftBlock() {
        return Pattern.compile("(\\s+minecraft-rank-to-discord-role:\\s*\\R\\s+enabled: (?:true|false)\\s*\\R\\s+remove-other-mapped-roles: true\\s*\\R\\s+roles:\\s*\\R)([\\s\\S]*?)(?=\\s+discord-role-to-minecraft-rank:)");
    }

    private static Pattern discordBlock() {
        return Pattern.compile("(\\s+discord-role-to-minecraft-rank:\\s*\\R\\s+enabled: (?:true|false)\\s*\\R\\s+roles:)(?:\\s*\\{\\})?\\s*\\R?([\\s\\S]*?)(?=\\s+nickname-sync:)");
    }

    private static Map<String, String> readMap(String block) {
        Map<String, String> values = new TreeMap<>();
        Matcher matcher = Pattern.compile("\\s{6}([A-Za-z0-9_.-]+):\\s*\"([^\"]*)\"").matcher(block);
        while (matcher.find()) {
            values.put(matcher.group(1), matcher.group(2));
        }
        return values;
    }

    private static String renderMap(Map<String, String> map) {
        StringBuilder builder = new StringBuilder();
        map.forEach((key, value) -> builder.append("      ").append(key).append(": \"").append(value).append("\"\n"));
        return builder.toString();
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
