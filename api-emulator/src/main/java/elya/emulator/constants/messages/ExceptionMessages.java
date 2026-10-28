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
}