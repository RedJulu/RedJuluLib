package de.redjulu;

import de.redjulu.lib.MessageHelper;
import de.redjulu.lib.bossbar.BossbarManager;
import de.redjulu.lib.filestore.FilestoreConfigLoader;
import de.redjulu.lib.filestore.FilestoreManager;
import de.redjulu.lib.gui.GUIAnimationTask;
import de.redjulu.lib.gui.GUIListener;
import de.redjulu.lib.item.BoundItem;
import de.redjulu.lib.lang.LanguageService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Main entry point for the RedJuluLib.
 * Handles initialization of services and event listeners.
 */
public class RedJuluLib {

    private static JavaPlugin plugin;
    private static LanguageService lang;
    private static BossbarManager bossbarManager;
    private static FilestoreManager filestoreManager;
    private static boolean debug = false;
    private static boolean initialized = false;

    /**
     * Initializes the library services and registers necessary listeners.
     *
     * @param pluginInstance   The JavaPlugin instance using the library.
     * @param selectedLanguage The language code to be used for translations.
     */
    public static void init(@NotNull JavaPlugin pluginInstance, @NotNull String selectedLanguage) {
        if (initialized) return;

        plugin = pluginInstance;
        lang = new LanguageService(plugin, selectedLanguage);
        bossbarManager = new BossbarManager(plugin);

        Bukkit.getPluginManager().registerEvents(new GUIListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new BoundItem.BoundListener(), plugin);

        new GUIAnimationTask(plugin).start(10L);
        MessageHelper.init(lang);

        filestoreManager = FilestoreConfigLoader.createManager(plugin);

        initialized = true;
    }

    /**
     * Returns the registered JavaPlugin instance.
     *
     * @return The JavaPlugin instance.
     */
    public static JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Returns the global LanguageService.
     *
     * @return The LanguageService instance.
     */
    public static LanguageService getLang() {
        return lang;
    }

    /**
     * Returns the registered BossbarManager instance.
     *
     * @return The BossbarManager instance.
     */
    public static BossbarManager getBossbarManager() {
        return bossbarManager;
    }

    /**
     * Returns the FilestoreManager (initialized in init).
     */
    @NotNull
    public static FilestoreManager getFilestoreManager() {
        if (filestoreManager == null) {
            throw new IllegalStateException("RedJuluLib.init() must be called first.");
        }
        return filestoreManager;
    }

    /**
     * Checks if debug mode is enabled.
     *
     * @return True if debug is active.
     */
    public static boolean isDebug() {
        return debug;
    }

    /**
     * Toggles the debug mode.
     *
     * @param enabled Set to true to enable debug messages.
     */
    public static void setDebug(boolean enabled) {
        debug = enabled;
    }

    /**
     * Call from your plugin's onDisable().
     */
    public static void shutdown() {
        if (filestoreManager != null) {
            filestoreManager.shutdown();
            filestoreManager = null;
        }
    }
}