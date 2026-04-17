package elya.emulator.constants.excpetions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base custom exception for the API Emulator application.
 * <p>Extends {@link RuntimeException} to provide unchecked exception handling
 * while encapsulating a specific {@link HttpStatus}. This allows for unified
 * error handling and consistent API error responses.</p>
 */
@Getter
public class ApiEmulatorException extends RuntimeException {

    /**
     * The HTTP status code intended to be returned in the API response.
     */
    private final HttpStatus status;

    /**
     * Constructs a new exception with the specified detail message and HTTP status.
     *
     * @param message the detail message explaining the reason for the exception.
     * @param status  the {@link HttpStatus} code that represents this error to the client.
     */
    public ApiEmulatorException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}