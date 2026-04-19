package me.juusk.itemblacklist.command;


import me.juusk.itemblacklist.ItemBlacklist;
import me.juusk.itemblacklist.manager.BlacklistManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class ItemBlacklistCommand implements CommandExecutor, TabCompleter {

    private final ItemBlacklist plugin;
    private final BlacklistManager manager;

    public ItemBlacklistCommand(ItemBlacklist plugin, BlacklistManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!sender.hasPermission("itemblacklist.manage")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add"    -> handleAdd(sender, label, args);
            case "remove" -> handleRemove(sender, label, args);
            case "list"   -> handleList(sender);
            default       -> sendUsage(sender, label);
        }

        return true;
    }

    private void handleAdd(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can add items (you need to hold them).", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /" + label + " add <id>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1].toLowerCase(Locale.ROOT);

        if (!id.matches("[a-z0-9_\\-]+")) {
            sender.sendMessage(Component.text("ID may only contain letters, numbers, underscores and hyphens.", NamedTextColor.RED));
            return;
        }

        if (manager.contains(id)) {
            sender.sendMessage(Component.text("An entry with the id '" + id + "' already exists. Remove it first.", NamedTextColor.RED));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            sender.sendMessage(Component.text("Hold the item you want to blacklist in your main hand.", NamedTextColor.RED));
            return;
        }

        manager.add(id, hand);

        sender.sendMessage(Component.text()
            .append(Component.text("[ItemBlacklist] ", NamedTextColor.GOLD))
            .append(Component.text("Added ", NamedTextColor.GREEN))
            .append(Component.text(hand.getType().name(), NamedTextColor.WHITE))
            .append(Component.text(" to the blacklist as '", NamedTextColor.GREEN))
            .append(Component.text(id, NamedTextColor.AQUA))
            .append(Component.text("'.", NamedTextColor.GREEN))
            .build());

        if (this.plugin.blacklistManager != null) {
            this.plugin.blacklistManager.save();
        }
        plugin.getLogger().info(sender.getName() + " added item '" + id + "' (" + hand.getType() + ") to the blacklist.");
    }

    private void handleRemove(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /" + label + " remove <id>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1].toLowerCase(Locale.ROOT);

        if (!manager.remove(id)) {
            sender.sendMessage(Component.text("No blacklist entry found with id '" + id + "'.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text()
            .append(Component.text("[ItemBlacklist] ", NamedTextColor.DARK_RED))
            .append(Component.text("Removed '", NamedTextColor.GREEN))
            .append(Component.text(id, NamedTextColor.AQUA))
            .append(Component.text("' from the blacklist.", NamedTextColor.GREEN))
            .build());

        if (this.plugin.blacklistManager != null) {
            this.plugin.blacklistManager.save();
        }

        plugin.getLogger().info(sender.getName() + " removed item '" + id + "' from the blacklist.");
    }

    private void handleList(CommandSender sender) {
        Map<String, ItemStack> blacklist = manager.getBlacklist();

        if (blacklist.isEmpty()) {
            sender.sendMessage(Component.text("[ItemBlacklist] The blacklist is empty.", NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("=== Item Blacklist (" + blacklist.size() + " entries) ===", NamedTextColor.GOLD));

        blacklist.forEach((id, item) -> {
            String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : item.getType().name();

            sender.sendMessage(Component.text()
                .append(Component.text("  • ", NamedTextColor.GRAY))
                .append(Component.text(id, NamedTextColor.AQUA))
                .append(Component.text(" → ", NamedTextColor.GRAY))
                .append(Component.text(displayName, NamedTextColor.WHITE))
                .build());
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                     @NotNull Command command,
                                     @NotNull String alias,
                                     @NotNull String[] args) {
        if (!sender.hasPermission("itemblacklist.manage")) return List.of();

        if (args.length == 1) {
            return Stream.of("add", "remove", "list")
                .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return manager.getIds().stream()
                .filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT)))
                .toList();
        }

        return List.of();
    }


    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text("=== ItemBlacklist Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/" + label + " add <id>", NamedTextColor.YELLOW)
            .append(Component.text(" - Blacklist the item in your hand", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " remove <id>", NamedTextColor.YELLOW)
            .append(Component.text(" - Remove a blacklist entry by id", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " list", NamedTextColor.YELLOW)
            .append(Component.text(" - List all blacklisted items", NamedTextColor.GRAY)));
    }
}
