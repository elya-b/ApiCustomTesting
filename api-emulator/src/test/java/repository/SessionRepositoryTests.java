package repository;

import elya.emulator.objects.TokenRecord;
import elya.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        repository = new SessionRepository(fullPath);
    }

    @Test
    @DisplayName("save() - Should store session and create a file")
    void save_ShouldStoreSessionAndCreateFile() {
        TokenRecord record = new TokenRecord(LOGIN, TIME);

        repository.save(TOKEN, record);

        // Assert
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

        // Assert
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

        // Assert
        assertTrue(repository.find(TOKEN).isEmpty());
        assertTrue(repository.find(TOKEN_2).isEmpty());
        File file = new File(fullPath);
        assertTrue(file.exists(), "File should still exist but be empty");
    }

    @Test
    @DisplayName("init() - Should load sessions from existing file")
    void init_ShouldLoadSessionsFromExistingFile() {
        TokenRecord record = new TokenRecord(LOGIN, TIME);

        repository.save(TOKEN, record);
        assertTrue(new File(fullPath).exists(), "Baseline file should exist on disk");

        SessionRepository repository2 = new SessionRepository(fullPath);
        assertTrue(repository2.find(TOKEN).isEmpty(), "New repository should be empty before init");

        repository2.init();

        // Assert
        Optional<TokenRecord> loadedRecord = repository2.find(TOKEN);

        assertTrue(loadedRecord.isPresent(), "Session should be restored from file");
        assertEquals(LOGIN, loadedRecord.get().login(), "Login should match the saved one");
    }

    @Test
    @DisplayName("init() - Should handle corrupted file")
    void init_ShouldHandleCorruptedFile() throws IOException {
        File file = new File(fullPath);
        Files.writeString(file.toPath(), "{ invalid_json: ... }");

        SessionRepository corruptedRepo = new SessionRepository(fullPath);

        corruptedRepo.init();

        // Assert
        Optional<TokenRecord> loadedRecord = corruptedRepo.find(TOKEN);

        assertTrue(loadedRecord.isEmpty(), "Session should NOT be restored from file");
    }
}
