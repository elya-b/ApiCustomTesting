package elya.emulator.constants.excpetions;

import lombok.experimental.UtilityClass;

/**
 * Technical note: This class serves as a centralized dictionary for all error messages
 * to ensure consistent error reporting across the emulator.
 */
@UtilityClass
public class ExceptionMessage {
    public static final String UNSUPPORTED_CURRENCY = "Unsupported currency ";
    public static final String UNSUPPORTED_CARD_TYPE = "Unsupported card type: ";
    public static final String AUTH_TOKEN_VALIDATION_FAILED = "Token token is invalid, expired or missing";
    public static final String TOKEN_STORAGE_INIT_FAILED = "Token storage initialization failed!";
    public static final String AUTH_TOKEN_IS_EXPIRED = "Token token is expired";
}
