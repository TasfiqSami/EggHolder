package me.tasfiq.eggholder;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.kyori.adventure.title.Title;

public final class MessageManager {

    private final EggHolderPlugin plugin;
    private FileConfiguration messages;

    public MessageManager(EggHolderPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        YamlConfiguration loadedMessages = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream defaultsStream = plugin.getResource("messages.yml");
        if (defaultsStream != null) {
            YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultsStream, StandardCharsets.UTF_8));
            loadedMessages.setDefaults(defaultMessages);
        }

        this.messages = loadedMessages;
    }

    public String get(String path) {
        return get(path, Map.of());
    }

    public String get(String path, Map<String, String> placeholders) {
        String raw = messages.getString(path);
        if (raw == null) {
            raw = "&cMissing message: " + path;
        }

        return TextUtil.colorize(TextUtil.replacePlaceholders(raw, placeholders));
    }

    public String getPrefixed(String path, Map<String, String> placeholders) {
        return get("prefix") + get(path, placeholders);
    }

    public net.kyori.adventure.text.Component getComponent(String path, Map<String, String> placeholders) {
        return TextUtil.component(get(path, placeholders));
    }

    public net.kyori.adventure.text.Component getPrefixedComponent(String path, Map<String, String> placeholders) {
        return TextUtil.component(getPrefixed(path, placeholders));
    }

    public void sendPrefixed(CommandSender sender, String path) {
        sendPrefixed(sender, path, Map.of());
    }

    public void sendPrefixed(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(getPrefixed(path, placeholders));
    }

    public void broadcastPrefixed(String path, Map<String, String> placeholders) {
        Bukkit.broadcast(getPrefixedComponent(path, placeholders));
    }

    public void sendTitle(Player player, String titlePath, String subtitlePath, Map<String, String> placeholders, PluginConfig pluginConfig) {
        sendTitle(
                player,
                titlePath,
                subtitlePath,
                placeholders,
                pluginConfig.getHolderTitleFadeIn(),
                pluginConfig.getHolderTitleStay(),
                pluginConfig.getHolderTitleFadeOut()
        );
    }

    public void sendTitle(
            Player player,
            String titlePath,
            String subtitlePath,
            Map<String, String> placeholders,
            int fadeIn,
            int stay,
            int fadeOut
    ) {
        player.showTitle(Title.title(
                getComponent(titlePath, placeholders),
                getComponent(subtitlePath, placeholders),
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        ));
    }

    public void sendTitleToAll(String titlePath, String subtitlePath, Map<String, String> placeholders, PluginConfig pluginConfig) {
        sendTitleToAll(
                titlePath,
                subtitlePath,
                placeholders,
                pluginConfig.getHolderTitleFadeIn(),
                pluginConfig.getHolderTitleStay(),
                pluginConfig.getHolderTitleFadeOut()
        );
    }

    public void sendTitleToAll(
            String titlePath,
            String subtitlePath,
            Map<String, String> placeholders,
            int fadeIn,
            int stay,
            int fadeOut
    ) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendTitle(player, titlePath, subtitlePath, placeholders, fadeIn, stay, fadeOut);
        }
    }
}
