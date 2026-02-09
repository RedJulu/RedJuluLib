package de.redjulu.lib.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class GUIListener implements Listener {

    private static final Map<Inventory, Map<Integer, BiConsumer<Player, ClickType>>> actions = new HashMap<>();

    /**
     * @param inv Inventory instance.
     * @param slot Slot index.
     * @param action Callback for player and click type.
     */
    public static void registerButton(Inventory inv, int slot, BiConsumer<Player, ClickType> action) {
        actions.computeIfAbsent(inv, k -> new HashMap<>()).put(slot, action);
    }

    /**
     * @param inv Inventory to clear.
     */
    public static void clearButtons(Inventory inv) {
        actions.remove(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof BaseGUI<?, ?> gui)) return;

        int slot = event.getRawSlot();
        int invSize = event.getInventory().getSize();

        if (slot < invSize && slot >= 0) {
            if (gui.isInteractableSlot(slot)) {
                return;
            }

            event.setCancelled(true);
            Map<Integer, BiConsumer<Player, ClickType>> invActions = actions.get(event.getInventory());
            if (invActions != null && invActions.containsKey(slot)) {
                invActions.get(slot).accept(player, event.getClick());
            }
        } else {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        clearButtons(event.getInventory());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BaseGUI.clearHistory(event.getPlayer().getUniqueId());
    }
}