package de.redjulu.lib.gui;

import de.redjulu.RedJuluLib;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

public class GUIListener implements Listener {

    private static final Map<Inventory, Map<Integer, BiConsumer<Player, ClickType>>> actions = new WeakHashMap<>();

    public static void registerButton(Inventory inv, int slot, BiConsumer<Player, ClickType> action) {
        actions.computeIfAbsent(inv, k -> new HashMap<>()).put(slot, action);
    }

    public static void clearButtons(Inventory inv) {
        actions.remove(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (!(event.getInventory().getHolder() instanceof BaseGUI<?, ?> gui)) return;

        int slot = event.getRawSlot();
        int invSize = event.getInventory().getSize();
        boolean isInteractable = gui.isInteractableSlot(slot);
        boolean isPlaceholder = gui.isPlaceholder(slot);

        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR || event.getAction() == InventoryAction.HOTBAR_SWAP) {
            event.setCancelled(true);
            return;
        }

        if (slot < invSize && slot >= 0) {
            if (isPlaceholder) {
                ItemStack current = event.getCurrentItem();
                ItemStack placeholder = gui.getPlaceholder(slot);
                ItemStack cursor = event.getCursor();

                if (current != null && placeholder != null && current.isSimilar(placeholder)) {
                    if (cursor != null && cursor.getType() != Material.AIR) {
                        event.setCancelled(true);
                        ItemStack toPlace = cursor.clone();

                        event.getInventory().setItem(slot, toPlace);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);

                        Bukkit.getScheduler().runTask(RedJuluLib.getPlugin(), () -> {
                            player.setItemOnCursor(null);
                            player.updateInventory();
                            gui.onItemChange(player, slot, toPlace);
                        });
                    } else {
                        event.setCancelled(true);
                        Map<Integer, BiConsumer<Player, ClickType>> invActions = actions.get(event.getInventory());
                        if (invActions != null && invActions.containsKey(slot)) {
                            invActions.get(slot).accept(player, event.getClick());
                        }
                    }
                    return;
                }
            }

            if (!isInteractable && !isPlaceholder) {
                event.setCancelled(true);
                Map<Integer, BiConsumer<Player, ClickType>> invActions = actions.get(event.getInventory());
                if (invActions != null && invActions.containsKey(slot)) {
                    invActions.get(slot).accept(player, event.getClick());
                }
                return;
            }

            handlePlaceholderRestoration(gui, player, event.getInventory(), slot);
        } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            handleShiftClick(event, gui, invSize, player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof BaseGUI<?, ?> gui)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                if (!gui.isInteractableSlot(slot) && !gui.isPlaceholder(slot)) {
                    event.setCancelled(true);
                    return;
                }
                handlePlaceholderRestoration(gui, player, event.getInventory(), slot);
            }
        }
    }

    private void handlePlaceholderRestoration(BaseGUI<?, ?> gui, Player player, Inventory inv, int slot) {
        if (!gui.isPlaceholder(slot)) return;

        Bukkit.getScheduler().runTask(RedJuluLib.getPlugin(), () -> {
            ItemStack current = inv.getItem(slot);
            ItemStack placeholder = gui.getPlaceholder(slot);

            if (current == null || current.getType() == Material.AIR) {
                inv.setItem(slot, placeholder != null ? placeholder.clone() : null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
                gui.onItemChange(player, slot, null);
            } else if (placeholder != null && !current.isSimilar(placeholder)) {
                gui.onItemChange(player, slot, current);
            }
        });
    }

    private void handleShiftClick(InventoryClickEvent event, BaseGUI<?, ?> gui, int invSize, Player player) {
        ItemStack itemToMove = event.getCurrentItem();
        if (itemToMove == null || itemToMove.getType() == Material.AIR) return;

        event.setCancelled(true);

        if (tryMoveToSlots(event, gui, invSize, player, itemToMove, true)) {
            event.setCurrentItem(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
            player.updateInventory();
            return;
        }

        if (tryMoveToSlots(event, gui, invSize, player, itemToMove, false)) {
            event.setCurrentItem(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
            player.updateInventory();
        }
    }

    private boolean tryMoveToSlots(InventoryClickEvent event, BaseGUI<?, ?> gui, int invSize, Player player, ItemStack itemToMove, boolean priorityOnly) {
        for (int i = 0; i < invSize; i++) {
            if (priorityOnly && !gui.isPrioritySlot(i)) continue;
            if (!gui.isInteractableSlot(i) && !gui.isPlaceholder(i)) continue;

            ItemStack targetItem = event.getInventory().getItem(i);
            ItemStack placeholder = gui.getPlaceholder(i);

            boolean isAir = targetItem == null || targetItem.getType() == Material.AIR;
            boolean isPlaceholderActive = !isAir && placeholder != null && targetItem.isSimilar(placeholder);

            if (isAir || isPlaceholderActive) {
                ItemStack clone = itemToMove.clone();
                event.getInventory().setItem(i, clone);
                gui.onItemChange(player, i, clone);
                return true;
            } else if (targetItem.isSimilar(itemToMove)) {
                int maxStack = targetItem.getMaxStackSize();
                int currentAmount = targetItem.getAmount();
                int moveAmount = itemToMove.getAmount();

                if (currentAmount < maxStack) {
                    int added = Math.min(maxStack - currentAmount, moveAmount);
                    targetItem.setAmount(currentAmount + added);
                    itemToMove.setAmount(moveAmount - added);
                    gui.onItemChange(player, i, targetItem.clone());
                    if (itemToMove.getAmount() <= 0) return true;
                }
            }
        }
        return itemToMove.getAmount() <= 0;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof BaseGUI<?, ?> gui) {
            gui.handleClose(player);
            clearButtons(event.getInventory());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BaseGUI.clearHistory(event.getPlayer().getUniqueId());
    }
}