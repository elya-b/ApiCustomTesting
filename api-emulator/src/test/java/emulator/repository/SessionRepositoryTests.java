package emulator.repository;

import elya.emulator.config.*;
import elya.emulator.objects.*;
import elya.emulator.repository.*;
import elya.emulator.storage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for {@link SessionRepository}.
 * <p>The repository runs on a {@code PERSISTENT} store, so reloading from the file can be verified.</p>
 * <ul>
 *   <li>{@code save()} — persists the session and creates a file on disk</li>
 *   <li>{@code delete()} — removes the session and updates the file</li>
 *   <li>{@code clear()} — removes all sessions and deletes the file</li>
 *   <li>{@code init()} — loads sessions from an existing file</li>
 *   <li>{@code init()} — gracefully handles a corrupted JSON file</li>
 *   <li>{@code find()} — returns an empty Optional for a non-existent token</li>
 *   <li>{@code delete()} — does not throw when the token does not exist</li>
 *   <li>{@code save()} — overwrites the existing session for the same token</li>
 *   <li>{@code init()} — starts empty when the storage file does not exist</li>
 *   <li>{@code delete()} — deleted session is not restored by a subsequent {@code init()} call</li>
 * </ul>
 */
public class SessionRepositoryTests {

    private SessionRepository repository;
    private String fullPath;
    private static final String TOKEN = "token";
    private static final String LOGIN = "test";
    private static final Long TIME = System.currentTimeMillis() + 60000;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fullPath = tempDir.resolve("test_session_storage.json").toString();
        repository = newRepository();
    }

    @Test
    @DisplayName("save() - Should store session and create a file")
    void save_ShouldStoreSessionAndCreateFile() {
        TokenRecord record = new TokenRecord(LOGIN, TIME);

        repository.save(TOKEN, record);

        assertTrue(repository.find(TOKEN).isPresent());
        File file = new File(fullPath);
        assertTrue(file.exists(), "Physical file must be created");
    }

    @Test
    @DisplayName("delete() - Should remove session and update file")
    void delete_ShouldRemoveSessionAndUpdateFile() {
        TokenRecord record = new TokenRecord(LOGIN, TIME);

        repository.save(TOKEN, record);
        repository.delete(TOKEN);

        assertTrue(repository.find(TOKEN).isEmpty());
        File file = new File(fullPath);
        assertTrue(file.exists(), "File should still exist but be empty");
    }

    @Test
    @DisplayName("clear() - Should remove all sessions")
    void clear_ShouldRemoveAllSessions() {
        String TOKEN_2 = "token2";
        TokenRecord record = new TokenRecord(LOGIN, TIME);
        TokenRecord record2 = new TokenRecord("test2", TIME);

        repository.save(TOKEN, record);
        repository.save(TOKEN_2, record2);

        repository.clear();

        assertTrue(repository.find(TOKEN).isEmpty());
        assertTrue(repository.find(TOKEN_2).isEmpty());
        File file = new File(fullPath);
        assertFalse(file.exists(), "File should still exist but be empty");
    }

    @Test
    @DisplayName("init() - Should load sessions from existing file")
    void init_ShouldLoadSessionsFromExistingFile() {
        TokenRecord record = new TokenRecord(LOGIN, TIME);

        repository.save(TOKEN, record);
        assertTrue(new File(fullPath).exists(), "Baseline file should exist on disk");

        SessionRepository repository2 = newRepository();
        assertTrue(repository2.find(TOKEN).isEmpty(), "New repository should be empty before init");

        repository2.init();

        Optional<TokenRecord> loadedRecord = repository2.find(TOKEN);
        assertTrue(loadedRecord.isPresent(), "Session should be restored from file");
        assertEquals(LOGIN, loadedRecord.get().login(), "Login should match the saved one");
    }

    @Test
    @DisplayName("init() - Should handle corrupted file")
    void init_ShouldHandleCorruptedFile() throws IOException {
        File file = new File(fullPath);
        Files.writeString(file.toPath(), "{ invalid_json: ... }");

        SessionRepository corruptedRepo = newRepository();
        corruptedRepo.init();

        Optional<TokenRecord> loadedRecord = corruptedRepo.find(TOKEN);
        assertTrue(loadedRecord.isEmpty(), "Session should NOT be restored from file");
    }

    // --- ADDITIONAL CASES ---

    @Test
    @DisplayName("find() - Should return empty Optional for non-existent token")
    void find_ShouldReturnEmpty_WhenTokenNotFound() {
        Optional<TokenRecord> result = repository.find("nonexistent-token");

        assertTrue(result.isEmpty(), "find() must return empty Optional for unknown token");
    }

    @Test
    @DisplayName("delete() - Should not throw when token does not exist")
    void delete_ShouldNotThrow_WhenTokenDoesNotExist() {
        assertDoesNotThrow(() -> repository.delete("nonexistent-token"),
                "delete() must not throw when the token is not present");
    }

    @Test
    @DisplayName("save() - Should overwrite existing session for the same token")
    void save_ShouldOverwrite_WhenTokenAlreadyExists() {
        TokenRecord original  = new TokenRecord(LOGIN, TIME);
        TokenRecord overwrite = new TokenRecord("updated_login", TIME + 1000);

        repository.save(TOKEN, original);
        repository.save(TOKEN, overwrite);

        Optional<TokenRecord> result = repository.find(TOKEN);
        assertTrue(result.isPresent());
        assertEquals("updated_login", result.get().login(),
                "Second save must overwrite the first for the same token");
    }

    @Test
    @DisplayName("init() - Should start empty when storage file does not exist")
    void init_ShouldStartEmpty_WhenFileDoesNotExist() {
        assertFalse(new File(fullPath).exists(), "Precondition: file must not exist");

        repository.init();

        assertTrue(repository.find(TOKEN).isEmpty(),
                "Repository must be empty when no file is present on startup");
    }

    @Test
    @DisplayName("delete() - Deleted session must not be restored by subsequent init()")
    void delete_SessionMustNotBeRestoredByInit() {
        TokenRecord record = new TokenRecord(LOGIN, TIME);
        repository.save(TOKEN, record);
        repository.delete(TOKEN);

        // Create a new repo pointing at the same file and call init()
        SessionRepository freshRepo = newRepository();
        freshRepo.init();

        assertTrue(freshRepo.find(TOKEN).isEmpty(),
                "Deleted session must not appear after re-loading from the updated file");
    }

    private SessionRepository newRepository() {
        return new SessionRepository(new JsonSnapshotStore<>(
                Path.of(fullPath), TokenRecord.class, StorageRetention.PERSISTENT));
    }
}