package de.redjulu.lib.gui;

import de.redjulu.RedJuluLib;
import de.redjulu.lib.ItemBuilder;
import de.redjulu.lib.MessageHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.persistence.PersistentDataType;
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
    protected final Map<Integer, ItemStack> activeItems = new HashMap<>();
    protected final Set<Integer> interactableSlots = new HashSet<>();
    protected final Set<Integer> ignoredSlots = new HashSet<>();
    protected final Set<Integer> placeholderSlots = new HashSet<>();
    protected final Set<Integer> prioritySlots = new HashSet<>();
    protected final Map<Integer, ItemStack> placeholderItems = new HashMap<>();
    private final List<DynamicButtonInfo> dynamicButtons = new ArrayList<>();

    protected Sound openSound = Sound.BLOCK_CHEST_OPEN;
    protected Sound clickSound = Sound.UI_BUTTON_CLICK;
    protected Sound backSound = Sound.ITEM_ARMOR_EQUIP_GENERIC;

    protected List<T> allItems = new ArrayList<>();
    protected C currentCategory;
    protected int page = 0;
    protected final int pageSize;

    private boolean switching = false;
    private boolean dialogOpen = false;

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
        switching = false;
        dialogOpen = false;
        if (saveToHistory) {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof BaseGUI<?, ?> current) {
                HISTORY.put(player.getUniqueId(), current);
            }
        }
        update(player);
        if (!player.getOpenInventory().getTopInventory().equals(inventory)) {
            player.openInventory(inventory);
            if (openSound != null) player.playSound(player.getLocation(), openSound, 0.5f, 1.0f);
        }
    }

    /**
     * Rebuilds the GUI content.
     * @param player Target player.
     */
    public void update(Player player) {
        if(dialogOpen) return;

        GUIListener.clearButtons(inventory);
        animatedSlots.clear();
        interactableSlots.clear();
        placeholderSlots.clear();
        prioritySlots.clear();
        placeholderItems.clear();
        ignoredSlots.clear();
        dynamicButtons.clear();
        compose(player);
        tickAnimations(0);
    }

    /**
     * Re-applies dynamic button display (e.g. after placeholder slot content changed). Call from listener.
     */
    public void updateDynamicButtons(Player player) {
        for (DynamicButtonInfo info : dynamicButtons) {
            inventory.setItem(info.slot, info.condition.test(player) ? info.activeItem : info.inactiveItem);
        }
    }

    private record DynamicButtonInfo(int slot, Predicate<Player> condition, ItemStack activeItem, ItemStack inactiveItem) {}

    /**
     * Marks a slot as interactable (e.g. for item input); clicks are not cancelled for these slots.
     */
    protected void setInteractable(int slot, boolean interactable) {
        if (interactable) interactableSlots.add(slot);
        else interactableSlots.remove(slot);
    }

    /**
     * Marks a slot to be skipped when using fillBackground with skipIgnoredSlots.
     */
    protected void setIgnored(int slot) {
        ignoredSlots.add(slot);
    }

    /**
     * Marks a slot as placeholder and sets the display item. Placeholder can't be taken;
     * placing an item replaces it, taking the item restores the placeholder.
     */
    protected void setPlaceholder(int slot) {
        ItemStack current = inventory.getItem(slot);

        if (current == null || current.getType() == Material.AIR) return;

        ItemStack marked = markAsPlaceholderItem(current);
        inventory.setItem(slot, marked);

        placeholderSlots.add(slot);
        placeholderItems.put(slot, marked.clone());
    }


    /**
     * Registers a slot as placeholder (so taking the item out restores the placeholder) without setting the display.
     * Use when the slot sometimes shows a user item; call this first, then set the slot to item or to getPlaceholderForSlot(slot).
     */
    protected void registerPlaceholderSlot(int slot, @NotNull ItemStack placeholderItem) {
        ItemStack marked = markAsPlaceholderItem(placeholderItem);
        placeholderSlots.add(slot);
        placeholderItems.put(slot, marked.clone());
    }

    private ItemStack markAsPlaceholderItem(ItemStack placeholderItem) {
        ItemStack marked = placeholderItem.clone();
        var meta = marked.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(placeholderKey(), PersistentDataType.BYTE, (byte) 1);
            marked.setItemMeta(meta);
        }
        return marked;
    }

    /**
     * Gives this placeholder slot priority when shift-clicking (e.g. item goes here first).
     */
    protected void setPriority(int slot) {
        prioritySlots.add(slot);
    }

    private static NamespacedKey placeholderKey() {
        return new NamespacedKey(RedJuluLib.getPlugin(), "gui_placeholder");
    }

    /**
     * Returns true if the item is a GUI placeholder (background).
     */
    public static boolean isPlaceholderItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(placeholderKey(), PersistentDataType.BYTE);
    }

    /**
     * Returns the stored placeholder item for a slot (to restore when user takes their item out).
     */
    public ItemStack getPlaceholderForSlot(int slot) {
        return placeholderItems.get(slot);
    }

    /**
     * Returns whether the slot has shift priority.
     */
    public boolean isPrioritySlot(int slot) {
        return prioritySlots.contains(slot);
    }

    /**
     * Set true before opening a menu (e.g. AnvilInput) so onClose does not return items to the player.
     */
    protected void setSwitching(boolean switching) {
        this.switching = switching;
    }

    /**
     * Whether the GUI is currently switching to another menu (items will not be returned on close).
     */
    public boolean isSwitching() {
        return switching;
    }

    /**
     * Setzt, ob gerade ein Paper-Dialog offen ist, um Schließen-Logik zu unterdrücken.
     */
    public void setDialogOpen(boolean open) {
        this.dialogOpen = open;
    }

    public boolean isDialogOpen() {
        return dialogOpen;
    }

    /**
     * Sets an item in a slot without registering a click action.
     */
    protected void setButton(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    /**
     * Clears the stored active item for a slot (e.g. when user takes item out of placeholder slot so placeholder shows again).
     */
    protected void clearActiveItem(int slot) {
        activeItems.remove(slot);
    }

    /**
     * Returns the active/custom item for a slot. For placeholder slots: inventory first, then activeItems (e.g. after anvil).
     */
    protected ItemStack getActiveItem(int slot) {
        if (placeholderSlots.contains(slot)) {
            ItemStack in = inventory.getItem(slot);
            if (in != null && in.getType() != Material.AIR && !isPlaceholderItem(in)) return in;
            return activeItems.get(slot);
        }
        if (interactableSlots.contains(slot)) {
            ItemStack in = inventory.getItem(slot);
            if (in != null && in.getType() != Material.AIR) return in;
        }
        return activeItems.get(slot);
    }

    /**
     * Registers a button that shows different items based on a condition and runs the same action.
     * @param player The player for whom the GUI is composed (used to evaluate the condition).
     */
    protected void setDynamicButton(Player player, int slot, Predicate<Player> condition, ItemStack activeItem, ItemStack inactiveItem, BiConsumer<Player, ClickType> action) {
        dynamicButtons.add(new DynamicButtonInfo(slot, condition, activeItem, inactiveItem));
        inventory.setItem(slot, condition.test(player) ? activeItem : inactiveItem);
        GUIListener.registerButton(inventory, slot, (p, click) -> {
            if (!condition.test(p)) {
                MessageHelper.playError(p);
                return;
            }
            if (clickSound != null) p.playSound(p.getLocation(), clickSound, 0.5f, 1.0f);
            action.accept(p, click);
        });
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
     * Defines if a slot is interactable (e.g. for item input).
     * @param slot Target slot.
     * @return true if the slot was marked with setInteractable(slot, true).
     */
    public boolean isInteractableSlot(int slot) {
        return interactableSlots.contains(slot);
    }

    /**
     * Returns whether the slot is a placeholder (display-only, no take/action).
     */
    public boolean isPlaceholderSlot(int slot) {
        return placeholderSlots.contains(slot);
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
        fillBackground(material, false);
    }

    /**
     * Fills slots outside the content area, optionally skipping ignored slots.
     * @param material Filler material.
     * @param skipIgnoredSlots If true, slots added via setIgnored() are not filled.
     */
    protected void fillBackground(@NotNull Material material, boolean skipIgnoredSlots) {
        ItemStack item = new ItemBuilder(material).setName(Component.empty()).build();
        for (int i = 0; i < size; i++) {
            if (contentSlots.contains(i)) continue;
            if (skipIgnoredSlots && ignoredSlots.contains(i)) continue;
            inventory.setItem(i, item);
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