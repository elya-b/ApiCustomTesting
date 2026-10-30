package elya.engine.services.emulator.constants.logs;

import lombok.experimental.*;

/**
 * Static dictionary for emulator lifecycle informational logs.
 * <p>Used by the management service to track startup milestones and
 * port assignments during integration test execution.</p>
 */
@UtilityClass
public class InfoLogs {

    /** * Logged when the emulator process has fully initialized and
     * is capable of handling incoming REST requests.
     */
    public static final String EMULATOR_IS_READY = "Emulator is ready";

    /** Logged when the Lifecycle Manager bean is initialized. */
    public static final String MANAGER_START = "MANAGER: Lifecycle START.";

    /** Logged once the emulator is bound. Takes the port number as the single argument. */
    public static final String MANAGER_READY_ON_PORT = "MANAGER: ready on port {}";

    /** Logged after a successful login; names the user, never the token. */
    public static final String MANAGER_AUTHENTICATED_AS = "MANAGER: authenticated as [{}]";
}