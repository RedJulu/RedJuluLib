package de.redjulu.lib.item;

import de.redjulu.RedJuluLib;
import de.redjulu.lib.ItemBuilder;
import de.redjulu.lib.MessageHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class GenericItem implements Listener {

    private static final Map<String, GenericItem> REGISTRY = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, String> LAST_VIEWED_ID = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> RUNNING_TASKS = new HashMap<>();
    private static final Map<UUID, Long> ERROR_BLINK = new HashMap<>();
    private static boolean listenerRegistered = false;

    protected final String id;
    protected final String category;
    protected final NamespacedKey key;
    protected final ItemStack itemStack;

    public GenericItem(String id, String category, ItemBuilder builder) {
        this.id = id;
        this.category = category;
        this.key = new NamespacedKey(RedJuluLib.getPlugin(), "generic_id");
        this.itemStack = builder.pdc(key, PersistentDataType.STRING, id).build();
        REGISTRY.put(id, this);
        if (!listenerRegistered) {
            Bukkit.getPluginManager().registerEvents(new GenericListener(), RedJuluLib.getPlugin());
            listenerRegistered = true;
        }
    }

    public void onInteract(PlayerInteractEvent event) {}
    public void onEntityInteract(PlayerInteractEntityEvent event) {}

    public ItemStack getItem() {
        return itemStack.clone();
    }

    public static Map<String, GenericItem> getRegistry() {
        return REGISTRY;
    }

    public static List<GenericItem> getByCategory(String category) {
        return REGISTRY.values().stream()
                .filter(item -> item.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public void setCooldown(Player player, double seconds) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = COOLDOWNS.computeIfAbsent(uuid, k -> new HashMap<>());
        long end = System.currentTimeMillis() + (long) (seconds * 1000L);
        playerCooldowns.put(id, end);
        LAST_VIEWED_ID.put(uuid, id);

        Map<String, Integer> tasks = RUNNING_TASKS.computeIfAbsent(uuid, k -> new HashMap<>());
        if (tasks.containsKey(id)) return;

        String itemName = getPlainName(itemStack);
        BukkitRunnable runnable = new BukkitRunnable() {
            private int finishTicks = 0;
            private boolean playedSound = false;

            @Override
            public void run() {
                long remainingMs = end - System.currentTimeMillis();
                if (!player.isOnline()) {
                    stopTask(uuid);
                    this.cancel();
                    return;
                }

                String handId = getHeldId(player);
                if (handId != null && COOLDOWNS.getOrDefault(uuid, new HashMap<>()).containsKey(handId)) {
                    LAST_VIEWED_ID.put(uuid, handId);
                }

                String displayId = LAST_VIEWED_ID.get(uuid);
                if (!id.equals(displayId)) return;

                if (remainingMs <= 0) {
                    if (!playedSound) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.8f);
                        playedSound = true;
                    }
                    if (finishTicks < 20) {
                        player.sendActionBar(MessageHelper.get("system.cooldown_ready", "item", itemName, "bar", getFullBar("<green>")));
                        finishTicks += 2;
                        return;
                    }
                    stopTask(uuid);
                    this.cancel();
                    return;
                }

                if (ERROR_BLINK.getOrDefault(uuid, 0L) > System.currentTimeMillis()) {
                    player.sendActionBar(MessageHelper.get("system.cooldown_format", "item", itemName, "bar", getFullBar("<red>"), "seconds", String.format("%.1f", remainingMs / 1000.0)));
                    return;
                }

                double progress = 1.0 - ((double) remainingMs / (seconds * 1000.0));
                player.sendActionBar(MessageHelper.get("system.cooldown_format", "item", itemName, "bar", getProgressBar(progress), "seconds", String.format("%.1f", remainingMs / 1000.0)));
            }

            private void stopTask(UUID uuid) {
                if (COOLDOWNS.containsKey(uuid)) COOLDOWNS.get(uuid).remove(id);
                if (id.equals(LAST_VIEWED_ID.get(uuid))) LAST_VIEWED_ID.remove(uuid);
                if (RUNNING_TASKS.containsKey(uuid)) RUNNING_TASKS.get(uuid).remove(id);
            }
        };
        tasks.put(id, runnable.runTaskTimer(RedJuluLib.getPlugin(), 0, 2).getTaskId());
    }

    public boolean hasCooldown(Player player) {
        Map<String, Long> playerCooldowns = COOLDOWNS.get(player.getUniqueId());
        return playerCooldowns != null && playerCooldowns.containsKey(id) && System.currentTimeMillis() < playerCooldowns.get(id);
    }

    private String getHeldId(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private String getFullBar(String color) {
        StringBuilder bar = new StringBuilder(color);
        for (int i = 0; i < 15; i++) {
            bar.append("|");
            if (i < 14) bar.append("<bold> </bold>");
        }
        return bar.toString();
    }

    private String getProgressBar(double progress) {
        int totalBars = 15;
        int filledBars = (int) (totalBars * progress);
        StringBuilder bar = new StringBuilder("<aqua>");
        for (int i = 0; i < totalBars; i++) {
            if (i == filledBars) bar.append("<dark_gray>");
            bar.append("|");
            if (i < totalBars - 1) bar.append("<bold> </bold>");
        }
        return bar.toString();
    }

    private String getPlainName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component nameComp = meta.displayName();
            if (nameComp != null) return PlainTextComponentSerializer.plainText().serialize(nameComp);
        }
        return id;
    }

    private static class GenericListener implements Listener {
        @EventHandler
        public void onInteract(PlayerInteractEvent e) {
            ItemStack item = e.getItem();
            if (item == null || !item.hasItemMeta()) return;
            String id = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(RedJuluLib.getPlugin(), "generic_id"), PersistentDataType.STRING);
            if (id != null && REGISTRY.containsKey(id)) {
                GenericItem generic = REGISTRY.get(id);
                if (generic instanceof BoundItem) {
                    NamespacedKey ownerKey = new NamespacedKey(RedJuluLib.getPlugin(), "bound_owner");
                    if (!item.getItemMeta().getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
                        ItemMeta meta = item.getItemMeta();
                        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, e.getPlayer().getUniqueId().toString());
                        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
                        if (lore == null) lore = new ArrayList<>();
                        lore.add(Component.empty());
                        lore.add(MessageHelper.get("item.bound_to_lore", "player", e.getPlayer().getName()));
                        meta.lore(lore);
                        item.setItemMeta(meta);
                        MessageHelper.send(e.getPlayer(), "system.item_bound_to_you");
                        return;
                    }
                }
                if (generic.hasCooldown(e.getPlayer())) {
                    ERROR_BLINK.put(e.getPlayer().getUniqueId(), System.currentTimeMillis() + 400);
                    MessageHelper.playError(e.getPlayer());
                    return;
                }
                generic.onInteract(e);
            }
        }
    }
}