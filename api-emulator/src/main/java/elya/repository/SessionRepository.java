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

import static elya.emulator.constants.logs.ApiErrorLogs.*;
import static elya.emulator.constants.logs.ApiInfoLogs.*;

/**
 * Repository for managing active emulator sessions.
 * Provides thread-safe in-memory storage with file-based persistence
 * to ensure session continuity across application restarts.
 * * <p>Designed to be decoupled from business logic to facilitate future migration
 * to external storage solutions like Redis.</p>
 */
@Slf4j
@Component
public class SessionRepository {
    private final String storagePath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Internal thread-safe storage for active sessions.
     * <ul>
     * <li>Key: Unique token string</li>
     * <li>Value: {@link TokenRecord} containing user login and expiration timestamp</li>
     * </ul>
     */
    private final Map<String, TokenRecord> activeSessions = new ConcurrentHashMap<>();

    /**
     * Initializes the repository with a configurable storage path.
     *
     * @param storagePath filesystem path to the JSON file where sessions are persisted.
     * Defaults to 'session.json' if not explicitly configured.
     */
    public SessionRepository(@Value("${session.storage.path:session.json}") String storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * Synchronizes the in-memory state with the file system upon application startup.
     * If the persistence file exists, it is deserialized into the active sessions map.
     */
    @PostConstruct
    public void init() {
        File file = new File(storagePath);
        if (file.exists()) {
            try {
                Map<String, TokenRecord> loaded = objectMapper.readValue(file, new TypeReference<>() {});
                activeSessions.putAll(loaded);
                log.info(LOADED_SESSIONS_FROM_STORAGE, activeSessions.size());
            } catch (IOException e) {
                log.error(FAILED_LOAD_SESSION_FROM_FILE, e);
            }
        }
    }

    /**
     * Registers a new session or updates an existing one in both memory and disk storage.
     *
     * @param token  unique session identifier (Auth Token)
     * @param record {@link TokenRecord} containing metadata like login and expiry time
     */
    public void save(String token, TokenRecord record) {
        activeSessions.put(token, record);
        saveToFile();
    }

    /**
     * Retrieves session data associated with the provided token.
     *
     * @param token unique session identifier to look up
     * @return an {@link Optional} containing the record if found, otherwise an empty Optional
     */
    public Optional<TokenRecord> find(String token) {
        return Optional.ofNullable(activeSessions.get(token));
    }

    /**
     * Permanently removes a session from memory and updates the persistence file.
     *
     * @param token session identifier to be invalidated and deleted
     */
    public void delete(String token) {
        activeSessions.remove(token);
        saveToFile();
    }

    /**
     * Flushes the current in-memory session map to the JSON storage file.
     * This method is synchronized to ensure atomic write operations and prevent data corruption.
     */
    private synchronized void saveToFile() {
        try {
            File file = new File(storagePath);
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                parentDir.mkdirs();
            }
            objectMapper.writeValue(file, activeSessions);
        } catch (IOException e) {
            log.error(FAILED_SAVE_SESSION_TO_STORAGE, e);
        }
    }

    /**
     * Wipes all active sessions from both memory and the physical storage file.
     * Primarily used for system resets or full cleanup operations.
     */
    public void clear() {
        activeSessions.clear();
        File file = new File(storagePath);
        if (file.exists()) {
            file.delete();
        }
    }
}