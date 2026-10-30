package elya.emulator.repository;

import elya.dto.bankcard.*;
import elya.emulator.storage.*;
import jakarta.annotation.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static elya.emulator.constants.logs.EmulatorErrorLogs.*;
import static elya.emulator.constants.logs.EmulatorInfoLogs.*;
import static elya.emulator.logging.SensitiveDataMasker.*;

/**
 * Repository for managing persistent mock responses.
 * <p>Keeps the mocks in a thread-safe in-memory map, which is the source of truth, and mirrors
 * every change to a JSON file through {@link JsonSnapshotStore}. How long the file lives is set
 * by its {@link elya.StorageRetention}.</p>
 */
@Slf4j
@Component
public class MockRepository {
    private final JsonSnapshotStore<BankCardListResponse> store;
    private final Map<String, BankCardListResponse> mockedResponses = new ConcurrentHashMap<>();

    /**
     * Creates the repository on top of its file store.
     *
     * @param store the JSON store of the mocks, configured by {@code api.emulator.storage.mocks.*}
     */
    public MockRepository(JsonSnapshotStore<BankCardListResponse> store) {
        this.store = store;
    }

    /**
     * Loads the mocks the store restores on startup (none with the default retention).
     */
    @PostConstruct
    public void init() {
        mockedResponses.putAll(store.restore());
    }

    /**
     * Releases the store on shutdown; with the default retention this deletes the file.
     */
    @PreDestroy
    public void shutdown() {
        store.release();
    }

    /**
     * Persists the bank card response in memory and updates the physical storage file.
     *
     * @param token    unique session identifier associated with the mock data
     * @param response fully formed response object containing card data and metadata
     * @return         true if the data was successfully saved to both memory and file
     */
    public boolean save(String token, BankCardListResponse response) {
        try {
            log.info(PERSISTING_MOCK_DATA_TO_STORAGE_FOR_TOKEN, maskToken(token));
            mockedResponses.put(token, response);
            store.save(mockedResponses);
            return true;
        } catch (Exception e) {
            log.error(ERROR_DURING_MOCK_PERSISTENCE_FOR_TOKEN, maskToken(token), e);
            return false;
        }
    }

    /**
     * Finds the mocked response associated with the given token.
     *
     * @param token unique session identifier
     * @return      an {@link Optional} containing the response if it exists,
     * otherwise an empty Optional
     */
    public Optional<BankCardListResponse> find(String token) {
        return Optional.ofNullable(mockedResponses.get(token));
    }

    /**
     * Removes the mock response for the specified token and synchronizes changes with the storage file.
     *
     * @param token unique session identifier to be cleared
     * @return      true if the record was successfully found and removed;
     * false if no record existed for the given token
     */
    public boolean clear(String token) {
        BankCardListResponse removed = mockedResponses.remove(token);
        if (removed != null) {
            store.save(mockedResponses);
        }
        return removed != null;
    }

    /**
     * Completely wipes all data from memory and deletes the persistence file from the disk.
     * Typically used for broad test cleanup or system resets.
     */
    public void clearAll() {
        mockedResponses.clear();
        store.delete();
    }

    /**
     * Atomically applies {@code change} to the response stored for {@code token}.
     * <p>Optimistic compare-and-set: reads the current value, lets {@code change}
     * build a new one, and swaps it in only if no other writer replaced the entry
     * in the meantime; otherwise retries with the fresh value. {@code change} may
     * therefore run more than once and must be free of side effects.</p>
     *
     * @param token  session whose mock response is being changed
     * @param change pure function from the current value (empty if nothing is stored)
     *               to the new value, or {@link Optional#empty()} to leave storage untouched
     * @return the value that was written, or empty if {@code change} chose not to write
     */
    public Optional<BankCardListResponse> update(
            String token,
            Function<Optional<BankCardListResponse>, Optional<BankCardListResponse>> change) {
        while (true) {
            BankCardListResponse current = mockedResponses.get(token);
            Optional<BankCardListResponse> next = change.apply(Optional.ofNullable(current));
            if (next.isEmpty()) {
                return Optional.empty();
            }
            boolean swapped = (current == null)
                    ? mockedResponses.putIfAbsent(token, next.get()) == null
                    : mockedResponses.replace(token, current, next.get());
            if (swapped) {
                store.save(mockedResponses);
                return next;
            }
        }
    }
}