package de.redjulu.lib.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class LiveDialogSession {

    private final Map<UUID, Map<String, Object>> currentInputs = new ConcurrentHashMap<>();
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private final LiveDialogProvider provider;

    /**
     * Creates a new LiveDialogSession with the specified provider.
     * @param provider The provider for dialog updates, cannot be null
     * @throws IllegalArgumentException if provider is null
     */
    public LiveDialogSession(@NotNull LiveDialogProvider provider) {
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
    }

    public void join(@NotNull Player player) {
        viewers.add(player.getUniqueId());
        currentInputs.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        refresh(player);
    }

    public void leave(@NotNull Player player) {
        viewers.remove(player.getUniqueId());
    }

    public void capture(@NotNull Player player, @NotNull String key, @Nullable Object value) {
        Map<String, Object> map = currentInputs.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        map.put(key, value == null ? "" : value);
    }

    public void clearInputs(@NotNull Player player) {
        currentInputs.remove(player.getUniqueId());
    }

    public Map<String, Object> getInputs(@NotNull Player player) {
        return currentInputs.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
    }

    public void refresh(@NotNull Player player) {
        if (!player.isOnline()) return;
        Map<String, Object> inputs = getInputs(player);
        provider.createUpdate(this, player, inputs).show(player);
    }

    public void refreshAll() {
        for (UUID uuid : viewers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) refresh(p);
        }
    }

    public void refreshAllExcept(Player exclude) {
        for (UUID uuid : viewers) {
            if (uuid.equals(exclude.getUniqueId())) continue;
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) refresh(p);
        }
    }

    public void updateAll(BiConsumer<Player, Boolean> action) {
        for (UUID uuid : viewers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;
            action.accept(p, true);
        }
    }

    public Set<Player> getParticipants() {
        Set<Player> players = ConcurrentHashMap.newKeySet();
        for (UUID uuid : viewers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) players.add(p);
        }
        return players;
    }

    public LiveDialogProvider getProvider() {
        return provider;
    }

    /**
     * Removes a player from the session and clears their inputs.
     * @param player The player to remove, cannot be null
     * @throws IllegalArgumentException if player is null
     */
    public void quit(@NotNull Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        UUID playerId = player.getUniqueId();
        viewers.remove(playerId);
        currentInputs.remove(playerId);
    }
}