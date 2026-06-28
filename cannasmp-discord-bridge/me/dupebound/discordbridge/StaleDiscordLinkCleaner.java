/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.java.JavaPlugin
 */
package me.dupebound.discordbridge;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class StaleDiscordLinkCleaner {
    private StaleDiscordLinkCleaner() {
    }

    public static void handle(JavaPlugin javaPlugin, String string, String string2, Throwable throwable) {
        ConfigurationSection configurationSection;
        String string3;
        String string4 = string3 = throwable == null ? "" : String.valueOf(throwable.getMessage());
        if (!StaleDiscordLinkCleaner.isUnknownMember(string3)) {
            javaPlugin.getLogger().warning("Could not poll Discord roles for " + string + ": " + string3);
            return;
        }
        File file = new File(javaPlugin.getDataFolder(), "links.yml");
        FileConfiguration fileConfiguration = StaleDiscordLinkCleaner.liveLinksConfig(javaPlugin);
        if (fileConfiguration == null) {
            fileConfiguration = YamlConfiguration.loadConfiguration((File)file);
        }
        if ((configurationSection = fileConfiguration.getConfigurationSection("links")) == null) {
            return;
        }
        String string5 = string;
        boolean bl = false;
        Set set = configurationSection.getKeys(false);
        for (String string6 : set) {
            String string7;
            String string8 = "links." + string6 + ".";
            String string9 = fileConfiguration.getString(string8 + "player-name", "");
            if (!StaleDiscordLinkCleaner.matches(string, string2, string9, string7 = fileConfiguration.getString(string8 + "discord-id", ""))) continue;
            string5 = string9 == null || string9.isBlank() ? string : string9;
            fileConfiguration.set(string8 + "discord-id", null);
            fileConfiguration.set(string8 + "discord-name", null);
            fileConfiguration.set(string8 + "discord-rank", (Object)"default");
            bl = true;
        }
        if (!bl) {
            return;
        }
        try {
            fileConfiguration.save(file);
            javaPlugin.getLogger().info("Cleared stale Discord link for " + string5 + " because that Discord member is no longer in the server.");
        }
        catch (IOException iOException) {
            javaPlugin.getLogger().warning("Failed to clear stale Discord link for " + string5 + ": " + iOException.getMessage());
        }
    }

    private static FileConfiguration liveLinksConfig(JavaPlugin javaPlugin) {
        try {
            Field field = javaPlugin.getClass().getDeclaredField("linksConfig");
            field.setAccessible(true);
            Object object = field.get(javaPlugin);
            if (object instanceof FileConfiguration) {
                FileConfiguration fileConfiguration = (FileConfiguration)object;
                return fileConfiguration;
            }
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            // empty catch block
        }
        return null;
    }

    private static boolean isUnknownMember(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        return string2.contains("10007") || string2.contains("unknown member");
    }

    private static boolean matches(String string, String string2, String string3, String string4) {
        return StaleDiscordLinkCleaner.equalsIgnoreCase(string, string3) || StaleDiscordLinkCleaner.equalsIgnoreCase(string2, string3) || StaleDiscordLinkCleaner.equals(string, string4) || StaleDiscordLinkCleaner.equals(string2, string4);
    }

    private static boolean equalsIgnoreCase(String string, String string2) {
        return string != null && string2 != null && string.equalsIgnoreCase(string2);
    }

    private static boolean equals(String string, String string2) {
        return string != null && string2 != null && string.equals(string2);
    }
}

