package elya.emulator.constants.excpetions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base custom exception for the API Emulator application.
 * Extends RuntimeException to provide unchecked exception handling
 * with an associated HTTP status code.
 */
@Getter
public class ApiEmulatorException extends RuntimeException {

    /**
     * The HTTP status code to be returned in the response.
     */
    private final HttpStatus status;

    /**
     * Constructs a new ApiEmulatorException with a specific message and status.
     *
     * @param message The detailed error message.
     * @param status  The HTTP status code associated with this error.
     */
    public ApiEmulatorException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
