package elya.constants.logs;

import lombok.experimental.UtilityClass;

/**
 * Static dictionary for configuration and startup error logs.
 * <p>Focuses on environment-level failures that prevent the emulator
 * from establishing its core operational state.</p>
 */
@UtilityClass
public class ErrorLogs {

    /**
     * Logged when the emulator's security context cannot be initialized
     * because the user list is empty or missing in the configuration properties.
     */
    public static final String CONFIG_FAILED_NO_USERS =
            "API Emulator Configuration failed: No users found in properties.";
}