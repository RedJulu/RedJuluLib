package de.redjulu.lib.gui;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.Map;

public interface LiveDialogProvider {
    @NotNull DialogBuilder createUpdate(@NotNull LiveDialogSession session, @NotNull Player viewer, Map<String, Object> initialData);
}