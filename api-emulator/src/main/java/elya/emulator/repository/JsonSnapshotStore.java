package elya.emulator.repository;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

import static elya.emulator.constants.logs.EmulatorErrorLogs.FAILED_LOAD_STORAGE_FILE;
import static elya.emulator.constants.logs.EmulatorErrorLogs.FAILED_SAVE_STORAGE_FILE;
import static elya.emulator.constants.logs.EmulatorInfoLogs.LOADED_ENTRIES_FROM_STORAGE_FILE;
import static elya.emulator.constants.logs.EmulatorInfoLogs.STORAGE_FILE_DELETED;
import static elya.emulator.constants.logs.EmulatorWarnLogs.FAILED_DELETE_STORAGE_FILE;

/**
 * Persists a key-value snapshot as a single JSON file.
 * <p>Holds no data itself: the owning repository keeps the in-memory map and passes the whole
 * map on every change, so each {@link #save} rewrites the file with the current snapshot.</p>
 * <p>Writes are atomic: the snapshot goes to a temporary file in the same directory, which then
 * replaces the storage file, so a crash mid-write cannot leave a corrupted file behind.
 * I/O failures are logged and never propagated: the in-memory state stays the source of truth.</p>
 *
 * @param <V> the type of the stored values
 */
@Slf4j
public final class JsonSnapshotStore<V> {

    /** Suffix of the temporary file a snapshot is written to before it replaces the storage file. */
    private static final String TEMP_FILE_SUFFIX = ".tmp";

    private final Path file;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JavaType snapshotType;

    /**
     * Creates a store for the given file.
     *
     * @param file      the JSON file holding the snapshot; resolved to an absolute path
     * @param valueType the class of the stored values, needed to read them back despite type erasure
     */
    public JsonSnapshotStore(Path file, Class<V> valueType) {
        this.file = file.toAbsolutePath().normalize();
        this.snapshotType = objectMapper.getTypeFactory().constructMapType(Map.class, String.class, valueType);
    }

    /**
     * Reads the last saved snapshot.
     *
     * @return the stored entries, or an empty map if the file is missing or unreadable
     */
    public synchronized Map<String, V> load() {
        if (Files.notExists(file)) {
            return Map.of();
        }
        try (InputStream in = Files.newInputStream(file)) {
            Map<String, V> entries = Objects.requireNonNullElse(objectMapper.readValue(in, snapshotType), Map.of());
            log.info(LOADED_ENTRIES_FROM_STORAGE_FILE, entries.size(), file);
            return entries;
        } catch (IOException e) {
            log.error(FAILED_LOAD_STORAGE_FILE, file, e);
            return Map.of();
        }
    }

    /**
     * Atomically replaces the file content with the given snapshot, creating missing directories.
     *
     * @param entries the complete current state to persist
     */
    public synchronized void save(Map<String, V> entries) {
        try {
            Path directory = file.getParent();
            Files.createDirectories(directory);
            Path tempFile = Files.createTempFile(directory, file.getFileName().toString(), TEMP_FILE_SUFFIX);
            try {
                try (OutputStream out = Files.newOutputStream(tempFile)) {
                    objectMapper.writeValue(out, entries);
                }
                replaceAtomically(tempFile);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            log.error(FAILED_SAVE_STORAGE_FILE, file, e);
        }
    }

    /**
     * Deletes the storage file if it exists.
     */
    public synchronized void delete() {
        try {
            if (Files.deleteIfExists(file)) {
                log.info(STORAGE_FILE_DELETED, file);
            }
        } catch (IOException e) {
            log.warn(FAILED_DELETE_STORAGE_FILE, file, e);
        }
    }

    /**
     * Moves the temporary file over the storage file.
     * <p>Falls back to a plain replacing move on file systems without atomic moves.</p>
     *
     * @param tempFile the fully written temporary file
     * @throws IOException if the file cannot be moved
     */
    private void replaceAtomically(Path tempFile) throws IOException {
        try {
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}