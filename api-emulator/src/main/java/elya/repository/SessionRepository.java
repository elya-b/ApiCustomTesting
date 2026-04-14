package elya.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.emulator.objects.TokenRecord;
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
 * Repository for managing active emulator sessions.
 * Decoupled from business logic to allow future scaling (e.g., moving to Redis).
 */
@Slf4j
@Component
public class SessionRepository {
    private final String storagePath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SessionRepository(@Value("${mock.storage.path:session.json}") String storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * Internal storage.
     * Key: Token String,
     * Value : TokenRecord  (login + expiry).
     */
    private final Map<String, TokenRecord> activeSessions = new ConcurrentHashMap<>();

    /**
     * Loads existing sessions from the file system on startup.
     */
    @PostConstruct
    public void init() {
        File file = new File(storagePath);
        if (file.exists()) {
            try {
                Map<String, TokenRecord> loaded = objectMapper.readValue(file, new TypeReference<>() {});
                activeSessions.putAll(loaded);
                log.info("Loaded {} sessions from persistence storage.", activeSessions.size());
            } catch (IOException e) {
                log.error("Failed to load sessions from file", e);
            }
        }
    }

    /**
     * Saves a new session or updates an existing one.
     * @param token  unique session identifier (key)
     * @param record object containing login and expiry time (value)
     */
    public void save(String token, TokenRecord record) {
        activeSessions.put(token, record);
        saveToFile();
    }

    /**
     * Retrieves session data by its token.
     * @param token unique session identifier to search for
     * @return an Optional containing the record if found, otherwise empty
     */
    public Optional<TokenRecord> find(String token) {
        return Optional.ofNullable(activeSessions.get(token));
    }

    /**
     * Removes a session and updates the persistence file.
     *
     * @param token session identifier to delete
     */
    public void delete(String token) {
        activeSessions.remove(token);
        saveToFile();
    }

    /**
     * Synchronizes the in-memory map to the JSON storage file.
     */
    private synchronized void saveToFile() {
        try {
            objectMapper.writeValue(new File(storagePath), activeSessions);
        } catch (IOException e) {
            log.error("Failed to save sessions to persistence storage", e);
        }
    }

    /**
     * Clears all sessions from memory and disk.
     */
    public void clear() {
        activeSessions.clear();
        saveToFile();
    }
}
