package elya.engine.services.emulator.constants.logs;

import lombok.experimental.*;

/**
 * Static dictionary for emulator lifecycle informational logs.
 * <p>Used by the lifecycle manager and the emulator sessions to track startup
 * milestones and port assignments during integration test execution.</p>
 */
@UtilityClass
public class InfoLogs {

    /** * Logged when the emulator process has fully initialized and
     * is capable of handling incoming REST requests.
     */
    public static final String EMULATOR_IS_READY = "Emulator is ready";

    /** Logged once the emulator is bound. Takes the port number as the single argument. */
    public static final String MANAGER_READY_ON_PORT = "MANAGER: ready on port {}";

    /** Logged after a successful login; names the user, never the token. */
    public static final String MANAGER_AUTHENTICATED_AS = "MANAGER: authenticated as [{}]";

    /** Logged when a session is ready for a test. Takes the URL of its emulator. */
    public static final String SESSION_STARTED = "SESSION: emulator at {} is healthy and authenticated";

    /** Logged when a session has stopped its emulator. Takes the URL of that emulator. */
    public static final String SESSION_CLOSED = "SESSION: emulator at {} stopped";
}
