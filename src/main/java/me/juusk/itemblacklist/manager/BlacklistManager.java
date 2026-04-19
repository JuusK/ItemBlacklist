package me.juusk.itemblacklist.manager;

import me.juusk.itemblacklist.ItemBlacklist;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class BlacklistManager {

    private final ItemBlacklist plugin;
    private final File dataFile;

    /** id -> serialized ItemStack */
    private final Map<String, ItemStack> blacklist = new LinkedHashMap<>();

    public BlacklistManager(ItemBlacklist plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "blacklist.yml");
    }

    public void load() {
        blacklist.clear();
        if (!dataFile.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        var section = cfg.getConfigurationSection("items");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            String base64 = section.getString(id);
            if (base64 == null) continue;
            try {
                byte[] bytes = Base64.getDecoder().decode(base64);
                ItemStack item = ItemStack.deserializeBytes(bytes);
                blacklist.put(id.toLowerCase(), item);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load blacklisted item with id '" + id + "'", e);
            }
        }

        plugin.getLogger().info("Loaded " + blacklist.size() + " blacklisted item(s) from blacklist.yml");
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();

        for (Map.Entry<String, ItemStack> entry : blacklist.entrySet()) {
            try {
                byte[] bytes = entry.getValue().serializeAsBytes();
                cfg.set("items." + entry.getKey(), Base64.getEncoder().encodeToString(bytes));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save blacklisted item with id '" + entry.getKey() + "'", e);
            }
        }

        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save blacklist.yml", e);
        }
    }


    /**
     * Adds an item to the blacklist.
     *
     * @return false if the id is already taken
     */
    public boolean add(String id, ItemStack item) {
        id = id.toLowerCase();
        if (blacklist.containsKey(id)) return false;
        ItemStack copy = item.clone();
        copy.setAmount(1);
        blacklist.put(id, copy);
        save();
        return true;
    }

    /**
     * Removes an entry by id.
     *
     * @return false if the id didn't exist
     */
    public boolean remove(String id) {
        id = id.toLowerCase();
        if (!blacklist.containsKey(id)) return false;
        blacklist.remove(id);
        save();
        return true;
    }

    public boolean contains(String id) {
        return blacklist.containsKey(id.toLowerCase());
    }

    public Map<String, ItemStack> getBlacklist() {
        return Collections.unmodifiableMap(blacklist);
    }

    public List<String> getIds() {
        return List.copyOf(blacklist.keySet());
    }


    /**
     * Returns true if the given ItemStack matches any blacklisted entry.
     */
    public boolean isBlacklisted(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        for (ItemStack ref : blacklist.values()) {
            if (item.isSimilar(ref)) return true;
        }
        return false;
    }

    /**
     * Scans a player's full inventory and removes all blacklisted items.
     * Does nothing if the player has the bypass permission.
     */
    public void scanAndRemove(Player player) {
        if (player.hasPermission("itemblacklist.bypass")) return;

        boolean removed = false;
        var inv = player.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack slot = inv.getItem(i);
            if (isBlacklisted(slot)) {
                inv.setItem(i, null);
                removed = true;
            }
        }

        if (isBlacklisted(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
            removed = true;
        }

        if (removed) {
            player.sendMessage(org.bukkit.ChatColor.RED + "[ItemBlacklist] A blacklisted item was removed from your inventory.");
        }
    }
}
