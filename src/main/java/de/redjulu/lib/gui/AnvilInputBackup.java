package de.redjulu.lib.gui;

import de.redjulu.RedJuluLib;
import de.redjulu.lib.ItemBuilder;
import de.redjulu.lib.lang.LanguageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Hilfsklasse zum Abfragen von Text-Eingaben über ein Anvil-GUI.
 * Nutzt Paper Menu Type API für echtes AnvilView (getRenameText, AnvilInventory).
 */
public class AnvilInputBackup implements Listener {

    private static final String DEFAULT_PREFIX_MINI = "<gray>► </gray>";

    private static LanguageService lang;

    private static final Map<Player, AnvilInputBackup> activeByPlayer = new ConcurrentHashMap<>();

    /** MiniMessage-String für die Anzeige des Prefix. Lazy, damit keine NPE wenn lang beim Klassenladen noch null ist. */
    private static String getPrefixMini() {
        return (lang != null && lang.has("system.anvil.prefix")) ? lang.getRaw("system.anvil.prefix") : DEFAULT_PREFIX_MINI;
    }

    /** Plain-Text des Prefix für Stripping (Anvil liefert nur Plain-Text). */
    private static String getPrefixPlain() {
        return PlainTextComponentSerializer.plainText()
                .serialize(MiniMessage.miniMessage().deserialize(getPrefixMini()));
    }

    /** So serialisiert MiniMessage den Prefix – zum Strippen bei aus Component gelesenem Text. */
    private static String getPrefixMiniSerialized() {
        return MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(getPrefixMini()));
    }

    /** Entfernt den Prefix vom Anfang (alle Varianten aus lang.getRaw), damit das gespeicherte Item ihn nicht hat. */
    private static String stripPrefix(String s) {
        if (s == null || s.isBlank()) return s;
        String t = s.trim();
        for (String prefix : new String[]{ getPrefixMiniSerialized(), getPrefixMini(), getPrefixPlain() }) {
            if (prefix != null && !prefix.isEmpty() && t.startsWith(prefix)) {
                t = t.substring(prefix.length()).trim();
                break;
            }
        }
        String prefixPlain = getPrefixPlain();
        if (prefixPlain != null && !prefixPlain.isEmpty()) {
            while (t.startsWith(prefixPlain)) {
                t = t.substring(prefixPlain.length()).trim();
            }
            t = t.replaceFirst(Pattern.quote(prefixPlain), "");
        }
        return t.trim();
    }

    /** Setzt die Standardfarbe auf der Wurzel-Komponente, falls noch keine Farbe gesetzt (z. B. für Item-Anzeige). */
    private static Component ensureDefaultColor(Component component, NamedTextColor defaultColor) {
        if (component.style().color() == null) {
            return component.style(component.style().color(defaultColor));
        }
        return component;
    }

    private static Component getTitle() {
        return lang != null && lang.has("system.anvil.title") ? lang.get("system.anvil.title") : Component.text("Eingabe...");
    }

    public static void setLanguageService(@Nullable LanguageService languageService) {
        lang = languageService;
    }

    private final Player player;
    private final CompletableFuture<String> future;
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private String lastPreparedText = null;

    private AnvilInputBackup(Player player) {
        this.player = player;
        this.future = new CompletableFuture<>();
        this.lastPreparedText = getPrefixPlain();
        Bukkit.getPluginManager().registerEvents(this, RedJuluLib.getPlugin());
    }

    public static CompletableFuture<String> getInput(@NotNull Player player) {
        AnvilInputBackup input = new AnvilInputBackup(player);
        activeByPlayer.put(player, input);
        AnvilView view = MenuType.ANVIL.create(player, getTitle());
        view.getTopInventory().setFirstItem(new ItemBuilder(Material.NAME_TAG).setName(getPrefixMini()).build());
        player.openInventory(view);
        return input.future;
    }

    @EventHandler
    public void onPrepare(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player p) || activeByPlayer.get(p) != this) return;

        Inventory top = event.getView().getTopInventory();
        ItemStack base = top.getItem(0);
        if (base == null || base.getType() == Material.AIR) return;

        String rename = null;
        if (event.getView() instanceof AnvilView view) {
            rename = view.getRenameText();
        }
        String finalText = (rename == null || rename.isBlank()) ? getPrefixPlain() : rename;
        this.lastPreparedText = finalText;

        ItemStack result = base.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;

        String prefixPlain = getPrefixPlain();
        Component displayName;
        if (finalText.startsWith(prefixPlain)) {
            String userPart = finalText.substring(prefixPlain.length());
            Component userComponent = userPart.isBlank() ? Component.empty() : ensureDefaultColor(MiniMessage.miniMessage().deserialize(userPart), NamedTextColor.WHITE);
            displayName = MiniMessage.miniMessage().deserialize(getPrefixMini()).append(userComponent);
        } else {
            Component userComponent = finalText.isBlank() ? Component.empty() : ensureDefaultColor(MiniMessage.miniMessage().deserialize(finalText), NamedTextColor.WHITE);
            displayName = userComponent;
        }
        meta.displayName(displayName.decoration(TextDecoration.ITALIC, false));
        result.setItemMeta(meta);
        event.setResult(result);

        Bukkit.getScheduler().runTask(RedJuluLib.getPlugin(), () -> {
            if (player.getOpenInventory() instanceof AnvilView view) {
                view.setRepairCost(0);
                view.setRepairItemCountCost(0);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (activeByPlayer.get(p) != this) return;

        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            event.setCancelled(true);
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        if (event.getRawSlot() != 2) return;

        event.getView().setCursor(null);

        Inventory top = event.getView().getTopInventory();
        String text = null;
        boolean fromComponent = false;
        Component displayNameComponent = null;
        if (top instanceof AnvilInventory anvil) {
            ItemStack result = anvil.getResult();
            if (result != null && result.hasItemMeta() && result.getItemMeta().displayName() != null) {
                displayNameComponent = result.getItemMeta().displayName();
                fromComponent = true;
            }
        }
        if (displayNameComponent == null && top != null) {
            ItemStack resultSlot = top.getItem(2);
            if (resultSlot != null && resultSlot.hasItemMeta() && resultSlot.getItemMeta().displayName() != null) {
                displayNameComponent = resultSlot.getItemMeta().displayName();
                fromComponent = true;
            }
        }
        if (displayNameComponent == null && top instanceof AnvilInventory anvil) {
            ItemStack first = anvil.getFirstItem();
            if (first != null && first.hasItemMeta() && first.getItemMeta().displayName() != null) {
                displayNameComponent = first.getItemMeta().displayName();
                fromComponent = true;
            }
        }
        if (fromComponent && displayNameComponent != null) {
            text = MiniMessage.miniMessage().serialize(displayNameComponent);
        }
        if ((text == null || text.isBlank()) && event.getView() instanceof AnvilView view) {
            text = view.getRenameText();
        }
        if (text == null || text.isBlank()) {
            text = lastPreparedText;
        }
        String finalText = stripPrefix(text == null ? "" : text.trim());

        if (top instanceof AnvilInventory anvil) anvil.setResult(null);
        if (top != null) top.setItem(2, null);

        finish(finalText);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getView() instanceof AnvilView)) return;
        if (!(event.getView().getPlayer() instanceof Player p) || activeByPlayer.get(p) != this) return;

        Bukkit.getScheduler().runTaskLater(RedJuluLib.getPlugin(), () -> {
            if (!completed.get()) {
                finish(null);
            }
        }, 2L);
    }

    private void finish(String result) {
        if (!completed.compareAndSet(false, true)) return;
        activeByPlayer.remove(player, this);
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().runTask(RedJuluLib.getPlugin(), () -> {
            if (player.getOpenInventory() instanceof AnvilView view && view.getTopInventory() instanceof AnvilInventory anvil) {
                anvil.setFirstItem(null);
                anvil.setSecondItem(null);
                anvil.setResult(null);
                player.closeInventory();
            }
        });
        if (result == null) {
            future.complete(null);
        } else {
            future.complete(result);
        }
    }

}
