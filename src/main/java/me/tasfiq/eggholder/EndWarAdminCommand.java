package me.tasfiq.eggholder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

public final class EndWarAdminCommand implements CommandExecutor, TabCompleter {

    private final EggHolderPlugin plugin;
    private final EndWarService endWarService;

    public EndWarAdminCommand(EggHolderPlugin plugin, EndWarService endWarService) {
        this.plugin = plugin;
        this.endWarService = endWarService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("eggholder.admin")) {
            plugin.getMessageManager().sendPrefixed(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            plugin.getMessageManager().sendPrefixed(sender, "endwar-usage", Map.of("%label%", label));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "start" -> {
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
            case "stop" -> {
                endWarService.stopGame();
                plugin.getMessageManager().sendPrefixed(sender, "game-stopped-admin");
                return true;
            }
            case "feature" -> {
                return handleFeatureCommand(sender, label, args);
            }
            case "audience" -> {
                return handleAudienceCommand(sender, label, args);
            }
            case "world" -> {
                return handleWorldCommand(sender, label, args);
            }
            default -> {
                plugin.getMessageManager().sendPrefixed(sender, "endwar-usage", Map.of("%label%", label));
                return true;
            }
        }
    }

    private boolean handleFeatureCommand(CommandSender sender, String label, String[] args) {
        if (args.length != 3) {
            plugin.getMessageManager().sendPrefixed(sender, "endwar-feature-usage", Map.of("%label%", label));
            return true;
        }

        String featureId = args[1];
        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "on", "off" -> {
                boolean enabled = action.equals("on");
                if (!endWarService.setFeature(featureId, enabled)) {
                    plugin.getMessageManager().sendPrefixed(sender, "endwar-feature-invalid", Map.of("%feature%", featureId));
                    return true;
                }
                Map<String, String> placeholders = Map.of(
                        "%feature%", featureId,
                        "%state%", enabled ? "enabled" : "disabled",
                        "%admin%", sender.getName()
                );
                plugin.getMessageManager().sendPrefixed(sender, "endwar-feature-set", placeholders);
                plugin.getMessageManager().broadcastPrefixed("endwar-feature-set-broadcast", placeholders);
                return true;
            }
            case "trigger" -> {
                if (!endWarService.triggerFeature(featureId)) {
                    plugin.getMessageManager().sendPrefixed(sender, "endwar-feature-trigger-failed", Map.of("%feature%", featureId));
                    return true;
                }
                Map<String, String> placeholders = Map.of("%feature%", featureId, "%admin%", sender.getName());
                plugin.getMessageManager().sendPrefixed(sender, "endwar-feature-triggered", placeholders);
                plugin.getMessageManager().broadcastPrefixed("endwar-feature-triggered-broadcast", placeholders);
                return true;
            }
            default -> {
                plugin.getMessageManager().sendPrefixed(sender, "endwar-feature-usage", Map.of("%label%", label));
                return true;
            }
        }
    }

    private boolean handleAudienceCommand(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            plugin.getMessageManager().sendPrefixed(sender, "endwar-audience-usage", Map.of("%label%", label));
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "open" -> {
                if (!endWarService.openAudienceVote()) {
                    plugin.getMessageManager().sendPrefixed(sender, "endwar-audience-open-failed");
                    return true;
                }
                plugin.getMessageManager().sendPrefixed(sender, "endwar-audience-opened");
                return true;
            }
            case "close" -> {
                endWarService.closeAudienceVote(false);
                plugin.getMessageManager().sendPrefixed(sender, "endwar-audience-closed");
                return true;
            }
            case "resolve" -> {
                endWarService.closeAudienceVote(true);
                plugin.getMessageManager().sendPrefixed(sender, "endwar-audience-resolved");
                return true;
            }
            default -> {
                plugin.getMessageManager().sendPrefixed(sender, "endwar-audience-usage", Map.of("%label%", label));
                return true;
            }
        }
    }

    private boolean handleWorldCommand(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            plugin.getMessageManager().sendPrefixed(sender, "endwar-world-usage", Map.of("%label%", label));
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "prepare" -> {
                if (endWarService.isGameRunning()) {
                    plugin.getMessageManager().sendPrefixed(sender, "endwar-world-locked");
                    return true;
                }
                if (!plugin.getManagedEndWorldService().prepareWorld(true)) {
                    plugin.getMessageManager().sendPrefixed(sender, "endwar-world-prepare-failed");
                    return true;
                }
                plugin.getMessageManager().sendPrefixed(
                        sender,
                        "endwar-world-prepared",
                        plugin.getManagedEndWorldService().createStatusPlaceholders()
                );
                return true;
            }
            case "status" -> {
                plugin.getMessageManager().sendPrefixed(
                        sender,
                        "endwar-world-status",
                        plugin.getManagedEndWorldService().createStatusPlaceholders()
                );
                return true;
            }
            default -> {
                plugin.getMessageManager().sendPrefixed(sender, "endwar-world-usage", Map.of("%label%", label));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("eggholder.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return partial(args[0], List.of("start", "stop", "feature", "audience", "world"));
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("feature")) {
                return partial(args[1], endWarService.getFeatureIds());
            }
            if (args[0].equalsIgnoreCase("audience")) {
                return partial(args[1], List.of("open", "close", "resolve"));
            }
            if (args[0].equalsIgnoreCase("world")) {
                return partial(args[1], List.of("prepare", "status"));
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("feature")) {
            return partial(args[2], List.of("on", "off", "trigger"));
        }

        return Collections.emptyList();
    }

    private List<String> partial(String token, Iterable<String> options) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, matches);
        Collections.sort(matches);
        return matches;
    }
}
