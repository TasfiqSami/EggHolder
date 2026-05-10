package me.tasfiq.eggholder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

public final class ReviveCommand implements CommandExecutor, TabCompleter {

    private final EggHolderPlugin plugin;
    private final DeadPlayerService deadPlayerService;

    public ReviveCommand(EggHolderPlugin plugin, DeadPlayerService deadPlayerService) {
        this.plugin = plugin;
        this.deadPlayerService = deadPlayerService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("eggholder.revive")) {
            plugin.getMessageManager().sendPrefixed(sender, "no-permission");
            return true;
        }

        if (args.length != 1) {
            plugin.getMessageManager().sendPrefixed(sender, "revive-usage", Map.of("%label%", label));
            return true;
        }

        if (plugin.getEndWarService() != null && !plugin.getEndWarService().areRevivesAllowed()) {
            plugin.getMessageManager().sendPrefixed(sender, "revive-disabled");
            return true;
        }

        String playerName = args[0];
        if (!deadPlayerService.reviveByName(playerName)) {
            plugin.getMessageManager().sendPrefixed(sender, "revive-not-dead", Map.of("%player%", playerName));
            return true;
        }

        plugin.getMessageManager().sendPrefixed(sender, "revive-success", Map.of("%player%", playerName));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("eggholder.revive")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], deadPlayerService.getKnownDeadNames(), matches);
            Collections.sort(matches);
            return matches;
        }

        return Collections.emptyList();
    }
}
