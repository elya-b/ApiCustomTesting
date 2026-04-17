package elya.emulator.constants.logs;

import lombok.experimental.UtilityClass;

/**
 * Static dictionary of informational log templates for the API Emulator.
 * <p>Used to track service lifecycles, data persistence events, and
 * successful business operations without cluttering the business logic with string literals.</p>
 */
@UtilityClass
public class ApiInfoLogs {

    /** Logged when a specific card is successfully removed from the emulator's state. */
    public static final String ATTEMPTING_TO_DELETE_CARD_ID_FOR_TOKEN = "Attempting to delete cardId: [{}] for token: [{}]";

    /** Logged during the card deletion process for audit purposes. */
    public static final String CARD_WITH_ID_DELETED_SUCCESSFULLY = "Card with ID [{}] successfully deleted.";

    /** Logged when a mock response configuration is removed for a specific session. */
    public static final String CLEARING_MOCK_RESPONSE_FOR_TOKEN = "Clearing mock response for token: [{}]";

    /** Logged on application startup once the server is ready to accept requests. */
    public static final String EMULATOR_STARTED_ON_PORT_SUCCESSFULLY = "API Emulator has started successfully on the configured port.";

    /** Logged when a saved mock configuration is successfully retrieved from disk. */
    public static final String LOADED_MOCKED_RESPONSE_FROM_STORAGE = "Loaded mocked response from persistence storage.";

    /** Logged after a successful batch load of active sessions from the file system. */
    public static final String LOADED_SESSIONS_FROM_STORAGE = "Loaded {} sessions from persistence storage.";

    /** Logged when the MockResponseService is initialized. */
    public static final String MOCK_RESPONSE_SERVICE_START = "MockResponseService: Lifecycle START. Ready to receive mock configurations.";

    /** Logged when the MockResponseService is gracefully shut down. */
    public static final String MOCK_RESPONSE_SERVICE_STOP = "MockResponseService: Lifecycle STOP. Mocked data has been cleared.";

    /** Logged when the application begins writing mock data to the disk. */
    public static final String PERSISTING_MOCK_DATA_TO_STORAGE_FOR_TOKEN = "Persisting mock data to storage for token: [{}]";

    /** Logged when a new mock configuration is being parsed and updated. */
    public static final String PROCESSING_MOCK_UPDATE_REQUEST = "Processing mock update request for token: [{}]";

    /** Logged when searching for a card to verify its existence before an operation. */
    public static final String SEARCHING_FOR_CARD_ID_FOR_TOKEN = "Searching for cardId: [{}] for token: [{}]";

    /** Logged when the persistence file is cleaned up (e.g., during reset). */
    public static final String STORAGE_FILE_DELETED_SUCCESSFULLY = "Persistence storage file deleted successfully.";

    /** Logged when the TokenValidationService is initialized. */
    public static final String TOKEN_VALIDATION_SERVICE_START = "TokenValidationService: Lifecycle START. Token storage is initialized and empty.";

    /** Logged when the TokenValidationService is shut down. */
    public static final String TOKEN_VALIDATION_SERVICE_STOP = "TokenValidationService: Lifecycle STOP. All active tokens have been invalidated.";
}