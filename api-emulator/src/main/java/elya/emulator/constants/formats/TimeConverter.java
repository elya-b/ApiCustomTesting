package elya.emulator.constants.formats;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Interface providing flexible temporal formatting capabilities.
 * <p>Designed as a mix-in component, it allows services to convert epoch-based
 * timestamps into human-readable strings using either custom or standardized patterns.</p>
 */
public interface TimeConverter {

    /**
     * Formats epoch milliseconds into a string representation based on the provided pattern.
     * <p>The conversion is performed using the system's default time zone.</p>
     *
     * @param millis  the timestamp in milliseconds since the Unix epoch.
     * @param pattern the date-time pattern string to be used for formatting.
     * @return a formatted date-time string.
     * @throws java.time.format.DateTimeParseException if the pattern is invalid.
     */
    default String formatWithPattern(long millis, String pattern) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Formats epoch milliseconds using the project's predefined standard date-time pattern.
     * <p>Relies on {@link DateTimeConstants#DATE_TIME_PATTERN} for consistency
     * across the application.</p>
     *
     * @param millis the timestamp in milliseconds since the Unix epoch.
     * @return a string formatted according to the standard system pattern.
     */
    default String formatToStandard(long millis) {
        return formatWithPattern(millis, DateTimeConstants.DATE_TIME_PATTERN);
    }
}