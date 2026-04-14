package elya.emulator.constants.formats;

import lombok.experimental.UtilityClass;

/**
 * Global configuration for date-time strings.
 * It defines the pattern used for parsing and formatting dates in JSON responses
 * to ensure that the Rest-Client and Emulator use the same temporal data format.
 */
@UtilityClass // makes the class final, creates a private constructor
public class DateTimeConstants {
    /**
     * Standard format for token expiration and logging: 2026-02-15 19:43:00
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    /**
     * ISO 8601 compliant format with milliseconds and timezone offset: 2026-02-15T19:43:00.000+0000
     */
    public static final String ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
}
