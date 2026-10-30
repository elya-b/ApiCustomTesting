package elya.emulator.constants.messages;

import lombok.experimental.UtilityClass;

/**
 * Centralized dictionary of error messages used throughout the API Emulator.
 * <p>Using a {@link UtilityClass} ensures that error reporting remains consistent
 * across different services and simplify maintenance of localized or technical messages.</p>
 */
@UtilityClass
public class ExceptionMessages {

    /**
     * Error message for cases where the provided authentication token
     * is malformed, missing, or fails signature validation.
     */
    public static final String AUTH_TOKEN_VALIDATION_FAILED = "Token is invalid, expired or missing";

    /**
     * Error message specifically for tokens that were once valid but have since passed
     * their expiration timestamp.
     */
    public static final String AUTH_TOKEN_IS_EXPIRED = "Token is expired";

    /**
     * Error message for an {@code Authorization} header that is missing
     * or does not use the Bearer scheme.
     */
    public static final String INVALID_AUTHORIZATION_HEADER =
            "Invalid Authorization header format. Expected: Bearer <token>";

    /**
     * Error message for a handler that asks for the session token on a path
     * the session token interceptor does not cover (server misconfiguration).
     */
    public static final String SESSION_TOKEN_NOT_AUTHENTICATED =
            "No authenticated session token for %s(): the request path is not covered by SessionTokenInterceptor";

    /** Error message for a request whose body is absent. */
    public static final String REQUEST_BODY_MISSING = "Required request body is missing";

    /** Error message for a request body that cannot be parsed as the expected JSON. */
    public static final String MALFORMED_REQUEST_BODY = "Malformed JSON request body";

    /**
     * Error message template for a JSON field whose value cannot be converted to the field type.
     * <p>The argument is the field path, e.g. {@code cards[0].cardType}; the rejected value is never included.</p>
     */
    public static final String INVALID_FIELD_VALUE = "Invalid value for field '%s'";

    /** Fallback error message for a bean validation failure that has no field error. */
    public static final String VALIDATION_FAILED = "Validation failed";

    /** Error message for unexpected server failures; hides internal details from the client. */
    public static final String UNEXPECTED_INTERNAL_ERROR = "An unexpected internal error occurred";

    /** Error message for a request path that matches no endpoint. */
    public static final String RESOURCE_NOT_FOUND = "Resource not found";

    /**
     * Error message template for a card ID that the session mock does not contain.
     * <p>The argument is the requested card ID.</p>
     */
    public static final String CARD_NOT_FOUND = "Card with ID [%d] not found";

    /** Error message for a login request with blank or unknown credentials. */
    public static final String INVALID_OR_MISSING_CREDENTIALS = "Invalid or missing credentials.";
}