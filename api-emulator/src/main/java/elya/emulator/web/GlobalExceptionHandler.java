package elya.emulator.web;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import elya.dto.auth.AuthResponse;
import elya.emulator.exceptions.ApiEmulatorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Optional;

import static elya.emulator.constants.logs.EmulatorErrorLogs.REQUEST_FAILED;
import static elya.emulator.constants.logs.EmulatorWarnLogs.REQUEST_REJECTED;
import static elya.emulator.constants.messages.ExceptionMessages.INVALID_FIELD_VALUE;
import static elya.emulator.constants.messages.ExceptionMessages.MALFORMED_REQUEST_BODY;
import static elya.emulator.constants.messages.ExceptionMessages.REQUEST_BODY_MISSING;
import static elya.emulator.constants.messages.ExceptionMessages.RESOURCE_NOT_FOUND;
import static elya.emulator.constants.messages.ExceptionMessages.UNEXPECTED_INTERNAL_ERROR;
import static elya.emulator.constants.messages.ExceptionMessages.VALIDATION_FAILED;

/**
 * Centralized exception handling for the API Emulator.
 * <p>Extends {@link ResponseEntityExceptionHandler}, so Spring MVC exceptions (unknown path,
 * unsupported method or media type, type mismatch, unreadable body) keep their HTTP status
 * and headers instead of falling into the catch-all {@code 500}.</p>
 * <p>Every handler builds a {@link ProblemDetail} and passes it to {@link #handleExceptionInternal},
 * which logs the failure by status; {@link #createResponseEntity} then renders the detail
 * as the {@link AuthResponse} error envelope.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Authentication challenge sent with every {@code 401} response (RFC 6750, section 3). */
    private static final String BEARER_CHALLENGE = "Bearer";

    /**
     * Handles emulator exceptions with the HTTP status they carry.
     * <p>Adds the {@code WWW-Authenticate} challenge to {@code 401 Unauthorized} responses.</p>
     *
     * @param ex      the caught {@link ApiEmulatorException}
     * @param request the current request
     * @return the error response with the status from {@code ex}
     */
    @Nullable
    @ExceptionHandler(ApiEmulatorException.class)
    public ResponseEntity<Object> handleApiEmulatorException(ApiEmulatorException ex, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (ex.getStatus() == HttpStatus.UNAUTHORIZED) {
            headers.set(HttpHeaders.WWW_AUTHENTICATE, BEARER_CHALLENGE);
        }
        ProblemDetail body = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        return handleExceptionInternal(ex, body, headers, ex.getStatus(), request);
    }

    /**
     * Catch-all for exceptions that no other handler covers.
     * <p>Returns a generic message, so internal details never reach the client.</p>
     *
     * @param ex      the unexpected exception
     * @param request the current request
     * @return the error response with {@code 500 Internal Server Error}
     */
    @Nullable
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, UNEXPECTED_INTERNAL_ERROR);
        return handleExceptionInternal(ex, body, new HttpHeaders(), status, request);
    }

    /**
     * Reports the first field validation message (e.g. {@code @NotNull}, {@code @Range}).
     * <p>The exception message is not used: it contains the rejected value, which may be a card number.</p>
     */
    @Nullable
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        String detail = Optional.ofNullable(ex.getBindingResult().getFieldError())
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse(VALIDATION_FAILED);
        return handleExceptionInternal(ex, ProblemDetail.forStatusAndDetail(status, detail), headers, status, request);
    }

    /**
     * Reports why the request body could not be read: it is missing, a field has a value
     * of the wrong type, or the JSON is malformed.
     * <p>The rejected value is never echoed back, as it may be a card number.</p>
     */
    @Nullable
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(@NonNull HttpMessageNotReadableException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        String detail = switch (ex.getCause()) {
            case null -> REQUEST_BODY_MISSING;
            case MismatchedInputException mismatch when !mismatch.getPath().isEmpty() ->
                    INVALID_FIELD_VALUE.formatted(fieldPath(mismatch));
            default -> MALFORMED_REQUEST_BODY;
        };
        return handleExceptionInternal(ex, ProblemDetail.forStatusAndDetail(status, detail), headers, status, request);
    }

    /**
     * Reports an unknown path with an API-oriented message.
     * <p>Replaces Spring's default detail ("No static resource ..."), which describes
     * static file serving rather than a REST endpoint.</p>
     */
    @Nullable
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(@NonNull NoResourceFoundException ex,
                                                                    @NonNull HttpHeaders headers,
                                                                    @NonNull HttpStatusCode status,
                                                                    @NonNull WebRequest request) {
        return handleExceptionInternal(ex, ProblemDetail.forStatusAndDetail(status, RESOURCE_NOT_FOUND),
                headers, status, request);
    }

    /**
     * Logs every handled failure before the response is built.
     * <p>{@code 5xx} is logged as ERROR with the stack trace. {@code 4xx} is an expected
     * client error and is logged as WARN with the client-facing detail only.</p>
     */
    @Nullable
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception ex,
                                                             @Nullable Object body,
                                                             @NonNull HttpHeaders headers,
                                                             @NonNull HttpStatusCode statusCode,
                                                             @NonNull WebRequest request) {
        String requestDescription = request.getDescription(false);
        if (statusCode.is5xxServerError()) {
            log.error(REQUEST_FAILED, statusCode.value(), requestDescription, ex);
        } else {
            log.warn(REQUEST_REJECTED, statusCode.value(), requestDescription, detailOf(body).orElseGet(ex::getMessage));
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    /**
     * Renders the {@link ProblemDetail} built by the handlers as the {@link AuthResponse} error envelope,
     * keeping the status and the headers (e.g. {@code Allow}, {@code WWW-Authenticate}).
     */
    @NonNull
    @Override
    protected ResponseEntity<Object> createResponseEntity(@Nullable Object body,
                                                          @NonNull HttpHeaders headers,
                                                          @NonNull HttpStatusCode statusCode,
                                                          @NonNull WebRequest request) {
        AuthResponse errorBody = AuthResponse.builder()
                .success(false)
                .message(detailOf(body).orElseGet(statusCode::toString))
                .build();
        return new ResponseEntity<>(errorBody, headers, statusCode);
    }

    /**
     * Builds a client-facing field path from the Jackson reference chain, e.g. {@code cards[0].cardType}.
     *
     * @param ex the Jackson exception carrying the path to the rejected field
     * @return the dotted field path with list indexes in brackets
     */
    private static String fieldPath(MismatchedInputException ex) {
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference reference : ex.getPath()) {
            if (reference.getFieldName() == null) {
                path.append('[').append(reference.getIndex()).append(']');
            } else {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getFieldName());
            }
        }
        return path.toString();
    }

    /**
     * Extracts the client-facing detail from a response body built by the handlers.
     *
     * @param body the response body, usually a {@link ProblemDetail}
     * @return the detail, or empty if the body carries none
     */
    private static Optional<String> detailOf(@Nullable Object body) {
        return body instanceof ProblemDetail problem
                ? Optional.ofNullable(problem.getDetail())
                : Optional.empty();
    }
}