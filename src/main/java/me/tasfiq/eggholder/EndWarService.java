package me.tasfiq.eggholder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Display;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class EndWarService implements Listener {

    private final EggHolderPlugin plugin;
    private final TeamService teamService;
    private final KitService kitService;
    private final SidebarService sidebarService;
    private final NamespacedKey audienceItemKey;
    private final NamespacedKey trackedEggKey;
    private final NamespacedKey eggDropSourceKey;

    private final EnumMap<Feature, Boolean> runtimeFeatures = new EnumMap<>(Feature.class);
    private final Map<UUID, PlayerStats> playerStats = new LinkedHashMap<>();
    private final Map<UUID, Long> entryProtectionUntil = new LinkedHashMap<>();
    private final Map<UUID, Long> revengeMarkedUntil = new LinkedHashMap<>();
    private final Map<UUID, DamageContribution> holderDamage = new LinkedHashMap<>();
    private final Map<BlockKey, BlockData> startPortalSnapshot = new LinkedHashMap<>();
    private final Map<BlockKey, SupplyDrop> landedSupplyDrops = new LinkedHashMap<>();
    private final Map<BlockKey, CrystalObjective> crystalObjectives = new LinkedHashMap<>();
    private final Set<BlockKey> placedEndBlocks = new LinkedHashSet<>();

    private BukkitTask mainTask;
    private BukkitTask visualTask;
    private MatchState matchState = MatchState.LOBBY;
    private UUID currentHolderId;
    private long holderAssignedAtMillis;
    private long matchStartedAtMillis;
    private long portalOpenedAtMillis;
    private long nextStormAtMillis;
    private long nextHotspotAtMillis;
    private long nextSupplyDropAtMillis;
    private long nextBridgeCollapseAtMillis;
    private long nextEggBurnWarningAtMillis;
    private boolean panicPhaseTriggered;
    private boolean overtimeTriggered;
    private boolean mercylessTriggered;
    private boolean noEscapeTriggered;
    private SupplyDrop activeSupplyDrop;
    private TrackedEgg trackedEgg;
    private Hotspot hotspot;
    private AudienceVote audienceVote;

    private Material audienceMenuMaterial;
    private String audienceMenuName;
    private List<String> audienceMenuLore;
    private int audienceMenuSlot;
    private World overworld;
    private World endWorld;
    private Location overworldPortalCenter;
    private Location endCenter;
    private double centerNoBuildRadius;
    private int startPortalRadius;
    private int entryLockSeconds;
    private double teamScatterRadius;
    private int portalPlatformRadius;
    private long overtimeAfterSeconds;
    private long mercylessAfterSeconds;
    private long noEscapeAfterSeconds;
    private double overtimeDamage;
    private int endStormIntervalSeconds;
    private long visualUpdateIntervalTicks;
    private int hotspotIntervalSeconds;
    private int hotspotDurationSeconds;
    private double hotspotRadius;
    private int hotspotRingParticlePoints;
    private int hotspotCloudParticleCount;
    private double hotspotVisualHeight;
    private int supplyDropIntervalSeconds;
    private int supplyDropSpawnHeight;
    private double supplyDropDescentPerVisualTick;
    private double supplyDropSwayRadius;
    private double supplyDropCanopyRadius;
    private int audienceVoteDurationSeconds;
    private int eggDropCountdownSeconds;
    private int eggBurnSeconds;
    private int bridgeCollapseIntervalSeconds;
    private double bridgeCollapseRadius;
    private int bridgeCollapseBatchSize;
    private double antiCampRadius;
    private int antiCampSeconds;
    private double panicHealthThreshold;
    private List<ConfiguredPotionEffect> combatMomentumEffects = List.of();
    private List<ConfiguredPotionEffect> clutchStealEffects = List.of();
    private List<ConfiguredPotionEffect> antiCleanEffects = List.of();
    private List<ConfiguredPotionEffect> killConfirmEffects = List.of();
    private List<String> killConfirmCommands = List.of();
    private List<ConfiguredPotionEffect> hotspotEffects = List.of();
    private List<ConfiguredPotionEffect> noEscapeEffects = List.of();
    private List<ConfiguredPotionEffect> objectiveCaptureEffects = List.of();
    private List<ConfiguredPotionEffect> audienceHealEffects = List.of();
    private List<ConfiguredItem> supplyDropLoot = List.of();
    private List<Location> configuredObjectiveLocations = List.of();

    public EndWarService(EggHolderPlugin plugin, TeamService teamService, KitService kitService, SidebarService sidebarService) {
        this.plugin = plugin;
        this.teamService = teamService;
        this.kitService = kitService;
        this.sidebarService = sidebarService;
        this.audienceItemKey = new NamespacedKey(plugin, "audience_tool_item");
        this.trackedEggKey = new NamespacedKey(plugin, "tracked_egg");
        this.eggDropSourceKey = new NamespacedKey(plugin, "tracked_egg_source");
        reload();
    }

    public void start() {
        restartTasks();
        for (Player player : Bukkit.getOnlinePlayers()) {
            giveLobbyItems(player);
        }
    }

    public void shutdown() {
        cancelTasks();
        cleanupRuntimeEntities();
        restoreStartPortalArea();
        playerStats.clear();
        entryProtectionUntil.clear();
        revengeMarkedUntil.clear();
        holderDamage.clear();
        landedSupplyDrops.clear();
        crystalObjectives.clear();
        placedEndBlocks.clear();
        teamSpawnLocations.clear();
        trackedEgg = null;
        activeSupplyDrop = null;
        hotspot = null;
        audienceVote = null;
        currentHolderId = null;
        holderAssignedAtMillis = 0L;
        matchState = MatchState.LOBBY;
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();

        String overworldName = config.getString("game.worlds.overworld", "world");
        this.overworld = Bukkit.getWorld(overworldName);
        if (this.overworld == null && !Bukkit.getWorlds().isEmpty()) {
            this.overworld = Bukkit.getWorlds().get(0);
        }
        refreshWorldContext(config);
        this.centerNoBuildRadius = Math.max(0.0D, config.getDouble("features.center-no-build-ring.radius", 20.0D));
        this.startPortalRadius = Math.max(2, config.getInt("start.portal.radius", 3));
        this.entryLockSeconds = Math.max(0, config.getInt("start.entry-lock-seconds", 60));
        this.teamScatterRadius = Math.max(16.0D, config.getDouble("start.team-scatter-radius", 110.0D));
        this.portalPlatformRadius = Math.max(1, config.getInt("start.spawn-platform-radius", 3));
        this.overtimeAfterSeconds = Math.max(30L, config.getLong("features.overtime-win-condition.seconds", 1800L));
        this.mercylessAfterSeconds = Math.max(30L, config.getLong("features.mercyless-endgame.seconds", 2400L));
        this.noEscapeAfterSeconds = Math.max(10L, config.getLong("features.no-escape-timer.seconds", 240L));
        this.overtimeDamage = Math.max(0.0D, config.getDouble("features.overtime-win-condition.damage-per-second", 1.0D));
        this.endStormIntervalSeconds = Math.max(15, config.getInt("features.end-storm.interval-seconds", 180));
        this.visualUpdateIntervalTicks = Math.max(1L, config.getLong("systems.visual-update-interval-ticks", 4L));
        this.hotspotIntervalSeconds = Math.max(15, config.getInt("features.hotspot-rotation.interval-seconds", 150));
        this.hotspotDurationSeconds = Math.max(10, config.getInt("features.hotspot-rotation.duration-seconds", 60));
        this.hotspotRadius = Math.max(4.0D, config.getDouble("features.hotspot-rotation.radius", 14.0D));
        this.hotspotRingParticlePoints = Math.max(12, config.getInt("features.hotspot-rotation.visuals.ring-points", 28));
        this.hotspotCloudParticleCount = Math.max(8, config.getInt("features.hotspot-rotation.visuals.cloud-particles", 24));
        this.hotspotVisualHeight = Math.max(0.1D, config.getDouble("features.hotspot-rotation.visuals.height", 0.25D));
        this.supplyDropIntervalSeconds = Math.max(30, config.getInt("features.supply-drops.interval-seconds", 300));
        this.supplyDropSpawnHeight = Math.max(10, config.getInt("features.supply-drops.spawn-height", 40));
        this.supplyDropDescentPerVisualTick = Math.max(0.2D, config.getDouble("features.supply-drops.descent-per-visual-tick", 1.15D));
        this.supplyDropSwayRadius = Math.max(0.0D, config.getDouble("features.supply-drops.sway-radius", 0.75D));
        this.supplyDropCanopyRadius = Math.max(0.4D, config.getDouble("features.supply-drops.canopy-radius", 1.4D));
        this.audienceVoteDurationSeconds = Math.max(10, config.getInt("features.audience-tools.vote-duration-seconds", 30));
        this.eggDropCountdownSeconds = Math.max(0, config.getInt("features.egg-drop-countdown.seconds", 5));
        this.eggBurnSeconds = Math.max(15, config.getInt("features.egg-burn-timer.seconds", 90));
        this.bridgeCollapseIntervalSeconds = Math.max(20, config.getInt("features.bridge-collapse-event.interval-seconds", 180));
        this.bridgeCollapseRadius = Math.max(0.0D, config.getDouble("features.bridge-collapse-event.radius", 150.0D));
        this.bridgeCollapseBatchSize = Math.max(1, config.getInt("features.bridge-collapse-event.blocks-per-wave", 40));
        this.antiCampRadius = Math.max(1.0D, config.getDouble("features.anti-camp-detector.radius", 12.0D));
        this.antiCampSeconds = Math.max(5, config.getInt("features.anti-camp-detector.seconds", 20));
        this.panicHealthThreshold = Math.max(1.0D, config.getDouble("features.panic-phase.health-threshold", 8.0D));
        this.audienceMenuMaterial = parseMaterial(config.getString("menus.audience.material"), Material.ECHO_SHARD);
        this.audienceMenuName = config.getString("menus.audience.name", "&6&lAudience Vote");
        this.audienceMenuLore = config.getStringList("menus.audience.lore");
        this.audienceMenuSlot = clamp(config.getInt("menus.audience.slot", 5), 0, 8);
        this.runtimeFeatures.clear();
        for (Feature feature : Feature.values()) {
            runtimeFeatures.put(feature, config.getBoolean(feature.configPath(), feature.defaultEnabled()));
        }

        this.combatMomentumEffects = loadEffects(config.getConfigurationSection("features.combat-momentum-buff.effects"));
        this.clutchStealEffects = loadEffects(config.getConfigurationSection("features.clutch-steal-bonus.effects"));
        this.antiCleanEffects = loadEffects(config.getConfigurationSection("features.anti-clean-mechanic.effects"));
        this.killConfirmEffects = loadEffects(config.getConfigurationSection("features.kill-confirm-reward.effects"));
        this.killConfirmCommands = new ArrayList<>(config.getStringList("features.kill-confirm-reward.commands"));
        this.hotspotEffects = loadEffects(config.getConfigurationSection("features.hotspot-rotation.effects"));
        this.noEscapeEffects = loadEffects(config.getConfigurationSection("features.no-escape-timer.effects"));
        this.objectiveCaptureEffects = loadEffects(config.getConfigurationSection("features.end-crystal-objectives.capture-effects"));
        this.audienceHealEffects = loadEffects(config.getConfigurationSection("features.audience-tools.heal-effects"));
        this.supplyDropLoot = loadItems(config.getConfigurationSection("features.supply-drops.loot"));
        this.configuredObjectiveLocations = resolveObjectiveLocations(config.getConfigurationSection("features.end-crystal-objectives.objectives"));
        this.overworldPortalCenter = resolveOverworldPortalCenter();

        for (Feature feature : Feature.values()) {
            if (!runtimeFeatures.getOrDefault(feature, feature.defaultEnabled())) {
                disableRuntimeFeature(feature);
            }
        }

        if (mainTask != null || visualTask != null) {
            restartTasks();
        }
    }

    public boolean isGameRunning() {
        return matchState == MatchState.PORTAL_OPEN || matchState == MatchState.RUNNING || matchState == MatchState.OVERTIME || matchState == MatchState.MERCILESS;
    }

    public String getDisplayPhase() {
        return switch (matchState) {
            case LOBBY -> "Lobby";
            case PORTAL_OPEN -> "Portal Open";
            case RUNNING -> "Running";
            case OVERTIME -> "Overtime";
            case MERCILESS -> "Merciless";
            case ENDED -> "Ended";
        };
    }

    public int getKills(Player player) {
        if (player == null) {
            return 0;
        }
        PlayerStats stats = playerStats.get(player.getUniqueId());
        return stats == null ? 0 : stats.kills();
    }

    public int getKillStreak(Player player) {
        if (player == null) {
            return 0;
        }
        PlayerStats stats = playerStats.get(player.getUniqueId());
        return stats == null ? 0 : stats.killStreak();
    }

    public boolean areRevivesAllowed() {
        return matchState != MatchState.MERCILESS || !isFeatureEnabled(Feature.MERCILESS_ENDGAME);
    }

    public Collection<String> getFeatureIds() {
        List<String> ids = new ArrayList<>();
        for (Feature feature : Feature.values()) {
            ids.add(feature.id());
        }
        return ids;
    }

    public boolean setFeature(String rawId, boolean enabled) {
        Feature feature = Feature.fromId(rawId);
        if (feature == null) {
            return false;
        }
        runtimeFeatures.put(feature, enabled);
        if (!enabled) {
            disableRuntimeFeature(feature);
        }
        return true;
    }

    public boolean triggerFeature(String rawId) {
        Feature feature = Feature.fromId(rawId);
        if (feature == null) {
            return false;
        }

        return switch (feature) {
            case END_STORM -> triggerEndStorm(true);
            case HOTSPOT_ROTATION -> rotateHotspot(true);
            case SUPPLY_DROPS -> triggerSupplyDrop(true);
            case AUDIENCE_TOOLS -> openAudienceVote();
            default -> false;
        };
    }

    public boolean openAudienceVote() {
        if (!isFeatureEnabled(Feature.AUDIENCE_TOOLS) || audienceVote != null) {
            return false;
        }

        long expiresAt = System.currentTimeMillis() + audienceVoteDurationSeconds * 1000L;
        audienceVote = new AudienceVote(expiresAt);
        announceChatAndTitle(
                "audience-vote-opened",
                "audience-vote-title",
                "audience-vote-subtitle",
                Map.of("%seconds%", Integer.toString(audienceVoteDurationSeconds))
        );
        return true;
    }

    public void closeAudienceVote(boolean resolve) {
        if (audienceVote == null) {
            return;
        }

        AudienceVote vote = audienceVote;
        audienceVote = null;
        if (!resolve) {
            plugin.getMessageManager().broadcastPrefixed("audience-vote-cancelled", Map.of());
            return;
        }

        AudienceEffect winningEffect = vote.resolveWinner();
        if (winningEffect == null) {
            plugin.getMessageManager().broadcastPrefixed("audience-vote-no-winner", Map.of());
            return;
        }

        triggerAudienceEffect(winningEffect);
    }

    public boolean startGame() {
        if (isGameRunning()) {
            return false;
        }
        if (plugin.getManagedEndWorldService() != null
                && plugin.getManagedEndWorldService().isEnabled()
                && !plugin.getManagedEndWorldService().ensurePrepared()) {
            return false;
        }

        refreshWorldContext(plugin.getConfig());
        if (overworld == null || endWorld == null || endCenter == null) {
            return false;
        }

        cleanupRuntimeEntities();
        trackedEgg = null;
        audienceVote = null;
        hotspot = null;
        activeSupplyDrop = null;
        entryProtectionUntil.clear();
        revengeMarkedUntil.clear();
        holderDamage.clear();
        landedSupplyDrops.clear();
        crystalObjectives.clear();
        placedEndBlocks.clear();
        playerStats.clear();
        teamSpawnLocations.clear();
        teamService.resetMatchStats();
        teamService.ensureSoloTeamsForUngroupedPlayers(Bukkit.getOnlinePlayers());
        closePluginInventories();
        buildStartPortal();
        assignTeamSpawnLocations();
        spawnObjectiveCrystals();
        matchState = MatchState.PORTAL_OPEN;
        portalOpenedAtMillis = System.currentTimeMillis();
        matchStartedAtMillis = portalOpenedAtMillis;
        holderAssignedAtMillis = 0L;
        overtimeTriggered = false;
        mercylessTriggered = false;
        noEscapeTriggered = false;
        panicPhaseTriggered = false;
        currentHolderId = plugin.getEggHolderService().getCurrentHolderPlayer() == null ? null : plugin.getEggHolderService().getCurrentHolderPlayer().getUniqueId();
        nextStormAtMillis = portalOpenedAtMillis + endStormIntervalSeconds * 1000L;
        nextHotspotAtMillis = portalOpenedAtMillis + hotspotIntervalSeconds * 1000L;
        nextSupplyDropAtMillis = portalOpenedAtMillis + supplyDropIntervalSeconds * 1000L;
        nextBridgeCollapseAtMillis = portalOpenedAtMillis + bridgeCollapseIntervalSeconds * 1000L;
        giveLobbyItemsToAll();
        announceChatAndTitle("game-started", "game-start-title", "game-start-subtitle", Map.of());
        return true;
    }

    public void stopGame() {
        matchState = MatchState.ENDED;
        cleanupRuntimeEntities();
        restoreStartPortalArea();
        revengeMarkedUntil.clear();
        holderDamage.clear();
        entryProtectionUntil.clear();
        landedSupplyDrops.clear();
        crystalObjectives.clear();
        placedEndBlocks.clear();
        teamSpawnLocations.clear();
        playerStats.clear();
        teamService.resetMatchStats();
        currentHolderId = null;
        holderAssignedAtMillis = 0L;
        overtimeTriggered = false;
        mercylessTriggered = false;
        noEscapeTriggered = false;
        panicPhaseTriggered = false;
        trackedEgg = null;
        audienceVote = null;
        hotspot = null;
        activeSupplyDrop = null;
        closePluginInventories();
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeAudienceItem(player);
        }
        giveLobbyItemsToAll();
        announceChatAndTitle("game-stopped", "game-stopped-title", "game-stopped-subtitle", Map.of());
    }

    public void giveLobbyItems(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (isGameRunning()) {
            teamService.removeMenuItem(player);
            kitService.removeMenuItem(player);
        } else {
            teamService.giveMenuItem(player);
            kitService.giveMenuItem(player);
        }

        if (isGameRunning() && plugin.getDeadPlayerService().isDead(player)) {
            giveAudienceItem(player);
        } else {
            removeAudienceItem(player);
        }
        sidebarService.refresh(player);
    }

    private void giveLobbyItemsToAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            giveLobbyItems(player);
        }
    }

    public boolean isAudienceToolItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        Byte marker = stack.getItemMeta().getPersistentDataContainer().get(audienceItemKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    public boolean isAudienceMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof AudienceMenuHolder;
    }

    public void clearAudienceState(Player player) {
        if (player == null) {
            return;
        }
        if (isAudienceMenu(player.getOpenInventory().getTopInventory())) {
            player.closeInventory();
        }
        removeAudienceItem(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> giveLobbyItems(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        entryProtectionUntil.remove(event.getPlayer().getUniqueId());
        revengeMarkedUntil.remove(event.getPlayer().getUniqueId());
        if (!isGameRunning()) {
            playerStats.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            giveLobbyItems(event.getPlayer());
            if (isGameRunning() && plugin.getDeadPlayerService().isDead(event.getPlayer())) {
                giveAudienceItem(event.getPlayer());
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();

        if (isGameRunning()) {
            PlayerStats victimStats = playerStats.computeIfAbsent(victim.getUniqueId(), unused -> new PlayerStats());
            victimStats.resetStreak();

            if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
                PlayerStats killerStats = playerStats.computeIfAbsent(killer.getUniqueId(), unused -> new PlayerStats());
                killerStats.incrementKills();
                teamService.incrementTeamKill(killer);
            }
        }

        if (!plugin.getEggHolderService().isHolder(victim)) {
            return;
        }

        if (killer != null && isGameRunning()) {
            if (isFeatureEnabled(Feature.KILL_CONFIRM_REWARD)) {
                applyPotionEffects(killer, killConfirmEffects);
                runCommands(killConfirmCommands, killer);
                plugin.getMessageManager().sendPrefixed(killer, "kill-confirm-reward-received", Map.of("%player%", victim.getName()));
            }

            if (isFeatureEnabled(Feature.ANTI_CLEAN_MECHANIC)) {
                applyPotionEffects(killer, antiCleanEffects);
                plugin.getMessageManager().sendPrefixed(killer, "anti-clean-applied", Map.of("%player%", victim.getName()));
            }

            if (isFeatureEnabled(Feature.REVENGE_MARK)) {
                revengeMarkedUntil.put(killer.getUniqueId(), System.currentTimeMillis() + plugin.getConfig().getLong("features.revenge-mark.seconds", 30L) * 1000L);
                killer.setGlowing(true);
                announceChatAndTitle(
                        "revenge-mark-applied",
                        "revenge-mark-title",
                        "revenge-mark-subtitle",
                        Map.of("%player%", killer.getName())
                );
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !isGameRunning() || !isEndWorld(dragon.getWorld())) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> announceChatAndTitle(
                "dragon-killed-egg-instructions",
                "dragon-killed-egg-title",
                "dragon-killed-egg-subtitle",
                Map.of()
        ), 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHolderDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player holder) || !plugin.getEggHolderService().isHolder(holder)) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(holder.getUniqueId())) {
            return;
        }

        if (isFeatureEnabled(Feature.COMBAT_MOMENTUM_BUFF)) {
            applyPotionEffects(attacker, combatMomentumEffects);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntryProtectedDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if ((attacker != null && isEntryProtected(attacker)) || isEntryProtected(victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProtectedBuild(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!isGameRunning() || !isEndWorld(event.getBlockPlaced().getWorld())) {
            return;
        }

        if (isEntryProtected(player) || isInsideCenterRing(event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            return;
        }

        placedEndBlocks.add(BlockKey.of(event.getBlockPlaced()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProtectedBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isGameRunning() || !isEndWorld(event.getBlock().getWorld())) {
            return;
        }

        if (isEntryProtected(player) || isInsideCenterRing(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }

        if (landedSupplyDrops.containsKey(BlockKey.of(event.getBlock()))) {
            event.setCancelled(true);
            return;
        }

        placedEndBlocks.remove(BlockKey.of(event.getBlock()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProtectedInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (plugin.getDeadPlayerService().isDead(player) && isAudienceToolItem(event.getItem())) {
            event.setCancelled(true);
            if (audienceVote == null) {
                plugin.getMessageManager().sendPrefixed(player, "audience-vote-not-open");
            } else {
                player.openInventory(createAudienceMenu(player));
            }
            return;
        }

        if (isGameRunning() && isEndWorld(player.getWorld()) && isEntryProtected(player)) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            SupplyDrop supplyDrop = landedSupplyDrops.get(BlockKey.of(event.getClickedBlock()));
            if (supplyDrop != null) {
                event.setCancelled(true);
                player.openInventory(supplyDrop.inventory());
                return;
            }
        }

        if (plugin.getEggHolderService().isHolder(player) && isNoEscapeTriggered() && event.getItem() != null) {
            Material type = event.getItem().getType();
            if (type == Material.ENDER_PEARL || type == Material.WIND_CHARGE) {
                event.setCancelled(true);
                plugin.getMessageManager().sendPrefixed(player, "no-escape-active");
            }
        }
    }

    @EventHandler
    public void onAudienceMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof AudienceMenuHolder holder) || !holder.viewer().equals(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        String action = item.getItemMeta().getPersistentDataContainer().get(eggDropSourceKey, PersistentDataType.STRING);
        if (action == null || audienceVote == null) {
            return;
        }

        AudienceEffect effect = AudienceEffect.fromId(action);
        if (effect == null) {
            return;
        }

        audienceVote.setVote(player.getUniqueId(), effect);
        plugin.getMessageManager().sendPrefixed(player, "audience-voted", Map.of("%effect%", effect.displayName()));
        player.closeInventory();
    }

    @EventHandler
    public void onAudienceMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AudienceMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!isGameRunning()) {
            return;
        }
        if (!event.getEntity().getItemStack().getType().equals(plugin.getPluginConfig().getEggMaterial())) {
            return;
        }
        if (plugin.getEggHolderService().getCurrentHolderPlayer() != null) {
            return;
        }

        startTrackingEgg(event.getEntity());
    }

    @EventHandler
    public void onEggPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (trackedEgg == null || !event.getItem().getUniqueId().equals(trackedEgg.itemUuid())) {
            return;
        }

        if (trackedEgg.pickupLockedUntilMillis() > System.currentTimeMillis()) {
            event.setCancelled(true);
            return;
        }

        if (isFeatureEnabled(Feature.CLUTCH_STEAL_BONUS)) {
            applyPotionEffects(player, clutchStealEffects);
            plugin.getMessageManager().sendPrefixed(player, "clutch-steal-bonus-applied", Map.of("%player%", player.getName()));
        }

        if (isFeatureEnabled(Feature.EGG_SHOCKWAVE)) {
            triggerEggShockwave(player);
        }

        trackedEgg = null;
        nextEggBurnWarningAtMillis = 0L;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortalUse(PlayerPortalEvent event) {
        if (!isGameRunning() || overworldPortalCenter == null || event.getFrom().getWorld() == null || event.getCause() != PlayerPortalEvent.TeleportCause.END_PORTAL) {
            return;
        }
        if (!event.getFrom().getWorld().equals(overworld)) {
            return;
        }
        if (event.getFrom().distanceSquared(overworldPortalCenter) > (startPortalRadius + 4.0D) * (startPortalRadius + 4.0D)) {
            return;
        }

        event.setCancelled(true);
        teleportPlayerToTeamSpawn(event.getPlayer());
    }

    @EventHandler
    public void onSupplyDropDamage(EntityDamageEvent event) {
        if (activeSupplyDrop == null) {
            return;
        }
        UUID uuid = event.getEntity().getUniqueId();
        if (uuid.equals(activeSupplyDrop.chickenUuid()) || uuid.equals(activeSupplyDrop.displayUuid())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onObjectiveDamage(EntityDamageEvent event) {
        for (CrystalObjective objective : crystalObjectives.values()) {
            if (objective.crystalUuid() != null && objective.crystalUuid().equals(event.getEntity().getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onSupplyDropInventoryClose(InventoryCloseEvent event) {
        for (SupplyDrop supplyDrop : new ArrayList<>(landedSupplyDrops.values())) {
            if (!supplyDrop.landed() || !supplyDrop.inventory().equals(event.getInventory())) {
                continue;
            }

            if (isInventoryEmpty(supplyDrop.inventory())) {
                clearSupplyDrop(supplyDrop);
            }
        }
    }

    private void tickMain() {
        sidebarService.tick();
        long now = System.currentTimeMillis();

        if (audienceVote != null && audienceVote.expiresAtMillis() <= now) {
            closeAudienceVote(true);
        }

        if (!isGameRunning()) {
            clearExpiredRevengeMarks(now);
            for (Player player : Bukkit.getOnlinePlayers()) {
                removeAudienceItem(player);
            }
            return;
        }

        if (trackedEgg != null) {
            tickTrackedEgg(now);
        }

        syncCurrentHolder();
        clearExpiredRevengeMarks(now);
        tickEntryProtection(now);
        if (isFeatureEnabled(Feature.END_CRYSTAL_OBJECTIVES) && crystalObjectives.isEmpty() && !isDragonFightActive()) {
            spawnObjectiveCrystals();
        }
        tickOvertime(now);
        tickMercyless(now);
        tickNoEscape(now);
        tickPanicPhase();
        tickAntiCamp(now);
        tickStorm(now);
        tickHotspot(now);
        tickSupplyDrops(now);
        tickBridgeCollapse(now);
        tickCrystalObjectives();
        giveAudienceItemsToDeadPlayers();
    }

    private void tickVisuals() {
        if (!isGameRunning()) {
            return;
        }

        if (hotspot != null && isFeatureEnabled(Feature.HOTSPOT_ROTATION)) {
            renderHotspot();
        }

        if (activeSupplyDrop != null && !activeSupplyDrop.landed()) {
            updateFallingSupplyDrop(activeSupplyDrop);
            renderSupplyDrop(activeSupplyDrop);
        }

        for (CrystalObjective objective : crystalObjectives.values()) {
            if (objective.captureLocation().getWorld() != null) {
                objective.captureLocation().getWorld().spawnParticle(Particle.END_ROD, objective.captureLocation().clone().add(0.0D, 1.5D, 0.0D), 8, 0.6D, 0.8D, 0.6D, 0.01D);
            }
        }
    }

    private void syncCurrentHolder() {
        Player currentHolder = plugin.getEggHolderService().getCurrentHolderPlayer();
        UUID holderId = currentHolder == null ? null : currentHolder.getUniqueId();
        if (holderId == null) {
            currentHolderId = null;
            holderAssignedAtMillis = 0L;
            panicPhaseTriggered = false;
            noEscapeTriggered = false;
            holderDamage.clear();
            return;
        }

        if (!holderId.equals(currentHolderId)) {
            currentHolderId = holderId;
            holderAssignedAtMillis = System.currentTimeMillis();
            panicPhaseTriggered = false;
            noEscapeTriggered = false;
            holderDamage.clear();
            kitService.applySelectedKit(currentHolder);
        }
    }

    private void tickEntryProtection(long now) {
        entryProtectionUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private void tickOvertime(long now) {
        if (!isFeatureEnabled(Feature.OVERTIME_WIN_CONDITION) || matchStartedAtMillis == 0L) {
            return;
        }

        if (!overtimeTriggered) {
            if (now - matchStartedAtMillis < overtimeAfterSeconds * 1000L) {
                return;
            }

            overtimeTriggered = true;
            matchState = MatchState.OVERTIME;
            announceChatAndTitle("overtime-started", "overtime-title", "overtime-subtitle", Map.of());
        }

        Player holder = plugin.getEggHolderService().getCurrentHolderPlayer();
        if (holder != null && holder.isOnline() && !holder.isDead()) {
            holder.damage(overtimeDamage);
        }
    }

    private void tickMercyless(long now) {
        if (!isFeatureEnabled(Feature.MERCILESS_ENDGAME) || mercylessTriggered || matchStartedAtMillis == 0L) {
            return;
        }
        if (now - matchStartedAtMillis < mercylessAfterSeconds * 1000L) {
            return;
        }

        mercylessTriggered = true;
        matchState = MatchState.MERCILESS;
        announceChatAndTitle("mercyless-started", "mercyless-title", "mercyless-subtitle", Map.of());
    }

    private void tickNoEscape(long now) {
        if (!isFeatureEnabled(Feature.NO_ESCAPE_TIMER) || noEscapeTriggered || currentHolderId == null || holderAssignedAtMillis == 0L) {
            return;
        }
        if (now - holderAssignedAtMillis < noEscapeAfterSeconds * 1000L) {
            return;
        }

        noEscapeTriggered = true;
        Player holder = Bukkit.getPlayer(currentHolderId);
        if (holder != null) {
            applyPotionEffects(holder, noEscapeEffects);
        }
        announceChatAndTitle("no-escape-started", "no-escape-title", "no-escape-subtitle", Map.of("%player%", plugin.getEggHolderService().getCurrentHolderName() == null ? "Unknown" : plugin.getEggHolderService().getCurrentHolderName()));
    }

    private boolean isNoEscapeTriggered() {
        return noEscapeTriggered && isFeatureEnabled(Feature.NO_ESCAPE_TIMER);
    }

    private void tickPanicPhase() {
        if (!isFeatureEnabled(Feature.PANIC_PHASE) || panicPhaseTriggered || currentHolderId == null) {
            return;
        }

        Player holder = Bukkit.getPlayer(currentHolderId);
        if (holder == null || !holder.isOnline()) {
            return;
        }

        if (holder.getHealth() > panicHealthThreshold) {
            return;
        }

        panicPhaseTriggered = true;
        announceChatAndTitle("panic-phase-started", "panic-phase-title", "panic-phase-subtitle", Map.of("%player%", holder.getName()));
    }

    private void tickAntiCamp(long now) {
        if (!isFeatureEnabled(Feature.ANTI_CAMP_DETECTOR) || currentHolderId == null) {
            return;
        }
        Player holder = Bukkit.getPlayer(currentHolderId);
        if (holder == null || !holder.isOnline()) {
            return;
        }

        DamageContribution contribution = holderDamage.computeIfAbsent(currentHolderId, unused -> new DamageContribution());
        if (contribution.anchorLocation() == null || contribution.anchorLocation().distanceSquared(holder.getLocation()) > antiCampRadius * antiCampRadius) {
            contribution.setAnchor(holder.getLocation());
            contribution.setAnchorStartedAt(now);
            return;
        }

        if (now - contribution.anchorStartedAtMillis() >= antiCampSeconds * 1000L) {
            holder.removePotionEffect(PotionEffectType.WEAKNESS);
            holder.removePotionEffect(PotionEffectType.SLOWNESS);
            holder.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true, true));
            holder.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, true, true));
            plugin.getMessageManager().broadcastPrefixed("anti-camp-triggered", Map.of("%player%", holder.getName()));
            contribution.setAnchor(holder.getLocation());
            contribution.setAnchorStartedAt(now);
        }
    }

    private void tickStorm(long now) {
        if (!isFeatureEnabled(Feature.END_STORM) || now < nextStormAtMillis) {
            return;
        }

        triggerEndStorm(false);
        nextStormAtMillis = now + endStormIntervalSeconds * 1000L;
    }

    private void tickHotspot(long now) {
        if (isFeatureEnabled(Feature.HOTSPOT_ROTATION) && now >= nextHotspotAtMillis) {
            rotateHotspot(false);
            nextHotspotAtMillis = now + hotspotIntervalSeconds * 1000L;
        }

        if (hotspot == null || hotspot.expiresAtMillis() < now) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || plugin.getDeadPlayerService().isDead(player) || !isEndWorld(player.getWorld())) {
                continue;
            }

            if (player.getLocation().distanceSquared(hotspot.center()) <= hotspot.radius() * hotspot.radius()) {
                applyPotionEffects(player, hotspotEffects);
            }
        }
    }

    private void tickSupplyDrops(long now) {
        if (isFeatureEnabled(Feature.SUPPLY_DROPS) && activeSupplyDrop == null && now >= nextSupplyDropAtMillis) {
            triggerSupplyDrop(false);
            nextSupplyDropAtMillis = now + supplyDropIntervalSeconds * 1000L;
        }
    }

    private void tickBridgeCollapse(long now) {
        if (!isFeatureEnabled(Feature.BRIDGE_COLLAPSE_EVENT) || now < nextBridgeCollapseAtMillis || !isEndWorld(endCenter.getWorld())) {
            return;
        }

        nextBridgeCollapseAtMillis = now + bridgeCollapseIntervalSeconds * 1000L;
        int removed = 0;
        for (BlockKey blockKey : new ArrayList<>(placedEndBlocks)) {
            if (removed >= bridgeCollapseBatchSize) {
                break;
            }

            if (!blockKey.worldName().equals(endWorld.getName())) {
                placedEndBlocks.remove(blockKey);
                continue;
            }

            double distanceSquared = distanceSquared(blockKey, endCenter);
            if (distanceSquared < bridgeCollapseRadius * bridgeCollapseRadius) {
                continue;
            }

            Block block = endWorld.getBlockAt(blockKey.x(), blockKey.y(), blockKey.z());
            if (block.getType() != Material.AIR) {
                block.setType(Material.AIR, false);
                removed++;
            }
            placedEndBlocks.remove(blockKey);
        }

        if (removed > 0) {
            plugin.getMessageManager().broadcastPrefixed("bridge-collapse-wave", Map.of("%blocks%", Integer.toString(removed)));
        }
    }

    private void tickCrystalObjectives() {
        if (!isFeatureEnabled(Feature.END_CRYSTAL_OBJECTIVES) || crystalObjectives.isEmpty()) {
            return;
        }

        for (CrystalObjective objective : crystalObjectives.values()) {
            UUID capturingTeam = null;
            boolean contested = false;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isOnline() || plugin.getDeadPlayerService().isDead(player) || !isEndWorld(player.getWorld())) {
                    continue;
                }
                if (player.getLocation().distanceSquared(objective.captureLocation()) > objective.captureRadius() * objective.captureRadius()) {
                    continue;
                }

                UUID teamId = teamService.getTeamId(player);
                if (teamId == null) {
                    continue;
                }

                if (capturingTeam == null) {
                    capturingTeam = teamId;
                } else if (!capturingTeam.equals(teamId)) {
                    contested = true;
                    break;
                }
            }

            if (contested || capturingTeam == null) {
                objective.resetProgress();
                continue;
            }

            if (capturingTeam.equals(objective.ownerTeamId())) {
                continue;
            }

            objective.incrementProgress();
            if (objective.progressSeconds() < objective.captureSeconds()) {
                continue;
            }

            objective.setOwnerTeamId(capturingTeam);
            objective.resetProgress();
            TeamService.EndWarTeam team = teamService.getTeam(capturingTeam);
            if (team != null) {
                for (UUID memberId : team.members()) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null) {
                        applyPotionEffects(member, objectiveCaptureEffects);
                    }
                }
                plugin.getMessageManager().broadcastPrefixed("objective-captured", Map.of("%team%", team.name()));
            }
        }
    }

    private void tickTrackedEgg(long now) {
        Item item = Bukkit.getEntity(trackedEgg.itemUuid()) instanceof Item dropped ? dropped : null;
        if (item == null || !item.isValid()) {
            trackedEgg = null;
            nextEggBurnWarningAtMillis = 0L;
            return;
        }

        if (runtimeFeatures.get(Feature.EGG_BURN_TIMER) && now >= trackedEgg.spawnedAtMillis() + eggBurnSeconds * 1000L) {
            item.remove();
            trackedEgg = null;
            spawnCenterEgg();
            announceChatAndTitle("egg-burned", "egg-burned-title", "egg-burned-subtitle", Map.of());
            return;
        }

        if (runtimeFeatures.get(Feature.EGG_BURN_TIMER) && now >= nextEggBurnWarningAtMillis) {
            long remaining = Math.max(0L, (trackedEgg.spawnedAtMillis() + eggBurnSeconds * 1000L - now) / 1000L);
            plugin.getMessageManager().broadcastPrefixed("egg-burn-warning", Map.of("%seconds%", Long.toString(remaining)));
            nextEggBurnWarningAtMillis = now + 15_000L;
        }
    }

    private void clearExpiredRevengeMarks(long now) {
        if (revengeMarkedUntil.isEmpty()) {
            return;
        }

        revengeMarkedUntil.entrySet().removeIf(entry -> {
            if (entry.getValue() > now) {
                return false;
            }

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                boolean keepGlow = plugin.getEggHolderService().isHolder(player) && plugin.getPluginConfig().isHolderGlowEnabled();
                player.setGlowing(keepGlow);
            }
            return true;
        });
    }

    private void triggerEggShockwave(Player picker) {
        double radius = Math.max(2.0D, plugin.getConfig().getDouble("features.egg-shockwave.radius", 8.0D));
        double power = Math.max(0.2D, plugin.getConfig().getDouble("features.egg-shockwave.power", 1.0D));
        picker.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, picker.getLocation(), 1);
        picker.getWorld().playSound(picker.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.2F);

        for (Entity nearby : picker.getNearbyEntities(radius, radius, radius)) {
            if (!(nearby instanceof Player target) || target.equals(picker) || teamService.isOnSameTeam(picker, target)) {
                continue;
            }
            Vector push = target.getLocation().toVector().subtract(picker.getLocation().toVector());
            if (push.lengthSquared() == 0.0D) {
                push = new Vector(0.0D, 0.4D, 0.0D);
            }
            target.setVelocity(push.normalize().multiply(power).setY(0.35D));
        }
    }

    private boolean triggerEndStorm(boolean forced) {
        if (endWorld == null) {
            return false;
        }

        announceChatAndTitle("end-storm-triggered", "end-storm-title", "end-storm-subtitle", Map.of());
        List<Player> candidates = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOnline() && !plugin.getDeadPlayerService().isDead(player) && isEndWorld(player.getWorld())) {
                candidates.add(player);
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }

        Collections.shuffle(candidates, ThreadLocalRandom.current());
        for (int index = 0; index < Math.min(6, candidates.size()); index++) {
            Player target = candidates.get(index);
            endWorld.strikeLightningEffect(target.getLocation());
            target.removePotionEffect(PotionEffectType.LEVITATION);
            target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 0, false, true, true));
            target.getWorld().spawnParticle(Particle.DRAGON_BREATH, target.getLocation(), 24, 1.5D, 0.2D, 1.5D, 0.02D);
        }
        return true;
    }

    private boolean rotateHotspot(boolean forced) {
        if (endWorld == null) {
            return false;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
        double distance = Math.max(16.0D, teamScatterRadius * 0.65D);
        Location center = endCenter.clone().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
        center.setY(endCenter.getY());
        hotspot = new Hotspot(center, hotspotRadius, System.currentTimeMillis() + hotspotDurationSeconds * 1000L);
        announceChatAndTitle(
                "hotspot-rotated",
                "hotspot-title",
                "hotspot-subtitle",
                Map.of(
                        "%x%", Integer.toString(center.getBlockX()),
                        "%y%", Integer.toString(center.getBlockY()),
                        "%z%", Integer.toString(center.getBlockZ())
                )
        );
        return true;
    }

    private boolean triggerSupplyDrop(boolean forced) {
        if (endWorld == null || activeSupplyDrop != null) {
            return false;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
        double distance = random.nextDouble(Math.max(18.0D, centerNoBuildRadius + 8.0D), teamScatterRadius + 30.0D);
        Location dropCenter = endCenter.clone().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
        Location ground = findGround(dropCenter.clone());
        Location spawnLocation = ground.clone().add(0.0D, supplyDropSpawnHeight, 0.0D);

        Chicken chicken = endWorld.spawn(spawnLocation, Chicken.class, entity -> {
            entity.setAI(false);
            entity.setGravity(false);
            entity.setInvisible(true);
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setPersistent(false);
            entity.setAdult();
            entity.setGlowing(true);
            entity.setCollidable(false);
        });

        BlockDisplay display = endWorld.spawn(spawnLocation.clone().add(0.0D, -0.2D, 0.0D), BlockDisplay.class, blockDisplay -> {
            blockDisplay.setPersistent(false);
            blockDisplay.setInvulnerable(true);
            blockDisplay.setBlock(Bukkit.createBlockData(Material.ENDER_CHEST));
            blockDisplay.setBillboard(Display.Billboard.FIXED);
            blockDisplay.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(0.9F, 0.9F, 0.9F),
                    new Quaternionf()
            ));
        });
        chicken.addPassenger(display);
        Inventory loot = Bukkit.createInventory(new SupplyDropHolder(UUID.randomUUID()), 27, TextUtil.component("&5&lSupply Drop"));
        fillSupplyDropInventory(loot);
        activeSupplyDrop = new SupplyDrop(chicken.getUniqueId(), display.getUniqueId(), ground, loot, false);
        announceChatAndTitle(
                "supply-drop-spawned",
                "supply-drop-title",
                "supply-drop-subtitle",
                Map.of(
                        "%x%", Integer.toString(ground.getBlockX()),
                        "%y%", Integer.toString(ground.getBlockY()),
                        "%z%", Integer.toString(ground.getBlockZ())
                )
        );
        return true;
    }

    private void updateFallingSupplyDrop(SupplyDrop drop) {
        Chicken chicken = Bukkit.getEntity(drop.chickenUuid()) instanceof Chicken entity ? entity : null;
        Entity display = Bukkit.getEntity(drop.displayUuid());
        if (chicken == null || !chicken.isValid() || display == null || !display.isValid()) {
            cleanupSupplyDrop(drop);
            activeSupplyDrop = null;
            return;
        }

        Location current = chicken.getLocation();
        if (current.getY() <= drop.targetLocation().getY() + 1.0D || chicken.isOnGround()) {
            landSupplyDrop(drop);
            return;
        }

        double targetY = drop.targetLocation().getY() + 1.0D;
        double distanceRemaining = current.getY() - targetY;
        double descent = Math.max(0.25D, supplyDropDescentPerVisualTick);
        if (distanceRemaining < 6.0D) {
            descent = Math.min(descent, 0.8D);
        }
        double sway = Math.min(supplyDropSwayRadius, Math.max(0.0D, distanceRemaining / 10.0D));
        double offsetX = Math.sin(chicken.getTicksLived() / 6.0D) * sway;
        double offsetZ = Math.cos(chicken.getTicksLived() / 7.0D) * sway;
        double nextY = Math.max(targetY, current.getY() - descent);
        chicken.teleport(new Location(
                current.getWorld(),
                drop.targetLocation().getX() + offsetX,
                nextY,
                drop.targetLocation().getZ() + offsetZ,
                current.getYaw(),
                current.getPitch()
        ));
    }

    private void renderSupplyDrop(SupplyDrop drop) {
        Chicken chicken = Bukkit.getEntity(drop.chickenUuid()) instanceof Chicken entity ? entity : null;
        if (chicken == null || !chicken.isValid()) {
            return;
        }

        Location chestLocation = chicken.getLocation().clone();
        Location canopyCenter = chestLocation.clone().add(0.0D, 1.6D, 0.0D);
        chestLocation.getWorld().spawnParticle(Particle.CLOUD, canopyCenter, 8, 0.22D, 0.08D, 0.22D, 0.0D);

        int points = 10;
        for (int index = 0; index < points; index++) {
            double angle = (Math.PI * 2.0D * index) / points;
            double radius = supplyDropCanopyRadius;
            Location edge = canopyCenter.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            chestLocation.getWorld().spawnParticle(Particle.CLOUD, edge, 2, 0.06D, 0.03D, 0.06D, 0.0D);
            Location line = chestLocation.clone().add(Math.cos(angle) * 0.45D, 0.65D, Math.sin(angle) * 0.45D);
            chestLocation.getWorld().spawnParticle(Particle.END_ROD, line, 1, 0.02D, 0.18D, 0.02D, 0.0D);
        }
    }

    private void renderHotspot() {
        if (endWorld == null || hotspot == null) {
            return;
        }

        Location center = hotspot.center().clone().add(0.0D, hotspotVisualHeight, 0.0D);
        endWorld.spawnParticle(Particle.DRAGON_BREATH, center, hotspotCloudParticleCount, hotspot.radius() / 2.1D, 0.15D, hotspot.radius() / 2.1D, 0.0D);
        endWorld.spawnParticle(Particle.WITCH, center, Math.max(6, hotspotCloudParticleCount / 3), hotspot.radius() / 2.5D, 0.1D, hotspot.radius() / 2.5D, 0.01D);

        for (int index = 0; index < hotspotRingParticlePoints; index++) {
            double angle = (Math.PI * 2.0D * index) / hotspotRingParticlePoints;
            Location point = center.clone().add(Math.cos(angle) * hotspot.radius(), 0.0D, Math.sin(angle) * hotspot.radius());
            endWorld.spawnParticle(Particle.DRAGON_BREATH, point, 2, 0.12D, 0.04D, 0.12D, 0.0D);
        }
    }

    private void landSupplyDrop(SupplyDrop drop) {
        Chicken chicken = Bukkit.getEntity(drop.chickenUuid()) instanceof Chicken entity ? entity : null;
        if (chicken != null) {
            chicken.remove();
        }
        Entity display = Bukkit.getEntity(drop.displayUuid());
        if (display != null) {
            display.remove();
        }

        Block block = drop.targetLocation().getBlock();
        block.setType(Material.ENDER_CHEST, false);
        if (block.getState() instanceof Container container) {
            container.update(true, false);
        }

        SupplyDrop landed = drop.withLanded(true);
        landedSupplyDrops.put(BlockKey.of(block), landed);
        activeSupplyDrop = null;
        plugin.getMessageManager().broadcastPrefixed("supply-drop-landed", Map.of(
                "%x%", Integer.toString(block.getX()),
                "%y%", Integer.toString(block.getY()),
                "%z%", Integer.toString(block.getZ())
        ));
    }

    private void clearSupplyDrop(SupplyDrop drop) {
        Block block = drop.targetLocation().getBlock();
        landedSupplyDrops.remove(BlockKey.of(block));
        if (block.getType() == Material.ENDER_CHEST) {
            block.setType(Material.AIR, false);
        }
    }

    private void fillSupplyDropInventory(Inventory inventory) {
        inventory.clear();
        List<ConfiguredItem> items = new ArrayList<>(supplyDropLoot);
        Collections.shuffle(items);
        int slot = 0;
        for (ConfiguredItem item : items) {
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, item.createStack());
        }
    }

    private void startTrackingEgg(Item item) {
        if (item == null || !item.isValid()) {
            return;
        }

        ItemMeta meta = item.getItemStack().getItemMeta();
        ItemStack updated = item.getItemStack().clone();
        meta = updated.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(trackedEggKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(eggDropSourceKey, PersistentDataType.STRING, trackedEgg != null ? trackedEgg.source().name() : EggSource.GROUND.name());
            updated.setItemMeta(meta);
            item.setItemStack(updated);
        }

        long now = System.currentTimeMillis();
        long pickupLockUntil = isFeatureEnabled(Feature.EGG_DROP_COUNTDOWN)
                ? now + eggDropCountdownSeconds * 1000L
                : now;
        this.trackedEgg = new TrackedEgg(item.getUniqueId(), now, pickupLockUntil, EggSource.GROUND);
        this.nextEggBurnWarningAtMillis = now + 15_000L;

        if (isFeatureEnabled(Feature.EGG_DROP_COUNTDOWN) && eggDropCountdownSeconds > 0) {
            item.setPickupDelay(eggDropCountdownSeconds * 20);
            startEggDropCountdown(item.getLocation());
        }
    }

    private void startEggDropCountdown(Location location) {
        for (int second = eggDropCountdownSeconds; second >= 1; second--) {
            int remaining = second;
            Bukkit.getScheduler().runTaskLater(plugin, () -> announceChatAndTitle(
                    "egg-drop-countdown",
                    "egg-drop-title",
                    "egg-drop-subtitle",
                    Map.of("%seconds%", Integer.toString(remaining))
            ), (eggDropCountdownSeconds - second) * 20L);
        }
    }

    private void spawnCenterEgg() {
        if (endWorld == null) {
            return;
        }
        Item item = endWorld.dropItem(endCenter.clone(), new ItemStack(plugin.getPluginConfig().getEggMaterial()));
        item.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        plugin.getEggHolderService().syncHolderFromInventory(null);
        startTrackingEgg(item);
    }

    private void buildStartPortal() {
        if (overworld == null || overworldPortalCenter == null) {
            return;
        }

        restoreStartPortalArea();
        startPortalSnapshot.clear();

        int baseX = overworldPortalCenter.getBlockX();
        int baseY = overworldPortalCenter.getBlockY();
        int baseZ = overworldPortalCenter.getBlockZ();

        for (int x = -startPortalRadius; x <= startPortalRadius; x++) {
            for (int z = -startPortalRadius; z <= startPortalRadius; z++) {
                Block block = overworld.getBlockAt(baseX + x, baseY, baseZ + z);
                startPortalSnapshot.put(BlockKey.of(block), block.getBlockData().clone());
                boolean edge = Math.abs(x) == startPortalRadius || Math.abs(z) == startPortalRadius;
                block.setType(edge ? Material.OBSIDIAN : Material.END_PORTAL, false);
            }
        }
    }

    private void restoreStartPortalArea() {
        if (overworld == null || startPortalSnapshot.isEmpty()) {
            return;
        }

        for (Map.Entry<BlockKey, BlockData> entry : new ArrayList<>(startPortalSnapshot.entrySet())) {
            BlockKey key = entry.getKey();
            if (!overworld.getName().equals(key.worldName())) {
                continue;
            }

            overworld.getBlockAt(key.x(), key.y(), key.z()).setBlockData(entry.getValue(), false);
        }
        startPortalSnapshot.clear();
    }

    private void closePluginInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory topInventory = player.getOpenInventory().getTopInventory();
            if (topInventory == null) {
                continue;
            }

            InventoryHolder holder = topInventory.getHolder();
            if (holder instanceof TeamService.TeamMenuHolder
                    || holder instanceof KitService.KitMenuHolder
                    || holder instanceof DeadPlayerService.TeleportMenuHolder
                    || holder instanceof AudienceMenuHolder
                    || holder instanceof SupplyDropHolder) {
                player.closeInventory();
            }
        }
    }

    private void assignTeamSpawnLocations() {
        teamSpawnLocations.clear();
        List<TeamService.EndWarTeam> teams = new ArrayList<>(teamService.getTeams());
        teams.sort(Comparator.comparing(TeamService.EndWarTeam::name, String.CASE_INSENSITIVE_ORDER));
        if (teams.isEmpty() || endWorld == null) {
            return;
        }

        List<Location> claimedSpawns = new ArrayList<>();
        double minDistance = Math.max(24.0D, Math.min(52.0D, teamScatterRadius / 2.0D));
        for (TeamService.EndWarTeam team : teams) {
            Location safe = findRandomTeamSpawn(claimedSpawns, minDistance);
            buildSpawnPlatform(safe);
            teamSpawnLocations.put(team.id(), safe.clone().add(0.5D, 1.0D, 0.5D));
            claimedSpawns.add(safe.clone());
        }
    }

    private final Map<UUID, Location> teamSpawnLocations = new HashMap<>();

    private void teleportPlayerToTeamSpawn(Player player) {
        if (player == null || !player.isOnline() || endWorld == null) {
            return;
        }

        UUID teamId = teamService.getTeamId(player);
        Location target = teamId == null ? endCenter.clone() : teamSpawnLocations.get(teamId);
        if (target == null) {
            target = endCenter.clone();
        }

        matchState = MatchState.RUNNING;
        player.teleport(target);
        entryProtectionUntil.put(player.getUniqueId(), System.currentTimeMillis() + entryLockSeconds * 1000L);
        kitService.applySelectedKit(player);
        sidebarService.refresh(player);
        plugin.getMessageManager().sendPrefixed(player, "entered-end", Map.of(
                "%seconds%", Integer.toString(entryLockSeconds),
                "%x%", Integer.toString(target.getBlockX()),
                "%y%", Integer.toString(target.getBlockY()),
                "%z%", Integer.toString(target.getBlockZ())
        ));
    }

    private void buildSpawnPlatform(Location center) {
        if (center.getWorld() == null) {
            return;
        }

        int cx = center.getBlockX();
        int cy = center.getBlockY() - 1;
        int cz = center.getBlockZ();
        for (int x = -portalPlatformRadius; x <= portalPlatformRadius; x++) {
            for (int z = -portalPlatformRadius; z <= portalPlatformRadius; z++) {
                center.getWorld().getBlockAt(cx + x, cy, cz + z).setType(Material.END_STONE, false);
                center.getWorld().getBlockAt(cx + x, cy - 1, cz + z).setType(Material.OBSIDIAN, false);
            }
        }
    }

    private Location findRandomTeamSpawn(List<Location> claimedSpawns, double minDistance) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double minRadius = Math.max(centerNoBuildRadius + 12.0D, teamScatterRadius * 0.55D);
        double maxRadius = Math.max(minRadius + 8.0D, teamScatterRadius);

        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
            double radius = random.nextDouble(minRadius, maxRadius);
            Location candidate = endCenter.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            Location safe = findGround(candidate);
            safe.setY(Math.max(endCenter.getY(), safe.getY()));
            if (!isTooCloseToExistingSpawn(safe, claimedSpawns, minDistance)) {
                return safe;
            }
        }

        double fallbackAngle = claimedSpawns.size() * (Math.PI * 0.61803398875D * 2.0D);
        Location fallback = endCenter.clone().add(Math.cos(fallbackAngle) * maxRadius, 0.0D, Math.sin(fallbackAngle) * maxRadius);
        Location safeFallback = findGround(fallback);
        safeFallback.setY(Math.max(endCenter.getY(), safeFallback.getY()));
        return safeFallback;
    }

    private boolean isTooCloseToExistingSpawn(Location candidate, List<Location> claimedSpawns, double minDistance) {
        double minDistanceSquared = minDistance * minDistance;
        for (Location existing : claimedSpawns) {
            if (existing != null && existing.getWorld() != null && existing.getWorld().equals(candidate.getWorld())
                    && existing.distanceSquared(candidate) < minDistanceSquared) {
                return true;
            }
        }
        return false;
    }

    private void spawnObjectiveCrystals() {
        crystalObjectives.clear();
        if (!isFeatureEnabled(Feature.END_CRYSTAL_OBJECTIVES) || endWorld == null || isDragonFightActive()) {
            return;
        }

        int captureSeconds = Math.max(5, plugin.getConfig().getInt("features.end-crystal-objectives.capture-seconds", 10));
        double captureRadius = Math.max(3.0D, plugin.getConfig().getDouble("features.end-crystal-objectives.capture-radius", 8.0D));
        for (Location location : configuredObjectiveLocations) {
            if (location.getWorld() == null) {
                location.setWorld(endWorld);
            }

            EnderCrystal crystal = endWorld.spawn(location.clone(), EnderCrystal.class, entity -> {
                entity.setInvulnerable(true);
                entity.setShowingBottom(true);
                entity.setBeamTarget(null);
            });
            crystalObjectives.put(BlockKey.of(location.getBlock()), new CrystalObjective(location.clone(), captureRadius, captureSeconds, crystal.getUniqueId()));
        }
    }

    private Inventory createAudienceMenu(Player viewer) {
        Inventory inventory = Bukkit.createInventory(new AudienceMenuHolder(viewer.getUniqueId()), 27, TextUtil.component("&8Audience Tools"));
        int slot = 10;
        for (AudienceEffect effect : AudienceEffect.values()) {
            if (slot > 16) {
                break;
            }
            inventory.setItem(slot++, createAudienceVoteItem(effect));
        }
        return inventory;
    }

    private ItemStack createAudienceVoteItem(AudienceEffect effect) {
        ItemStack stack = new ItemStack(effect.material());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            TextUtil.applyItemText(meta, effect.displayName(), effect.lore());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(eggDropSourceKey, PersistentDataType.STRING, effect.id());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void triggerAudienceEffect(AudienceEffect effect) {
        switch (effect) {
            case STORM_BURST -> triggerEndStorm(true);
            case SUPPLY_DROP -> triggerSupplyDrop(true);
            case HOTSPOT_ROTATION -> rotateHotspot(true);
            case HOLDER_REVEAL -> announceChatAndTitle("audience-holder-reveal", "audience-holder-reveal-title", "audience-holder-reveal-subtitle", Map.of("%player%", plugin.getEggHolderService().getCurrentHolderName() == null ? "None" : plugin.getEggHolderService().getCurrentHolderName()));
            case TEAM_HEAL -> {
                Player holder = plugin.getEggHolderService().getCurrentHolderPlayer();
                if (holder != null) {
                    applyPotionEffects(holder, audienceHealEffects);
                }
            }
        }

        plugin.getMessageManager().broadcastPrefixed("audience-effect-triggered", Map.of("%effect%", effect.displayName()));
    }

    private void applyPotionEffects(Player player, List<ConfiguredPotionEffect> effects) {
        if (player == null) {
            return;
        }
        for (ConfiguredPotionEffect effect : effects) {
            player.removePotionEffect(effect.type());
            player.addPotionEffect(effect.toBukkitEffect());
        }
    }

    private void runCommands(List<String> commands, Player player) {
        for (String command : commands) {
            String parsed = TextUtil.replacePlaceholders(command, Map.of("%player%", player.getName()));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    private void giveAudienceItemsToDeadPlayers() {
        if (!isFeatureEnabled(Feature.AUDIENCE_TOOLS)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                removeAudienceItem(player);
            }
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getDeadPlayerService().isDead(player)) {
                continue;
            }
            giveAudienceItem(player);
        }
    }

    private void giveAudienceItem(Player player) {
        if (containsAudienceItem(player.getInventory())) {
            return;
        }
        ItemStack existing = player.getInventory().getItem(audienceMenuSlot);
        if (existing == null || existing.getType() == Material.AIR) {
            player.getInventory().setItem(audienceMenuSlot, createAudienceToolItem());
        }
    }

    private void removeAudienceItem(Player player) {
        if (player == null) {
            return;
        }
        removeAudienceItems(player.getInventory());
    }

    private boolean containsAudienceItem(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isAudienceToolItem(inventory.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private void removeAudienceItems(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isAudienceToolItem(inventory.getItem(slot))) {
                inventory.setItem(slot, null);
            }
        }
    }

    private ItemStack createAudienceToolItem() {
        ItemStack stack = new ItemStack(audienceMenuMaterial);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            TextUtil.applyItemText(meta, audienceMenuName, audienceMenuLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(audienceItemKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void announceChatAndTitle(String chatPath, String titlePath, String subtitlePath, Map<String, String> placeholders) {
        PluginConfig config = plugin.getPluginConfig();
        if (config.isAnnouncementChatEnabled()) {
            plugin.getMessageManager().broadcastPrefixed(chatPath, placeholders);
        }
        if (config.isAnnouncementTitlesEnabled()) {
            plugin.getMessageManager().sendTitleToAll(
                    titlePath,
                    subtitlePath,
                    placeholders,
                    config.getAnnouncementTitleFadeIn(),
                    config.getAnnouncementTitleStay(),
                    config.getAnnouncementTitleFadeOut()
            );
        }
    }

    private boolean isEntryProtected(Player player) {
        Long expiresAt = entryProtectionUntil.get(player.getUniqueId());
        return expiresAt != null && expiresAt > System.currentTimeMillis();
    }

    private boolean isInsideCenterRing(Location location) {
        return isGameRunning()
                && isFeatureEnabled(Feature.CENTER_NO_BUILD_RING)
                && isEndWorld(location.getWorld())
                && location.distanceSquared(endCenter) <= centerNoBuildRadius * centerNoBuildRadius;
    }

    private boolean isFeatureEnabled(Feature feature) {
        return runtimeFeatures.getOrDefault(feature, feature.defaultEnabled());
    }

    private void disableRuntimeFeature(Feature feature) {
        switch (feature) {
            case HOTSPOT_ROTATION -> hotspot = null;
            case AUDIENCE_TOOLS -> {
                closeAudienceVote(false);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    removeAudienceItem(player);
                }
            }
            case SUPPLY_DROPS -> {
                if (activeSupplyDrop != null) {
                    cleanupSupplyDrop(activeSupplyDrop);
                    activeSupplyDrop = null;
                }
                for (SupplyDrop drop : new ArrayList<>(landedSupplyDrops.values())) {
                    clearSupplyDrop(drop);
                }
                landedSupplyDrops.clear();
            }
            default -> {
            }
        }
    }

    private void cleanupRuntimeEntities() {
        cleanupTrackedEgg();
        if (activeSupplyDrop != null) {
            cleanupSupplyDrop(activeSupplyDrop);
        }
        for (SupplyDrop drop : new ArrayList<>(landedSupplyDrops.values())) {
            clearSupplyDrop(drop);
        }
        for (CrystalObjective objective : crystalObjectives.values()) {
            Entity entity = objective.crystalUuid() == null ? null : Bukkit.getEntity(objective.crystalUuid());
            if (entity != null) {
                entity.remove();
            }
        }
    }

    private void cleanupTrackedEgg() {
        if (trackedEgg == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(trackedEgg.itemUuid());
        if (entity != null) {
            entity.remove();
        }
    }

    private void cleanupSupplyDrop(SupplyDrop drop) {
        Entity chicken = Bukkit.getEntity(drop.chickenUuid());
        if (chicken != null) {
            chicken.remove();
        }
        Entity display = Bukkit.getEntity(drop.displayUuid());
        if (display != null) {
            display.remove();
        }
    }

    private void cancelTasks() {
        if (mainTask != null) {
            mainTask.cancel();
            mainTask = null;
        }
        if (visualTask != null) {
            visualTask.cancel();
            visualTask = null;
        }
    }

    private void restartTasks() {
        cancelTasks();
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickMain, 20L, 20L);
        visualTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickVisuals, visualUpdateIntervalTicks, visualUpdateIntervalTicks);
    }

    private Material parseMaterial(String rawValue, Material fallback) {
        if (rawValue == null) {
            return fallback;
        }
        Material material = Material.matchMaterial(rawValue.toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void refreshWorldContext(FileConfiguration config) {
        ManagedEndWorldService managedEndWorldService = plugin.getManagedEndWorldService();
        Location configuredCenter = null;
        if (managedEndWorldService != null && managedEndWorldService.isEnabled()) {
            this.endWorld = managedEndWorldService.getManagedWorld();
            configuredCenter = managedEndWorldService.getEndCenter();
        }

        if (this.endWorld == null) {
            String endWorldName = config.getString("game.worlds.end", "world_the_end");
            this.endWorld = Bukkit.getWorld(endWorldName);
        }

        if (configuredCenter != null) {
            this.endCenter = configuredCenter;
        } else {
            this.endCenter = new Location(
                    endWorld,
                    config.getDouble("game.end-center.x", 0.5D),
                    config.getDouble("game.end-center.y", 66.0D),
                    config.getDouble("game.end-center.z", 0.5D)
            );
        }
    }

    private Location resolveOverworldPortalCenter() {
        if (overworld == null) {
            return null;
        }
        int x = plugin.getConfig().getInt("start.portal.x", 0);
        int z = plugin.getConfig().getInt("start.portal.z", 0);
        int y = overworld.getHighestBlockYAt(x, z) + 2;
        return new Location(overworld, x, y, z);
    }

    private Location findGround(Location base) {
        if (endWorld == null) {
            return base;
        }

        int x = base.getBlockX();
        int z = base.getBlockZ();
        int y = endWorld.getHighestBlockYAt(x, z);
        if (y <= endWorld.getMinHeight()) {
            y = endCenter.getBlockY();
        }
        return new Location(endWorld, x + 0.5D, y + 1.0D, z + 0.5D);
    }

    private boolean isEndWorld(World world) {
        return world != null && endWorld != null && world.getName().equals(endWorld.getName());
    }

    private Player resolveAttacker(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private boolean isInventoryEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private double distanceSquared(BlockKey key, Location location) {
        double dx = key.x() + 0.5D - location.getX();
        double dy = key.y() + 0.5D - location.getY();
        double dz = key.z() + 0.5D - location.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private List<ConfiguredPotionEffect> loadEffects(ConfigurationSection section) {
        List<ConfiguredPotionEffect> effects = new ArrayList<>();
        if (section == null) {
            return effects;
        }
        for (Map<?, ?> rawMap : section.getMapList("list")) {
            ConfiguredPotionEffect effect = ConfiguredPotionEffect.fromMap(rawMap);
            if (effect != null) {
                effects.add(effect);
            }
        }
        if (effects.isEmpty()) {
            for (Map<?, ?> rawMap : section.getMapList("")) {
                ConfiguredPotionEffect effect = ConfiguredPotionEffect.fromMap(rawMap);
                if (effect != null) {
                    effects.add(effect);
                }
            }
        }
        return effects;
    }

    private List<ConfiguredItem> loadItems(ConfigurationSection section) {
        List<ConfiguredItem> items = new ArrayList<>();
        if (section == null) {
            return items;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null || !itemSection.getBoolean("enabled", true)) {
                continue;
            }
            items.add(ConfiguredItem.fromSection(itemSection));
        }
        return items;
    }

    private List<Location> resolveObjectiveLocations(ConfigurationSection section) {
        ManagedEndWorldService managedEndWorldService = plugin.getManagedEndWorldService();
        if (managedEndWorldService != null && managedEndWorldService.shouldUsePedestalsForObjectives()) {
            List<Location> managedAnchors = managedEndWorldService.getObjectiveAnchorLocations();
            if (!managedAnchors.isEmpty()) {
                return managedAnchors;
            }
        }
        return loadObjectiveLocations(section);
    }

    private List<Location> loadObjectiveLocations(ConfigurationSection section) {
        List<Location> locations = new ArrayList<>();
        if (section == null || endWorld == null) {
            return locations;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection objective = section.getConfigurationSection(key);
            if (objective == null) {
                continue;
            }
            locations.add(new Location(
                    endWorld,
                    objective.getDouble("x", 0.5D),
                    objective.getDouble("y", endCenter.getY()),
                    objective.getDouble("z", 0.5D)
            ));
        }
        return locations;
    }

    private boolean isDragonFightActive() {
        if (endWorld == null) {
            return false;
        }

        DragonBattle battle = endWorld.getEnderDragonBattle();
        if (battle != null) {
            EnderDragon dragon = battle.getEnderDragon();
            if (dragon != null && dragon.isValid() && !dragon.isDead()) {
                return true;
            }
        }
        return !endWorld.getEntitiesByClass(EnderDragon.class).isEmpty();
    }

    public enum Feature {
        END_STORM("end-storm", "features.end-storm.enabled", true),
        ANTI_CAMP_DETECTOR("anti-camp-detector", "features.anti-camp-detector.enabled", true),
        EGG_DROP_COUNTDOWN("egg-drop-countdown", "features.egg-drop-countdown.enabled", true),
        CLUTCH_STEAL_BONUS("clutch-steal-bonus", "features.clutch-steal-bonus.enabled", true),
        AUDIENCE_TOOLS("audience-tools", "features.audience-tools.enabled", true),
        OVERTIME_WIN_CONDITION("overtime-win-condition", "features.overtime-win-condition.enabled", true),
        REVENGE_MARK("revenge-mark", "features.revenge-mark.enabled", true),
        CENTER_NO_BUILD_RING("center-no-build-ring", "features.center-no-build-ring.enabled", true),
        EGG_BURN_TIMER("egg-burn-timer", "features.egg-burn-timer.enabled", true),
        KILL_CONFIRM_REWARD("kill-confirm-reward", "features.kill-confirm-reward.enabled", true),
        COMBAT_MOMENTUM_BUFF("combat-momentum-buff", "features.combat-momentum-buff.enabled", true),
        EGG_SHOCKWAVE("egg-shockwave", "features.egg-shockwave.enabled", true),
        HOTSPOT_ROTATION("hotspot-rotation", "features.hotspot-rotation.enabled", true),
        END_CRYSTAL_OBJECTIVES("end-crystal-objectives", "features.end-crystal-objectives.enabled", true),
        PANIC_PHASE("panic-phase", "features.panic-phase.enabled", true),
        BRIDGE_COLLAPSE_EVENT("bridge-collapse-event", "features.bridge-collapse-event.enabled", true),
        ANTI_CLEAN_MECHANIC("anti-clean-mechanic", "features.anti-clean-mechanic.enabled", true),
        MERCILESS_ENDGAME("mercyless-endgame", "features.mercyless-endgame.enabled", true),
        NO_ESCAPE_TIMER("no-escape-timer", "features.no-escape-timer.enabled", true),
        SUPPLY_DROPS("supply-drops", "features.supply-drops.enabled", true);

        private final String id;
        private final String configPath;
        private final boolean defaultEnabled;

        Feature(String id, String configPath, boolean defaultEnabled) {
            this.id = id;
            this.configPath = configPath;
            this.defaultEnabled = defaultEnabled;
        }

        public String id() {
            return id;
        }

        public String configPath() {
            return configPath;
        }

        public boolean defaultEnabled() {
            return defaultEnabled;
        }

        public static Feature fromId(String rawId) {
            for (Feature feature : values()) {
                if (feature.id.equalsIgnoreCase(rawId)) {
                    return feature;
                }
            }
            return null;
        }
    }

    public enum MatchState {
        LOBBY,
        PORTAL_OPEN,
        RUNNING,
        OVERTIME,
        MERCILESS,
        ENDED
    }

    public enum AudienceEffect {
        STORM_BURST("storm-burst", Material.LIGHTNING_ROD, "&c&lStorm Burst", List.of("&7Trigger an instant End Storm burst.")),
        SUPPLY_DROP("supply-drop", Material.ENDER_CHEST, "&5&lSupply Drop", List.of("&7Call in a public supply drop.")),
        HOTSPOT_ROTATION("hotspot-rotation", Material.COMPASS, "&d&lHotspot Rotation", List.of("&7Force a new hotspot to appear.")),
        HOLDER_REVEAL("holder-reveal", Material.DRAGON_BREATH, "&6&lHolder Reveal", List.of("&7Reveal the current EggHolder.")),
        TEAM_HEAL("team-heal", Material.GOLDEN_APPLE, "&a&lHolder Heal", List.of("&7Give the holder a short survival burst."));

        private final String id;
        private final Material material;
        private final String displayName;
        private final List<String> lore;

        AudienceEffect(String id, Material material, String displayName, List<String> lore) {
            this.id = id;
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
        }

        public String id() {
            return id;
        }

        public Material material() {
            return material;
        }

        public String displayName() {
            return displayName;
        }

        public List<String> lore() {
            return lore;
        }

        public static AudienceEffect fromId(String rawId) {
            for (AudienceEffect effect : values()) {
                if (effect.id.equalsIgnoreCase(rawId)) {
                    return effect;
                }
            }
            return null;
        }
    }

    private enum EggSource {
        GROUND,
        HOLDER_DEATH,
        REPLACEMENT
    }

    private record AudienceMenuHolder(UUID viewer) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private record BlockKey(String worldName, int x, int y, int z) {
        private static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }
    }

    private record SupplyDropHolder(UUID id) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private record SupplyDrop(UUID chickenUuid, UUID displayUuid, Location targetLocation, Inventory inventory, boolean landed) {
        private SupplyDrop withLanded(boolean landed) {
            return new SupplyDrop(chickenUuid, displayUuid, targetLocation, inventory, landed);
        }
    }

    private record TrackedEgg(UUID itemUuid, long spawnedAtMillis, long pickupLockedUntilMillis, EggSource source) {
    }

    private record Hotspot(Location center, double radius, long expiresAtMillis) {
    }

    private static final class PlayerStats {
        private int kills;
        private int killStreak;

        private int kills() {
            return kills;
        }

        private int killStreak() {
            return killStreak;
        }

        private void incrementKills() {
            kills++;
            killStreak++;
        }

        private void resetStreak() {
            killStreak = 0;
        }
    }

    private static final class DamageContribution {
        private Location anchorLocation;
        private long anchorStartedAtMillis;

        private Location anchorLocation() {
            return anchorLocation == null ? null : anchorLocation.clone();
        }

        private long anchorStartedAtMillis() {
            return anchorStartedAtMillis;
        }

        private void setAnchor(Location location) {
            this.anchorLocation = location == null ? null : location.clone();
        }

        private void setAnchorStartedAt(long anchorStartedAtMillis) {
            this.anchorStartedAtMillis = anchorStartedAtMillis;
        }
    }

    private static final class CrystalObjective {
        private final Location captureLocation;
        private final double captureRadius;
        private final int captureSeconds;
        private final UUID crystalUuid;
        private UUID ownerTeamId;
        private int progressSeconds;

        private CrystalObjective(Location captureLocation, double captureRadius, int captureSeconds, UUID crystalUuid) {
            this.captureLocation = captureLocation;
            this.captureRadius = captureRadius;
            this.captureSeconds = captureSeconds;
            this.crystalUuid = crystalUuid;
        }

        private Location captureLocation() {
            return captureLocation.clone();
        }

        private double captureRadius() {
            return captureRadius;
        }

        private int captureSeconds() {
            return captureSeconds;
        }

        private UUID crystalUuid() {
            return crystalUuid;
        }

        private UUID ownerTeamId() {
            return ownerTeamId;
        }

        private void setOwnerTeamId(UUID ownerTeamId) {
            this.ownerTeamId = ownerTeamId;
        }

        private int progressSeconds() {
            return progressSeconds;
        }

        private void incrementProgress() {
            progressSeconds++;
        }

        private void resetProgress() {
            progressSeconds = 0;
        }
    }

    private static final class AudienceVote {
        private final long expiresAtMillis;
        private final Map<UUID, AudienceEffect> votes = new LinkedHashMap<>();

        private AudienceVote(long expiresAtMillis) {
            this.expiresAtMillis = expiresAtMillis;
        }

        private long expiresAtMillis() {
            return expiresAtMillis;
        }

        private void setVote(UUID voter, AudienceEffect effect) {
            votes.put(voter, effect);
        }

        private AudienceEffect resolveWinner() {
            Map<AudienceEffect, Integer> counts = new EnumMap<>(AudienceEffect.class);
            for (AudienceEffect effect : votes.values()) {
                counts.put(effect, counts.getOrDefault(effect, 0) + 1);
            }
            AudienceEffect best = null;
            int bestCount = -1;
            for (Map.Entry<AudienceEffect, Integer> entry : counts.entrySet()) {
                if (entry.getValue() > bestCount) {
                    best = entry.getKey();
                    bestCount = entry.getValue();
                }
            }
            return best;
        }
    }

    private record ConfiguredPotionEffect(PotionEffectType type, int duration, int amplifier, boolean ambient, boolean particles, boolean icon) {
        private PotionEffect toBukkitEffect() {
            return new PotionEffect(type, duration, amplifier, ambient, particles, icon);
        }

        private static ConfiguredPotionEffect fromMap(Map<?, ?> rawMap) {
            Object typeValue = rawMap.get("type");
            if (typeValue == null) {
                return null;
            }
            PotionEffectType type = TextUtil.parsePotionEffectType(typeValue.toString());
            if (type == null) {
                return null;
            }
            int duration = parseInt(rawMap.get("duration"), 100);
            int amplifier = Math.max(0, parseInt(rawMap.get("amplifier"), 0));
            boolean ambient = parseBoolean(rawMap.get("ambient"), false);
            boolean particles = parseBoolean(rawMap.get("particles"), true);
            boolean icon = parseBoolean(rawMap.get("icon"), true);
            return new ConfiguredPotionEffect(type, duration, amplifier, ambient, particles, icon);
        }
    }

    private record ConfiguredItem(Material material, int amount, String name, List<String> lore, Map<Enchantment, Integer> enchants) {
        private ItemStack createStack() {
            ItemStack stack = new ItemStack(material, amount);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                TextUtil.applyItemText(meta, name, lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                stack.setItemMeta(meta);
            }
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                stack.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
            return stack;
        }

        private static ConfiguredItem fromSection(ConfigurationSection section) {
            Material material = Material.matchMaterial(section.getString("material", "STONE").toUpperCase(Locale.ROOT));
            if (material == null) {
                material = Material.STONE;
            }
            Map<Enchantment, Integer> enchants = new LinkedHashMap<>();
            ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                for (String key : enchantSection.getKeys(false)) {
                    Enchantment enchantment = TextUtil.parseEnchantment(key);
                    if (enchantment != null) {
                        enchants.put(enchantment, Math.max(1, enchantSection.getInt(key, 1)));
                    }
                }
            }
            return new ConfiguredItem(
                    material,
                    Math.max(1, section.getInt("amount", 1)),
                    section.getString("name", "&fLoot"),
                    section.getStringList("lore"),
                    enchants
            );
        }
    }

    private static int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static boolean parseBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return fallback;
    }
}
