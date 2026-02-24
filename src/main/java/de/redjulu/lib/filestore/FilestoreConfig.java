package de.redjulu.lib.filestore;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Database config: host, port, database, username, password, table.
 * For network DBs (MySQL, MariaDB, Postgres) or local file DB (SQLite, H2).
 */
public final class FilestoreConfig {

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String tableName;

    private FilestoreConfig(String jdbcUrl, String username, String password, String tableName) {
        this.jdbcUrl = jdbcUrl;
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
        this.tableName = tableName == null || tableName.isBlank() ? "redjulu_filestore" : tableName;
    }

    /**
     * Network DB (MySQL, MariaDB, Postgres).
     */
    public static FilestoreConfig network(@NotNull FilestoreType type, @NotNull String host, int port,
                                          @NotNull String database, String username, String password, String tableName) {
        String url = switch (type) {
            case MYSQL -> "jdbc:mysql://" + host + ":" + port + "/" + database;
            case MARIADB -> "jdbc:mariadb://" + host + ":" + port + "/" + database;
            case POSTGRES -> "jdbc:postgresql://" + host + ":" + port + "/" + database;
            default -> throw new IllegalArgumentException("Not a network type: " + type);
        };
        return new FilestoreConfig(url, username, password, tableName);
    }

    /**
     * H2: embedded file DB (local).
     */
    public static FilestoreConfig h2(@NotNull File dataFolder, @NotNull String database, String tableName) {
        String path = new File(dataFolder, database).getAbsolutePath().replace('\\', '/');
        return new FilestoreConfig("jdbc:h2:file:" + path + ";DB_CLOSE_DELAY=-1", "", "", tableName);
    }

    public @NotNull String getJdbcUrl() {
        return jdbcUrl;
    }

    public @NotNull String getUsername() {
        return username;
    }

    public @NotNull String getPassword() {
        return password;
    }

    public @NotNull String getTableName() {
        return tableName;
    }
}
