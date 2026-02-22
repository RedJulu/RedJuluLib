package de.redjulu.lib.time;

import de.redjulu.lib.bossbar.ManagedBossbar;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.function.BiConsumer;

public class CountdownTask extends BukkitRunnable {

    private final int max;
    private int current;
    private final ManagedBossbar bar;
    private final BiConsumer<Integer, CountdownTask> tickAction;

    public CountdownTask(int max, ManagedBossbar bar, BiConsumer<Integer, CountdownTask> tickAction) {
        this.max = max;
        this.current = max;
        this.bar = bar;
        this.tickAction = tickAction;
    }

    @Override
    public void run() {
        if (bar != null) {
            bar.setValue(current, max);
        }

        tickAction.accept(current, this);

        if (current <= 0) {
            if (bar != null) bar.destroy();
            this.cancel();
            return;
        }

        current--;
    }

    public int getMax() {
        return max;
    }

    public ManagedBossbar getBar() {
        return bar;
    }
}