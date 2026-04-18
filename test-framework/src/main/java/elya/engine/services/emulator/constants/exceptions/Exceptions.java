package elya.engine.services.emulator.constants.exceptions;

import lombok.experimental.UtilityClass;

/**
 * Static dictionary for emulator lifecycle and infrastructure exceptions.
 * <p>Contains error messages specifically related to the control,
 * startup, and resource management of the API emulator process.</p>
 */
@UtilityClass
public class Exceptions {

    /** Logged or thrown when the emulator fails to produce a valid JWT/Session token during setup. */
    public static final String FAILED_TO_GENERATE_AUTH_TOKEN = "Failed to generate auth token";

    /** * Base message for timeout scenarios.
     * Usually followed by the time duration and unit (e.g., "30 seconds").
     */
    public static final String FAILED_TO_START_EMULATOR = "Emulator failed to start within ";

    /** Logged when the thread waiting for the emulator to initialize is unexpectedly stopped. */
    public static final String INTERRUPTED_EMULATOR_STARTUP = "Interrupted while waiting for emulator startup";
}