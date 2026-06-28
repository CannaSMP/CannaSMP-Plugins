/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.dv8tion.jda.api.EmbedBuilder
 *  net.dv8tion.jda.api.JDA
 *  net.dv8tion.jda.api.JDABuilder
 *  net.dv8tion.jda.api.OnlineStatus
 *  net.dv8tion.jda.api.entities.Activity
 *  net.dv8tion.jda.api.entities.Guild
 *  net.dv8tion.jda.api.entities.Member
 *  net.dv8tion.jda.api.entities.Message
 *  net.dv8tion.jda.api.entities.MessageEmbed
 *  net.dv8tion.jda.api.entities.Role
 *  net.dv8tion.jda.api.entities.UserSnowflake
 *  net.dv8tion.jda.api.entities.channel.concrete.TextChannel
 *  net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
 *  net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
 *  net.dv8tion.jda.api.events.message.MessageReceivedEvent
 *  net.dv8tion.jda.api.hooks.ListenerAdapter
 *  net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager
 *  net.dv8tion.jda.api.requests.GatewayIntent
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  net.luckperms.api.LuckPerms
 *  net.luckperms.api.LuckPermsProvider
 *  net.luckperms.api.event.user.UserDataRecalculateEvent
 *  net.luckperms.api.model.user.User
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.AsyncPlayerPreLoginEvent
 *  org.bukkit.event.player.PlayerAdvancementDoneEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package me.dupebound.discordbridge;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import me.dupebound.discordbridge.DiscordConsoleCapture;
import me.dupebound.discordbridge.StaleDiscordLinkCleaner;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class DupeboundDiscordBridgePlugin
extends JavaPlugin
implements Listener {
    private int semanticsId;
    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<String, PendingLink>();
    private final Map<UUID, Integer> queuedMinecraftRankSyncs = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Long> discordAuthoritativeUntil = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, String> lastMinecraftRanks = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, String> lastDiscordRanks = new ConcurrentHashMap<UUID, String>();
    private final Map<String, Long> ignoreDiscordRoleEventsUntil = new ConcurrentHashMap<String, Long>();
    private final Map<String, Long> lastWarningTimes = new ConcurrentHashMap<String, Long>();
    private final Map<String, Queue<String>> queuedDiscordMessages = new ConcurrentHashMap<String, Queue<String>>();
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<UUID, Long>();
    private final Map<String, PendingDiscordCommand> pendingDiscordConfirms = new ConcurrentHashMap<String, PendingDiscordCommand>();
    private final SecureRandom random = new SecureRandom();
    private final long startedAt = System.currentTimeMillis();
    private JDA jda;
    private BridgeSettings settings;
    private File linksFile;
    private FileConfiguration linksConfig;
    private int statusTaskId = -1;
    private int discordPollTaskId = -1;
    private int discordFlushTaskId = -1;

    public void onEnable() {
        this.saveDefaultConfig();
        this.settings = BridgeSettings.from(this);
        this.loadLinks();
        this.loadKnownMinecraftRanks();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.ensureRankGroupsExist();
        this.registerLuckPermsSync();
        this.connectDiscord();
        this.startStatusTask();
        this.startDiscordPollTask();
        this.startDiscordFlushTask();
    }

    public void onDisable() {
        this.sendLifecycleMessage(this.settings.stopMessage, true);
        this.saveLinks();
        if (this.statusTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.statusTaskId);
            this.statusTaskId = -1;
        }
        if (this.discordPollTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.discordPollTaskId);
            this.discordPollTaskId = -1;
        }
        if (this.discordFlushTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.discordFlushTaskId);
            this.discordFlushTaskId = -1;
        }
        this.flushQueuedDiscordMessages();
        for (int n : this.queuedMinecraftRankSyncs.values()) {
            Bukkit.getScheduler().cancelTask(n);
        }
        this.queuedMinecraftRankSyncs.clear();
        this.discordAuthoritativeUntil.clear();
        this.lastMinecraftRanks.clear();
        this.lastDiscordRanks.clear();
        this.ignoreDiscordRoleEventsUntil.clear();
        if (this.jda != null) {
            this.jda.shutdownNow();
            this.jda = null;
        }
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        String string2 = command.getName().toLowerCase(Locale.ROOT);
        if (string2.equals("staffchat") || string2.equals("sc")) {
            this.handleStaffChat(commandSender, stringArray);
            return true;
        }
        if (string2.equals("link")) {
            this.handleLink(commandSender, stringArray);
            return true;
        }
        if (string2.equals("unlink")) {
            this.handleUnlink(commandSender);
            return true;
        }
        if (!string2.equals("discordbridge")) {
            return false;
        }
        if (!commandSender.hasPermission("dupebound.discordbridge.admin")) {
            commandSender.sendMessage(this.color("&cYou do not have permission."));
            return true;
        }
        if (stringArray.length == 0) {
            commandSender.sendMessage(this.color("&6/discordbridge reload &7- reload config and reconnect"));
            commandSender.sendMessage(this.color("&6/discordbridge status &7- show bridge status"));
            commandSender.sendMessage(this.color("&6/discordbridge test &7- send a test message"));
            commandSender.sendMessage(this.color("&6/discordbridge sync &7- sync online linked players"));
            commandSender.sendMessage(this.color("&6/discordbridge debug <player> &7- check link and Discord role permissions"));
            return true;
        }
        switch (stringArray[0].toLowerCase(Locale.ROOT)) {
            case "reload": {
                this.reloadConfig();
                this.settings = BridgeSettings.from(this);
                this.ensureRankGroupsExist();
                this.reconnectDiscord();
                this.startStatusTask();
                this.startDiscordPollTask();
                this.startDiscordFlushTask();
                commandSender.sendMessage(this.color("&aDiscord bridge reloaded."));
                break;
            }
            case "status": {
                commandSender.sendMessage(this.color("&7Enabled: &f" + this.settings.enabled));
                commandSender.sendMessage(this.color("&7Discord: &f" + (this.jda == null ? "offline" : this.jda.getStatus().name())));
                commandSender.sendMessage(this.color("&7Chat channel: &f" + this.settings.minecraftChatChannelId));
                commandSender.sendMessage(this.color("&7Staff channel: &f" + this.settings.staffChatChannelId));
                commandSender.sendMessage(this.color("&7Log channel: &f" + this.settings.logChannelId));
                commandSender.sendMessage(this.color("&7Links: &f" + this.getLinksSection().getKeys(false).size()));
                break;
            }
            case "test": {
                this.sendChat("Bridge test from Minecraft by " + commandSender.getName() + ".");
                this.sendStaff("Staff bridge test from Minecraft by " + commandSender.getName() + ".");
                this.sendLog("Log bridge test from Minecraft by " + commandSender.getName() + ".");
                this.updateStatusTopic();
                commandSender.sendMessage(this.color("&aSent bridge test messages and refreshed channel topic."));
                break;
            }
            case "sync": {
                this.syncAllOnlineLinkedPlayers();
                commandSender.sendMessage(this.color("&aQueued linked player sync."));
                break;
            }
            case "debug": {
                this.handleDebug(commandSender, stringArray);
                break;
            }
            default: {
                commandSender.sendMessage(this.color("&cUsage: /discordbridge <reload/status/test/sync/debug>"));
            }
        }
        return true;
    }

    private void handleDebug(CommandSender commandSender, String[] stringArray) {
        if (stringArray.length < 2) {
            commandSender.sendMessage(this.color("&cUsage: /discordbridge debug <player>"));
            return;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((String)stringArray[1]);
        UUID uUID = offlinePlayer.getUniqueId();
        String string = this.linksConfig.getString("links." + String.valueOf(uUID) + ".player-name", offlinePlayer.getName() == null ? stringArray[1] : offlinePlayer.getName());
        String string2 = this.linksConfig.getString("links." + String.valueOf(uUID) + ".discord-id", "");
        String string3 = this.getMinecraftRank(uUID);
        String string4 = this.settings.minecraftRankToDiscordRole.get(string3.toLowerCase(Locale.ROOT));
        commandSender.sendMessage(this.color("&6DupeBound bridge debug for &f" + string));
        commandSender.sendMessage(this.color("&7Linked: &f" + !string2.isBlank()));
        commandSender.sendMessage(this.color("&7Minecraft rank: &f" + string3));
        commandSender.sendMessage(this.color("&7Discord id: &f" + (string2.isBlank() ? "none" : string2)));
        commandSender.sendMessage(this.color("&7Target role id: &f" + (string4 == null || string4.isBlank() ? "none" : string4)));
        Guild guild = this.getMainGuild();
        if (guild == null || this.jda == null) {
            commandSender.sendMessage(this.color("&cDiscord guild/JDA is not ready."));
            return;
        }
        if (string4 != null && !string4.isBlank() && !string4.equals("0")) {
            Role role = guild.getRoleById(string4);
            commandSender.sendMessage(this.color("&7Can manage target role: &f" + (role != null && guild.getSelfMember().canInteract(role))));
        }
        if (string2.isBlank()) {
            return;
        }
        guild.retrieveMemberById(string2).queue(member -> Bukkit.getScheduler().runTask((Plugin)this, () -> {
            commandSender.sendMessage(this.color("&7Discord member found: &ftrue"));
            commandSender.sendMessage(this.color("&7Can manage member nickname: &f" + guild.getSelfMember().canInteract(member)));
            commandSender.sendMessage(this.color("&7Discord rank from roles: &f" + (this.getRankFromDiscordRoles((Member)member).isBlank() ? "none" : this.getRankFromDiscordRoles((Member)member))));
        }), throwable -> Bukkit.getScheduler().runTask((Plugin)this, () -> commandSender.sendMessage(this.color("&cCould not load Discord member: " + throwable.getMessage()))));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent asyncChatEvent) {
        if (!this.settings.minecraftChatEnabled) {
            return;
        }
        String string = PlainTextComponentSerializer.plainText().serialize(asyncChatEvent.message());
        String string2 = this.displayRank(this.getMinecraftRank(asyncChatEvent.getPlayer().getUniqueId()));
        this.sendChat("**[" + this.escapeDiscord(string2) + "] " + this.escapeDiscord(asyncChatEvent.getPlayer().getName()) + "**: " + this.escapeDiscord(string));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        long l = System.currentTimeMillis();
        this.joinTimes.put(playerJoinEvent.getPlayer().getUniqueId(), l);
        if (this.settings.joinLeaveEnabled) {
            this.sendEmbed(this.settings.minecraftChatChannelId, "Player Joined", "**" + this.escapeDiscord(playerJoinEvent.getPlayer().getName()) + "** joined the server.\nJoined: " + this.discordTimestamp(l), 2278750);
        }
        this.syncLinkedPlayer(playerJoinEvent.getPlayer().getUniqueId(), playerJoinEvent.getPlayer().getName());
        this.updateStatusTopic();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        if (this.settings.joinLeaveEnabled) {
            long l = this.joinTimes.getOrDefault(playerQuitEvent.getPlayer().getUniqueId(), System.currentTimeMillis());
            this.joinTimes.remove(playerQuitEvent.getPlayer().getUniqueId());
            StringBuilder stringBuilder = new StringBuilder().append("**").append(this.escapeDiscord(playerQuitEvent.getPlayer().getName())).append("** left the server!\n").append(this.settings.leaveTimeFormat.replace("{time}", this.formatDuration(System.currentTimeMillis() - l)));
            if (this.settings.leaveShowIp && !this.settings.leaveIp.isBlank()) {
                stringBuilder.append("\n").append(this.settings.leaveIpFormat.replace("{ip}", this.settings.leaveIp));
            }
            this.sendEmbed(this.settings.minecraftChatChannelId, "Player Left", stringBuilder.toString(), 0xEF4444);
        }
        this.updateStatusTopic();
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent asyncPlayerPreLoginEvent) {
        this.linksConfig.set("links." + String.valueOf(asyncPlayerPreLoginEvent.getUniqueId()) + ".player-name", (Object)asyncPlayerPreLoginEvent.getName());
        this.saveLinks();
    }

    private void registerLuckPermsSync() {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            luckPerms.getEventBus().subscribe((Object)this, UserDataRecalculateEvent.class, userDataRecalculateEvent -> {
                UUID uUID = userDataRecalculateEvent.getUser().getUniqueId();
                if (!this.isLinked(uUID)) {
                    return;
                }
                String string = userDataRecalculateEvent.getUser().getPrimaryGroup().toLowerCase(Locale.ROOT);
                if (this.isDiscordAuthoritative(uUID)) {
                    this.updateKnownMinecraftRank(uUID, string);
                    return;
                }
                String string2 = this.getKnownMinecraftRank(uUID);
                if (string2 == null) {
                    this.updateKnownMinecraftRank(uUID, string);
                    return;
                }
                if (string2.equalsIgnoreCase(string)) {
                    return;
                }
                String string3 = this.linksConfig.getString("links." + String.valueOf(uUID) + ".player-name", uUID.toString());
                Integer n = this.queuedMinecraftRankSyncs.remove(uUID);
                if (n != null) {
                    Bukkit.getScheduler().cancelTask(n.intValue());
                }
                int n2 = Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
                    this.queuedMinecraftRankSyncs.remove(uUID);
                    this.syncMinecraftRankToDiscord(uUID, string3, string);
                }, (long)this.settings.liveSyncDelayTicks).getTaskId();
                this.queuedMinecraftRankSyncs.put(uUID, n2);
            });
        }
        catch (Exception exception) {
            this.getLogger().warning("LuckPerms live sync could not be registered: " + exception.getMessage());
        }
    }

    private void ensureRankGroupsExist() {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            for (String string : this.settings.minecraftRankToDiscordRole.keySet()) {
                this.ensureRankGroupExists(luckPerms, string);
            }
            this.ensureRankGroupExists(luckPerms, this.settings.defaultRank);
        }
        catch (Exception exception) {
            this.getLogger().warning("Could not verify LuckPerms rank groups: " + exception.getMessage());
        }
    }

    private void ensureRankGroupExists(LuckPerms luckPerms, String string) {
        if (string == null || string.isBlank()) {
            return;
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        if (luckPerms.getGroupManager().getGroup(string2) != null) {
            return;
        }
        luckPerms.getGroupManager().createAndLoadGroup(string2).thenAccept(group -> this.getLogger().info("Created missing LuckPerms group: " + string2));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent playerDeathEvent) {
        if (this.settings.deathEnabled && playerDeathEvent.deathMessage() != null) {
            String string = PlainTextComponentSerializer.plainText().serialize(playerDeathEvent.deathMessage());
            this.sendEmbed(this.settings.minecraftChatChannelId, "Death", this.escapeDiscord(string), 0x991B1B);
        }
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent playerAdvancementDoneEvent) {
        if (!this.settings.advancementEnabled) {
            return;
        }
        String string = playerAdvancementDoneEvent.getAdvancement().getKey().getKey();
        if (!string.startsWith("recipes/")) {
            this.sendEmbed(this.settings.minecraftChatChannelId, "Advancement", "**" + this.escapeDiscord(playerAdvancementDoneEvent.getPlayer().getName()) + "** completed `" + this.escapeDiscord(string) + "`.", 16096779);
        }
    }

    private void handleLink(CommandSender commandSender, String[] stringArray) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Only players can use /link.");
            return;
        }
        Player player = (Player)commandSender;
        if (!this.settings.linkingEnabled) {
            player.sendMessage(this.color("&cDiscord linking is disabled."));
            return;
        }
        if (stringArray.length > 0 && stringArray[0].equalsIgnoreCase("unlink")) {
            this.unlinkPlayer(player);
            return;
        }
        if (this.isLinked(player.getUniqueId())) {
            player.sendMessage(this.color("&aYour account is already linked. Use &f/link unlink &ato unlink."));
            this.syncLinkedPlayer(player.getUniqueId(), player.getName());
            return;
        }
        String string = this.generateCode();
        this.pendingLinks.put(string, new PendingLink(player.getUniqueId(), player.getName(), System.currentTimeMillis() + (long)this.settings.linkCodeMinutes * 60000L));
        this.sendClickableLinkCode(player, string);
        player.sendMessage(this.color("&7Run &f/link code: " + string + " &7in the Discord link/general channel."));
    }

    private void sendClickableLinkCode(Player player, String string) {
        String string2 = "/link code: " + string;
        String string3 = "minecraft:tellraw " + player.getName() + " [{\"text\":\"Your Discord link code is \",\"color\":\"green\"},{\"text\":\"" + string + "\",\"color\":\"white\",\"underlined\":true,\"click_event\":{\"action\":\"copy_to_clipboard\",\"value\":\"" + string2 + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"Click to copy " + string2 + "\"}},{\"text\":\".  \",\"color\":\"green\"},{\"text\":\"[Put Discord Command In Chat]\",\"color\":\"yellow\",\"underlined\":true,\"click_event\":{\"action\":\"suggest_command\",\"command\":\"" + string2 + "\"},\"hover_event\":{\"action\":\"show_text\",\"value\":\"Click to put the Discord command in chat so you can copy it\"}}]";
        Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)string3);
    }

    private void handleUnlink(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Only players can use /unlink.");
            return;
        }
        Player player = (Player)commandSender;
        this.unlinkPlayer(player);
    }

    private void unlinkPlayer(Player player) {
        if (!this.isLinked(player.getUniqueId())) {
            player.sendMessage(this.color("&cYour Minecraft account is not linked to Discord."));
            return;
        }
        this.removeLinkedRole(player.getUniqueId());
        this.removeLink(player.getUniqueId());
        this.lastMinecraftRanks.remove(player.getUniqueId());
        this.lastDiscordRanks.remove(player.getUniqueId());
        this.discordAuthoritativeUntil.remove(player.getUniqueId());
        Integer n = this.queuedMinecraftRankSyncs.remove(player.getUniqueId());
        if (n != null) {
            Bukkit.getScheduler().cancelTask(n.intValue());
        }
        player.sendMessage(this.color("&aYour Minecraft account has been unlinked from Discord."));
    }

    private void handleStaffChat(CommandSender commandSender, String[] stringArray) {
        if (!commandSender.hasPermission(this.settings.staffPermission)) {
            commandSender.sendMessage(this.color("&cYou do not have permission."));
            return;
        }
        if (stringArray.length == 0) {
            commandSender.sendMessage(this.color("&cUsage: /staffchat <message>"));
            return;
        }
        String string = String.join((CharSequence)" ", stringArray);
        String string2 = this.color(this.settings.staffMinecraftFormat.replace("{player}", commandSender.getName()).replace("{message}", string));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission(this.settings.staffPermission)) continue;
            player.sendMessage(string2);
        }
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(string2);
        }
        this.sendStaff("**" + this.escapeDiscord(commandSender.getName()) + "**: " + this.escapeDiscord(string));
    }

    private void connectDiscord() {
        if (!this.settings.enabled) {
            this.getLogger().info("Discord bridge is disabled in config.yml.");
            return;
        }
        if (this.settings.token.isBlank() || this.settings.token.equalsIgnoreCase("PUT_BOT_TOKEN_HERE")) {
            this.getLogger().warning("Discord bridge token is not set. Edit plugins/DupeBoundDiscordBridge/config.yml.");
            return;
        }
        try {
            this.jda = JDABuilder.createDefault((String)this.settings.token).enableIntents(GatewayIntent.GUILD_MESSAGES, new GatewayIntent[]{GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS}).setStatus(OnlineStatus.ONLINE).setActivity(Activity.playing((String)this.settings.activity)).addEventListeners(new Object[]{new DiscordListener()}).build();
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this, () -> {
                try {
                    this.jda.awaitReady();
                    this.registerDiscordSlashCommands();
                    this.sendLifecycleMessage(this.settings.startMessage, false);
                    this.updateStatusTopic();
                    this.syncAllOnlineLinkedPlayers();
                }
                catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
                catch (Exception exception) {
                    this.getLogger().warning("Discord ready hook failed: " + exception.getMessage());
                }
            });
        }
        catch (Exception exception) {
            this.getLogger().severe("Failed to start Discord bridge: " + exception.getMessage());
        }
    }

    private void reconnectDiscord() {
        if (this.jda != null) {
            this.jda.shutdownNow();
            this.jda = null;
        }
        this.connectDiscord();
    }

    private void registerDiscordSlashCommands() {
        Guild guild = this.getMainGuild();
        if (guild == null) {
            this.getLogger().warning("Could not register Discord slash commands: guild is not ready.");
            return;
        }
        guild.upsertCommand(Commands.slash("link", "Link your Discord account to your CannaSMP account.")
                .addOption(OptionType.STRING, "code", "The code from /link in Minecraft.", true))
                .queue(command -> this.getLogger().info("Registered Discord slash command: /link"),
                        throwable -> this.getLogger().warning("Could not register Discord /link slash command: " + throwable.getMessage()));
    }

    private void startStatusTask() {
        if (this.statusTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.statusTaskId);
            this.statusTaskId = -1;
        }
        if (!this.settings.statusTopicEnabled) {
            return;
        }
        long l = (long)Math.max(30, this.settings.statusTopicIntervalSeconds) * 20L;
        this.statusTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this, this::updateStatusTopic, l, l);
    }

    private void startDiscordPollTask() {
        if (this.discordPollTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.discordPollTaskId);
            this.discordPollTaskId = -1;
        }
        if (!this.settings.rankSyncEnabled || !this.settings.discordToMinecraftRankEnabled || this.settings.discordPollSeconds <= 0) {
            return;
        }
        long l = Math.max(20L, (long)this.settings.discordPollSeconds * 20L);
        this.discordPollTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this, this::pollDiscordRanks, l, l);
    }

    private void startDiscordFlushTask() {
        if (this.discordFlushTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.discordFlushTaskId);
            this.discordFlushTaskId = -1;
        }
        if (!this.settings.outboundBatchingEnabled) {
            return;
        }
        long l = Math.max(1L, (long)this.settings.outboundBatchFlushSeconds) * 20L;
        this.discordFlushTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this, this::flushQueuedDiscordMessages, l, l);
    }

    private void flushQueuedDiscordMessages() {
        for (Map.Entry<String, Queue<String>> entry : this.queuedDiscordMessages.entrySet()) {
            String string = entry.getKey();
            Queue<String> queue = entry.getValue();
            while (!queue.isEmpty()) {
                ArrayList<String> arrayList = new ArrayList<String>();
                int n = Math.max(1, this.settings.outboundBatchMaxLines);
                while (!queue.isEmpty() && arrayList.size() < n) {
                    String string2 = queue.poll();
                    if (string2 == null || string2.isBlank()) continue;
                    arrayList.add(string2);
                }
                if (arrayList.isEmpty()) continue;
                this.sendDiscordNow(string, String.join((CharSequence)"\n", arrayList));
            }
        }
    }

    private void pollDiscordRanks() {
        if (this.jda == null || !this.settings.rankSyncEnabled || !this.settings.discordToMinecraftRankEnabled) {
            return;
        }
        Guild guild = this.getMainGuild();
        if (guild == null) {
            return;
        }
        ConfigurationSection configurationSection = this.getLinksSection();
        for (String string : configurationSection.getKeys(false)) {
            UUID uUID;
            String string2 = configurationSection.getString(string + ".discord-id", "");
            if (string2.isBlank() || this.shouldIgnoreDiscordRoleEvent(string2)) continue;
            try {
                uUID = UUID.fromString(string);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                continue;
            }
            String string3 = configurationSection.getString(string + ".player-name", uUID.toString());
            guild.retrieveMemberById(string2).queue(member -> this.handlePolledDiscordRank(uUID, string3, (Member)member), throwable -> StaleDiscordLinkCleaner.handle(this, string2, string3, throwable));
        }
    }

    private void handlePolledDiscordRank(UUID uUID, String string, Member member) {
        String string2 = this.getKnownDiscordRank(uUID);
        if (string2 == null) {
            String string3 = this.getRankFromDiscordRoles(member);
            if (string3.isBlank()) {
                string3 = this.settings.defaultRank;
            }
            this.updateKnownDiscordRank(uUID, string3);
            return;
        }
        if (this.memberHasRank(member, string2)) {
            return;
        }
        String string4 = this.getRankFromDiscordRoles(member);
        if (string4.isBlank()) {
            string4 = this.settings.defaultRank;
        }
        if (string2.equalsIgnoreCase(string4)) {
            return;
        }
        this.getLogger().info("Discord poll sync -> Minecraft: " + string + " = " + string4 + " (was " + string2 + ")");
        this.syncDiscordRankToMinecraft(member, string4, true);
        this.updateKnownDiscordRank(uUID, string4);
    }

    private void sendLifecycleMessage(String string, boolean bl) {
        if (!this.settings.lifecycleMessagesEnabled || string.isBlank()) {
            return;
        }
        String string2 = string.replace("{server}", this.settings.serverName).replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size())).replace("{max}", String.valueOf(Bukkit.getMaxPlayers()));
        if (bl) {
            this.sendEmbedBlocking(this.settings.minecraftChatChannelId, this.settings.serverName, string2, 3718648);
        } else {
            this.sendEmbed(this.settings.minecraftChatChannelId, this.settings.serverName, string2, 3718648);
        }
    }

    private void updateStatusTopic() {
        if (!this.settings.statusTopicEnabled || this.jda == null || this.settings.statusTopicChannelId.isBlank() || this.settings.statusTopicChannelId.equals("0")) {
            return;
        }
        TextChannel textChannel = this.jda.getTextChannelById(this.settings.statusTopicChannelId);
        if (textChannel == null) {
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        long l = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
        long l2 = runtime.maxMemory() / 1024L / 1024L;
        String string = this.settings.statusTopicFormat.replace("{server}", this.settings.serverName).replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size())).replace("{max}", String.valueOf(Bukkit.getMaxPlayers())).replace("{ram_used}", String.valueOf(l)).replace("{ram_max}", String.valueOf(l2));
        ((TextChannelManager)textChannel.getManager().setTopic(string)).queue(null, throwable -> this.getLogger().warning("Discord topic update failed: " + throwable.getMessage()));
    }

    private void sendChat(String string) {
        this.sendDiscord(this.settings.minecraftChatChannelId, string);
    }

    private void sendStaff(String string) {
        this.sendDiscord(this.settings.staffChatChannelId, string);
    }

    private void sendLog(String string) {
        this.sendEmbed(this.settings.logChannelId, "Server Log", string, 9741240);
    }

    private void sendEmbed(String string, String string2, String string3, int n) {
        if (this.jda == null || string.isBlank() || string.equals("0") || string3.isBlank()) {
            return;
        }
        TextChannel textChannel = this.jda.getTextChannelById(string);
        if (textChannel == null) {
            return;
        }
        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle(string2).setDescription((CharSequence)string3).setColor(n);
        textChannel.sendMessageEmbeds(embedBuilder.build(), new MessageEmbed[0]).queue(null, throwable -> this.getLogger().warning("Discord embed send failed: " + throwable.getMessage()));
    }

    private void sendEmbedBlocking(String string, String string2, String string3, int n) {
        if (this.jda == null || string.isBlank() || string.equals("0") || string3.isBlank()) {
            return;
        }
        TextChannel textChannel = this.jda.getTextChannelById(string);
        if (textChannel == null) {
            return;
        }
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder().setTitle(string2).setDescription((CharSequence)string3).setColor(n);
            textChannel.sendMessageEmbeds(embedBuilder.build(), new MessageEmbed[0]).complete();
        }
        catch (Exception exception) {
            this.getLogger().warning("Discord embed send failed: " + exception.getMessage());
        }
    }

    private void sendDiscord(String string, String string2) {
        if (this.settings.outboundBatchingEnabled) {
            this.queueDiscordMessage(string, string2);
            return;
        }
        this.sendDiscordNow(string, string2);
    }

    private void queueDiscordMessage(String string2, String string3) {
        if (this.jda == null || string2.isBlank() || string2.equals("0") || string3.isBlank()) {
            return;
        }
        this.queuedDiscordMessages.computeIfAbsent(string2, string -> new ConcurrentLinkedQueue()).add(string3);
    }

    private void sendDiscordNow(String string, String string2) {
        if (this.jda == null || string.isBlank() || string.equals("0")) {
            return;
        }
        TextChannel textChannel = this.jda.getTextChannelById(string);
        if (textChannel == null) {
            return;
        }
        textChannel.sendMessage((CharSequence)string2).queue(null, throwable -> this.getLogger().warning("Discord send failed: " + throwable.getMessage()));
    }

    private void sendDiscordBlocking(String string, String string2) {
        if (this.jda == null || string.isBlank() || string.equals("0")) {
            return;
        }
        TextChannel textChannel = this.jda.getTextChannelById(string);
        if (textChannel == null) {
            return;
        }
        try {
            textChannel.sendMessage((CharSequence)string2).complete();
        }
        catch (Exception exception) {
            this.getLogger().warning("Discord send failed: " + exception.getMessage());
        }
    }

    private void broadcastMinecraft(String string, String string2, String string3) {
        String string4 = this.color(string.replace("{user}", string2).replace("{message}", string3));
        Bukkit.getScheduler().runTask((Plugin)this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(string4);
            }
            Bukkit.getConsoleSender().sendMessage(string4);
        });
    }

    private void sendMinecraftStaff(String string, String string2) {
        String string3 = this.color(this.settings.staffDiscordToMinecraftFormat.replace("{user}", string).replace("{message}", string2));
        Bukkit.getScheduler().runTask((Plugin)this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission(this.settings.staffPermission)) continue;
                player.sendMessage(string3);
            }
            Bukkit.getConsoleSender().sendMessage(string3);
        });
    }

    private void runConsoleCommandFromDiscord(Message message, String string) {
        if (!this.settings.discordConsoleCommandsEnabled || !string.startsWith(this.settings.consoleCommandPrefix)) {
            return;
        }
        String string2 = message.getAuthor().getId();
        if (!this.settings.allowedDiscordCommandUserIds.contains(string2)) {
            message.reply((CharSequence)"You are not allowed to run console commands.").queue();
            return;
        }
        String string3 = string.substring(this.settings.consoleCommandPrefix.length()).trim();
        if (string3.isBlank()) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this, () -> DiscordConsoleCapture.run(string3, message));
    }

    private boolean handleMinecraftDiscordCommand(MessageReceivedEvent messageReceivedEvent, String string) {
        String string2 = string.trim();
        if (!string2.toLowerCase(Locale.ROOT).startsWith("!mc")) {
            return false;
        }
        Message message = messageReceivedEvent.getMessage();
        String[] stringArray = string2.split("\\s+", 2);
        String string3 = stringArray[0].toLowerCase(Locale.ROOT);
        String string4 = stringArray.length > 1 ? stringArray[1].trim() : "";
        if (string3.equals("!mcconfirm")) {
            this.confirmDiscordCommand(messageReceivedEvent);
            return true;
        }
        switch (string3) {
            case "!mchelp": {
                this.replyDiscordMcHelp(message);
                return true;
            }
            case "!mcip": {
                message.reply((CharSequence)("**CannaSMP IP:** `" + this.escapeDiscord(this.settings.leaveIp.isBlank() ? "cannasmp.smpserver.net" : this.settings.leaveIp) + "`")).queue();
                return true;
            }
            case "!mconline": {
                List<String> list = Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
                message.reply((CharSequence)("**Online:** " + list.size() + "/" + Bukkit.getMaxPlayers() + "\n`" + (list.isEmpty() ? "none" : this.escapeDiscord(String.join((CharSequence)", ", list))) + "`")).queue();
                return true;
            }
            case "!mctps": {
                message.reply((CharSequence)("```text\n" + this.tpsOutput() + "\n```")).queue();
                return true;
            }
            case "!mcuptime": {
                message.reply((CharSequence)("**Uptime:** `" + this.formatDuration(System.currentTimeMillis() - this.startedAt) + "`")).queue();
                return true;
            }
            case "!mcserver": {
                message.reply((CharSequence)("**" + this.escapeDiscord(this.settings.serverName) + "**\nOnline: `" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers() + "`\nVersion: `" + this.escapeDiscord(Bukkit.getVersion()) + "`\n" + this.tpsOutput())).queue();
                return true;
            }
            case "!mcseen": {
                return this.runInfoCommand(message, "seen", string4, "<player>");
            }
            case "!mcstats": {
                return this.runInfoCommand(message, "lp user", string4, "<player>");
            }
            case "!mcplaytime": {
                return this.runInfoCommand(message, "playtime", string4, "<player>");
            }
            case "!mcbal": {
                return this.runInfoCommand(message, "bal", string4, "<player>");
            }
            case "!mcrank": {
                return this.runInfoCommand(message, "lp user", string4, "<player>");
            }
        }
        if (!this.canRunStaffDiscordCommand(messageReceivedEvent)) {
            message.reply((CharSequence)"You are not allowed to run staff Minecraft commands.").queue();
            return true;
        }
        switch (string3) {
            case "!mckick": {
                return this.runStaffCommand(message, "kick", string4, "<player> [reason]");
            }
            case "!mcwarn": {
                return this.runStaffCommand(message, "warn", string4, "<player> <reason>");
            }
            case "!mcmute": {
                return this.runStaffCommand(message, "mute", string4, "<player> [reason]");
            }
            case "!mcunmute": {
                return this.runStaffCommand(message, "unmute", string4, "<player>");
            }
            case "!mctempban": {
                return this.runStaffCommand(message, "tempban", string4, "<player> <time> <reason>");
            }
            case "!mcban": {
                return this.runStaffCommand(message, "ban", string4, "<player> [reason]");
            }
            case "!mcunban": {
                return this.runStaffCommand(message, "pardon", string4, "<player>");
            }
            case "!mcipban": {
                return this.runStaffCommand(message, "ban-ip", string4, "<player/ip> [reason]");
            }
            case "!mcpardonip": {
                return this.runStaffCommand(message, "pardon-ip", string4, "<ip>");
            }
            case "!mcwhitelist": {
                return this.runStaffCommand(message, "whitelist", string4, "add/remove/list [player]");
            }
            case "!mcsaveall": {
                return this.runStaffCommand(message, "save-all", string4, "");
            }
            case "!mcbroadcast": {
                return this.runStaffCommand(message, "broadcast", string4, "<message>");
            }
            case "!mcop": {
                return this.runStaffCommand(message, "op", string4, "<player>");
            }
            case "!mcdeop": {
                return this.runStaffCommand(message, "deop", string4, "<player>");
            }
            case "!mcgamemode": {
                return this.runStaffCommand(message, "gamemode", string4, "<mode> [player]");
            }
            case "!mctime": {
                return this.runStaffCommand(message, "time", string4, "set/add/query <args>");
            }
            case "!mcweather": {
                return this.runStaffCommand(message, "weather", string4, "<clear/rain/thunder> [duration]");
            }
            case "!mcdifficulty": {
                return this.runStaffCommand(message, "difficulty", string4, "<peaceful/easy/normal/hard>");
            }
            case "!mctp": {
                return this.runStaffCommand(message, "tp", string4, "<target> [destination]");
            }
            case "!mcgive": {
                return this.runStaffCommand(message, "give", string4, "<player> <item> [amount]");
            }
            case "!mceffect": {
                return this.runStaffCommand(message, "effect", string4, "<give/clear> <args>");
            }
            case "!mcclear": {
                return this.runStaffCommand(message, "clear", string4, "[player] [item] [maxCount]");
            }
            case "!mcrestart": {
                return this.queueDangerousDiscordCommand(messageReceivedEvent, "restart " + string4, "!mcrestart");
            }
            case "!mcstop": {
                return this.queueDangerousDiscordCommand(messageReceivedEvent, "stop " + string4, "!mcstop");
            }
            case "!mcsudo": {
                return this.queueDangerousDiscordCommand(messageReceivedEvent, "sudo " + string4, "!mcsudo <player> <command>");
            }
            case "!mcconsole": {
                return this.queueDangerousDiscordCommand(messageReceivedEvent, string4, "!mcconsole <command>");
            }
        }
        message.reply((CharSequence)"Unknown `!mc` command. Use `!mchelp`.").queue();
        return true;
    }

    private boolean runInfoCommand(Message message, String string, String string2, String string3) {
        if (string2.isBlank()) {
            message.reply((CharSequence)("Usage: `" + string + " " + string3 + "`")).queue();
            return true;
        }
        Bukkit.getScheduler().runTask((Plugin)this, () -> DiscordConsoleCapture.run(string + " " + string2, message));
        return true;
    }

    private boolean runStaffCommand(Message message, String string, String string2, String string3) {
        if (!string3.isBlank() && string2.isBlank()) {
            message.reply((CharSequence)("Usage: `!" + string.replace(" ", "") + " " + string3 + "`")).queue();
            return true;
        }
        Bukkit.getScheduler().runTask((Plugin)this, () -> DiscordConsoleCapture.run((string + " " + string2).trim(), message));
        return true;
    }

    private boolean queueDangerousDiscordCommand(MessageReceivedEvent messageReceivedEvent, String string, String string2) {
        String string3 = string.trim();
        if (string3.isBlank()) {
            messageReceivedEvent.getMessage().reply((CharSequence)("Usage: `" + string2 + "`")).queue();
            return true;
        }
        String string4 = messageReceivedEvent.getAuthor().getId();
        this.pendingDiscordConfirms.put(string4, new PendingDiscordCommand(string3, System.currentTimeMillis() + 30000L, messageReceivedEvent.getChannel().getId()));
        messageReceivedEvent.getMessage().reply((CharSequence)("Queued dangerous console command:\n```text\n" + this.sanitizeCodeBlock(string3) + "\n```\nRun `!mcconfirm` in this channel within 30 seconds to execute it.")).queue();
        return true;
    }

    private void confirmDiscordCommand(MessageReceivedEvent messageReceivedEvent) {
        String string = messageReceivedEvent.getAuthor().getId();
        if (!this.canRunStaffDiscordCommand(messageReceivedEvent)) {
            messageReceivedEvent.getMessage().reply((CharSequence)"You are not allowed to confirm console commands.").queue();
            return;
        }
        PendingDiscordCommand pendingDiscordCommand = this.pendingDiscordConfirms.remove(string);
        if (pendingDiscordCommand == null || pendingDiscordCommand.expiresAt < System.currentTimeMillis()) {
            messageReceivedEvent.getMessage().reply((CharSequence)"No pending command to confirm, or it expired.").queue();
            return;
        }
        if (!pendingDiscordCommand.channelId.equals(messageReceivedEvent.getChannel().getId())) {
            messageReceivedEvent.getMessage().reply((CharSequence)"Confirm this in the same channel where the command was queued.").queue();
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this, () -> DiscordConsoleCapture.run(pendingDiscordCommand.command, messageReceivedEvent.getMessage()));
    }

    private boolean canRunStaffDiscordCommand(MessageReceivedEvent messageReceivedEvent) {
        return this.settings.allowedDiscordCommandUserIds.contains(messageReceivedEvent.getAuthor().getId());
    }

    private void replyDiscordMcHelp(Message message) {
        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("CannaSMP Discord -> Minecraft Commands").setColor(0x22C55E).setDescription("Run these from Discord. Staff/admin commands are restricted.");
        embedBuilder.addField("Discord Info", "`!mchelp`, `!mcip`, `!mconline`, `!mctps`, `!mcuptime`, `!mcserver`\n`!mcseen`, `!mcstats`, `!mcplaytime`, `!mcbal`, `!mcrank`", false);
        embedBuilder.addField("Staff Moderation", "`!mckick`, `!mcwarn`, `!mcmute`, `!mcunmute`, `!mctempban`\n`!mcban`, `!mcunban`, `!mcipban`, `!mcpardonip`\n`!mcwhitelist add/remove/list`", false);
        embedBuilder.addField("Staff Admin", "`!mcsaveall`, `!mcbroadcast`, `!mcop`, `!mcdeop`\n`!mcgamemode`, `!mctime`, `!mcweather`, `!mcdifficulty`\n`!mctp`, `!mcgive`, `!mceffect`, `!mcclear`\n`!mcrestart`, `!mcstop`, `!mcsudo`, `!mcconsole` require `!mcconfirm`", false);
        message.replyEmbeds(embedBuilder.build(), new MessageEmbed[0]).queue();
    }

    private String tpsOutput() {
        double[] dArray = Bukkit.getTPS();
        return String.format(Locale.US, "TPS: %s, %s, %s | MSPT: %.2f", this.formatTps(dArray, 0), this.formatTps(dArray, 1), this.formatTps(dArray, 2), Bukkit.getAverageTickTime());
    }

    private String formatTps(double[] dArray, int n) {
        if (dArray == null || dArray.length <= n) {
            return "n/a";
        }
        return String.format(Locale.US, "%.2f", Math.min(20.0, dArray[n]));
    }

    private String sanitizeCodeBlock(String string) {
        return string.replace("```", "'''").replace("\r", "");
    }

    private void handleDiscordLink(MessageReceivedEvent messageReceivedEvent, String string) {
        if (!this.settings.linkingEnabled || !string.toLowerCase(Locale.ROOT).startsWith(this.settings.linkCommandPrefix.toLowerCase(Locale.ROOT))) {
            return;
        }
        if (!this.settings.linkChannelId.equals("0") && !messageReceivedEvent.getChannel().getId().equals(this.settings.linkChannelId)) {
            return;
        }
        String string3 = string.substring(this.settings.linkCommandPrefix.length()).trim().toUpperCase(Locale.ROOT);
        this.completeDiscordLink(string3, messageReceivedEvent.getAuthor().getId(), messageReceivedEvent.getAuthor().getName(), messageReceivedEvent.getMember(), message -> messageReceivedEvent.getMessage().reply((CharSequence)message).queue());
    }

    private void handleDiscordSlashLink(SlashCommandInteractionEvent slashCommandInteractionEvent) {
        if (!slashCommandInteractionEvent.getName().equals("link")) {
            return;
        }
        if (!this.settings.linkingEnabled) {
            slashCommandInteractionEvent.reply("Discord linking is disabled.").setEphemeral(true).queue();
            return;
        }
        if (!this.settings.linkChannelId.equals("0") && !slashCommandInteractionEvent.getChannel().getId().equals(this.settings.linkChannelId)) {
            slashCommandInteractionEvent.reply("Use this command in the link/general channel.").setEphemeral(true).queue();
            return;
        }
        OptionMapping optionMapping = slashCommandInteractionEvent.getOption("code");
        String string = optionMapping == null ? "" : optionMapping.getAsString().trim().toUpperCase(Locale.ROOT);
        this.completeDiscordLink(string, slashCommandInteractionEvent.getUser().getId(), slashCommandInteractionEvent.getUser().getName(), slashCommandInteractionEvent.getMember(), message -> slashCommandInteractionEvent.reply(message).setEphemeral(true).queue());
    }

    private void completeDiscordLink(String string, String string2, String string3, Member member, java.util.function.Consumer<String> consumer) {
        String string4;
        PendingLink pendingLink = this.pendingLinks.remove(string);
        if (pendingLink == null || pendingLink.expiresAt < System.currentTimeMillis()) {
            consumer.accept("That link code is invalid or expired. Run `/link` in Minecraft for a new one.");
            return;
        }
        if (member == null) {
            consumer.accept("Linking must be done inside the CannaSMP Discord server.");
            return;
        }
        this.linksConfig.set("links." + String.valueOf(pendingLink.uuid) + ".player-name", (Object)pendingLink.playerName);
        this.linksConfig.set("links." + String.valueOf(pendingLink.uuid) + ".discord-id", (Object)string2);
        this.linksConfig.set("links." + String.valueOf(pendingLink.uuid) + ".discord-name", (Object)string3);
        this.saveLinks();
        this.updateKnownMinecraftRank(pendingLink.uuid, this.getMinecraftRank(pendingLink.uuid));
        string4 = this.getRankFromDiscordRoles(member);
        this.updateKnownDiscordRank(pendingLink.uuid, string4.isBlank() ? this.settings.defaultRank : string4);
        consumer.accept("Linked Discord account to Minecraft player `" + this.escapeDiscord(pendingLink.playerName) + "`.");
        this.syncLinkedPlayer(pendingLink.uuid, pendingLink.playerName);
        Player player = Bukkit.getPlayer((UUID)pendingLink.uuid);
        if (player != null) {
            player.sendMessage(this.color("&aDiscord account linked successfully."));
        }
    }

    private void syncAllOnlineLinkedPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.syncLinkedPlayer(player.getUniqueId(), player.getName());
        }
    }

    private void syncMinecraftRankToDiscord(UUID uUID, String string) {
        this.syncMinecraftRankToDiscord(uUID, string, this.getMinecraftRank(uUID));
    }

    private void syncMinecraftRankToDiscord(UUID uUID, String string, String string2) {
        if (!this.settings.rankSyncEnabled || this.jda == null || !this.isLinked(uUID)) {
            return;
        }
        if (this.isDiscordAuthoritative(uUID)) {
            return;
        }
        String string3 = this.getMinecraftRank(uUID);
        if (!string3.equalsIgnoreCase(string2)) {
            return;
        }
        String string4 = this.linksConfig.getString("links." + String.valueOf(uUID) + ".discord-id", "");
        if (string4.isBlank()) {
            return;
        }
        Guild guild = this.getMainGuild();
        if (guild == null) {
            return;
        }
        guild.retrieveMemberById(string4).queue(member -> {
            this.syncLinkedRole((Member)member);
            if (this.settings.minecraftRankToDiscordRoleEnabled) {
                this.syncDiscordRoles((Member)member, string3);
            }
            this.syncNickname(guild, (Member)member, string3, string);
            this.updateKnownMinecraftRank(uUID, string3);
            this.updateKnownDiscordRank(uUID, string3);
            this.getLogger().info("Minecraft rank sync -> Discord: " + string + " = " + string3);
        }, throwable -> this.getLogger().warning("Could not load Discord member for Minecraft rank sync " + string + ": " + throwable.getMessage()));
    }

    private void syncLinkedPlayer(UUID uUID, String string) {
        if (!this.settings.rankSyncEnabled || this.jda == null || !this.isLinked(uUID)) {
            return;
        }
        String string2 = this.linksConfig.getString("links." + String.valueOf(uUID) + ".discord-id", "");
        if (string2.isBlank()) {
            return;
        }
        Guild guild = this.getMainGuild();
        if (guild == null) {
            return;
        }
        guild.retrieveMemberById(string2).queue(member -> {
            this.syncLinkedRole((Member)member);
            String string3 = this.getMinecraftRank(uUID);
            if (this.settings.discordToMinecraftRankEnabled) {
                String string4 = this.getRankFromDiscordRoles((Member)member);
                String string5 = string4.isBlank() ? this.settings.defaultRank : string4;
                if (!string5.equalsIgnoreCase(string3)) {
                    this.markDiscordAuthoritative(uUID);
                    Bukkit.getScheduler().runTask((Plugin)this, () -> Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("lp user " + string + " parent set " + string5)));
                    string3 = string5;
                    this.updateKnownMinecraftRank(uUID, string5);
                }
            }
            this.syncNickname(guild, (Member)member, string3, string);
        }, throwable -> this.getLogger().warning("Could not load Discord member for linked player " + string + ": " + throwable.getMessage()));
    }

    private void syncDiscordRankToMinecraft(Member member) {
        this.syncDiscordRankToMinecraft(member, this.getRankFromDiscordRoles(member), true);
    }

    private void syncDiscordRankToMinecraft(Member member, String string, boolean bl) {
        if (!this.settings.rankSyncEnabled || !this.settings.discordToMinecraftRankEnabled) {
            return;
        }
        UUID uUID = this.getLinkedUuid(member.getId());
        if (uUID == null) {
            return;
        }
        this.markDiscordAuthoritative(uUID);
        String string2 = this.linksConfig.getString("links." + String.valueOf(uUID) + ".player-name", uUID.toString());
        if (string.isBlank() && bl) {
            string = this.settings.defaultRank;
        }
        if (string.isBlank()) {
            return;
        }
        String string3 = string;
        this.getLogger().info("Discord rank sync -> Minecraft: " + string2 + " = " + string3);
        Bukkit.getScheduler().runTask((Plugin)this, () -> Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("lp user " + string2 + " parent set " + string3)));
        this.updateKnownMinecraftRank(uUID, string3);
        this.updateKnownDiscordRank(uUID, string3);
        this.syncNickname(member.getGuild(), member, string3, string2);
    }

    private void syncDiscordRankToMinecraftFresh(Member member2) {
        UUID uUID = this.getLinkedUuid(member2.getId());
        if (uUID == null) {
            return;
        }
        this.markDiscordAuthoritative(uUID);
        Guild guild = member2.getGuild();
        guild.retrieveMemberById(member2.getId()).queue(member -> this.syncDiscordRankToMinecraft((Member)member), throwable -> this.warnThrottled("discord-rank:" + member2.getId(), "Could not refresh Discord member for rank sync: " + throwable.getMessage()));
    }

    private void syncDiscordRankToMinecraftFromRoleEvent(Member member, List<Role> list, boolean bl) {
        String string;
        if (this.shouldIgnoreDiscordRoleEvent(member.getId()) || !this.hasMappedRankRole(list)) {
            return;
        }
        UUID uUID = this.getLinkedUuid(member.getId());
        if (uUID == null) {
            return;
        }
        String string2 = this.getBestRankFromRoles(list);
        if (bl) {
            String string3 = this.getKnownDiscordRank(uUID);
            if (string3 != null && !string3.equalsIgnoreCase(string2)) {
                this.getLogger().info("Discord role removed for " + member.getUser().getName() + ", changed rank: " + string2 + ", active rank remains: " + string3);
                return;
            }
            string = this.getRankFromDiscordRolesAfterChange(member, list, true);
        } else {
            string = string2;
        }
        if (string.isBlank() && bl) {
            string = this.settings.defaultRank;
        }
        this.getLogger().info("Discord role " + (bl ? "removed" : "added") + " for " + member.getUser().getName() + ", changed rank: " + string2 + ", Minecraft rank now: " + (string.isBlank() ? "none" : string));
        this.syncDiscordRankToMinecraft(member, string, bl);
    }

    private void markDiscordAuthoritative(UUID uUID) {
        this.discordAuthoritativeUntil.put(uUID, System.currentTimeMillis() + (long)this.settings.discordAuthoritativeSeconds * 1000L);
        Integer n = this.queuedMinecraftRankSyncs.remove(uUID);
        if (n != null) {
            Bukkit.getScheduler().cancelTask(n.intValue());
        }
    }

    private boolean isDiscordAuthoritative(UUID uUID) {
        Long l = this.discordAuthoritativeUntil.get(uUID);
        if (l == null) {
            return false;
        }
        if (l < System.currentTimeMillis()) {
            this.discordAuthoritativeUntil.remove(uUID);
            return false;
        }
        return true;
    }

    private void ignoreOwnDiscordRoleEvents(String string) {
        this.ignoreDiscordRoleEventsUntil.put(string, System.currentTimeMillis() + 5000L);
    }

    private boolean shouldIgnoreDiscordRoleEvent(String string) {
        Long l = this.ignoreDiscordRoleEventsUntil.get(string);
        if (l == null) {
            return false;
        }
        if (l < System.currentTimeMillis()) {
            this.ignoreDiscordRoleEventsUntil.remove(string);
            return false;
        }
        return true;
    }

    private UUID getLinkedUuid(String string) {
        ConfigurationSection configurationSection = this.getLinksSection();
        for (String string2 : configurationSection.getKeys(false)) {
            if (!string.equals(configurationSection.getString(string2 + ".discord-id", ""))) continue;
            try {
                return UUID.fromString(string2);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                return null;
            }
        }
        return null;
    }

    private void syncNickname(Guild guild, Member member, String string, String string2) {
        if (!this.settings.nicknameSyncEnabled) {
            return;
        }
        String string3 = this.settings.nicknameFormat.replace("{rank}", this.displayRank(string)).replace("{player}", string2);
        try {
            if (!guild.getSelfMember().canInteract(member)) {
                this.warnThrottled("nickname:" + member.getId(), "Nickname sync skipped for " + string2 + ": move the DupeBound Bot role above this member's highest Discord role.");
                return;
            }
            guild.modifyNickname(member, string3).queue(null, throwable -> this.warnThrottled("nickname:" + member.getId(), "Nickname sync failed for " + string2 + ": " + throwable.getMessage()));
        }
        catch (Exception exception) {
            this.warnThrottled("nickname:" + member.getId(), "Nickname sync skipped for " + string2 + ": " + exception.getMessage());
        }
    }

    private String displayRank(String string) {
        if (string == null || string.isBlank()) {
            return "";
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        return string2.substring(0, 1).toUpperCase(Locale.ROOT) + string2.substring(1);
    }

    private String formatDuration(long l) {
        long l2 = Math.max(0L, l / 1000L);
        long l3 = l2 / 3600L;
        long l4 = l2 % 3600L / 60L;
        long l5 = l2 % 60L;
        if (l3 > 0L) {
            return l3 + "h, " + l4 + "m and " + l5 + "s";
        }
        if (l4 > 0L) {
            return l4 + "m and " + l5 + "s";
        }
        return l5 + "s";
    }

    private String discordTimestamp(long l) {
        return "<t:" + l / 1000L + ":F>";
    }

    private void syncLinkedRole(Member member) {
        if (this.settings.linkedRoleId == null || this.settings.linkedRoleId.isBlank() || this.settings.linkedRoleId.equals("0")) {
            return;
        }
        Role role = member.getGuild().getRoleById(this.settings.linkedRoleId);
        if (role != null && !member.getRoles().contains(role)) {
            try {
                if (!member.getGuild().getSelfMember().canInteract(role)) {
                    this.warnThrottled("linked-role", "Linked role sync skipped: move the DupeBound Bot role above Linked.");
                    return;
                }
                member.getGuild().addRoleToMember((UserSnowflake)member, role).queue(null, throwable -> this.warnThrottled("linked-role", "Linked role sync failed: " + throwable.getMessage()));
            }
            catch (Exception exception) {
                this.warnThrottled("linked-role", "Linked role sync skipped: " + exception.getMessage());
            }
        }
    }

    private void removeLinkedRole(UUID uUID) {
        if (this.jda == null || this.settings.linkedRoleId == null || this.settings.linkedRoleId.isBlank() || this.settings.linkedRoleId.equals("0")) {
            return;
        }
        String string = this.linksConfig.getString("links." + String.valueOf(uUID) + ".discord-id", "");
        if (string.isBlank()) {
            return;
        }
        Guild guild = this.getMainGuild();
        if (guild == null) {
            return;
        }
        Role role = guild.getRoleById(this.settings.linkedRoleId);
        if (role == null) {
            return;
        }
        guild.retrieveMemberById(string).queue(member -> {
            if (member.getRoles().contains(role)) {
                try {
                    guild.removeRoleFromMember((UserSnowflake)member, role).queue(null, throwable -> this.warnThrottled("linked-role-remove", "Linked role remove failed: " + throwable.getMessage()));
                }
                catch (Exception exception) {
                    this.warnThrottled("linked-role-remove", "Linked role remove skipped: " + exception.getMessage());
                }
            }
        }, throwable -> this.getLogger().warning("Could not load Discord member for unlink: " + throwable.getMessage()));
    }

    private String getMinecraftRank(UUID uUID) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = (User)luckPerms.getUserManager().loadUser(uUID).join();
            return user.getPrimaryGroup().toLowerCase(Locale.ROOT);
        }
        catch (Exception exception) {
            return this.settings.defaultRank;
        }
    }

    private String getRankFromDiscordRoles(Member member) {
        return this.getBestRankFromRoles(member.getRoles());
    }

    private String getRankFromDiscordRolesAfterChange(Member member, List<Role> list, boolean bl) {
        String string;
        int n;
        String string2;
        String string3 = "";
        int n2 = -1;
        for (Role role : member.getRoles()) {
            if (bl && this.containsRole(list, role.getId()) || (string2 = this.settings.discordRoleToMinecraftRank.get(role.getId())) == null || string2.isBlank() || (n = this.rankWeight(string = string2.toLowerCase(Locale.ROOT))) <= n2) continue;
            string3 = string;
            n2 = n;
        }
        if (!bl) {
            for (Role role : list) {
                string2 = this.settings.discordRoleToMinecraftRank.get(role.getId());
                if (string2 == null || string2.isBlank() || (n = this.rankWeight(string = string2.toLowerCase(Locale.ROOT))) <= n2) continue;
                string3 = string;
                n2 = n;
            }
        }
        return string3;
    }

    private String getBestRankFromRoles(List<Role> list) {
        String string = "";
        int n = -1;
        for (Role role : list) {
            String string2;
            int n2;
            String string3 = this.settings.discordRoleToMinecraftRank.get(role.getId());
            if (string3 == null || string3.isBlank() || (n2 = this.rankWeight(string2 = string3.toLowerCase(Locale.ROOT))) <= n) continue;
            string = string2;
            n = n2;
        }
        return string;
    }

    private boolean hasMappedRankRole(List<Role> list) {
        for (Role role : list) {
            if (!this.settings.discordRoleToMinecraftRank.containsKey(role.getId())) continue;
            return true;
        }
        return false;
    }

    private boolean memberHasRank(Member member, String string) {
        String string2 = this.settings.minecraftRankToDiscordRole.get(string.toLowerCase(Locale.ROOT));
        if (string2 == null || string2.isBlank() || string2.equals("0")) {
            return false;
        }
        for (Role role : member.getRoles()) {
            if (!role.getId().equals(string2)) continue;
            return true;
        }
        return false;
    }

    private boolean containsRole(List<Role> list, String string) {
        for (Role role : list) {
            if (!role.getId().equals(string)) continue;
            return true;
        }
        return false;
    }

    private int rankWeight(String string) {
        return switch (string.toLowerCase(Locale.ROOT)) {
            case "owner" -> 90;
            case "admin" -> 80;
            case "mod" -> 70;
            case "helper" -> 60;
            case "legend" -> 50;
            case "warlord" -> 40;
            case "hunter" -> 30;
            case "raider" -> 20;
            case "survivor" -> 10;
            default -> 0;
        };
    }

    private void syncDiscordRoles(Member member, String string) {
        Guild guild = member.getGuild();
        String string2 = this.settings.minecraftRankToDiscordRole.get(string.toLowerCase(Locale.ROOT));
        if (string2 == null || string2.isBlank() || string2.equals("0")) {
            return;
        }
        Role role = guild.getRoleById(string2);
        if (role == null) {
            return;
        }
        try {
            if (!guild.getSelfMember().canInteract(role)) {
                this.warnThrottled("role:" + string2, "Role sync skipped for " + string + ": move the DupeBound Bot role above " + role.getName() + ".");
                return;
            }
            this.ignoreOwnDiscordRoleEvents(member.getId());
            guild.addRoleToMember((UserSnowflake)member, role).queue(null, throwable -> this.warnThrottled("role:" + string2, "Role sync failed: " + throwable.getMessage()));
        }
        catch (Exception exception) {
            this.warnThrottled("role:" + string2, "Role sync skipped for " + string + ": " + exception.getMessage());
            return;
        }
        if (!this.settings.removeOtherMappedRoles) {
            return;
        }
        for (String string3 : this.settings.minecraftRankToDiscordRole.values()) {
            Role role2;
            if (string3.equals(string2) || string3.equals("0") || (role2 = guild.getRoleById(string3)) == null || !member.getRoles().contains(role2)) continue;
            try {
                this.ignoreOwnDiscordRoleEvents(member.getId());
                guild.removeRoleFromMember((UserSnowflake)member, role2).queue(null, throwable -> this.warnThrottled("old-role:" + string3, "Old role remove failed: " + throwable.getMessage()));
            }
            catch (Exception exception) {
                this.warnThrottled("old-role:" + string3, "Old role remove skipped: " + exception.getMessage());
            }
        }
    }

    private void warnThrottled(String string, String string2) {
        long l;
        long l2 = System.currentTimeMillis();
        if (l2 - (l = this.lastWarningTimes.getOrDefault(string, 0L).longValue()) < (long)this.settings.warningCooldownSeconds * 1000L) {
            return;
        }
        this.lastWarningTimes.put(string, l2);
        this.getLogger().warning(string2);
    }

    private Guild getMainGuild() {
        if (this.jda == null) {
            return null;
        }
        if (!this.settings.guildId.isBlank() && !this.settings.guildId.equals("0")) {
            return this.jda.getGuildById(this.settings.guildId);
        }
        TextChannel textChannel = this.jda.getTextChannelById(this.settings.minecraftChatChannelId);
        return textChannel == null ? null : textChannel.getGuild();
    }

    private String discordName(MessageReceivedEvent messageReceivedEvent) {
        if (messageReceivedEvent.getMember() != null) {
            return messageReceivedEvent.getMember().getEffectiveName();
        }
        return messageReceivedEvent.getAuthor().getName();
    }

    private String generateCode() {
        StringBuilder stringBuilder;
        String string;
        String string2 = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        do {
            stringBuilder = new StringBuilder();
            for (int i = 0; i < 6; ++i) {
                stringBuilder.append(string2.charAt(this.random.nextInt(string2.length())));
            }
        } while (this.pendingLinks.containsKey(string = stringBuilder.toString()));
        return string;
    }

    private void loadLinks() {
        this.linksFile = new File(this.getDataFolder(), "links.yml");
        this.linksConfig = YamlConfiguration.loadConfiguration((File)this.linksFile);
    }

    private void loadKnownMinecraftRanks() {
        ConfigurationSection configurationSection = this.getLinksSection();
        for (String string : configurationSection.getKeys(false)) {
            try {
                UUID uUID = UUID.fromString(string);
                String string2 = configurationSection.getString(string + ".minecraft-rank", "");
                if (string2 == null || string2.isBlank()) {
                    string2 = this.getMinecraftRank(uUID);
                }
                this.updateKnownMinecraftRank(uUID, string2);
                String string3 = configurationSection.getString(string + ".discord-rank", "");
                if (string3 != null && !string3.isBlank()) {
                    this.updateKnownDiscordRank(uUID, string3);
                    continue;
                }
                this.updateKnownDiscordRank(uUID, string2);
            }
            catch (IllegalArgumentException illegalArgumentException) {}
        }
    }

    private void saveLinks() {
        if (this.linksConfig == null || this.linksFile == null) {
            return;
        }
        try {
            this.linksConfig.save(this.linksFile);
        }
        catch (IOException iOException) {
            this.getLogger().warning("Could not save links.yml: " + iOException.getMessage());
        }
    }

    private ConfigurationSection getLinksSection() {
        ConfigurationSection configurationSection = this.linksConfig.getConfigurationSection("links");
        if (configurationSection == null) {
            configurationSection = this.linksConfig.createSection("links");
        }
        return configurationSection;
    }

    private boolean isLinked(UUID uUID) {
        return this.linksConfig.isSet("links." + String.valueOf(uUID) + ".discord-id");
    }

    private String getKnownMinecraftRank(UUID uUID) {
        String string = this.lastMinecraftRanks.get(uUID);
        if (string != null && !string.isBlank()) {
            return string;
        }
        string = this.linksConfig.getString("links." + String.valueOf(uUID) + ".minecraft-rank", "");
        return string == null || string.isBlank() ? null : string.toLowerCase(Locale.ROOT);
    }

    private void updateKnownMinecraftRank(UUID uUID, String string) {
        if (string == null || string.isBlank()) {
            return;
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        this.lastMinecraftRanks.put(uUID, string2);
        this.linksConfig.set("links." + String.valueOf(uUID) + ".minecraft-rank", (Object)string2);
        this.saveLinks();
    }

    private String getKnownDiscordRank(UUID uUID) {
        String string = this.lastDiscordRanks.get(uUID);
        if (string != null && !string.isBlank()) {
            return string;
        }
        string = this.linksConfig.getString("links." + String.valueOf(uUID) + ".discord-rank", "");
        return string == null || string.isBlank() ? null : string.toLowerCase(Locale.ROOT);
    }

    private void updateKnownDiscordRank(UUID uUID, String string) {
        if (string == null || string.isBlank()) {
            return;
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        this.lastDiscordRanks.put(uUID, string2);
        this.linksConfig.set("links." + String.valueOf(uUID) + ".discord-rank", (Object)string2);
        this.saveLinks();
    }

    private void removeLink(UUID uUID) {
        this.linksConfig.set("links." + String.valueOf(uUID), null);
        this.saveLinks();
    }

    private String color(String string) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)string);
    }

    private String escapeDiscord(String string) {
        return string.replace("@", "@\u200b");
    }

    private record BridgeSettings(boolean enabled, String token, String guildId, String serverName, String activity, String minecraftChatChannelId, String staffChatChannelId, String logChannelId, String consoleCommandChannelId, boolean minecraftChatEnabled, boolean discordToMinecraftChatEnabled, boolean discordToMinecraftStaffEnabled, boolean joinLeaveEnabled, boolean deathEnabled, boolean advancementEnabled, boolean outboundBatchingEnabled, int outboundBatchFlushSeconds, int outboundBatchMaxLines, boolean leaveShowIp, String leaveIp, String leaveTimeFormat, String leaveIpFormat, boolean lifecycleMessagesEnabled, String startMessage, String stopMessage, boolean statusTopicEnabled, String statusTopicChannelId, int statusTopicIntervalSeconds, String statusTopicFormat, boolean linkingEnabled, String linkChannelId, String linkCommandPrefix, int linkCodeMinutes, String linkedRoleId, boolean rankSyncEnabled, boolean minecraftRankToDiscordRoleEnabled, boolean discordToMinecraftRankEnabled, boolean nicknameSyncEnabled, boolean removeOtherMappedRoles, String defaultRank, int liveSyncDelayTicks, int discordRoleEventDelayTicks, int discordAuthoritativeSeconds, int discordPollSeconds, int warningCooldownSeconds, String nicknameFormat, Map<String, String> minecraftRankToDiscordRole, Map<String, String> discordRoleToMinecraftRank, boolean discordConsoleCommandsEnabled, String consoleCommandPrefix, List<String> allowedDiscordCommandUserIds, String staffPermission, String discordToMinecraftFormat, String staffMinecraftFormat, String staffDiscordToMinecraftFormat) {
        static BridgeSettings from(JavaPlugin javaPlugin) {
            return new BridgeSettings(javaPlugin.getConfig().getBoolean("enabled", false), javaPlugin.getConfig().getString("discord.bot-token", "PUT_BOT_TOKEN_HERE"), javaPlugin.getConfig().getString("discord.guild-id", "0"), javaPlugin.getConfig().getString("server-name", "DupeBound"), javaPlugin.getConfig().getString("discord.activity", "DupeBound"), javaPlugin.getConfig().getString("channels.minecraft-chat", "0"), javaPlugin.getConfig().getString("channels.staff-chat", "0"), javaPlugin.getConfig().getString("channels.log", "0"), javaPlugin.getConfig().getString("channels.console-commands", "0"), javaPlugin.getConfig().getBoolean("features.minecraft-to-discord-chat", true), javaPlugin.getConfig().getBoolean("features.discord-to-minecraft-chat", true), javaPlugin.getConfig().getBoolean("features.discord-to-minecraft-staff", true), javaPlugin.getConfig().getBoolean("features.join-leave-log", true), javaPlugin.getConfig().getBoolean("features.death-log", true), javaPlugin.getConfig().getBoolean("features.advancement-log", false), javaPlugin.getConfig().getBoolean("outbound-batching.enabled", true), javaPlugin.getConfig().getInt("outbound-batching.flush-seconds", 3), javaPlugin.getConfig().getInt("outbound-batching.max-lines-per-message", 8), javaPlugin.getConfig().getBoolean("leave-embed.show-ip", true), javaPlugin.getConfig().getString("leave-embed.ip", "play.dupebound.net"), javaPlugin.getConfig().getString("leave-embed.time-format", "Time played: {time}"), javaPlugin.getConfig().getString("leave-embed.ip-format", "IP: {ip}"), javaPlugin.getConfig().getBoolean("lifecycle-messages.enabled", true), javaPlugin.getConfig().getString("lifecycle-messages.start", "**DupeBound is online.**"), javaPlugin.getConfig().getString("lifecycle-messages.stop", "**DupeBound is offline.**"), javaPlugin.getConfig().getBoolean("status-topic.enabled", true), javaPlugin.getConfig().getString("status-topic.channel-id", javaPlugin.getConfig().getString("channels.minecraft-chat", "0")), javaPlugin.getConfig().getInt("status-topic.interval-seconds", 60), javaPlugin.getConfig().getString("status-topic.format", "DupeBound | Online: {online}/{max} | RAM: {ram_used}/{ram_max} MB"), javaPlugin.getConfig().getBoolean("linking.enabled", true), javaPlugin.getConfig().getString("linking.channel-id", javaPlugin.getConfig().getString("channels.minecraft-chat", "0")), javaPlugin.getConfig().getString("linking.command-prefix", "!link "), javaPlugin.getConfig().getInt("linking.code-minutes", 10), javaPlugin.getConfig().getString("linking.linked-role-id", "0"), javaPlugin.getConfig().getBoolean("rank-sync.enabled", true), javaPlugin.getConfig().getBoolean("rank-sync.minecraft-rank-to-discord-role.enabled", true), javaPlugin.getConfig().getBoolean("rank-sync.discord-role-to-minecraft-rank.enabled", true), javaPlugin.getConfig().getBoolean("rank-sync.nickname-sync.enabled", true), javaPlugin.getConfig().getBoolean("rank-sync.minecraft-rank-to-discord-role.remove-other-mapped-roles", true), javaPlugin.getConfig().getString("rank-sync.default-rank", "default"), javaPlugin.getConfig().getInt("rank-sync.live-sync-delay-ticks", 40), javaPlugin.getConfig().getInt("rank-sync.discord-role-event-delay-ticks", 40), javaPlugin.getConfig().getInt("rank-sync.discord-authoritative-seconds", 20), javaPlugin.getConfig().getInt("rank-sync.discord-poll-seconds", 10), javaPlugin.getConfig().getInt("rank-sync.warning-cooldown-seconds", 60), javaPlugin.getConfig().getString("rank-sync.nickname-sync.format", "[{rank}] {player}"), BridgeSettings.loadStringMap(javaPlugin, "rank-sync.minecraft-rank-to-discord-role.roles"), BridgeSettings.loadStringMap(javaPlugin, "rank-sync.discord-role-to-minecraft-rank.roles"), javaPlugin.getConfig().getBoolean("discord-console-commands.enabled", false), javaPlugin.getConfig().getString("discord-console-commands.prefix", "!cmd "), new ArrayList<String>(javaPlugin.getConfig().getStringList("discord-console-commands.allowed-user-ids")), javaPlugin.getConfig().getString("staff.permission", "dupebound.staffchat"), javaPlugin.getConfig().getString("formats.discord-to-minecraft-chat", "&9[Discord] &f{user}&7: &f{message}"), javaPlugin.getConfig().getString("formats.minecraft-staff-chat", "&c[Staff] &f{player}&7: &f{message}"), javaPlugin.getConfig().getString("formats.discord-to-minecraft-staff", "&9[Discord Staff] &f{user}&7: &f{message}"));
        }

        private static Map<String, String> loadStringMap(JavaPlugin javaPlugin, String string) {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            ConfigurationSection configurationSection = javaPlugin.getConfig().getConfigurationSection(string);
            if (configurationSection == null) {
                return hashMap;
            }
            for (String string2 : configurationSection.getKeys(false)) {
                hashMap.put(string2.toLowerCase(Locale.ROOT), configurationSection.getString(string2, "0"));
            }
            return hashMap;
        }
    }

    private record PendingLink(UUID uuid, String playerName, long expiresAt) {
    }

    private record PendingDiscordCommand(String command, long expiresAt, String channelId) {
    }

    private final class DiscordListener
    extends ListenerAdapter {
        private DiscordListener() {
        }

        public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent guildMemberRoleAddEvent) {
            DupeboundDiscordBridgePlugin.this.syncDiscordRankToMinecraftFromRoleEvent(guildMemberRoleAddEvent.getMember(), guildMemberRoleAddEvent.getRoles(), false);
        }

        public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent guildMemberRoleRemoveEvent) {
            DupeboundDiscordBridgePlugin.this.syncDiscordRankToMinecraftFromRoleEvent(guildMemberRoleRemoveEvent.getMember(), guildMemberRoleRemoveEvent.getRoles(), true);
        }

        public void onSlashCommandInteraction(SlashCommandInteractionEvent slashCommandInteractionEvent) {
            DupeboundDiscordBridgePlugin.this.handleDiscordSlashLink(slashCommandInteractionEvent);
        }

        public void onMessageReceived(MessageReceivedEvent messageReceivedEvent) {
            if (messageReceivedEvent.getAuthor().isBot() || messageReceivedEvent.isWebhookMessage()) {
                return;
            }
            String string = messageReceivedEvent.getChannel().getId();
            String string2 = messageReceivedEvent.getMessage().getContentDisplay();
            DupeboundDiscordBridgePlugin.this.runConsoleCommandFromDiscord(messageReceivedEvent.getMessage(), string2);
            if (string2.toLowerCase(Locale.ROOT).startsWith(DupeboundDiscordBridgePlugin.this.settings.consoleCommandPrefix.toLowerCase(Locale.ROOT))) {
                return;
            }
            if (string2.isBlank()) {
                return;
            }
            if (DupeboundDiscordBridgePlugin.this.handleMinecraftDiscordCommand(messageReceivedEvent, string2)) {
                return;
            }
            DupeboundDiscordBridgePlugin.this.handleDiscordLink(messageReceivedEvent, string2);
            if (string.equals(DupeboundDiscordBridgePlugin.this.settings.minecraftChatChannelId) && DupeboundDiscordBridgePlugin.this.settings.discordToMinecraftChatEnabled) {
                if (!string2.toLowerCase(Locale.ROOT).startsWith(DupeboundDiscordBridgePlugin.this.settings.linkCommandPrefix.toLowerCase(Locale.ROOT))) {
                    DupeboundDiscordBridgePlugin.this.broadcastMinecraft(DupeboundDiscordBridgePlugin.this.settings.discordToMinecraftFormat, DupeboundDiscordBridgePlugin.this.discordName(messageReceivedEvent), string2);
                }
            } else if (string.equals(DupeboundDiscordBridgePlugin.this.settings.staffChatChannelId) && DupeboundDiscordBridgePlugin.this.settings.discordToMinecraftStaffEnabled) {
                DupeboundDiscordBridgePlugin.this.sendMinecraftStaff(DupeboundDiscordBridgePlugin.this.discordName(messageReceivedEvent), string2);
            } else if (string.equals(DupeboundDiscordBridgePlugin.this.settings.consoleCommandChannelId)) {
                DupeboundDiscordBridgePlugin.this.runConsoleCommandFromDiscord(messageReceivedEvent.getMessage(), string2);
            }
        }
    }
}
