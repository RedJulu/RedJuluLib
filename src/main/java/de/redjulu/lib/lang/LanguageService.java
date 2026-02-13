package de.redjulu.lib.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling multi-language support and MiniMessage rendering.
 */
public class LanguageService {

    private final JavaPlugin plugin;
    private final Map<String, Object> cache = new HashMap<>();
    public final MiniMessage mm = MiniMessage.miniMessage();
    private final String currentLang;

    /**
     * @param plugin The providing JavaPlugin.
     * @param langCode The language code (e.g., "de", "en").
     */
    public LanguageService(JavaPlugin plugin, String langCode) {
        this.plugin = plugin;
        this.currentLang = langCode;
        reload();
    }

    /**
     * Reloads all keys from the language file into the cache.
     */
    public void reload() {
        File langFile = new File(plugin.getDataFolder(), "languages/" + currentLang + ".yml");

        if (!langFile.exists()) {
            plugin.saveResource("languages/de.yml", false);
            plugin.saveResource("languages/en.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
        cache.clear();
        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key)) {
                cache.put(key, config.get(key));
            }
        }
    }

    /**
     * Checks if a specific key exists in the language cache.
     *
     * @param key The key to check.
     * @return True if the key is registered.
     */
    public boolean has(String key) {
        return cache.containsKey(key);
    }

    /**
     * Retrieves a translated component.
     *
     * @param key The language key.
     * @param placeholders Key-Value pairs for placeholders.
     * @return The rendered Component.
     */
    public Component get(String key, Object... placeholders) {
        Object raw = cache.get(key);

        if (raw == null) return mm.deserialize("<red>Missing Key: " + key);
        String template = String.valueOf(raw);
        if (template.isEmpty() || template.equalsIgnoreCase("none")) return Component.empty();

        return render(template, placeholders);
    }

    /**
     * Holt einen Text, entfernt alle vorhandenen Tags und forced ihn auf Rot + Durchgestrichen.
     * @param key The language key.
     * @return The rendered Component.
     */
    public Component getLocked(String key) {
        String raw = getRaw(key);
        if (raw.startsWith("<red>Missing Key")) return mm.deserialize(raw);

        String stripped = raw.replaceAll("<[^>]*>", "");
        return mm.deserialize("<red><strikethrough>" + stripped);
    }

    /**
     * Holt eine Liste (Lore), entfernt alle Tags und forced jede Zeile auf Rot + Durchgestrichen.
     * @param key Der Language Key.
     * @return Eine Liste von Components für den ItemBuilder.
     */
    public List<Component> getLockedList(String key) {
        Object raw = cache.get(key);
        if (!(raw instanceof List<?> rawList)) {
            return Collections.singletonList(getLocked(key));
        }

        List<Component> translated = new ArrayList<>();
        for (Object line : rawList) {
            String stripped = String.valueOf(line).replaceAll("<[^>]*>", "");
            translated.add(mm.deserialize("<red><strikethrough>" + stripped));
        }
        return translated;
    }

    /**
     * Retrieves a list of translated components.
     *
     * @param key The language key.
     * @param placeholders Key-Value pairs for placeholders.
     * @return A list of rendered Components.
     */
    public List<Component> getList(String key, Object... placeholders) {
        Object raw = cache.get(key);
        if (!(raw instanceof List<?> rawList)) return Collections.emptyList();

        List<Component> translated = new ArrayList<>();
        for (Object line : rawList) {
            translated.add(render(String.valueOf(line), placeholders));
        }
        return translated;
    }

    /**
     * Renders a string template with placeholders using MiniMessage.
     *
     * @param template The MiniMessage string.
     * @param placeholders Key-Value pairs.
     * @return The final Component.
     */
    private Component render(String template, Object... placeholders) {
        TagResolver.Builder builder = TagResolver.builder();
        builder.resolver(Placeholder.parsed("prefix", String.valueOf(cache.getOrDefault("prefix", "<gray>» "))));

        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String tag = String.valueOf(placeholders[i]);
                Object value = placeholders[i + 1];

                if (value instanceof Component c) {
                    builder.resolver(Placeholder.component(tag, c));
                } else {
                    builder.resolver(Placeholder.parsed(tag, String.valueOf(value)));
                }
            }
        }
        return mm.deserialize(template, builder.build());
    }

    /**
     * Gets the raw string value of a key.
     *
     * @param key The language key.
     * @return The raw string or the key itself if not found.
     */
    public String getRaw(String key) {
        Object val = cache.get(key);
        if (val == null) return "<red>Missing Key: " + key;
        return String.valueOf(val);
    }

    /**
     * Parses a raw MiniMessage string without cache lookup.
     *
     * @param miniMessageText The text to parse.
     * @return The parsed Component.
     */
    public Component parse(@NotNull String miniMessageText) {
        return mm.deserialize(miniMessageText);
    }
}