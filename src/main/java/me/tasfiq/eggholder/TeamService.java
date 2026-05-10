package me.tasfiq.eggholder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.StringUtil;

public final class TeamService implements Listener {

    private final EggHolderPlugin plugin;
    private final Scoreboard scoreboard;
    private final NamespacedKey teamMenuItemKey;
    private final NamespacedKey teamMenuActionKey;
    private final Map<UUID, EndWarTeam> teams = new LinkedHashMap<>();
    private final Map<UUID, UUID> playerTeams = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> pendingInvites = new LinkedHashMap<>();

    private int maxTeamSize;
    private Material teamMenuMaterial;
    private String teamMenuName;
    private List<String> teamMenuLore;
    private int teamMenuSlot;
    private List<String> teamColorPalette;
    private boolean autoNameEnabled;
    private String autoNameFormat;
    private List<String> autoNameAdjectives;
    private List<String> autoNameNouns;
    private String teamPrefixFormat;
    private String teamListFormat;
    private String soloTeamNameFormat;

    public TeamService(EggHolderPlugin plugin) {
        this.plugin = plugin;
        this.teamMenuItemKey = new NamespacedKey(plugin, "team_menu_item");
        this.teamMenuActionKey = new NamespacedKey(plugin, "team_menu_action");

        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            throw new IllegalStateException("Bukkit scoreboard manager is not available.");
        }
        this.scoreboard = scoreboardManager.getMainScoreboard();
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        this.maxTeamSize = Math.max(1, config.getInt("teams.max-size", 4));
        this.teamMenuMaterial = parseMaterial(config.getString("menus.team.material"), Material.NETHER_STAR);
        this.teamMenuName = config.getString("menus.team.name", "&b&lTeam Menu");
        this.teamMenuLore = config.getStringList("menus.team.lore");
        this.teamMenuSlot = clamp(config.getInt("menus.team.slot", 7), 0, 8);
        this.teamColorPalette = new ArrayList<>(config.getStringList("teams.colors"));
        if (teamColorPalette.isEmpty()) {
            teamColorPalette = List.of("#FF6B9D", "#5AD1FF", "#C4FF4D", "#FFC857", "#B76EFF", "#7AE582", "#FF8FAB", "#A29BFE");
        }
        this.autoNameEnabled = config.getBoolean("teams.auto-names.enabled", true);
        this.autoNameFormat = config.getString("teams.auto-names.format", "%adjective% %noun%");
        this.autoNameAdjectives = new ArrayList<>(config.getStringList("teams.auto-names.adjectives"));
        if (autoNameAdjectives.isEmpty()) {
            autoNameAdjectives = List.of("Crimson", "Void", "Iron", "Shadow", "Solar", "Frost", "Storm", "Ember", "Ivory", "Night");
        }
        this.autoNameNouns = new ArrayList<>(config.getStringList("teams.auto-names.nouns"));
        if (autoNameNouns.isEmpty()) {
            autoNameNouns = List.of("Raiders", "Guard", "Reapers", "Dragons", "Hunters", "Titans", "Vanguard", "Phantoms", "Legion", "Sentinels");
        }
        this.teamPrefixFormat = config.getString("teams.prefix-format", "%color%[%team%] &r");
        this.teamListFormat = config.getString("teams.list-format", "%prefix%%player_color%%player%");
        this.soloTeamNameFormat = config.getString("teams.solo-name-format", "%player% Team");

        for (EndWarTeam team : teams.values()) {
            applyScoreboardTeam(team);
        }
        refreshAllPlayerDisplays();
    }

    public void shutdown() {
        for (EndWarTeam team : new ArrayList<>(teams.values())) {
            for (UUID memberId : team.members()) {
                unregisterPlayerScoreboardTeam(memberId);
            }
        }
        teams.clear();
        playerTeams.clear();
        pendingInvites.clear();
    }

    public int getMaxTeamSize() {
        return maxTeamSize;
    }

    public void giveMenuItem(Player player) {
        ItemStack menuItem = createTeamMenuItem();
        ItemStack existing = player.getInventory().getItem(teamMenuSlot);
        if (existing == null || existing.getType() == Material.AIR || isTeamMenuItem(existing)) {
            player.getInventory().setItem(teamMenuSlot, menuItem);
            return;
        }

        if (!player.getInventory().containsAtLeast(menuItem, 1)) {
            player.getInventory().addItem(menuItem);
        }
    }

    public void removeMenuItem(Player player) {
        if (player == null) {
            return;
        }
        removeMenuItems(player.getInventory());
    }

    public void ensureSoloTeamsForUngroupedPlayers(Collection<? extends Player> players) {
        for (Player player : players) {
            if (player != null && player.isOnline() && !playerTeams.containsKey(player.getUniqueId())) {
                createTeam(player, null);
            }
        }
    }

    public boolean isOnSameTeam(Player first, Player second) {
        if (first == null || second == null) {
            return false;
        }

        UUID firstTeamId = playerTeams.get(first.getUniqueId());
        UUID secondTeamId = playerTeams.get(second.getUniqueId());
        return firstTeamId != null && firstTeamId.equals(secondTeamId);
    }

    public EndWarTeam getTeam(Player player) {
        if (player == null) {
            return null;
        }
        UUID teamId = playerTeams.get(player.getUniqueId());
        return teamId == null ? null : teams.get(teamId);
    }

    public EndWarTeam getTeam(UUID teamId) {
        return teamId == null ? null : teams.get(teamId);
    }

    public Collection<EndWarTeam> getTeams() {
        return Collections.unmodifiableCollection(teams.values());
    }

    public UUID getTeamId(Player player) {
        return player == null ? null : playerTeams.get(player.getUniqueId());
    }

    public int getTeamKills(Player player) {
        EndWarTeam team = getTeam(player);
        return team == null ? 0 : team.kills();
    }

    public void resetMatchStats() {
        for (EndWarTeam team : teams.values()) {
            team.resetKills();
        }
    }

    public void incrementTeamKill(Player player) {
        EndWarTeam team = getTeam(player);
        if (team != null) {
            team.incrementKills();
        }
    }

    public List<Player> getOnlineTeamMembers(Player player) {
        EndWarTeam team = getTeam(player);
        if (team == null) {
            return List.of(player);
        }

        List<Player> members = new ArrayList<>();
        for (UUID memberId : team.members()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                members.add(member);
            }
        }
        members.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        return members;
    }

    public boolean createTeam(Player leader, String requestedName) {
        if (leader == null || playerTeams.containsKey(leader.getUniqueId())) {
            return false;
        }

        String name = resolveTeamName(requestedName, null, leader.getName());

        UUID teamId = UUID.randomUUID();
        EndWarTeam team = new EndWarTeam(
                teamId,
                name,
                pickNextColor()
        );
        team.setLeader(leader.getUniqueId());
        team.members().add(leader.getUniqueId());
        teams.put(teamId, team);
        playerTeams.put(leader.getUniqueId(), teamId);
        applyScoreboardTeam(team);
        addPlayerToScoreboardTeam(leader, team);
        refreshTeamDisplays(team);
        return true;
    }

    public boolean invitePlayer(Player leader, Player target) {
        EndWarTeam team = getTeam(leader);
        if (team == null || target == null || leader.getUniqueId().equals(target.getUniqueId())) {
            return false;
        }
        if (!isLeader(leader, team) || playerTeams.containsKey(target.getUniqueId()) || team.members().size() >= maxTeamSize) {
            return false;
        }

        pendingInvites.computeIfAbsent(target.getUniqueId(), unused -> new LinkedHashSet<>()).add(team.id());
        return true;
    }

    public boolean acceptInvite(Player player, String inviterOrTeamName) {
        Set<UUID> invites = pendingInvites.get(player.getUniqueId());
        if (invites == null || invites.isEmpty() || playerTeams.containsKey(player.getUniqueId())) {
            return false;
        }

        EndWarTeam selectedTeam = null;
        for (UUID teamId : invites) {
            EndWarTeam team = teams.get(teamId);
            if (team == null) {
                continue;
            }

            Player leader = Bukkit.getPlayer(team.leader());
            String leaderName = leader != null ? leader.getName() : "";
            if (team.name().equalsIgnoreCase(inviterOrTeamName) || leaderName.equalsIgnoreCase(inviterOrTeamName)) {
                selectedTeam = team;
                break;
            }
        }

        if (selectedTeam == null || selectedTeam.members().size() >= maxTeamSize) {
            return false;
        }

        selectedTeam.members().add(player.getUniqueId());
        playerTeams.put(player.getUniqueId(), selectedTeam.id());
        addPlayerToScoreboardTeam(player, selectedTeam);
        invites.clear();
        pendingInvites.remove(player.getUniqueId());
        cleanupEmptyInvites();
        refreshTeamDisplays(selectedTeam);
        return true;
    }

    public boolean leaveTeam(Player player) {
        EndWarTeam team = getTeam(player);
        if (team == null) {
            return false;
        }

        removePlayerFromTeam(player, team);
        return true;
    }

    public boolean kickMember(Player leader, Player target) {
        EndWarTeam team = getTeam(leader);
        if (team == null || target == null || !isLeader(leader, team) || target.getUniqueId().equals(leader.getUniqueId())) {
            return false;
        }

        EndWarTeam targetTeam = getTeam(target);
        if (targetTeam == null || !targetTeam.id().equals(team.id())) {
            return false;
        }

        removePlayerFromTeam(target, team);
        return true;
    }

    public boolean disbandTeam(Player player) {
        EndWarTeam team = getTeam(player);
        if (team == null || !isLeader(player, team)) {
            return false;
        }

        for (UUID memberId : new ArrayList<>(team.members())) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                applyPlayerDisplay(member);
            }
            unregisterPlayerScoreboardTeam(memberId);
            playerTeams.remove(memberId);
        }

        pendingInvites.values().forEach(invites -> invites.remove(team.id()));
        cleanupEmptyInvites();
        teams.remove(team.id());
        return true;
    }

    public boolean renameTeam(Player player, String requestedName) {
        EndWarTeam team = getTeam(player);
        if (team == null || !isLeader(player, team)) {
            return false;
        }

        String name = resolveTeamName(requestedName, team.id(), player.getName());
        if (name.isBlank()) {
            return false;
        }

        team.setName(name);
        applyScoreboardTeam(team);
        refreshTeamDisplays(team);
        return true;
    }

    public List<String> getInviterNames(Player player) {
        Set<UUID> invites = pendingInvites.get(player.getUniqueId());
        if (invites == null) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (UUID teamId : invites) {
            EndWarTeam team = teams.get(teamId);
            if (team != null) {
                Player leader = Bukkit.getPlayer(team.leader());
                names.add(leader != null ? leader.getName() : team.name());
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public void openMenu(Player player) {
        player.openInventory(createMainMenu(player));
    }

    public List<String> getInvitablePlayerNames(Player requester) {
        EndWarTeam team = getTeam(requester);
        if (team == null || !isLeader(requester, team) || team.members().size() >= maxTeamSize) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(requester) || playerTeams.containsKey(online.getUniqueId())) {
                continue;
            }
            names.add(online.getName());
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public List<String> getKickablePlayerNames(Player requester) {
        EndWarTeam team = getTeam(requester);
        if (team == null || !isLeader(requester, team)) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (UUID memberId : team.members()) {
            if (memberId.equals(requester.getUniqueId())) {
                continue;
            }
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                names.add(member.getName());
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private boolean isLeader(Player player, EndWarTeam team) {
        return player != null && team != null && team.leader().equals(player.getUniqueId());
    }

    private void removePlayerFromTeam(Player player, EndWarTeam team) {
        if (player == null || team == null) {
            return;
        }

        team.members().remove(player.getUniqueId());
        playerTeams.remove(player.getUniqueId());
        unregisterPlayerScoreboardTeam(player.getUniqueId());

        if (team.members().isEmpty()) {
            disbandEmptyTeam(team);
            applyPlayerDisplay(player);
            return;
        }

        if (team.leader().equals(player.getUniqueId())) {
            team.setLeader(team.members().iterator().next());
        }
        refreshTeamDisplays(team);
        applyPlayerDisplay(player);
    }

    private void disbandEmptyTeam(EndWarTeam team) {
        pendingInvites.values().forEach(invites -> invites.remove(team.id()));
        cleanupEmptyInvites();
        for (UUID memberId : team.members()) {
            unregisterPlayerScoreboardTeam(memberId);
        }
        teams.remove(team.id());
    }

    private void applyScoreboardTeam(EndWarTeam team) {
        for (UUID memberId : team.members()) {
            applyPlayerScoreboardTeam(memberId, team);
        }
    }

    private void addPlayerToScoreboardTeam(Player player, EndWarTeam team) {
        if (player == null || team == null) {
            return;
        }
        applyPlayerScoreboardTeam(player.getUniqueId(), team);
    }

    private void unregisterPlayerScoreboardTeam(UUID playerId) {
        Team scoreboardTeam = scoreboard.getTeam(buildPlayerScoreboardId(playerId));
        if (scoreboardTeam != null) {
            scoreboardTeam.unregister();
        }
    }

    private void removeMenuItems(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isTeamMenuItem(inventory.getItem(slot))) {
                inventory.setItem(slot, null);
            }
        }
    }

    private void refreshTeamDisplays(EndWarTeam team) {
        if (team == null) {
            return;
        }
        for (UUID memberId : team.members()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                applyPlayerDisplay(member);
            }
        }
    }

    private void refreshAllPlayerDisplays() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPlayerDisplay(player);
        }
    }

    private void applyPlayerDisplay(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (plugin.getDeadPlayerService().isDead(player) || plugin.getEggHolderService().isHolder(player)) {
            return;
        }

        EndWarTeam team = getTeam(player);
        if (team == null) {
            TextUtil.setPlayerListName(player, player.getName());
            return;
        }

        TextUtil.setPlayerListName(player, formatPlayerListName(team, player));
    }

    private String colorForPlayer(UUID uuid) {
        int hash = Math.abs(uuid.hashCode());
        int red = 96 + (hash & 0x3F);
        int green = 96 + ((hash >> 6) & 0x3F);
        int blue = 96 + ((hash >> 12) & 0x3F);
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    private Inventory createMainMenu(Player viewer) {
        EndWarTeam team = getTeam(viewer);
        Inventory inventory = Bukkit.createInventory(new TeamMenuHolder(viewer.getUniqueId(), TeamMenuPage.MAIN), 27, TextUtil.component("&8Team Menu"));

        if (team == null) {
            inventory.setItem(11, createActionItem(Material.LIME_WOOL, "&a&lCreate Team", List.of("&7Create a team instantly.", "&7A unique team name is generated automatically."), "team:create"));
            inventory.setItem(13, createActionItem(Material.PLAYER_HEAD, "&b&lInvites", List.of("&7View incoming team invites."), "team:invites"));
            inventory.setItem(15, createActionItem(Material.BOOK, "&e&lInfo", List.of("&7Use /team create [name]", "&7or click create to make one now.", "&7Team leaders can rename later with", "&f/team rename <name>"), "team:none"));
            return inventory;
        }

        inventory.setItem(10, createMembersItem(viewer, team));
        inventory.setItem(12, createActionItem(Material.WRITABLE_BOOK, "&d&lInvite Players", List.of("&7Invite online players to your team."), "team:invite-menu"));
        inventory.setItem(13, createActionItem(Material.NAME_TAG, "&e&lRename Team", List.of("&7Use &f/team rename <name>", isLeader(viewer, team) ? "&aYou are the leader." : "&cOnly the leader can rename."), "team:rename-help"));
        inventory.setItem(14, createActionItem(Material.PLAYER_HEAD, "&b&lPending Invites", List.of("&7View invites sent to you."), "team:invites"));
        inventory.setItem(16, createActionItem(
                isLeader(viewer, team) ? Material.BARRIER : Material.OAK_DOOR,
                isLeader(viewer, team) ? "&c&lDisband Team" : "&c&lLeave Team",
                List.of(isLeader(viewer, team) ? "&7Disband the entire team." : "&7Leave your current team."),
                isLeader(viewer, team) ? "team:disband" : "team:leave"
        ));
        return inventory;
    }

    private Inventory createInvitesMenu(Player viewer) {
        Inventory inventory = Bukkit.createInventory(new TeamMenuHolder(viewer.getUniqueId(), TeamMenuPage.INVITES), 27, TextUtil.component("&8Team Invites"));
        Set<UUID> invites = pendingInvites.getOrDefault(viewer.getUniqueId(), Collections.emptySet());
        int slot = 0;
        for (UUID teamId : invites) {
            if (slot >= inventory.getSize()) {
                break;
            }
            EndWarTeam team = teams.get(teamId);
            if (team == null) {
                continue;
            }

            Player leader = Bukkit.getPlayer(team.leader());
            String leaderName = leader != null ? leader.getName() : "Unknown";
            inventory.setItem(slot++, createHeadAction(
                    leader,
                    "&a&lJoin " + team.name(),
                    List.of("&7Leader: &f" + leaderName, "&7Members: &f" + team.members().size() + "/" + maxTeamSize, "&eClick to accept."),
                    "team:accept:" + team.id()
            ));
        }
        inventory.setItem(26, createActionItem(Material.ARROW, "&7Back", List.of("&7Return to the main team menu."), "team:back"));
        return inventory;
    }

    private Inventory createInvitePlayersMenu(Player viewer) {
        Inventory inventory = Bukkit.createInventory(new TeamMenuHolder(viewer.getUniqueId(), TeamMenuPage.INVITE_PLAYERS), 54, TextUtil.component("&8Invite Players"));
        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (slot >= inventory.getSize() - 1 || target.equals(viewer) || playerTeams.containsKey(target.getUniqueId())) {
                continue;
            }
            inventory.setItem(slot++, createHeadAction(
                    target,
                    "&dInvite " + target.getName(),
                    List.of("&7Click to invite this player."), "team:invite:" + target.getUniqueId()
            ));
        }
        inventory.setItem(53, createActionItem(Material.ARROW, "&7Back", List.of("&7Return to the main team menu."), "team:back"));
        return inventory;
    }

    private ItemStack createMembersItem(Player viewer, EndWarTeam team) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Team: &f" + team.name());
        lore.add("&7Members:");
        for (UUID memberId : team.members()) {
            Player member = Bukkit.getPlayer(memberId);
            String name = member != null ? member.getName() : "Offline";
            lore.add("&f- " + name + (memberId.equals(team.leader()) ? " &7(Leader)" : ""));
        }
        lore.add("&7Total Kills: &f" + team.kills());
        lore.add(isLeader(viewer, team) ? "&7Rename with: &f/team rename <name>" : "&7Only the leader can rename the team.");
        return createActionItem(Material.NAME_TAG, "&6&lTeam Members", lore, "team:none");
    }

    private ItemStack createActionItem(Material material, String name, List<String> lore, String action) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            TextUtil.applyItemText(meta, name, lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(teamMenuActionKey, PersistentDataType.STRING, action);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack createHeadAction(Player player, String name, List<String> lore, String action) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        if (meta != null) {
            if (player != null) {
                meta.setOwningPlayer(player);
            }
            TextUtil.applyItemText(meta, name, lore);
            meta.getPersistentDataContainer().set(teamMenuActionKey, PersistentDataType.STRING, action);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack createTeamMenuItem() {
        ItemStack stack = new ItemStack(teamMenuMaterial);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            TextUtil.applyItemText(meta, teamMenuName, teamMenuLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(teamMenuItemKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public boolean isTeamMenuItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        Byte marker = stack.getItemMeta().getPersistentDataContainer().get(teamMenuItemKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    @EventHandler
    public void onTeamItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getDeadPlayerService().isDead(player)) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isTeamMenuItem(item)) {
            return;
        }

        if (plugin.getEndWarService() != null && plugin.getEndWarService().isGameRunning()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        openMenu(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        Player attacker = getResponsiblePlayer(event.getDamager());
        if (!(event.getEntity() instanceof Player victim) || attacker == null) {
            return;
        }

        if (isOnSameTeam(attacker, victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> applyPlayerDisplay(event.getPlayer()));
    }

    @EventHandler
    public void onTeamMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof TeamMenuHolder holder) || !holder.viewer().equals(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        String action = clicked.getItemMeta().getPersistentDataContainer().get(teamMenuActionKey, PersistentDataType.STRING);
        if (action == null) {
            return;
        }

        handleMenuAction(player, action);
    }

    @EventHandler
    public void onTeamMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TeamMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMenuAction(Player player, String action) {
        boolean teamLocked = plugin.getEndWarService() != null && plugin.getEndWarService().isGameRunning();

        if (action.equals("team:create")) {
            if (teamLocked) {
                plugin.getMessageManager().sendPrefixed(player, "team-locked");
                player.closeInventory();
                return;
            }
            if (createTeam(player, null)) {
                plugin.getMessageManager().sendPrefixed(player, "team-created", Map.of("%team%", getTeam(player).name()));
            } else {
                plugin.getMessageManager().sendPrefixed(player, "team-create-failed");
            }
            player.openInventory(createMainMenu(player));
            return;
        }

        if (action.equals("team:invites")) {
            player.openInventory(createInvitesMenu(player));
            return;
        }

        if (action.equals("team:invite-menu")) {
            player.openInventory(createInvitePlayersMenu(player));
            return;
        }

        if (action.equals("team:back")) {
            player.openInventory(createMainMenu(player));
            return;
        }

        if (action.equals("team:leave")) {
            if (teamLocked) {
                plugin.getMessageManager().sendPrefixed(player, "team-locked");
                player.closeInventory();
                return;
            }
            if (leaveTeam(player)) {
                plugin.getMessageManager().sendPrefixed(player, "team-left");
            }
            player.closeInventory();
            return;
        }

        if (action.equals("team:rename-help")) {
            plugin.getMessageManager().sendPrefixed(player, "team-rename-help");
            return;
        }

        if (action.equals("team:disband")) {
            if (teamLocked) {
                plugin.getMessageManager().sendPrefixed(player, "team-locked");
                player.closeInventory();
                return;
            }
            EndWarTeam team = getTeam(player);
            String teamName = team == null ? "Team" : team.name();
            if (disbandTeam(player)) {
                plugin.getMessageManager().sendPrefixed(player, "team-disbanded", Map.of("%team%", teamName));
            }
            player.closeInventory();
            return;
        }

        if (action.startsWith("team:invite:")) {
            if (teamLocked) {
                plugin.getMessageManager().sendPrefixed(player, "team-locked");
                player.closeInventory();
                return;
            }
            UUID targetId = parseUuid(action.substring("team:invite:".length()));
            Player target = targetId == null ? null : Bukkit.getPlayer(targetId);
            if (target != null && invitePlayer(player, target)) {
                plugin.getMessageManager().sendPrefixed(player, "team-invite-sent", Map.of("%player%", target.getName()));
                plugin.getMessageManager().sendPrefixed(target, "team-invite-received", Map.of("%player%", player.getName(), "%team%", getTeam(player).name()));
            } else {
                plugin.getMessageManager().sendPrefixed(player, "team-invite-failed");
            }
            player.openInventory(createInvitePlayersMenu(player));
            return;
        }

        if (action.startsWith("team:accept:")) {
            if (teamLocked) {
                plugin.getMessageManager().sendPrefixed(player, "team-locked");
                player.closeInventory();
                return;
            }
            UUID teamId = parseUuid(action.substring("team:accept:".length()));
            EndWarTeam team = getTeam(teamId);
            if (team != null && acceptInvite(player, team.name())) {
                plugin.getMessageManager().sendPrefixed(player, "team-joined", Map.of("%team%", team.name()));
            } else {
                plugin.getMessageManager().sendPrefixed(player, "team-join-failed");
            }
            player.closeInventory();
        }
    }

    private UUID parseUuid(String rawValue) {
        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Player getResponsiblePlayer(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private String pickNextColor() {
        Set<String> used = new LinkedHashSet<>();
        for (EndWarTeam team : teams.values()) {
            used.add(team.colorHex());
        }

        for (String color : teamColorPalette) {
            if (!used.contains(color)) {
                return color.startsWith("#") ? color : "#" + color;
            }
        }

        int seed = teams.size() * 57;
        int red = 96 + (seed * 23) % 128;
        int green = 96 + (seed * 41) % 128;
        int blue = 96 + (seed * 59) % 128;
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    private String abbreviate(String teamName) {
        String stripped = TextUtil.stripColor(teamName);
        if (stripped.length() <= 8) {
            return stripped;
        }
        return stripped.substring(0, 8);
    }

    private void applyPlayerScoreboardTeam(UUID memberId, EndWarTeam team) {
        if (team == null) {
            unregisterPlayerScoreboardTeam(memberId);
            return;
        }

        Player player = Bukkit.getPlayer(memberId);
        if (player != null && plugin.getEggHolderService().isHolder(player)) {
            return;
        }

        String playerName = Bukkit.getOfflinePlayer(memberId).getName();
        if (playerName == null || playerName.isBlank()) {
            return;
        }

        String scoreboardId = buildPlayerScoreboardId(memberId);
        Team scoreboardTeam = scoreboard.getTeam(scoreboardId);
        if (scoreboardTeam == null) {
            scoreboardTeam = scoreboard.registerNewTeam(scoreboardId);
        }

        scoreboardTeam.setAllowFriendlyFire(false);
        scoreboardTeam.setCanSeeFriendlyInvisibles(true);
        scoreboardTeam.prefix(TextUtil.component(formatTeamPrefix(team) + colorForPlayer(memberId)));

        for (String entry : new ArrayList<>(scoreboardTeam.getEntries())) {
            if (!entry.equals(playerName)) {
                scoreboardTeam.removeEntry(entry);
            }
        }
        if (!scoreboardTeam.hasEntry(playerName)) {
            scoreboardTeam.addEntry(playerName);
        }
    }

    private String formatTeamPrefix(EndWarTeam team) {
        return TextUtil.replacePlaceholders(
                teamPrefixFormat,
                Map.of(
                        "%team%", abbreviate(team.name()),
                        "%color%", team.colorHex(),
                        "%team_color%", team.colorHex()
                )
        );
    }

    private String formatPlayerListName(EndWarTeam team, Player player) {
        return TextUtil.replacePlaceholders(
                teamListFormat,
                Map.of(
                        "%team%", team.name(),
                        "%color%", team.colorHex(),
                        "%team_color%", team.colorHex(),
                        "%prefix%", formatTeamPrefix(team),
                        "%player%", player.getName(),
                        "%player_color%", colorForPlayer(player.getUniqueId())
                )
        );
    }

    private String sanitizeTeamName(String input) {
        String stripped = TextUtil.stripColor(input).trim();
        if (stripped.length() > 20) {
            stripped = stripped.substring(0, 20);
        }
        return stripped;
    }

    private String resolveTeamName(String requestedName, UUID excludedTeamId, String playerName) {
        String rawName = requestedName;
        if (rawName == null || rawName.isBlank()) {
            rawName = autoNameEnabled ? generateAutoTeamName(excludedTeamId) : TextUtil.replacePlaceholders(soloTeamNameFormat, Map.of("%player%", playerName));
        }

        String sanitized = sanitizeTeamName(rawName);
        if (sanitized.isBlank()) {
            sanitized = sanitizeTeamName(TextUtil.replacePlaceholders(soloTeamNameFormat, Map.of("%player%", playerName)));
        }
        if (sanitized.isBlank()) {
            sanitized = "Team";
        }
        return makeUniqueTeamName(sanitized, excludedTeamId);
    }

    private String generateAutoTeamName(UUID excludedTeamId) {
        if (autoNameAdjectives.isEmpty() || autoNameNouns.isEmpty()) {
            return "Team";
        }

        int startIndex = Math.floorMod(teams.size(), autoNameAdjectives.size() * autoNameNouns.size());
        for (int offset = 0; offset < autoNameAdjectives.size() * autoNameNouns.size(); offset++) {
            int index = (startIndex + offset) % (autoNameAdjectives.size() * autoNameNouns.size());
            String adjective = autoNameAdjectives.get(index % autoNameAdjectives.size());
            String noun = autoNameNouns.get((index / autoNameAdjectives.size()) % autoNameNouns.size());
            String candidate = sanitizeTeamName(TextUtil.replacePlaceholders(
                    autoNameFormat,
                    Map.of("%adjective%", adjective, "%noun%", noun)
            ));
            if (!candidate.isBlank() && isTeamNameAvailable(candidate, excludedTeamId)) {
                return candidate;
            }
        }

        return "Team";
    }

    private String makeUniqueTeamName(String baseName, UUID excludedTeamId) {
        String sanitized = sanitizeTeamName(baseName);
        if (sanitized.isBlank()) {
            sanitized = "Team";
        }
        if (isTeamNameAvailable(sanitized, excludedTeamId)) {
            return sanitized;
        }

        for (int suffix = 2; suffix <= 99; suffix++) {
            String candidate = sanitizeTeamName(trimToLength(sanitized, 20 - (" " + suffix).length()) + " " + suffix);
            if (isTeamNameAvailable(candidate, excludedTeamId)) {
                return candidate;
            }
        }
        return sanitizeTeamName(trimToLength(sanitized, 18) + " X");
    }

    private boolean isTeamNameAvailable(String candidate, UUID excludedTeamId) {
        String sanitized = sanitizeTeamName(candidate);
        for (EndWarTeam team : teams.values()) {
            if (excludedTeamId != null && excludedTeamId.equals(team.id())) {
                continue;
            }
            if (sanitizeTeamName(team.name()).equalsIgnoreCase(sanitized)) {
                return false;
            }
        }
        return true;
    }

    private String trimToLength(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength)).trim();
    }

    private String buildPlayerScoreboardId(UUID playerId) {
        return "ewp_" + playerId.toString().replace("-", "").substring(0, 12);
    }

    private Material parseMaterial(String raw, Material fallback) {
        if (raw == null) {
            return fallback;
        }
        Material material = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void cleanupEmptyInvites() {
        pendingInvites.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isEmpty());
    }

    public List<String> tabCompleteSubCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            return partial(args[0], List.of("create", "invite", "accept", "rename", "leave", "kick", "disband", "menu"));
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("invite")) {
                return partial(args[1], getInvitablePlayerNames(player));
            }
            if (sub.equals("accept")) {
                return partial(args[1], getInviterNames(player));
            }
            if (sub.equals("kick")) {
                return partial(args[1], getKickablePlayerNames(player));
            }
        }
        return List.of();
    }

    private List<String> partial(String token, Iterable<String> options) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, matches);
        Collections.sort(matches);
        return matches;
    }

    public record TeamMenuHolder(UUID viewer, TeamMenuPage page) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public enum TeamMenuPage {
        MAIN,
        INVITES,
        INVITE_PLAYERS
    }

    public static final class EndWarTeam {

        private final UUID id;
        private UUID leader;
        private String name;
        private final String colorHex;
        private final Set<UUID> members = new LinkedHashSet<>();
        private int kills;

        private EndWarTeam(UUID id, String name, String colorHex) {
            this.id = id;
            this.leader = id;
            this.name = name;
            this.colorHex = colorHex;
        }

        public UUID id() {
            return id;
        }

        public UUID leader() {
            return leader;
        }

        public void setLeader(UUID leader) {
            this.leader = leader;
        }

        public String name() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String colorHex() {
            return colorHex;
        }

        public Set<UUID> members() {
            return members;
        }

        public int kills() {
            return kills;
        }

        public void incrementKills() {
            this.kills++;
        }

        public void resetKills() {
            this.kills = 0;
        }
    }
}
