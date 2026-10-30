package elya.emulator.constants.logs;

import lombok.experimental.*;

/**
 * Static dictionary of error log templates used across the emulator application.
 * <p>Consolidating log messages here ensures consistency in application diagnostics
 * and simplifies the process of monitoring and debugging system failures.</p>
 */
@UtilityClass
public class EmulatorErrorLogs {

    /** Log message for critical errors during mock data persistence. */
    public static final String ERROR_DURING_MOCK_PERSISTENCE_FOR_TOKEN = "Critical error during mock persistence for token: {}";

    /** Logged with the stack trace when a request fails with a 5xx status: status code, request description. */
    public static final String REQUEST_FAILED = "Request failed with status [{}] ({})";

    /** Logged with the stack trace when a storage file cannot be read: file path. */
    public static final String FAILED_LOAD_STORAGE_FILE = "Failed to load storage file [{}]; starting empty";

    /** Logged with the stack trace when a snapshot cannot be written: file path. */
    public static final String FAILED_SAVE_STORAGE_FILE = "Failed to save storage file [{}]";
}