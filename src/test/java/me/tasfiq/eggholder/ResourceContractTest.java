package me.tasfiq.eggholder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ResourceContractTest {

    private static final Set<String> CONFIGURATION_FILES = Set.of(
            "config.yml", "messages.yml", "kits.yml", "scoreboard.yml", "plugin.yml");

    @Test
    void bundledYamlResourcesParseAndAreNonEmpty() {
        for (String resource : CONFIGURATION_FILES) {
            YamlConfiguration yaml = load(resource);
            assertFalse(yaml.getKeys(true).isEmpty(), resource + " must not be empty");
        }
    }

    @Test
    void everyCommandPermissionIsDeclared() {
        YamlConfiguration plugin = load("plugin.yml");
        Set<String> declaredPermissions = plugin.getConfigurationSection("permissions").getKeys(true);
        for (String command : plugin.getConfigurationSection("commands").getKeys(false)) {
            String permission = plugin.getString("commands." + command + ".permission");
            if (permission != null) {
                assertTrue(declaredPermissions.contains(permission),
                        "Command /" + command + " references undeclared permission " + permission);
            }
        }
    }

    @Test
    void defaultKitDefinitionsHaveTheRequiredStructure() {
        YamlConfiguration kits = load("kits.yml");
        assertNotNull(kits.getConfigurationSection("kits"));
        assertFalse(kits.getConfigurationSection("kits").getKeys(false).isEmpty());
        for (String kit : kits.getConfigurationSection("kits").getKeys(false)) {
            assertNotNull(kits.getString("kits." + kit + ".icon"), kit + " needs an icon");
            assertNotNull(kits.getConfigurationSection("kits." + kit + ".items"), kit + " needs an items section");
            assertFalse(kits.getConfigurationSection("kits." + kit + ".items").getKeys(false).isEmpty(),
                    kit + " needs at least one item");
        }
    }

    private YamlConfiguration load(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, "Missing bundled resource " + name);
        try (InputStream input = stream;
                InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            throw new AssertionError("Could not parse " + name, exception);
        }
    }
}
