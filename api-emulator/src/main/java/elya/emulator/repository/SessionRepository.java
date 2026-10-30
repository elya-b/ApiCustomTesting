package elya.emulator.repository;

import elya.emulator.objects.TokenRecord;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for managing active emulator sessions.
 * <p>Keeps the sessions in a thread-safe in-memory map, which is the source of truth, and mirrors
 * every change to a JSON file through {@link JsonSnapshotStore}, so sessions survive restarts.</p>
 * <p>Designed to be decoupled from business logic to facilitate future migration
 * to external storage solutions like Redis.</p>
 */
@Component
public class SessionRepository {
    private final JsonSnapshotStore<TokenRecord> store;

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
     *                    Defaults to 'session.json' if not explicitly configured.
     */
    public SessionRepository(@Value("${session.storage.path:session.json}") String storagePath) {
        this.store = new JsonSnapshotStore<>(Path.of(storagePath), TokenRecord.class);
    }

    /**
     * Loads the sessions saved by a previous run into memory.
     * Starts empty if the storage file is missing or unreadable.
     */
    @PostConstruct
    public void init() {
        activeSessions.putAll(store.load());
    }

    /**
     * Registers a new session or updates an existing one in both memory and disk storage.
     *
     * @param token  unique session identifier (Auth Token)
     * @param record {@link TokenRecord} containing metadata like login and expiry time
     */
    public void save(String token, TokenRecord record) {
        activeSessions.put(token, record);
        store.save(activeSessions);
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
        store.save(activeSessions);
    }

    /**
     * Wipes all active sessions from both memory and the physical storage file.
     * Primarily used for system resets or full cleanup operations.
     */
    public void clear() {
        activeSessions.clear();
        store.delete();
    }
}