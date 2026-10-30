package elya.emulator.constants.logs;

import lombok.experimental.UtilityClass;

/**
 * Static dictionary of warning log templates for the API Emulator.
 * <p>Contains messages for non-critical failures and edge cases that
 * do not interrupt the main application flow but may require attention.</p>
 */
@UtilityClass
public class EmulatorWarnLogs {

    /** Logged with the stack trace when a storage file cannot be deleted: file path. */
    public static final String FAILED_DELETE_STORAGE_FILE = "Failed to delete storage file [{}]";

    /** Logged when a request is rejected with a 4xx status: status code, request description, client-facing reason. */
    public static final String REQUEST_REJECTED = "Request rejected with status [{}] ({}): {}";
}