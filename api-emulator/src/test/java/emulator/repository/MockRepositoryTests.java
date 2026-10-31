package emulator.repository;

import elya.dto.bankcard.*;
import elya.emulator.config.*;
import elya.emulator.repository.*;
import elya.emulator.storage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for {@link MockRepository}.
 * <p>The repository runs on a {@code PERSISTENT} store, so reloading from the file can be verified.</p>
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
 *   <li>{@code update()} — inserts the new value when nothing is stored</li>
 *   <li>{@code update()} — replaces the stored value with the one built from it</li>
 *   <li>{@code update()} — leaves storage untouched when the change declines to write</li>
 *   <li>{@code update()} — retries on top of a concurrent write instead of overwriting it</li>
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
        repository = newRepository();
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

        MockRepository repository2 = newRepository();
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

        MockRepository corruptedRepo = newRepository();
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

    // --- UPDATE (COMPARE-AND-SET) TESTS ---

    @Test
    @DisplayName("update() - Should insert the new value when nothing is stored")
    void update_ShouldInsert_WhenNothingStored() {
        BankCardListResponse next = BankCardListResponse.of(List.of(card(1L)));

        Optional<BankCardListResponse> written = repository.update(TOKEN, current -> {
            assertTrue(current.isEmpty(), "Nothing should be stored yet");
            return Optional.of(next);
        });

        assertAll(
                () -> assertSame(next, written.orElseThrow()),
                () -> assertSame(next, repository.find(TOKEN).orElseThrow()),
                () -> assertTrue(new File(fullPath).exists(), "A successful write must be persisted")
        );
    }

    @Test
    @DisplayName("update() - Should replace the stored value with the one built from it")
    void update_ShouldReplace_WhenValueStored() {
        repository.save(TOKEN, BankCardListResponse.of(List.of(card(1L))));

        repository.update(TOKEN, current -> {
            List<BankCardResponse> cards = new ArrayList<>(current.orElseThrow().getResponse().getCards());
            cards.add(card(2L));
            return Optional.of(BankCardListResponse.of(cards));
        });

        assertEquals(List.of(1L, 2L), cardIds(repository.find(TOKEN).orElseThrow()));
    }

    @Test
    @DisplayName("update() - Should leave storage untouched when the change declines to write")
    void update_ShouldNotWrite_WhenChangeReturnsEmpty() {
        BankCardListResponse original = BankCardListResponse.of(List.of(card(1L)));
        repository.save(TOKEN, original);

        Optional<BankCardListResponse> written = repository.update(TOKEN, current -> Optional.empty());

        assertAll(
                () -> assertTrue(written.isEmpty()),
                () -> assertSame(original, repository.find(TOKEN).orElseThrow())
        );
    }

    @Test
    @DisplayName("update() - Should retry on top of a concurrent write instead of overwriting it")
    void update_ShouldRetry_WhenAnotherWriterInterferes() {
        repository.save(TOKEN, BankCardListResponse.of(List.of(card(1L))));
        AtomicInteger attempts = new AtomicInteger();

        repository.update(TOKEN, current -> {
            if (attempts.incrementAndGet() == 1) {
                // Another writer sneaks in between our read and our compare-and-set
                repository.save(TOKEN, BankCardListResponse.of(List.of(card(1L), card(2L))));
            }
            List<BankCardResponse> cards = new ArrayList<>(current.orElseThrow().getResponse().getCards());
            cards.add(card(3L));
            return Optional.of(BankCardListResponse.of(cards));
        });

        assertAll(
                () -> assertEquals(2, attempts.get(),
                        "The first attempt must lose the race and be retried"),
                () -> assertEquals(List.of(1L, 2L, 3L), cardIds(repository.find(TOKEN).orElseThrow()),
                        "The retry must build on the concurrent write, not overwrite it")
        );
    }

    private MockRepository newRepository() {
        return new MockRepository(new JsonSnapshotStore<>(
                Path.of(fullPath), BankCardListResponse.class, StorageRetention.PERSISTENT));
    }

    private static BankCardResponse card(long id) {
        return BankCardResponse.builder().cardId(id).build();
    }

    private static List<Long> cardIds(BankCardListResponse response) {
        return response.getResponse().getCards().stream()
                .map(BankCardResponse::getCardId)
                .toList();
    }
}