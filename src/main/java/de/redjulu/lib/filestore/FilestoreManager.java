package de.redjulu.lib.filestore;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Save and load data by bucket (sub-table) and path. Backend from db.yml.
 */
public final class FilestoreManager {

    private static final String DEFAULT_BUCKET = "default";

    private final FilestoreBackend backend;

    FilestoreManager(@NotNull FilestoreType type, @NotNull FilestoreConfig config) {
        this.backend = new SqlFilestoreBackend(type, config);
    }

    private static String[] splitBucketPath(String path) {
        int i = path.indexOf('/');
        if (i <= 0) return new String[]{DEFAULT_BUCKET, path};
        String bucket = path.substring(0, i);
        if (bucket.isBlank()) return new String[]{DEFAULT_BUCKET, path};
        return new String[]{bucket, path.substring(i + 1)};
    }

    // --- Explicit bucket API ---

    public void save(@NotNull String bucket, @NotNull String path, byte @NotNull [] bytes) {
        backend.save(bucket, path, bytes);
    }

    public void save(@NotNull String bucket, @NotNull String path, @NotNull String content) {
        backend.save(bucket, path, content.getBytes(StandardCharsets.UTF_8));
    }

    public void save(@NotNull String bucket, @NotNull String path, @NotNull Map<String, Object> map) {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Object> e : map.entrySet()) config.set(e.getKey(), e.getValue());
        save(bucket, path, config.saveToString());
    }

    public void save(@NotNull String bucket, @NotNull String path, @NotNull List<?> list) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("list", list);
        save(bucket, path, config.saveToString());
    }

    public byte @Nullable [] load(@NotNull String bucket, @NotNull String path) {
        return backend.load(bucket, path);
    }

    @Nullable
    public String loadString(@NotNull String bucket, @NotNull String path) {
        byte[] b = backend.load(bucket, path);
        return b == null ? null : new String(b, StandardCharsets.UTF_8);
    }

    @Nullable
    public Map<String, Object> loadMap(@NotNull String bucket, @NotNull String path) {
        String content = loadString(bucket, path);
        if (content == null) return null;
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.load(new StringReader(content));
            return sectionToMap(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load map: " + bucket + "/" + path, e);
        }
    }

    @Nullable
    public List<?> loadList(@NotNull String bucket, @NotNull String path) {
        String content = loadString(bucket, path);
        if (content == null) return null;
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.load(new StringReader(content));
            return config.getList("list");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load list: " + bucket + "/" + path, e);
        }
    }

    public boolean exists(@NotNull String bucket, @NotNull String path) {
        return backend.exists(bucket, path);
    }

    public void delete(@NotNull String bucket, @NotNull String path) {
        backend.delete(bucket, path);
    }

    @NotNull
    public List<String> list(@NotNull String bucket) {
        return backend.list(bucket);
    }

    // --- Path-only API (backward compat: "bucket/path" or default bucket) ---

    public void save(@NotNull String path, byte @NotNull [] bytes) {
        var sp = splitBucketPath(path);
        backend.save(sp[0], sp[1], bytes);
    }

    public void save(@NotNull String path, @NotNull String content) {
        var sp = splitBucketPath(path);
        backend.save(sp[0], sp[1], content.getBytes(StandardCharsets.UTF_8));
    }

    public void save(@NotNull String path, @NotNull Map<String, Object> map) {
        var sp = splitBucketPath(path);
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Object> e : map.entrySet()) config.set(e.getKey(), e.getValue());
        backend.save(sp[0], sp[1], config.saveToString().getBytes(StandardCharsets.UTF_8));
    }

    public void save(@NotNull String path, @NotNull List<?> list) {
        var sp = splitBucketPath(path);
        YamlConfiguration config = new YamlConfiguration();
        config.set("list", list);
        backend.save(sp[0], sp[1], config.saveToString().getBytes(StandardCharsets.UTF_8));
    }

    public byte @Nullable [] load(@NotNull String path) {
        var sp = splitBucketPath(path);
        return backend.load(sp[0], sp[1]);
    }

    @Nullable
    public String loadString(@NotNull String path) {
        byte[] b = load(path);
        return b == null ? null : new String(b, StandardCharsets.UTF_8);
    }

    @Nullable
    public Map<String, Object> loadMap(@NotNull String path) {
        var sp = splitBucketPath(path);
        return loadMap(sp[0], sp[1]);
    }

    @Nullable
    public List<?> loadList(@NotNull String path) {
        var sp = splitBucketPath(path);
        return loadList(sp[0], sp[1]);
    }

    public boolean exists(@NotNull String path) {
        var sp = splitBucketPath(path);
        return backend.exists(sp[0], sp[1]);
    }

    public void delete(@NotNull String path) {
        var sp = splitBucketPath(path);
        backend.delete(sp[0], sp[1]);
    }

    private static Map<String, Object> sectionToMap(ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            Object val = section.get(key);
            if (val instanceof ConfigurationSection nested) {
                map.put(key, sectionToMap(nested));
            } else {
                map.put(key, val);
            }
        }
        return map;
    }

    public void shutdown() {
        ((SqlFilestoreBackend) backend).close();
    }
}
