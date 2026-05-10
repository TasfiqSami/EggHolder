package me.tasfiq.eggholder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

public final class TextUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)&?#([0-9a-f]{6})");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)&([0-9A-FK-OR])");
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private TextUtil() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }

        return translateLegacyCodes(translateHexColors(input));
    }

    public static String replacePlaceholders(String input, Map<String, String> placeholders) {
        if (input == null) {
            return "";
        }

        String output = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            output = output.replace(entry.getKey(), entry.getValue());
        }

        return output;
    }

    public static String format(String input, Map<String, String> placeholders) {
        return colorize(replacePlaceholders(input, placeholders));
    }

    public static List<String> colorizeList(List<String> input) {
        List<String> colored = new ArrayList<>();
        for (String line : input) {
            colored.add(colorize(line));
        }
        return colored;
    }

    public static Component component(String input) {
        return LEGACY_SERIALIZER.deserialize(colorize(input));
    }

    public static List<Component> componentList(List<String> input) {
        List<Component> components = new ArrayList<>();
        for (String line : input) {
            components.add(component(line));
        }
        return components;
    }

    public static String legacy(Component component) {
        return component == null ? null : LEGACY_SERIALIZER.serialize(component);
    }

    public static void applyItemText(ItemMeta meta, String displayName, List<String> lore) {
        if (meta == null) {
            return;
        }
        meta.displayName(component(displayName));
        meta.lore(lore == null || lore.isEmpty() ? null : componentList(lore));
    }

    public static void setPlayerListName(Player player, String value) {
        if (player == null) {
            return;
        }
        player.playerListName(component(value));
    }

    public static String getPlayerListName(Player player) {
        if (player == null) {
            return null;
        }
        return legacy(player.playerListName());
    }

    public static String safePlayerListName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static String stripColor(String input) {
        return PlainTextComponentSerializer.plainText().serialize(component(input));
    }

    public static Enchantment parseEnchantment(String rawValue) {
        NamespacedKey key = parseNamespacedKey(rawValue);
        return key == null ? null : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);
    }

    public static PotionEffectType parsePotionEffectType(String rawValue) {
        NamespacedKey key = parseNamespacedKey(rawValue);
        return key == null ? null : Registry.EFFECT.get(key);
    }

    public static NamedTextColor parseNamedTextColor(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return NamedTextColor.NAMES.value(rawValue.trim().toLowerCase(Locale.ROOT));
    }

    private static String translateHexColors(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, toMinecraftHex(matcher.group(1)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String toMinecraftHex(String hexValue) {
        StringBuilder builder = new StringBuilder("§x");
        for (char character : hexValue.toCharArray()) {
            builder.append('§').append(character);
        }
        return builder.toString();
    }

    private static String translateLegacyCodes(String input) {
        Matcher matcher = LEGACY_COLOR_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "§" + matcher.group(1));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static NamespacedKey parseNamespacedKey(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        NamespacedKey namespaced = NamespacedKey.fromString(normalized);
        if (namespaced != null) {
            return namespaced;
        }
        return NamespacedKey.minecraft(normalized);
    }
}
