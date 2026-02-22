package de.redjulu.lib.bossbar;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ManagedBossbar {

    private final BossbarManager manager;
    private final BossBar bar;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private BukkitTask timerTask;
    private String id;

    protected ManagedBossbar(BossbarManager manager, Component title, float progress, BossBar.Color color, BossBar.Overlay overlay) {
        this.manager = manager;
        this.bar = BossBar.bossBar(title, progress, color, overlay);
    }

    public ManagedBossbar setTitle(@NotNull Component title) {
        bar.name(title);
        return this;
    }

    public ManagedBossbar setProgress(float progress) {
        bar.progress(Math.max(0.0f, Math.min(1.0f, progress)));
        return this;
    }

    public ManagedBossbar setValue(double current, double max) {
        return setProgress((float) (current / max));
    }

    public ManagedBossbar setColor(@NotNull BossBar.Color color) {
        bar.color(color);
        return this;
    }

    public ManagedBossbar setOverlay(@NotNull BossBar.Overlay overlay) {
        bar.overlay(overlay);
        return this;
    }

    public ManagedBossbar addPlayer(@NotNull Player player) {
        viewers.add(player.getUniqueId());
        player.showBossBar(bar);
        manager.register(player, this);
        return this;
    }

    public ManagedBossbar removePlayer(@NotNull Player player) {
        viewers.remove(player.getUniqueId());
        player.hideBossBar(bar);
        manager.unregister(player, this);
        return this;
    }

    public ManagedBossbar showAll() {
        Bukkit.getOnlinePlayers().forEach(this::addPlayer);
        return this;
    }

    public ManagedBossbar startCountdown(int seconds, Consumer<ManagedBossbar> onFinish) {
        stopTimer();
        final long start = System.currentTimeMillis();
        final long duration = seconds * 1000L;

        timerTask = Bukkit.getScheduler().runTaskTimer(manager.getPlugin(), () -> {
            long elapsed = System.currentTimeMillis() - start;
            float progress = 1.0f - ((float) elapsed / duration);

            if (progress <= 0) {
                setProgress(0.0f);
                stopTimer();
                if (onFinish != null) onFinish.accept(this);
            } else {
                setProgress(progress);
            }
        }, 0L, 1L);
        return this;
    }

    public void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    public void destroy() {
        stopTimer();
        if (id != null) {
            manager.removeInternal(id);
        }
        for (UUID uuid : viewers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.hideBossBar(bar);
                manager.unregister(p, this);
            }
        }
        viewers.clear();
    }

    protected void setId(String id) {
        this.id = id;
    }

    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    public BossBar getAdventureBar() {
        return bar;
    }
}