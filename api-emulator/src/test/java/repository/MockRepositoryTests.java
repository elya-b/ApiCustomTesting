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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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

        // Assert
        assertTrue(result);
        // Directly check the file existence to prove 'save' did its job
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

        // ASSERT
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

        // Assert
        assertTrue(resultClear);
        assertFalse(resultFind.isPresent());
        assertTrue(new File(fullPath).exists());
    }

    @Test
    @DisplayName("clear() - Should return false when token data does not exist")
    void clear_ShouldReturnFalse_WhenTokenDoesNotExist() {

        boolean resultClear = repository.clear(TOKEN);
        Optional<BankCardListResponse> resultFind = repository.find(TOKEN);

        // Assert
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

        // Trigger the loading logic
        repository2.init();
        // ASSERT: Data should now be present in the new instance
        Optional<BankCardListResponse> foundData = repository2.find(TOKEN);
        assertTrue(foundData.isPresent(), "Data should be restored from the file");
    }

    @Test
    @DisplayName("init() - Should handle corrupted JSON file gracefully")
    void init_ShouldHandleCorruptedFile() throws IOException {
        File file = new File(fullPath);
        Files.writeString(file.toPath(), "{ invalid_json: ... }");

        // ACT
        MockRepository corruptedRepo = new MockRepository(fullPath);
        corruptedRepo.init();

        // ASSERT: Should not crash, just stay empty
        assertTrue(corruptedRepo.find(TOKEN).isEmpty());
    }
}
