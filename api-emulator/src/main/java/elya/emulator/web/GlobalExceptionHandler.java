package elya.emulator.web;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.*;
import elya.emulator.exceptions.*;
import lombok.extern.slf4j.*;
import org.springframework.context.support.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.lang.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;
import org.springframework.web.servlet.mvc.method.annotation.*;
import org.springframework.web.servlet.resource.*;

import java.util.*;

import static elya.emulator.constants.logs.EmulatorErrorLogs.*;
import static elya.emulator.constants.logs.EmulatorWarnLogs.*;
import static elya.emulator.constants.messages.ExceptionMessages.*;

/**
 * Centralized exception handling for the API Emulator.
 * <p>Extends {@link ResponseEntityExceptionHandler}, so Spring MVC exceptions (unknown path,
 * unsupported method or media type, type mismatch, unreadable body) keep their HTTP status
 * and headers instead of falling into the catch-all {@code 500}.</p>
 * <p>Every error is returned as an RFC 9457 {@link ProblemDetail}: each handler builds the detail
 * and passes it to {@link #handleExceptionInternal}, which logs the failure by status.
 * Spring fills {@code title} from the status and {@code instance} from the request path.</p>
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
     * Extracts the client-facing detail from a handler-built body, so the log shows
     * exactly what the client receives.
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