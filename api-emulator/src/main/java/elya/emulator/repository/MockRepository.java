package elya.emulator.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.dto.bankcard.BankCardListResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static elya.emulator.constants.logs.EmulatorErrorLogs.*;
import static elya.emulator.constants.logs.EmulatorInfoLogs.*;
import static elya.emulator.constants.logs.EmulatorWarnLogs.*;
import static elya.emulator.logging.SensitiveDataMasker.maskToken;

/**
 * Repository for managing persistent mock responses.
 * Provides thread-safe in-memory storage with file-based persistence
 * to survive application restarts.
 */
@Slf4j
@Component
public class MockRepository {
    private final String storagePath;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, BankCardListResponse> mockedResponses = new ConcurrentHashMap<>();

    /**
     * Initializes the repository with a configurable storage path.
     * * @param storagePath path to the JSON file where mock data is persisted.
     * Defaults to 'mock_response.json' if not provided via properties.
     */
    public MockRepository(@Value("${mock.storage.path:mock_response.json}") String storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * Loads existing mock data from the persistence storage into memory during startup.
     * If the file does not exist, the repository starts with an empty state.
     */
    @PostConstruct
    public void init() {
        File file = new File(storagePath);
        if (file.exists()) {
            try {
                Map<String, BankCardListResponse> loaded = objectMapper.readValue(file,
                        new TypeReference<ConcurrentHashMap<String, BankCardListResponse>>() {});
                mockedResponses.putAll(loaded);
                log.info(LOADED_MOCKED_RESPONSE_FROM_STORAGE);
            } catch (IOException e) {
                log.error(FAILED_LOAD_MOCK_FROM_FILE, e);
            }
        }
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
            saveToFile();
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
            saveToFile();
        }
        return removed != null;
    }

    /**
     * Completely wipes all data from memory and deletes the persistence file from the disk.
     * Typically used for broad test cleanup or system resets.
     */
    public void clearAll() {
        mockedResponses.clear();
        File file = new File(storagePath);
        if (file.exists()) {
            if (file.delete()) {
                log.info(STORAGE_FILE_DELETED_SUCCESSFULLY);
            } else {
                log.warn(FAILED_DELETE_STORAGE_FILE);
            }
        }
    }

    /**
     * Internal helper method to write the current in-memory state to the JSON storage file.
     * Synchronized to prevent concurrent write issues.
     */
    private synchronized void saveToFile() {
        try {
            File file = new File(storagePath);
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                parentDir.mkdirs();
            }
            objectMapper.writeValue(file, mockedResponses);
        } catch (IOException e) {
            log.error(FAILED_SAVE_SESSION_TO_STORAGE, e);
        }
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
                saveToFile();
                return next;
            }
        }
    }
}