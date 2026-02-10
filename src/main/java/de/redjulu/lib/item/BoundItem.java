package de.redjulu.lib.item;

import de.redjulu.RedJuluLib;
import de.redjulu.lib.ItemBuilder;
import de.redjulu.lib.MessageHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Generic-Item, das an einen Spieler gebunden ist: nur Besitzer kann es nutzen, aufheben, tragen, weitergeben.
 * Beim ersten Nutzen wird das Item gebunden; danach wird der Besitzer per PDC und Lore angezeigt.
 */
public abstract class BoundItem extends GenericItem {

    /**
     * Erstellt ein Bound-Item (wie {@link GenericItem}, mit Bindung an Besitzer bei {@link #getItem(Player)}).
     *
     * @param id       Eindeutige ID.
     * @param category Kategorie.
     * @param builder  ItemBuilder für das Basis-Item (wird mit {@link ItemBuilder#setGenericId} versehen).
     */
    public BoundItem(@NotNull String id, @NotNull String category, @NotNull ItemBuilder builder) {
        super(id, category, builder);
    }

    /**
     * Liefert das Item ohne Besitzer-Bindung (wie {@link #getItem()}).
     *
     * @return Ungebundener ItemStack.
     */
    public @NotNull ItemStack getUnboundedItem() {
        return super.getItem();
    }

    /**
     * Liefert eine Kopie des ItemStacks, gebunden an den angegebenen Besitzer (PDC + Lore).
     *
     * @param owner Besitzer-Spieler.
     * @return Gebundener ItemStack.
     */
    public @NotNull ItemStack getItem(@NotNull Player owner) {
        return new ItemBuilder(super.getItem())
                .setBoundOwner(owner.getUniqueId())
                .appendLore(Component.empty(), MessageHelper.get("item.bound_to_lore", "player", owner.getName()))
                .build();
    }

    /**
     * Listener für Bound-Items: Tod (Items behalten), Respawn (zurückgeben), Pickup/Click/Drop nur für Besitzer.
     */
    public static class BoundListener implements Listener {

        private static final org.bukkit.NamespacedKey OWNER_KEY = ItemBuilder.boundOwnerKey();
        private final Map<UUID, List<ItemStack>> respawnItems = new HashMap<>();

        @EventHandler(priority = EventPriority.LOWEST)
        public void onDeath(PlayerDeathEvent e) {
            Player p = e.getEntity();
            List<ItemStack> drops = e.getDrops();
            List<ItemStack> toKeep = new ArrayList<>();

            Iterator<ItemStack> iterator = drops.iterator();
            while (iterator.hasNext()) {
                ItemStack item = iterator.next();
                if (isRestricted(item, p)) {
                    toKeep.add(item.clone());
                    iterator.remove();
                }
            }

            if (!toKeep.isEmpty()) {
                respawnItems.put(p.getUniqueId(), toKeep);
            }
        }

        @EventHandler
        public void onRespawn(PlayerRespawnEvent e) {
            Player p = e.getPlayer();
            List<ItemStack> items = respawnItems.remove(p.getUniqueId());

            if (items != null) {
                Bukkit.getScheduler().runTaskLater(RedJuluLib.getPlugin(), () -> {
                    for (ItemStack item : items) {
                        p.getInventory().addItem(item).values().forEach(over ->
                                p.getWorld().dropItemNaturally(p.getLocation(), over)
                        );
                    }
                }, 1L);
            }
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPickup(EntityPickupItemEvent e) {
            if (!(e.getEntity() instanceof Player p)) return;
            if (isRestricted(e.getItem().getItemStack(), p)) {
                e.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onInventoryClick(InventoryClickEvent e) {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            if (isRestricted(e.getCurrentItem(), p) || isRestricted(e.getCursor(), p)) {
                e.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onDrop(PlayerDropItemEvent e) {
            if (isRestricted(e.getItemDrop().getItemStack(), e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        private static boolean isRestricted(ItemStack item, Player player) {
            if (item == null || !item.hasItemMeta()) return false;
            String ownerUuid = item.getItemMeta().getPersistentDataContainer().get(OWNER_KEY, PersistentDataType.STRING);
            if (ownerUuid == null) return false;
            return !ownerUuid.equals(player.getUniqueId().toString());
        }
    }
}