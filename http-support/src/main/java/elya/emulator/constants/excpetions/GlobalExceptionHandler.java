package elya.emulator.constants.excpetions;

import elya.dto.auth.AuthResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

/**
 * Global interceptor for handling exceptions across the entire application.
 * It uses Spring's @RestControllerAdvice to catch exceptions thrown by any controller
 * and transform them into a standardized JSON response format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * Specifically handles TokenValidationException to ensure 401 Unauthorized status.
     *
     * @param ex The exception thrown during failed authentication.
     * @return ResponseEntity with AuthResponse and 401 Unauthorized status.
     */
    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<AuthResponse> handleTokenValidation(TokenValidationException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles custom application exceptions derived from ApiEmulatorException.
     *
     * @param ex The caught exception containing the specific HttpStatus and error message.
     * @return ResponseEntity containing the standardized error body and the corresponding status code.
     */
    @ExceptionHandler(ApiEmulatorException.class)
    public ResponseEntity<AuthResponse> handleApiError(ApiEmulatorException ex) {
        return buildErrorResponse(ex.getMessage(), ex.getStatus());
    }

    /**
     * Handles validation exceptions that occur when @Valid body fails constraints.
     * It extracts the default message from the first validation error found.
     *
     * @param ex The MethodArgumentNotValidException containing binding results.
     * @return ResponseEntity containing the standardized error body with a 400 Bad Request status.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = Optional.ofNullable(ex.getBindingResult().getFieldError())
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Validation failed");

        return buildErrorResponse(errorMessage, HttpStatus.BAD_REQUEST);
    }

    /**
     * Catch-all handler for any unexpected internal server errors.
     * Prevents leaking technical stack traces to the client.
     *
     * @param ex The unexpected exception.
     * @return ResponseEntity with a generic error message and 500 Internal Server Error status.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponse> handleGeneralException(Exception ex) {
        return buildErrorResponse("An unexpected internal error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles cases where the request body is missing or cannot be read (e.g., null body).
     * Fixes: ApiEmulatorControllerTests.setMockResponse_ShouldHandleNullBody
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<AuthResponse> handleMissingBody(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return buildErrorResponse("Required request body is missing", HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles cases where a required header (like Authorization) is missing.
     * Fixes: ApiEmulatorControllerTests.setMockResponse_ShouldReturn400_WhenHeaderIsMissing
     */
    @ExceptionHandler(org.springframework.web.bind.ServletRequestBindingException.class)
    public ResponseEntity<AuthResponse> handleMissingHeader(org.springframework.web.bind.ServletRequestBindingException ex) {
        return buildErrorResponse("Required request header is missing", HttpStatus.BAD_REQUEST);
    }

    /**
     * Helper method to construct a standardized error ResponseEntity.
     *
     * @param message The error message to be returned to the client.
     * @param status  The HTTP status code for the response.
     * @return A ResponseEntity containing the formatted AuthResponse.
     */
    private ResponseEntity<AuthResponse> buildErrorResponse(String message, HttpStatus status) {
        AuthResponse response = AuthResponse.builder()
                .success(false)
                .message(message)
                .build();

        return new ResponseEntity<>(response, status);
    }
}
