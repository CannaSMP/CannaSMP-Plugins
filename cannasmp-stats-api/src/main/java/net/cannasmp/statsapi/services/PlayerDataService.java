package net.cannasmp.statsapi.services;

import net.cannasmp.statsapi.managers.IntegrationManager;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerDataService {
    private final IntegrationManager integrations;

    public PlayerDataService(IntegrationManager integrations) {
        this.integrations = integrations;
    }

    public Map<String, Object> collect(Player player) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("username", player.getName());
        map.put("uuid", player.getUniqueId().toString());
        map.put("ping", player.getPing());
        map.put("health", round(player.getHealth()));
        map.put("food", player.getFoodLevel());
        map.put("xp", player.getExp());
        map.put("level", player.getLevel());
        map.put("gamemode", player.getGameMode().name());
        map.put("world", player.getWorld().getName());
        map.put("coordinates", coordinates(player));
        map.put("armor", armor(player));
        map.put("heldItem", item(player.getInventory().getItemInMainHand()));
        map.put("inventorySummary", inventorySummary(player));
        map.put("playtimeSeconds", player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L);
        map.put("balance", integrations.balance(player));
        map.put("rank", integrations.rank(player));
        map.put("team", player.getScoreboard().getEntryTeam(player.getName()) == null ? null : player.getScoreboard().getEntryTeam(player.getName()).getName());
        map.put("afk", integrations.isAfk(player));
        map.put("vanish", integrations.isVanished(player));
        return map;
    }

    private Map<String, Object> coordinates(Player player) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("x", round(player.getLocation().getX()));
        map.put("y", round(player.getLocation().getY()));
        map.put("z", round(player.getLocation().getZ()));
        map.put("yaw", round(player.getLocation().getYaw()));
        map.put("pitch", round(player.getLocation().getPitch()));
        return map;
    }

    private Map<String, Object> armor(Player player) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("helmet", item(player.getInventory().getHelmet()));
        map.put("chestplate", item(player.getInventory().getChestplate()));
        map.put("leggings", item(player.getInventory().getLeggings()));
        map.put("boots", item(player.getInventory().getBoots()));
        return map;
    }

    private Map<String, Object> inventorySummary(Player player) {
        Map<String, Object> summary = new LinkedHashMap<>();
        int used = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            used++;
            summary.merge(stack.getType().name(), stack.getAmount(), (left, right) -> (Integer) left + (Integer) right);
        }
        summary.put("usedSlots", used);
        return summary;
    }

    private Map<String, Object> item(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return Map.of("type", "AIR", "amount", 0);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", stack.getType().name());
        map.put("amount", stack.getAmount());
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            map.put("displayName", stack.getItemMeta().getDisplayName());
        }
        return map;
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }
}
