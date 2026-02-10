package de.redjulu.lib.item;

import de.redjulu.RedJuluLib;
import de.redjulu.lib.ItemBuilder;
import de.redjulu.lib.MessageHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Basis für registrierte Items mit fester ID (PDC), Kategorien, Cooldowns und Interaktions-Logik.
 * Über {@link #getRegistry()} und {@link #getByCategory(String)} abfragbar.
 */
public abstract class GenericItem implements Listener {

    private static final Map<String, GenericItem> REGISTRY = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, String> LAST_VIEWED_ID = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> RUNNING_TASKS = new HashMap<>();
    private static final Map<UUID, Long> ERROR_BLINK = new HashMap<>();
    private static boolean listenerRegistered = false;

    protected final String id;
    protected final String category;
    protected final ItemStack itemStack;

    /**
     * Registriert ein Generic-Item mit ID, Kategorie und vom {@link ItemBuilder} erzeugtem Item.
     *
     * @param id       Eindeutige ID (wird per PDC gespeichert).
     * @param category Kategorie für {@link #getByCategory(String)}.
     * @param builder  ItemBuilder; wird mit {@link ItemBuilder#setGenericId(String)} ergänzt und gebaut.
     */
    public GenericItem(@NotNull String id, @NotNull String category, @NotNull ItemBuilder builder) {
        this.id = id;
        this.category = category;
        this.itemStack = builder.setGenericId(id).build();
        REGISTRY.put(id, this);
        if (!listenerRegistered) {
            Bukkit.getPluginManager().registerEvents(new GenericListener(), RedJuluLib.getPlugin());
            listenerRegistered = true;
        }
    }

    /** Wird bei Rechts-/Linksklick mit dem Item ausgelöst. */
    public void onInteract(PlayerInteractEvent event) {}

    /** Wird bei Interaktion mit einer Entity mit dem Item ausgelöst. */
    public void onEntityInteract(PlayerInteractEntityEvent event) {}

    /**
     * Liefert eine Kopie des Standard-ItemStacks (ohne spielerspezifische Daten).
     *
     * @return Klon des ItemStacks.
     */
    public @NotNull ItemStack getItem() {
        return itemStack.clone();
    }

    /** Alle registrierten Generic-Items (ID → Instanz). */
    public static @NotNull Map<String, GenericItem> getRegistry() {
        return REGISTRY;
    }

    /**
     * Alle Generic-Items einer Kategorie (case-insensitive).
     *
     * @param category Kategorie-Name.
     * @return Liste der Items.
     */
    public static @NotNull List<GenericItem> getByCategory(@NotNull String category) {
        return REGISTRY.values().stream()
                .filter(item -> item.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    /** Eindeutige ID dieses Items. */
    public @NotNull String getId() {
        return id;
    }

    /** Kategorie dieses Items. */
    public @NotNull String getCategory() {
        return category;
    }

    /**
     * Setzt einen Cooldown für diesen Spieler und dieses Item (Actionbar-Anzeige).
     *
     * @param player  Spieler.
     * @param seconds Dauer in Sekunden.
     */
    public void setCooldown(@NotNull Player player, double seconds) {
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

    /**
     * Prüft, ob für den Spieler bei diesem Item noch Cooldown aktiv ist.
     *
     * @param player Spieler.
     * @return true wenn Cooldown aktiv.
     */
    public boolean hasCooldown(@NotNull Player player) {
        Map<String, Long> playerCooldowns = COOLDOWNS.get(player.getUniqueId());
        return playerCooldowns != null && playerCooldowns.containsKey(id) && System.currentTimeMillis() < playerCooldowns.get(id);
    }

    private static String getHeldId(@NotNull Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ItemBuilder.genericIdKey(), PersistentDataType.STRING);
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
            String id = item.getItemMeta().getPersistentDataContainer().get(ItemBuilder.genericIdKey(), PersistentDataType.STRING);
            if (id == null || !REGISTRY.containsKey(id)) return;

            GenericItem generic = REGISTRY.get(id);
            if (generic instanceof BoundItem) {
                if (!item.getItemMeta().getPersistentDataContainer().has(ItemBuilder.boundOwnerKey(), PersistentDataType.STRING)) {
                    ItemStack bound = new ItemBuilder(item)
                            .setBoundOwner(e.getPlayer().getUniqueId())
                            .appendLore(Component.empty(), MessageHelper.get("item.bound_to_lore", "player", e.getPlayer().getName()))
                            .build();
                    e.getPlayer().getInventory().setItemInMainHand(bound);
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