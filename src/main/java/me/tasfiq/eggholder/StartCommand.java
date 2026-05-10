package me.tasfiq.eggholder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class StartCommand implements CommandExecutor, TabCompleter {

    private final EggHolderPlugin plugin;
    private final EndWarService endWarService;

    public StartCommand(EggHolderPlugin plugin, EndWarService endWarService) {
        this.plugin = plugin;
        this.endWarService = endWarService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("eggholder.start")) {
            plugin.getMessageManager().sendPrefixed(sender, "no-permission");
            return true;
        }

        if (args.length != 0) {
            plugin.getMessageManager().sendPrefixed(sender, "start-usage", Map.of("%label%", label));
            return true;
        }

        if (endWarService.isGameRunning()) {
            plugin.getMessageManager().sendPrefixed(sender, "game-already-running");
            return true;
        }
        if (!endWarService.startGame()) {
            plugin.getMessageManager().sendPrefixed(sender, "game-start-failed");
            return true;
        }

        plugin.getMessageManager().sendPrefixed(sender, "game-started-admin");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
