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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

public final class TextUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)(?<!<)&?#([0-9a-f]{6})");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)&([0-9A-FK-OR])");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("(?i)[&§]x(?:[&§][0-9a-f]){6}");
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private TextUtil() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }

        return legacy(MINI_MESSAGE.deserialize(legacyToMiniMessage(input)));
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
        return MINI_MESSAGE.deserialize(legacyToMiniMessage(input == null ? "" : input));
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

    /**
     * Resolves a configured material while keeping newer optional gear usable
     * on older supported Minecraft versions.
     */
    public static Material parseCompatibleMaterial(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        Material material = Material.matchMaterial(normalized);
        if (material != null) {
            return material;
        }
        return switch (normalized.replace("MINECRAFT:", "")) {
            case "NETHERITE_SPEAR" -> Material.TRIDENT;
            case "MACE" -> Material.NETHERITE_AXE;
            default -> null;
        };
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

    private static String legacyToMiniMessage(String input) {
        Matcher extendedHex = LEGACY_HEX_PATTERN.matcher(input);
        StringBuffer extendedBuffer = new StringBuffer();
        while (extendedHex.find()) {
            String hexValue = extendedHex.group().replaceAll("(?i)[^0-9a-f]", "");
            extendedHex.appendReplacement(extendedBuffer, "&#" + hexValue);
        }
        extendedHex.appendTail(extendedBuffer);

        Matcher hex = HEX_PATTERN.matcher(extendedBuffer.toString());
        StringBuffer hexBuffer = new StringBuffer();
        while (hex.find()) {
            hex.appendReplacement(hexBuffer, "<#" + hex.group(1) + ">");
        }
        hex.appendTail(hexBuffer);

        Matcher legacy = Pattern.compile("(?i)[&§]([0-9a-fk-or])").matcher(hexBuffer.toString());
        StringBuffer output = new StringBuffer();
        while (legacy.find()) {
            String tag = switch (Character.toLowerCase(legacy.group(1).charAt(0))) {
                case '0' -> "black";
                case '1' -> "dark_blue";
                case '2' -> "dark_green";
                case '3' -> "dark_aqua";
                case '4' -> "dark_red";
                case '5' -> "dark_purple";
                case '6' -> "gold";
                case '7' -> "gray";
                case '8' -> "dark_gray";
                case '9' -> "blue";
                case 'a' -> "green";
                case 'b' -> "aqua";
                case 'c' -> "red";
                case 'd' -> "light_purple";
                case 'e' -> "yellow";
                case 'f' -> "white";
                case 'k' -> "obfuscated";
                case 'l' -> "bold";
                case 'm' -> "strikethrough";
                case 'n' -> "underlined";
                case 'o' -> "italic";
                default -> "reset";
            };
            legacy.appendReplacement(output, "<" + tag + ">");
        }
        legacy.appendTail(output);
        return output.toString();
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
