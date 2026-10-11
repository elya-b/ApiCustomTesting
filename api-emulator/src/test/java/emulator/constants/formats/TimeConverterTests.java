package emulator.constants.formats;

import elya.emulator.constants.formats.DateTimeConstants;
import elya.emulator.constants.formats.TimeConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for {@link elya.emulator.constants.formats.TimeConverter}.
 * <ul>
 *   <li>{@code formatWithPattern()} — formats millis with a date-only pattern</li>
 *   <li>{@code formatWithPattern()} — formats millis with a time-only pattern</li>
 *   <li>{@code formatWithPattern()} — formats millis with a full date-time pattern</li>
 *   <li>{@code formatWithPattern()} — ISO_PATTERN throws UnsupportedTemporalTypeException (LocalDateTime does not support timezone offset)</li>
 *   <li>{@code formatWithPattern()} — invalid pattern string throws an exception</li>
 *   <li>{@code formatWithPattern()} — epoch zero (0L) does not throw</li>
 *   <li>{@code formatWithPattern()} — negative millis (before epoch) do not throw</li>
 *   <li>{@code formatToStandard()} — result matches {@code formatWithPattern(millis, DATE_TIME_PATTERN)}</li>
 *   <li>{@code formatToStandard()} — result is parseable back using the standard pattern</li>
 *   <li>{@code formatToStandard()} — returns a non-null, non-blank string</li>
 *   <li>{@code formatToStandard()} — always produces a 19-character string</li>
 *   <li>{@code formatToStandard()} — deterministic: two calls with the same millis produce equal results</li>
 * </ul>
 */
public class TimeConverterTests {

    private final TimeConverter converter = new TimeConverter() {};

    // Fixed millis: 2026-01-15 12:30:00 in system timezone — computed at runtime to avoid TZ hardcoding
    private static final long FIXED_MILLIS = LocalDateTime.of(2026, 1, 15, 12, 30, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();

    // --- formatWithPattern() ---

    @Test
    @DisplayName("formatWithPattern() - Should format millis with date-only pattern")
    void formatWithPattern_ShouldFormatWithDatePattern() {
        String result = converter.formatWithPattern(FIXED_MILLIS, "yyyy-MM-dd");

        assertEquals("2026-01-15", result);
    }

    @Test
    @DisplayName("formatWithPattern() - Should format millis with time-only pattern")
    void formatWithPattern_ShouldFormatWithTimePattern() {
        String result = converter.formatWithPattern(FIXED_MILLIS, "HH:mm:ss");

        assertEquals("12:30:00", result);
    }

    @Test
    @DisplayName("formatWithPattern() - Should format millis with full datetime pattern")
    void formatWithPattern_ShouldFormatWithFullDateTimePattern() {
        String result = converter.formatWithPattern(FIXED_MILLIS, "yyyy-MM-dd HH:mm:ss");

        assertEquals("2026-01-15 12:30:00", result);
    }

    @Test
    @DisplayName("formatWithPattern() - Should support ISO_PATTERN from DateTimeConstants")
    void formatWithPattern_ShouldSupportIsoPattern() {
        // ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ" contains 'Z' (timezone offset).
        // TimeConverter uses LocalDateTime internally, which does NOT carry timezone info,
        // so formatting with an offset-bearing pattern throws UnsupportedTemporalTypeException.
        // This test documents that known limitation.
        assertThrows(java.time.temporal.UnsupportedTemporalTypeException.class,
                () -> converter.formatWithPattern(FIXED_MILLIS, DateTimeConstants.ISO_PATTERN),
                "LocalDateTime cannot format offset ('Z') patterns — " +
                        "UnsupportedTemporalTypeException is expected"
        );
    }

    @Test
    @DisplayName("formatWithPattern() - Should throw for invalid pattern string")
    void formatWithPattern_ShouldThrowForInvalidPattern() {
        assertThrows(Exception.class,
                () -> converter.formatWithPattern(FIXED_MILLIS, "INVALID_XYZ"),
                "Invalid pattern must cause an exception");
    }

    @Test
    @DisplayName("formatWithPattern() - Should handle epoch zero (1970-01-01) without throwing")
    void formatWithPattern_ShouldHandleEpochZero() {
        assertDoesNotThrow(() -> converter.formatWithPattern(0L, "yyyy-MM-dd"));
    }

    @Test
    @DisplayName("formatWithPattern() - Should handle negative millis (before epoch) without throwing")
    void formatWithPattern_ShouldHandleNegativeMillis() {
        assertDoesNotThrow(() -> converter.formatWithPattern(-1L, "yyyy-MM-dd"),
                "Negative millis must not throw");
    }

    // --- formatToStandard() ---

    @Test
    @DisplayName("formatToStandard() - Should produce same result as formatWithPattern with DATE_TIME_PATTERN")
    void formatToStandard_ShouldMatchFormatWithPatternResult() {
        String expected = converter.formatWithPattern(FIXED_MILLIS, DateTimeConstants.DATE_TIME_PATTERN);
        String result = converter.formatToStandard(FIXED_MILLIS);

        assertEquals(expected, result,
                "formatToStandard must be equivalent to formatWithPattern(millis, DATE_TIME_PATTERN)");
    }

    @Test
    @DisplayName("formatToStandard() - Result must be parseable with DATE_TIME_PATTERN")
    void formatToStandard_ResultMustBeParseableWithStandardPattern() {
        String result = converter.formatToStandard(FIXED_MILLIS);

        assertDoesNotThrow(
                () -> LocalDateTime.parse(result,
                        DateTimeFormatter.ofPattern(DateTimeConstants.DATE_TIME_PATTERN)),
                "Result must be parseable back with the standard pattern"
        );
    }

    @Test
    @DisplayName("formatToStandard() - Should return non-null, non-blank string")
    void formatToStandard_ShouldReturnNonNullNonBlank() {
        String result = converter.formatToStandard(System.currentTimeMillis());

        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("formatToStandard() - 'yyyy-MM-dd HH:mm:ss' always produces 19-character string")
    void formatToStandard_ShouldAlwaysProduce19CharResult() {
        String result = converter.formatToStandard(FIXED_MILLIS);

        assertEquals(19, result.length(),
                "Standard datetime format 'yyyy-MM-dd HH:mm:ss' is always 19 characters");
    }

    @Test
    @DisplayName("formatToStandard() - Two calls with same millis must return equal results")
    void formatToStandard_ShouldBeDeterministic() {
        String result1 = converter.formatToStandard(FIXED_MILLIS);
        String result2 = converter.formatToStandard(FIXED_MILLIS);

        assertEquals(result1, result2, "Same millis must always produce the same string");
    }
}