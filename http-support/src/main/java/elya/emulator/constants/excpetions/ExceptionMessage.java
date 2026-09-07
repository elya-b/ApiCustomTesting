package elya.emulator.constants.excpetions;

import lombok.experimental.UtilityClass;

/**
 * Centralized dictionary of error messages used throughout the API Emulator.
 * <p>Using a {@link UtilityClass} ensures that error reporting remains consistent
 * across different services and simplify maintenance of localized or technical messages.</p>
 */
@UtilityClass
public class ExceptionMessage {

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
}