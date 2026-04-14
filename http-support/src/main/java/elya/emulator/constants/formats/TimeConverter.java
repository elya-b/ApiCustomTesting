package elya.emulator.constants.formats;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Interface providing temporal formatting capabilities.
 * Designed as a mix-in to allow any service to format timestamps without direct dependencies.
 */
public interface TimeConverter {

    /**
     * Formats epoch milliseconds into a string based on a specific pattern.
     * @param millis  The timestamp in milliseconds since Unix epoch.
     * @param pattern The date-time pattern string (e.g., yyyy-MM-dd).
     * @return A formatted date-time string.
     */
    default String formatWithPattern(long millis, String pattern) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Formats epoch milliseconds using the project's standard date-time pattern.
     * @param millis The timestamp in milliseconds since Unix epoch.
     * @return A string formatted according to DateTimeConstants.DATE_TIME_PATTERN.
     */
    default String formatToStandard(long millis) {
        return formatWithPattern(millis, DateTimeConstants.DATE_TIME_PATTERN);
    }
}
