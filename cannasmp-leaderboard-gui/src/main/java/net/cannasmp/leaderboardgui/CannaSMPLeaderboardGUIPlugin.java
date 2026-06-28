package net.cannasmp.leaderboardgui;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class CannaSMPLeaderboardGUIPlugin extends JavaPlugin implements Listener {
    private static final String TITLE = ChatColor.DARK_GREEN + "CannaSMP Leaderboards";

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        open(player);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    private void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(10, board(player, Material.EMERALD, ChatColor.GOLD + "" + ChatColor.BOLD + "Top Balances",
                "vault_eco_balance", "$", true, ChatColor.GOLD));
        inventory.setItem(11, board(player, Material.DIAMOND_SWORD, ChatColor.RED + "" + ChatColor.BOLD + "Top Kills",
                "statistic_player_kills", "", false, ChatColor.RED));
        inventory.setItem(13, board(player, Material.SKELETON_SKULL, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Top Deaths",
                "statistic_deaths", "", false, ChatColor.LIGHT_PURPLE));
        inventory.setItem(15, board(player, Material.CLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "Top Playtime",
                "statistic_time_played", "", false, ChatColor.GREEN));
        inventory.setItem(16, board(player, Material.AMETHYST_SHARD, ChatColor.GREEN + "" + ChatColor.BOLD + "Top Shards",
                "cannasmp_shards", "", true, ChatColor.GREEN));

        player.openInventory(inventory);
    }

    private ItemStack board(Player player, Material material, String title, String board, String prefix, boolean formatted, ChatColor color) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "----------------");
        for (int rank = 1; rank <= 10; rank++) {
            String name = parse(player, "%ajlb_lb_" + board + "_" + rank + "_alltime_name%");
            String valuePlaceholder = "%ajlb_lb_" + board + "_" + rank + "_alltime_value" + (formatted ? "_formatted" : "") + "%";
            String value = parse(player, valuePlaceholder);
            if (isBlankEntry(name) || isBlankEntry(value)) {
                lore.add(ChatColor.DARK_GRAY + "#" + rank + " Empty");
            } else {
                lore.add(color + "#" + rank + " " + ChatColor.WHITE + name + ChatColor.GRAY + " - " + color + prefix + value);
            }
        }
        lore.add(ChatColor.DARK_GRAY + "----------------");
        String position = parse(player, "%ajlb_position_" + board + "_alltime%");
        lore.add(color + "Your rank: " + ChatColor.WHITE + "#" + (position.isBlank() ? "---" : position));
        return item(material, title, lore);
    }

    private String parse(Player player, String placeholder) {
        String value = PlaceholderAPI.setPlaceholders(player, placeholder);
        return value == null ? "" : ChatColor.stripColor(value).trim();
    }

    private boolean isBlankEntry(String value) {
        return value == null || value.isBlank() || value.equals("---") || value.contains("%ajlb_");
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
