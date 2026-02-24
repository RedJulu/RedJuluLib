package de.redjulu.lib.filestore;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Backend for storing data by bucket (logical sub-table) and path.
 */
public interface FilestoreBackend {

    void save(@NotNull String bucket, @NotNull String path, byte @NotNull [] bytes);

    byte @Nullable [] load(@NotNull String bucket, @NotNull String path);

    boolean exists(@NotNull String bucket, @NotNull String path);

    void delete(@NotNull String bucket, @NotNull String path);

    /**
     * Lists all path keys in the bucket. Returns empty list if bucket has no entries.
     */
    @NotNull
    List<String> list(@NotNull String bucket);
}
