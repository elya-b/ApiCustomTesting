package elya.emulator.constants.excpetions;

import org.springframework.http.HttpStatus;

/**
 * Exception specifically designed for authentication and token-related errors.
 * Defaults to 401 Unauthorized status but allows overriding if necessary.
 */
public class TokenValidationException extends ApiEmulatorException {

    /**
     * Constructs a TokenValidationException with a default 401 Unauthorized status.
     *
     * @param message The detailed authentication error message.
     */
    public TokenValidationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Constructs a TokenValidationException with a custom HTTP status.
     *
     * @param message The detailed error message.
     * @param status  The specific HTTP status code.
     */
    public TokenValidationException(String message, HttpStatus status) {
        super(message, status);
    }
}
