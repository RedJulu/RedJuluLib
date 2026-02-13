package de.redjulu.lib.gui;

import de.redjulu.RedJuluLib;
import de.redjulu.lib.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Basis für alle Custom-GUIs mit einheitlichem Layout, Pagination, Animation und History.
 * Diese Klasse verwaltet das Inventar-Handling, automatische Button-Registrierung und
 * bietet Schutzmechanismen für interaktive Slots.
 *
 * @param <T> Datentyp der angezeigten Objekte in Listen.
 * @param <C> Enum-Typ für Kategorien oder Filter innerhalb der GUI.
 */
public abstract class BaseGUI<T, C extends Enum<C>> implements InventoryHolder {

    /** Speichert die GUI-Historie pro Spieler für die Zurück-Navigation. */
    private static final Map<UUID, BaseGUI<?, ?>> HISTORY = new HashMap<>();

    /** Das zugrunde liegende Bukkit-Inventar. */
    protected final Inventory inventory;
    /** Die Gesamtgröße des Inventars. */
    protected final int size;
    /** Liste der Slots, die für dynamischen Content (z.B. Items aus einer Liste) reserviert sind. */
    protected final List<Integer> contentSlots = new ArrayList<>();
    /** Slots, in die der Spieler Items legen kann. */
    protected final Set<Integer> interactableSlots = new HashSet<>();
    /** Slots, aus denen der Spieler Items entnehmen kann, ohne dass die Interaktion blockiert wird. */
    protected final Set<Integer> placeholder = new HashSet<>();
    /** Slots, die beim Shift-Klick bevorzugt behandelt werden. */
    protected final Set<Integer> prioritySlots = new HashSet<>();
    /** Slots, die bei einem Update nicht geleert werden (verhindert Head-Flackern). */
    protected final Set<Integer> ignoredSlots = new HashSet<>();
    /** Snapshots der ursprünglichen Items in interaktiven Slots zur Wiederherstellung (Placeholder-System). */
    protected final Map<Integer, ItemStack> slotPlaceholders = new HashMap<>();
    /** Speichert Frames für animierte Slots. */
    protected final Map<Integer, List<ItemStack>> animatedSlots = new HashMap<>();

    /** Speichert Items, die der Spieler in interaktive Slots gelegt hat, um sie über Updates hinweg zu erhalten. */
    protected final Map<Integer, ItemStack> activeItems = new HashMap<>();

    /** Sound, der beim Öffnen der GUI abgespielt wird. */
    protected Sound openSound = Sound.BLOCK_CHEST_OPEN;
    /** Sound, der bei jedem Button-Klick abgespielt wird. */
    protected Sound clickSound = Sound.UI_BUTTON_CLICK;
    /** Sound, der bei der Nutzung der back-Funktion abgespielt wird. */
    protected Sound backSound = Sound.ITEM_ARMOR_EQUIP_GENERIC;

    /** Liste aller verfügbaren Daten-Objekte für diese GUI. */
    protected List<T> allItems = new ArrayList<>();
    /** Die aktuell aktive Kategorie/Filterung. */
    protected C currentCategory;
    /** Die aktuelle Seite der Pagination (0-basiert). */
    protected int page = 0;
    /** Anzahl der maximalen Elemente pro Seite im Content-Bereich. */
    protected final int pageSize;

    /**
     * Erstellt eine neue GUI-Instanz.
     *
     * @param rows            Anzahl der Reihen (1-6).
     * @param titleKey        Sprach-Key aus der RedJuluLib oder ein MiniMessage-String.
     * @param t, b, l, r      Padding für den Content-Bereich (Top, Bottom, Left, Right).
     * @param defaultCategory Die initial gewählte Kategorie.
     */
    public BaseGUI(int rows, String titleKey, int t, int b, int l, int r, C defaultCategory) {
        this.size = rows * 9;
        this.currentCategory = defaultCategory;

        Component title = RedJuluLib.getLang().has(titleKey)
                ? RedJuluLib.getLang().get(titleKey)
                : MiniMessage.miniMessage().deserialize(titleKey);

        this.inventory = Bukkit.createInventory(this, size, title);

        for (int row = t; row < (rows - b); row++) {
            for (int col = l; col < (9 - r); col++) {
                contentSlots.add(col + (row * 9));
            }
        }
        this.pageSize = contentSlots.size();
    }

    /**
     * Löscht die GUI-History für einen Spieler (z. B. beim Logout).
     *
     * @param uuid UUID des Spielers.
     */
    public static void clearHistory(UUID uuid) {
        HISTORY.remove(uuid);
    }

    /**
     * Öffnet die GUI für einen Spieler und triggert {@link #update(Player)}.
     *
     * @param player        Ziel-Spieler.
     * @param saveToHistory Wenn true, wird die aktuelle GUI des Spielers in der History gespeichert.
     */
    public void open(Player player, boolean saveToHistory) {
        if (saveToHistory) {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof BaseGUI<?, ?> current) {
                HISTORY.put(player.getUniqueId(), current);
            }
        }
        update(player);
        player.openInventory(inventory);
        if (openSound != null) player.playSound(player.getLocation(), openSound, 0.5f, 1.0f);
    }

    /**
     * Baut den GUI-Inhalt neu auf (löscht Buttons, Animationen und triggert {@link #compose(Player)}).
     * Stellt zudem gespeicherte Spieler-Items aus activeItems wieder her.
     *
     * @param player Ziel-Spieler (für kontextabhängige Inhalte).
     */
    public void update(Player player) {
        GUIListener.clearButtons(inventory);

        for (int i = 0; i < size; i++) {
            if (!ignoredSlots.contains(i)) {
                inventory.setItem(i, null);
            }
        }

        animatedSlots.clear();
        interactableSlots.clear();
        placeholder.clear();
        prioritySlots.clear();
        slotPlaceholders.clear();
        ignoredSlots.clear();

        compose(player);

        activeItems.forEach((slot, item) -> {
            if (item != null && item.getType() != Material.AIR) {
                inventory.setItem(slot, item);
            }
        });

        tickAnimations(0);
    }

    /**
     * Führt ein Update im nächsten Server-Tick aus. Verhindert Probleme bei Inventar-Änderungen während Events.
     *
     * @param player Ziel-Spieler.
     */
    protected void updateDelayed(Player player) {
        Bukkit.getScheduler().runTask(RedJuluLib.getPlugin(), () -> update(player));
    }

    /**
     * Wechselt die aktuelle Kategorie, setzt die Seite auf 0 und aktualisiert die GUI.
     *
     * @param player   Ziel-Spieler.
     * @param category Neue Kategorie.
     */
    public void setCategory(Player player, C category) {
        this.currentCategory = category;
        this.page = 0;
        update(player);
    }

    /**
     * Öffnet das zuvor gespeicherte GUI (History) oder schließt das Inventar, falls keine History existiert.
     *
     * @param player Ziel-Spieler.
     */
    protected void back(Player player) {
        BaseGUI<?, ?> last = HISTORY.remove(player.getUniqueId());
        if (last != null) {
            last.open(player, false);
            if (backSound != null) player.playSound(player.getLocation(), backSound, 0.5f, 1.2f);
        } else {
            player.closeInventory();
        }
    }

    /**
     * Registriert einen statischen Button ohne Klick-Aktion, aber mit Klick-Sound.
     *
     * @param slot Inventar-Slot.
     * @param item Anzuzeigender ItemStack.
     */
    protected void setButton(int slot, ItemStack item) {
        setButton(slot, item, (p, c) -> {});
    }

    /**
     * Registriert einen interaktiven Button.
     *
     * @param slot   Inventar-Slot.
     * @param item   Anzuzeigender ItemStack.
     * @param action Funktion, die beim Klick ausgeführt wird (Player, ClickType).
     */
    protected void setButton(int slot, ItemStack item, BiConsumer<Player, ClickType> action) {
        ItemStack current = inventory.getItem(slot);

        boolean shouldSet = true;
        if (current != null && current.getType() != Material.AIR) {
            if (ignoredSlots.contains(slot)) {
                if (current.getType() == item.getType()) {
                    shouldSet = false;
                }
            } else if (current.isSimilar(item)) {
                shouldSet = false;
            }
        }

        if (shouldSet) {
            inventory.setItem(slot, item);
        }

        GUIListener.registerButton(inventory, slot, (p, click) -> {
            if (clickSound != null) p.playSound(p.getLocation(), clickSound, 0.5f, 1.0f);
            action.accept(p, click);
        });
    }

    /**
     * Registriert einen Button, der seinen Zustand basierend auf einer Bedingung ändert.
     *
     * @param slot      Inventar-Slot.
     * @param condition Bedingung, ob der Button aktiv ist.
     * @param active    Item, wenn Bedingung erfüllt.
     * @param inactive  Item, wenn Bedingung nicht erfüllt.
     * @param action    Aktion bei Klick (wird nur ausgeführt, wenn Bedingung erfüllt).
     */
    protected void setDynamicButton(int slot, Predicate<Player> condition, ItemStack active, ItemStack inactive, BiConsumer<Player, ClickType> action) {
        Player viewer = inventory.getViewers().isEmpty() ? null : (Player) inventory.getViewers().get(0);
        boolean met = viewer != null && condition.test(viewer);

        setButton(slot, met ? active : inactive, (p, click) -> {
            if (condition.test(p)) {
                action.accept(p, click);
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            }
        });
    }

    /**
     * Registriert statische Buttons für mehrere Slots gleichzeitig.
     *
     * @param slots Liste der Slots.
     * @param item  Anzuzeigender ItemStack.
     */
    protected void setButtons(Iterable<Integer> slots, ItemStack item) {
        setButtons(slots, item, (p, c) -> {});
    }

    /**
     * Registriert funktionale Buttons für mehrere Slots gleichzeitig.
     *
     * @param slots  Liste der Slots.
     * @param item   Anzuzeigender ItemStack.
     * @param action Gemeinsame Klick-Aktion.
     */
    protected void setButtons(Iterable<Integer> slots, ItemStack item, BiConsumer<Player, ClickType> action) {
        for (int slot : slots) setButton(slot, item, action);
    }

    /**
     * Markiert einen Slot als interaktionsfähig. Platzhalter werden automatisch durch
     * das Einlegen eines Items ersetzt und beim Entfernen wiederhergestellt.
     * Erstellt einen Klon des aktuellen Items im Slot als permanenten Platzhalter.
     *
     * @param slot         Der Ziel-Slot.
     * @param interactable true, wenn Interaktion aktiviert werden soll.
     */
    protected void setInteractable(int slot, @Nullable Boolean interactable) {
        if (interactable == null) interactable = true;
        if (interactable) {
            interactableSlots.add(slot);
        } else {
            interactableSlots.remove(slot);
        }
    }

    /**
     * Markiert mehrere Slots als interaktionsfähig.
     *
     * @param slots        Die Slots.
     * @param interactable true für Aktivierung.
     */
    protected void setInteractable(Iterable<Integer> slots, @Nullable Boolean interactable) {
        if (interactable == null) interactable = true;
        for (int slot : slots) setInteractable(slot, interactable);
    }

    /**
     * Markiert einen Slot als Placeholder.
     *
     * @param slot        Der Ziel-Slot.
     * @param isPlaceholder true für Aktivierung.
     */
    protected void setPlaceholder(int slot, boolean isPlaceholder) {
        if(!isInteractableSlot(slot)) setInteractable(slot, true);
        if(!isPrioritySlot(slot)) setPriority(slot, true);

        if (isPlaceholder) {
            this.placeholder.add(slot);
            if (!activeItems.containsKey(slot)) {
                ItemStack current = inventory.getItem(slot);
                if (current != null && current.getType() != Material.AIR) {
                    slotPlaceholders.putIfAbsent(slot, current.clone());
                }
            }

            ItemStack itemInInv = inventory.getItem(slot);
            if (itemInInv == null || itemInInv.getType() == Material.AIR) {
                ItemStack ph = slotPlaceholders.get(slot);
                if (ph != null) inventory.setItem(slot, ph);
            }
        } else {
            this.placeholder.remove(slot);
            slotPlaceholders.remove(slot);
        }
    }

    /**
     * Markiert einen Slot als Placeholder.
     *
     * @param slot        Der Ziel-Slot.
     */
    protected void setPlaceholder(int slot) {
        setPlaceholder(slot, true);
    }

    /**
     * Markiert mehrere Slots als placeholder.
     *
     * @param slots   Die Slots.
     * @param placeholder true für Aktivierung.
     */
    protected void setPlaceholders(Iterable<Integer> slots, boolean placeholder) {
        slots.forEach(slot -> {
            if (!isInteractableSlot(slot)) setInteractable(slot, true);
            if (!isPrioritySlot(slot)) setPriority(slot, true);
        });
        for (int slot : slots) setPlaceholder(slot, placeholder);
    }

    /**
     * Markiert mehrere Slots als placeholder.
     *
     * @param slots   Die Slots.
     */
    protected void setPlaceholders(Iterable<Integer> slots) {
        slots.forEach(slot -> {
            if (!isInteractableSlot(slot)) setInteractable(slot, true);
            if (!isPrioritySlot(slot)) setPriority(slot, true);
        });
        for (int slot : slots) setPlaceholder(slot);
    }

    /**
     * Markiert einen Slot als Prioritätsslot für Shift-Klicks.
     *
     * @param slot     Der Slot.
     * @param priority true für hohe Priorität.
     */
    protected void setPriority(int slot, boolean priority) {
        if (priority) prioritySlots.add(slot);
        else prioritySlots.remove(slot);
    }

    /**
     * Markiert einen Slot als ignoriert für Clear-Vorgänge (verhindert Flackern).
     *
     * @param slot Der Slot.
     */
    protected void setIgnored(int slot) {
        ignoredSlots.add(slot);
    }

    /**
     * Wird aufgerufen, wenn sich der Inhalt eines interaktiven Slots ändert.
     * Verwaltet automatisch die activeItems Map basierend auf Platzhaltern.
     *
     * @param player Der handelnde Spieler.
     * @param slot   Der betroffene Slot.
     * @param item   Das neue Item im Slot (null bei Leerung/Placeholder).
     */
    public void onItemChange(Player player, int slot, ItemStack item) {
        ItemStack ph = getPlaceholder(slot);
        if (item == null || item.getType() == Material.AIR || (ph != null && item.isSimilar(ph))) {
            activeItems.remove(slot);
        } else {
            activeItems.put(slot, item);
        }
    }

    /**
     * Wird aufgerufen, wenn das Inventar geschlossen wird.
     * Gibt alle Items aus activeItems an den Spieler zurück.
     *
     * @param player Der Spieler, der das Inventar schließt.
     */
    public void handleClose(Player player) {
        activeItems.values().forEach(item -> {
            if (item != null && item.getType() != Material.AIR) {
                Map<Integer, ItemStack> remaining = player.getInventory().addItem(item);
                remaining.values().forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
            }
        });
        activeItems.clear();
    }

    /**
     * Gibt das aktuell vom Spieler abgelegte Item in einem Slot zurück.
     *
     * @param slot Der Slot.
     * @return Das gespeicherte ItemStack oder null.
     */
    protected ItemStack getActiveItem(int slot) {
        return activeItems.get(slot);
    }

    /**
     * Aktualisiert animierte Slots basierend auf einem Tick-Wert.
     * Wird üblicherweise durch einen globalen Task aufgerufen.
     *
     * @param tick Aktueller Animations-Tick.
     */
    public void tickAnimations(long tick) {
        if (animatedSlots.isEmpty()) return;
        for (Map.Entry<Integer, List<ItemStack>> entry : animatedSlots.entrySet()) {
            List<ItemStack> frames = entry.getValue();
            if (frames == null || frames.isEmpty()) continue;
            inventory.setItem(entry.getKey(), frames.get((int) (tick % frames.size())));
        }
    }

    /**
     * Prüft, ob ein Slot als interactable registriert ist.
     *
     * @param slot Slot-Index.
     * @return true wenn registriert.
     */
    public boolean isInteractableSlot(int slot) {
        return interactableSlots.contains(slot);
    }

    /**
     * Prüft, ob ein Slot als placeholder registriert ist.
     *
     * @param slot Slot-Index.
     * @return true wenn registriert.
     */
    public boolean isPlaceholder(int slot) {
        return placeholder.contains(slot);
    }

    /**
     * Prüft, ob ein Slot ein Prioritätsslot ist.
     *
     * @param slot Slot-Index.
     * @return true wenn Priorität.
     */
    public boolean isPrioritySlot(int slot) {
        return prioritySlots.contains(slot);
    }

    /**
     * Liefert das Klon-Item (Platzhalter) für einen Slot zurück.
     *
     * @param slot Der Slot-Index.
     * @return Der ursprüngliche ItemStack-Klon.
     */
    public ItemStack getPlaceholder(int slot) {
        return slotPlaceholders.get(slot);
    }

    /**
     * Rendert die aktuelle Seite der gefilterten Liste in den Content-Bereich der GUI.
     *
     * @param filter   Prädikat zur Filterung der {@link #allItems}.
     * @param renderer Logik zum Rendern eines einzelnen Elements in einen Slot.
     */
    protected void renderPage(Predicate<T> filter, BiConsumer<T, Integer> renderer) {
        List<T> filteredItems = allItems.stream().filter(filter).toList();
        int start = page * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int slot = contentSlots.get(i);
            int index = start + i;
            if (index < filteredItems.size()) renderer.accept(filteredItems.get(index), slot);
            else inventory.setItem(slot, null);
        }
    }

    /**
     * Füllt alle Slots außerhalb des Content-Bereichs mit einem Standard-Platzhalter.
     *
     * @param material Material für den Hintergrund.
     */
    protected void fillBackground(@NotNull Material material) {
        fillBackground(material, true);
    }

    /**
     * Füllt den Hintergrund der GUI mit einem Material.
     *
     * @param material         Das Material.
     * @param fillInteractable Wenn false, werden interaktive Slots übersprungen.
     */
    protected void fillBackground(@NotNull Material material, boolean fillInteractable) {
        ItemStack item = ItemBuilder.placeholder(material).build();
        for (int i = 0; i < size; i++) {
            if (!contentSlots.contains(i)) {
                if (!fillInteractable && (isInteractableSlot(i) || isPlaceholder(i))) continue;
                if (ignoredSlots.contains(i)) continue;
                inventory.setItem(i, item);
            }
        }
    }

    /**
     * Füllt den gesamten Content-Bereich mit einem Material.
     *
     * @param material Material für den Content-Bereich.
     */
    protected void fillContentArea(@NotNull Material material) {
        fillContentArea(material, true);
    }

    /**
     * Füllt den Content-Bereich der GUI mit einem Material.
     *
     * @param material         Das Material.
     * @param fillInteractable Wenn false, werden interaktive Slots übersprungen.
     */
    protected void fillContentArea(@NotNull Material material, boolean fillInteractable) {
        ItemStack item = ItemBuilder.placeholder(material).build();
        for (int slot : contentSlots) {
            if (!fillInteractable && (isInteractableSlot(slot) || isPlaceholder(slot))) continue;
            if (ignoredSlots.contains(slot)) continue;
            inventory.setItem(slot, item);
        }
    }

    /**
     * Fügt automatisch Vor- und Zurück-Buttons für die Pagination hinzu.
     * Buttons werden nur funktional, wenn eine entsprechende Seite existiert.
     *
     * @param prev Slot für die vorherige Seite.
     * @param next Slot für die nächste Seite.
     * @param p    Der Spieler, dem die GUI gehört.
     * @param f    Der Filter, der für die Berechnung der Seitenanzahl genutzt werden soll.
     */
    protected void addPaginationButtons(int prev, int next, Player p, Predicate<T> f) {
        List<T> filtered = allItems.stream().filter(f).toList();
        if (page > 0) setButton(prev, new ItemBuilder(Material.ARROW).setName(RedJuluLib.getLang().get("gui.prev_page")).build(), (pl, c) -> { page--; update(pl); });
        else inventory.setItem(prev, new ItemBuilder(Material.ARROW).setName(RedJuluLib.getLang().get("gui.no_prev")).build());

        if ((page + 1) * pageSize < filtered.size()) setButton(next, new ItemBuilder(Material.ARROW).setName(RedJuluLib.getLang().get("gui.next_page")).build(), (pl, c) -> { page++; update(pl); });
        else inventory.setItem(next, new ItemBuilder(Material.ARROW).setName(RedJuluLib.getLang().get("gui.no_next")).build());
    }

    /**
     * Registriert eine Item-Animation für einen Slot.
     *
     * @param slot   Der Slot.
     * @param frames Liste von Items, die nacheinander angezeigt werden.
     */
    protected void setAnimatedItem(int slot, List<ItemStack> frames) {
        animatedSlots.put(slot, frames);
    }

    /**
     * Abstrakte Methode zum Aufbau des GUI-Layouts. Hier werden Buttons und Slots definiert.
     *
     * @param player Spieler, für den die GUI aufgebaut wird.
     */
    public abstract void compose(Player player);

    @Override public @NotNull Inventory getInventory() { return inventory; }
}