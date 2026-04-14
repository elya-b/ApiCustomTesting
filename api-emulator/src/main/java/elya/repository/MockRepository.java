package elya.repository;

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

/**
 * Repository for managing persistent mock responses.
 * Survives application restarts by saving data to a JSON file.
 */
@Slf4j
@Component
public class MockRepository {
    private final String storagePath;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, BankCardListResponse> mockedResponses = new ConcurrentHashMap<>();

    public MockRepository(@Value("${mock.storage.path:mock_response.json}") String storagePath) {
        this.storagePath = storagePath;
    }

    @PostConstruct
    public void init() {
        File file = new File(storagePath);
        if (file.exists()) {
            try {
                Map<String, BankCardListResponse> loaded = objectMapper.readValue(file,
                        new TypeReference<ConcurrentHashMap<String, BankCardListResponse>>() {});
                mockedResponses.putAll(loaded);
                log.info("Loaded mocked response from persistence storage.");
            } catch (IOException e) {
                log.error("Failed to load mock from file", e);
            }
        }
    }

    /**
     * Persists the bank card response in memory and updates the physical storage.
     *
     * @param token    unique session identifier
     * @param response fully formed response object with metadata
     * @return         true if operation completed without errors
     */
    public boolean save(String token, BankCardListResponse response) {
        try {
            log.info("Persisting mock data to storage for token: [{}]", token);
            mockedResponses.put(token, response);
            saveToFile();
            return true;
        } catch (Exception e) {
            log.error("Critical error during mock persistence for token: {}", token, e);
            return false;
        }
    }

    /**
     * Finds the mocked response associated with the given token.
     *
     * @param token session token
     * @return      Optional containing the response if found
     */
    public Optional<BankCardListResponse> find(String token) {
        return Optional.ofNullable(mockedResponses.get(token));
    }

    /**
     * Removes the mock response for the specified token and updates storage.
     *
     * @param token session token
     * @return      true if the record was removed
     */
    public boolean clear(String token) {
        BankCardListResponse removed = mockedResponses.remove(token);
        if (removed != null) {
            saveToFile();
        }
        return removed != null;
    }

    /**
     * Clears all data from memory and deletes the persistence file.
     * Useful for test cleanup.
     */
    public void clearAll() {
        mockedResponses.clear();
        File file = new File(storagePath);
        if (file.exists()) {
            if (file.delete()) {
                log.info("Persistence storage file deleted successfully.");
            } else {
                log.warn("Failed to delete persistence storage file.");
            }
        }
    }

    private synchronized void saveToFile() {
        try {
            objectMapper.writeValue(new File(storagePath), mockedResponses);
        } catch (IOException e) {
            log.error("Failed to save mock to storage", e);
        }
    }
}
