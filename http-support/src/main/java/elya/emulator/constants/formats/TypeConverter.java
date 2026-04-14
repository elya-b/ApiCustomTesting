package elya.emulator.constants.formats;

/**
 * Interface for universal type-to-string conversion.
 * Simplifies JSON mapping by handling nulls and type casting consistently.
 */
public interface TypeConverter {

    /**
     * Converts a generic Object to a String safely.
     * @param value The object to convert.
     * @return The string representation of the object, or an empty string if null.
     */
    default String asString(Object value) {
        return (value == null) ? "" : String.valueOf(value);
    }

    /**
     * Converts a Long value to a String safely, specifically for numeric fields.
     * @param value The Long value to convert.
     * @return The string representation of the number, or "0" if null to maintain API contracts.
     */
    default String asString(Long value) {
        return (value == null) ? "0" : String.valueOf(value);
    }
}
