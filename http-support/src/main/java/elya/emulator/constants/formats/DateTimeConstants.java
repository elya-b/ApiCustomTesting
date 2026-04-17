package elya.emulator.constants.formats;

import lombok.experimental.UtilityClass;

/**
 * Global configuration for date and time string representations.
 * <p>Defines the standardized patterns used for parsing and formatting temporal data
 * throughout the emulator. Consistency in these patterns is critical for seamless
 * synchronization between the Emulator services and REST clients.</p>
 */
@UtilityClass
public class DateTimeConstants {

    /**
     * Standard human-readable format, primarily used for token expiration fields
     * and application logging.
     * <p>Example: {@code 2026-02-15 19:43:00}</p>
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * ISO 8601 compliant format including milliseconds and timezone offset.
     * <p>Used for high-precision timestamps and standardized data exchange.
     * Example: {@code 2026-02-15T19:43:00.000+00:00}</p>
     */
    public static final String ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
}