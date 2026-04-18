package elya.emulator.constants.excpetions;

import elya.dto.auth.AuthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

/**
 * Global interceptor for centralized exception handling across the API Emulator.
 * <p>Uses {@link RestControllerAdvice} to intercept exceptions thrown by controllers
 * and normalize them into a consistent {@link AuthResponse} format. This ensures
 * that the client always receives a structured JSON response instead of a stack trace.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles authentication failures specifically related to token validation.
     *
     * @param ex the caught {@link TokenValidationException}.
     * @return a {@link ResponseEntity} with {@code 401 Unauthorized} and error details.
     */
    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<AuthResponse> handleTokenValidation(TokenValidationException ex) {
        log.error("Unexpected error: ", ex);
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles custom business logic exceptions derived from {@link ApiEmulatorException}.
     *
     * @param ex the caught exception containing a pre-defined HTTP status.
     * @return a {@link ResponseEntity} with the specific status and error message.
     */
    @ExceptionHandler(ApiEmulatorException.class)
    public ResponseEntity<AuthResponse> handleApiError(ApiEmulatorException ex) {
        log.error("Unexpected error: ", ex);
        return buildErrorResponse(ex.getMessage(), ex.getStatus());
    }

    /**
     * Handles JSR-303 validation errors (e.g., {@code @NotNull}, {@code @Range}).
     * <p>Extracts the first available validation message to provide specific
     * feedback to the client regarding invalid request fields.</p>
     *
     * @param ex the exception containing validation results.
     * @return a {@link ResponseEntity} with {@code 400 Bad Request}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = Optional.ofNullable(ex.getBindingResult().getFieldError())
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Validation failed");

        return buildErrorResponse(errorMessage, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles malformed or missing JSON request bodies.
     *
     * @param ex the exception thrown when the body is unreadable.
     * @return a {@link ResponseEntity} with {@code 400 Bad Request}.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AuthResponse> handleMissingBody(HttpMessageNotReadableException ex) {
        log.error("Unexpected error: ", ex);
        return buildErrorResponse("Required request body is missing", HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles missing mandatory request headers (e.g., missing Authorization header).
     *
     * @param ex the exception thrown when a required binding is missing.
     * @return a {@link ResponseEntity} with {@code 400 Bad Request}.
     */
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<AuthResponse> handleMissingHeader(ServletRequestBindingException ex) {
        log.error("Unexpected error: ", ex);
        return buildErrorResponse("Required request header is missing", HttpStatus.BAD_REQUEST);
    }

    /**
     * Catch-all handler for any unhandled internal server errors.
     * <p>Prevents sensitive technical details and stack traces from leaking to the client.</p>
     *
     * @param ex the unexpected exception.
     * @return a {@link ResponseEntity} with {@code 500 Internal Server Error}.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponse> handleGeneralException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return buildErrorResponse("An unexpected internal error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Helper method to wrap error messages into a consistent {@link AuthResponse} envelope.
     *
     * @param message the error message to display.
     * @param status  the {@link HttpStatus} code for the response.
     * @return a {@link ResponseEntity} containing the standardized error body.
     */
    private ResponseEntity<AuthResponse> buildErrorResponse(String message, HttpStatus status) {
        AuthResponse response = AuthResponse.builder()
                .success(false)
                .message(message)
                .build();

        return new ResponseEntity<>(response, status);
    }
}