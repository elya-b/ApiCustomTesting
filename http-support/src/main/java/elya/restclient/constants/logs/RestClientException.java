package elya.restclient.constants.logs;

/**
 * Base exception class for the REST client module.
 * Used to signal errors during API communication or data processing.
 */
public class RestClientException extends RuntimeException {
    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail error message
     */
    public RestClientException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail error message
     * @param cause   the cause of the exception (usually an IOException or Serialization error)
     */
    public RestClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
