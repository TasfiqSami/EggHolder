package me.tasfiq.eggholder;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

/** Shared visual treatment for EggHolder's custom inventories. */
public final class GuiStyle {

    private GuiStyle() {
    }

    public static void frame(Inventory inventory) {
        ItemStack pane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.component("<dark_purple> "));
            pane.setItemMeta(meta);
        }

        int rows = inventory.getSize() / 9;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row == 0 || row == rows - 1 || column == 0 || column == 8) {
                inventory.setItem(slot, pane.clone());
            }
        }
    }

    public static List<Integer> contentSlots(Inventory inventory) {
        List<Integer> slots = new ArrayList<>();
        int rows = inventory.getSize() / 9;
        for (int row = 1; row < rows - 1; row++) {
            for (int column = 1; column < 8; column++) {
                slots.add(row * 9 + column);
            }
        }
        return slots;
    }

    public static ItemStack info(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            TextUtil.applyItemText(meta, name, lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static int sizeForContent(int count) {
        int rows = Math.max(3, Math.min(6, (Math.max(0, count) + 6) / 7 + 2));
        return rows * 9;
    }
}
