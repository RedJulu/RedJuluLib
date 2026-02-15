package de.redjulu.lib.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class GUIListener implements Listener {

    private static final Map<Inventory, Map<Integer, BiConsumer<Player, ClickType>>> actions = new HashMap<>();

    public static void registerButton(Inventory inv, int slot, BiConsumer<Player, ClickType> action) {
        actions.computeIfAbsent(inv, k -> new HashMap<>()).put(slot, action);
    }

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
            if (gui.isPlaceholderSlot(slot)) {
                event.setCancelled(true);
                ItemStack inSlot = event.getCurrentItem();
                ItemStack cursor = event.getCursor();
                boolean cursorHasItem = cursor != null && cursor.getType() != Material.AIR;

                if (BaseGUI.isPlaceholderItem(inSlot)) {
                    if (cursorHasItem) {
                        event.setCursor(null);
                        event.getInventory().setItem(slot, cursor.clone());
                    }
                } else {
                    ItemStack userItem = (inSlot != null && inSlot.getType() != Material.AIR) ? inSlot : null;
                    if (userItem != null) {
                        ItemStack placeholder = gui.getPlaceholderForSlot(slot);
                        if (event.getClick().isShiftClick()) {
                            event.getInventory().setItem(slot, placeholder != null ? placeholder.clone() : null);
                            player.getInventory().addItem(userItem);
                        } else {
                            if (cursorHasItem) {
                                event.setCursor(userItem.clone());
                                event.getInventory().setItem(slot, cursor.clone());
                            } else {
                                event.setCursor(userItem.clone());
                                event.getInventory().setItem(slot, placeholder != null ? placeholder.clone() : null);
                            }
                        }
                        if (event.getInventory().getItem(slot) != null && BaseGUI.isPlaceholderItem(event.getInventory().getItem(slot))) {
                            gui.clearActiveItem(slot);
                        }
                    }
                }
                gui.updateDynamicButtons(player);
                return;
            }
            event.setCancelled(true);
            Map<Integer, BiConsumer<Player, ClickType>> invActions = actions.get(event.getInventory());
            if (invActions != null && invActions.containsKey(slot)) {
                invActions.get(slot).accept(player, event.getClick());
            }
        } else {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                Inventory top = event.getView().getTopInventory();
                if (top.getHolder() instanceof BaseGUI<?, ?> guiTop) {
                    ItemStack fromPlayer = event.getCurrentItem();
                    if (fromPlayer != null && fromPlayer.getType() != Material.AIR) {
                        for (int i = 0; i < top.getSize(); i++) {
                            if (!guiTop.isPlaceholderSlot(i) || !guiTop.isPrioritySlot(i)) continue;
                            ItemStack s = top.getItem(i);
                            if (s == null || !BaseGUI.isPlaceholderItem(s)) continue;

                            top.setItem(i, fromPlayer.clone());
                            fromPlayer.setAmount(0);
                            event.setCurrentItem(null);
                            event.setCancelled(true);
                            guiTop.updateDynamicButtons(player);
                            return;
                        }
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player && event.getInventory().getHolder() instanceof BaseGUI<?, ?> gui) {
            if (event.getInventory().getType().equals(InventoryType.ANVIL)) return;

            // Wenn ein Dialog offen ist (z.B. Suche), lassen wir das Inventar im Hintergrund "ruhen"
            if (!gui.isSwitching() && !gui.isDialogOpen()) {
                for (int i = 0; i < event.getInventory().getSize(); i++) {
                    if (gui.isPlaceholderSlot(i) || gui.isInteractableSlot(i)) {
                        ItemStack item = event.getInventory().getItem(i);
                        if (item != null && item.getType() != Material.AIR && !BaseGUI.isPlaceholderItem(item)) {
                            player.getInventory().addItem(item);
                        }
                    }
                }
            }
        }
        // Button-Aktionen werden nur gelöscht, wenn wir wirklich fertig sind
        if (event.getInventory().getHolder() instanceof BaseGUI<?, ?> gui && !gui.isDialogOpen()) {
            clearButtons(event.getInventory());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BaseGUI.clearHistory(event.getPlayer().getUniqueId());
    }
}