package elya.emulator.constants.logs;

import lombok.experimental.UtilityClass;

/**
 * Static dictionary of warning log templates for the API Emulator.
 * <p>Contains messages for non-critical failures and edge cases that
 * do not interrupt the main application flow but may require attention.</p>
 */
@UtilityClass
public class ApiWarnLogs {

    /** Logged when a card deletion is requested but the ID does not exist in the state. */
    public static final String CARD_NOT_FOUND_BY_ID_FOR_DELETION = "Card with ID [{}] not found for deletion";

    /** Logged when the application fails to clean up the persistence file from the disk. */
    public static final String FAILED_DELETE_STORAGE_FILE = "Failed to delete persistence storage file.";
}