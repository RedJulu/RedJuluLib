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
 * The core foundation for all Custom GUIs.
 * Supports pagination, animation, history (back-button), and categories.
 *
 * @param <T> The data type of the objects to be displayed.
 * @param <C> The Enum type for categorization/filtering.
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
     * Clears the GUI history for a player.
     * @param uuid Player's UUID.
     */
    public static void clearHistory(UUID uuid) {
        HISTORY.remove(uuid);
    }

    /**
     * Opens the GUI for a player.
     * @param player Target player.
     * @param saveToHistory Whether to store previous GUI.
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
     * Rebuilds the GUI content.
     * @param player Target player.
     */
    public void update(Player player) {
        GUIListener.clearButtons(inventory);
        inventory.clear();
        animatedSlots.clear();
        compose(player);
        tickAnimations(0);
    }

    /**
     * Switches the current category, resets page and updates.
     * @param player Target player.
     * @param category The new category.
     */
    public void setCategory(Player player, C category) {
        this.currentCategory = category;
        this.page = 0;
        update(player);
    }

    /**
     * Returns to the previous GUI.
     * @param player Target player.
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
     * Registers a button in the GUI.
     * @param slot Target slot.
     * @param item ItemStack.
     * @param action Click logic.
     */
    protected void setButton(int slot, ItemStack item, BiConsumer<Player, ClickType> action) {
        inventory.setItem(slot, item);
        GUIListener.registerButton(inventory, slot, (p, click) -> {
            if (clickSound != null) p.playSound(p.getLocation(), clickSound, 0.5f, 1.0f);
            action.accept(p, click);
        });
    }

    /**
     * Ticks animations.
     * @param tick Current animation tick.
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
     * Defines if a slot is interactable.
     * @param slot Target slot.
     * @return true if unlocked.
     */
    public boolean isInteractableSlot(int slot) {
        return false;
    }

    /**
     * Renders filtered content into the content area.
     * @param filter Filter logic.
     * @param renderer Logic for each item.
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
     * Fills slots outside the content area.
     * @param material Filler material.
     */
    protected void fillBackground(@NotNull Material material) {
        ItemStack item = new ItemBuilder(material).setName(Component.empty()).build();
        for (int i = 0; i < size; i++) {
            if (!contentSlots.contains(i)) inventory.setItem(i, item);
        }
    }

    /**
     * Fills the content area with a material.
     * @param material Content material.
     */
    protected void fillContentArea(@NotNull Material material) {
        ItemStack item = new ItemBuilder(material).setName(Component.empty()).build();
        for (int slot : contentSlots) inventory.setItem(slot, item);
    }

    /**
     * Adds pagination controls.
     * @param prev Slot for back arrow.
     * @param next Slot for forward arrow.
     * @param p Target player.
     * @param f Page filter.
     */
    protected void addPaginationButtons(int prev, int next, Player p, Predicate<T> f) {
        List<T> filtered = allItems.stream().filter(f).toList();
        if (page > 0) setButton(prev, new ItemBuilder(Material.ARROW).setName(RedJuluLib.getLang().get("gui.prev_page")).build(), (pl, c) -> { page--; update(pl); });
        else inventory.setItem(prev, new ItemBuilder(Material.ARROW).setName(RedJuluLib.getLang().get("gui.no_prev")).build());

        if ((page + 1) * pageSize < filtered.size()) setButton(next, new ItemBuilder(Material.ARROW).setName(RedJuluLib.getLang().get("gui.next_page")).build(), (pl, c) -> { page++; update(pl); });
        else inventory.setItem(next, new ItemBuilder(Material.ARROW).setName(RedJuluLib.getLang().get("gui.no_next")).build());
    }

    /**
     * Registers animation frames for a slot.
     * @param slot Target slot.
     * @param frames Frame list.
     */
    protected void setAnimatedItem(int slot, List<ItemStack> frames) {
        animatedSlots.put(slot, frames);
    }

    /**
     * GUI layout construction logic.
     * @param player Target player.
     */
    public abstract void compose(Player player);

    @Override public @NotNull Inventory getInventory() { return inventory; }
}