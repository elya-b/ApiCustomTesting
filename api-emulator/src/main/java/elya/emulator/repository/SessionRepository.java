package elya.emulator.repository;

import elya.emulator.config.*;
import elya.emulator.objects.*;
import elya.emulator.storage.*;
import jakarta.annotation.*;
import org.springframework.stereotype.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Repository for managing active emulator sessions.
 * <p>Keeps the sessions in a thread-safe in-memory map, which is the source of truth, and mirrors
 * every change to a JSON file through {@link JsonSnapshotStore}. How long the file lives is set
 * by its {@link StorageRetention}: by default, tokens never outlive the emulator.</p>
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
     * Creates the repository on top of its file store.
     *
     * @param store the JSON store of the sessions, configured by {@code api.emulator.storage.sessions.*}
     */
    public SessionRepository(JsonSnapshotStore<TokenRecord> store) {
        this.store = store;
    }

    /**
     * Loads the sessions the store restores on startup (none with the default retention).
     */
    @PostConstruct
    public void init() {
        activeSessions.putAll(store.restore());
    }

    /**
     * Releases the store on shutdown; with the default retention this deletes the file.
     */
    @PreDestroy
    public void shutdown() {
        store.release();
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