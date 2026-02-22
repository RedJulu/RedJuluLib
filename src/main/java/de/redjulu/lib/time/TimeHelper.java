package de.redjulu.lib.time;

import de.redjulu.RedJuluLib;
import de.redjulu.lib.lang.LanguageService;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TimeHelper {

    /**
     * Converts a total number of seconds into a list of time Components.
     * Zero values are hidden by default.
     * Only specified units will be included if provided.
     *
     * @param totalSeconds total time in seconds to format
     * @param showSuffixes whether to append unit suffixes (e.g. "h","m","s")
     * @param onlyUnits optional list of units to include (e.g. ["year","month"]), null for all units
     * @return a list of Components representing the time
     */
    public static @NotNull List<Component> parseTime(long totalSeconds, boolean showSuffixes, @Nullable List<String> onlyUnits) {
        long seconds = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes = totalMinutes % 60;
        long totalHours = totalMinutes / 60;
        long hours = totalHours % 24;
        long totalDays = totalHours / 24;
        long days = totalDays % 30;
        long totalMonths = totalDays / 30;
        long months = totalMonths % 12;
        long years = totalMonths / 12;

        List<Component> components = new ArrayList<>();
        LanguageService lang = RedJuluLib.getLang();

        if (years > 0 && (onlyUnits == null || onlyUnits.contains("year")))
            components.add(Component.text(years + (showSuffixes ? lang.getOrDefault("system.time.year","y") : "")));
        if (months > 0 && (onlyUnits == null || onlyUnits.contains("month")))
            components.add(Component.text(months + (showSuffixes ? lang.getOrDefault("system.time.month","mo") : "")));
        if (days > 0 && (onlyUnits == null || onlyUnits.contains("day")))
            components.add(Component.text(days + (showSuffixes ? lang.getOrDefault("system.time.day","d") : "")));
        if (hours > 0 && (onlyUnits == null || onlyUnits.contains("hour")))
            components.add(Component.text(hours + (showSuffixes ? lang.getOrDefault("system.time.hour","h") : "")));
        if (minutes > 0 && (onlyUnits == null || onlyUnits.contains("minutes")))
            components.add(Component.text(minutes + (showSuffixes ? lang.getOrDefault("system.time.minutes","m") : "")));
        if ((seconds > 0 && (onlyUnits == null || onlyUnits.contains("seconds"))) || totalSeconds == 0)
            components.add(Component.text(seconds + (showSuffixes ? lang.getOrDefault("system.time.seconds","s") : "")));

        return components;
    }
}