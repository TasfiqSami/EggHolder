package me.tasfiq.eggholder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BossBar;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class EggHolderService implements Listener {

    private final EggHolderPlugin plugin;
    private final DeadPlayerService deadPlayerService;
    private final Scoreboard scoreboard;
    private final NamespacedKey roleItemKey;
    private final NamespacedKey roleItemIdKey;
    private final NamespacedKey headDisplayKey;

    private PluginConfig config;
    private Team holderTeam;
    private HolderState currentHolder;
    private PendingHolderLogout pendingHolderLogout;
    private BukkitTask bossBarTask;
    private BukkitTask eggScanTask;
    private BukkitTask headDisplayTask;
    private BukkitTask holderLogoutTask;

    public EggHolderService(EggHolderPlugin plugin, DeadPlayerService deadPlayerService) {
        this.plugin = plugin;
        this.deadPlayerService = deadPlayerService;
        this.config = plugin.getPluginConfig();
        this.roleItemKey = new NamespacedKey(plugin, "role_item");
        this.roleItemIdKey = new NamespacedKey(plugin, "role_item_id");
        this.headDisplayKey = new NamespacedKey(plugin, "holder_head_display");

        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            throw new IllegalStateException("Bukkit scoreboard manager is not available.");
        }
        this.scoreboard = scoreboardManager.getMainScoreboard();
    }

    public void start() {
        cleanupStrayHeadDisplays();
        refreshHolderTeam();
        restartTasks();
        Bukkit.getScheduler().runTask(plugin, () -> syncHolderFromInventory(null));
    }

    public void shutdown() {
        cancelTasks();
        removeHolder(RemovalReason.SHUTDOWN, false);
        cleanupStrayHeadDisplays();
    }

    public void reloadSettings() {
        PendingHolderLogout pendingLogout = pendingHolderLogout;
        cancelHolderLogoutTask();
        this.config = plugin.getPluginConfig();
        refreshHolderTeam();

        if (currentHolder != null) {
            Player player = Bukkit.getPlayer(currentHolder.uuid());
            if (player != null && player.isOnline()) {
                applyHolderVisuals(player);
                applyHolderEffects(player);
            }

            if (currentHolder.bossBar() != null) {
                if (!config.isBossBarEnabled()) {
                    currentHolder.bossBar().removeAll();
                    currentHolder = currentHolder.withBossBar(null);
                } else {
                    currentHolder.bossBar().setColor(config.getBossBarColor());
                    currentHolder.bossBar().setStyle(config.getBossBarStyle());
                    currentHolder.bossBar().setProgress(config.getBossBarProgress());
                }
            } else if (config.isBossBarEnabled() && player != null && player.isOnline()) {
                currentHolder = currentHolder.withBossBar(createBossBar(player));
            }

            if (currentHolder.headDisplay() != null && (!config.isHeadDisplayEnabled() || !currentHolder.headDisplay().isValid())) {
                if (currentHolder.headDisplay().isValid()) {
                    currentHolder.headDisplay().remove();
                }
                currentHolder = currentHolder.withHeadDisplay(null);
            }

            if (config.isHeadDisplayEnabled() && currentHolder.headDisplay() == null && player != null && player.isOnline()) {
                currentHolder = currentHolder.withHeadDisplay(spawnHeadDisplay(player));
            }

            if (currentHolder.headDisplay() != null) {
                updateHeadDisplayAppearance(currentHolder.headDisplay());
                if (player != null && player.isOnline()) {
                    syncHeadDisplayVisibility(player, currentHolder.headDisplay());
                }
            }
        }

        restartTasks();

        if (pendingLogout != null) {
            pendingHolderLogout = pendingLogout;
            if (config.isHolderLogoutTimeoutEnabled()) {
                scheduleHolderLogoutTask(pendingLogout, false);
            } else {
                expirePendingHolderLogout(pendingLogout);
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> syncHolderFromInventory(null));
    }

    public boolean assignHolder(Player player, AssignmentSource source) {
        if (deadPlayerService.isDead(player)) {
            return false;
        }

        if (currentHolder != null && currentHolder.uuid().equals(player.getUniqueId())) {
            return false;
        }

        if (currentHolder != null) {
            removeHolder(RemovalReason.REPLACED, false);
        }

        String entry = player.getName();
        String previousListName = TextUtil.getPlayerListName(player);
        boolean previousGlowing = player.isGlowing();
        Team previousTeam = scoreboard.getEntryTeam(entry);
        String previousTeamName = null;
        if (previousTeam != null && !previousTeam.getName().equals(holderTeam.getName())) {
            previousTeamName = previousTeam.getName();
            previousTeam.removeEntry(entry);
        }

        applyHolderVisuals(player);
        applyHolderEffects(player);
        giveRoleItems(player);

        BossBar bossBar = config.isBossBarEnabled() ? createBossBar(player) : null;
        ItemDisplay headDisplay = config.isHeadDisplayEnabled() ? spawnHeadDisplay(player) : null;

        this.currentHolder = new HolderState(
                player.getUniqueId(),
                player.getName(),
                previousListName,
                previousGlowing,
                previousTeamName,
                bossBar,
                headDisplay
        );

        announceNewHolder(player, source);
        return true;
    }

    public boolean removeHolderByName(String name) {
        if (currentHolder == null) {
            return false;
        }

        return currentHolder.playerName().equalsIgnoreCase(name) && removeHolder(RemovalReason.COMMAND, true);
    }

    public boolean removeHolder(RemovalReason reason, boolean respectRemovalBroadcastConfig) {
        HolderState state = currentHolder;
        if (state == null) {
            return false;
        }

        clearPendingHolderLogout();
        currentHolder = null;

        if (state.bossBar() != null) {
            state.bossBar().removeAll();
        }

        if (state.headDisplay() != null && state.headDisplay().isValid()) {
            state.headDisplay().remove();
        }

        if (holderTeam.hasEntry(state.playerName())) {
            holderTeam.removeEntry(state.playerName());
        }

        if (config.isRestorePreviousTeam() && state.previousTeamName() != null) {
            Team previousTeam = scoreboard.getTeam(state.previousTeamName());
            if (previousTeam != null) {
                previousTeam.addEntry(state.playerName());
            }
        }

        Player player = Bukkit.getPlayer(state.uuid());
        if (player != null && player.isOnline()) {
            TextUtil.setPlayerListName(player, TextUtil.safePlayerListName(state.previousListName(), player.getName()));
            player.setGlowing(state.previousGlowing());
            if (config.isStripEffectsOnRemove()) {
                removeHolderEffects(player);
            }
            if (config.isStripRoleItemsOnRemove()) {
                stripRoleItems(player);
            }
        }

        if (respectRemovalBroadcastConfig && config.isRemovalBroadcastEnabled()) {
            plugin.getMessageManager().broadcastPrefixed(
                    "holder-removed-broadcast",
                    Collections.singletonMap("%player%", state.playerName())
            );
        }

        return true;
    }

    public boolean hasEgg(Player player) {
        return player.getInventory().contains(config.getEggMaterial());
    }

    private boolean isEgg(ItemStack stack) {
        return stack != null && stack.getType() == config.getEggMaterial();
    }

    private boolean isInventoryEggRelevant(Player player, ItemStack primary, ItemStack secondary) {
        return isHolder(player) || isEgg(primary) || isEgg(secondary) || hasEgg(player);
    }

    public boolean isHolder(Player player) {
        return currentHolder != null && currentHolder.uuid().equals(player.getUniqueId());
    }

    public Player getCurrentHolderPlayer() {
        return currentHolder == null ? null : Bukkit.getPlayer(currentHolder.uuid());
    }

    public String getCurrentHolderName() {
        return currentHolder == null ? null : currentHolder.playerName();
    }

    public Collection<String> getAssignableOnlineNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!deadPlayerService.isDead(player) && !isHolder(player)) {
                names.add(player.getName());
            }
        }
        return names;
    }

    public Collection<String> getCurrentHolderNames() {
        if (currentHolder == null) {
            return List.of();
        }
        return List.of(currentHolder.playerName());
    }

    public void syncHolderFromInventory(Player priorityPlayer) {
        if (!config.isAutoDetectEggHolder()) {
            return;
        }

        if (currentHolder != null) {
            if (pendingHolderLogout != null && currentHolder.uuid().equals(pendingHolderLogout.uuid())) {
                return;
            }

            Player player = Bukkit.getPlayer(currentHolder.uuid());
            if (player != null && player.isOnline() && !deadPlayerService.isDead(player) && hasEgg(player)) {
                return;
            }
        }

        if (priorityPlayer != null && priorityPlayer.isOnline() && !deadPlayerService.isDead(priorityPlayer) && hasEgg(priorityPlayer)) {
            assignHolder(priorityPlayer, AssignmentSource.AUTOMATIC);
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (deadPlayerService.isDead(player)) {
                continue;
            }

            if (hasEgg(player)) {
                assignHolder(player, AssignmentSource.AUTOMATIC);
                return;
            }
        }

        if (currentHolder != null && config.isRemoveHolderWhenEggMissing()) {
            removeHolder(RemovalReason.NO_EGG, true);
        }
    }

    private void claimEggBlock(Player player, Block eggBlock) {
        eggBlock.setType(Material.AIR, false);
        player.getInventory().addItem(new ItemStack(config.getEggMaterial()));
        Bukkit.getScheduler().runTask(plugin, () -> syncHolderFromInventory(player));
    }

    public boolean isRoleItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return false;
        }

        Byte marker = itemStack.getItemMeta().getPersistentDataContainer().get(roleItemKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    public void stripRoleItems(Player player) {
        stripRoleItemsFromInventory(player.getInventory());
        stripRoleItemsFromInventory(player.getEnderChest());
    }

    public void refreshHolderStateAfterRevive(Player player) {
        if (currentHolder != null && currentHolder.uuid().equals(player.getUniqueId())) {
            applyHolderVisuals(player);
            applyHolderEffects(player);
            if (config.isHeadDisplayEnabled()) {
                if (currentHolder.headDisplay() == null || !currentHolder.headDisplay().isValid()) {
                    currentHolder = currentHolder.withHeadDisplay(spawnHeadDisplay(player));
                } else {
                    syncHeadDisplayVisibility(player, currentHolder.headDisplay());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (currentHolder != null && currentHolder.bossBar() != null) {
            currentHolder.bossBar().addPlayer(event.getPlayer());
        }

        if (currentHolder != null && currentHolder.uuid().equals(event.getPlayer().getUniqueId()) && config.isHeadDisplayEnabled()) {
            if (currentHolder.headDisplay() == null || !currentHolder.headDisplay().isValid()) {
                currentHolder = currentHolder.withHeadDisplay(spawnHeadDisplay(event.getPlayer()));
            } else {
                syncHeadDisplayVisibility(event.getPlayer(), currentHolder.headDisplay());
            }
        }

        if (pendingHolderLogout != null && pendingHolderLogout.uuid().equals(event.getPlayer().getUniqueId())) {
            Map<String, String> placeholders = Collections.singletonMap("%player%", event.getPlayer().getName());
            clearPendingHolderLogout();
            applyHolderVisuals(event.getPlayer());
            applyHolderEffects(event.getPlayer());
            announceEvent(
                    "holder-reconnected-broadcast",
                    "holder-reconnected-title",
                    "holder-reconnected-subtitle",
                    placeholders
            );
        }

        Bukkit.getScheduler().runTask(plugin, () -> syncHolderFromInventory(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (currentHolder != null && currentHolder.bossBar() != null) {
            currentHolder.bossBar().removePlayer(event.getPlayer());
        }

        if (isHolder(event.getPlayer())) {
            Map<String, String> placeholders = Map.of(
                    "%player%", event.getPlayer().getName(),
                    "%seconds%", Integer.toString(config.getHolderLogoutTimeoutSeconds())
            );
            if (config.isHolderLogoutTimeoutEnabled()) {
                if (currentHolder != null && currentHolder.headDisplay() != null && currentHolder.headDisplay().isValid()) {
                    currentHolder.headDisplay().remove();
                    currentHolder = currentHolder.withHeadDisplay(null);
                }
                scheduleHolderLogoutTask(new PendingHolderLogout(
                        event.getPlayer().getUniqueId(),
                        event.getPlayer().getName(),
                        event.getPlayer().getLocation().clone()
                ), true);
            } else {
                announceEvent(
                        "holder-disconnected-no-grace-broadcast",
                        "holder-disconnected-no-grace-title",
                        "holder-disconnected-no-grace-subtitle",
                        placeholders
                );
                removeHolder(RemovalReason.QUIT, false);
            }
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> syncHolderFromInventory(null));
    }

    @EventHandler
    public void onPlayerPickupEgg(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getItem().getItemStack().getType() == config.getEggMaterial()) {
            Bukkit.getScheduler().runTask(plugin, () -> syncHolderFromInventory(player));
        }
    }

    @EventHandler
    public void onEggBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != config.getEggMaterial()) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        event.setCancelled(true);
        if (deadPlayerService.isDead(player)) {
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            plugin.getMessageManager().sendPrefixed(player, "egg-pickup-no-space");
            return;
        }

        claimEggBlock(player, clickedBlock);
    }

    @EventHandler
    public void onEggBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != config.getEggMaterial()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> syncHolderFromInventory(event.getPlayer()));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!isInventoryEggRelevant(player, event.getCurrentItem(), event.getCursor())) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> syncHolderFromInventory(player));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!isInventoryEggRelevant(player, event.getOldCursor(), event.getCursor())) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> syncHolderFromInventory(player));
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();

        if (item.getItemStack().getType() == config.getEggMaterial()) {
            applyDroppedEggVisuals(item);
            Bukkit.getScheduler().runTask(plugin, () -> syncHolderFromInventory(null));
        }
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        if (!config.isAdvancementTriggerEnabled()) {
            return;
        }

        if (event.getAdvancement().getKey().equals(config.getAdvancementKey())) {
            Bukkit.getScheduler().runTask(plugin, () -> syncHolderFromInventory(event.getPlayer()));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        boolean wasHolder = isHolder(player);

        if (config.isPreventRoleItemDrops()) {
            event.getDrops().removeIf(this::isRoleItem);
        }

        if (!wasHolder) {
            return;
        }

        Player killer = player.getKiller();
        if (config.isKillBroadcastEnabled()) {
            if (killer != null) {
                announceEvent(
                        "holder-killed-broadcast",
                        "holder-killed-title",
                        "holder-killed-subtitle",
                        Map.of(
                                "%killer%", killer.getName(),
                                "%player%", player.getName()
                        )
                );
            } else {
                announceEvent(
                        "holder-died-broadcast",
                        "holder-died-title",
                        "holder-died-subtitle",
                        Collections.singletonMap("%player%", player.getName())
                );
            }
        }

        Location deathLocation = player.getLocation().clone();
        boolean spawnReplacementEgg = shouldSpawnReplacementEggAfterDeath(player);
        removeHolder(RemovalReason.DEATH, false);
        Bukkit.getScheduler().runTask(plugin, () -> {
            highlightDroppedEggs(deathLocation);
            if (spawnReplacementEgg) {
                spawnReplacementEgg(ReplacementEggReason.VOID_DEATH);
            }
        });
    }

    private void refreshHolderTeam() {
        Team previousTeam = this.holderTeam;
        Team team = scoreboard.getTeam(config.getHolderTeamId());
        if (team == null) {
            team = scoreboard.registerNewTeam(config.getHolderTeamId());
        }
        this.holderTeam = team;
        applyHolderTeamSettings(team);

        if (previousTeam != null && !previousTeam.getName().equals(team.getName()) && currentHolder != null && previousTeam.hasEntry(currentHolder.playerName())) {
            previousTeam.removeEntry(currentHolder.playerName());
            team.addEntry(currentHolder.playerName());
        }

        for (String entry : new ArrayList<>(team.getEntries())) {
            if (currentHolder == null || !entry.equalsIgnoreCase(currentHolder.playerName())) {
                team.removeEntry(entry);
            }
        }
    }

    private void restartTasks() {
        cancelTasks();

        if (config.isBossBarEnabled()) {
            bossBarTask = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    this::updateBossBar,
                    0L,
                    config.getBossBarUpdateIntervalTicks()
            );
        }

        if (config.isHeadDisplayEnabled()) {
            headDisplayTask = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    this::updateHeadDisplay,
                    0L,
                    config.getHeadDisplayUpdateIntervalTicks()
            );
        }

        if (config.isAutoDetectEggHolder()) {
            eggScanTask = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    () -> syncHolderFromInventory(null),
                    config.getAutoDetectIntervalTicks(),
                    config.getAutoDetectIntervalTicks()
            );
        }
    }

    private void cancelTasks() {
        if (bossBarTask != null) {
            bossBarTask.cancel();
            bossBarTask = null;
        }
        if (eggScanTask != null) {
            eggScanTask.cancel();
            eggScanTask = null;
        }
        if (headDisplayTask != null) {
            headDisplayTask.cancel();
            headDisplayTask = null;
        }

        cancelHolderLogoutTask();
    }

    private void cancelHolderLogoutTask() {
        if (holderLogoutTask != null) {
            holderLogoutTask.cancel();
            holderLogoutTask = null;
        }
    }

    private void clearPendingHolderLogout() {
        pendingHolderLogout = null;
        cancelHolderLogoutTask();
    }

    private void scheduleHolderLogoutTask(PendingHolderLogout pendingLogout, boolean broadcastDisconnect) {
        clearPendingHolderLogout();
        pendingHolderLogout = pendingLogout;

        if (broadcastDisconnect) {
            announceEvent(
                    "holder-disconnected-broadcast",
                    "holder-disconnected-title",
                    "holder-disconnected-subtitle",
                    Map.of(
                            "%player%", pendingLogout.playerName(),
                            "%seconds%", Integer.toString(config.getHolderLogoutTimeoutSeconds())
                    )
            );
        }

        holderLogoutTask = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> expirePendingHolderLogout(pendingLogout),
                config.getHolderLogoutTimeoutTicks()
        );
    }

    private void expirePendingHolderLogout(PendingHolderLogout pendingLogout) {
        if (pendingLogout == null || currentHolder == null || !currentHolder.uuid().equals(pendingLogout.uuid())) {
            clearPendingHolderLogout();
            return;
        }

        Player onlinePlayer = Bukkit.getPlayer(pendingLogout.uuid());
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            clearPendingHolderLogout();
            return;
        }

        pendingHolderLogout = null;
        holderLogoutTask = null;

        if (!removeHolder(RemovalReason.LOGOUT_TIMEOUT, false)) {
            return;
        }

        deadPlayerService.markEliminated(pendingLogout.uuid(), pendingLogout.playerName(), pendingLogout.location());
        announceEvent(
                "holder-offline-eliminated-broadcast",
                "holder-offline-eliminated-title",
                "holder-offline-eliminated-subtitle",
                Map.of(
                        "%player%", pendingLogout.playerName(),
                        "%seconds%", Integer.toString(config.getHolderLogoutTimeoutSeconds())
                )
        );

        boolean eggSpawned = true;
        if (config.isHolderLogoutSpawnReplacementEgg()) {
            eggSpawned = spawnReplacementEgg(ReplacementEggReason.LOGOUT_TIMEOUT);
        }

        if (!eggSpawned) {
            plugin.getLogger().warning("Replacement dragon egg could not be spawned after the EggHolder logout timeout expired.");
        }
    }

    private boolean spawnReplacementEgg(ReplacementEggReason reason) {
        Location spawnLocation = config.createHolderLogoutRespawnLocation();
        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            return false;
        }

        spawnLocation.getChunk().load();
        Item eggItem = spawnLocation.getWorld().dropItem(spawnLocation, new ItemStack(config.getEggMaterial()));
        eggItem.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        applyDroppedEggVisuals(eggItem);
        announceReplacementEggSpawned(spawnLocation, reason);
        return true;
    }

    private void updateBossBar() {
        if (currentHolder == null || currentHolder.bossBar() == null) {
            return;
        }

        Player player = Bukkit.getPlayer(currentHolder.uuid());
        if (player == null || !player.isOnline()) {
            return;
        }

        currentHolder.bossBar().setTitle(config.formatBossBarTitle(player));
        currentHolder.bossBar().setProgress(config.getBossBarProgress());
    }

    private void updateHeadDisplay() {
        if (currentHolder == null || currentHolder.headDisplay() == null) {
            return;
        }

        Player player = Bukkit.getPlayer(currentHolder.uuid());
        ItemDisplay headDisplay = currentHolder.headDisplay();
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!headDisplay.isValid()) {
            currentHolder = currentHolder.withHeadDisplay(spawnHeadDisplay(player));
            return;
        }

        Location targetLocation = createHeadDisplayLocation(player);
        Location currentLocation = headDisplay.getLocation();
        if (currentLocation.getWorld() != targetLocation.getWorld()
                || currentLocation.distanceSquared(targetLocation) > 0.0004D) {
            headDisplay.teleport(targetLocation);
        }
    }

    private void applyHolderVisuals(Player player) {
        holderTeam.addEntry(player.getName());
        TextUtil.setPlayerListName(player, config.formatHolderTabName(player));
        if (config.isHolderGlowEnabled()) {
            player.setGlowing(true);
        }
    }

    private void applyHolderEffects(Player player) {
        for (PluginConfig.ConfiguredPotionEffectDefinition definition : config.getHolderPotionEffects()) {
            player.removePotionEffect(definition.getType());
            player.addPotionEffect(definition.createEffect());
        }
    }

    private void removeHolderEffects(Player player) {
        for (PluginConfig.ConfiguredPotionEffectDefinition definition : config.getHolderPotionEffects()) {
            player.removePotionEffect(definition.getType());
        }
    }

    private BossBar createBossBar(Player player) {
        BossBar bossBar = Bukkit.createBossBar(
                config.formatBossBarTitle(player),
                config.getBossBarColor(),
                config.getBossBarStyle()
        );
        bossBar.setProgress(config.getBossBarProgress());
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(onlinePlayer);
        }
        return bossBar;
    }

    private ItemDisplay spawnHeadDisplay(Player player) {
        ItemDisplay display = player.getWorld().spawn(createHeadDisplayLocation(player), ItemDisplay.class, itemDisplay -> {
            itemDisplay.setPersistent(false);
            itemDisplay.setInvulnerable(true);
            itemDisplay.setGravity(false);
            itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            itemDisplay.setBillboard(Display.Billboard.FIXED);
            itemDisplay.getPersistentDataContainer().set(headDisplayKey, PersistentDataType.BYTE, (byte) 1);
        });

        updateHeadDisplayAppearance(display);
        syncHeadDisplayVisibility(player, display);
        return display;
    }

    private void cleanupStrayHeadDisplays() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                Byte marker = display.getPersistentDataContainer().get(headDisplayKey, PersistentDataType.BYTE);
                if (marker != null && marker == (byte) 1) {
                    display.remove();
                }
            }
        }
    }

    private Location createHeadDisplayLocation(Player player) {
        Location location = player.getLocation().clone().add(0.0D, config.getHeadDisplayYOffset(), 0.0D);
        location.setYaw(0.0F);
        location.setPitch(0.0F);
        return location;
    }

    private void updateHeadDisplayAppearance(ItemDisplay display) {
        if (display == null || !display.isValid()) {
            return;
        }

        float scale = (float) config.getHeadDisplayScale();
        display.setItemStack(config.createHeadDisplayItem());
        display.setInterpolationDuration(config.getHeadDisplayInterpolationDurationTicks());
        display.setTeleportDuration(config.getHeadDisplayTeleportDurationTicks());
        display.setInterpolationDelay(config.getHeadDisplayInterpolationDelayTicks());
        display.setViewRange(config.getHeadDisplayViewRange());
        display.setShadowRadius(0.0F);
        display.setShadowStrength(0.0F);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf()
        ));
    }

    private void syncHeadDisplayVisibility(Player holder, ItemDisplay display) {
        if (holder == null || display == null || !display.isValid()) {
            return;
        }

        if (config.isHeadDisplayHideFromHolder()) {
            holder.hideEntity(plugin, display);
        } else {
            holder.showEntity(plugin, display);
        }
    }

    private void applyHolderTeamSettings(Team team) {
        team.prefix(TextUtil.component(config.getHolderNametagPrefix()));
        team.color(config.getHolderGlowColor());
        team.setCanSeeFriendlyInvisibles(false);
    }

    private void giveRoleItems(Player player) {
        for (PluginConfig.HolderItemDefinition definition : config.getHolderItems()) {
            if (!definition.isEnabled()) {
                continue;
            }

            ItemStack item = definition.createItem(roleItemKey, roleItemIdKey);
            if (tryPlacePreferredSlot(player.getInventory(), definition.getSlot(), item)) {
                continue;
            }

            java.util.Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
            if (leftovers.isEmpty()) {
                continue;
            }

            if (config.isStoreOverflowInEnderChest()) {
                leftovers = player.getEnderChest().addItem(leftovers.values().toArray(new ItemStack[0]));
            }

            if (!leftovers.isEmpty()) {
                plugin.getMessageManager().sendPrefixed(
                        player,
                        "item-overflow",
                        Collections.singletonMap("%item%", definition.getDisplayNamePlain())
                );
            }
        }
    }

    private boolean tryPlacePreferredSlot(Inventory inventory, int slot, ItemStack item) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return false;
        }

        ItemStack current = inventory.getItem(slot);
        if (current == null || current.getType() == Material.AIR) {
            inventory.setItem(slot, item);
            return true;
        }

        return false;
    }

    private void stripRoleItemsFromInventory(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isRoleItem(item)) {
                inventory.setItem(slot, null);
            }
        }
    }

    private void announceNewHolder(Player player, AssignmentSource source) {
        Map<String, String> placeholders = Map.of(
                "%player%", player.getName(),
                "%source%", source.name().toLowerCase(Locale.ROOT)
        );
        announceEvent(
                "holder-announced-broadcast",
                config.isHolderTitleEnabled() ? "holder-title" : null,
                config.isHolderTitleEnabled() ? "holder-subtitle" : null,
                placeholders
        );
    }

    private void applyDroppedEggVisuals(Item item) {
        if (config.isDroppedEggGlowEnabled()) {
            item.setGlowing(true);
        }

        String customName = config.getDroppedEggCustomName();
        if (!customName.isBlank()) {
            item.customName(TextUtil.component(customName));
            item.setCustomNameVisible(config.isDroppedEggCustomNameVisible());
        }
    }

    private void highlightDroppedEggs(Location location) {
        double radius = 4.0D;
        for (Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
            if (entity instanceof Item item && item.getItemStack().getType() == config.getEggMaterial()) {
                applyDroppedEggVisuals(item);
            }
        }
    }

    private boolean shouldSpawnReplacementEggAfterDeath(Player player) {
        if (!config.isSpawnReplacementEggOnVoidDeath()) {
            return false;
        }

        EntityDamageEvent lastDamageCause = player.getLastDamageCause();
        return lastDamageCause != null && lastDamageCause.getCause() == EntityDamageEvent.DamageCause.VOID;
    }

    private void announceReplacementEggSpawned(Location spawnLocation, ReplacementEggReason reason) {
        long titleDelayTicks = switch (reason) {
            case LOGOUT_TIMEOUT, VOID_DEATH -> config.getAnnouncementFollowUpTitleDelayTicks();
        };

        announceEvent(
                "replacement-egg-spawned-broadcast",
                "replacement-egg-spawned-title",
                "replacement-egg-spawned-subtitle",
                createLocationPlaceholders(spawnLocation),
                titleDelayTicks
        );
    }

    private void announceEvent(
            String chatMessagePath,
            String titlePath,
            String subtitlePath,
            Map<String, String> placeholders
    ) {
        announceEvent(chatMessagePath, titlePath, subtitlePath, placeholders, 0L);
    }

    private void announceEvent(
            String chatMessagePath,
            String titlePath,
            String subtitlePath,
            Map<String, String> placeholders,
            long titleDelayTicks
    ) {
        if (config.isAnnouncementChatEnabled() && chatMessagePath != null && !chatMessagePath.isBlank()) {
            plugin.getMessageManager().broadcastPrefixed(chatMessagePath, placeholders);
        }

        if (!config.isAnnouncementTitlesEnabled() || titlePath == null || subtitlePath == null) {
            return;
        }

        Runnable sendTitle = () -> plugin.getMessageManager().sendTitleToAll(
                titlePath,
                subtitlePath,
                placeholders,
                config.getAnnouncementTitleFadeIn(),
                config.getAnnouncementTitleStay(),
                config.getAnnouncementTitleFadeOut()
        );

        if (titleDelayTicks > 0L) {
            Bukkit.getScheduler().runTaskLater(plugin, sendTitle, titleDelayTicks);
            return;
        }

        sendTitle.run();
    }

    private Map<String, String> createLocationPlaceholders(Location location) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("%world%", formatWorldName(location));
        placeholders.put("%x%", Integer.toString(location.getBlockX()));
        placeholders.put("%y%", Integer.toString(location.getBlockY()));
        placeholders.put("%z%", Integer.toString(location.getBlockZ()));
        return placeholders;
    }

    private String formatWorldName(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }

        String worldName = location.getWorld().getName().replace('_', ' ');
        if (worldName.isEmpty()) {
            return "unknown";
        }

        return Character.toUpperCase(worldName.charAt(0)) + worldName.substring(1);
    }

    public enum AssignmentSource {
        COMMAND,
        AUTOMATIC
    }

    public enum RemovalReason {
        COMMAND,
        DEATH,
        NO_EGG,
        REPLACED,
        QUIT,
        LOGOUT_TIMEOUT,
        SHUTDOWN
    }

    private enum ReplacementEggReason {
        VOID_DEATH,
        LOGOUT_TIMEOUT
    }

    private record HolderState(
            UUID uuid,
            String playerName,
            String previousListName,
            boolean previousGlowing,
            String previousTeamName,
            BossBar bossBar,
            ItemDisplay headDisplay
    ) {
        private HolderState withBossBar(BossBar updatedBossBar) {
            return new HolderState(uuid, playerName, previousListName, previousGlowing, previousTeamName, updatedBossBar, headDisplay);
        }

        private HolderState withHeadDisplay(ItemDisplay updatedHeadDisplay) {
            return new HolderState(uuid, playerName, previousListName, previousGlowing, previousTeamName, bossBar, updatedHeadDisplay);
        }
    }

    private record PendingHolderLogout(
            UUID uuid,
            String playerName,
            Location location
    ) {
        private PendingHolderLogout {
            location = location == null ? null : location.clone();
        }

        @Override
        public Location location() {
            return location == null ? null : location.clone();
        }
    }
}
