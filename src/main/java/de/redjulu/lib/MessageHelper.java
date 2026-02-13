package de.redjulu.lib;

import de.redjulu.RedJuluLib;
import de.redjulu.lib.lang.LanguageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Universal helper for communication.
 * Handles translations, MiniMessage, Sounds and Actionbars.
 */
public class MessageHelper {

    private static LanguageService lang;

    /**
     * Initializes the helper with the language service.
     * @param languageService The service used for translations.
     */
    public static void init(LanguageService languageService) {
        lang = languageService;
    }

    /**
     * Sends a translated message to a sender.
     * @param sender The recipient of the message.
     * @param key The translation key.
     * @param placeholders Key-Value pairs for placeholders.
     */
    public static void send(@NotNull CommandSender sender, @NotNull String key, Object... placeholders) {
        Component msg = lang.get(key, placeholders);
        if (!msg.equals(Component.empty())) {
            sender.sendMessage(msg);
        }
    }

    /**
     * Retrieves a translated component.
     * @param key The translation key.
     * @param placeholders Key-Value pairs for placeholders.
     * @return The translated component or Component.empty().
     */
    public static Component get(@NotNull String key, Object... placeholders) {
        return lang.get(key, placeholders);
    }

    /**
     * Retrieves a translated component in "Locked" style (Red + Strikethrough).
     * @param key The translation key.
     * @return The formatted component.
     */
    public static Component getLocked(@NotNull String key) {
        return lang.getLocked(key);
    }

    /**
     * Retrieves a translated list of components (e.g., for Item Lore).
     * @param key The translation key.
     * @param placeholders Key-Value pairs for placeholders.
     * @return A list of translated components.
     */
    public static List<Component> getLore(@NotNull String key, Object... placeholders) {
        return lang.getList(key, placeholders);
    }

    /**
     * Retrieves a translated list of components in "Locked" style.
     * @param key The translation key.
     * @return A list of formatted components.
     */
    public static List<Component> getLockedLore(@NotNull String key) {
        return lang.getLockedList(key);
    }

    /**
     * Sends a raw MiniMessage string directly.
     * @param sender The recipient.
     * @param miniMessageText The raw text to parse.
     */
    public static void sendRaw(@NotNull CommandSender sender, @NotNull String miniMessageText) {
        sender.sendMessage(lang.parse(miniMessageText));
    }

    /**
     * Sends a translated message to a player's action bar.
     * @param player The player.
     * @param key The translation key.
     * @param placeholders Key-Value pairs for placeholders.
     */
    public static void actionBar(@NotNull Player player, @NotNull String key, Object... placeholders) {
        Component msg = lang.get(key, placeholders);
        if (!msg.equals(Component.empty())) {
            player.sendActionBar(msg);
        }
    }

    /**
     * Sends a raw MiniMessage string to a player's action bar.
     * @param player The player.
     * @param miniMessageText The raw text.
     */
    public static void rawActionBar(@NotNull Player player, @NotNull String miniMessageText) {
        player.sendActionBar(lang.parse(miniMessageText));
    }

    /**
     * Broadcasts a translated message to all online players.
     * @param key The translation key.
     * @param placeholders Key-Value pairs for placeholders.
     */
    public static void broadcast(@NotNull String key, Object... placeholders) {
        Component msg = lang.get(key, placeholders);
        if (!msg.equals(Component.empty())) {
            Bukkit.broadcast(msg);
        }
    }

    /**
     * Sends a formatted toggle message.
     * @param sender The sender.
     * @param labelKey The key for the label.
     * @param active The status.
     */
    public static void sendToggle(@NotNull CommandSender sender, @NotNull String labelKey, boolean active) {
        String statusKey = active ? "system.status_on" : "system.status_off";
        send(sender, "system.toggle_format",
                "label", lang.getRaw(labelKey),
                "status", lang.getRaw(statusKey)
        );
    }

    /**
     * Sends a message to the console with a prefix.
     * @param message The message (MiniMessage support).
     */
    public static void console(@NotNull String message) {
        Bukkit.getConsoleSender().sendMessage(lang.parse("<gray>[<red>Console</red>] <white>" + message));
    }

    /**
     * Sends a debug message to the console if debug mode is enabled.
     * @param message The debug message.
     */
    public static void debug(@NotNull String message) {
        if (RedJuluLib.isDebug()) {
            Bukkit.getConsoleSender().sendMessage(lang.parse("<dark_gray>[<aqua>DEBUG</aqua>] <gray>" + message));
        }
    }

    /**
     * Gets the raw string value of a key.
     * @param key The key.
     * @return The raw string or the key itself.
     */
    public static String getRaw(@NotNull String key) {
        String raw = lang.getRaw(key);
        return raw != null ? raw : key;
    }

    /**
     * Translates MiniMessage to Legacy Section (§).
     */
    public static String translateMMToLegacy(@NotNull String mmString) {
        return LegacyComponentSerializer.legacySection().serialize(lang.parse(mmString));
    }

    /**
     * Translates Legacy Section (§) to MiniMessage.
     */
    public static String translateLegacyToMM(@NotNull String legacyString) {
        var component = LegacyComponentSerializer.legacySection().deserialize(legacyString);
        return MiniMessage.miniMessage().serialize(component);
    }

    /**
     * Plays a sound for a player.
     */
    public static void playSound(@NotNull Player player, @NotNull Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 0.5f, pitch);
    }

    public static void playSuccess(@NotNull Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.2f);
    }

    public static void playError(@NotNull Player player) {
        playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f);
    }

    public static void playClick(@NotNull Player player) {
        playSound(player, Sound.UI_BUTTON_CLICK, 1.0f);
    }
}