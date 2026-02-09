package de.redjulu.lib.item;

import de.redjulu.RedJuluLib;
import de.redjulu.lib.ItemBuilder;
import de.redjulu.lib.MessageHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
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

import java.util.*;

public abstract class BoundItem extends GenericItem {

    private final NamespacedKey ownerKey;

    public BoundItem(String id, String category, ItemBuilder builder) {
        super(id, category, builder);
        this.ownerKey = new NamespacedKey(RedJuluLib.getPlugin(), "bound_owner");
    }

    public ItemStack getUnboundedItem() {
        return super.getItem();
    }

    public ItemStack getItem(Player owner) {
        ItemStack item = super.getItem();
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());

            List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            if (lore == null) lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MessageHelper.get("item.bound_to_lore", "player", owner.getName()));
            meta.lore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    public static class BoundListener implements Listener {

        private final NamespacedKey ownerKey = new NamespacedKey(RedJuluLib.getPlugin(), "bound_owner");
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

        private boolean isRestricted(ItemStack item, Player player) {
            if (item == null || !item.hasItemMeta()) return false;
            String ownerUuid = item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (ownerUuid == null) return false;
            return !ownerUuid.equals(player.getUniqueId().toString());
        }
    }
}