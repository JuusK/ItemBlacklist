package me.juusk.itemblacklist.listener;

import me.juusk.itemblacklist.ItemBlacklist;
import me.juusk.itemblacklist.manager.BlacklistManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    private final BlacklistManager manager;
    private final ItemBlacklist plugin;

    public InventoryListener(ItemBlacklist plugin, BlacklistManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.hasPermission("itemblacklist.bypass")) return;

        Item itemEntity = event.getItem();
        if (manager.isBlacklisted(itemEntity.getItemStack())) {
            event.setCancelled(true);
            itemEntity.remove();
            player.sendMessage(Component.text("[ItemBlacklist] A blacklisted item was destroyed.", NamedTextColor.RED));
        }
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.hasPermission("itemblacklist.bypass")) return;

        ItemStack cursor  = event.getCursor();
        ItemStack current = event.getCurrentItem();

        boolean blacklistedCursor  = manager.isBlacklisted(cursor);
        boolean blacklistedCurrent = manager.isBlacklisted(current);

        if (blacklistedCursor || blacklistedCurrent) {
            event.setCancelled(true);

            if (blacklistedCurrent) {
                event.setCurrentItem(null);
            }
            if (blacklistedCursor) {
                player.setItemOnCursor(null);
            }

            player.sendMessage(Component.text("[ItemBlacklist] A blacklisted item was removed.", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().getServer().getScheduler().runTaskLater(
            plugin,
            () -> manager.scanAndRemove(event.getPlayer()),
            5L
        );
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (event.getPlayer().hasPermission("itemblacklist.bypass")) return;

        if (manager.isBlacklisted(event.getMainHandItem()) ||
            manager.isBlacklisted(event.getOffHandItem())) {
            event.setCancelled(true);
            manager.scanAndRemove(event.getPlayer());
        }
    }
}
