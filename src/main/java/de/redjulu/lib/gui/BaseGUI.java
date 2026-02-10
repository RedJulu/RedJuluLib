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

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Basis für alle Custom-GUIs mit einheitlichem Layout, Pagination, Animation und History.
 * <ul>
 *   <li>Content-Bereich über Zeilen/Spalten (top, bottom, left, right) konfigurierbar</li>
 *   <li>Pagination über {@link #renderPage} und {@link #addPaginationButtons}</li>
 *   <li>Animation über {@link #setAnimatedItem} und {@link #tickAnimations}</li>
 *   <li>Zurück-Navigation über {@link #back(Player)} und History</li>
 *   <li>Kategorien über {@link #setCategory(Player, Enum)} und Filter</li>
 * </ul>
 *
 * @param <T> Datentyp der angezeigten Objekte.
 * @param <C> Enum-Typ für Kategorien/Filter.
 */
public abstract class BaseGUI<T, C extends Enum<C>> implements InventoryHolder {

    private static final Map<UUID, BaseGUI<?, ?>> HISTORY = new HashMap<>();

    protected final Inventory inventory;
    protected final int size;
    protected final List<Integer> contentSlots = new ArrayList<>();
    protected final Map<Integer, List<ItemStack>> animatedSlots = new HashMap<>();

    protected Sound openSound = Sound.BLOCK_CHEST_OPEN;
    protected Sound clickSound = Sound.UI_BUTTON_CLICK;
    protected Sound backSound = Sound.ITEM_ARMOR_EQUIP_GENERIC;

    protected List<T> allItems = new ArrayList<>();
    protected C currentCategory;
    protected int page = 0;
    protected final int pageSize;

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
     * Öffnet die GUI für einen Spieler.
     *
     * @param player        Ziel-Spieler.
     * @param saveToHistory true = aktuelles GUI in History speichern (für Zurück-Button).
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
     * Baut den GUI-Inhalt neu auf (Slots, Buttons, Animationen).
     *
     * @param player Ziel-Spieler (für kontextabhängige Inhalte).
     */
    public void update(Player player) {
        GUIListener.clearButtons(inventory);
        inventory.clear();
        animatedSlots.clear();
        compose(player);
        tickAnimations(0);
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
     * Öffnet das zuvor gespeicherte GUI (History) oder schließt das Inventar.
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
     * Registriert einen klickbaren Button (inkl. Klick-Sound).
     *
     * @param slot   Inventar-Slot.
     * @param item   Anzuzeigender ItemStack.
     * @param action Aktion bei Klick (Spieler, ClickType).
     */
    protected void setButton(int slot, ItemStack item, BiConsumer<Player, ClickType> action) {
        inventory.setItem(slot, item);
        GUIListener.registerButton(inventory, slot, (p, click) -> {
            if (clickSound != null) p.playSound(p.getLocation(), clickSound, 0.5f, 1.0f);
            action.accept(p, click);
        });
    }

    /**
     * Aktualisiert animierte Slots (Frame-Wechsel).
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
     * Gibt an, ob ein Slot interagierbar ist (z. B. für Shift-Click-Schutz).
     *
     * @param slot Slot-Index.
     * @return true wenn der Slot nicht gesperrt ist.
     */
    public boolean isInteractableSlot(int slot) {
        return false;
    }

    /**
     * Rendert die aktuelle Seite der gefilterten Liste in den Content-Bereich.
     *
     * @param filter   Filter für die anzuzeigenden Elemente.
     * @param renderer Wird für jedes sichtbare Element mit (Element, Slot) aufgerufen.
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
     * Füllt alle Slots außerhalb des Content-Bereichs mit einem Platzhalter-Item (leerer Name).
     *
     * @param material Füller-Material.
     */
    protected void fillBackground(@NotNull Material material) {
        ItemStack item = ItemBuilder.placeholder(material).build();
        for (int i = 0; i < size; i++) {
            if (!contentSlots.contains(i)) inventory.setItem(i, item);
        }
    }

    /**
     * Füllt den gesamten Content-Bereich mit einem Platzhalter-Item (leerer Name).
     *
     * @param material Füller-Material.
     */
    protected void fillContentArea(@NotNull Material material) {
        ItemStack item = ItemBuilder.placeholder(material).build();
        for (int slot : contentSlots) inventory.setItem(slot, item);
    }

    /**
     * Fügt Vor/Zurück-Pagination-Buttons hinzu (Pfeile; inaktiv wenn keine Seite vorhanden).
     *
     * @param prev Slot für „zurück“-Pfeil.
     * @param next Slot für „weiter“-Pfeil.
     * @param p    Spieler (für Update nach Klick).
     * @param f    Filter für die gefilterte Liste (muss zu {@link #allItems} passen).
     */
    protected void addPaginationButtons(int prev, int next, Player p, Predicate<T> f) {
        List<T> filtered = allItems.stream().filter(f).toList();
        if (page > 0) setButton(prev, new ItemBuilder(Material.ARROW).setName(RedJuluLib.getLang().get("gui.prev_page")).build(), (pl, c) -> { page--; update(pl); });
        else inventory.setItem(prev, new ItemBuilder(Material.ARROW).setName(RedJuluLib.getLang().get("gui.no_prev")).build());

        if ((page + 1) * pageSize < filtered.size()) setButton(next, new ItemBuilder(Material.ARROW).setName(RedJuluLib.getLang().get("gui.next_page")).build(), (pl, c) -> { page++; update(pl); });
        else inventory.setItem(next, new ItemBuilder(Material.ARROW).setName(RedJuluLib.getLang().get("gui.no_next")).build());
    }

    /**
     * Registriert Animations-Frames für einen Slot (zyklische Anzeige via {@link #tickAnimations}).
     *
     * @param slot   Slot-Index.
     * @param frames Liste der anzuzeigenden ItemStacks (Frames).
     */
    protected void setAnimatedItem(int slot, List<ItemStack> frames) {
        animatedSlots.put(slot, frames);
    }

    /**
     * Hier wird das konkrete GUI-Layout aufgebaut (Buttons, Content, Pagination, etc.).
     *
     * @param player Spieler, für den die GUI aufgebaut wird.
     */
    public abstract void compose(Player player);

    @Override public @NotNull Inventory getInventory() { return inventory; }
}