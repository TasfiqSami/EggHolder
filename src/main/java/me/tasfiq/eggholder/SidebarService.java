package me.tasfiq.eggholder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public final class SidebarService implements Listener {

    private final EggHolderPlugin plugin;
    private final TeamService teamService;
    private final YamlResourceFile scoreboardFile;
    private final Scoreboard mainScoreboard;
    private final Map<UUID, Scoreboard> scoreboards = new LinkedHashMap<>();
    private final Map<UUID, List<String>> lastRenderedLines = new LinkedHashMap<>();

    private boolean enabled;
    private String title;
    private List<String> lines;

    public SidebarService(EggHolderPlugin plugin, TeamService teamService) {
        this.plugin = plugin;
        this.teamService = teamService;
        this.scoreboardFile = new YamlResourceFile(plugin, "scoreboard.yml");
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            throw new IllegalStateException("Bukkit scoreboard manager is not available.");
        }
        this.mainScoreboard = scoreboardManager.getMainScoreboard();
        reload();
    }

    public void reload() {
        scoreboardFile.load();
        FileConfiguration configuration = scoreboardFile.getConfiguration();
        this.enabled = configuration.getBoolean("enabled", true);
        this.title = configuration.getString("title", "&5&lEND WAR");
        this.lastRenderedLines.clear();
        this.lines = configuration.getStringList("lines");
        if (this.lines.isEmpty()) {
            this.lines = List.of(
                    "&7Phase: &f%phase%",
                    "&7Holder: &d%holder%",
                    "&7Kills: &f%kills%",
                    "&7Streak: &f%streak%",
                    "&7Team: %team_color%%team%",
                    "&7Team Kills: &f%team_kills%",
                    "&7M1: &f%member_1% &c%member_1_health%",
                    "&7M2: &f%member_2% &c%member_2_health%"
            );
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(mainScoreboard);
        }
        scoreboards.clear();
        lastRenderedLines.clear();
    }

    public void tick() {
        if (!enabled) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void refresh(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!enabled) {
            player.setScoreboard(mainScoreboard);
            scoreboards.remove(player.getUniqueId());
            lastRenderedLines.remove(player.getUniqueId());
            return;
        }

        List<String> rendered = renderLines(player);
        List<String> previous = lastRenderedLines.get(player.getUniqueId());
        Scoreboard scoreboard = scoreboards.computeIfAbsent(player.getUniqueId(), unused -> Bukkit.getScoreboardManager().getNewScoreboard());
        syncTeamsFromMain(scoreboard);
        Objective objective = scoreboard.getObjective("endwar");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("endwar", Criteria.DUMMY, TextUtil.component(title));
        }
        objective.displayName(TextUtil.component(title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        if (previous != null && previous.equals(rendered) && player.getScoreboard().equals(scoreboard)) {
            return;
        }

        for (String entry : new ArrayList<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        int score = rendered.size();
        for (String line : rendered) {
            objective.getScore(line).setScore(score--);
        }

        lastRenderedLines.put(player.getUniqueId(), rendered);
        player.setScoreboard(scoreboard);
    }

    private void syncTeamsFromMain(Scoreboard target) {
        if (target == null || target == mainScoreboard) {
            return;
        }

        Set<String> sourceNames = new LinkedHashSet<>();
        for (Team sourceTeam : mainScoreboard.getTeams()) {
            sourceNames.add(sourceTeam.getName());
            Team targetTeam = target.getTeam(sourceTeam.getName());
            if (targetTeam == null) {
                targetTeam = target.registerNewTeam(sourceTeam.getName());
            }

            targetTeam.displayName(sourceTeam.displayName());
            targetTeam.prefix(sourceTeam.prefix());
            targetTeam.suffix(sourceTeam.suffix());
            if (sourceTeam.color() instanceof net.kyori.adventure.text.format.NamedTextColor namedTextColor) {
                targetTeam.color(namedTextColor);
            }
            targetTeam.setAllowFriendlyFire(sourceTeam.allowFriendlyFire());
            targetTeam.setCanSeeFriendlyInvisibles(sourceTeam.canSeeFriendlyInvisibles());
            for (Team.Option option : Team.Option.values()) {
                targetTeam.setOption(option, sourceTeam.getOption(option));
            }

            for (String entry : new ArrayList<>(targetTeam.getEntries())) {
                if (!sourceTeam.hasEntry(entry)) {
                    targetTeam.removeEntry(entry);
                }
            }
            for (String entry : sourceTeam.getEntries()) {
                if (!targetTeam.hasEntry(entry)) {
                    targetTeam.addEntry(entry);
                }
            }
        }

        for (Team existingTeam : new ArrayList<>(target.getTeams())) {
            if (!sourceNames.contains(existingTeam.getName())) {
                existingTeam.unregister();
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scoreboards.remove(event.getPlayer().getUniqueId());
        lastRenderedLines.remove(event.getPlayer().getUniqueId());
    }

    private List<String> renderLines(Player player) {
        TeamService.EndWarTeam team = teamService.getTeam(player);
        EndWarService endWarService = plugin.getEndWarService();
        List<Player> members = team == null ? List.of(player) : teamService.getOnlineTeamMembers(player);

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("%phase%", endWarService == null ? "Lobby" : endWarService.getDisplayPhase());
        placeholders.put("%holder%", safeValue(plugin.getEggHolderService().getCurrentHolderName(), "None"));
        placeholders.put("%kills%", Integer.toString(endWarService == null ? 0 : endWarService.getKills(player)));
        placeholders.put("%streak%", Integer.toString(endWarService == null ? 0 : endWarService.getKillStreak(player)));
        placeholders.put("%team%", team == null ? "Solo" : team.name());
        placeholders.put("%team_color%", team == null ? "" : team.colorHex());
        placeholders.put("%team_kills%", Integer.toString(team == null ? 0 : team.kills()));
        placeholders.put("%members_count%", Integer.toString(members.size()));

        int maxMembers = Math.max(4, teamService.getMaxTeamSize());
        for (int index = 1; index <= maxMembers; index++) {
            Player member = index <= members.size() ? members.get(index - 1) : null;
            placeholders.put("%member_" + index + "%", member == null ? "-" : member.getName());
            placeholders.put("%member_" + index + "_health%", member == null ? "-" : Integer.toString((int) Math.ceil(member.getHealth())));
        }

        List<String> output = new ArrayList<>();
        int uniqueIndex = 0;
        for (String line : lines.subList(0, Math.min(lines.size(), 15))) {
            String rendered = TextUtil.format(line, placeholders);
            output.add(makeUnique(rendered, uniqueIndex++));
        }
        return output;
    }

    private String makeUnique(String rendered, int index) {
        String[] suffixes = {"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e"};
        String suffix = suffixes[Math.max(0, Math.min(index, suffixes.length - 1))];
        String line = rendered.length() > 38 ? rendered.substring(0, 38) : rendered;
        return line + suffix;
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
