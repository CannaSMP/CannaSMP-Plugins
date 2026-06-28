/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.dv8tion.jda.api.entities.Message
 *  net.kyori.adventure.audience.MessageType
 *  net.kyori.adventure.identity.Identity
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.ComponentLike
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  net.md_5.bungee.api.chat.BaseComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.Server
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.CommandSender$Spigot
 *  org.bukkit.command.ConsoleCommandSender
 *  org.bukkit.conversations.Conversation
 *  org.bukkit.conversations.ConversationAbandonedEvent
 *  org.bukkit.permissions.Permission
 *  org.bukkit.permissions.PermissionAttachment
 *  org.bukkit.permissions.PermissionAttachmentInfo
 *  org.bukkit.plugin.Plugin
 */
package me.dupebound.discordbridge;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.dv8tion.jda.api.entities.Message;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

public final class DiscordConsoleCapture {
    private DiscordConsoleCapture() {
    }

    public static void run(String string, Message message) {
        CapturingSender capturingSender = new CapturingSender(Bukkit.getConsoleSender());
        boolean bl = Bukkit.dispatchCommand((CommandSender)capturingSender, (String)string);
        Object object = capturingSender.output();
        if (((String)object).isBlank() && DiscordConsoleCapture.isTpsCommand(string)) {
            object = DiscordConsoleCapture.tpsOutput();
        }
        if (((String)object).isBlank()) {
            object = DiscordConsoleCapture.fallbackOutput(string, bl);
        }
        if (((String)object).isBlank()) {
            Object object2 = object = bl ? "No output returned." : "Unknown command or command failed.";
        }
        if (((String)object).length() > 1800) {
            object = ((String)object).substring(0, 1800) + "\n...output trimmed";
        }
        message.reply((CharSequence)("Ran console command: `" + DiscordConsoleCapture.escape(string) + "`\n```text\n" + DiscordConsoleCapture.sanitizeCodeBlock((String)object) + "\n```")).queue();
    }

    private static String escape(String string) {
        return string.replace("`", "'");
    }

    private static String sanitizeCodeBlock(String string) {
        return string.replace("```", "'''").replace("\r", "");
    }

    private static boolean isTpsCommand(String string) {
        String string2 = string.trim().toLowerCase(Locale.ROOT);
        return string2.equals("tps") || string2.equals("minecraft:tps") || string2.equals("paper:tps") || string2.equals("purpur:tps");
    }

    private static String tpsOutput() {
        double[] dArray = Bukkit.getTPS();
        return String.format(Locale.US, "TPS from last 1m, 5m, 15m: %s, %s, %s%nMSPT: %.2f ms", DiscordConsoleCapture.formatTps(dArray, 0), DiscordConsoleCapture.formatTps(dArray, 1), DiscordConsoleCapture.formatTps(dArray, 2), Bukkit.getAverageTickTime());
    }

    private static String formatTps(double[] dArray, int n) {
        if (dArray == null || dArray.length <= n) {
            return "n/a";
        }
        return String.format(Locale.US, "%.2f", Math.min(20.0, dArray[n]));
    }

    private static String fallbackOutput(String string, boolean bl) {
        String string2 = string.trim();
        if (string2.isEmpty()) {
            return "";
        }
        String[] stringArray = string2.split("\\s+");
        String string3 = stringArray[0].toLowerCase(Locale.ROOT);
        int n = string3.indexOf(58);
        if (n >= 0) {
            string3 = string3.substring(n + 1);
        }
        if (string3.equals("list")) {
            List<String> list = Bukkit.getOnlinePlayers().stream().map(player -> player.getName()).sorted(String.CASE_INSENSITIVE_ORDER).toList();
            String string4 = list.isEmpty() ? "none" : String.join((CharSequence)", ", list);
            return "Online players (" + list.size() + "/" + Bukkit.getMaxPlayers() + "): " + string4;
        }
        if (!bl || stringArray.length < 2) {
            return "";
        }
        String string5 = stringArray[1];
        return switch (string3) {
            case "kick" -> "Kick command ran for " + string5 + ".";
            case "ban", "minecraft:ban" -> "Ban command ran for " + string5 + ".";
            case "ban-ip" -> "IP ban command ran for " + string5 + ".";
            case "pardon", "unban" -> "Pardon command ran for " + string5 + ".";
            case "pardon-ip" -> "IP pardon command ran for " + string5 + ".";
            case "op" -> "Op command ran for " + string5 + ".";
            case "deop" -> "Deop command ran for " + string5 + ".";
            case "whitelist" -> "Whitelist command ran: " + string2 + ".";
            case "gamemode" -> "Gamemode command ran: " + string2 + ".";
            case "tp", "teleport" -> "Teleport command ran: " + string2 + ".";
            case "give" -> "Give command ran: " + string2 + ".";
            case "effect" -> "Effect command ran: " + string2 + ".";
            default -> "";
        };
    }

    private static final class CapturingSender
    implements ConsoleCommandSender {
        private final ConsoleCommandSender delegate;
        private final StringBuilder output = new StringBuilder();

        private CapturingSender(ConsoleCommandSender consoleCommandSender) {
            this.delegate = consoleCommandSender;
        }

        private void capture(String string) {
            if (string == null || string.isBlank()) {
                return;
            }
            if (this.output.length() > 0) {
                this.output.append('\n');
            }
            this.output.append(string.replaceAll("\u00a7[0-9A-FK-ORa-fk-or]", ""));
        }

        private void capture(Component component) {
            if (component == null) {
                return;
            }
            this.capture(PlainTextComponentSerializer.plainText().serialize(component));
        }

        private String output() {
            return this.output.toString().trim();
        }

        public void sendMessage(String string) {
            this.capture(string);
        }

        public void sendMessage(Component component) {
            this.capture(component);
        }

        public void sendMessage(ComponentLike componentLike) {
            if (componentLike != null) {
                this.capture(componentLike.asComponent());
            }
        }

        public void sendMessage(Identity identity, Component component, MessageType messageType) {
            this.capture(component);
        }

        public void sendMessage(String ... stringArray) {
            for (String string : stringArray) {
                this.capture(string);
            }
        }

        public void sendMessage(UUID uUID, String string) {
            this.capture(string);
        }

        public void sendMessage(UUID uUID, String ... stringArray) {
            this.sendMessage(stringArray);
        }

        public void sendRawMessage(String string) {
            this.capture(string);
        }

        public void sendRawMessage(UUID uUID, String string) {
            this.capture(string);
        }

        public Server getServer() {
            return this.delegate.getServer();
        }

        public String getName() {
            return this.delegate.getName();
        }

        public Component name() {
            return this.delegate.name();
        }

        public CommandSender.Spigot spigot() {
            return new CommandSender.Spigot(){

                public void sendMessage(BaseComponent baseComponent) {
                    this.capture(baseComponent == null ? "" : baseComponent.toPlainText());
                }

                public void sendMessage(BaseComponent ... baseComponentArray) {
                    this.capture(BaseComponent.toPlainText((BaseComponent[])baseComponentArray));
                }

                public void sendMessage(UUID uUID, BaseComponent baseComponent) {
                    this.sendMessage(baseComponent);
                }

                public void sendMessage(UUID uUID, BaseComponent ... baseComponentArray) {
                    this.sendMessage(baseComponentArray);
                }
            };
        }

        public boolean isConversing() {
            return this.delegate.isConversing();
        }

        public void acceptConversationInput(String string) {
            this.delegate.acceptConversationInput(string);
        }

        public boolean beginConversation(Conversation conversation) {
            return this.delegate.beginConversation(conversation);
        }

        public void abandonConversation(Conversation conversation) {
            this.delegate.abandonConversation(conversation);
        }

        public void abandonConversation(Conversation conversation, ConversationAbandonedEvent conversationAbandonedEvent) {
            this.delegate.abandonConversation(conversation, conversationAbandonedEvent);
        }

        public boolean isPermissionSet(String string) {
            return this.delegate.isPermissionSet(string);
        }

        public boolean isPermissionSet(Permission permission) {
            return this.delegate.isPermissionSet(permission);
        }

        public boolean hasPermission(String string) {
            return this.delegate.hasPermission(string);
        }

        public boolean hasPermission(Permission permission) {
            return this.delegate.hasPermission(permission);
        }

        public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bl) {
            return this.delegate.addAttachment(plugin, string, bl);
        }

        public PermissionAttachment addAttachment(Plugin plugin) {
            return this.delegate.addAttachment(plugin);
        }

        public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bl, int n) {
            return this.delegate.addAttachment(plugin, string, bl, n);
        }

        public PermissionAttachment addAttachment(Plugin plugin, int n) {
            return this.delegate.addAttachment(plugin, n);
        }

        public void removeAttachment(PermissionAttachment permissionAttachment) {
            this.delegate.removeAttachment(permissionAttachment);
        }

        public void recalculatePermissions() {
            this.delegate.recalculatePermissions();
        }

        public Set<PermissionAttachmentInfo> getEffectivePermissions() {
            return this.delegate.getEffectivePermissions();
        }

        public boolean isOp() {
            return true;
        }

        public void setOp(boolean bl) {
            this.delegate.setOp(bl);
        }
    }
}

