package me.tasfiq.eggholder;

import java.util.Collections;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class TeleportMenuListener implements Listener {

    private final EggHolderPlugin plugin;
    private final DeadPlayerService deadPlayerService;

    public TeleportMenuListener(EggHolderPlugin plugin, DeadPlayerService deadPlayerService) {
        this.plugin = plugin;
        this.deadPlayerService = deadPlayerService;
    }

    @EventHandler
    public void onTeleporterUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!deadPlayerService.isDead(player)) {
            return;
        }

        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!deadPlayerService.isTeleporterItem(item)) {
            return;
        }

        event.setCancelled(true);

        Inventory menu = deadPlayerService.createTeleporterMenu(player);
        boolean hasTargets = false;
        for (ItemStack stack : menu.getContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                hasTargets = true;
                break;
            }
        }

        if (!hasTargets) {
            plugin.getMessageManager().sendPrefixed(player, "teleporter-no-targets");
            return;
        }

        player.openInventory(menu);
    }

    @EventHandler
    public void onTeleportMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof DeadPlayerService.TeleportMenuHolder)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        String targetValue = clickedItem.getItemMeta().getPersistentDataContainer().get(
                deadPlayerService.getTeleporterTargetKey(),
                PersistentDataType.STRING
        );
        if (targetValue == null) {
            return;
        }

        UUID targetUuid;
        try {
            targetUuid = UUID.fromString(targetValue);
        } catch (IllegalArgumentException exception) {
            plugin.getMessageManager().sendPrefixed(player, "teleporter-target-unavailable");
            return;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline() || deadPlayerService.isDead(target)) {
            plugin.getMessageManager().sendPrefixed(player, "teleporter-target-unavailable");
            return;
        }

        player.teleport(target.getLocation());
        plugin.getMessageManager().sendPrefixed(
                player,
                "teleporter-teleported",
                Collections.singletonMap("%player%", target.getName())
        );

        if (plugin.getPluginConfig().isTeleporterCloseAfterClick()) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onTeleportMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof DeadPlayerService.TeleportMenuHolder) {
            event.setCancelled(true);
        }
    }
}
