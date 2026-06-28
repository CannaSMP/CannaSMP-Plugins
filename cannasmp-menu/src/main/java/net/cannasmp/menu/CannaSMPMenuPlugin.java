package net.cannasmp.menu;

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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class CannaSMPMenuPlugin extends JavaPlugin implements Listener {
    private static final String TITLE = ChatColor.DARK_GREEN + "CannaSMP Menu";
    private final Map<Integer, MenuAction> actions = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerActions();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /menu.");
            return true;
        }
        open(player);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        MenuAction action = actions.get(event.getRawSlot());
        if (action == null) {
            return;
        }

        player.closeInventory();
        if (action.command() != null) {
            player.performCommand(action.command());
        } else {
            player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Menu" + ChatColor.DARK_GRAY + "] "
                    + ChatColor.GRAY + action.help());
        }
    }

    private void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, TITLE);
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        gui.setItem(4, item(Material.LIME_DYE, ChatColor.GREEN + "" + ChatColor.BOLD + "CannaSMP",
                ChatColor.GRAY + "Player commands and shortcuts."));

        put(gui, 10, Material.CLOCK, ChatColor.GREEN + "AFK", "afk",
                ChatColor.GRAY + "/afk",
                ChatColor.GRAY + "Teleports to AFK after 5 seconds.",
                ChatColor.GRAY + "Moving cancels it.",
                ChatColor.GRAY + "Earn shards only in AFK.");
        put(gui, 11, Material.AMETHYST_SHARD, ChatColor.GREEN + "Shards", "shards",
                ChatColor.GRAY + "/shards or /afkshards",
                ChatColor.GRAY + "Shows your AFK shard balance.");
        put(gui, 12, Material.EMERALD, ChatColor.GREEN + "Shard Shop", "shardshop",
                ChatColor.GRAY + "/shardshop or /afkshop",
                ChatColor.GRAY + "Spend shards on keys, tools, spawners.");
        put(gui, 13, Material.BOOK, ChatColor.GREEN + "Leaderboards", "leaderboard",
                ChatColor.GRAY + "/leaderboard, /leaderboards, /lbs",
                ChatColor.GRAY + "Shows bal, kills, deaths, playtime, shards.");
        put(gui, 14, Material.PAPER, ChatColor.GREEN + "Notice", "notice",
                ChatColor.GRAY + "/serverwarning, /warning, /notice",
                ChatColor.GRAY + "Shows the CannaSMP mature-theme notice.");
        put(gui, 15, Material.COMPASS, ChatColor.GREEN + "Random Teleport", "rtp",
                ChatColor.GRAY + "/rtp",
                ChatColor.GRAY + "Random teleport GUI/command.");
        put(gui, 16, Material.PLAYER_HEAD, ChatColor.GREEN + "Skin", null,
                ChatColor.GRAY + "/skin <username>",
                ChatColor.GRAY + "Changes your skin to another player.");

        put(gui, 19, Material.NETHER_STAR, ChatColor.GREEN + "Spawn", "spawn",
                ChatColor.GRAY + "/spawn",
                ChatColor.GRAY + "Teleports to spawn.");
        put(gui, 20, Material.LANTERN, ChatColor.GREEN + "Warp AFK", "warp afk",
                ChatColor.GRAY + "/warp afk",
                ChatColor.GRAY + "Teleports to AFK warp.");
        put(gui, 21, Material.CHEST, ChatColor.GREEN + "Warp Crates", "warp crates",
                ChatColor.GRAY + "/warp crates",
                ChatColor.GRAY + "Teleports to crates.");
        put(gui, 22, Material.IRON_HELMET, ChatColor.GREEN + "Warp Staffteam", "warp staffteam",
                ChatColor.GRAY + "/warp staffteam",
                ChatColor.GRAY + "Teleports to staff team area.");
        put(gui, 23, Material.BARREL, ChatColor.GREEN + "Shop", "shop",
                ChatColor.GRAY + "/shop",
                ChatColor.GRAY + "Opens the server shop.");
        put(gui, 24, Material.GOLD_INGOT, ChatColor.GREEN + "Balance", "bal",
                ChatColor.GRAY + "/bal or /balance",
                ChatColor.GRAY + "Shows your money balance.");
        put(gui, 25, Material.GOLD_NUGGET, ChatColor.GREEN + "Pay", null,
                ChatColor.GRAY + "/pay <player> <amount>",
                ChatColor.GRAY + "Pays another player.");

        put(gui, 29, Material.ENDER_CHEST, ChatColor.GREEN + "Auction House", "ah",
                ChatColor.GRAY + "/ah or /auctionhouse",
                ChatColor.GRAY + "Opens the auction house.");
        put(gui, 30, Material.WRITABLE_BOOK, ChatColor.GREEN + "Message", null,
                ChatColor.GRAY + "/msg <player> <message>",
                ChatColor.GRAY + "Private message another player.");
        put(gui, 31, Material.FEATHER, ChatColor.GREEN + "Reply", null,
                ChatColor.GRAY + "/r <message>",
                ChatColor.GRAY + "Reply to your last private message.");
        put(gui, 32, Material.NAME_TAG, ChatColor.GREEN + "Discord Link", "link",
                ChatColor.GRAY + "/link",
                ChatColor.GRAY + "Get your Discord link code.");
        put(gui, 33, Material.BARRIER, ChatColor.RED + "Discord Unlink", "unlink",
                ChatColor.GRAY + "/unlink",
                ChatColor.GRAY + "Unlink Minecraft from Discord.");
        gui.setItem(40, item(Material.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close this menu."));

        player.openInventory(gui);
    }

    private void put(Inventory gui, int slot, Material material, String name, String command, String... lore) {
        gui.setItem(slot, item(material, name, lore));
        actions.put(slot, new MenuAction(command, lore.length > 0 ? ChatColor.stripColor(lore[0]) : name));
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void registerActions() {
        actions.clear();
        actions.put(40, new MenuAction(null, "Menu closed."));
    }

    private record MenuAction(String command, String help) {
    }
}
