package elya.emulator.constants.logs;

import lombok.experimental.UtilityClass;

/**
 * Static dictionary of error log templates used across the emulator application.
 * <p>Consolidating log messages here ensures consistency in application diagnostics
 * and simplifies the process of monitoring and debugging system failures.</p>
 */
@UtilityClass
public class ApiErrorLogs {

    /** Log message for critical errors during mock data persistence. */
    public static final String ERROR_DURING_MOCK_PERSISTENCE_FOR_TOKEN = "Critical error during mock persistence for token: {}";

    /** Log message when mock data cannot be read from the file. */
    public static final String FAILED_LOAD_MOCK_FROM_FILE = "Failed to load mock from file.";

    /** Log message when session data cannot be loaded from the file. */
    public static final String FAILED_LOAD_SESSION_FROM_FILE = "Failed to load sessions from file.";

    /** Log message when saving mock data to the internal storage fails. */
    public static final String FAILED_SAVE_MOCK_TO_STORAGE = "Failed to save mock to storage.";

    /** Log message when saving sessions to the persistence storage fails. */
    public static final String FAILED_SAVE_SESSION_TO_STORAGE = "Failed to save sessions to persistence storage.";

    /** Log message for unauthorized access or missing credentials. */
    public static final String INVALID_OR_MISSING_CREDENTIALS = "Invalid or missing credentials.";
}