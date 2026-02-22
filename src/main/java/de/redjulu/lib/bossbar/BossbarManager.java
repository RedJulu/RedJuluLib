package de.redjulu.lib.bossbar;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossbarManager {

    private final Plugin plugin;
    private final Map<UUID, Set<ManagedBossbar>> activeBars = new ConcurrentHashMap<>();
    private final Map<String, ManagedBossbar> identifiedBars = new ConcurrentHashMap<>();

    public BossbarManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public ManagedBossbar create(@NotNull Component title, float progress, @NotNull BossBar.Color color, @NotNull BossBar.Overlay overlay) {
        return new ManagedBossbar(this, title, progress, color, overlay);
    }

    public ManagedBossbar create(@NotNull Component title) {
        return create(title, 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
    }

    public ManagedBossbar create(@NotNull String id, @NotNull Component title) {
        ManagedBossbar bar = create(title);
        identifiedBars.put(id, bar);
        return bar;
    }

    @Nullable
    public ManagedBossbar getById(@NotNull String id) {
        return identifiedBars.get(id);
    }

    protected void register(@NotNull Player player, @NotNull ManagedBossbar bar) {
        activeBars.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(bar);
    }

    protected void unregister(@NotNull Player player, @NotNull ManagedBossbar bar) {
        Set<ManagedBossbar> bars = activeBars.get(player.getUniqueId());
        if (bars != null) {
            bars.remove(bar);
            if (bars.isEmpty()) activeBars.remove(player.getUniqueId());
        }
    }

    protected void removeInternal(@NotNull String id) {
        identifiedBars.remove(id);
    }

    public void cleanup(@NotNull Player player) {
        Set<ManagedBossbar> bars = activeBars.remove(player.getUniqueId());
        if (bars != null) {
            bars.forEach(bar -> bar.removePlayer(player));
        }
    }

    public void removeAll() {
        identifiedBars.values().forEach(ManagedBossbar::destroy);
        identifiedBars.clear();
        activeBars.values().forEach(set -> set.forEach(ManagedBossbar::destroy));
        activeBars.clear();
    }

    public Plugin getPlugin() {
        return plugin;
    }
}