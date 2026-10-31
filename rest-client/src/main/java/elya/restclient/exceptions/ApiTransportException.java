package elya.restclient.exceptions;

/**
 * Thrown when no HTTP response was received: the server could not be reached,
 * the connection failed or timed out, or the calling thread was interrupted.
 * <p>Unlike {@link ApiHttpStatusException}, it carries no status code: the client never invents one,
 * so a test cannot mistake an unreachable server for a server that answered with an error.</p>
 */
public class ApiTransportException extends RestClientException {

    /**
     * Constructs the exception with the request it failed for and the underlying cause.
     *
     * @param message the descriptive error message, naming the request URI.
     * @param cause   the network or interruption failure.
     */
    public ApiTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}