package de.redjulu.lib.filestore;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Loads filestore config from db.yml. Creates it with defaults if missing.
 */
public final class FilestoreConfigLoader {

    private static final String FILENAME = "db.yml";

    @NotNull
    public static FilestoreManager createManager(@NotNull JavaPlugin plugin) {
        var loaded = load(plugin);
        return new FilestoreManager(loaded.type(), loaded.config());
    }

    @NotNull
    static LoadedConfig load(@NotNull JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), FILENAME);
        if (!file.exists()) {
            createDefault(file);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return parse(yaml, plugin);
    }

    private static void createDefault(@NotNull File file) {
        try {
            file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), """
                    # H2 = local file, MYSQL/MARIADB/POSTGRES = network DB
                    type: H2

                    settings:
                      # Host / IP (network DB only)
                      host: localhost
                      # Port (3306 MySQL/MariaDB, 5432 Postgres)
                      port: 3306
                      # DB name or H2 filename
                      database: filestore
                      # Username (network DB only)
                      username: user
                      # Password (network DB only)
                      password: password
                      # Table name for stored data
                      table: redjulu_filestore
                    """);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create db.yml", e);
        }
    }

    private static LoadedConfig parse(@NotNull YamlConfiguration yaml, @NotNull JavaPlugin plugin) {
        String typeStr = yaml.getString("type", "H2").trim().toUpperCase();
        FilestoreType type = switch (typeStr) {
            case "MYSQL" -> FilestoreType.MYSQL;
            case "MARIADB" -> FilestoreType.MARIADB;
            case "POSTGRES" -> FilestoreType.POSTGRES;
            default -> FilestoreType.H2;
        };

        String base = "settings.";
        FilestoreConfig config = switch (type) {
            case MYSQL, MARIADB, POSTGRES -> FilestoreConfig.network(
                    type,
                    yaml.getString(base + "host", "localhost"),
                    yaml.getInt(base + "port", type == FilestoreType.POSTGRES ? 5432 : 3306),
                    yaml.getString(base + "database", "mydb"),
                    yaml.getString(base + "username"),
                    yaml.getString(base + "password"),
                    yaml.getString(base + "table"));
            case H2 -> FilestoreConfig.h2(plugin.getDataFolder(), yaml.getString(base + "database", "filestore"), yaml.getString(base + "table"));
        };
        return new LoadedConfig(type, config);
    }

    record LoadedConfig(@NotNull FilestoreType type, @NotNull FilestoreConfig config) {}
}
