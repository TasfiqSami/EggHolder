package me.tasfiq.eggholder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.boss.DragonBattle;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.generator.ChunkGenerator;

public final class ManagedEndWorldService {

    private final EggHolderPlugin plugin;

    private boolean enabled;
    private boolean prepareOnStartup;
    private boolean clearRegionFirst;
    private boolean extractBundledSchematic;
    private boolean overwriteExtractedSchematic;
    private boolean usePedestalsForObjectives;
    private boolean dragonBattleResetPreviouslyKilled;
    private boolean dragonBattleSpawnHealingCrystals;
    private boolean dragonBattleShowCrystalBottom;
    private int dragonCrystalLimit;
    private int objectiveLocationLimit;
    private int dragonCrystalAnchorSearchRadius;
    private double targetCenterX;
    private double targetCenterY;
    private double targetCenterZ;
    private double pedestalMinDistance;
    private double pedestalMaxDistance;
    private String worldName;
    private String schematicResourcePath;
    private String extractedSchematicPath;

    private World managedWorld;
    private Location endCenter;
    private List<Location> dragonCrystalLocations = List.of();
    private SpongeSchematic cachedSchematic;
    private String statusMessage = "Not prepared";
    private boolean prepared;
    private boolean unsupportedStatesLogged;

    public ManagedEndWorldService(EggHolderPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void start() {
        if (!enabled) {
            statusMessage = "Managed End world disabled";
            return;
        }

        if (prepareOnStartup) {
            prepareWorld(true);
            return;
        }

        refreshLoadedWorldContext();
    }

    public void shutdown() {
        dragonCrystalLocations = List.of();
        prepared = false;
    }

    public void reload() {
        this.enabled = plugin.getConfig().getBoolean("managed-end-world.enabled", true);
        this.prepareOnStartup = plugin.getConfig().getBoolean("managed-end-world.prepare-on-startup", true);
        this.clearRegionFirst = plugin.getConfig().getBoolean("managed-end-world.clear-region-first", true);
        this.extractBundledSchematic = plugin.getConfig().getBoolean("managed-end-world.extract-bundled-schematic", true);
        this.overwriteExtractedSchematic = plugin.getConfig().getBoolean("managed-end-world.overwrite-extracted-schematic", true);
        this.schematicResourcePath = plugin.getConfig().getString("managed-end-world.resource-path", "maps/ScarworldEndIsland.schem");
        this.extractedSchematicPath = plugin.getConfig().getString("managed-end-world.extracted-file", "maps/ScarworldEndIsland.schem");
        this.usePedestalsForObjectives = plugin.getConfig().getBoolean("managed-end-world.use-crystal-pedestals-for-objectives", true);
        this.objectiveLocationLimit = Math.max(1, plugin.getConfig().getInt("managed-end-world.objective-location-limit", 4));
        this.dragonBattleResetPreviouslyKilled = plugin.getConfig().getBoolean("managed-end-world.dragon-battle.reset-previously-killed", true);
        this.dragonBattleSpawnHealingCrystals = plugin.getConfig().getBoolean("managed-end-world.dragon-battle.spawn-healing-crystals", true);
        this.dragonBattleShowCrystalBottom = plugin.getConfig().getBoolean("managed-end-world.dragon-battle.show-crystal-bottom", true);
        this.dragonCrystalLimit = Math.max(1, plugin.getConfig().getInt("managed-end-world.dragon-battle.max-healing-crystals", 10));
        this.dragonCrystalAnchorSearchRadius = Math.max(0, plugin.getConfig().getInt("managed-end-world.dragon-battle.anchor-search-radius", 4));
        this.pedestalMinDistance = Math.max(0.0D, plugin.getConfig().getDouble("managed-end-world.dragon-battle.pedestal-min-distance", 20.0D));
        this.pedestalMaxDistance = Math.max(pedestalMinDistance, plugin.getConfig().getDouble("managed-end-world.dragon-battle.pedestal-max-distance", 120.0D));
        this.worldName = plugin.getConfig().getString("game.worlds.end", "world_the_end");
        this.targetCenterX = plugin.getConfig().getDouble("game.end-center.x", 0.5D);
        this.targetCenterY = plugin.getConfig().getDouble("game.end-center.y", 66.0D);
        this.targetCenterZ = plugin.getConfig().getDouble("game.end-center.z", 0.5D);
        this.cachedSchematic = null;
        this.prepared = false;
        this.unsupportedStatesLogged = false;
        refreshLoadedWorldContext();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean ensurePrepared() {
        if (!enabled) {
            return true;
        }
        if (prepared && managedWorld != null && endCenter != null) {
            return true;
        }
        return prepareWorld(true);
    }

    public boolean prepareWorld(boolean forceRepaste) {
        if (!enabled) {
            statusMessage = "Managed End world disabled";
            return false;
        }

        try {
            extractBundledSchematicIfConfigured();
            SpongeSchematic schematic = loadSchematic();
            logUnsupportedStates(schematic);
            World world = getOrCreateManagedWorld();
            if (world == null) {
                statusMessage = "Failed to create " + worldName;
                return false;
            }

            managedWorld = world;
            PastePlacement placement = createPlacement(world, schematic);
            ensureChunksLoaded(world, placement, schematic);
            if (clearRegionFirst || forceRepaste) {
                clearPasteRegion(world, placement, schematic);
            }
            pasteSchematic(world, placement, schematic);

            this.endCenter = placement.centerLocation();
            world.setSpawnLocation(endCenter);

            List<SpongeSchematic.PedestalAnchor> pedestals = schematic.detectCrystalPedestals(
                    pedestalMinDistance,
                    pedestalMaxDistance,
                    dragonCrystalLimit
            );
            this.dragonCrystalLocations = refineCrystalAnchors(toWorldLocations(placement, pedestals));
            prepareDragonBattle(world);
            this.prepared = true;

            statusMessage = "Ready in " + world.getName()
                    + " at " + endCenter.getBlockX() + " " + endCenter.getBlockY() + " " + endCenter.getBlockZ()
                    + " with " + dragonCrystalLocations.size() + " crystal anchors";
            plugin.getLogger().info("Managed End world prepared from " + schematicResourcePath + ". " + statusMessage);
            return true;
        } catch (Exception exception) {
            prepared = false;
            statusMessage = "Prepare failed: " + exception.getMessage();
            plugin.getLogger().severe("Failed to prepare the managed End world: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }

    public World getManagedWorld() {
        return managedWorld;
    }

    public Location getEndCenter() {
        return endCenter == null ? null : endCenter.clone();
    }

    public boolean shouldUsePedestalsForObjectives() {
        return enabled && usePedestalsForObjectives && !dragonCrystalLocations.isEmpty();
    }

    public List<Location> getObjectiveAnchorLocations() {
        if (dragonCrystalLocations.isEmpty()) {
            return List.of();
        }
        return evenlySampleLocations(dragonCrystalLocations, objectiveLocationLimit);
    }

    public String getWorldName() {
        return worldName;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Map<String, String> createStatusPlaceholders() {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("%world%", worldName == null ? "unknown" : worldName);
        placeholders.put("%schematic%", schematicResourcePath == null ? "unknown" : schematicResourcePath);
        placeholders.put("%status%", statusMessage);
        placeholders.put("%crystals%", Integer.toString(dragonCrystalLocations.size()));
        if (endCenter != null) {
            placeholders.put("%x%", Integer.toString(endCenter.getBlockX()));
            placeholders.put("%y%", Integer.toString(endCenter.getBlockY()));
            placeholders.put("%z%", Integer.toString(endCenter.getBlockZ()));
        } else {
            placeholders.put("%x%", "?");
            placeholders.put("%y%", "?");
            placeholders.put("%z%", "?");
        }
        return placeholders;
    }

    private void refreshLoadedWorldContext() {
        this.managedWorld = worldName == null ? null : Bukkit.getWorld(worldName);
        if (managedWorld != null) {
            this.endCenter = new Location(managedWorld, targetCenterX, targetCenterY, targetCenterZ);
            if (dragonCrystalLocations.isEmpty()) {
                statusMessage = "Loaded existing world " + worldName;
            }
        } else {
            this.endCenter = null;
            dragonCrystalLocations = List.of();
            statusMessage = "World " + worldName + " is not loaded yet";
        }
        prepared = false;
    }

    private SpongeSchematic loadSchematic() throws IOException {
        if (cachedSchematic != null) {
            return cachedSchematic;
        }

        try (InputStream resource = plugin.getResource(schematicResourcePath)) {
            if (resource == null) {
                throw new IOException("Bundled schematic resource not found: " + schematicResourcePath);
            }
            cachedSchematic = SpongeSchematic.load(resource);
            return cachedSchematic;
        }
    }

    private void logUnsupportedStates(SpongeSchematic schematic) {
        if (unsupportedStatesLogged) {
            return;
        }
        List<String> unsupportedStates = schematic.unsupportedStates();
        if (unsupportedStates.isEmpty()) {
            return;
        }

        int previewCount = Math.min(5, unsupportedStates.size());
        String preview = String.join(", ", unsupportedStates.subList(0, previewCount));
        plugin.getLogger().warning(
                "The bundled End schematic contains " + unsupportedStates.size()
                        + " block states that this server could not fully parse. "
                        + "Those blocks will be skipped or simplified. Examples: " + preview
        );
        unsupportedStatesLogged = true;
    }

    private void extractBundledSchematicIfConfigured() throws IOException {
        if (!extractBundledSchematic) {
            return;
        }

        File targetFile = new File(plugin.getDataFolder(), extractedSchematicPath);
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (targetFile.exists() && !overwriteExtractedSchematic) {
            return;
        }

        try (InputStream resource = plugin.getResource(schematicResourcePath)) {
            if (resource == null) {
                throw new IOException("Bundled schematic resource not found: " + schematicResourcePath);
            }
            Files.copy(resource, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private World getOrCreateManagedWorld() {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return world;
        }

        WorldCreator creator = WorldCreator.name(worldName);
        creator.environment(World.Environment.THE_END);
        creator.generator(new EmptyEndChunkGenerator());
        creator.generateStructures(false);
        return creator.createWorld();
    }

    private PastePlacement createPlacement(World world, SpongeSchematic schematic) {
        int centerBlockX = (int) Math.floor(targetCenterX);
        int centerBlockY = (int) Math.floor(targetCenterY);
        int centerBlockZ = (int) Math.floor(targetCenterZ);
        int originX = centerBlockX - schematic.centerX();
        int originY = centerBlockY - schematic.centerY();
        int originZ = centerBlockZ - schematic.centerZ();
        Location center = new Location(world, targetCenterX, targetCenterY, targetCenterZ);
        return new PastePlacement(originX, originY, originZ, center);
    }

    private void ensureChunksLoaded(World world, PastePlacement placement, SpongeSchematic schematic) {
        int minChunkX = Math.floorDiv(placement.originX(), 16);
        int maxChunkX = Math.floorDiv(placement.originX() + schematic.width() - 1, 16);
        int minChunkZ = Math.floorDiv(placement.originZ(), 16);
        int maxChunkZ = Math.floorDiv(placement.originZ() + schematic.length() - 1, 16);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                world.getChunkAt(chunkX, chunkZ).load(true);
            }
        }
    }

    private void clearPasteRegion(World world, PastePlacement placement, SpongeSchematic schematic) {
        for (int x = 0; x < schematic.width(); x++) {
            for (int z = 0; z < schematic.length(); z++) {
                for (int y = 0; y < schematic.height(); y++) {
                    org.bukkit.block.Block block = world.getBlockAt(
                            placement.originX() + x,
                            placement.originY() + y,
                            placement.originZ() + z
                    );
                    if (block.getType().isAir()) {
                        continue;
                    }
                    block.setType(org.bukkit.Material.AIR, false);
                }
            }
        }
    }

    private void pasteSchematic(World world, PastePlacement placement, SpongeSchematic schematic) {
        for (SpongeSchematic.PlacedBlock block : schematic.nonAirBlocks()) {
            world.getBlockAt(
                    placement.originX() + block.x(),
                    placement.originY() + block.y(),
                    placement.originZ() + block.z()
            ).setBlockData(block.blockData(), false);
        }
    }

    private List<Location> toWorldLocations(PastePlacement placement, List<SpongeSchematic.PedestalAnchor> pedestals) {
        if (pedestals.isEmpty() || managedWorld == null) {
            return List.of();
        }

        List<Location> locations = new ArrayList<>(pedestals.size());
        for (SpongeSchematic.PedestalAnchor pedestal : pedestals) {
            locations.add(new Location(
                    managedWorld,
                    placement.originX() + pedestal.x() + 0.5D,
                    placement.originY() + pedestal.y(),
                    placement.originZ() + pedestal.z() + 0.5D
            ));
        }
        return List.copyOf(locations);
    }

    private void prepareDragonBattle(World world) {
        DragonBattle battle = world.getEnderDragonBattle();
        if (battle != null && dragonBattleResetPreviouslyKilled) {
            battle.setPreviouslyKilled(false);
        }

        if (!dragonBattleSpawnHealingCrystals || dragonCrystalLocations.isEmpty()) {
            return;
        }

        double cleanupRadius = Math.max(32.0D, pedestalMaxDistance + 24.0D);
        double cleanupRadiusSquared = cleanupRadius * cleanupRadius;
        Location center = endCenter == null ? new Location(world, targetCenterX, targetCenterY, targetCenterZ) : endCenter;
        for (EnderCrystal crystal : world.getEntitiesByClass(EnderCrystal.class)) {
            if (crystal.getLocation().distanceSquared(center) <= cleanupRadiusSquared) {
                crystal.remove();
            }
        }

        for (Location location : dragonCrystalLocations) {
            world.spawn(location, EnderCrystal.class, crystal -> {
                crystal.setShowingBottom(dragonBattleShowCrystalBottom);
                crystal.setInvulnerable(false);
                crystal.setBeamTarget(null);
            });
        }
    }

    private List<Location> refineCrystalAnchors(List<Location> approximateLocations) {
        if (approximateLocations.isEmpty() || managedWorld == null) {
            return List.of();
        }

        List<Location> refined = new ArrayList<>(approximateLocations.size());
        for (Location approximate : approximateLocations) {
            refined.add(refineCrystalAnchor(approximate));
        }
        return List.copyOf(refined);
    }

    private Location refineCrystalAnchor(Location approximate) {
        if (approximate == null || approximate.getWorld() == null) {
            return approximate;
        }

        int supportY = approximate.getBlockY() - 1;
        int centerX = approximate.getBlockX();
        int centerZ = approximate.getBlockZ();
        List<BlockCandidate> candidates = new ArrayList<>();

        for (int x = centerX - dragonCrystalAnchorSearchRadius; x <= centerX + dragonCrystalAnchorSearchRadius; x++) {
            for (int z = centerZ - dragonCrystalAnchorSearchRadius; z <= centerZ + dragonCrystalAnchorSearchRadius; z++) {
                for (int y = supportY - 3; y <= supportY + 3; y++) {
                    Block block = approximate.getWorld().getBlockAt(x, y, z);
                    if (!isCrystalSupportBlock(block)) {
                        continue;
                    }

                    Block above = block.getRelative(0, 1, 0);
                    Block twoAbove = block.getRelative(0, 2, 0);
                    if (!isCrystalClearanceBlock(above) || !isCrystalClearanceBlock(twoAbove)) {
                        continue;
                    }

                    candidates.add(new BlockCandidate(block, countSupportNeighbors(block)));
                }
            }
        }

        if (candidates.isEmpty()) {
            return approximate.clone();
        }

        int highestY = candidates.stream()
                .map(BlockCandidate::block)
                .mapToInt(Block::getY)
                .max()
                .orElse(approximate.getBlockY() - 1);

        List<BlockCandidate> topCandidates = new ArrayList<>();
        for (BlockCandidate candidate : candidates) {
            if (candidate.block().getY() == highestY) {
                topCandidates.add(candidate);
            }
        }

        if (topCandidates.isEmpty()) {
            topCandidates = candidates;
        }

        List<SupportCluster> clusters = buildSupportClusters(topCandidates);
        SupportCluster bestCluster = clusters.stream()
                .max(Comparator
                        .comparingInt(SupportCluster::size)
                        .thenComparingInt(SupportCluster::maxSupportNeighbors)
                        .thenComparingDouble(cluster -> -horizontalDistanceSquared(cluster.centroidX(), cluster.centroidZ(), approximate)))
                .orElse(null);
        if (bestCluster == null || bestCluster.blocks().isEmpty()) {
            return approximate.clone();
        }

        Block support = bestCluster.blocks().stream()
                .sorted(Comparator
                        .comparingInt(BlockCandidate::supportNeighbors).reversed()
                        .thenComparingDouble(candidate -> horizontalDistanceSquared(candidate.block(), bestCluster.centroidX(), bestCluster.centroidZ()))
                        .thenComparingDouble(candidate -> horizontalDistanceSquared(candidate.block(), approximate)))
                .map(BlockCandidate::block)
                .findFirst()
                .orElse(bestCluster.blocks().get(0).block());
        return new Location(
                support.getWorld(),
                support.getX() + 0.5D,
                support.getY() + 1.0D,
                support.getZ() + 0.5D
        );
    }

    private List<SupportCluster> buildSupportClusters(List<BlockCandidate> candidates) {
        List<SupportCluster> clusters = new ArrayList<>();
        boolean[] visited = new boolean[candidates.size()];

        for (int index = 0; index < candidates.size(); index++) {
            if (visited[index]) {
                continue;
            }

            ArrayDeque<Integer> queue = new ArrayDeque<>();
            List<BlockCandidate> clusterBlocks = new ArrayList<>();
            queue.add(index);
            visited[index] = true;

            double totalX = 0.0D;
            double totalZ = 0.0D;
            int maxSupportNeighbors = 0;
            while (!queue.isEmpty()) {
                int currentIndex = queue.removeFirst();
                BlockCandidate candidate = candidates.get(currentIndex);
                clusterBlocks.add(candidate);
                totalX += candidate.block().getX() + 0.5D;
                totalZ += candidate.block().getZ() + 0.5D;
                maxSupportNeighbors = Math.max(maxSupportNeighbors, candidate.supportNeighbors());

                for (int otherIndex = 0; otherIndex < candidates.size(); otherIndex++) {
                    if (visited[otherIndex]) {
                        continue;
                    }
                    if (areAdjacent(candidates.get(currentIndex).block(), candidates.get(otherIndex).block())) {
                        visited[otherIndex] = true;
                        queue.addLast(otherIndex);
                    }
                }
            }

            clusters.add(new SupportCluster(
                    clusterBlocks,
                    totalX / clusterBlocks.size(),
                    totalZ / clusterBlocks.size(),
                    clusterBlocks.size(),
                    maxSupportNeighbors
            ));
        }

        return clusters;
    }

    private boolean areAdjacent(Block first, Block second) {
        return Math.abs(first.getX() - second.getX()) <= 1
                && Math.abs(first.getZ() - second.getZ()) <= 1
                && first.getY() == second.getY();
    }

    private boolean isCrystalSupportBlock(Block block) {
        Material type = block.getType();
        return type == Material.OBSIDIAN || type == Material.BEDROCK;
    }

    private boolean isCrystalClearanceBlock(Block block) {
        Material type = block.getType();
        return type.isAir() || type == Material.FIRE;
    }

    private int countSupportNeighbors(Block block) {
        int neighbors = 0;
        int y = block.getY();
        World world = block.getWorld();
        for (int x = block.getX() - 1; x <= block.getX() + 1; x++) {
            for (int z = block.getZ() - 1; z <= block.getZ() + 1; z++) {
                if (x == block.getX() && z == block.getZ()) {
                    continue;
                }
                Material type = world.getBlockAt(x, y, z).getType();
                if (type == Material.OBSIDIAN || type == Material.BEDROCK) {
                    neighbors++;
                }
            }
        }
        return neighbors;
    }

    private double horizontalDistanceSquared(Block block, Location location) {
        double dx = block.getX() + 0.5D - location.getX();
        double dz = block.getZ() + 0.5D - location.getZ();
        return dx * dx + dz * dz;
    }

    private double horizontalDistanceSquared(Block block, double x, double z) {
        double dx = block.getX() + 0.5D - x;
        double dz = block.getZ() + 0.5D - z;
        return dx * dx + dz * dz;
    }

    private double horizontalDistanceSquared(double x, double z, Location location) {
        double dx = x - location.getX();
        double dz = z - location.getZ();
        return dx * dx + dz * dz;
    }

    private List<Location> evenlySampleLocations(List<Location> locations, int limit) {
        if (limit <= 0 || locations.size() <= limit) {
            List<Location> copy = new ArrayList<>(locations.size());
            for (Location location : locations) {
                copy.add(location.clone());
            }
            return List.copyOf(copy);
        }

        List<Location> sampled = new ArrayList<>(limit);
        for (int index = 0; index < limit; index++) {
            int sampleIndex = (int) Math.floor((double) index * locations.size() / limit);
            sampleIndex = Math.max(0, Math.min(locations.size() - 1, sampleIndex));
            sampled.add(locations.get(sampleIndex).clone());
        }
        return List.copyOf(sampled);
    }

    private record PastePlacement(int originX, int originY, int originZ, Location centerLocation) {
    }

    private record BlockCandidate(Block block, int supportNeighbors) {
    }

    private record SupportCluster(List<BlockCandidate> blocks, double centroidX, double centroidZ, int size, int maxSupportNeighbors) {
    }

    private static final class EmptyEndChunkGenerator extends ChunkGenerator {

        @Override
        public boolean shouldGenerateNoise() {
            return false;
        }

        @Override
        public boolean shouldGenerateSurface() {
            return false;
        }

        @Override
        public boolean shouldGenerateCaves() {
            return false;
        }

        @Override
        public boolean shouldGenerateDecorations() {
            return false;
        }

        @Override
        public boolean shouldGenerateMobs() {
            return false;
        }

        @Override
        public boolean shouldGenerateStructures() {
            return false;
        }

        @Override
        public Location getFixedSpawnLocation(World world, Random random) {
            return new Location(world, 0.5D, 80.0D, 0.5D);
        }
    }
}
