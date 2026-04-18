package elya.emulator.constants.formats;

/**
 * Aggregator interface for universal data transformation and formatting.
 * <p>This interface consolidates {@link TimeConverter} and {@link TypeConverter}
 * capabilities into a single contract. It is designed to be implemented by
 * formatting services to provide a unified entry point for all data conversion
 * tasks across the application layers.</p>
 */
public interface DataTransformer extends TimeConverter, TypeConverter {
    // Acts as a composite interface for centralized data manipulation logic.
}