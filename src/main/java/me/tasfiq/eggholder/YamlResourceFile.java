package me.tasfiq.eggholder;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlResourceFile {

    private final JavaPlugin plugin;
    private final String fileName;
    private FileConfiguration configuration;

    public YamlResourceFile(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        YamlConfiguration loadedConfiguration = YamlConfiguration.loadConfiguration(file);
        InputStream defaultsStream = plugin.getResource(fileName);
        if (defaultsStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultsStream, StandardCharsets.UTF_8));
            loadedConfiguration.setDefaults(defaults);
        }

        this.configuration = loadedConfiguration;
    }

    public FileConfiguration getConfiguration() {
        return configuration;
    }
}
