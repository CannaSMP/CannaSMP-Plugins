package net.cannasmp.statsapi.managers;

import net.cannasmp.statsapi.config.ApiConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class IntegrationManager {
    private final JavaPlugin plugin;
    private final ApiConfig config;
    private Object economy;
    private Class<?> economyClass;

    public IntegrationManager(JavaPlugin plugin, ApiConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void refresh() {
        economy = null;
        economyClass = null;
        if (config.vaultEnabled() && isEnabled("Vault")) {
            try {
                economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(economyClass);
                economy = rsp == null ? null : rsp.getProvider();
            } catch (ReflectiveOperationException | RuntimeException ex) {
                plugin.getLogger().fine("Vault economy unavailable: " + ex.getMessage());
            }
        }
    }

    public Map<String, Object> statusMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Vault", economy != null);
        map.put("LuckPerms", config.luckPermsEnabled() && isEnabled("LuckPerms"));
        map.put("PlaceholderAPI", config.placeholderApiEnabled() && isEnabled("PlaceholderAPI"));
        map.put("Essentials", config.essentialsEnabled() && (isEnabled("Essentials") || isEnabled("EssentialsX")));
        return map;
    }

    public Double balance(Player player) {
        if (economy == null || economyClass == null) {
            return null;
        }
        try {
            Method method = economyClass.getMethod("getBalance", OfflinePlayer.class);
            Object value = method.invoke(economy, player);
            return value instanceof Number number ? number.doubleValue() : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    public String rank(Player player) {
        if (!config.luckPermsEnabled() || !isEnabled("LuckPerms")) {
            return null;
        }
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = provider.getMethod("get").invoke(null);
            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, player.getUniqueId());
            if (user == null) {
                return null;
            }
            return Objects.toString(user.getClass().getMethod("getPrimaryGroup").invoke(user), null);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    public boolean isAfk(Player player) {
        if (!config.essentialsEnabled()) {
            return false;
        }
        try {
            Object essentials = Bukkit.getPluginManager().getPlugin("Essentials");
            if (essentials == null) {
                return false;
            }
            Object user = essentials.getClass().getMethod("getUser", org.bukkit.entity.Player.class).invoke(essentials, player);
            Object value = user.getClass().getMethod("isAfk").invoke(user);
            return value instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return false;
        }
    }

    public boolean isVanished(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player) && !viewer.canSee(player)) {
                return true;
            }
        }
        return player.hasMetadata("vanished") || player.hasMetadata("invisible");
    }

    private boolean isEnabled(String pluginName) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }
}
