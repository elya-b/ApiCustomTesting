package emulator.repository;

import elya.emulator.objects.TokenRecord;
import elya.emulator.repository.JsonSnapshotStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Named.named;

/**
 * Unit tests for {@link elya.emulator.repository.JsonSnapshotStore}.
 * <ul>
 *   <li>{@code load()} — returns an empty snapshot when the file does not exist</li>
 *   <li>{@code save()} + {@code load()} — round-trips the entries</li>
 *   <li>{@code save()} — creates missing parent directories</li>
 *   <li>{@code save()} — replaces the file and leaves no temporary file behind</li>
 *   <li>{@code load()} — returns an empty snapshot for unreadable content (parameterized)</li>
 *   <li>{@code delete()} — removes the storage file</li>
 *   <li>{@code delete()} — does not throw when the file does not exist</li>
 * </ul>
 */
public class JsonSnapshotStoreTests {

    private static final String STORAGE_FILE_NAME = "session.json";
    private static final String TOKEN = "token-1";
    private static final String OTHER_TOKEN = "token-2";
    private static final TokenRecord RECORD = new TokenRecord("admin_user", 1_000L);
    private static final TokenRecord OTHER_RECORD = new TokenRecord("test_user", 2_000L);

    @TempDir
    Path tempDir;

    private Path storageFile;
    private JsonSnapshotStore<TokenRecord> store;

    @BeforeEach
    void setUp() {
        storageFile = tempDir.resolve(STORAGE_FILE_NAME);
        store = new JsonSnapshotStore<>(storageFile, TokenRecord.class);
    }

    @Test
    @DisplayName("load() - Should return an empty snapshot when the file does not exist")
    void load_ShouldReturnEmpty_WhenFileDoesNotExist() {
        Map<String, TokenRecord> snapshot = store.load();

        assertTrue(snapshot.isEmpty(), "Snapshot must be empty when there is no file");
    }

    @Test
    @DisplayName("save() + load() - Should round-trip the entries")
    void saveAndLoad_ShouldRoundTripEntries() {
        Map<String, TokenRecord> entries = Map.of(TOKEN, RECORD, OTHER_TOKEN, OTHER_RECORD);

        store.save(entries);

        assertEquals(entries, store.load(), "Loaded snapshot must equal the saved one");
    }

    @Test
    @DisplayName("save() - Should create missing parent directories")
    void save_ShouldCreateParentDirectories() {
        Path nestedFile = tempDir.resolve("nested").resolve("dir").resolve(STORAGE_FILE_NAME);
        JsonSnapshotStore<TokenRecord> nestedStore = new JsonSnapshotStore<>(nestedFile, TokenRecord.class);

        nestedStore.save(Map.of(TOKEN, RECORD));

        assertTrue(Files.exists(nestedFile), "Storage file must be created with its parent directories");
    }

    @Test
    @DisplayName("save() - Should replace the file and leave no temporary file behind")
    void save_ShouldReplaceFileWithoutLeavingTemporaryFiles() throws IOException {
        store.save(Map.of(TOKEN, RECORD));
        store.save(Map.of(OTHER_TOKEN, OTHER_RECORD));

        List<Path> files;
        try (Stream<Path> listing = Files.list(tempDir)) {
            files = listing.toList();
        }

        assertAll("Only the storage file with the latest snapshot must remain",
                () -> assertEquals(List.of(storageFile), files, "No temporary file may remain"),
                () -> assertEquals(Map.of(OTHER_TOKEN, OTHER_RECORD), store.load(), "The latest snapshot must win"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unreadableContents")
    @DisplayName("load() - Should return an empty snapshot for unreadable content")
    void load_ShouldReturnEmpty_WhenContentIsUnreadable(String content) throws IOException {
        Files.writeString(storageFile, content);

        assertTrue(store.load().isEmpty(), "Unreadable content must not break the startup");
    }

    @Test
    @DisplayName("delete() - Should remove the storage file")
    void delete_ShouldRemoveFile() {
        store.save(Map.of(TOKEN, RECORD));

        store.delete();

        assertFalse(Files.exists(storageFile), "Storage file must be deleted");
    }

    @Test
    @DisplayName("delete() - Should not throw when the file does not exist")
    void delete_ShouldNotThrow_WhenFileDoesNotExist() {
        assertDoesNotThrow(store::delete, "Deleting a missing file must be a no-op");
    }

    private static Stream<Arguments> unreadableContents() {
        return Stream.of(
                Arguments.of(named("broken JSON", "{ !!! invalid json !!! }")),
                Arguments.of(named("JSON null", "null")),
                Arguments.of(named("empty file", "")));
    }
}