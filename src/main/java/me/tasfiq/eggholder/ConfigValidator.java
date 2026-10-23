package me.tasfiq.eggholder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Validates user supplied settings before services consume them. */
final class ConfigValidator {

    private static final Map<String, TimingRule> TIMINGS = createTimingRules();

    private ConfigValidator() {
    }

    static void validate(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Logger logger = plugin.getLogger();
        int warnings = 0;

        for (EndWarService.Feature feature : EndWarService.Feature.values()) {
            Object raw = config.get(feature.configPath());
            if (raw != null && !(raw instanceof Boolean)) {
                logger.warning("Invalid " + feature.configPath() + " value '" + raw
                        + "' (expected true/false). Using default " + feature.defaultEnabled() + ".");
                config.set(feature.configPath(), feature.defaultEnabled());
                warnings++;
            }
        }

        for (Map.Entry<String, TimingRule> entry : TIMINGS.entrySet()) {
            String path = entry.getKey();
            Object raw = config.get(path);
            if (raw == null) {
                continue;
            }
            TimingRule rule = entry.getValue();
            if (!(raw instanceof Number number) || !Double.isFinite(number.doubleValue())) {
                logger.warning("Invalid " + path + " value '" + raw + "'. Using safe default " + rule.fallback() + ".");
                config.set(path, rule.fallback());
                warnings++;
                continue;
            }
            double value = number.doubleValue();
            if (value < rule.minimum() || value > rule.maximum()) {
                double safe = Math.max(rule.minimum(), Math.min(rule.maximum(), value));
                logger.warning("Out-of-range " + path + " value '" + raw + "'. Clamped to " + safe + ".");
                config.set(path, safe == Math.rint(safe) ? (long) safe : safe);
                warnings++;
            }
        }

        warnings += validateValues(plugin, config, "");
        if (warnings == 0) {
            logger.info("Configuration validation passed.");
        } else {
            logger.warning("Configuration validation found " + warnings + " issue(s); safe fallbacks were applied where supported.");
        }
    }

    static void validateResource(JavaPlugin plugin, String resourceName, FileConfiguration resource) {
        int warnings = validateValues(plugin, resource, resourceName);
        if (warnings > 0) {
            plugin.getLogger().warning(resourceName + " validation found " + warnings
                    + " issue(s); item/effect loaders will apply their safe fallbacks.");
        }
    }

    private static int validateValues(JavaPlugin plugin, Object value, String path) {
        int warnings = 0;
        if (value instanceof ConfigurationSection section) {
            for (String key : section.getKeys(false)) {
                String childPath = path.isEmpty() ? key : path + "." + key;
                warnings += validateValues(plugin, section.get(key), childPath);
            }
            return warnings;
        }
        if (value instanceof Iterable<?> iterable) {
            int index = 0;
            for (Object item : iterable) {
                warnings += validateValues(plugin, item, path + "[" + index++ + "]");
            }
            return warnings;
        }
        if (value instanceof Map<?, ?> map) {
            Object effect = map.get("type");
            if (path.toLowerCase(java.util.Locale.ROOT).contains("effect")
                    && effect != null && TextUtil.parsePotionEffectType(effect.toString()) == null) {
                plugin.getLogger().warning("Invalid " + path + ".type potion effect '" + effect + "'. It will be skipped or use the configured fallback.");
                warnings++;
            }
            if (path.toLowerCase(java.util.Locale.ROOT).contains("effect")) {
                Object duration = map.get("duration");
                if (duration != null && (!(duration instanceof Number number)
                        || !Double.isFinite(number.doubleValue())
                        || (number.intValue() <= 0 && number.intValue() != -1))) {
                    plugin.getLogger().warning("Invalid " + path + ".duration value '" + duration + "'. The effect loader will use a safe duration.");
                    warnings++;
                }
                Object amplifier = map.get("amplifier");
                if (amplifier != null && (!(amplifier instanceof Number number)
                        || !Double.isFinite(number.doubleValue()) || number.intValue() < 0)) {
                    plugin.getLogger().warning("Invalid " + path + ".amplifier value '" + amplifier + "'. The effect loader will use amplifier 0.");
                    warnings++;
                }
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String childPath = path + "." + entry.getKey();
                warnings += validateValues(plugin, entry.getValue(), childPath);
            }
            return warnings;
        }
        if (value instanceof String text && (path.endsWith(".material") || path.endsWith(".item-material"))) {
            if (TextUtil.parseCompatibleMaterial(text) == null) {
                plugin.getLogger().warning("Invalid " + path + " value '" + text + "'. Existing item loader fallback will be used.");
                warnings++;
            }
        }
        return warnings;
    }

    private static Map<String, TimingRule> createTimingRules() {
        Map<String, TimingRule> rules = new LinkedHashMap<>();
        rules.put("start.entry-lock-seconds", new TimingRule(0, 3600, 60));
        rules.put("systems.visual-update-interval-ticks", new TimingRule(1, 1200, 4));
        rules.put("systems.auto-detect-interval-ticks", new TimingRule(1, 1200, 10));
        rules.put("bossbar.update-interval-ticks", new TimingRule(1, 1200, 5));
        rules.put("features.end-storm.interval-seconds", new TimingRule(15, 86400, 180));
        rules.put("features.anti-camp-detector.seconds", new TimingRule(5, 86400, 20));
        rules.put("features.egg-drop-countdown.seconds", new TimingRule(0, 60, 5));
        rules.put("features.audience-tools.vote-duration-seconds", new TimingRule(10, 3600, 30));
        rules.put("features.overtime-win-condition.seconds", new TimingRule(30, 86400, 1800));
        rules.put("features.mercyless-endgame.seconds", new TimingRule(30, 86400, 2400));
        rules.put("features.no-escape-timer.seconds", new TimingRule(10, 86400, 240));
        rules.put("features.hotspot-rotation.interval-seconds", new TimingRule(15, 86400, 150));
        rules.put("features.hotspot-rotation.duration-seconds", new TimingRule(10, 86400, 60));
        rules.put("features.supply-drops.interval-seconds", new TimingRule(30, 86400, 300));
        rules.put("features.egg-burn-timer.seconds", new TimingRule(15, 86400, 90));
        rules.put("features.bridge-collapse-event.interval-seconds", new TimingRule(20, 86400, 180));
        rules.put("features.revenge-mark.seconds", new TimingRule(1, 86400, 30));
        return Map.copyOf(rules);
    }

    private record TimingRule(double minimum, double maximum, double fallback) {
    }
}
