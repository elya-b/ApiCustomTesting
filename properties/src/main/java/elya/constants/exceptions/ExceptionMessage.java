package elya.constants.exceptions;

import lombok.experimental.UtilityClass;

/**
 * Constants for critical exception messages related to the emulator's core logic.
 * <p>Used primarily by {@link ApiEmulatorConfigurationException} to signal
 * fatal setup errors that prevent the application from starting correctly.</p>
 */
@UtilityClass
public class ExceptionMessage {

    /**
     * Error message used when the emulator detects missing security credentials
     * in the environment configuration.
     */
    public static final String EMULATOR_START_FAILED_NO_CREDENTIALS =
            "API Emulator cannot start without credentials. Check your application.yml file.";
}