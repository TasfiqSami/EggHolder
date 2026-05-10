package me.tasfiq.eggholder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PluginConfig {

    private final boolean commandsRequireEgg;
    private final boolean autoDetectEggHolder;
    private final int autoDetectIntervalTicks;
    private final boolean coordinateTrackingEnabled;
    private final boolean advancementTriggerEnabled;
    private final NamespacedKey advancementKey;
    private final boolean removeHolderWhenEggMissing;

    private final boolean bossBarEnabled;
    private final BarColor bossBarColor;
    private final BarStyle bossBarStyle;
    private final int bossBarUpdateIntervalTicks;
    private final double bossBarProgress;
    private final String bossBarTitleFormat;
    private final String hiddenCoordinateText;

    private final String holderTeamId;
    private final String holderNametagPrefix;
    private final String holderTabNameFormat;
    private final boolean restorePreviousTeam;
    private final boolean stripRoleItemsOnRemove;
    private final boolean stripEffectsOnRemove;
    private final boolean holderGlowEnabled;
    private final NamedTextColor holderGlowColor;
    private final boolean headDisplayEnabled;
    private final double headDisplayYOffset;
    private final Material headDisplayMaterial;
    private final double headDisplayScale;
    private final int headDisplayUpdateIntervalTicks;
    private final int headDisplayInterpolationDurationTicks;
    private final int headDisplayTeleportDurationTicks;
    private final int headDisplayInterpolationDelayTicks;
    private final float headDisplayViewRange;
    private final boolean headDisplayHideFromHolder;
    private final boolean holderTitleEnabled;
    private final int holderTitleFadeIn;
    private final int holderTitleStay;
    private final int holderTitleFadeOut;

    private final boolean announcementChatEnabled;
    private final boolean announcementTitlesEnabled;
    private final int announcementTitleFadeIn;
    private final int announcementTitleStay;
    private final int announcementTitleFadeOut;
    private final int announcementFollowUpTitleDelayTicks;
    private final boolean killBroadcastEnabled;
    private final boolean removalBroadcastEnabled;

    private final Material eggMaterial;
    private final boolean droppedEggGlowEnabled;
    private final boolean droppedEggCustomNameVisible;
    private final String droppedEggCustomName;
    private final boolean spawnReplacementEggOnVoidDeath;
    private final boolean holderLogoutTimeoutEnabled;
    private final int holderLogoutTimeoutSeconds;
    private final boolean holderLogoutSpawnReplacementEgg;
    private final String holderLogoutRespawnWorldName;
    private final double holderLogoutRespawnX;
    private final double holderLogoutRespawnY;
    private final double holderLogoutRespawnZ;
    private final float holderLogoutRespawnYaw;
    private final float holderLogoutRespawnPitch;

    private final boolean preventRoleItemDrops;
    private final boolean storeOverflowInEnderChest;
    private final List<HolderItemDefinition> holderItems;

    private final List<ConfiguredPotionEffectDefinition> holderPotionEffects;

    private final boolean deadStateEnabled;
    private final GameMode deadStateGameMode;
    private final boolean deadStateAllowFlight;
    private final boolean deadStateStartFlying;
    private final boolean deadStateVanishFromAlive;
    private final boolean deadStateInvulnerable;
    private final boolean deadStateCanPickupItems;
    private final boolean deadStateCanDropItems;
    private final boolean deadStateCanModifyInventory;
    private final String deadStateListNameFormat;
    private final boolean reviveToDeathLocation;
    private final boolean safeEndReviveEnabled;
    private final int safeEndReviveMinY;
    private final String safeEndReviveWorldName;
    private final double safeEndReviveX;
    private final double safeEndReviveY;
    private final double safeEndReviveZ;
    private final float safeEndReviveYaw;
    private final float safeEndRevivePitch;
    private final boolean teleporterEnabled;
    private final Material teleporterMaterial;
    private final int teleporterSlot;
    private final String teleporterName;
    private final List<String> teleporterLore;
    private final String teleporterMenuTitle;
    private final String teleporterHeadNameFormat;
    private final List<String> teleporterHeadLore;
    private final boolean teleporterCloseAfterClick;

    private PluginConfig(
            boolean commandsRequireEgg,
            boolean autoDetectEggHolder,
            int autoDetectIntervalTicks,
            boolean coordinateTrackingEnabled,
            boolean advancementTriggerEnabled,
            NamespacedKey advancementKey,
            boolean removeHolderWhenEggMissing,
            boolean bossBarEnabled,
            BarColor bossBarColor,
            BarStyle bossBarStyle,
            int bossBarUpdateIntervalTicks,
            double bossBarProgress,
            String bossBarTitleFormat,
            String hiddenCoordinateText,
            String holderTeamId,
            String holderNametagPrefix,
            String holderTabNameFormat,
            boolean restorePreviousTeam,
            boolean stripRoleItemsOnRemove,
            boolean stripEffectsOnRemove,
            boolean holderGlowEnabled,
            NamedTextColor holderGlowColor,
            boolean headDisplayEnabled,
            double headDisplayYOffset,
            Material headDisplayMaterial,
            double headDisplayScale,
            int headDisplayUpdateIntervalTicks,
            int headDisplayInterpolationDurationTicks,
            int headDisplayTeleportDurationTicks,
            int headDisplayInterpolationDelayTicks,
            float headDisplayViewRange,
            boolean headDisplayHideFromHolder,
            boolean holderTitleEnabled,
            int holderTitleFadeIn,
            int holderTitleStay,
            int holderTitleFadeOut,
            boolean announcementChatEnabled,
            boolean announcementTitlesEnabled,
            int announcementTitleFadeIn,
            int announcementTitleStay,
            int announcementTitleFadeOut,
            int announcementFollowUpTitleDelayTicks,
            boolean killBroadcastEnabled,
            boolean removalBroadcastEnabled,
            Material eggMaterial,
            boolean droppedEggGlowEnabled,
            boolean droppedEggCustomNameVisible,
            String droppedEggCustomName,
            boolean spawnReplacementEggOnVoidDeath,
            boolean holderLogoutTimeoutEnabled,
            int holderLogoutTimeoutSeconds,
            boolean holderLogoutSpawnReplacementEgg,
            String holderLogoutRespawnWorldName,
            double holderLogoutRespawnX,
            double holderLogoutRespawnY,
            double holderLogoutRespawnZ,
            float holderLogoutRespawnYaw,
            float holderLogoutRespawnPitch,
            boolean preventRoleItemDrops,
            boolean storeOverflowInEnderChest,
            List<HolderItemDefinition> holderItems,
            List<ConfiguredPotionEffectDefinition> holderPotionEffects,
            boolean deadStateEnabled,
            GameMode deadStateGameMode,
            boolean deadStateAllowFlight,
            boolean deadStateStartFlying,
            boolean deadStateVanishFromAlive,
            boolean deadStateInvulnerable,
            boolean deadStateCanPickupItems,
            boolean deadStateCanDropItems,
            boolean deadStateCanModifyInventory,
            String deadStateListNameFormat,
            boolean reviveToDeathLocation,
            boolean safeEndReviveEnabled,
            int safeEndReviveMinY,
            String safeEndReviveWorldName,
            double safeEndReviveX,
            double safeEndReviveY,
            double safeEndReviveZ,
            float safeEndReviveYaw,
            float safeEndRevivePitch,
            boolean teleporterEnabled,
            Material teleporterMaterial,
            int teleporterSlot,
            String teleporterName,
            List<String> teleporterLore,
            String teleporterMenuTitle,
            String teleporterHeadNameFormat,
            List<String> teleporterHeadLore,
            boolean teleporterCloseAfterClick
    ) {
        this.commandsRequireEgg = commandsRequireEgg;
        this.autoDetectEggHolder = autoDetectEggHolder;
        this.autoDetectIntervalTicks = autoDetectIntervalTicks;
        this.coordinateTrackingEnabled = coordinateTrackingEnabled;
        this.advancementTriggerEnabled = advancementTriggerEnabled;
        this.advancementKey = advancementKey;
        this.removeHolderWhenEggMissing = removeHolderWhenEggMissing;
        this.bossBarEnabled = bossBarEnabled;
        this.bossBarColor = bossBarColor;
        this.bossBarStyle = bossBarStyle;
        this.bossBarUpdateIntervalTicks = bossBarUpdateIntervalTicks;
        this.bossBarProgress = bossBarProgress;
        this.bossBarTitleFormat = bossBarTitleFormat;
        this.hiddenCoordinateText = hiddenCoordinateText;
        this.holderTeamId = holderTeamId;
        this.holderNametagPrefix = holderNametagPrefix;
        this.holderTabNameFormat = holderTabNameFormat;
        this.restorePreviousTeam = restorePreviousTeam;
        this.stripRoleItemsOnRemove = stripRoleItemsOnRemove;
        this.stripEffectsOnRemove = stripEffectsOnRemove;
        this.holderGlowEnabled = holderGlowEnabled;
        this.holderGlowColor = holderGlowColor;
        this.headDisplayEnabled = headDisplayEnabled;
        this.headDisplayYOffset = headDisplayYOffset;
        this.headDisplayMaterial = headDisplayMaterial;
        this.headDisplayScale = headDisplayScale;
        this.headDisplayUpdateIntervalTicks = headDisplayUpdateIntervalTicks;
        this.headDisplayInterpolationDurationTicks = headDisplayInterpolationDurationTicks;
        this.headDisplayTeleportDurationTicks = headDisplayTeleportDurationTicks;
        this.headDisplayInterpolationDelayTicks = headDisplayInterpolationDelayTicks;
        this.headDisplayViewRange = headDisplayViewRange;
        this.headDisplayHideFromHolder = headDisplayHideFromHolder;
        this.holderTitleEnabled = holderTitleEnabled;
        this.holderTitleFadeIn = holderTitleFadeIn;
        this.holderTitleStay = holderTitleStay;
        this.holderTitleFadeOut = holderTitleFadeOut;
        this.announcementChatEnabled = announcementChatEnabled;
        this.announcementTitlesEnabled = announcementTitlesEnabled;
        this.announcementTitleFadeIn = announcementTitleFadeIn;
        this.announcementTitleStay = announcementTitleStay;
        this.announcementTitleFadeOut = announcementTitleFadeOut;
        this.announcementFollowUpTitleDelayTicks = announcementFollowUpTitleDelayTicks;
        this.killBroadcastEnabled = killBroadcastEnabled;
        this.removalBroadcastEnabled = removalBroadcastEnabled;
        this.eggMaterial = eggMaterial;
        this.droppedEggGlowEnabled = droppedEggGlowEnabled;
        this.droppedEggCustomNameVisible = droppedEggCustomNameVisible;
        this.droppedEggCustomName = droppedEggCustomName;
        this.spawnReplacementEggOnVoidDeath = spawnReplacementEggOnVoidDeath;
        this.holderLogoutTimeoutEnabled = holderLogoutTimeoutEnabled;
        this.holderLogoutTimeoutSeconds = holderLogoutTimeoutSeconds;
        this.holderLogoutSpawnReplacementEgg = holderLogoutSpawnReplacementEgg;
        this.holderLogoutRespawnWorldName = holderLogoutRespawnWorldName;
        this.holderLogoutRespawnX = holderLogoutRespawnX;
        this.holderLogoutRespawnY = holderLogoutRespawnY;
        this.holderLogoutRespawnZ = holderLogoutRespawnZ;
        this.holderLogoutRespawnYaw = holderLogoutRespawnYaw;
        this.holderLogoutRespawnPitch = holderLogoutRespawnPitch;
        this.preventRoleItemDrops = preventRoleItemDrops;
        this.storeOverflowInEnderChest = storeOverflowInEnderChest;
        this.holderItems = holderItems;
        this.holderPotionEffects = holderPotionEffects;
        this.deadStateEnabled = deadStateEnabled;
        this.deadStateGameMode = deadStateGameMode;
        this.deadStateAllowFlight = deadStateAllowFlight;
        this.deadStateStartFlying = deadStateStartFlying;
        this.deadStateVanishFromAlive = deadStateVanishFromAlive;
        this.deadStateInvulnerable = deadStateInvulnerable;
        this.deadStateCanPickupItems = deadStateCanPickupItems;
        this.deadStateCanDropItems = deadStateCanDropItems;
        this.deadStateCanModifyInventory = deadStateCanModifyInventory;
        this.deadStateListNameFormat = deadStateListNameFormat;
        this.reviveToDeathLocation = reviveToDeathLocation;
        this.safeEndReviveEnabled = safeEndReviveEnabled;
        this.safeEndReviveMinY = safeEndReviveMinY;
        this.safeEndReviveWorldName = safeEndReviveWorldName;
        this.safeEndReviveX = safeEndReviveX;
        this.safeEndReviveY = safeEndReviveY;
        this.safeEndReviveZ = safeEndReviveZ;
        this.safeEndReviveYaw = safeEndReviveYaw;
        this.safeEndRevivePitch = safeEndRevivePitch;
        this.teleporterEnabled = teleporterEnabled;
        this.teleporterMaterial = teleporterMaterial;
        this.teleporterSlot = teleporterSlot;
        this.teleporterName = teleporterName;
        this.teleporterLore = teleporterLore;
        this.teleporterMenuTitle = teleporterMenuTitle;
        this.teleporterHeadNameFormat = teleporterHeadNameFormat;
        this.teleporterHeadLore = teleporterHeadLore;
        this.teleporterCloseAfterClick = teleporterCloseAfterClick;
    }

    public static PluginConfig load(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();

        boolean commandsRequireEgg = config.getBoolean("systems.commands-require-egg", true);
        boolean autoDetectEggHolder = config.getBoolean("systems.auto-detect-egg-holder", true);
        int autoDetectIntervalTicks = Math.max(1, config.getInt("systems.auto-detect-interval-ticks", 10));
        boolean coordinateTrackingEnabled = config.getBoolean("systems.coordinate-tracking-enabled", true);
        boolean advancementTriggerEnabled = config.getBoolean("systems.advancement-trigger-enabled", true);
        NamespacedKey advancementKey = parseKey(
                plugin,
                config.getString("systems.advancement-key", "minecraft:end/dragon_egg"),
                NamespacedKey.fromString("minecraft:end/dragon_egg"),
                "systems.advancement-key"
        );
        boolean removeHolderWhenEggMissing = config.getBoolean("systems.remove-holder-when-egg-missing", true);

        boolean bossBarEnabled = config.getBoolean("bossbar.enabled", true);
        BarColor bossBarColor = parseEnum(plugin, config.getString("bossbar.color", "RED"), BarColor.class, BarColor.RED, "bossbar.color");
        BarStyle bossBarStyle = parseEnum(plugin, config.getString("bossbar.style", "SOLID"), BarStyle.class, BarStyle.SOLID, "bossbar.style");
        int bossBarUpdateIntervalTicks = Math.max(1, config.getInt("bossbar.update-interval-ticks", 5));
        double bossBarProgress = Math.max(0.0D, Math.min(1.0D, config.getDouble("bossbar.progress", 1.0D)));
        String bossBarTitleFormat = config.getString("bossbar.title-format", "&c&lEGGHOLDER &d%player% &6&lX:&6 %x% &6&lY:&6 %y% &6&lZ:&6 %z%");
        String hiddenCoordinateText = config.getString("bossbar.hidden-coordinate-text", "???");

        String holderTeamId = safeString(plugin, config.getString("holder.team-id", "egg_holder"), "egg_holder", "holder.team-id");
        String holderNametagPrefix = TextUtil.colorize(config.getString("holder.nametag-prefix", "&d[EggHolder] "));
        String holderTabNameFormat = config.getString("holder.tab-name-format", "&d[EggHolder] %player%");
        boolean restorePreviousTeam = config.getBoolean("holder.restore-previous-team", true);
        boolean stripRoleItemsOnRemove = config.getBoolean("holder.strip-role-items-on-remove", true);
        boolean stripEffectsOnRemove = config.getBoolean("holder.strip-effects-on-remove", true);
        boolean holderGlowEnabled = config.getBoolean("holder.glow.enabled", true);
        NamedTextColor holderGlowColor = parseNamedTextColor(plugin, config.getString("holder.glow.color", "LIGHT_PURPLE"), NamedTextColor.LIGHT_PURPLE, "holder.glow.color");
        boolean headDisplayEnabled = config.getBoolean("holder.head-display.enabled", true);
        double headDisplayYOffset = config.getDouble("holder.head-display.y-offset", 2.15D);
        Material headDisplayMaterial = parseMaterial(plugin, config.getString("holder.head-display.item-material", "DRAGON_EGG"), Material.DRAGON_EGG, "holder.head-display.item-material");
        double headDisplayScale = Math.max(0.1D, config.getDouble("holder.head-display.scale", 0.85D));
        int headDisplayUpdateIntervalTicks = Math.max(1, config.getInt("holder.head-display.update-interval-ticks", 1));
        int headDisplayInterpolationDurationTicks = clamp(config.getInt("holder.head-display.interpolation-duration-ticks", 2), 0, 59);
        int headDisplayTeleportDurationTicks = clamp(config.getInt("holder.head-display.teleport-duration-ticks", 2), 0, 59);
        int headDisplayInterpolationDelayTicks = clamp(config.getInt("holder.head-display.interpolation-delay-ticks", 0), -1, 59);
        float headDisplayViewRange = (float) Math.max(0.1D, config.getDouble("holder.head-display.view-range", 32.0D));
        boolean headDisplayHideFromHolder = config.getBoolean("holder.head-display.hide-from-holder", true);
        boolean holderTitleEnabled = config.getBoolean("holder.title.enabled", true);
        int holderTitleFadeIn = Math.max(0, config.getInt("holder.title.fade-in", 10));
        int holderTitleStay = Math.max(0, config.getInt("holder.title.stay", 60));
        int holderTitleFadeOut = Math.max(0, config.getInt("holder.title.fade-out", 20));

        boolean announcementChatEnabled = config.getBoolean("announcements.chat-enabled", true);
        boolean announcementTitlesEnabled = config.getBoolean("announcements.titles-enabled", true);
        int announcementTitleFadeIn = Math.max(0, config.getInt("announcements.title.fade-in", 10));
        int announcementTitleStay = Math.max(0, config.getInt("announcements.title.stay", 60));
        int announcementTitleFadeOut = Math.max(0, config.getInt("announcements.title.fade-out", 20));
        int announcementFollowUpTitleDelayTicks = Math.max(0, config.getInt("announcements.title.follow-up-delay-ticks", 40));
        boolean killBroadcastEnabled = config.getBoolean("announcements.kill-broadcast-enabled", true);
        boolean removalBroadcastEnabled = config.getBoolean("announcements.removal-broadcast-enabled", false);

        Material eggMaterial = parseMaterial(plugin, config.getString("egg.material", "DRAGON_EGG"), Material.DRAGON_EGG, "egg.material");
        boolean droppedEggGlowEnabled = config.getBoolean("egg.dropped-item.glow", true);
        boolean droppedEggCustomNameVisible = config.getBoolean("egg.dropped-item.custom-name-visible", false);
        String droppedEggCustomName = config.getString("egg.dropped-item.custom-name", "&dDragon Egg");
        boolean spawnReplacementEggOnVoidDeath = config.getBoolean("egg.void-death.spawn-replacement-egg", true);
        boolean holderLogoutTimeoutEnabled = config.getBoolean("egg.logout-timeout.enabled", true);
        int holderLogoutTimeoutSeconds = Math.max(1, config.getInt("egg.logout-timeout.seconds", 60));
        boolean holderLogoutSpawnReplacementEgg = config.getBoolean("egg.logout-timeout.spawn-replacement-egg", true);
        String holderLogoutRespawnWorldName = optionalString(config.getString("egg.logout-timeout.respawn-location.world", ""));
        double holderLogoutRespawnX = config.getDouble("egg.logout-timeout.respawn-location.x", 0.5D);
        double holderLogoutRespawnY = config.getDouble("egg.logout-timeout.respawn-location.y", 66.0D);
        double holderLogoutRespawnZ = config.getDouble("egg.logout-timeout.respawn-location.z", 0.5D);
        float holderLogoutRespawnYaw = (float) config.getDouble("egg.logout-timeout.respawn-location.yaw", 0.0D);
        float holderLogoutRespawnPitch = (float) config.getDouble("egg.logout-timeout.respawn-location.pitch", 0.0D);

        boolean preventRoleItemDrops = config.getBoolean("items.prevent-role-item-drops", true);
        boolean storeOverflowInEnderChest = config.getBoolean("items.store-overflow-in-ender-chest", true);
        List<HolderItemDefinition> holderItems = loadHolderItems(plugin, config.getConfigurationSection("items.holder-items"));
        List<ConfiguredPotionEffectDefinition> holderPotionEffects = loadPotionEffects(plugin, config.getConfigurationSection("effects"));

        boolean deadStateEnabled = config.getBoolean("dead-state.enabled", true);
        GameMode deadStateGameMode = parseEnum(plugin, config.getString("dead-state.game-mode", "ADVENTURE"), GameMode.class, GameMode.ADVENTURE, "dead-state.game-mode");
        boolean deadStateAllowFlight = config.getBoolean("dead-state.allow-flight", true);
        boolean deadStateStartFlying = config.getBoolean("dead-state.start-flying", true);
        boolean deadStateVanishFromAlive = config.getBoolean("dead-state.vanish-from-alive", true);
        boolean deadStateInvulnerable = config.getBoolean("dead-state.invulnerable", true);
        boolean deadStateCanPickupItems = config.getBoolean("dead-state.can-pickup-items", false);
        boolean deadStateCanDropItems = config.getBoolean("dead-state.can-drop-items", false);
        boolean deadStateCanModifyInventory = config.getBoolean("dead-state.can-modify-inventory", false);
        String deadStateListNameFormat = config.getString("dead-state.list-name-format", "&7&o%player%");
        boolean reviveToDeathLocation = config.getBoolean("dead-state.revive-to-death-location", true);
        boolean safeEndReviveEnabled = config.getBoolean("dead-state.safe-end-platform.enabled", true);
        int safeEndReviveMinY = config.getInt("dead-state.safe-end-platform.min-safe-y", 32);
        String safeEndReviveWorldName = optionalString(config.getString("dead-state.safe-end-platform.world", ""));
        double safeEndReviveX = config.getDouble("dead-state.safe-end-platform.x", 0.5D);
        double safeEndReviveY = config.getDouble("dead-state.safe-end-platform.y", 66.0D);
        double safeEndReviveZ = config.getDouble("dead-state.safe-end-platform.z", 0.5D);
        float safeEndReviveYaw = (float) config.getDouble("dead-state.safe-end-platform.yaw", 0.0D);
        float safeEndRevivePitch = (float) config.getDouble("dead-state.safe-end-platform.pitch", 0.0D);
        boolean teleporterEnabled = config.getBoolean("dead-state.teleporter.enabled", true);
        Material teleporterMaterial = parseMaterial(plugin, config.getString("dead-state.teleporter.material", "RECOVERY_COMPASS"), Material.RECOVERY_COMPASS, "dead-state.teleporter.material");
        int teleporterSlot = clamp(config.getInt("dead-state.teleporter.slot", 4), 0, 8);
        String teleporterName = config.getString("dead-state.teleporter.name", "&b&lTeleporter");
        List<String> teleporterLore = TextUtil.colorizeList(config.getStringList("dead-state.teleporter.lore"));
        String teleporterMenuTitle = config.getString("dead-state.teleporter.menu-title", "&8Alive Players");
        String teleporterHeadNameFormat = config.getString("dead-state.teleporter.head-name-format", "&a%player%");
        List<String> teleporterHeadLore = config.getStringList("dead-state.teleporter.head-lore");
        boolean teleporterCloseAfterClick = config.getBoolean("dead-state.teleporter.close-after-click", true);

        return new PluginConfig(
                commandsRequireEgg,
                autoDetectEggHolder,
                autoDetectIntervalTicks,
                coordinateTrackingEnabled,
                advancementTriggerEnabled,
                advancementKey,
                removeHolderWhenEggMissing,
                bossBarEnabled,
                bossBarColor,
                bossBarStyle,
                bossBarUpdateIntervalTicks,
                bossBarProgress,
                bossBarTitleFormat,
                hiddenCoordinateText,
                holderTeamId,
                holderNametagPrefix,
                holderTabNameFormat,
                restorePreviousTeam,
                stripRoleItemsOnRemove,
                stripEffectsOnRemove,
                holderGlowEnabled,
                holderGlowColor,
                headDisplayEnabled,
                headDisplayYOffset,
                headDisplayMaterial,
                headDisplayScale,
                headDisplayUpdateIntervalTicks,
                headDisplayInterpolationDurationTicks,
                headDisplayTeleportDurationTicks,
                headDisplayInterpolationDelayTicks,
                headDisplayViewRange,
                headDisplayHideFromHolder,
                holderTitleEnabled,
                holderTitleFadeIn,
                holderTitleStay,
                holderTitleFadeOut,
                announcementChatEnabled,
                announcementTitlesEnabled,
                announcementTitleFadeIn,
                announcementTitleStay,
                announcementTitleFadeOut,
                announcementFollowUpTitleDelayTicks,
                killBroadcastEnabled,
                removalBroadcastEnabled,
                eggMaterial,
                droppedEggGlowEnabled,
                droppedEggCustomNameVisible,
                droppedEggCustomName,
                spawnReplacementEggOnVoidDeath,
                holderLogoutTimeoutEnabled,
                holderLogoutTimeoutSeconds,
                holderLogoutSpawnReplacementEgg,
                holderLogoutRespawnWorldName,
                holderLogoutRespawnX,
                holderLogoutRespawnY,
                holderLogoutRespawnZ,
                holderLogoutRespawnYaw,
                holderLogoutRespawnPitch,
                preventRoleItemDrops,
                storeOverflowInEnderChest,
                holderItems,
                holderPotionEffects,
                deadStateEnabled,
                deadStateGameMode,
                deadStateAllowFlight,
                deadStateStartFlying,
                deadStateVanishFromAlive,
                deadStateInvulnerable,
                deadStateCanPickupItems,
                deadStateCanDropItems,
                deadStateCanModifyInventory,
                deadStateListNameFormat,
                reviveToDeathLocation,
                safeEndReviveEnabled,
                safeEndReviveMinY,
                safeEndReviveWorldName,
                safeEndReviveX,
                safeEndReviveY,
                safeEndReviveZ,
                safeEndReviveYaw,
                safeEndRevivePitch,
                teleporterEnabled,
                teleporterMaterial,
                teleporterSlot,
                teleporterName,
                teleporterLore,
                teleporterMenuTitle,
                teleporterHeadNameFormat,
                teleporterHeadLore,
                teleporterCloseAfterClick
        );
    }

    public boolean isCommandsRequireEgg() {
        return commandsRequireEgg;
    }

    public boolean isAutoDetectEggHolder() {
        return autoDetectEggHolder;
    }

    public int getAutoDetectIntervalTicks() {
        return autoDetectIntervalTicks;
    }

    public boolean isAdvancementTriggerEnabled() {
        return advancementTriggerEnabled;
    }

    public NamespacedKey getAdvancementKey() {
        return advancementKey;
    }

    public boolean isRemoveHolderWhenEggMissing() {
        return removeHolderWhenEggMissing;
    }

    public boolean isBossBarEnabled() {
        return bossBarEnabled;
    }

    public BarColor getBossBarColor() {
        return bossBarColor;
    }

    public BarStyle getBossBarStyle() {
        return bossBarStyle;
    }

    public int getBossBarUpdateIntervalTicks() {
        return bossBarUpdateIntervalTicks;
    }

    public double getBossBarProgress() {
        return bossBarProgress;
    }

    public String getHolderTeamId() {
        return holderTeamId;
    }

    public String getHolderNametagPrefix() {
        return holderNametagPrefix;
    }

    public boolean isRestorePreviousTeam() {
        return restorePreviousTeam;
    }

    public boolean isStripRoleItemsOnRemove() {
        return stripRoleItemsOnRemove;
    }

    public boolean isStripEffectsOnRemove() {
        return stripEffectsOnRemove;
    }

    public boolean isHolderGlowEnabled() {
        return holderGlowEnabled;
    }

    public NamedTextColor getHolderGlowColor() {
        return holderGlowColor;
    }

    public boolean isHeadDisplayEnabled() {
        return headDisplayEnabled;
    }

    public double getHeadDisplayYOffset() {
        return headDisplayYOffset;
    }

    public Material getHeadDisplayMaterial() {
        return headDisplayMaterial;
    }

    public double getHeadDisplayScale() {
        return headDisplayScale;
    }

    public int getHeadDisplayUpdateIntervalTicks() {
        return headDisplayUpdateIntervalTicks;
    }

    public int getHeadDisplayInterpolationDurationTicks() {
        return headDisplayInterpolationDurationTicks;
    }

    public int getHeadDisplayTeleportDurationTicks() {
        return headDisplayTeleportDurationTicks;
    }

    public int getHeadDisplayInterpolationDelayTicks() {
        return headDisplayInterpolationDelayTicks;
    }

    public float getHeadDisplayViewRange() {
        return headDisplayViewRange;
    }

    public boolean isHeadDisplayHideFromHolder() {
        return headDisplayHideFromHolder;
    }

    public boolean isHolderTitleEnabled() {
        return holderTitleEnabled;
    }

    public int getHolderTitleFadeIn() {
        return holderTitleFadeIn;
    }

    public int getHolderTitleStay() {
        return holderTitleStay;
    }

    public int getHolderTitleFadeOut() {
        return holderTitleFadeOut;
    }

    public boolean isAnnouncementChatEnabled() {
        return announcementChatEnabled;
    }

    public boolean isAnnouncementTitlesEnabled() {
        return announcementTitlesEnabled;
    }

    public int getAnnouncementTitleFadeIn() {
        return announcementTitleFadeIn;
    }

    public int getAnnouncementTitleStay() {
        return announcementTitleStay;
    }

    public int getAnnouncementTitleFadeOut() {
        return announcementTitleFadeOut;
    }

    public int getAnnouncementFollowUpTitleDelayTicks() {
        return announcementFollowUpTitleDelayTicks;
    }

    public boolean isKillBroadcastEnabled() {
        return killBroadcastEnabled;
    }

    public boolean isRemovalBroadcastEnabled() {
        return removalBroadcastEnabled;
    }

    public Material getEggMaterial() {
        return eggMaterial;
    }

    public boolean isDroppedEggGlowEnabled() {
        return droppedEggGlowEnabled;
    }

    public boolean isDroppedEggCustomNameVisible() {
        return droppedEggCustomNameVisible;
    }

    public String getDroppedEggCustomName() {
        return TextUtil.colorize(droppedEggCustomName);
    }

    public boolean isSpawnReplacementEggOnVoidDeath() {
        return spawnReplacementEggOnVoidDeath;
    }

    public boolean isHolderLogoutTimeoutEnabled() {
        return holderLogoutTimeoutEnabled;
    }

    public int getHolderLogoutTimeoutSeconds() {
        return holderLogoutTimeoutSeconds;
    }

    public int getHolderLogoutTimeoutTicks() {
        return holderLogoutTimeoutSeconds * 20;
    }

    public boolean isHolderLogoutSpawnReplacementEgg() {
        return holderLogoutSpawnReplacementEgg;
    }

    public Location createHolderLogoutRespawnLocation() {
        return createConfiguredLocation(
                holderLogoutRespawnWorldName,
                holderLogoutRespawnX,
                holderLogoutRespawnY,
                holderLogoutRespawnZ,
                holderLogoutRespawnYaw,
                holderLogoutRespawnPitch,
                World.Environment.THE_END
        );
    }

    public boolean isPreventRoleItemDrops() {
        return preventRoleItemDrops;
    }

    public boolean isStoreOverflowInEnderChest() {
        return storeOverflowInEnderChest;
    }

    public List<HolderItemDefinition> getHolderItems() {
        return holderItems;
    }

    public List<ConfiguredPotionEffectDefinition> getHolderPotionEffects() {
        return holderPotionEffects;
    }

    public boolean isDeadStateEnabled() {
        return deadStateEnabled;
    }

    public GameMode getDeadStateGameMode() {
        return deadStateGameMode;
    }

    public boolean isDeadStateAllowFlight() {
        return deadStateAllowFlight;
    }

    public boolean isDeadStateStartFlying() {
        return deadStateStartFlying;
    }

    public boolean isDeadStateVanishFromAlive() {
        return deadStateVanishFromAlive;
    }

    public boolean isDeadStateInvulnerable() {
        return deadStateInvulnerable;
    }

    public boolean isDeadStateCanPickupItems() {
        return deadStateCanPickupItems;
    }

    public boolean isDeadStateCanDropItems() {
        return deadStateCanDropItems;
    }

    public boolean isDeadStateCanModifyInventory() {
        return deadStateCanModifyInventory;
    }

    public boolean isReviveToDeathLocation() {
        return reviveToDeathLocation;
    }

    public boolean isSafeEndReviveEnabled() {
        return safeEndReviveEnabled;
    }

    public int getSafeEndReviveMinY() {
        return safeEndReviveMinY;
    }

    public Location createSafeEndReviveLocation() {
        return createConfiguredLocation(
                safeEndReviveWorldName,
                safeEndReviveX,
                safeEndReviveY,
                safeEndReviveZ,
                safeEndReviveYaw,
                safeEndRevivePitch,
                World.Environment.THE_END
        );
    }

    public boolean isTeleporterEnabled() {
        return teleporterEnabled;
    }

    public Material getTeleporterMaterial() {
        return teleporterMaterial;
    }

    public int getTeleporterSlot() {
        return teleporterSlot;
    }

    public boolean isTeleporterCloseAfterClick() {
        return teleporterCloseAfterClick;
    }

    public String formatBossBarTitle(Player player) {
        Location location = player.getLocation();
        String coordinateValue = coordinateTrackingEnabled ? Integer.toString(location.getBlockX()) : hiddenCoordinateText;
        String yValue = coordinateTrackingEnabled ? Integer.toString(location.getBlockY()) : hiddenCoordinateText;
        String zValue = coordinateTrackingEnabled ? Integer.toString(location.getBlockZ()) : hiddenCoordinateText;
        return TextUtil.format(
                bossBarTitleFormat,
                Map.of(
                        "%player%", player.getName(),
                        "%x%", coordinateValue,
                        "%y%", yValue,
                        "%z%", zValue,
                        "%world%", getWorldName(location)
                )
        );
    }

    public String formatHolderTabName(Player player) {
        return TextUtil.format(holderTabNameFormat, Map.of("%player%", player.getName()));
    }

    public String formatDeadListName(Player player) {
        return TextUtil.format(deadStateListNameFormat, Map.of("%player%", player.getName()));
    }

    public ItemStack createHeadDisplayItem() {
        return new ItemStack(headDisplayMaterial);
    }

    public ItemStack createTeleporterItem() {
        ItemStack stack = new ItemStack(teleporterMaterial);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            TextUtil.applyItemText(meta, teleporterName, teleporterLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public String formatTeleporterMenuTitle() {
        return TextUtil.colorize(teleporterMenuTitle);
    }

    public String formatTeleporterHeadName(Player player) {
        return TextUtil.format(teleporterHeadNameFormat, Map.of("%player%", player.getName()));
    }

    public List<String> formatTeleporterHeadLore(Player player, boolean eggHolder) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("%player%", player.getName());
        placeholders.put("%world%", getWorldName(player.getLocation()));
        placeholders.put("%x%", Integer.toString(player.getLocation().getBlockX()));
        placeholders.put("%y%", Integer.toString(player.getLocation().getBlockY()));
        placeholders.put("%z%", Integer.toString(player.getLocation().getBlockZ()));
        placeholders.put("%role%", eggHolder ? "EggHolder" : "Alive");

        List<String> lore = new ArrayList<>();
        for (String line : teleporterHeadLore) {
            lore.add(TextUtil.format(line, placeholders));
        }
        return lore;
    }

    private String getWorldName(Location location) {
        if (location.getWorld() == null) {
            return "unknown";
        }

        String worldName = location.getWorld().getName().replace('_', ' ');
        if (worldName.isEmpty()) {
            return "unknown";
        }

        return Character.toUpperCase(worldName.charAt(0)) + worldName.substring(1);
    }

    private Location createConfiguredLocation(
            String configuredWorldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            World.Environment fallbackEnvironment
    ) {
        World world = resolveWorld(configuredWorldName, fallbackEnvironment);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    private World resolveWorld(String configuredWorldName, World.Environment fallbackEnvironment) {
        if (configuredWorldName != null && !configuredWorldName.isBlank()) {
            return Bukkit.getWorld(configuredWorldName);
        }

        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == fallbackEnvironment) {
                return world;
            }
        }

        return null;
    }

    private static List<HolderItemDefinition> loadHolderItems(JavaPlugin plugin, ConfigurationSection itemsSection) {
        List<HolderItemDefinition> items = new ArrayList<>();
        if (itemsSection == null) {
            return items;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection != null) {
                items.add(HolderItemDefinition.load(plugin, key, itemSection));
            }
        }

        return items;
    }

    private static List<ConfiguredPotionEffectDefinition> loadPotionEffects(JavaPlugin plugin, ConfigurationSection effectsSection) {
        List<ConfiguredPotionEffectDefinition> effects = new ArrayList<>();
        if (effectsSection == null) {
            return effects;
        }

        List<Map<?, ?>> rawList = effectsSection.getMapList("holder-potion-effects");
        for (Map<?, ?> entry : rawList) {
            effects.add(ConfiguredPotionEffectDefinition.load(plugin, entry));
        }
        return effects;
    }

    private static String safeString(JavaPlugin plugin, String value, String fallback, String path) {
        if (value == null || value.isBlank()) {
            plugin.getLogger().warning("Invalid " + path + " value. Falling back to " + fallback + ".");
            return fallback;
        }

        return value;
    }

    private static String optionalString(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Material parseMaterial(JavaPlugin plugin, String rawValue, Material fallback, String path) {
        if (rawValue == null) {
            plugin.getLogger().warning("Invalid " + path + " value 'null'. Falling back to " + fallback + ".");
            return fallback;
        }

        Material material = Material.matchMaterial(rawValue.toUpperCase(Locale.ROOT));
        if (material != null) {
            return material;
        }

        plugin.getLogger().warning("Invalid " + path + " value '" + rawValue + "'. Falling back to " + fallback + ".");
        return fallback;
    }

    private static NamedTextColor parseNamedTextColor(JavaPlugin plugin, String rawValue, NamedTextColor fallback, String path) {
        if (rawValue == null) {
            plugin.getLogger().warning("Invalid " + path + " value 'null'. Falling back to " + fallback + ".");
            return fallback;
        }

        NamedTextColor color = TextUtil.parseNamedTextColor(rawValue);
        if (color != null) {
            return color;
        }

        plugin.getLogger().warning("Invalid " + path + " value '" + rawValue + "'. Falling back to " + fallback + ".");
        return fallback;
    }

    private static NamespacedKey parseKey(JavaPlugin plugin, String rawValue, NamespacedKey fallback, String path) {
        if (rawValue != null) {
            NamespacedKey key = NamespacedKey.fromString(rawValue);
            if (key != null) {
                return key;
            }
        }

        plugin.getLogger().warning("Invalid " + path + " value '" + rawValue + "'. Falling back to " + fallback + ".");
        return fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static <E extends Enum<E>> E parseEnum(
            JavaPlugin plugin,
            String rawValue,
            Class<E> enumType,
            E fallback,
            String path
    ) {
        if (rawValue == null) {
            plugin.getLogger().warning("Invalid " + path + " value 'null'. Falling back to " + fallback + ".");
            return fallback;
        }

        try {
            return Enum.valueOf(enumType, rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("Invalid " + path + " value '" + rawValue + "'. Falling back to " + fallback + ".");
            return fallback;
        }
    }

    public static final class HolderItemDefinition {

        private final String id;
        private final boolean enabled;
        private final Material material;
        private final int amount;
        private final int slot;
        private final String displayName;
        private final List<String> lore;
        private final boolean unbreakable;
        private final Map<Enchantment, Integer> enchantments;

        private HolderItemDefinition(
                String id,
                boolean enabled,
                Material material,
                int amount,
                int slot,
                String displayName,
                List<String> lore,
                boolean unbreakable,
                Map<Enchantment, Integer> enchantments
        ) {
            this.id = id;
            this.enabled = enabled;
            this.material = material;
            this.amount = amount;
            this.slot = slot;
            this.displayName = displayName;
            this.lore = lore;
            this.unbreakable = unbreakable;
            this.enchantments = enchantments;
        }

        public static HolderItemDefinition load(JavaPlugin plugin, String id, ConfigurationSection section) {
            boolean enabled = section.getBoolean("enabled", true);
            Material material = parseMaterial(plugin, section.getString("material", "STONE"), Material.STONE, "items.holder-items." + id + ".material");
            int amount = Math.max(1, section.getInt("amount", 1));
            int slot = section.getInt("slot", -1);
            String displayName = section.getString("name", "&f" + id);
            List<String> lore = section.getStringList("lore");
            boolean unbreakable = section.getBoolean("unbreakable", false);
            Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();

            ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                for (String enchantKey : enchantSection.getKeys(false)) {
                    Enchantment enchantment = parseEnchantment(plugin, enchantKey, "items.holder-items." + id + ".enchantments." + enchantKey);
                    if (enchantment != null) {
                        enchantments.put(enchantment, Math.max(1, enchantSection.getInt(enchantKey, 1)));
                    }
                }
            }

            return new HolderItemDefinition(
                    id,
                    enabled,
                    material,
                    amount,
                    slot,
                    displayName,
                    lore,
                    unbreakable,
                    enchantments
            );
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getSlot() {
            return slot;
        }

        public String getDisplayNamePlain() {
            return TextUtil.colorize(displayName);
        }

        public ItemStack createItem(NamespacedKey roleItemKey, NamespacedKey roleItemIdKey) {
            ItemStack stack = new ItemStack(material, amount);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                TextUtil.applyItemText(meta, displayName, lore);
                meta.setUnbreakable(unbreakable);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
                meta.getPersistentDataContainer().set(roleItemKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                meta.getPersistentDataContainer().set(roleItemIdKey, org.bukkit.persistence.PersistentDataType.STRING, id);
                stack.setItemMeta(meta);
            }

            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                stack.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }

            return stack;
        }

        private static Enchantment parseEnchantment(JavaPlugin plugin, String rawValue, String path) {
            Enchantment enchantment = TextUtil.parseEnchantment(rawValue);
            if (enchantment != null) {
                return enchantment;
            }

            plugin.getLogger().warning("Invalid " + path + " value '" + rawValue + "'. Skipping enchantment.");
            return null;
        }
    }

    public static final class ConfiguredPotionEffectDefinition {

        private final PotionEffectType type;
        private final int duration;
        private final int amplifier;
        private final boolean ambient;
        private final boolean particles;
        private final boolean icon;

        private ConfiguredPotionEffectDefinition(
                PotionEffectType type,
                int duration,
                int amplifier,
                boolean ambient,
                boolean particles,
                boolean icon
        ) {
            this.type = type;
            this.duration = duration;
            this.amplifier = amplifier;
            this.ambient = ambient;
            this.particles = particles;
            this.icon = icon;
        }

        public static ConfiguredPotionEffectDefinition load(JavaPlugin plugin, Map<?, ?> rawMap) {
            Object typeValue = rawMap.get("type");
            PotionEffectType type = typeValue == null
                    ? PotionEffectType.SPEED
                    : TextUtil.parsePotionEffectType(typeValue.toString());
            if (type == null) {
                plugin.getLogger().warning("Invalid potion effect type '" + typeValue + "'. Falling back to SPEED.");
                type = PotionEffectType.SPEED;
            }

            int duration = parseInt(rawMap.get("duration"), -1);
            int amplifier = Math.max(0, parseInt(rawMap.get("amplifier"), 0));
            boolean ambient = parseBoolean(rawMap.get("ambient"), false);
            boolean particles = parseBoolean(rawMap.get("particles"), true);
            boolean icon = parseBoolean(rawMap.get("icon"), true);

            return new ConfiguredPotionEffectDefinition(
                    type,
                    duration < 0 ? Integer.MAX_VALUE : duration,
                    amplifier,
                    ambient,
                    particles,
                    icon
            );
        }

        public PotionEffect createEffect() {
            return new PotionEffect(type, duration, amplifier, ambient, particles, icon);
        }

        public PotionEffectType getType() {
            return type;
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
}
