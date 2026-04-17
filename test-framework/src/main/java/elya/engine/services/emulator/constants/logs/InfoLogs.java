package elya.engine.services.emulator.constants.logs;

import lombok.experimental.UtilityClass;

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

    /** * Logged when the Lifecycle Manager starts.
     * Use with a single placeholder for the port number (e.g., 8080).
     */
    public static final String MANAGER_START = "MANAGER: Lifecycle START. Manager is ready on port {}";
}