package de.redjulu.lib;

import de.redjulu.RedJuluLib;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final String name;
    protected final String permission;
    protected final boolean playerOnly;

    protected BaseCommand(@NotNull String name, @Nullable String permission, boolean playerOnly) {
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
    }

    public abstract void run(@NotNull CommandSender sender, @Nullable Player player, @NotNull String[] args);

    public @NotNull List<String> tab(@NotNull CommandSender sender, @NotNull String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (playerOnly && !(sender instanceof Player)) {
            try {
                MessageHelper.send(sender, "system.player_only");
            } catch (Exception e) {
                sender.sendMessage("§cThis command is for players only.");
            }
            return true;
        }

        if (permission != null && !permission.isBlank()) {
            if (!sender.hasPermission(permission)) {
                try {
                    MessageHelper.send(sender, "system.no_permission");
                } catch (Exception e) {
                    sender.sendMessage("§cYou don't have permission to do this.");
                }
                return true;
            }
        }

        try {
            run(sender, (sender instanceof Player p ? p : null), args);
        } catch (Exception e) {
            sender.sendMessage("§cAn internal error occurred while executing this command.");
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (permission != null && !permission.isBlank() && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }
        return tab(sender, args);
    }

    public void register() {
        if (RedJuluLib.getPlugin() == null) {
            return;
        }

        var cmd = RedJuluLib.getPlugin().getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        } else {
            RedJuluLib.getPlugin().getLogger().warning("Could not register /" + name + ". Is it in plugin.yml?");
        }
    }
}