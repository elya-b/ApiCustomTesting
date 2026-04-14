package elya.emulator.constants.formats;

/**
 * Aggregator interface for universal data transformation.
 * Provides methods for consistent type conversion and temporal formatting
 * across the entire application, regardless of the data's destination.
 */
public interface DataTransformer extends TimeConverter, TypeConverter {
    // A single entry point for data formatting capabilities
}
