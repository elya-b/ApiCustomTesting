package elya.emulator.constants.formats;

/**
 * Interface providing safe type-to-string conversion utilities.
 * <p>Designed to handle common data mapping scenarios where null safety and
 * consistent default values are required to maintain strict API contracts during
 * JSON serialization or logging.</p>
 */
public interface TypeConverter {

    /**
     * Converts a generic {@link Object} to its string representation safely.
     * * @param value the object to convert (can be null).
     * @return the string representation of the object, or an empty string if the input is null.
     */
    default String asString(Object value) {
        return (value == null) ? "" : String.valueOf(value);
    }

    /**
     * Converts a {@link Long} value to a string, specifically optimized for numeric fields.
     * <p>Returns "0" instead of an empty string for null inputs to ensure that
     * downstream consumers receive a valid numeric string.</p>
     * * @param value the Long value to convert (can be null).
     * @return the string representation of the number, or "0" if the input is null.
     */
    default String asString(Long value) {
        return (value == null) ? "0" : String.valueOf(value);
    }
}