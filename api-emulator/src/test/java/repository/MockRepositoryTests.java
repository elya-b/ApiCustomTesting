package repository;

import elya.dto.bankcard.BankCardListResponse;
import elya.repository.MockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for {@link elya.repository.MockRepository}.
 * <ul>
 *   <li>{@code save()} — persists data in memory and creates a file on disk</li>
 *   <li>{@code find()} — returns an empty Optional when the token does not exist</li>
 *   <li>{@code find()} — returns data when the token exists</li>
 *   <li>{@code clear()} — removes data and returns true when the token exists</li>
 *   <li>{@code clear()} — returns false and does not throw when the token is absent</li>
 *   <li>{@code init()} — loads data from an existing file into memory</li>
 *   <li>{@code init()} — gracefully handles a corrupted JSON file</li>
 *   <li>{@code init()} — starts empty when the storage file does not exist</li>
 *   <li>{@code save()} — overwrites existing data for the same token</li>
 *   <li>{@code clearAll()} — removes all data from memory</li>
 *   <li>{@code clearAll()} — deletes the persistence file from disk</li>
 *   <li>{@code clearAll()} — does not throw when the file does not exist</li>
 * </ul>
 */
public class MockRepositoryTests {

    private MockRepository repository;
    private String fullPath;
    private static final String TOKEN = "token";

    // Create a temporary folder for each test run
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fullPath = tempDir.resolve("test_mock_storage.json").toString();
        repository = new MockRepository(fullPath);
    }

    @Test
    @DisplayName("save() - Should store data in memory and create a file")
    void save_ShouldPersistData() {
        BankCardListResponse response = BankCardListResponse.builder().build();

        boolean result = repository.save(TOKEN, response);

        assertTrue(result);
        File file = new File(fullPath);
        assertTrue(file.exists(), "Physical file must be created");
    }

    @Test
    @DisplayName("find() - Should return empty Optional when token does not exist")
    void find_ShouldReturnEmpty_WhenTokenNotFound() {
        Optional<BankCardListResponse> result = repository.find(TOKEN);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("find() - Should return data when token exists")
    void find_ShouldReturnData_WhenTokenExists() {
        BankCardListResponse response = BankCardListResponse.builder().build();
        repository.save(TOKEN, response);

        Optional<BankCardListResponse> result = repository.find(TOKEN);

        assertTrue(result.isPresent());
        assertEquals(response, result.get());
    }

    @Test
    @DisplayName("clear() - Should remove data and return true if token exists")
    void clear_ShouldRemoveData_AndReturnTrue_IfTokenExists() {
        BankCardListResponse response = BankCardListResponse.builder().build();
        repository.save(TOKEN, response);

        boolean resultClear = repository.clear(TOKEN);
        Optional<BankCardListResponse> resultFind = repository.find(TOKEN);

        assertTrue(resultClear);
        assertFalse(resultFind.isPresent());
        assertTrue(new File(fullPath).exists());
    }

    @Test
    @DisplayName("clear() - Should return false when token data does not exist")
    void clear_ShouldReturnFalse_WhenTokenDoesNotExist() {

        boolean resultClear = repository.clear(TOKEN);
        Optional<BankCardListResponse> resultFind = repository.find(TOKEN);

        assertFalse(resultClear);
        assertFalse(resultFind.isPresent());
        assertFalse(new File(fullPath).exists());
    }

    @Test
    @DisplayName("init() - Should load data from existing file into memory")
    void init_ShouldLoadDataFromExistingFile() {
        BankCardListResponse response = BankCardListResponse.builder().build();

        repository.save(TOKEN, response);
        assertTrue(new File(fullPath).exists(), "Baseline file should exist on disk");

        MockRepository repository2 = new MockRepository(fullPath);
        assertTrue(repository2.find(TOKEN).isEmpty(), "New repository should be empty before init");

        repository2.init();
        Optional<BankCardListResponse> foundData = repository2.find(TOKEN);
        assertTrue(foundData.isPresent(), "Data should be restored from the file");
    }

    @Test
    @DisplayName("init() - Should handle corrupted JSON file gracefully")
    void init_ShouldHandleCorruptedFile() throws IOException {
        File file = new File(fullPath);
        Files.writeString(file.toPath(), "{ invalid_json: ... }");

        MockRepository corruptedRepo = new MockRepository(fullPath);
        corruptedRepo.init();

        assertTrue(corruptedRepo.find(TOKEN).isEmpty());
    }

    // --- ADDITIONAL CASES ---

    @Test
    @DisplayName("init() - Should start empty when storage file does not exist")
    void init_ShouldStartEmpty_WhenFileDoesNotExist() {
        // fullPath points to a non-existent file (setUp creates path but not the file itself)
        assertFalse(new File(fullPath).exists(), "Precondition: file must not exist");

        repository.init();

        assertTrue(repository.find(TOKEN).isEmpty(),
                "Repository must be empty when no file is present on startup");
    }

    @Test
    @DisplayName("save() - Should overwrite existing data for the same token")
    void save_ShouldOverwrite_WhenTokenAlreadyExists() {
        BankCardListResponse first  = BankCardListResponse.builder().build();
        BankCardListResponse second = BankCardListResponse.of(List.of());

        repository.save(TOKEN, first);
        repository.save(TOKEN, second);

        Optional<BankCardListResponse> result = repository.find(TOKEN);
        assertTrue(result.isPresent());
        assertEquals(second, result.get(), "Second save must overwrite the first for the same token");
    }

    @Test
    @DisplayName("clearAll() - Should remove all data from memory")
    void clearAll_ShouldRemoveAllDataFromMemory() {
        String TOKEN_2 = "token2";
        repository.save(TOKEN,   BankCardListResponse.builder().build());
        repository.save(TOKEN_2, BankCardListResponse.builder().build());

        repository.clearAll();

        assertTrue(repository.find(TOKEN).isEmpty(),   "Token 1 must be gone after clearAll");
        assertTrue(repository.find(TOKEN_2).isEmpty(), "Token 2 must be gone after clearAll");
    }

    @Test
    @DisplayName("clearAll() - Should delete the persistence file from disk")
    void clearAll_ShouldDeleteFileFromDisk() {
        repository.save(TOKEN, BankCardListResponse.builder().build());
        assertTrue(new File(fullPath).exists(), "Precondition: file must exist before clearAll");

        repository.clearAll();

        assertFalse(new File(fullPath).exists(), "File must be deleted after clearAll");
    }

    @Test
    @DisplayName("clearAll() - Should not throw when file does not exist")
    void clearAll_ShouldNotThrow_WhenFileDoesNotExist() {
        assertFalse(new File(fullPath).exists(), "Precondition: file must not exist");

        assertDoesNotThrow(() -> repository.clearAll(),
                "clearAll must not throw even when there is no file to delete");
    }
}