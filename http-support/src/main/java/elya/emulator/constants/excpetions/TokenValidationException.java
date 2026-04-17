package elya.emulator.constants.excpetions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication or token validation fails.
 * <p>By default, this exception maps to an {@code 401 Unauthorized} HTTP status,
 * signaling that the client must provide valid credentials to access the resource.</p>
 */
public class TokenValidationException extends ApiEmulatorException {

    /**
     * Constructs a new exception with the specified message and a default
     * {@link HttpStatus#UNAUTHORIZED} status.
     *
     * @param message the detailed authentication error message.
     */
    public TokenValidationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Constructs a new exception with a specific message and a custom HTTP status.
     * Useful for cases where token issues might require different status codes (e.g., 403 Forbidden).
     *
     * @param message the detailed error message.
     * @param status  the specific {@link HttpStatus} to be returned.
     */
    public TokenValidationException(String message, HttpStatus status) {
        super(message, status);
    }
}