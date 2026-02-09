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
     * Empty strings or "none" in the config will result in no message being sent.
     * Supports String and Component placeholders.
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
     * Retrieves a translated list of components (e.g., for Item Lore).
     * @param key The translation key.
     * @param placeholders Key-Value pairs for placeholders.
     * @return A list of translated components.
     */
    public static List<Component> getLore(@NotNull String key, Object... placeholders) {
        return lang.getList(key, placeholders);
    }

    /**
     * Sends a raw MiniMessage string directly without using the language file.
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
     * Uses keys 'system.status_on', 'system.status_off' and 'system.toggle_format'.
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
     * Sends a message to the console with a specific prefix.
     * @param message The message to send.
     */
    public static void console(@NotNull String message) {
        Bukkit.getConsoleSender().sendMessage(lang.parse("<gray>[<red>Console</red>] <white>" + message));
    }

    /**
     * Sends a message to the console with a specific prefix.
     * @param message The message to send.
     */
    public static void console(@NotNull Component message) {
        Bukkit.getConsoleSender().sendMessage(lang.parse("<gray>[<red>Console</red>] <white>" + message));
    }

    /**
     * Sends a debug message to the Console
     * @param message
     */
    public static void debug(@NotNull String message) {
        if (RedJuluLib.isDebug()) Bukkit.getConsoleSender().sendMessage(lang.parse("<dark_gray>[<aqua>DEBUG</aqua>] <gray>" + message));
    }

    /**
     * Sends a debug message to the Console
     * @param message
     */
    public static void debug(@NotNull Component message) {
        if (RedJuluLib.isDebug()) Bukkit.getConsoleSender().sendMessage(lang.parse("<dark_gray>[<aqua>DEBUG</aqua>] <gray>" + message));
    }

    /**
     * Returns the raw String
     * @param key
     * @return String
     */

    public static String getRaw(@NotNull String key) {
        return lang.getRaw(key);
    }

    /**
     * Translates MiniMessage to Legacy
     * @param mmString
     * @return
     */
    public static String translateMMToLegacy(String mmString) {
        var component = MiniMessage.miniMessage().deserialize(mmString);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    /**
     * Sends a Text in the Actionbar
     * @param player
     * @param key
     * @param replacements
     */
    public static void sendActionbar(Player player, String key, String... replacements) {
        String message = getRaw(key);
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace("<" + replacements[i] + ">", replacements[i + 1]);
        }
        player.sendActionBar(MiniMessage.miniMessage().deserialize(message));
    }

    /**
     * Sends a raw Actionbar
     * @param player
     * @param message
     */
    public static void sendRawActionbar(Player player, String message) {
        player.sendActionBar(MiniMessage.miniMessage().deserialize(message));
    }

    /**
     * Plays a sound for a player.
     * @param player The player.
     * @param sound The sound.
     * @param pitch The pitch.
     */
    public static void playSound(@NotNull Player player, @NotNull Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 0.5f, pitch);
    }

    /**
     * Plays a success sound.
     * @param player The player.
     */
    public static void playSuccess(@NotNull Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.2f);
    }

    /**
     * Plays an error sound.
     * @param player The player.
     */
    public static void playError(@NotNull Player player) {
        playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f);
    }

    /**
     * Plays a generic click sound.
     * @param player The player.
     */
    public static void playClick(@NotNull Player player) {
        playSound(player, Sound.UI_BUTTON_CLICK, 1.0f);
    }
}