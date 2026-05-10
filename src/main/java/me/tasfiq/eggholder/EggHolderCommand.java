package me.tasfiq.eggholder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public final class EggHolderCommand implements CommandExecutor, TabCompleter {

    private final EggHolderPlugin plugin;
    private final EggHolderService eggHolderService;
    private final DeadPlayerService deadPlayerService;

    public EggHolderCommand(EggHolderPlugin plugin, EggHolderService eggHolderService, DeadPlayerService deadPlayerService) {
        this.plugin = plugin;
        this.eggHolderService = eggHolderService;
        this.deadPlayerService = deadPlayerService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("eggholder.use")) {
            plugin.getMessageManager().sendPrefixed(sender, "no-permission");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            try {
                plugin.reloadPluginFiles();
                plugin.getMessageManager().sendPrefixed(sender, "reload-success");
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "Failed to reload EggHolder files.", exception);
                plugin.getMessageManager().sendPrefixed(sender, "reload-failed");
            }
            return true;
        }

        if (args.length != 2) {
            sendUsage(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        String playerName = args[1];

        if (subCommand.equals("add")) {
            Player target = getOnlinePlayer(playerName);
            if (target == null) {
                plugin.getMessageManager().sendPrefixed(sender, "player-must-be-online");
                return true;
            }

            if (deadPlayerService.isDead(target)) {
                plugin.getMessageManager().sendPrefixed(sender, "player-is-dead", Map.of("%player%", target.getName()));
                return true;
            }

            if (plugin.getPluginConfig().isCommandsRequireEgg() && !eggHolderService.hasEgg(target)) {
                plugin.getMessageManager().sendPrefixed(sender, "player-must-have-egg", Map.of("%player%", target.getName()));
                return true;
            }

            if (!eggHolderService.assignHolder(target, EggHolderService.AssignmentSource.COMMAND)) {
                plugin.getMessageManager().sendPrefixed(sender, "holder-already-active", Map.of("%player%", target.getName()));
                return true;
            }

            plugin.getMessageManager().sendPrefixed(sender, "holder-assigned", Map.of("%player%", target.getName()));
            return true;
        }

        if (subCommand.equals("remove")) {
            if (!eggHolderService.removeHolderByName(playerName)) {
                plugin.getMessageManager().sendPrefixed(sender, "no-active-holder", Map.of("%player%", playerName));
                return true;
            }

            plugin.getMessageManager().sendPrefixed(sender, "holder-removed", Map.of("%player%", playerName));
            return true;
        }

        sendUsage(sender, label);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("eggholder.use")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return partialMatches(args[0], List.of("add", "remove", "reload"));
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);
            if (subCommand.equals("add")) {
                return partialMatches(args[1], eggHolderService.getAssignableOnlineNames());
            }

            if (subCommand.equals("remove")) {
                return partialMatches(args[1], eggHolderService.getCurrentHolderNames());
            }
        }

        return Collections.emptyList();
    }

    private Player getOnlinePlayer(String playerName) {
        Player exact = Bukkit.getPlayerExact(playerName);
        if (exact != null) {
            return exact;
        }

        return Bukkit.getPlayer(playerName);
    }

    private List<String> partialMatches(String token, Iterable<String> options) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, matches);
        Collections.sort(matches);
        return matches;
    }

    private void sendUsage(CommandSender sender, String label) {
        plugin.getMessageManager().sendPrefixed(sender, "usage", Map.of("%label%", label));
    }
}
