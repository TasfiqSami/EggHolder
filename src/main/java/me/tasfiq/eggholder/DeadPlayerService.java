package me.tasfiq.eggholder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public final class DeadPlayerService implements Listener {

    private final EggHolderPlugin plugin;
    private final NamespacedKey teleporterItemKey;
    private final NamespacedKey teleporterTargetKey;

    private PluginConfig config;
    private final Map<UUID, DeadPlayerState> deadPlayers = new LinkedHashMap<>();
    private final Map<UUID, Location> pendingReviveLocations = new LinkedHashMap<>();

    public DeadPlayerService(EggHolderPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
        this.teleporterItemKey = new NamespacedKey(plugin, "dead_teleporter");
        this.teleporterTargetKey = new NamespacedKey(plugin, "teleport_target");
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            DeadPlayerState state = deadPlayers.get(player.getUniqueId());
            if (state != null) {
                restoreAliveState(player, state);
            }
        }
        deadPlayers.clear();
        pendingReviveLocations.clear();
        showEveryoneToEveryone();
        refreshVisibilityForAll();
    }

    public void reloadSettings() {
        this.config = plugin.getPluginConfig();

        if (!config.isDeadStateEnabled()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                DeadPlayerState state = deadPlayers.get(player.getUniqueId());
                if (state != null) {
                    restoreAliveState(player, state);
                }
            }
            deadPlayers.clear();
            pendingReviveLocations.clear();
            showEveryoneToEveryone();
            refreshVisibilityForAll();
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isDead(player)) {
                applyDeadState(player);
            }
        }
        refreshVisibilityForAll();
    }

    public boolean isDead(Player player) {
        return isDead(player.getUniqueId());
    }

    public boolean isDead(UUID uuid) {
        return deadPlayers.containsKey(uuid);
    }

    public Collection<String> getKnownDeadNames() {
        List<String> names = new ArrayList<>();
        for (DeadPlayerState state : deadPlayers.values()) {
            names.add(state.playerName());
        }
        return names;
    }

    public Collection<Player> getAliveOnlinePlayers() {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isDead(player)) {
                players.add(player);
            }
        }
        return players;
    }

    public boolean reviveByName(String name) {
        UUID targetUuid = null;
        for (Map.Entry<UUID, DeadPlayerState> entry : deadPlayers.entrySet()) {
            if (entry.getValue().playerName().equalsIgnoreCase(name)) {
                targetUuid = entry.getKey();
                break;
            }
        }

        if (targetUuid == null) {
            return false;
        }

        DeadPlayerState state = deadPlayers.remove(targetUuid);
        if (state == null) {
            return false;
        }

        Player onlinePlayer = Bukkit.getPlayer(targetUuid);
        Location reviveLocation = resolveReviveLocation(state);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            restoreAliveState(onlinePlayer, state);
            if (reviveLocation != null) {
                onlinePlayer.teleport(reviveLocation);
            }
            resetRevivedPlayerState(onlinePlayer);
            refreshVisibilityForAll();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getEggHolderService().syncHolderFromInventory(onlinePlayer));
            return true;
        }

        if (reviveLocation != null) {
            pendingReviveLocations.put(targetUuid, reviveLocation);
        }
        return true;
    }

    public void markEliminated(UUID uuid, String playerName, Location deathLocation) {
        if (uuid == null) {
            return;
        }

        pendingReviveLocations.remove(uuid);
        deadPlayers.put(uuid, DeadPlayerState.offline(uuid, playerName, deathLocation));

        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> applyDeadState(onlinePlayer));
        }
    }

    public boolean isTeleporterItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return false;
        }

        Byte marker = itemStack.getItemMeta().getPersistentDataContainer().get(teleporterItemKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    public NamespacedKey getTeleporterTargetKey() {
        return teleporterTargetKey;
    }

    public Inventory createTeleporterMenu(Player viewer) {
        List<Player> targets = new ArrayList<>();
        for (Player player : getAliveOnlinePlayers()) {
            if (!player.getUniqueId().equals(viewer.getUniqueId())) {
                targets.add(player);
            }
        }

        int size = Math.max(9, ((targets.size() - 1) / 9 + 1) * 9);
        size = Math.min(size, 54);

        Inventory inventory = Bukkit.createInventory(
                new TeleportMenuHolder(viewer.getUniqueId()),
                size,
                TextUtil.component(config.formatTeleporterMenuTitle())
        );

        int slot = 0;
        for (Player target : targets) {
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, createTeleportTargetItem(target));
        }

        return inventory;
    }

    public void refreshVisibilityForAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            applyVisibilityForViewer(viewer);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!config.isDeadStateEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        DeadPlayerState state = DeadPlayerState.fresh(player, player.getLocation().clone());
        state.capture(player);
        deadPlayers.put(player.getUniqueId(), state);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!config.isDeadStateEnabled()) {
            return;
        }

        if (deadPlayers.containsKey(event.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> applyDeadState(event.getPlayer()));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (pendingReviveLocations.containsKey(uuid)) {
            Location reviveLocation = pendingReviveLocations.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                restorePendingOfflineReviveState(player);
                player.teleport(reviveLocation);
                resetRevivedPlayerState(player);
                refreshVisibilityForAll();
                plugin.getEggHolderService().syncHolderFromInventory(player);
            });
            return;
        }

        if (deadPlayers.containsKey(uuid)) {
            deadPlayers.get(uuid).setPlayerName(player.getName());
            Bukkit.getScheduler().runTask(plugin, () -> applyDeadState(player));
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> applyVisibilityForViewer(player));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        DeadPlayerState state = deadPlayers.get(event.getPlayer().getUniqueId());
        if (state != null) {
            state.setPlayerName(event.getPlayer().getName());
        }
    }

    @EventHandler
    public void onDeadPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isDead(player) && config.isDeadStateInvulnerable()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeadPlayerFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && isDead(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeadPlayerBreak(BlockBreakEvent event) {
        if (isDead(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeadPlayerPlace(BlockPlaceEvent event) {
        if (isDead(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeadPlayerPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && isDead(player) && !config.isDeadStateCanPickupItems()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeadPlayerDrop(PlayerDropItemEvent event) {
        if (isDead(event.getPlayer()) && !config.isDeadStateCanDropItems()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeadPlayerSwapHands(PlayerSwapHandItemsEvent event) {
        if (isDead(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeadPlayerInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && isDead(player)
                && !config.isDeadStateCanModifyInventory()
                && !isPluginDeadMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeadPlayerInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && isDead(player)
                && !config.isDeadStateCanModifyInventory()
                && !isPluginDeadMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeadPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isDead(player)) {
            return;
        }

        ItemStack item = event.getItem();
        if (isTeleporterItem(item)) {
            return;
        }

        if (plugin.getEndWarService() != null && plugin.getEndWarService().isAudienceToolItem(item)) {
            return;
        }

        event.setCancelled(true);
    }

    private void applyDeadState(Player player) {
        if (!config.isDeadStateEnabled()) {
            return;
        }

        DeadPlayerState state = deadPlayers.get(player.getUniqueId());
        if (state == null) {
            return;
        }

        if (!state.initialized()) {
            state.capture(player);
        }

        player.setGameMode(config.getDeadStateGameMode());
        player.setAllowFlight(config.isDeadStateAllowFlight());
        player.setFlying(config.isDeadStateAllowFlight() && config.isDeadStateStartFlying());
        player.setInvulnerable(config.isDeadStateInvulnerable());
        player.setCollidable(false);
        player.setCanPickupItems(config.isDeadStateCanPickupItems());
        TextUtil.setPlayerListName(player, config.formatDeadListName(player));

        giveTeleporter(player, state);
        refreshVisibilityForAll();
    }

    private boolean isPluginDeadMenu(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        if (inventory.getHolder() instanceof TeleportMenuHolder) {
            return true;
        }
        return plugin.getEndWarService() != null && plugin.getEndWarService().isAudienceMenu(inventory);
    }

    private void restoreAliveState(Player player, DeadPlayerState state) {
        player.closeInventory();
        removeTeleporterItems(player.getInventory());
        if (plugin.getEndWarService() != null) {
            plugin.getEndWarService().clearAudienceState(player);
        }

        if (state.replacedTeleporterSlotItem() != null) {
            player.getInventory().setItem(config.getTeleporterSlot(), state.replacedTeleporterSlotItem());
        }

        player.setGameMode(state.previousGameMode());
        player.setAllowFlight(state.previousAllowFlight());
        player.setFlying(state.previousAllowFlight() && state.previousFlying());
        player.setInvulnerable(false);
        player.setCollidable(state.previousCollidable());
        player.setCanPickupItems(state.previousCanPickupItems());
        TextUtil.setPlayerListName(player, TextUtil.safePlayerListName(state.previousListName(), player.getName()));

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
            player.showPlayer(plugin, online);
        }
    }

    private void resetRevivedPlayerState(Player player) {
        player.setInvulnerable(false);
        player.setNoDamageTicks(0);
        player.setFallDistance(0.0F);
        player.setFireTicks(0);
        player.setFreezeTicks(0);

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setInvulnerable(false);
            player.setNoDamageTicks(0);
        });
    }

    private void restorePendingOfflineReviveState(Player player) {
        player.closeInventory();
        removeTeleporterItems(player.getInventory());
        if (plugin.getEndWarService() != null) {
            plugin.getEndWarService().clearAudienceState(player);
        }
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.setCollidable(true);
        player.setCanPickupItems(true);
        TextUtil.setPlayerListName(player, player.getName());
    }

    private Location resolveReviveLocation(DeadPlayerState state) {
        if (!config.isReviveToDeathLocation()) {
            return null;
        }

        Location deathLocation = state.deathLocation();
        if (deathLocation == null) {
            return null;
        }

        if (!config.isSafeEndReviveEnabled()
                || deathLocation.getWorld() == null
                || deathLocation.getWorld().getEnvironment() != World.Environment.THE_END) {
            return deathLocation;
        }

        if (deathLocation.getY() > config.getSafeEndReviveMinY() && isSafeStandingLocation(deathLocation)) {
            return deathLocation;
        }

        Location platformLocation = config.createSafeEndReviveLocation();
        return platformLocation == null ? deathLocation : platformLocation;
    }

    private boolean isSafeStandingLocation(Location location) {
        if (location.getWorld() == null) {
            return false;
        }

        Location blockLocation = location.clone();
        int blockX = blockLocation.getBlockX();
        int blockY = blockLocation.getBlockY();
        int blockZ = blockLocation.getBlockZ();
        if (blockY <= location.getWorld().getMinHeight()) {
            return false;
        }

        Material below = location.getWorld().getBlockAt(blockX, blockY - 1, blockZ).getType();
        Material feet = location.getWorld().getBlockAt(blockX, blockY, blockZ).getType();
        Material head = location.getWorld().getBlockAt(blockX, blockY + 1, blockZ).getType();

        return below.isSolid() && !feet.isSolid() && !head.isSolid();
    }

    private void giveTeleporter(Player player, DeadPlayerState state) {
        if (!config.isTeleporterEnabled()) {
            return;
        }

        ItemStack teleporter = config.createTeleporterItem();
        ItemMeta meta = teleporter.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(teleporterItemKey, PersistentDataType.BYTE, (byte) 1);
            teleporter.setItemMeta(meta);
        }

        int slot = config.getTeleporterSlot();
        ItemStack existing = player.getInventory().getItem(slot);
        if (!isTeleporterItem(existing) && (existing != null && existing.getType() != Material.AIR) && state.replacedTeleporterSlotItem() == null) {
            state.setReplacedTeleporterSlotItem(existing.clone());
        }

        player.getInventory().setItem(slot, teleporter);
    }

    private void removeTeleporterItems(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isTeleporterItem(inventory.getItem(slot))) {
                inventory.setItem(slot, null);
            }
        }
    }

    private void applyVisibilityForViewer(Player viewer) {
        boolean viewerDead = isDead(viewer);
        for (DeadPlayerState state : deadPlayers.values()) {
            Player target = Bukkit.getPlayer(state.uuid());
            if (target == null || !target.isOnline() || viewer.getUniqueId().equals(target.getUniqueId())) {
                continue;
            }

            if (viewerDead) {
                viewer.showPlayer(plugin, target);
            } else if (config.isDeadStateVanishFromAlive()) {
                viewer.hidePlayer(plugin, target);
            } else {
                viewer.showPlayer(plugin, target);
            }
        }
    }

    private ItemStack createTeleportTargetItem(Player target) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            TextUtil.applyItemText(meta, config.formatTeleporterHeadName(target), config.formatTeleporterHeadLore(target, plugin.getEggHolderService().isHolder(target)));
            meta.getPersistentDataContainer().set(teleporterTargetKey, PersistentDataType.STRING, target.getUniqueId().toString());
            head.setItemMeta(meta);
        }
        return head;
    }

    private void showEveryoneToEveryone() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!viewer.getUniqueId().equals(target.getUniqueId())) {
                    viewer.showPlayer(plugin, target);
                }
            }
        }
    }

    public record TeleportMenuHolder(UUID viewerUuid) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class DeadPlayerState {

        private final UUID uuid;
        private String playerName;
        private Location deathLocation;
        private boolean initialized;
        private GameMode previousGameMode;
        private boolean previousAllowFlight;
        private boolean previousFlying;
        private boolean previousCollidable;
        private boolean previousCanPickupItems;
        private String previousListName;
        private ItemStack replacedTeleporterSlotItem;

        private DeadPlayerState(UUID uuid, String playerName, Location deathLocation) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.deathLocation = deathLocation;
        }

        private static DeadPlayerState fresh(Player player, Location deathLocation) {
            return new DeadPlayerState(player.getUniqueId(), player.getName(), deathLocation);
        }

        private static DeadPlayerState offline(UUID uuid, String playerName, Location deathLocation) {
            return new DeadPlayerState(uuid, playerName == null ? "Unknown" : playerName, deathLocation == null ? null : deathLocation.clone());
        }

        private void capture(Player player) {
            this.initialized = true;
            this.previousGameMode = player.getGameMode();
            this.previousAllowFlight = player.getAllowFlight();
            this.previousFlying = player.isFlying();
            this.previousCollidable = player.isCollidable();
            this.previousCanPickupItems = player.getCanPickupItems();
            this.previousListName = TextUtil.getPlayerListName(player);
        }

        private UUID uuid() {
            return uuid;
        }

        private String playerName() {
            return playerName;
        }

        private void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        private Location deathLocation() {
            return deathLocation == null ? null : deathLocation.clone();
        }

        private boolean initialized() {
            return initialized;
        }

        private GameMode previousGameMode() {
            return previousGameMode == null ? GameMode.SURVIVAL : previousGameMode;
        }

        private boolean previousAllowFlight() {
            return previousAllowFlight;
        }

        private boolean previousFlying() {
            return previousFlying;
        }

        private boolean previousCollidable() {
            return previousCollidable;
        }

        private boolean previousCanPickupItems() {
            return previousCanPickupItems;
        }

        private String previousListName() {
            return previousListName;
        }

        private ItemStack replacedTeleporterSlotItem() {
            return replacedTeleporterSlotItem == null ? null : replacedTeleporterSlotItem.clone();
        }

        private void setReplacedTeleporterSlotItem(ItemStack replacedTeleporterSlotItem) {
            this.replacedTeleporterSlotItem = replacedTeleporterSlotItem == null ? null : replacedTeleporterSlotItem.clone();
        }
    }
}
