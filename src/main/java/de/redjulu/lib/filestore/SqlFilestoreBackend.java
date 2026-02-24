package de.redjulu.lib.filestore;

import de.redjulu.lib.MessageHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Filestore backend: one table, bucket + path for structure.
 * Schema: bucket, path, content. PK (bucket, path).
 */
public final class SqlFilestoreBackend implements FilestoreBackend {

    private final FilestoreConfig config;
    private final FilestoreType type;
    private final ConcurrentHashMap<Thread, Connection> connectionPerThread = new ConcurrentHashMap<>();
    private final ReentrantLock initLock = new ReentrantLock();
    private volatile boolean tableChecked;

    public SqlFilestoreBackend(@NotNull FilestoreType type, @NotNull FilestoreConfig config) {
        this.type = type;
        this.config = config;
        ensureDriverLoaded();
    }

    private void ensureDriverLoaded() {
        switch (type) {
            case MYSQL -> { try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (ClassNotFoundException e) { throw new IllegalStateException("MySQL driver not on classpath. Add mysql-connector-j.", e); } }
            case MARIADB -> { try { Class.forName("org.mariadb.jdbc.Driver"); } catch (ClassNotFoundException e) { throw new IllegalStateException("MariaDB driver not on classpath. Add mariadb-java-client.", e); } }
            case POSTGRES -> { try { Class.forName("org.postgresql.Driver"); } catch (ClassNotFoundException e) { throw new IllegalStateException("PostgreSQL driver not on classpath. Add postgresql.", e); } }
            case H2 -> { try { Class.forName("org.h2.Driver"); } catch (ClassNotFoundException e) { throw new IllegalStateException("H2 driver not on classpath. Add h2.", e); } }
        }
    }

    private Connection getConnection() throws SQLException {
        Connection c = connectionPerThread.get(Thread.currentThread());
        if (c != null && c.isValid(2)) return c;
        if (c != null) { try { c.close(); } catch (SQLException ignored) {} connectionPerThread.remove(Thread.currentThread()); }
        try {
            Connection newConn = DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword());
            connectionPerThread.put(Thread.currentThread(), newConn);
            return newConn;
        } catch (SQLException e) {
            MessageHelper.console("Filestore DB connection failed (" + type + "): " + e.getMessage() + " - check db.yml");
            throw e;
        }
    }

    private void ensureTable(Connection conn) throws SQLException {
        if (tableChecked) return;
        initLock.lock();
        try {
            if (tableChecked) return;
            String tbl = config.getTableName();
            String contentType = switch (type) {
                case POSTGRES -> "BYTEA";
                case MYSQL, MARIADB -> "LONGTEXT";
                default -> "BLOB";
            };
            // MySQL/MariaDB utf8mb4: max key 3072 bytes = 768 chars. bucket(64)+path(256)=320 chars
            String sql = "CREATE TABLE IF NOT EXISTS " + tbl + " (bucket VARCHAR(64) NOT NULL, path VARCHAR(256) NOT NULL, content " + contentType + ", PRIMARY KEY (bucket, path))";
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
            }
            tableChecked = true;
        } finally {
            initLock.unlock();
        }
    }

    @Override
    public void save(@NotNull String bucket, @NotNull String path, byte @NotNull [] bytes) {
        try {
            Connection conn = getConnection();
            ensureTable(conn);
            String tbl = config.getTableName();
            String upsert = switch (type) {
                case MYSQL, MARIADB -> "INSERT INTO " + tbl + " (bucket, path, content) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE content = ?";
                case POSTGRES -> "INSERT INTO " + tbl + " (bucket, path, content) VALUES (?, ?, ?) ON CONFLICT (bucket, path) DO UPDATE SET content = EXCLUDED.content";
                case H2 -> "MERGE INTO " + tbl + " (bucket, path, content) KEY(bucket, path) VALUES (?, ?, ?)";
            };
            try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                ps.setString(1, bucket);
                ps.setString(2, path);
                if (type == FilestoreType.MYSQL || type == FilestoreType.MARIADB) {
                    String str = new String(bytes, StandardCharsets.UTF_8);
                    ps.setString(3, str);
                    ps.setString(4, str);
                } else if (type == FilestoreType.POSTGRES) {
                    ps.setBytes(3, bytes);
                } else {
                    ps.setBytes(3, bytes);
                }
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Filestore save failed: " + bucket + "/" + path, e);
        }
    }

    @Override
    public byte @Nullable [] load(@NotNull String bucket, @NotNull String path) {
        try {
            Connection conn = getConnection();
            ensureTable(conn);
            String tbl = config.getTableName();
            try (PreparedStatement ps = conn.prepareStatement("SELECT content FROM " + tbl + " WHERE bucket = ? AND path = ?")) {
                ps.setString(1, bucket);
                ps.setString(2, path);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    if (type == FilestoreType.MYSQL || type == FilestoreType.MARIADB) {
                        String s = rs.getString("content");
                        return s == null ? null : s.getBytes(StandardCharsets.UTF_8);
                    }
                    return rs.getBytes("content");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Filestore load failed: " + bucket + "/" + path, e);
        }
    }

    @Override
    public boolean exists(@NotNull String bucket, @NotNull String path) {
        try {
            Connection conn = getConnection();
            ensureTable(conn);
            String tbl = config.getTableName();
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM " + tbl + " WHERE bucket = ? AND path = ?")) {
                ps.setString(1, bucket);
                ps.setString(2, path);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Filestore exists failed: " + bucket + "/" + path, e);
        }
    }

    @Override
    public void delete(@NotNull String bucket, @NotNull String path) {
        try {
            Connection conn = getConnection();
            ensureTable(conn);
            String tbl = config.getTableName();
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + tbl + " WHERE bucket = ? AND path = ?")) {
                ps.setString(1, bucket);
                ps.setString(2, path);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Filestore delete failed: " + bucket + "/" + path, e);
        }
    }

    @Override
    @NotNull
    public List<String> list(@NotNull String bucket) {
        try {
            Connection conn = getConnection();
            ensureTable(conn);
            String tbl = config.getTableName();
            List<String> out = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT path FROM " + tbl + " WHERE bucket = ?")) {
                ps.setString(1, bucket);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(rs.getString("path"));
                }
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Filestore list failed: " + bucket, e);
        }
    }

    public void close() {
        Connection c = connectionPerThread.remove(Thread.currentThread());
        if (c != null) try { c.close(); } catch (SQLException ignored) {}
    }
}
