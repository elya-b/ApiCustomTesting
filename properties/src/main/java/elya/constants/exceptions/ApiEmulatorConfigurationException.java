package elya.constants.exceptions;

/**
 * Exception thrown when a critical configuration error is detected during startup.
 * <p>This exception indicates that the API Emulator cannot proceed with initialization
 * due to missing or invalid configuration parameters (e.g., incorrect port mapping,
 * inaccessible storage paths, or malformed environment variables).</p>
 */
public class ApiEmulatorConfigurationException extends RuntimeException {

    /**
     * Constructs a new configuration exception with the specified detail message.
     *
     * @param message the descriptive error message explaining the configuration failure.
     */
    public ApiEmulatorConfigurationException(String message) {
        super(message);
    }
}