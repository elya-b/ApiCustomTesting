package elya.constants.logs;

import lombok.experimental.UtilityClass;

/**
 * Static dictionary for configuration-related informational logs.
 * <p>Tracks successful initialization events during the application startup
 * and verifies that environment settings have been correctly applied.</p>
 */
@UtilityClass
public class InfoLogs {

    /**
     * Logged after the security configuration successfully parses the user
     * list from application properties.
     */
    public static final String CONFIG_LOADED_SUCCESSFULLY =
            "API Emulator Configuration: Successfully loaded {} users.";
}