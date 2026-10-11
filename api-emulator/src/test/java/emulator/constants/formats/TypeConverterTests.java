package emulator.constants.formats;

import elya.emulator.constants.formats.TypeConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Unit tests for {@link elya.emulator.constants.formats.TypeConverter}.
 * <ul>
 *   <li>{@code asString(Object)} — returns the string representation of a non-null value</li>
 *   <li>{@code asString(Object)} — returns an empty string for null</li>
 *   <li>{@code asString(Object)} — parameterized: various types are converted to strings</li>
 *   <li>{@code asString(Long)} — returns the string representation of a non-null Long</li>
 *   <li>{@code asString(Long)} — returns "0" for null Long (not an empty string)</li>
 *   <li>{@code asString(Long)} — correctly handles Long.MAX_VALUE</li>
 *   <li>{@code asString(Long)} — correctly handles Long.MIN_VALUE</li>
 *   <li>Null for Object vs null for Long return different results ("" vs "0")</li>
 *   <li>Null Long cast to Object uses the Object overload and returns ""</li>
 * </ul>
 */
public class TypeConverterTests {
    private final TypeConverter converter = new TypeConverter() {};

    @Test
    @DisplayName("asString(Object) - Should return string representation of non-null value")
    void asStringObject_ShouldReturnStringRepresentation() {
        assertEquals("42", converter.asString((Object) 42));
        assertEquals("true", converter.asString((Object) true));
        assertEquals("hello", converter.asString((Object) "hello"));
    }

    @Test
    @DisplayName("asString(Object) - Should return empty string for null")
    void asStringObject_ShouldReturnEmptyStringForNull() {
        assertEquals("", converter.asString((Object) null));
    }

    @ParameterizedTest(name = "[{index}] input={0}, expected={1}")
    @MethodSource("provideObjectValues")
    @DisplayName("asString(Object) - Parameterized: various types to string")
    void asStringObject_ParameterizedConversion(Object input, String expected) {
        assertEquals(expected, converter.asString(input));
    }

    static Stream<Arguments> provideObjectValues() {
        return Stream.of(
                Arguments.of(0, "0"),
                Arguments.of(3.14, "3.14"),
                Arguments.of("text", "text"),
                Arguments.of(Long.MAX_VALUE, String.valueOf(Long.MAX_VALUE)),
                Arguments.of(null, "")
        );
    }

    @Test
    @DisplayName("asString(Long) - Should return string representation for non-null Long")
    void asStringLong_ShouldReturnStringRepresentation() {
        assertEquals("100", converter.asString(100L));
        assertEquals("0", converter.asString(0L));
        assertEquals("-1", converter.asString(-1L));
    }

    @Test
    @DisplayName("asString(Long) - Should return '0' for null Long (not empty string)")
    void asStringLong_ShouldReturnZeroStringForNull() {
        assertEquals("0", converter.asString((Long) null),
                "Null Long must map to '0', not empty string");
    }

    @Test
    @DisplayName("asString(Long) - Should handle Long.MAX_VALUE correctly")
    void asStringLong_ShouldHandleMaxValue() {
        assertEquals(String.valueOf(Long.MAX_VALUE), converter.asString(Long.MAX_VALUE));
    }

    @Test
    @DisplayName("asString(Long) - Should handle Long.MIN_VALUE correctly")
    void asStringLong_ShouldHandleMinValue() {
        assertEquals(String.valueOf(Long.MIN_VALUE), converter.asString(Long.MIN_VALUE));
    }

    @Test
    @DisplayName("asString(Object) vs asString(Long) - null returns differ ('' vs '0')")
    void asString_NullBehaviorDiffersBetweenOverloads() {
        String objectResult = converter.asString((Object) null);
        String longResult   = converter.asString((Long) null);

        assertEquals("", objectResult, "Object null -> empty string");
        assertEquals("0", longResult,  "Long null -> '0'");
    }

    @Test
    @DisplayName("asString(Long) - Long null cast to Object must use Object overload, returning ''")
    void asString_LongNullCastToObjectUsesObjectOverload() {
        // When (Long) null is explicitly cast to Object, the Object overload is resolved
        // and must return "" — not "0" (which would be the Long overload result)
        Object longAsObject = (Long) null;
        String result = converter.asString(longAsObject);

        assertEquals("", result,
                "Null Long cast to Object must use asString(Object), returning empty string");
    }
}