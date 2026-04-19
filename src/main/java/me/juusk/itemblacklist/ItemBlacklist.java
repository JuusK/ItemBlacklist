package me.juusk.itemblacklist;


import me.juusk.itemblacklist.command.ItemBlacklistCommand;
import me.juusk.itemblacklist.listener.InventoryListener;
import me.juusk.itemblacklist.manager.BlacklistManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemBlacklist extends JavaPlugin {

    public BlacklistManager blacklistManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        blacklistManager = new BlacklistManager(this);
        blacklistManager.load();

        ItemBlacklistCommand command = new ItemBlacklistCommand(this, blacklistManager);
        var cmd = getCommand("itemblacklist");
        if (cmd != null) {
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }

        Bukkit.getPluginManager().registerEvents(new InventoryListener(this, blacklistManager), this);

        Bukkit.getScheduler().runTaskTimer(this, () ->
            Bukkit.getOnlinePlayers().forEach(blacklistManager::scanAndRemove),
            20L, 20L
        );

        getLogger().info("ItemBlacklist enabled! Loaded " + blacklistManager.getBlacklist().size() + " blacklisted item(s).");
    }

    @Override
    public void onDisable() {
        if (blacklistManager != null) {
            blacklistManager.save();
        }
        getLogger().info("ItemBlacklist disabled.");
    }
}
