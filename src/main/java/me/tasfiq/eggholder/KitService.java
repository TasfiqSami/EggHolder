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
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.plugin.java.JavaPlugin;

public final class KitService implements Listener {

    private final EggHolderPlugin plugin;
    private final YamlResourceFile kitsFile;
    private final NamespacedKey kitMenuItemKey;
    private final NamespacedKey kitMenuActionKey;
    private final NamespacedKey kitItemKey;
    private final Map<String, KitDefinition> kits = new LinkedHashMap<>();
    private final Map<UUID, String> selectedKits = new LinkedHashMap<>();

    private Material menuMaterial;
    private String menuName;
    private List<String> menuLore;
    private int menuSlot;

    public KitService(EggHolderPlugin plugin) {
        this.plugin = plugin;
        this.kitsFile = new YamlResourceFile(plugin, "kits.yml");
        this.kitMenuItemKey = new NamespacedKey(plugin, "kit_menu_item");
        this.kitMenuActionKey = new NamespacedKey(plugin, "kit_menu_action");
        this.kitItemKey = new NamespacedKey(plugin, "kit_item");
        reload();
    }

    public void reload() {
        kitsFile.load();
        kits.clear();

        FileConfiguration config = plugin.getConfig();
        this.menuMaterial = parseMaterial(config.getString("menus.kits.material"), Material.ENDER_EYE);
        this.menuName = config.getString("menus.kits.name", "&d&lHunter Kits");
        this.menuLore = config.getStringList("menus.kits.lore");
        this.menuSlot = Math.max(0, Math.min(8, config.getInt("menus.kits.slot", 8)));

        ConfigurationSection kitsSection = kitsFile.getConfiguration().getConfigurationSection("kits");
        if (kitsSection == null) {
            return;
        }

        for (String kitId : kitsSection.getKeys(false)) {
            ConfigurationSection section = kitsSection.getConfigurationSection(kitId);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }

            KitDefinition definition = KitDefinition.load(plugin, kitId, section);
            kits.put(kitId.toLowerCase(Locale.ROOT), definition);
        }
    }

    public void giveMenuItem(Player player) {
        ItemStack menuItem = createMenuItem();
        ItemStack existing = player.getInventory().getItem(menuSlot);
        if (existing == null || existing.getType() == Material.AIR || isKitMenuItem(existing)) {
            player.getInventory().setItem(menuSlot, menuItem);
            return;
        }

        if (!player.getInventory().containsAtLeast(menuItem, 1)) {
            player.getInventory().addItem(menuItem);
        }
    }

    public void removeMenuItem(Player player) {
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            if (isKitMenuItem(player.getInventory().getItem(slot))) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    public void openMenu(Player player) {
        int size = Math.max(9, ((Math.max(1, kits.size()) - 1) / 9 + 1) * 9);
        Inventory inventory = Bukkit.createInventory(new KitMenuHolder(player.getUniqueId()), Math.min(size, 54), TextUtil.component("&8Hunter Kits"));
        int slot = 0;
        for (KitDefinition kit : kits.values()) {
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, kit.toDisplayItem(kitMenuActionKey));
        }
        player.openInventory(inventory);
    }

    public Collection<String> getKitNames() {
        return Collections.unmodifiableCollection(kits.keySet());
    }

    public String getSelectedKitId(Player player) {
        String selected = selectedKits.get(player.getUniqueId());
        if (selected != null && kits.containsKey(selected)) {
            return selected;
        }
        return kits.keySet().stream().findFirst().orElse(null);
    }

    public KitDefinition getSelectedKit(Player player) {
        String kitId = getSelectedKitId(player);
        return kitId == null ? null : kits.get(kitId);
    }

    public boolean selectKit(Player player, String kitId) {
        if (player == null || kitId == null) {
            return false;
        }

        String normalized = kitId.toLowerCase(Locale.ROOT);
        if (!kits.containsKey(normalized)) {
            return false;
        }

        selectedKits.put(player.getUniqueId(), normalized);
        return true;
    }

    public void applySelectedKit(Player player) {
        if (player == null) {
            return;
        }

        KitDefinition definition = getSelectedKit(player);
        if (definition == null) {
            return;
        }

        stripKitItems(player);
        for (KitDefinition kit : kits.values()) {
            for (PotionEffectType type : kit.effectTypes()) {
                if (type != null) {
                    player.removePotionEffect(type);
                }
            }
        }

        for (PotionEffect effect : definition.effects()) {
            player.removePotionEffect(effect.getType());
            player.addPotionEffect(effect);
        }

        for (KitItem item : definition.items()) {
            ItemStack stack = item.toItemStack(kitItemKey);
            if (item.slot() >= 0 && item.slot() < player.getInventory().getSize()) {
                if (placeInPreferredSlot(player, item.slot(), stack)) {
                    continue;
                }
            }

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            if (!leftover.isEmpty()) {
                leftover = player.getEnderChest().addItem(leftover.values().toArray(new ItemStack[0]));
            }
        }
    }

    private boolean placeInPreferredSlot(Player player, int slot, ItemStack stack) {
        PlayerInventory inventory = player.getInventory();
        ItemStack existing = inventory.getItem(slot);
        if (existing == null || existing.getType() == Material.AIR || isKitItem(existing)) {
            inventory.setItem(slot, stack);
            return true;
        }

        Map<Integer, ItemStack> moved = inventory.addItem(existing.clone());
        if (!moved.isEmpty()) {
            moved = player.getEnderChest().addItem(moved.values().toArray(new ItemStack[0]));
        }
        if (moved.isEmpty()) {
            inventory.setItem(slot, stack);
            return true;
        }
        return false;
    }

    public void stripKitItems(Player player) {
        stripKitItems(player.getInventory());
        stripKitItems(player.getEnderChest());
    }

    private void stripKitItems(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isKitItem(stack)) {
                inventory.setItem(slot, null);
            }
        }
    }

    public boolean isKitMenuItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        Byte marker = stack.getItemMeta().getPersistentDataContainer().get(kitMenuItemKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    public boolean isKitItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        Byte marker = stack.getItemMeta().getPersistentDataContainer().get(kitItemKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    @EventHandler
    public void onMenuItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getDeadPlayerService().isDead(player)) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isKitMenuItem(item)) {
            return;
        }

        if (plugin.getEndWarService() != null && plugin.getEndWarService().isGameRunning()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        openMenu(player);
    }

    @EventHandler
    public void onKitMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof KitMenuHolder holder) || !holder.viewer().equals(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        String action = clicked.getItemMeta().getPersistentDataContainer().get(kitMenuActionKey, PersistentDataType.STRING);
        if (action == null || !action.startsWith("kit:select:")) {
            return;
        }

        EndWarService endWarService = plugin.getEndWarService();
        if (endWarService != null
                && endWarService.isGameRunning()
                && player.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END
                && !plugin.getDeadPlayerService().isDead(player)) {
            plugin.getMessageManager().sendPrefixed(player, "kit-locked");
            player.closeInventory();
            return;
        }

        String kitId = action.substring("kit:select:".length());
        if (!selectKit(player, kitId)) {
            plugin.getMessageManager().sendPrefixed(player, "kit-select-failed");
            return;
        }

        plugin.getMessageManager().sendPrefixed(player, "kit-selected", Map.of("%kit%", kits.get(kitId).displayNamePlain()));
        if (endWarService != null && endWarService.isGameRunning()) {
            applySelectedKit(player);
        }
        player.closeInventory();
    }

    @EventHandler
    public void onKitMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof KitMenuHolder) {
            event.setCancelled(true);
        }
    }

    private ItemStack createMenuItem() {
        ItemStack stack = new ItemStack(menuMaterial);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            TextUtil.applyItemText(meta, menuName, menuLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(kitMenuItemKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Material parseMaterial(String rawValue, Material fallback) {
        if (rawValue == null) {
            return fallback;
        }
        Material material = Material.matchMaterial(rawValue.toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }

    public record KitMenuHolder(UUID viewer) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record KitDefinition(
            String id,
            String displayName,
            Material iconMaterial,
            List<String> description,
            List<PotionEffect> effects,
            List<KitItem> items,
            List<PotionEffectType> effectTypes
    ) {
        private static KitDefinition load(JavaPlugin plugin, String kitId, ConfigurationSection section) {
            String displayName = section.getString("name", "&f" + kitId);
            Material iconMaterial = Material.matchMaterial(section.getString("icon", "BOOK").toUpperCase(Locale.ROOT));
            if (iconMaterial == null) {
                iconMaterial = Material.BOOK;
            }

            List<PotionEffect> effects = new ArrayList<>();
            List<PotionEffectType> effectTypes = new ArrayList<>();
            for (Map<?, ?> rawMap : section.getMapList("effects")) {
                Object typeValue = rawMap.get("type");
                PotionEffectType type = typeValue == null ? null : TextUtil.parsePotionEffectType(typeValue.toString());
                if (type == null) {
                    continue;
                }
                int duration = parseInt(rawMap.get("duration"), 20 * 60 * 15);
                int amplifier = Math.max(0, parseInt(rawMap.get("amplifier"), 0));
                boolean ambient = parseBoolean(rawMap.get("ambient"), false);
                boolean particles = parseBoolean(rawMap.get("particles"), true);
                boolean icon = parseBoolean(rawMap.get("icon"), true);
                effectTypes.add(type);
                effects.add(new PotionEffect(type, duration, amplifier, ambient, particles, icon));
            }

            List<KitItem> items = new ArrayList<>();
            ConfigurationSection itemsSection = section.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                    if (itemSection == null || !itemSection.getBoolean("enabled", true)) {
                        continue;
                    }
                    items.add(KitItem.load(plugin, key, itemSection));
                }
            }

            return new KitDefinition(
                    kitId.toLowerCase(Locale.ROOT),
                    displayName,
                    iconMaterial,
                    section.getStringList("description"),
                    effects,
                    items,
                    effectTypes
            );
        }

        private ItemStack toDisplayItem(NamespacedKey actionKey) {
            ItemStack stack = new ItemStack(iconMaterial);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                TextUtil.applyItemText(meta, displayName, description);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "kit:select:" + id);
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private String displayNamePlain() {
            return TextUtil.stripColor(displayName);
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

    public record KitItem(
            Material material,
            int amount,
            int slot,
            PotionType potionType,
            String displayName,
            List<String> lore,
            boolean unbreakable,
            Map<Enchantment, Integer> enchantments
    ) {
        private static KitItem load(JavaPlugin plugin, String key, ConfigurationSection section) {
            Material material = Material.matchMaterial(section.getString("material", "STONE").toUpperCase(Locale.ROOT));
            if (material == null) {
                material = Material.STONE;
            }

            Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
            ConfigurationSection enchants = section.getConfigurationSection("enchantments");
            if (enchants != null) {
                for (String enchantKey : enchants.getKeys(false)) {
                    Enchantment enchantment = TextUtil.parseEnchantment(enchantKey);
                    if (enchantment != null) {
                        enchantments.put(enchantment, Math.max(1, enchants.getInt(enchantKey, 1)));
                    }
                }
            }

            return new KitItem(
                    material,
                    Math.max(1, section.getInt("amount", 1)),
                    parseSlot(section.get("slot")),
                    parsePotionType(section.getString("potion-type")),
                    section.getString("name", "&f" + key),
                    section.getStringList("lore"),
                    section.getBoolean("unbreakable", false),
                    enchantments
            );
        }

        private static int parseSlot(Object rawValue) {
            if (rawValue instanceof Number number) {
                return number.intValue();
            }
            if (rawValue == null) {
                return -1;
            }

            String normalized = rawValue.toString().trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "boots" -> 36;
                case "leggings" -> 37;
                case "chestplate" -> 38;
                case "helmet" -> 39;
                case "offhand" -> 40;
                default -> {
                    try {
                        yield Integer.parseInt(normalized);
                    } catch (NumberFormatException ignored) {
                        yield -1;
                    }
                }
            };
        }

        private static PotionType parsePotionType(String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                return null;
            }
            try {
                return PotionType.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        private ItemStack toItemStack(NamespacedKey itemKey) {
            ItemStack stack = new ItemStack(material, amount);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                if (meta instanceof PotionMeta potionMeta && potionType != null) {
                    potionMeta.setBasePotionType(potionType);
                    meta = potionMeta;
                }
                TextUtil.applyItemText(meta, displayName, lore);
                meta.setUnbreakable(unbreakable);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
                meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
                stack.setItemMeta(meta);
            }

            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                stack.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
            return stack;
        }
    }
}
