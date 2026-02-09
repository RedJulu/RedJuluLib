package de.redjulu.lib.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Task responsible for updating animated slots in all active BaseGUI instances.
 */
public class GUIAnimationTask {

    private final JavaPlugin plugin;
    private long tick = 0;

    /**
     * @param plugin The JavaPlugin instance to register the scheduler.
     */
    public GUIAnimationTask(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the global animation timer.
     *
     * @param interval Ticks between animation frames.
     */
    public void start(long interval) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tick++;
            for (Player player : Bukkit.getOnlinePlayers()) {
                InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
                if (holder instanceof BaseGUI<?, ?> gui) {
                    gui.tickAnimations(tick);
                }
            }
        }, 0L, interval);
    }
}