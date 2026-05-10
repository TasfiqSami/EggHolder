package me.tasfiq.eggholder;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class TeamCommand implements CommandExecutor, TabCompleter {

    private final EggHolderPlugin plugin;
    private final TeamService teamService;

    public TeamCommand(EggHolderPlugin plugin, TeamService teamService) {
        this.plugin = plugin;
        this.teamService = teamService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            teamService.openMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        boolean teamLocked = plugin.getEndWarService() != null && plugin.getEndWarService().isGameRunning();

        if (subCommand.equals("create")) {
            if (teamLocked) {
                plugin.getMessageManager().sendPrefixed(player, "team-locked");
                return true;
            }
            String requestedName = args.length > 1
                    ? List.of(args).subList(1, args.length).stream().collect(Collectors.joining(" "))
                    : null;
            if (!teamService.createTeam(player, requestedName)) {
                plugin.getMessageManager().sendPrefixed(player, "team-create-failed");
                return true;
            }

            plugin.getMessageManager().sendPrefixed(player, "team-created", Map.of("%team%", teamService.getTeam(player).name()));
            return true;
        }

        if (subCommand.equals("rename")) {
            if (teamLocked) {
                plugin.getMessageManager().sendPrefixed(player, "team-locked");
                return true;
            }
            if (args.length < 2) {
                plugin.getMessageManager().sendPrefixed(player, "team-rename-help");
                return true;
            }

            String requestedName = List.of(args).subList(1, args.length).stream().collect(Collectors.joining(" "));
            if (!teamService.renameTeam(player, requestedName)) {
                plugin.getMessageManager().sendPrefixed(player, "team-rename-failed");
                return true;
            }

            TeamService.EndWarTeam team = teamService.getTeam(player);
            plugin.getMessageManager().sendPrefixed(player, "team-renamed", Map.of("%team%", team == null ? requestedName : team.name()));
            return true;
        }

        if (subCommand.equals("invite")) {
            if (teamLocked) {
                plugin.getMessageManager().sendPrefixed(player, "team-locked");
                return true;
            }
            if (args.length != 2) {
                plugin.getMessageManager().sendPrefixed(player, "team-usage");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                plugin.getMessageManager().sendPrefixed(player, "player-must-be-online");
                return true;
            }

            if (!teamService.invitePlayer(player, target)) {
                plugin.getMessageManager().sendPrefixed(player, "team-invite-failed");
                return true;
            }

            plugin.getMessageManager().sendPrefixed(player, "team-invite-sent", Map.of("%player%", target.getName()));
            plugin.getMessageManager().sendPrefixed(target, "team-invite-received", Map.of("%player%", player.getName(), "%team%", teamService.getTeam(player).name()));
            return true;
        }

        if (subCommand.equals("accept")) {
            if (teamLocked) {
                plugin.getMessageManager().sendPrefixed(player, "team-locked");
                return true;
            }
            if (args.length != 2) {
                plugin.getMessageManager().sendPrefixed(player, "team-usage");
                return true;
            }

            if (!teamService.acceptInvite(player, args[1])) {
                plugin.getMessageManager().sendPrefixed(player, "team-join-failed");
                return true;
            }

            TeamService.EndWarTeam team = teamService.getTeam(player);
            plugin.getMessageManager().sendPrefixed(player, "team-joined", Map.of("%team%", team == null ? "Team" : team.name()));
            return true;
        }

        if (subCommand.equals("leave")) {
            if (teamLocked) {
                plugin.getMessageManager().sendPrefixed(player, "team-locked");
                return true;
            }
            if (!teamService.leaveTeam(player)) {
                plugin.getMessageManager().sendPrefixed(player, "team-not-in-team");
                return true;
            }
            plugin.getMessageManager().sendPrefixed(player, "team-left");
            return true;
        }

        if (subCommand.equals("kick")) {
            if (teamLocked) {
                plugin.getMessageManager().sendPrefixed(player, "team-locked");
                return true;
            }
            if (args.length != 2) {
                plugin.getMessageManager().sendPrefixed(player, "team-usage");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                plugin.getMessageManager().sendPrefixed(player, "player-must-be-online");
                return true;
            }

            if (!teamService.kickMember(player, target)) {
                plugin.getMessageManager().sendPrefixed(player, "team-kick-failed");
                return true;
            }

            plugin.getMessageManager().sendPrefixed(player, "team-kick-success", Map.of("%player%", target.getName()));
            plugin.getMessageManager().sendPrefixed(target, "team-kicked", Map.of("%player%", player.getName()));
            return true;
        }

        if (subCommand.equals("disband")) {
            if (teamLocked) {
                plugin.getMessageManager().sendPrefixed(player, "team-locked");
                return true;
            }
            TeamService.EndWarTeam team = teamService.getTeam(player);
            String teamName = team == null ? "Team" : team.name();
            if (!teamService.disbandTeam(player)) {
                plugin.getMessageManager().sendPrefixed(player, "team-disband-failed");
                return true;
            }

            plugin.getMessageManager().sendPrefixed(player, "team-disbanded", Map.of("%team%", teamName));
            return true;
        }

        plugin.getMessageManager().sendPrefixed(player, "team-usage");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return teamService.tabCompleteSubCommand(sender, args);
    }
}
