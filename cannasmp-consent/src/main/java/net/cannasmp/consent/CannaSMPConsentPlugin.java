package net.cannasmp.consent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CannaSMPConsentPlugin extends JavaPlugin implements Listener {
    private static final String GUI_TITLE = ChatColor.DARK_GREEN + "CannaSMP Notice";
    private static final int CONFIRM_SLOT = 15;
    private static final int DECLINE_SLOT = 11;

    private final Set<UUID> pending = new HashSet<>();
    private final Set<UUID> kicking = new HashSet<>();
    private File acceptedFile;
    private FileConfiguration accepted;

    @Override
    public void onEnable() {
        acceptedFile = new File(getDataFolder(), "accepted.yml");
        if (!acceptedFile.exists()) {
            getDataFolder().mkdirs();
        }
        accepted = YamlConfiguration.loadConfiguration(acceptedFile);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Consent GUI enabled. Data file: " + acceptedFile.getPath());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        touch(player);
        if (hasAccepted(player.getUniqueId())) {
            return;
        }

        pending.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline() && !hasAccepted(player.getUniqueId())) {
                openConsentGui(player);
            }
        }, 40L);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() == CONFIRM_SLOT) {
            accept(player);
            pending.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "CannaSMP" + ChatColor.DARK_GRAY + "] "
                    + ChatColor.GREEN + "Notice accepted. Welcome to CannaSMP.");
        } else if (event.getRawSlot() == DECLINE_SLOT) {
            decline(player);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        if (!pending.contains(player.getUniqueId()) || kicking.contains(player.getUniqueId()) || hasAccepted(player.getUniqueId())) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline() && pending.contains(player.getUniqueId()) && !hasAccepted(player.getUniqueId())) {
                openConsentGui(player);
            }
        }, 10L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Use /cannaconsent status <player> or /cannaconsent reset <player>.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String path = "players." + target.getUniqueId();
        if (args[0].equalsIgnoreCase("status")) {
            ConfigurationSection section = accepted.getConfigurationSection(path);
            if (section == null) {
                sender.sendMessage(ChatColor.YELLOW + args[1] + " has no consent record.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + section.getString("name", args[1])
                    + ChatColor.GRAY + " accepted=" + section.getBoolean("accepted", false)
                    + " declines=" + section.getInt("declines", 0));
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            accepted.set(path + ".accepted", false);
            accepted.set(path + ".reset-at", Instant.now().toString());
            saveAccepted();
            sender.sendMessage(ChatColor.GREEN + "Reset consent for " + args[1] + ".");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Use /cannaconsent status <player> or /cannaconsent reset <player>.");
        return true;
    }

    private void openConsentGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ", "");
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        gui.setItem(4, named(Material.LIME_DYE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "CannaSMP Notice",
                ChatColor.GRAY + "This server has cannabis/weed themes.",
                ChatColor.GRAY + "You may see smoking, tobacco, mature jokes,",
                ChatColor.GRAY + "swearing, and cannabis references.",
                ChatColor.YELLOW + "Confirm to acknowledge and play."));
        gui.setItem(DECLINE_SLOT, named(Material.RED_CONCRETE,
                ChatColor.RED + "" + ChatColor.BOLD + "Decline",
                ChatColor.GRAY + "Leave the server for now.",
                ChatColor.GRAY + "You can rejoin and accept later."));
        gui.setItem(13, named(Material.PAPER,
                ChatColor.WHITE + "" + ChatColor.BOLD + "Why confirm?",
                ChatColor.GRAY + "CannaSMP has cannabis-themed branding.",
                ChatColor.GRAY + "Confirming means you understand the server",
                ChatColor.GRAY + "may include weed-related references."));
        gui.setItem(CONFIRM_SLOT, named(Material.LIME_CONCRETE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm",
                ChatColor.GRAY + "I understand the CannaSMP theme.",
                ChatColor.GRAY + "Do not ask me again."));

        player.openInventory(gui);
    }

    private ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0 && !(lore.length == 1 && lore[0].isEmpty())) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void touch(Player player) {
        String path = path(player.getUniqueId());
        accepted.set(path + ".name", player.getName());
        accepted.set(path + ".last-seen", Instant.now().toString());
        if (!accepted.contains(path + ".first-seen")) {
            accepted.set(path + ".first-seen", Instant.now().toString());
            accepted.set(path + ".accepted", false);
            accepted.set(path + ".declines", 0);
        }
        saveAccepted();
    }

    private void accept(Player player) {
        String path = path(player.getUniqueId());
        accepted.set(path + ".name", player.getName());
        accepted.set(path + ".accepted", true);
        accepted.set(path + ".accepted-at", Instant.now().toString());
        accepted.set(path + ".last-seen", Instant.now().toString());
        saveAccepted();
    }

    private void decline(Player player) {
        UUID uuid = player.getUniqueId();
        String path = path(uuid);
        int declines = accepted.getInt(path + ".declines", 0) + 1;
        accepted.set(path + ".name", player.getName());
        accepted.set(path + ".accepted", false);
        accepted.set(path + ".declines", declines);
        accepted.set(path + ".last-declined-at", Instant.now().toString());
        saveAccepted();

        kicking.add(uuid);
        pending.remove(uuid);
        player.kickPlayer(ChatColor.GREEN + "CannaSMP Notice\n"
                + ChatColor.GRAY + "You declined the server notice.\n"
                + ChatColor.YELLOW + "Please rejoin and press Confirm to play.");
        Bukkit.getScheduler().runTaskLater(this, () -> kicking.remove(uuid), 20L);
    }

    private boolean hasAccepted(UUID uuid) {
        return accepted.getBoolean(path(uuid) + ".accepted", false);
    }

    private String path(UUID uuid) {
        return "players." + uuid;
    }

    private void saveAccepted() {
        try {
            accepted.save(acceptedFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save accepted.yml: " + exception.getMessage());
        }
    }
}
