package elya.restclient.constants.logs;

/**
 * Base unchecked exception for the REST client module.
 * <p>Used to signal critical failures during remote API communication,
 * data transformation, or unexpected server responses. Being a {@link RuntimeException},
 * it allows for cleaner method signatures while still providing detailed error context.</p>
 */
public class RestClientException extends RuntimeException {

    /**
     * Constructs a new exception with a specific detail message.
     *
     * @param message the descriptive error message.
     */
    public RestClientException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a specific message and the underlying cause.
     * <p>This constructor is particularly useful for wrapping lower-level exceptions
     * like {@code IOException} or {@code JsonProcessingException}.</p>
     *
     * @param message the descriptive error message.
     * @param cause   the root cause of the failure (e.g., networking or serialization errors).
     */
    public RestClientException(String message, Throwable cause) {
        super(message, cause);
    }
}